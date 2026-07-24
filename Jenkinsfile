pipeline {
    agent any

    environment {
        IMAGE_NAME = 'yak-allim-backend:latest'
        JENKINS_NODE_COOKIE = 'dontKillMe'
    }

    stages {
        stage('Build') {
            steps {
                script {
                    // OS별 빌드 실행
                    if (isUnix()) {
                        sh 'chmod +x gradlew'
                        sh './gradlew clean bootJar'
                    } else {
                        bat 'gradlew.bat clean bootJar'
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                // 자격 증명 주입 및 블루-그린 배포 실행
                withCredentials([file(credentialsId: 'firebase-messaging-key', variable: 'FIREBASE_KEY_FILE')]) {
                    script {
                        if (isUnix()) {
                            def deployDir = env.DEPLOY_DIR ?: "${env.WORKSPACE}/deploy"
                            def resourceDir = env.RESOURCE_DIR ?: ""

                            sh """
                                # 1. 배포 디렉터리 생성 및 키/모델 준비
                                mkdir -p "${deployDir}/models"
                                rm -f "${deployDir}/yak-allim-firebase-key.json" 2>/dev/null || true
                                cp "\$FIREBASE_KEY_FILE" "${deployDir}/yak-allim-firebase-key.json"

                                if [ -n "${resourceDir}" ] && [ -d "${resourceDir}" ] && [ "\$(ls -A "${resourceDir}" 2>/dev/null)" ]; then
                                    cp -r "${resourceDir}"/* "${deployDir}/models/"
                                elif [ -d "src/main/resources/models" ]; then
                                    cp -r src/main/resources/models/* "${deployDir}/models/"
                                else
                                    echo "OCR 모델 리소스 누락" && exit 1
                                fi

                                # 2. Docker 이미지 빌드
                                docker build -t ${env.IMAGE_NAME} .

                                # 3. 블루-그린 포트 및 컨테이너 이름 결정 (8082 <-> 8083)
                                IS_BLUE_ACTIVE=\$(docker ps --filter "name=^/yak-allim-backend-blue\$" --filter "status=running" -q 2>/dev/null || true)

                                if [ -n "\$IS_BLUE_ACTIVE" ]; then
                                    TARGET_PORT=8083
                                    TARGET_NAME="yak-allim-backend-green"
                                    OLD_CONTAINER_NAME="yak-allim-backend-blue"
                                else
                                    TARGET_PORT=8082
                                    TARGET_NAME="yak-allim-backend-blue"
                                    OLD_CONTAINER_NAME="yak-allim-backend-green"
                                fi

                                echo "=== Target 배포 설정 ==="
                                echo "Target Port: \${TARGET_PORT}"
                                echo "Target Container Name: \${TARGET_NAME}"

                                # 4. 대상 명시적 이름 및 포트 점유 컨테이너 정리
                                MY_CONTAINER_ID=\$(hostname 2>/dev/null || true)

                                docker stop \${TARGET_NAME} 2>/dev/null || true
                                docker rm -f \${TARGET_NAME} 2>/dev/null || true

                                PORT_OCCUPIED_CONTAINERS=\$(docker ps -aq --filter "publish=\${TARGET_PORT}" 2>/dev/null || true)
                                if [ -n "\$PORT_OCCUPIED_CONTAINERS" ]; then
                                    for cid in \$PORT_OCCUPIED_CONTAINERS; do
                                        if [ -n "\$MY_CONTAINER_ID" ] && [ "\$cid" = "\$MY_CONTAINER_ID" ]; then
                                            echo "Warning: Jenkins 컨테이너가 Target Port(\${TARGET_PORT})를 사용 중이므로 정리 대상에서 제외합니다."
                                            continue
                                        fi
                                        echo "=== Target Port(\${TARGET_PORT}) 점유 컨테이너(\$cid) 정리 중... ==="
                                        docker rm -f \$cid 2>/dev/null || true
                                    done
                                fi

                                # 5. 신규 컨테이너 생성 및 자격 증명/모델 파일 주입
                                docker create \
                                    --name \${TARGET_NAME} \
                                    --restart unless-stopped \
                                    -p \${TARGET_PORT}:8081 \
                                    ${env.IMAGE_NAME} \
                                    --server.port=8081 \
                                    --notification.firebase.key-path="file:/app/yak-allim-firebase-key.json" \
                                    --ocr.engine.onnx.detection-model-path="file:/app/models/ch_PP-OCRv4_det_infer.onnx" \
                                    --ocr.engine.onnx.recognition-model-path="file:/app/models/korean_PP-OCRv4_rec_infer.onnx" \
                                    --ocr.engine.onnx.recognition-dictionary-path="file:/app/models/korean_dict.txt"

                                docker cp "${deployDir}/yak-allim-firebase-key.json" \${TARGET_NAME}:/app/yak-allim-firebase-key.json
                                if [ -d "${deployDir}/models" ]; then
                                    docker cp "${deployDir}/models" \${TARGET_NAME}:/app/models
                                fi

                                docker start \${TARGET_NAME}

                                # 6. Spring Boot Actuator HTTP 헬스 체크 진행
                                HEALTH_SUCCESS=false
                                echo "=== 신규 컨테이너(\${TARGET_NAME}) Actuator HTTP 헬스 체크 진행 중... ==="

                                TARGET_IP=\$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' \${TARGET_NAME} 2>/dev/null || true)

                                for retry in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
                                    sleep 3
                                    HTTP_CODE=\$(curl -s -o /dev/null -w "%{http_code}" http://localhost:\${TARGET_PORT}/actuator/health 2>/dev/null || true)
                                    if [ "\$HTTP_CODE" != "200" ] && [ -n "\$TARGET_IP" ]; then
                                        HTTP_CODE=\$(curl -s -o /dev/null -w "%{http_code}" http://\${TARGET_IP}:8081/actuator/health 2>/dev/null || true)
                                    fi

                                    if [ "\$HTTP_CODE" = "200" ]; then
                                        HEALTH_SUCCESS=true
                                        break
                                    fi
                                    echo "Spring Boot 구동 확인 중... (HTTP Status: \${HTTP_CODE:-000}, 시도 \$retry/20)"
                                done

                                if [ "\$HEALTH_SUCCESS" = "true" ]; then
                                    echo "=== 신규 컨테이너(\${TARGET_NAME}) 정상 구동 완료 (PORT: \${TARGET_PORT}) ==="
                                    docker logs --tail 25 \${TARGET_NAME}

                                    # 7. Nginx 포트 스위칭 및 Reload (Nginx 환경이 구성되어 있을 경우)
                                    if command -v nginx >/dev/null 2>&1 || [ -d "/etc/nginx" ]; then
                                        echo "=== Nginx 포트 스위칭 (Target: \${TARGET_PORT}) 및 Reload 진행 ==="
                                        if [ -d "/etc/nginx/conf.d" ]; then
                                            echo "set \\\$service_url http://127.0.0.1:\${TARGET_PORT};" | sudo tee /etc/nginx/conf.d/service-url.inc >/dev/null 2>&1 || echo "set \\\$service_url http://127.0.0.1:\${TARGET_PORT};" > /etc/nginx/conf.d/service-url.inc 2>/dev/null || true
                                        fi
                                        sudo nginx -s reload 2>/dev/null || nginx -s reload 2>/dev/null || echo "Nginx reload 권한 또는 실행 실패 (수동확인 필요)"
                                    fi

                                    # 8. 이전 구버전 컨테이너 정지 및 삭제
                                    OLD_CONTAINER_ID=\$(docker ps --filter "name=^/\${OLD_CONTAINER_NAME}\$" --filter "status=running" -q 2>/dev/null || true)
                                    if [ -n "\$OLD_CONTAINER_ID" ]; then
                                        if [ -n "\$MY_CONTAINER_ID" ] && [ "\$OLD_CONTAINER_ID" = "\$MY_CONTAINER_ID" ]; then
                                            echo "Warning: Jenkins 컨테이너는 구버전 정지 대상에서 제외합니다."
                                        else
                                            echo "=== 이전 구버전 컨테이너(\${OLD_CONTAINER_NAME}) 정지 및 정리 중... ==="
                                            docker stop \$OLD_CONTAINER_ID 2>/dev/null || true
                                            docker rm -f \$OLD_CONTAINER_ID 2>/dev/null || true
                                        fi
                                    fi
                                else
                                    echo "=== 신규 컨테이너(\${TARGET_NAME}) 헬스 체크 실패 ==="
                                    docker logs --tail 30 \${TARGET_NAME} 2>/dev/null || true
                                    docker rm -f \${TARGET_NAME} 2>/dev/null || true
                                    exit 1
                                fi
                            """
                        } else {
                            def deployDir = env.DEPLOY_DIR ?: "${env.WORKSPACE}\\deploy"
                            def resourceDir = env.RESOURCE_DIR ?: ""

                            powershell """
                                # 1. 배포 디렉터리 생성 및 키/모델 준비
                                if (!(Test-Path -Path "${deployDir}")) {
                                    New-Item -ItemType Directory -Force -Path "${deployDir}"
                                }
                                \$modelsDir = "${deployDir}\\models"
                                if (!(Test-Path -Path \$modelsDir)) {
                                    New-Item -ItemType Directory -Force -Path \$modelsDir
                                }

                                Copy-Item -Path \$env:FIREBASE_KEY_FILE -Destination "${deployDir}\\yak-allim-firebase-key.json" -Force

                                if ("${resourceDir}" -and (Test-Path -Path "${resourceDir}") -and (Get-ChildItem -Path "${resourceDir}" | Select-Object -First 1)) {
                                    Copy-Item -Path "${resourceDir}\\*" -Destination \$modelsDir -Force -Recurse
                                } elseif (Test-Path -Path "src\\main\\resources\\models") {
                                    Copy-Item -Path "src\\main\\resources\\models\\*" -Destination \$modelsDir -Force -Recurse
                                } else {
                                    throw "OCR 모델 리소스 누락"
                                }

                                # 2. Docker 이미지 빌드
                                docker build -t ${env.IMAGE_NAME} .

                                # 3. 블루-그린 포트 및 컨테이너 이름 결정 (8082 <-> 8083)
                                \$ErrorActionPreference = 'SilentlyContinue'
                                \$isBlueActive = docker ps --filter "name=^/yak-allim-backend-blue\$" --filter "status=running" -q
                                \$ErrorActionPreference = 'Stop'

                                if (\$isBlueActive) {
                                    \$targetPort = "8083"
                                    \$targetName = "yak-allim-backend-green"
                                    \$oldContainerName = "yak-allim-backend-blue"
                                } else {
                                    \$targetPort = "8082"
                                    \$targetName = "yak-allim-backend-blue"
                                    \$oldContainerName = "yak-allim-backend-green"
                                }

                                Write-Host "=== Target 배포 설정 ==="
                                Write-Host "Target Port: \${targetPort}"
                                Write-Host "Target Container Name: \${targetName}"

                                # 4. 대상 명시적 이름 및 포트 점유 컨테이너 정리
                                \$myContainerId = \$env:COMPUTERNAME
                                \$ErrorActionPreference = 'SilentlyContinue'
                                docker stop \$targetName
                                docker rm -f \$targetName

                                \$portOccupiedContainers = docker ps -aq --filter "publish=\${targetPort}"
                                if (\$portOccupiedContainers) {
                                    foreach (\$cid in \$portOccupiedContainers) {
                                        if (\$myContainerId -and (\$cid -eq \$myContainerId)) {
                                            Write-Host "Warning: Jenkins 컨테이너가 Target Port(\${targetPort})를 사용 중이므로 정리 대상에서 제외합니다."
                                            continue
                                        }
                                        Write-Host "=== Target Port(\${targetPort}) 점유 컨테이너(\$cid) 정리 중... ==="
                                        docker rm -f \$cid
                                    }
                                }
                                \$ErrorActionPreference = 'Stop'

                                # 5. 신규 컨테이너 생성 및 자격 증명/모델 파일 주입
                                docker create `
                                    --name \$targetName `
                                    --restart unless-stopped `
                                    -p "\${targetPort}:8081" `
                                    ${env.IMAGE_NAME} `
                                    --server.port=8081 `
                                    --notification.firebase.key-path="file:/app/yak-allim-firebase-key.json" `
                                    --ocr.engine.onnx.detection-model-path="file:/app/models/ch_PP-OCRv4_det_infer.onnx" `
                                    --ocr.engine.onnx.recognition-model-path="file:/app/models/korean_PP-OCRv4_rec_infer.onnx" `
                                    --ocr.engine.onnx.recognition-dictionary-path="file:/app/models/korean_dict.txt"

                                docker cp "${deployDir}\\yak-allim-firebase-key.json" "\${targetName}:/app/yak-allim-firebase-key.json"
                                if (Test-Path \$modelsDir) {
                                    docker cp "\$modelsDir\\." "\${targetName}:/app/models/"
                                }

                                docker start \$targetName

                                # 6. Spring Boot Actuator HTTP 헬스 체크 진행
                                \$healthSuccess = \$false
                                Write-Host "=== 신규 컨테이너(\${targetName}) Actuator HTTP 헬스 체크 진행 중... ==="

                                for (\$retry = 1; \$retry -le 15; \$retry++) {
                                    Start-Sleep -Seconds 3
                                    try {
                                        \$res = Invoke-WebRequest -Uri "http://localhost:\${targetPort}/actuator/health" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
                                        if (\$res.StatusCode -eq 200) {
                                            \$healthSuccess = \$true
                                            break
                                        }
                                    } catch {
                                        Write-Host "Spring Boot 구동 확인 중... (시도 \$retry/15)"
                                    }
                                }

                                if (\$healthSuccess) {
                                    Write-Host "=== 신규 컨테이너(\${targetName}) 정상 구동 완료 (PORT: \${targetPort}) ==="
                                    docker logs --tail 25 \$targetName

                                    # 7. Nginx 포트 스위칭 및 Reload (Windows/외부 Nginx 환경 구성 시)
                                    \$nginxIncPath = "C:/nginx/conf/service-url.inc"
                                    if (Test-Path \$nginxIncPath) {
                                        Write-Host "=== Nginx 포트 스위칭 (Target: \${targetPort}) 및 Reload 진행 ==="
                                        "set `$service_url http://127.0.0.1:\${targetPort};" | Out-File -Encoding utf8 \$nginxIncPath
                                        \$ErrorActionPreference = 'SilentlyContinue'
                                        nginx -s reload
                                        \$ErrorActionPreference = 'Stop'
                                    }

                                    # 8. 이전 구버전 컨테이너 정지 및 삭제
                                    \$ErrorActionPreference = 'SilentlyContinue'
                                    \$oldContainerId = docker ps --filter "name=^/\${oldContainerName}\$" --filter "status=running" -q
                                    \$ErrorActionPreference = 'Stop'
                                    if (\$oldContainerId) {
                                        if (\$myContainerId -and (\$oldContainerId -eq \$myContainerId)) {
                                            Write-Host "Warning: Jenkins 컨테이너는 구버전 정지 대상에서 제외합니다."
                                        } else {
                                            Write-Host "=== 이전 구버전 컨테이너(\${oldContainerName}) 정지 및 정리 중... ==="
                                            \$ErrorActionPreference = 'SilentlyContinue'
                                            docker stop \$oldContainerId
                                            docker rm -f \$oldContainerId
                                            \$ErrorActionPreference = 'Stop'
                                        }
                                    }
                                } else {
                                    Write-Host "=== 신규 컨테이너(\${targetName}) 헬스 체크 실패 ==="
                                    docker logs --tail 30 \$targetName
                                    \$ErrorActionPreference = 'SilentlyContinue'
                                    docker rm -f \$targetName
                                    \$ErrorActionPreference = 'Stop'
                                    throw "신규 컨테이너 구동 실패"
                                }
                            """
                        }
                    }
                }
            }
        }
    }
}