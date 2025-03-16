import groovy.json.JsonSlurper

def call(String releaseName, String namespace) {
    deploymentScale(releaseName, namespace)
    
}

def deploymentScale(String releaseName, String namespace) {
    // Read the pipeline.json file content
    def jsonContent = readFile('pipeline.json').trim()

    // Parse the JSON content
    def jsonData = new JsonSlurper().parseText(jsonContent)

    // Extract values with default fallbacks
    def scaleUpEnabled = jsonData?.scale_up?.enabled ?: false
    def scaleDownEnabled = jsonData?.scale_down ?: false
    def minReplicas = jsonData?.min_replicas ?: 1
    def maxReplicas = jsonData?.scale_up?.max_replicas ?: 1

    // Fetch the current replicas via shell command and convert to integer
    def currentReplicas = sh(script: "kubectl get deployment ${releaseName} -n ${environment} -o json | jq -r '.spec.replicas'", returnStdout: true).trim()
    def newReplicas = currentReplicas

    // Check for invalid configuration (both up and down enabled)
    if (scaleUpEnabled && scaleDownEnabled) {
        error "Invalid choice: Both scale-up and scale-down are enabled simultaneously"
    }

    // Scale up logic
    if (scaleUpEnabled && currentReplicas < maxReplicas) {
        newReplicas = currentReplicas + 1
        def scaleCommand = "kubectl scale deployment ${releaseName} -n ${namespace}  --replicas=${newReplicas}"
        echo "Executing: ${scaleCommand}"
        sh(script: scaleCommand)
    }
    // Scale down logic
    else if (scaleDownEnabled && currentReplicas > minReplicas) {
        newReplicas = currentReplicas - 1
        def scaleCommand = "kubectl scale deployment ${releaseName} -n ${namespace}  --replicas=${newReplicas}"
        echo "Executing: ${scaleCommand}"
        sh(script: scaleCommand)
    }
    // No action needed
    else {
        echo "No action required or limit reached or scaling disabled"
    }
}
