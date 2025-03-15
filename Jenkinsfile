@Library('my-shared-library') _

pipeline {
    agent any
    // agent {
    //      docker {
    //   image 'abhishekf5/maven-abhishek-docker-agent:v1'
    //   args '--user root -v /var/run/docker.sock:/var/run/docker.sock' // mount Docker socket to access the host's Docker daemon
    // }     
// }

    environment {
        DOCKER_IMAGE = 'myapp'
        DOCKER_HOST = "unix:///var/run/docker.sock"
        PATH = "/opt/homebrew/bin:/usr/local/bin:$PATH"
        USERNAME = "7002370412"
        ENVIRONMENT = 'dev'   
        PIPELINE_FILE = "${env.WORKSPACE}/pipeline.json"
        // GIT_COMMIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        GIT_COMMIT_SHA = "af37813"

    }
stages {
    // stage('Sample text') {
    //         steps {
    //             script {
    //                def jsonFilePath = "pipeline.json"
    //                sampleText(jsonFilePath)

                    
    //                 // def dockerDetails = sampleText.getDockerDetails(jsonFilePath)
    //                 // echo "Docker Image: ${dockerDetails.image}"
    //                 // echo "Docker Tag: ${dockerDetails.tag}"
    //             }
    //         }
    //     }

       // stage('Set Commit SHA') {
        //     steps {
        //         script {
        //             env.GIT_COMMIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        //         }
       //      }
      //   }
         // stage('Pip Builder'){
         //     steps {
         //         script {
         //            pipBuilder( pythonVersion: 'python3',
         //            requirementsFile: 'requirements.txt',
         //             outputDir: 'build_output/',
         //             venvDir: 'venv'
         //            )
         //        }
         //    }
         // }

        // stage('SonarQube Analysis') {
        //     steps {
        //         script {
        //             sonarScan(projectKey: 'my_local_project', sonarHost: 'http://host.docker.internal:9000')
        //         }
        //     }
        // }
        // stage('check if image exist'){

        //     steps {

        //         script {
        //             def dockerImage = "${env.USERNAME}/${env.DOCKER_IMAGE}"
        //             def imageTag = "${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}"
        //             // def dockerImage = "7002370412/nginx"
        //             // def imageTag = "latest"

        //             def exist = imageExist(dockerImage, imageTag)
        //             if (exist) {
        //                 echo "Skipping build because image already exists."
        //                 currentBuild.result = 'SUCCESS'
        //                 return
        //             } else {
        //                 echo "No existing image found. Proceeding with build."
        //             }


        //             }

        //         }


        //     }

        // stage('Build and Tag Docker Image') {
        //     when { expression { currentBuild.result == null } }
        //      steps {
        //          script {
        //              def dockerTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.GIT_COMMIT_SHA}"
        //              buildAndTagImage(dockerTag)
        //          }
        //      }
        //  }

         // stage('Image scan'){
         //     steps {
         //    script {
         //        def imageTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.GIT_COMMIT_SHA}"
         //         imageScan(imageTag)
         //     }
         //   }
         // }
        // stage('Docker push to registry'){
        //     when { expression { currentBuild.result == null } }
        //     steps {
        //         script {
        //             def dockerTag = "${env.USERNAME}/${env.DOCKER_IMAGE}:${env.GIT_COMMIT_SHA}"
        //             dockerPush(dockerTag)
        //         }
        //     }
        // }
        stage('Aks deployer Dev') {
            steps {
                script {

                    def dockerImage = ""
                    def imageTag = ""
                    def PIPELINE_FILE = "${env.PIPELINE_FILE}"
                    AKSdeployer('dev', 'credentials',dockerImage, imageTag, PIPELINE_FILE)
                }
            }
        }
        stage('Aks deployer qa') {
            steps {
                script {

                    def dockerImage = ""
                    def imageTag = ""
                    def PIPELINE_FILE = "${env.PIPELINE_FILE}"
                    AKSdeployer('qa', 'credentials',dockerImage, imageTag,PIPELINE_FILE)
                }
            }
        }
        // stage('Aks deployer preprod') {
        //     steps {
        //         script {

        //             def dockerImage = "${env.USERNAME}/${env.DOCKER_IMAGE}"
        //             def imageTag = "bfca98a"
        //             AKSdeployer('preprod', 'credentials',dockerImage, imageTag )
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
                               def dockerImage = ""
                               def imageTag = ""
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
            echo "Job success"
            emailext (
                to: 'mintu2831@gmail.com',
                subject: "✅ Build Successful",
                body: "Job: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}"
            )
        }
        failure {
            echo "Build failed"
            emailext (
                to: 'mintu2831@gmail.com',
                subject: "Jenkins Pipeline Failed! 🔥",
                body: "Job: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nStatus: ❌ Failed"
            )
        }
    }
}
