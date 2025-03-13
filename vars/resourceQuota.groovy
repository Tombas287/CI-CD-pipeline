import groovy.json.JsonSlurper

def call(String quotaName, String namespace) {
    try {
        // Run the kubectl command to get the resource quota in JSON format
        def quotaJson = sh(script: "kubectl get resourcequota ${quotaName} -n ${namespace} -o json", returnStdout: true).trim()

        if (!quotaJson?.trim()) {
            error("No output from kubectl command. Is Kubernetes running?")
        }

        // Parse JSON response
        def quotaData = new JsonSlurper().parseText(quotaJson)

        def hardLimits = quotaData.spec.hard
        def usedLimits = quotaData.status.used

        // Extract and handle CPU and memory usage
        def cpuLimit = extractValue(hardLimits['limits.cpu'], "cpu")
        def memoryLimit = extractValue(hardLimits['limits.memory'], "memory")
        def cpuUsed = extractValue(usedLimits['limits.cpu'], "cpu")
        def memoryUsed = extractValue(usedLimits['limits.memory'], "memory")

        def cpuUsagePercent = (cpuUsed / cpuLimit) * 100
        def memoryUsagePercent = (memoryUsed / memoryLimit) * 100

        println "CPU Usage: ${cpuUsed}/${cpuLimit} (${cpuUsagePercent}%)"
        println "Memory Usage: ${memoryUsed}Gi/${memoryLimit}Gi (${memoryUsagePercent}%)"

        // Alert if usage exceeds 80%
        if (cpuUsagePercent > 80 || memoryUsagePercent > 80) {
            println "⚠️ ALERT: Resource quota usage exceeded 80% ⚠️"
            error("ResourceQuota Limit Exceeded!")
        } else {
            println "✅ Resource usage is within limits."
        }

    } catch (Exception e) {
        println "❌ Error checking resource quota: ${e.message}"
        throw e
    }
}

// Helper function to extract resource values and convert to float
def extractValue(resourceValue, resourceType) {
    def resourceValueStr = resourceValue.replaceAll("\"", "")

    // Handle CPU values with 'm' (milli-cores)
    if (resourceValueStr.endsWith("m")) {
        resourceValueStr = resourceValueStr.replaceAll("m", "")
        return resourceValueStr.toFloat() / 1000  // Convert milli-cores to cores
    }

    // Handle memory units (Gi, Mi, etc.)
    if (resourceValueStr.endsWith("Gi")) {
        resourceValueStr = resourceValueStr.replaceAll("Gi", "")
        return resourceValueStr.toFloat()  // Return Gi in Gi
    } else if (resourceValueStr.endsWith("Mi")) {
        resourceValueStr = resourceValueStr.replaceAll("Mi", "")
        return resourceValueStr.toFloat() / 1024  // Convert Mi to Gi
    }

    // If no known unit suffix, return as float
    try {
        return resourceValueStr.toFloat()
    } catch (Exception e) {
        throw new IllegalArgumentException("Invalid ${resourceType} value: ${resourceValueStr}")
    }
}
