import groovy.json.JsonSlurper

def call(String environment, String credentials, String pipeline) {
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
            deploymentScale(releaseName, environment , pipeline)
            }

        }
    }
// def call(String releaseName, String namespace, String pipeline) {
//     deploymentScale(releaseName, namespace, pipeline)
// }

def deploymentScale(String releaseName, String namespace, String pipeline) {
    try {
        def jsonContent = readFile(pipeline).trim()
        def jsonData = new JsonSlurper().parseText(jsonContent)

        def scaleUpEnabled = jsonData?.scale_up?.enabled ?: false
        def scaleDownEnabled = jsonData?.scale_down?.enabled ?: false
        def minReplicas = jsonData?.min_replicas?.toInteger() ?: 1
        def maxReplicas = jsonData?.scale_up?.max_replicas?.toInteger() ?: 1

        if (scaleUpEnabled && scaleDownEnabled) {
           echo "Warning: Both scale-up and scale-down are enabled simultaneously. Scaling will not occur."
           scaleUpEnabled = false;
           scaleDownEnabled = false;
        }

        // def currentReplicas = sh(
        //     script: "kubectl get deployment ${releaseName} -n ${namespace} -o json | jq -r .spec.replicas",
        //     returnStdout: true
        // ).trim()
        def currentReplicas = sh(
            script: "kubectl get deployment ${releaseName} -n ${namespace} -o=jsonpath='{.spec.replicas}'",
            returnStdout: true
        ).trim().toInteger()


        // def currentReplicas = 1

        def newReplicas = currentReplicas

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
