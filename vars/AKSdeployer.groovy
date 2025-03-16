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
                    deploy(environment, finalImage, finalTag)
                    resourceQuota("my-quota", environment)
                    // blueGreenDeployment("my-app-release-${environment}", environment)
                } else {
                    error "❌ Image not found in the registry. Deployment to PROD is not allowed!"
                }
            } else if (nonProdEnv.contains(environment)) {
                if (imageExists) {
                    echo "✅ Image exists. Deploying existing image to ${environment}."
                    deploy(environment, finalImage, finalTag)
                    sleep(time: 30, unit: 'SECONDS')                                                       
                } else {
                    echo "🚀 Image not found. Proceeding with alternative flow..."
                }
            } else {
                error "❌ Invalid environment: ${environment}"
            }
            // Trigger rollback if enabled
                        
        
        }
    }
}

def deploy(String environment, String image, String tag) {
    try {
        echo "✅ Image exists. Deploying to ${environment}..."
        sh """
            export KUBECONFIG=\$KUBECONFIG
            kubectl config current-context
            kubectl config get-contexts
            helm upgrade --install my-app-release-${environment} myrelease \
                --set image.repository=${image} \
                --set image.tag=${tag} \
                --set namespace=${environment} \
                --namespace=${environment}
        """
        resourceQuota("my-quota", environment)
        def releaseName = "my-app-release-${environment}-myrelease"
        // sh "kubectl  get pod  -n dev"
        blueGreenDeployment(releaseName, environment)
        // blueGreenDeployment("my-app-release-${environment}", environment)
    } catch (Exception e) {
        echo "❌ Deployment failed for ${environment}. Rolling back..."
        // rollbackHelm(environment)
        error "❌ Deployment failed: ${e.message}"
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
