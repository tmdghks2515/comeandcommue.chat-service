pipeline {
  agent any
  environment {
    REGISTRY    = '192.168.219.113:5000'  // A서버(사설 레지스트리)
    DEPLOY_HOST = '192.168.219.145'       // B서버
    DEPLOY_DIR  = '/srv/apps/daneyo'
    SERVICE     = 'chat-service'      // 각 서비스마다 변경
    IMAGE_TAG   = "${env.BUILD_NUMBER}"
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Docker Build & Push') {
      steps {
        script {
          def image = "${REGISTRY}/${SERVICE}:${IMAGE_TAG}"
          def latest = "${REGISTRY}/${SERVICE}:latest"

          withCredentials([string(credentialsId: 'GITHUB_PKG_TOKEN', variable: 'GPR_KEY')]) {
            sh """
              DOCKER_BUILDKIT=0 docker build \
              --build-arg GITHUB_ACTOR=tmdghks2515 \
              --build-arg GITHUB_TOKEN=${GPR_KEY} \
                -t ${image} -t ${latest} .
              docker push ${image}
              docker push ${latest}
            """
          }
        }
      }
    }

    stage('Deploy') {
      steps {
        withCredentials([sshUserPrivateKey(
          credentialsId: 'anan-server-ssh',
          keyFileVariable: 'SSH_KEY',
          usernameVariable: 'SSH_USER'
        )]) {
          sh(script: """
            ssh -i "\$SSH_KEY" -o StrictHostKeyChecking=no \$SSH_USER@${DEPLOY_HOST} '
              set -e
              cd ${DEPLOY_DIR}
              export IMAGE_TAG=${IMAGE_TAG}
              # .env 로드 (compose가 --env-file 못 쓸 때)
              set -a; [ -f .env ] && . ./.env; set +a

              docker compose pull ${SERVICE}
              docker compose up -d --no-deps ${SERVICE}
            '
          """)
        }
      }
    }
  }

  post {
    success { sh 'docker image prune -f' }
  }
}
