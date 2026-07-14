pipeline {
    agent any
    
    environment {
        DEPLOY_DIR = 'C:\\yakallim-deploy'
        RESOURCE_DIR = 'C:\\yakallim-resources'
        PORT = '8080'
        // 백그라운드 프로세스 유지
        JENKINS_NODE_COOKIE = 'dontKillMe'
    }
    
    stages {
        stage('Checkout') {
            steps {
                // 코드 동기화
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                // 애플리케이션 빌드
                bat 'gradlew.bat clean bootJar'
            }
        }
        
        stage('Deploy') {
            steps {
                // 자격 증명 주입 및 배포 프로세스 가동
                withCredentials([file(credentialsId: 'firebase-messaging-key', variable: 'FIREBASE_KEY_FILE')]) {
                    powershell """
                        # 배포 디렉터리 보장
                        if (!(Test-Path -Path "${env.DEPLOY_DIR}")) {
                            New-Item -ItemType Directory -Force -Path "${env.DEPLOY_DIR}"
                        }
                        \$modelsDir = "${env.DEPLOY_DIR}\\models"
                        if (!(Test-Path -Path \$modelsDir)) {
                            New-Item -ItemType Directory -Force -Path \$modelsDir
                        }
                        
                        # Firebase 자격 증명 복사
                        Copy-Item -Path \$env:FIREBASE_KEY_FILE -Destination "${env.DEPLOY_DIR}\\yak-allim-firebase-key.json" -Force
                        
                        # 외장 OCR 모델 파일 복사
                        if (Test-Path -Path "${env.RESOURCE_DIR}") {
                            Copy-Item -Path "${env.RESOURCE_DIR}\\*" -Destination \$modelsDir -Force -Recurse
                        } else {
                            throw "리소스 누락"
                        }
                        
                        # 기존 실행 프로세스 종료
                        \$portConn = Get-NetTCPConnection -LocalPort ${env.PORT} -ErrorAction SilentlyContinue
                        if (\$portConn) {
                            \$pidToKill = \$portConn.OwningProcess | Select-Object -Unique
                            Stop-Process -Id \$pidToKill -Force
                            Start-Sleep -Seconds 3
                        }
                        
                        # 배포 산출물 이동
                        \$jarFile = Get-ChildItem -Path "build\\libs\\*.jar" | Select-Object -First 1
                        if (\$jarFile) {
                            Copy-Item -Path \$jarFile.FullName -Destination "${env.DEPLOY_DIR}\\server.jar" -Force
                        } else {
                            throw "JAR 파일 누락"
                        }
                        
                        # 서버 백그라운드 구동
                        \$arguments = "-jar ${env.DEPLOY_DIR}\\server.jar --server.port=${env.PORT} " +
                                     "--ocr.engine.onnx.detection-model-path=file:\$modelsDir\\ch_PP-OCRv4_det_infer.onnx " +
                                     "--ocr.engine.onnx.recognition-model-path=file:\$modelsDir\\korean_PP-OCRv4_rec_infer.onnx " +
                                     "--ocr.engine.onnx.recognition-dictionary-path=file:\$modelsDir\\korean_dict.txt"
                        
                        Start-Process -FilePath "java" -ArgumentList \$arguments -WorkingDirectory "${env.DEPLOY_DIR}" -RedirectStandardOutput "${env.DEPLOY_DIR}\\spring-server.log" -RedirectStandardError "${env.DEPLOY_DIR}\\spring-server-error.log" -WindowStyle Hidden
                    """
                }
            }
        }
    }
}
