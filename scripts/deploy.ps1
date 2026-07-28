$ErrorActionPreference = 'Stop'

Write-Host "=== [Deploy Stage] Blue-Green Deployment Started (Windows) ==="

$deployDir = if ($env:DEPLOY_DIR) { $env:DEPLOY_DIR } else { "$env:WORKSPACE\deploy" }
$resourceDir = if ($env:RESOURCE_DIR) { $env:RESOURCE_DIR } else { "" }
$imageName = if ($env:IMAGE_NAME) { $env:IMAGE_NAME } else { "yak-allim-backend:latest" }
$n8nWebhookUrl = if ($env:N8N_WEBHOOK_URL) { $env:N8N_WEBHOOK_URL } else { "http://yak-allim-n8n:5678/webhook/ocr" }

# 1. 배포 디렉터리 생성 및 키/모델 준비
if (!(Test-Path -Path "$deployDir")) {
    New-Item -ItemType Directory -Force -Path "$deployDir" | Out-Null
}
$modelsDir = "$deployDir\models"
if (!(Test-Path -Path $modelsDir)) {
    New-Item -ItemType Directory -Force -Path $modelsDir | Out-Null
}

$firebaseKeyPath = "$deployDir\yak-allim-firebase-key.json"
if ($env:FIREBASE_KEY_FILE -and (Test-Path -Path $env:FIREBASE_KEY_FILE)) {
    Copy-Item -Path $env:FIREBASE_KEY_FILE -Destination $firebaseKeyPath -Force
}

if ($resourceDir -and (Test-Path -Path "$resourceDir") -and (Get-ChildItem -Path "$resourceDir" | Select-Object -First 1)) {
    Copy-Item -Path "$resourceDir\*" -Destination $modelsDir -Force -Recurse
} elseif (Test-Path -Path "src\main\resources\models") {
    Copy-Item -Path "src\main\resources\models\*" -Destination $modelsDir -Force -Recurse
} else {
    throw "OCR 모델 리소스 누락"
}

# 2. Docker 이미지 빌드
docker build -t $imageName .

# 3. 블루-그린 포트 및 컨테이너 이름 결정 (8082 <-> 8083)
$ErrorActionPreference = 'SilentlyContinue'
$isBlueActive = docker ps --filter "name=^/yak-allim-backend-blue$" --filter "status=running" -q
$ErrorActionPreference = 'Stop'

if ($isBlueActive) {
    $targetPort = "8083"
    $targetName = "yak-allim-backend-green"
    $oldContainerName = "yak-allim-backend-blue"
} else {
    $targetPort = "8082"
    $targetName = "yak-allim-backend-blue"
    $oldContainerName = "yak-allim-backend-green"
}

Write-Host "=== Target 배포 설정 ==="
Write-Host "Target Port: $targetPort"
Write-Host "Target Container Name: $targetName"

# 4. 대상 명시적 이름 및 포트 점유 컨테이너 정리
$myContainerId = $env:COMPUTERNAME
$ErrorActionPreference = 'SilentlyContinue'
docker stop $targetName
docker rm -f $targetName

$portOccupiedContainers = docker ps -aq --filter "publish=$targetPort"
if ($portOccupiedContainers) {
    foreach ($cid in $portOccupiedContainers) {
        if ($myContainerId -and ($cid -eq $myContainerId)) {
            Write-Host "Warning: Jenkins 컨테이너가 Target Port($targetPort)를 사용 중이므로 정리 대상에서 제외합니다."
            continue
        }
        Write-Host "=== Target Port($targetPort) 점유 컨테이너($cid) 정리 중... ==="
        docker rm -f $cid
    }
}
$ErrorActionPreference = 'Stop'

# 5. 신규 컨테이너 생성 및 자격 증명/모델 파일 주입
docker create `
    --name $targetName `
    --restart unless-stopped `
    --network app-network `
    -p "${targetPort}:8081" `
    $imageName `
    --server.port=8081 `
    --notification.firebase.key-path="file:/app/yak-allim-firebase-key.json" `
    --ocr.engine.onnx.detection-model-path="file:/app/models/ch_PP-OCRv4_det_infer.onnx" `
    --ocr.engine.onnx.recognition-model-path="file:/app/models/korean_PP-OCRv4_rec_infer.onnx" `
    --ocr.engine.onnx.recognition-dictionary-path="file:/app/models/korean_dict.txt" `
    --ocr.n8n.webhook-url="$n8nWebhookUrl"

# Firebase Key 파일이 실제로 존재하는 경우에만 docker cp 수행
if (Test-Path $firebaseKeyPath) {
    docker cp $firebaseKeyPath "${targetName}:/app/yak-allim-firebase-key.json"
}

if (Test-Path $modelsDir) {
    docker cp "$modelsDir" "${targetName}:/app/"
}

docker start $targetName

# 6. Spring Boot Actuator HTTP 헬스 체크 진행
$healthSuccess = $false
Write-Host "=== 신규 컨테이너(${targetName}) Actuator HTTP 헬스 체크 진행 중... ==="

for ($retry = 1; $retry -le 40; $retry++) {
    Start-Sleep -Seconds 4
    try {
        $res = Invoke-WebRequest -Uri "http://localhost:${targetPort}/actuator/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($res.StatusCode -eq 200) {
            $healthSuccess = $true
            break
        }
    } catch {
        Write-Host "Spring Boot 구동 확인 중... (시도 $retry/40)"
    }
}

if ($healthSuccess) {
    Write-Host "=== 신규 컨테이너(${targetName}) 정상 구동 완료 (PORT: ${targetPort}) ==="
    docker logs --tail 25 $targetName

    # 7. Nginx 포트 스위칭
    $deployIncPath = "$deployDir\service-url.inc"
    Write-Host "=== Nginx 포트 스위칭 (Target: ${targetName} / Port: ${targetPort}) 진행 ==="
    [System.IO.File]::WriteAllText("$deployIncPath", 'set $service_url http://' + $targetName + ':8081;')
    docker exec yak-allim-nginx sh -c "printf 'set \x24service_url http://$targetName:8081;\n' > /etc/nginx/conf.d/service-url.inc"

    Write-Host "=== NGINX 설정 재설정 ==="
    $ErrorActionPreference = 'SilentlyContinue'
    docker exec yak-allim-nginx nginx -s reload
    $ErrorActionPreference = 'Stop'

    # 8. 이전 구버전 컨테이너 정지 및 삭제
    $ErrorActionPreference = 'SilentlyContinue'
    $oldContainerId = docker ps --filter "name=^/${oldContainerName}$" --filter "status=running" -q
    $ErrorActionPreference = 'Stop'
    if ($oldContainerId) {
        if ($myContainerId -and ($oldContainerId -eq $myContainerId)) {
            Write-Host "Warning: Jenkins 컨테이너는 구버전 정지 대상에서 제외합니다."
        } else {
            Write-Host "=== 이전 구버전 컨테이너(${oldContainerName}) 정지 및 정리 중... ==="
            $ErrorActionPreference = 'SilentlyContinue'
            docker stop $oldContainerId
            docker rm -f $oldContainerId
            $ErrorActionPreference = 'Stop'
        }
    }
} else {
    Write-Host "=== 신규 컨테이너(${targetName}) 헬스 체크 실패 ==="
    docker logs --tail 30 $targetName
    $ErrorActionPreference = 'SilentlyContinue'
    docker rm -f $targetName
    $ErrorActionPreference = 'Stop'
    throw "신규 컨테이너 구동 실패"
}