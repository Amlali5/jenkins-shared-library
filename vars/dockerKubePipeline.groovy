def call(Map config) {
    pipeline {
        agent any
        stages {
            stage('Clone Repository') {
                steps {
                    checkout([$class: 'GitSCM', 
                              branches: [[name: config.branch]],
                              userRemoteConfigs: [[url: config.repositoryUrl, credentialsId: config.gitCredentialsId]]])
                }
            }
            stage('Verify Files') { 
                steps {
                    echo 'Checking if static_website directory exists...'
                    sh 'ls -la jenkins/lab3 || echo "Directory not found!"'
                }
            }
            stage('Prepare Build Context') { 
                steps {
                    echo 'Preparing build context...'
                    sh '''
                    if [ ! -d "jenkins/lab3/static_website" ]; then
                        mkdir -p jenkins/lab3/static_website
                        cp -r /path/to/static_website/* jenkins/lab3/static_website/
                    else
                        echo "static_website directory already exists."
                    fi
                    '''
                }
            }
            stage('Build Docker Image') {
                steps {
                    sh "docker build -t ${config.dockerImageName}:${config.dockerImageTag} -f ${config.dockerFilePath} ."
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
                    withCredentials([file(credentialsId: 'minikube-kubeconfig', variable: 'KUBECONFIG')]) {
                        sh '''
                        export KUBECONFIG=$KUBECONFIG
                        kubectl apply -f ${config.kubeDeploymentFile}
                        '''
                    }
                }
            }
        }
    }
}






