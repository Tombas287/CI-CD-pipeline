import groovy.json.JsonSlurperClassic

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
            def releaseName = "my-app-release-${environment}-myrelease"
            def sample = sh "kubectl get deployment ${releaseName} -n ${environment} -o=jsonpath='{.spec.replicas}'"
            echo "sample: ${sample}"


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
                    // resourceQuota("my-quota", environment)
                    def releaseName = "my-app-release-${environment}-myrelease"
                    blueGreenDeployment.deploymentScale(releaseName, environment, pipeline)                    
                    // // blueGreenDeployment(releaseName, environment, pipeline)
                    // deploymentScale(releaseName, environment, pipeline)
                } else {
                    error "❌ Image not found in the registry. Deployment to PROD is not allowed!"
                }
            } else if (nonProdEnv.contains(environment)) {
                if (imageExists) {
                    echo "✅ Image exists. Deploying existing image to ${environment}."
                    deploy(environment, finalImage, finalTag)
                    def releaseName = "my-app-release-${environment}-myrelease"
                    blueGreenDeployment.deploymentScale(releaseName, environment, pipeline)  
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
                    // blueGreenDeployment(releaseName, environment, pipeline)
                
        // sh "kubectl  get pod  -n dev"
        
        // blueGreenDeployment("my-app-release-${environment}", environment)
    } catch (Exception e) {
        echo "❌ Deployment failed for ${environment}. Rolling back..."
        // rollbackHelm(environment)
        error "❌ Deployment failed: ${e.message}"
    }
}


def fetchImage(String pipeline) {
    def configFile = readFile(pipeline).trim()
    def jsonSlurper = new JsonSlurperClassic()
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

// import groovy.json.JsonSlurper

// def call(String releaseName, String namespace, String pipeline) {
//     deploymentScale(releaseName, namespace, pipeline)
// }

def deploymentScale(String releaseName, String namespace, String pipeline) {
    try {
        def jsonContent = readFile(pipeline).trim()
        def jsonData = new JsonSlurperClassic().parseText(jsonContent)

        def scaleUpEnabled = jsonData?.scale_up?.enabled ?: false
        def scaleDownEnabled = jsonData?.scale_down?.enabled ?: false
        def minReplicas = jsonData?.min_replicas ?: 1
        def maxReplicas = jsonData?.scale_up?.max_replicas ?: 3

        if (scaleUpEnabled && scaleDownEnabled) {
            error "❌ Both scale-up and scale-down are enabled. Aborting!"
        }

        // def currentReplicas = sh(
        //     script: "kubectl get deployment ${releaseName} -n ${namespace} -o jsonpath='{.spec.replicas}'",
        //     returnStdout: true
        // ).trim()
        def currentReplicas = sh(
            script: "kubectl get deployment ${releaseName} -n ${namespace} -o json | jq -r '.spec.replicas'",
            returnStdout: true
        ).trim()

        echo "Current Replicas: ${currentReplicas}"

        currentReplicas = currentReplicas.toInteger()
        def newReplicas = currentReplicas

        if (scaleUpEnabled && currentReplicas < maxReplicas) {
            newReplicas = currentReplicas + 1
            echo "Scaling up to ${newReplicas} replicas..."
        } else if (scaleDownEnabled && currentReplicas > minReplicas) {
            newReplicas = currentReplicas - 1
            echo "Scaling down to ${newReplicas} replicas..."
        } else {
            echo "No scaling action needed."
            return
        }

        sh "kubectl scale deployment ${releaseName} -n ${namespace} --replicas=${newReplicas}"

    } catch (Exception e) {
        error "❌ Error in scaling: ${e.getMessage()}"
    }
}
