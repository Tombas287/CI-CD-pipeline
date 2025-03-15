import groovy.json.JsonSlurperClassic

def call(String environment, String credentials, String dockerImage , String imageTag, String pipeline) {
    withCredentials([file(credentialsId: credentials, variable: 'KUBECONFIG')]) {
        script {
            echo "✅ Setting KUBECONFIG..."
            sh """
            export KUBECONFIG=\$KUBECONFIG
            kubectl config current-context
            kubectl config get-contexts
            helm version
            """
            // Check if image exists
            def configFile = readFile(pipeline)
            def jsonslurper = new JsonSlurperClassic()
            def jsonObj = jsonslurper.parseText(configFile)
            def finalImage = dockerImage ?: jsonObj.imageName
            def finalTag = imageTag ?: jsonObj.imageTag
            def imageExists = imageExist(finalImage, finalTag)


            def nonProdEnv = ["dev", "preprod", "qa"]
            if (environment == "prod") {
                if (imageExists) {
                    echo "✅ Image exists.deploying to ${environment}"

                    sh """
                        helm install my-release-${environment} myrelease \
                            --set image.repository=${finalImage} \
                            --set image.tag=${finalTag}
                    """
                    resourceQuota("my-quota", "default")
                }
               else {
                  error "❌ Image not found in the registry. Deployment to PROD is not allowed!"

               }
            } else if (nonProdEnv.contains(environment)) {
                if (imageExists){
                echo "✅ Image exists. Deploying existing image to ${environment}."
                sh """
                    helm upgrade --install my-app-release-${environment} myrelease \
                        --set image.repository=${finalImage} \
                        --set image.tag=${finalTag}
           """
           resourceQuota("my-quota", environment)
            } else {
                echo "🚀 Image not found..."
                
               }

            }

        }
    }
}
