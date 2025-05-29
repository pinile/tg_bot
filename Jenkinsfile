pipeline {
  agent any

  tools {
      maven 'Maven3'
    }

  parameters {
      string(
        name: 'TARGET_BRANCH',
        defaultValue: 'main',
        description: 'Ветка, из которой собирать и деплоить.'
      )
      booleanParam(
        name: 'MANUAL_DEPLOY',
        defaultValue: false,
        description: 'Триггер для ручного деплоя.'
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
        checkout([$class: 'GitSCM',
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

    stage('Deploy') {
      when { branch 'master' }
      steps {
        sh './scripts/deploy.sh'
      }
    }

    stage('Auto-Deploy') {
          when { expression { params.TARGET_BRANCH == 'main' } }
          steps {
            sh './scripts/deploy.sh'
          }
        }

    stage('Manual Deploy') {
      when {
        expression { params.MANUAL_DEPLOY }
        }
        steps {
        script {
                  try {
                    input message: "Deploy bot from ${params.TARGET_BRANCH}?"
                    sh './scripts/deploy.sh'
                  } catch (err) {
                    echo "Manual deploy aborted."
                    currentBuild.result = 'ABORTED'
                  }
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
