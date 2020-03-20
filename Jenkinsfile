pipeline {
    agent any
    tools {
            maven 'Maven 3.3.9'
            jdk 'jdk8'
    }
    stages {
        stage('Build') {
            steps {
                sh 'maven clean install'
            }
        }
    }
    post {
           always {
               archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
               cleanWs()
           }
    }
}