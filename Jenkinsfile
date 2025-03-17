@Library('my-shared-library') _

pipeline {
    agent any
    environment {
        DOCKER_IMAGE = 'myapp'
        DOCKER_HOST = "unix:///var/run/docker.sock"
        PATH = "/opt/homebrew/bin:/usr/local/bin:$PATH"
        USERNAME = "7002370412"
        ENVIRONMENT = 'dev'   
        PIPELINE_FILE = "${env.WORKSPACE}/pipeline.json"
        GIT_COMMIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        // GIT_COMMIT_SHA = "af37813"

    }
stages {
         stage('Checkout') {
            steps {
                checkout scm
            }
         stage('Pip Builder'){
             steps {
                 script {
                    pipBuilder( pythonVersion: 'python3',
                    requirementsFile: 'requirements.txt',
                     outputDir: 'build_output/',
                     venvDir: 'venv'
                    )                   
                }
            }
         }
         stage('SonarQube Analysis') {
            steps {
                script {
                    sonarScan(projectKey: 'my_local_project', sonarHost: 'http://host.docker.internal:9000')
                }
            }
        }
        stage('Build and Tag Docker Image') {
            when { expression { currentBuild.result == null } }
             steps {
                 script {
                     def dockerTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.GIT_COMMIT_SHA}"
                     buildAndTagImage(dockerTag)
                 }
             }
         }

         stage('Image scan'){
             steps {
            script {
                def imageTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.GIT_COMMIT_SHA}"
                 imageScan(imageTag)
             }
           }
         }
        stage('Docker push to registry'){
            when { expression { currentBuild.result == null } }
            steps {
                script {
                    def dockerTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.GIT_COMMIT_SHA}"
                    dockerPush(dockerTag)
                }
            }
        }
        stage('Aks deployer Dev') {
            steps {
                script {

                    def dockerImage = "${env.USERNAME}/${env.DOCKER_IMAGE}"
                    def imageTag = "${env.GIT_COMMIT_SHA}"
                    def PIPELINE_FILE = "${env.PIPELINE_FILE}"
                    AKSdeployer('dev', 'credentials',dockerImage, imageTag, PIPELINE_FILE)
                }
            }
        }
        // stage('Aks deployer qa') {
        //     steps {
        //         script {

        //             def dockerImage = ""
        //             def imageTag = ""
        //             def PIPELINE_FILE = "${env.PIPELINE_FILE}"
        //             AKSdeployer('qa', 'credentials',dockerImage, imageTag,PIPELINE_FILE)
        //         }
        //     }
        // }
        // stage('Aks deployer preprod') {
        //     steps {
        //         script {

        //             def dockerImage = ""
        //             def imageTag = ""
        //             def PIPELINE_FILE = "${env.PIPELINE_FILE}"
        //             AKSdeployer('preprod', 'credentials',dockerImage, imageTag,PIPELINE_FILE)
        //         }
        //     }
        // }
        stage('Aks deployer prod') {
            steps {
                script {
                       def userInput = input(
                       message: 'proceed with prod deployment?',
                       parameters : [booleanParam(name: 'Confirm', defaultValue: false, description: 'Yes proceed')]
                       )

                       if (userInput) {
                               def dockerImage = "${env.USERNAME}/${env.DOCKER_IMAGE}"
                               def imageTag = "${env.GIT_COMMIT_SHA}"
                               def PIPELINE_FILE = "${env.PIPELINE_FILE}"
                               AKSdeployer('prod', 'credentials',dockerImage, imageTag, PIPELINE_FILE)
                       }
                       else {
                         error("Deployment aborted by user.")
                      }
               }
           }
        }

    }

    post {
        success {
            script {
                sh "docker rmi -f ${env.USERNAME}/${env.DOCKER_IMAGE}:${env.GIT_COMMIT_SHA}"                
            }
            cleanWs()

            echo "Job success"
            // emailext (
            //     to: 'mintu2831@gmail.com',
            //     subject: "✅ Build Successful",
            //     body: "Job: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}"
            // )
        }
//         failure {
//              echo "Build failed"
//             // emailext (
//             //     to: 'mintu2831@gmail.com',
//             //     subject: "Jenkins Pipeline Failed! 🔥",
//             //     body: "Job: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nStatus: ❌ Failed"
//             // )
//         }
    }
}
