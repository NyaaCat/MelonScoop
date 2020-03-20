pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean install'
            }
        }
    }

    post {
           always {
               archiveArtifacts artifacts: 'target/MelonScoop-*.jar', fingerprint: true
               cleanWs()
           }
    }
}