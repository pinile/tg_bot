pipeline {
  agent any
  tools {
      maven 'Maven3'
    }

  environment {
    BOT_TOKEN = credentials('BOT_TOKEN')
    MONGO_DATABASE_NAME = credentials('MONGO_DATABASE_NAME')
    MONGO_ROOT_USERNAME = credentials('MONGO_ROOT_USERNAME')
    MONGO_ROOT_PASSWORD = credentials('MONGO_ROOT_PASSWORD')
  }

  parameters {
    booleanParam(name: 'MANUAL_DEPLOY', defaultValue: false, description: 'Trigger manual deploy stage')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
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

    stage('Manual Deploy') {
      when {
        anyOf {
          branch 'develop'
          expression { return params.MANUAL_DEPLOY == true }
        }
      }
      steps {
        script {
          try {
            input message: "Deploy bot from ${env.BRANCH_NAME}?"
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
    failure {
           subject: "Build failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
           body: "See ${env.BUILD_URL}"
    }
  }
}
