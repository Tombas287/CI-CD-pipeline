import groovy.json.JsonSlurper

def call(String environment, String credentials, String dockerImage, String imageTag, String pipeline) {
    if (!pipeline) {
        error "❌ Missing 'pipeline' argument"
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

            // Fetch image details from the JSON pipeline file
            def fetchedImage = fetchImage(pipeline)
            def finalImage = dockerImage ?: fetchedImage.finalImage
            def finalTag = imageTag ?: fetchedImage.finalTag

            // Check if the image exists
            def imageExists = imageExist(finalImage, finalTag)

            def nonProdEnv = ["dev", "preprod", "qa"]

            if (environment == "prod") {
                if (imageExists) {
                    echo "✅ Image exists. Deploying to ${environment}..."
                    sh """
                        helm upgrade --install my-release-${environment} myrelease \
                            --set image.repository=${finalImage} \
                            --set image.tag=${finalTag} \
                            --namespace=${environment}
                    """
                    resourceQuota("my-quota", environment)
                } else {
                    error "❌ Image not found in the registry. Deployment to PROD is not allowed!"
                }
            } else if (nonProdEnv.contains(environment)) {
                if (imageExists) {
                    echo "✅ Image exists. Deploying existing image to ${environment}."
                    sh """
                        helm upgrade --install my-app-release-${environment} myrelease \
                            --set image.repository=${finalImage} \
                            --set image.tag=${finalTag} \
                            --namespace=${environment}
                    """
                    resourceQuota("my-quota", environment)
                } else {
                    echo "🚀 Image not found. Proceeding with alternative flow..."
                }
            } else {
                error "❌ Invalid environment: ${environment}"
            }
        }
    }
}

def fetchImage(String pipeline) {
    def configFile = readFile(pipeline).trim()
    def jsonSlurper = new JsonSlurper()
    def jsonObj

    try {
        jsonObj = jsonSlurper.parseText(configFile)
    } catch (Exception e) {
        error "❌ Failed to parse JSON: ${e.message}"
    }

    def finalImage = jsonObj?.imageName?.trim()
    def finalTag = jsonObj?.imageTag?.trim()

    if (finalImage && finalTag) {
        return [finalImage: finalImage, finalTag: finalTag]
    } else {
        echo "⚠️ Missing 'imageName' or 'imageTag' in the JSON file."
        return [:]
    }
}
