import groovy.json.JsonSlurper

def call(String environment, String credentials, String dockerImage, String imageTag, String pipeline) {
    if (!environment || !credentials || !pipeline) {
        error "❌ Missing required parameters!"
    }

    withCredentials([file(credentialsId: credentials, variable: 'KUBECONFIG')]) {
        script {
            echo "✅ Setting KUBECONFIG..."
            sh """
            export KUBECONFIG=\$KUBECONFIG
            kubectl config current-context
            kubectl config get-contexts
            helm version
            """

            // Parse pipeline JSON file
            def configFile = readFile(pipeline)
            def jsonslurper = new JsonSlurper()
            def jsonObj = jsonslurper.parseText(configFile)

            // Determine final image and tag
            def finalImage = dockerImage ?: jsonObj.imageName
            def finalTag = imageTag ?: jsonObj.imageTag
            println(finalImage)
            println(finalTag)

            // Check if the image exists in the registry
            def imageExists = imageExist(finalImage, finalTag)

            def nonProdEnv = ["dev", "preprod", "qa"]

            if (environment == "prod") {
                if (imageExists) {
                    echo "✅ Image exists. Deploying to PROD..."
                    sh """
                        helm install my-release-${environment} myrelease \
                            --set image.repository=${finalImage} \
                            --set image.tag=${finalTag}
                    """
                    resourceQuota("my-quota", environment)
                } else {
                    error "❌ Image not found in the registry. Deployment to PROD is not allowed!"
                }
            } else if (nonProdEnv.contains(environment)) {
                if (imageExists) {
                    echo "✅ Image exists. Deploying to ${environment}..."
                    sh """
                        helm upgrade --install my-app-release-${environment} myrelease \
                            --set image.repository=${finalImage} \
                            --set image.tag=${finalTag}
                    """
                    resourceQuota("my-quota", environment)
                } else {
                    echo "🚀 Image not found. Proceeding with a new build or alternative flow..."
                }
            } else {
                error "❌ Invalid environment: ${environment}"
            }
        }
    }
}
