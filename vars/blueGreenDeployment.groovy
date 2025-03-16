import groovy.json.JsonSlurperClassic

def deploymentScale(String releaseName, String namespace, String pipeline, String credentialsId) {
    withCredentials([file(credentialsId: credentialsId, variable: 'KUBECONFIG')]) {
        script {
            try {
                // Step 1: Check if KUBECONFIG is set
                echo "✅ Setting KUBECONFIG..."
                sh """
                echo "Current Context:"
                kubectl config current-context
                echo "Available Contexts:"
                kubectl config get-contexts
                helm version
                """

                // Step 2: Parse pipeline JSON
                def jsonContent = readFile(pipeline).trim()
                def jsonData = new JsonSlurperClassic().parseText(jsonContent)

                def scaleUpEnabled = jsonData?.scale_up?.enabled ?: false
                def scaleDownEnabled = jsonData?.scale_down?.enabled ?: false
                def minReplicas = jsonData?.scale_down?.min_replicas ?: 1
                def maxReplicas = jsonData?.scale_up?.max_replicas ?: 5

                if (scaleUpEnabled && scaleDownEnabled) {
                    echo "Warning: Both scale-up and scale-down are enabled simultaneously. Scaling will not occur."
                    scaleUpEnabled = false
                    scaleDownEnabled = false
                }

                // Step 3: Get current replica count
                def currentReplicas = sh(script: """
                    kubectl get deployment ${releaseName} -n ${namespace} -o=jsonpath='{.spec.replicas}'
                """, returnStdout: true).trim()

                echo "Current replica count: ${currentReplicas}"

                // Step 4: Scale up or down based on conditions
                def newReplicas = currentReplicas
                if (scaleUpEnabled && currentReplicas < maxReplicas) {
                    newReplicas = currentReplicas + 1
                    echo "Scaling up to ${newReplicas} replicas..."
                    sh "kubectl scale deployment ${releaseName} -n ${namespace} --replicas=${newReplicas}"
                } else if (scaleDownEnabled && currentReplicas > minReplicas) {
                    newReplicas = currentReplicas - 1
                    echo "Scaling down to ${newReplicas} replicas..."
                    sh "kubectl scale deployment ${releaseName} -n ${namespace} --replicas=${newReplicas}"
                } else {
                    echo "No action required or scaling limits reached."
                }

            } catch (FileNotFoundException e) {
                error "❌ pipeline.json not found: ${e.getMessage()}"
            } catch (groovy.json.JsonException e) {
                error "❌ Error parsing pipeline.json: ${e.getMessage()}"
            } catch (Exception e) {
                error "❌ An unexpected error occurred: ${e.getMessage()}"
            }
        }
    }
}
