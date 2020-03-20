pipeline {
    agent any
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