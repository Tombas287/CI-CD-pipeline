def call(String releaseName, String namespace){    
    
    def selectedVersion = selectHelmVersion(releaseName, namespace)
    println("Rolling back to version: ${selectedVersion}")
    rollbackHelm(releaseName, namespace, selectedVersion)
    
}


def getHelmReleaseVersions(String releaseName, String namespace) {
    // Fetch Helm release history in JSON format
    def historyJson = sh(script: "helm history ${releaseName} -n ${namespace} -o json", returnStdout: true).trim()
    return readJSON(text: historyJson)
}

def rollbackHelm(String releaseName, String namespace, int version) {
    // Perform Helm rollback to the specified version
    sh "helm rollback ${releaseName} ${version} -n ${namespace}"
}

def selectHelmVersion(String releaseName, String namespace) {
    // Fetch Helm release history
    def history = getHelmReleaseVersions(releaseName, namespace)
    
    // Check if history is empty
    if (history.isEmpty()) {
        error "No Helm releases found for ${namespace}"
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

// Example usage

