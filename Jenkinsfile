pipeline {
  agent any
  
  environment {
    JAVA_HOME = tool 'JDK-21'
    MAVEN_HOME = tool 'Maven-3.9'
    PATH = "${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"
    DOCKER_REGISTRY = 'your-registry.company.com'
    IMAGE_NAME = 'transaction-platform/ingest'
  }
  
  tools {
    jdk 'JDK-21'
    maven 'Maven-3.9'
  }
  
  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.BUILD_VERSION = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"
        }
      }
    }
    
    stage('Code Quality & Security') {
      parallel {
        stage('SonarQube Analysis') {
          steps {
            withSonarQubeEnv('SonarQube') {
              sh './mvnw sonar:sonar -Dsonar.projectKey=transaction-platform'
            }
          }
        }
        
        stage('OWASP Dependency Check') {
          steps {
            sh './mvnw org.owasp:dependency-check-maven:check'
            publishHTML([
              allowMissing: true,
              alwaysLinkToLastBuild: true,
              keepAll: true,
              reportDir: 'ingest-service/target',
              reportFiles: 'dependency-check-report.html',
              reportName: 'OWASP Dependency Check'
            ])
          }
        }
      }
    }
    
    stage('Build & Test') {
      steps {
        sh './mvnw clean compile'
        sh './mvnw test'
      }
      post {
        always {
          junit 'ingest-service/target/surefire-reports/*.xml'
          jacoco(
            execPattern: 'ingest-service/target/jacoco.exec',
            classPattern: 'ingest-service/target/classes',
            sourcePattern: 'ingest-service/src/main/java'
          )
        }
      }
    }
    
    stage('Integration Tests') {
      steps {
        sh '''
          docker-compose -f docker-compose.test.yml up -d
          sleep 30
          ./mvnw verify -Pintegration-test
        '''
      }
      post {
        always {
          sh 'docker-compose -f docker-compose.test.yml down -v'
        }
      }
    }
    
    stage('Package') {
      steps {
        sh './mvnw package -DskipTests'
        archiveArtifacts artifacts: 'ingest-service/target/*.jar', fingerprint: true
      }
    }
    
    stage('Docker Build & Push') {
      when {
        anyOf {
          branch 'main'
          branch 'develop'
          branch 'transaction-platform'
        }
      }
      steps {
        script {
          def image = docker.build("${IMAGE_NAME}:${BUILD_VERSION}", "./ingest-service")
          docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-registry-credentials') {
            image.push()
            image.push('latest')
          }
        }
      }
    }
    
    stage('Deploy to Dev') {
      when { branch 'develop' }
      steps {
        sh """
          helm upgrade --install transaction-platform-dev ./chart \\
            --namespace transaction-platform-dev \\
            --set image.tag=${BUILD_VERSION} \\
            --set environment=dev \\
            -f chart/values-dev.yaml
        """
      }
    }
    
    stage('Deploy to Staging') {
      when { 
        anyOf {
          branch 'main'
          branch 'transaction-platform'
        }
      }
      steps {
        sh """
          helm upgrade --install transaction-platform-staging ./chart \\
            --namespace transaction-platform-staging \\
            --set image.tag=${BUILD_VERSION} \\
            --set environment=staging \\
            -f chart/values-staging.yaml
        """
      }
    }
    
    stage('Production Deployment Approval') {
      when { branch 'main' }
      steps {
        input message: 'Deploy to Production?', ok: 'Deploy',
              submitterParameter: 'APPROVER'
      }
    }
    
    stage('Deploy to Production') {
      when { branch 'main' }
      steps {
        sh """
          helm upgrade --install transaction-platform-prod ./chart \\
            --namespace transaction-platform-prod \\
            --set image.tag=${BUILD_VERSION} \\
            --set environment=prod \\
            -f chart/values-prod.yaml
        """
      }
    }
  }
  
  post {
    always {
      publishHTML([
        allowMissing: false,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: 'ingest-service/target/site/jacoco',
        reportFiles: 'index.html',
        reportName: 'JaCoCo Coverage Report'
      ])
    }
    success {
      slackSend(
        channel: '#transaction-platform-builds',
        color: 'good',
        message: ":white_check_mark: Transaction Platform build ${BUILD_VERSION} succeeded on ${BRANCH_NAME}"
      )
    }
    failure {
      slackSend(
        channel: '#transaction-platform-builds',
        color: 'danger',
        message: ":x: Transaction Platform build ${BUILD_VERSION} failed on ${BRANCH_NAME}"
      )
    }
  }
} 