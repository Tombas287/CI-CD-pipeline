import groovy.json.JsonSlurper

def checkResourceQuota(String quotaName, String namespace ) {
    try {
        // Run the kubectl command
//         def quotaJson = "kubectl get resourcequota ${quotaName} -n ${namespace} -o json".execute().text.trim()
        def quotaJson = sh(script: "kubectl get resourcequota ${quotaName} -n ${namespace} -o json", returnStdout: true).trim()

        if (!quotaJson?.trim()) {
            error("No output from kubectl command. Is Kubernetes running?")
        }

        // Parse JSON
        def quotaData = new JsonSlurper().parseText(quotaJson)

        def hardLimits = quotaData.spec.hard
        def usedLimits = quotaData.status.used

        def cpuLimit = hardLimits['limits.cpu'].replaceAll("\"", "").toFloat()
        def memoryLimit = hardLimits['limits.memory'].replaceAll("Gi", "").toFloat()
        def cpuUsed = usedLimits['limits.cpu'].replaceAll("\"", "").toFloat()
        def memoryUsed = usedLimits['limits.memory'].replaceAll("Gi", "").toFloat()

        def cpuUsagePercent = (cpuUsed / cpuLimit) * 100
        def memoryUsagePercent = (memoryUsed / memoryLimit) * 100

        println "CPU Usage: ${cpuUsed}/${cpuLimit} (${cpuUsagePercent}%)"
        println "Memory Usage: ${memoryUsed}Gi/${memoryLimit}Gi (${memoryUsagePercent}%)"

        if (cpuUsagePercent > 80 || memoryUsagePercent > 80) {
            println "⚠️ ALERT: Resource quota usage exceeded 80% ⚠️"
            error("ResourceQuota Limit Exceeded!")
        } else {
            println "✅ Resource usage is within limits."
        }

    } catch (Exception e) {
        println "❌ Error checking resource quota: ${e.message}"
    }
}

// ✅ Correct Function Call
checkResourceQuota("my-quota", "default")
