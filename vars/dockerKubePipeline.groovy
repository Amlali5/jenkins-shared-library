def call(Map config) {
    pipeline {
        agent any
        stages {
            stage('Clone Repository') {
                steps {
                    checkout([$class: 'GitSCM', branches: [[name: config.branch]],
                              userRemoteConfigs: [[url: config.repositoryUrl, credentialsId: config.gitCredentialsId]]])
                }
            }
            stage('Build Docker Image') {
                steps {
                    sh "docker build -t ${config.dockerImageName}:${config.dockerImageTag} ."
                }
            }
            stage('Push Docker Image') {
                steps {
                    withCredentials([usernamePassword(credentialsId: config.dockerHubCredentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"
                        sh "docker push ${config.dockerImageName}:${config.dockerImageTag}"
                    }
                }
            }
            stage('Remove Local Docker Image') {
                steps {
                    sh "docker rmi ${config.dockerImageName}:${config.dockerImageTag}"
                }
            }
            stage('Deploy to Kubernetes') {
                steps {
                    withCredentials([string(credentialsId: config.k8sTokenCredentialsId, variable: 'K8S_TOKEN')]) {
                        sh "kubectl --token=$K8S_TOKEN apply -f ${config.kubeDeploymentFile}"
                    }
                }
            }
        }
    }
}

