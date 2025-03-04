@Library('my-shared-library') _

pipeline {

    agent { label 'azure'}
    environment {
        DOCKER_IMAGE = 'myapp'
        DOCKER_HOST = "unix:///var/run/docker.sock"
        PATH = "/opt/homebrew/bin:/usr/local/bin:$PATH"
        GIT_COMMIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        ENVIRONMENT = 'dev'

    }
    stages {
        stage('Checkout code'){
            steps {
                echo "hello world"
            }

        }
        stage('Build and Tag Docker Image'){
            steps {
                script {
                    def dockerTag = "${env.DOCKER_IMAGE}:${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}"
                    buildAndTagImage(dockerTag)



                } 


            }



        }




    }




}
post {

    success {
        sh "docker rmi -f ${env.DOCKER_IMAGE}:${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}"
    }
    failure {
        echo "build failed"

    }

}
