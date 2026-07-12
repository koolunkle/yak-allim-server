pipeline {
    agent any
    
    environment {
        DEPLOY_DIR = 'C:\\yakallim-deploy'
        PORT = '8080'
        // 빌드 완료 후에도 백그라운드 프로세스가 유지되도록 설정
        JENKINS_NODE_COOKIE = 'dontKillMe'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                bat 'gradlew.bat clean bootJar'
            }
        }
        
        stage('Deploy') {
            steps {
                powershell """
                    # 배포 디렉터리 생성
                    if (!(Test-Path -Path "${env.DEPLOY_DIR}")) {
                        New-Item -ItemType Directory -Force -Path "${env.DEPLOY_DIR}"
                    }
                    
                    # 기존 실행 중인 포트 프로세스 종료
                    \$portConn = Get-NetTCPConnection -LocalPort ${env.PORT} -ErrorAction SilentlyContinue
                    if (\$portConn) {
                        \$pidToKill = \$portConn.OwningProcess | Select-Object -Unique
                        Stop-Process -Id \$pidToKill -Force
                        Start-Sleep -Seconds 3
                    }
                    
                    # 신규 빌드된 JAR 파일 배포 디렉터리로 복사
                    \$jarFile = Get-ChildItem -Path "build\\libs\\*.jar" | Select-Object -First 1
                    if (\$jarFile) {
                        Copy-Item -Path \$jarFile.FullName -Destination "${env.DEPLOY_DIR}\\server.jar" -Force
                    } else {
                        throw "JAR 파일이 존재하지 않습니다."
                    }
                    
                    # 애플리케이션 백그라운드 구동 및 로그 리다이렉트
                    Start-Process -FilePath "java" -ArgumentList "-jar ${env.DEPLOY_DIR}\\server.jar --server.port=${env.PORT}" -WorkingDirectory "${env.DEPLOY_DIR}" -RedirectStandardOutput "${env.DEPLOY_DIR}\\spring-server.log" -RedirectStandardError "${env.DEPLOY_DIR}\\spring-server-error.log" -WindowStyle Hidden
                """
            }
        }
    }
}
