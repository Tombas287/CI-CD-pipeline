import groovy.json.JsonSlurper

def call(String releaseName, String namespace) {    
    try {
        def selectedVersion = selectHelmVersion(releaseName, namespace)
        if (selectedVersion) {
            println("Rolling back to version: ${selectedVersion}")
            rollbackHelm(releaseName, namespace, selectedVersion)
        } else {
            println("No rollback performed.")
        }
    } catch (Exception e) {
        println "Error: ${e.message}"
    }
}

def getHelmReleaseVersions(String releaseName, String namespace) {
    try {
        // Fetch Helm history in JSON format
        def historyJson = sh(script: "helm history ${releaseName} -n ${namespace} -o json", returnStdout: true).trim()
        if (!historyJson) {
            error "Helm history is empty for release '${releaseName}' in namespace '${namespace}'"
        }
        def jsonSlurper = new JsonSlurper()
        return jsonSlurper.parseText(historyJson)
    } catch (Exception e) {
        error "Failed to fetch Helm release history: ${e.message}"
    }
}

def rollbackHelm(String releaseName, String namespace, int version) {
    try {
        // Perform Helm rollback to the specified version
        sh "helm rollback ${releaseName} ${version} -n ${namespace}"
    } catch (Exception e) {
        error "Helm rollback failed: ${e.message}"
    }
}

def selectHelmVersion(String releaseName, String namespace) {
    def history = getHelmReleaseVersions(releaseName, namespace)
    
    if (!history || history.isEmpty()) {
        error "No Helm releases found for ${releaseName} in namespace ${namespace}"
    }

    if (history.size() == 1) {
        println "Only one Helm revision found. Rollback is not possible."
        return null
    }

    // Display available Helm versions
    println "Available Helm Versions:"
    history.each { entry ->
        println "[Release: ${entry.name}, Revision: ${entry.revision}] Deployed on: ${entry.updated}, Status: ${entry.status}"
    }

    // Prompt user to select a version for rollback
    def selectedVersion = timeout(time: 30, unit: 'SECONDS') {
        input(
        message: "Select Helm version for rollback",
        parameters: [
            choice(name: 'Versions', choices: history.collect { it.revision.toString() }, description: "Choose Helm version to rollback")
        ]
    )
    }
    return selectedVersion.toInteger()
}
