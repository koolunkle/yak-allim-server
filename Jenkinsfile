pipeline {
    agent any
    
    environment {
        PORT = '8081'
        // 백그라운드 프로세스 유지
        JENKINS_NODE_COOKIE = 'dontKillMe'
    }

    stages {
        stage('Checkout') {
            steps {
                // 소스 코드 동기화
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    // OS 환경(Linux / Windows)에 따른 빌드 명령어 분기
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
                // 자격 증명 주입 및 배포 프로세스 가동
                withCredentials([file(credentialsId: 'firebase-messaging-key', variable: 'FIREBASE_KEY_FILE')]) {
                    script {
                        if (isUnix()) {
                            def deployDir = env.DEPLOY_DIR ?: "${env.WORKSPACE}/deploy"
                            def resourceDir = env.RESOURCE_DIR ?: ""
                            def port = env.PORT ?: "8081"

                            sh """
                                # 1. 배포 디렉터리 보장
                                mkdir -p "${deployDir}/models"

                                # 2. Firebase 자격 증명 복사
                                rm -f "${deployDir}/yak-allim-firebase-key.json" 2>/dev/null || true
                                cp "\$FIREBASE_KEY_FILE" "${deployDir}/yak-allim-firebase-key.json"

                                # 3. 외장 OCR 모델 파일 복사 (외장 경로 우선, 없으면 프로젝트 내 src/main/resources/models 사용)
                                if [ -n "${resourceDir}" ] && [ -d "${resourceDir}" ] && [ "\$(ls -A "${resourceDir}" 2>/dev/null)" ]; then
                                    cp -r "${resourceDir}"/* "${deployDir}/models/"
                                elif [ -d "src/main/resources/models" ]; then
                                    cp -r src/main/resources/models/* "${deployDir}/models/"
                                else
                                    echo "OCR 모델 리소스 누락" && exit 1
                                fi

                                # 4. 기존 실행 프로세스 종료 (lsof/fuser 미설치 환경 대응을 위해 pkill 병행)
                                if command -v lsof >/dev/null 2>&1; then
                                    PID=\$(lsof -t -i:${port} || true)
                                    [ -n "\$PID" ] && kill -9 \$PID || true
                                elif command -v fuser >/dev/null 2>&1; then
                                    PID=\$(fuser ${port}/tcp 2>/dev/null || true)
                                    [ -n "\$PID" ] && kill -9 \$PID || true
                                fi
                                pkill -9 -f "${deployDir}/server.jar" || true
                                sleep 3

                                # 5. 배포 산출물 이동
                                JAR_FILE=\$(ls build/libs/*.jar 2>/dev/null | head -n 1)
                                if [ -n "\$JAR_FILE" ]; then
                                    rm -f "${deployDir}/server.jar" 2>/dev/null || true
                                    cp "\$JAR_FILE" "${deployDir}/server.jar"
                                else
                                    echo "JAR 파일 누락" && exit 1
                                fi

                                # 6. 서버 백그라운드 구동 (작업 디렉터리 이동 및 키 경로 명시)
                                cd "${deployDir}"
                                BUILD_ID=dontKillMe JENKINS_NODE_COOKIE=dontKillMe nohup java -jar "${deployDir}/server.jar" --server.port=${port} \\
                                    --notification.firebase.key-path="file:${deployDir}/yak-allim-firebase-key.json" \\
                                    --ocr.engine.onnx.detection-model-path="file:${deployDir}/models/ch_PP-OCRv4_det_infer.onnx" \\
                                    --ocr.engine.onnx.recognition-model-path="file:${deployDir}/models/korean_PP-OCRv4_rec_infer.onnx" \\
                                    --ocr.engine.onnx.recognition-dictionary-path="file:${deployDir}/models/korean_dict.txt" > "${deployDir}/spring-server.log" 2> "${deployDir}/spring-server-error.log" &

                                echo "Spring Boot 서버 초기화 대기 중..."
                                SUCCESS=0
                                for i in \$(seq 1 15); do
                                    sleep 1
                                    if grep -q "Started " "${deployDir}/spring-server.log" 2>/dev/null || grep -q "Tomcat started" "${deployDir}/spring-server.log" 2>/dev/null; then
                                        SUCCESS=1
                                        break
                                    fi
                                    if ! ps aux | grep -v grep | grep "${deployDir}/server.jar" > /dev/null; then
                                        break
                                    fi
                                done

                                echo "=== Spring Boot 서버 구동 로그 ==="
                                tail -n 25 "${deployDir}/spring-server.log" 2>/dev/null || true

                                if [ \$SUCCESS -eq 1 ]; then
                                    echo "Spring Boot 서버가 정상적으로 구동되었습니다. (PORT: ${port})"
                                else
                                    echo "=== Spring Boot 서버 구동 실패 - 에러 로그 ==="
                                    cat "${deployDir}/spring-server-error.log" 2>/dev/null || true
                                    exit 1
                                fi
                            """
                        } else {
                            def deployDir = env.DEPLOY_DIR ?: "${env.WORKSPACE}\\deploy"
                            def resourceDir = env.RESOURCE_DIR ?: ""
                            def port = env.PORT ?: '8081'

                            powershell """
                                # 1. 배포 디렉터리 보장
                                if (!(Test-Path -Path "${deployDir}")) {
                                    New-Item -ItemType Directory -Force -Path "${deployDir}"
                                }
                                \$modelsDir = "${deployDir}\\models"
                                if (!(Test-Path -Path \$modelsDir)) {
                                    New-Item -ItemType Directory -Force -Path \$modelsDir
                                }

                                # 2. Firebase 자격 증명 복사
                                Copy-Item -Path \$env:FIREBASE_KEY_FILE -Destination "${deployDir}\\yak-allim-firebase-key.json" -Force

                                # 3. 외장 OCR 모델 파일 복사 (외장 경로 우선, 없으면 프로젝트 내 src/main/resources/models 사용)
                                if ("${resourceDir}" -and (Test-Path -Path "${resourceDir}") -and (Get-ChildItem -Path "${resourceDir}" | Select-Object -First 1)) {
                                    Copy-Item -Path "${resourceDir}\\*" -Destination \$modelsDir -Force -Recurse
                                } elseif (Test-Path -Path "src\\main\\resources\\models") {
                                    Copy-Item -Path "src\\main\\resources\\models\\*" -Destination \$modelsDir -Force -Recurse
                                } else {
                                    throw "OCR 모델 리소스 누락"
                                }

                                # 4. 기존 실행 프로세스 종료
                                \$portConn = Get-NetTCPConnection -LocalPort ${port} -ErrorAction SilentlyContinue
                                if (\$portConn) {
                                    \$pidToKill = \$portConn.OwningProcess | Select-Object -Unique
                                    Stop-Process -Id \$pidToKill -Force
                                    Start-Sleep -Seconds 3
                                }

                                # 5. 배포 산출물 이동
                                \$jarFile = Get-ChildItem -Path "build\\libs\\*.jar" | Select-Object -First 1
                                if (\$jarFile) {
                                    Copy-Item -Path \$jarFile.FullName -Destination "${deployDir}\\server.jar" -Force
                                } else {
                                    throw "JAR 파일 누락"
                                }

                                # 6. 서버 백그라운드 구동
                                \$arguments = "-jar ${deployDir}\\server.jar --server.port=${port} " +
                                             "--notification.firebase.key-path=file:${deployDir}\\yak-allim-firebase-key.json " +
                                             "--ocr.engine.onnx.detection-model-path=file:\$modelsDir\\ch_PP-OCRv4_det_infer.onnx " +
                                             "--ocr.engine.onnx.recognition-model-path=file:\$modelsDir\\korean_PP-OCRv4_rec_infer.onnx " +
                                             "--ocr.engine.onnx.recognition-dictionary-path=file:\$modelsDir\\korean_dict.txt"

                                Start-Process -FilePath "java" -ArgumentList \$arguments -WorkingDirectory "${deployDir}" -RedirectStandardOutput "${deployDir}\\spring-server.log" -RedirectStandardError "${deployDir}\\spring-server-error.log" -WindowStyle Hidden
                            """
                        }
                    }
                }
            }
        }
    }
}
