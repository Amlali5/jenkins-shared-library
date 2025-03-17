def call(Map config) {
    pipeline {
        agent any
        stages {
            stage('Clone Repository') {
                steps {
                    checkout scm
                }
            }
            stage('Build Docker Image') {
                steps {
                    sh "docker build -t ${config.imageName} ."
                }
            }
            stage('Push Docker Image') {
                steps {
                    sh "docker push ${config.imageName}"
                }
            }
            stage('Remove Local Docker Image') {
                steps {
                    sh "docker rmi ${config.imageName}"
                }
            }
            stage('Deploy to Kubernetes') {
                steps {
                    sh "kubectl apply -f ${config.deploymentFile}"
                }
            }
        }
    }
}
