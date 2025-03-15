import groovy.json.JsonSlurper
def call(String releaseName, String namespace){    
    
    def selectedVersion = selectHelmVersion(releaseName, namespace)
    println("Rolling back to version: ${selectedVersion}")
    rollbackHelm(releaseName, namespace, selectedVersion)
    
}

def getHelmReleaseVersions(String releaseName, String namespace) {
    try {
        // Fetch Helm history in JSON format
        def historyJson = sh(script: "helm history ${releaseName} -n ${namespace} -o json", returnStdout: true).trim()
        def getConfig = readFile(historyJson)
        def jsonSlurper = new JsonSlurper()
        return jsonSlurper.parseText(getConfig)
        // return readJSON(text: historyJson)
    } catch (Exception e) {
        error "Helm release '${releaseName}' not found in namespace '${namespace}'"
    }
}

def rollbackHelm(String releaseName, String namespace, int version) {
    // Perform Helm rollback to the specified version
    sh "helm rollback ${releaseName} ${version} -n ${namespace}"
}

def selectHelmVersion(String releaseName, String namespace) {
    // Fetch Helm release history
    def history = getHelmReleaseVersions(releaseName, namespace)
    
    // Check if history is empty
    if (!history || history.isEmpty()) {
        error "No Helm releases found for ${releaseName} in namespace ${namespace}"
    }

    // If only one revision exists, rollback is not possible
    if (history.size() == 1) {
        println "Only one Helm revision found. No rollback possible."
        return history[0].revision.toInteger()
    }

    // Display available Helm versions
    println "Available Helm Versions:"
    history.each { entry ->
        println "[Release name: ${entry.name} -- ${entry.revision}] Deployed on: ${entry.updated} - Status: ${entry.status}"
    }

    // Prompt user to select a version for rollback
    def selectedVersion = input(
        message: "Select Helm version for rollback",
        parameters: [
            choice(name: 'Versions', choices: history.collect { it.revision as String }, description: "Choose the Helm version to rollback")
        ]
    )

    return selectedVersion.toInteger()
}

