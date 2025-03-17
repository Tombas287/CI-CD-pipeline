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
            def currentReplicas = getReplicas(releaseName, environment)
            echo "Current Replicas: ${currentReplicas}"

            def fetchedImage = fetchImage(pipeline)
            def finalImage = dockerImage ?: fetchedImage.finalImage
            def finalTag = imageTag ?: fetchedImage.finalTag
            def imageExists = imageExist(finalImage, finalTag)

            def nonProdEnv = ["dev", "preprod", "qa"]

            if (environment == "prod" && !imageExists) {
                error "❌ Image not found in the registry. Deployment to PROD is not allowed!"
            }

            if (imageExists) {
                echo "✅ Deploying image to ${environment}..."
                deploy(environment, finalImage, finalTag)
                deploymentScale(releaseName, environment, pipeline, credentials)
            } else {
                echo "🚀 Image not found. Proceeding with alternative flow..."
            }
        }
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

    return [
        finalImage: jsonObj?.imageName?.trim() ?: "",
        finalTag: jsonObj?.imageTag?.trim() ?: ""
    ]
}

def imageExist(String image, String tag) {
    try {
        def result = sh(script: "docker manifest inspect ${image}:${tag}", returnStatus: true)
        return (result == 0)
    } catch (Exception e) {
        echo "⚠️ Error checking image: ${e.message}"
        return false
    }
}

def getReplicas(String releaseName, String namespace) {
    try {
        return sh(script: "kubectl get deployment ${releaseName} -n ${namespace} -o=jsonpath='{.spec.replicas}'", returnStdout: true).trim().toInteger()
    } catch (Exception e) {
        echo "⚠️ Failed to get replicas: ${e.message}"
        return 0
    }
}

def deploy(String environment, String image, String tag) {
    try {
        sh """
        export KUBECONFIG=\$KUBECONFIG
        helm upgrade --install my-app-release-${environment} myrelease \
            --set image.repository=${image} \
            --set image.tag=${tag} \
            --set namespace=${environment} \
            --namespace=${environment}
        """
        echo "✅ Deployment successful to ${environment}."
    } catch (Exception e) {
        error "❌ Deployment failed: ${e.message}"
    }
}

def deploymentScale(String releaseName, String namespace, String pipeline, String credentials) {
    def jsonContent = readFile(pipeline).trim()
    def jsonData = new JsonSlurperClassic().parseText(jsonContent)

    def scaleUpEnabled = jsonData?.scale_up?.enabled ?: false
    def scaleDownEnabled = jsonData?.scale_down?.enabled ?: false
    def minReplicas = jsonData?.min_replicas ?: 1
    def maxReplicas = jsonData?.scale_up?.max_replicas ?: 3

    if (scaleUpEnabled && scaleDownEnabled) {
        error "❌ Both scale-up and scale-down are enabled. Aborting!"
    }

    def currentReplicas = getReplicas(releaseName, namespace)
    def newReplicas = currentReplicas

    if (scaleUpEnabled && currentReplicas < maxReplicas) {
        newReplicas = currentReplicas + 1
        echo "⬆️ Scaling up to ${newReplicas} replicas..."
    } else if (scaleDownEnabled && currentReplicas > minReplicas) {
        newReplicas = currentReplicas - 1
        echo "⬇️ Scaling down to ${newReplicas} replicas..."
    } else {
        echo "🔄 No scaling action needed."
        return
    }

    try {
        sh "kubectl scale deployment ${releaseName} -n ${namespace} --replicas=${newReplicas}"
    } catch (Exception e) {
        error "❌ Scaling failed: ${e.message}"
    }
}
