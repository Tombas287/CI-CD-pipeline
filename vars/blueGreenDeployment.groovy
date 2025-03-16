import groovy.json.JsonSlurperClassic

def deploymentScale(String releaseName, String namespace, String pipeline) {
    try {
        def jsonContent = readFile(pipeline).trim()
        def jsonData = new JsonSlurperClassic().parseText(jsonContent)

        def scaleUpEnabled = jsonData?.scale_up?.enabled ?: false
        def scaleDownEnabled = jsonData?.scale_down?.enabled ?: false
        def minReplicas = jsonData.scale_down.min_replicas 
        def maxReplicas = jsonData.scale_up.max_replicas 

        if (scaleUpEnabled && scaleDownEnabled) {
           echo "Warning: Both scale-up and scale-down are enabled simultaneously. Scaling will not occur."
           scaleUpEnabled = false;
           scaleDownEnabled = false;
        }
        def currentReplicas = sh(script: """
            kubectl get deployment ${releaseName} -n ${namespace} -o=jsonpath="{.spec.replicas}"
        """, returnStdout: true).trim().toInteger()
       
        def newReplicas = currentReplicas
        echo "Current replicaCount: " + newReplicas

        if (scaleUpEnabled && currentReplicas < maxReplicas) {
            newReplicas = currentReplicas + 1
            def scaleCommand = "kubectl scale deployment ${releaseName} -n ${namespace} --replicas=${newReplicas}"
            echo "Scaling up to ${newReplicas} replicas..."
            
            sh(script: scaleCommand, returnStatus: true)
        } else if (scaleDownEnabled && currentReplicas > minReplicas) {
            newReplicas = currentReplicas - 1
            def scaleCommand = "kubectl scale deployment ${releaseName} -n ${namespace} --replicas=${newReplicas}"
            echo "Scaling down to ${newReplicas} replicas..."
            sh(script: scaleCommand, returnStatus: true)
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
