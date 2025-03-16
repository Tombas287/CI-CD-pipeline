import groovy.json.JsonSlurper

def call(String releaseName, String namespace) {
    deploymentScale(releaseName, namespace)
}

def deploymentScale(String releaseName, String namespace) {
    try {
        def jsonContent = readFile('pipeline.json').trim()
        def jsonData = new JsonSlurper().parseText(jsonContent)

        def scaleUpEnabled = jsonData?.scale_up?.enabled ?: false
        def scaleDownEnabled = jsonData?.scale_down?.enabled ?: false
        def minReplicas = jsonData?.min_replicas?.toInteger() ?: 1
        def maxReplicas = jsonData?.scale_up?.max_replicas?.toInteger() ?: 1

        if (scaleUpEnabled && scaleDownEnabled) {
           echo "Warning: Both scale-up and scale-down are enabled simultaneously. Scaling will not occur."
        }

        def currentReplicas = sh(
            script: "kubectl get deployment ${releaseName} -n ${namespace} -o json | jq -r .spec.replicas",
            returnStdout: true
        ).trim().toInteger()

        def newReplicas = currentReplicas

        if (scaleUpEnabled && currentReplicas < maxReplicas) {
            newReplicas = currentReplicas + 1
            def scaleCommand = "kubectl scale deployment ${releaseName} -n ${namespace} --replicas=${newReplicas}"
            echo "Scaling up to ${newReplicas} replicas..."
            sh(script: scaleCommand)
        } else if (scaleDownEnabled && currentReplicas > minReplicas) {
            newReplicas = currentReplicas - 1
            def scaleCommand = "kubectl scale deployment ${releaseName} -n ${namespace} --replicas=${newReplicas}"
            echo "Scaling down to ${newReplicas} replicas..."
            sh(script: scaleCommand)
        } else {
            echo "No action required or limit reached or scaling disabled."
        }

    } catch (FileNotFoundException e) {
        error "pipeline.json not found: ${e.getMessage()}"
    } catch (groovy.json.JsonException e) {
        error "Error parsing pipeline.json: ${e.getMessage()}"
    } catch (Exception e) {
        error "An unexpected error occurred: ${e.getMessage()}"
    }
}
