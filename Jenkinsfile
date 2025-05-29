pipeline {
  agent any

  tools {
    maven 'Maven3'
  }

  parameters {
    string(name: 'TARGET_BRANCH', defaultValue: 'main', description: 'Ветка для сборки.')
    booleanParam(name: 'MANUAL_DEPLOY', defaultValue: false, description: 'Ручной деплой.')
    choice(
      name: 'DEPLOY_MODE',
      choices: ['partial', 'full'],
      description: 'Выберите режим деплоя: partial — только bot, full — весь стек.'
    )
  }

  environment {
    BOT_TOKEN = credentials('BOT_TOKEN')
    MONGO_DATABASE_NAME = credentials('MONGO_DATABASE_NAME')
    MONGO_ROOT_USERNAME = credentials('MONGO_ROOT_USERNAME')
    MONGO_ROOT_PASSWORD = credentials('MONGO_ROOT_PASSWORD')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout([
          $class: 'GitSCM',
          branches: [[name: "*/${params.TARGET_BRANCH}"]],
          userRemoteConfigs: scm.userRemoteConfigs
        ])
      }
    }

    stage('Build') {
      steps {
        sh './scripts/build.sh'
      }
    }

    stage('Test') {
      steps {
        sh './scripts/test.sh'
      }
    }

    stage('Deploy (auto/manual)') {
      when {
        anyOf {
          expression { params.TARGET_BRANCH == 'main' }
          expression { params.MANUAL_DEPLOY }
        }
      }
      steps {
        script {
          if (params.MANUAL_DEPLOY) {
            try {
              input message: "Deploy (${params.DEPLOY_MODE}) from branch: ${params.TARGET_BRANCH}?"
            } catch (err) {
              echo "Manual deploy aborted."
              currentBuild.result = 'ABORTED'
              return
            }
          }

          sh "DEPLOY_MODE=${params.DEPLOY_MODE} ./scripts/deploy.sh"
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: '**/target/*.jar', onlyIfSuccessful: true
      junit '**/target/surefire-reports/*.xml'
    }
  }
}
