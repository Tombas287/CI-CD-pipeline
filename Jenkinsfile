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

        stage('Build and Tag Docker Image') {
            steps {
                script {
                    def dockerTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}"
                    sh "docker build -t ${dockerTag} ."
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
