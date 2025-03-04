@Library('my-shared-library') _

pipeline {

    agent { label 'azure'}
    environment {
        DOCKER_IMAGE = 'myapp'
        GIT_COMMIT_SHA = sh(script: 'git rev-parse --short HEAD' , returnStdout: true).trim())
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
                    def dockerTag = "${DOCKER_IMAGE}:${ENVIRONMENT}-${GIT_COMMIT_SHA}"
                    buildAndTagImage(dockerTag)


                } 


            }



        }




    }




}
