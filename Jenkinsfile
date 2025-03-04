@Library('my-shared-library') _

pipeline {
    agent { label 'azure' }

    environment {
        DOCKER_IMAGE = 'myapp'
        DOCKER_HOST = "unix:///var/run/docker.sock"
        PATH = "/opt/homebrew/bin:/usr/local/bin:$PATH"
        USERNAME = "7002370412"
        ENVIRONMENT = 'dev'
    }

    stages {
        stage('Checkout Code') {
            steps {
                echo "Hello, world!"
            }
        }

        stage('Set Commit SHA') {
            steps {
                script {
                    env.GIT_COMMIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                }
            }
        }
        stage('check if image exist'){

            steps {

                script {
                    def dockerImage = "${env.USERNAME}/${env.DOCKER_IMAGE}"
                    def dockerTag = "${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}"

                    def exist = imageExist(dockerImage, imageTag)
                    if (exists) {
                        echo "Skipping build because image already exists."
                        currentBuild.result = 'SUCCESS'
                        return
                    } else {
                        echo "No existing image found. Proceeding with build."
                    }


                    }

                }


            }

        }

        stage('Build and Tag Docker Image') {
            steps {
                script {
                    def dockerTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}"
                    buildAndTagImage(dockerTag)
                }
            }
        
        }
        stage('Docker push to registry'){

            steps {
                script {
                    def dockerTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}"
                    dockerPush(dockerTag)
                }


            }


        }



    }

    post {
        success {
            script {
                sh "docker rmi -f ${env.DOCKER_IMAGE}:${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}"
            }
        }
        failure {
            echo "Build failed"
        }
    }
}
