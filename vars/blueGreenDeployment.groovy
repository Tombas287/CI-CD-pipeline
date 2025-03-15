def getHelmReleaseVersions(String releaseName , String namespace) {
    def historyJson = sh(script: "helm history ${releaseName} -n ${namespace} -o json", returnStdout:true).trim()
    return readJSON(text: historyJson)

}

def rollbackHelm(String releaseName, String namespace, int version) {
    sh "helm rollback ${releaseName} ${version} -n ${namespace}"

}

def selectHelmVersion(String releaseName, String namespace ) {
    def history = getHelmReleaseVersions(releaseName, namespace)
    if (history.isEmpty()) {
        error "No Helm releases found for ${namespace}"
    }
    
    println "Available Helm Versions:"
    history.each { entry -> }
        println "[Release name: ${entry.name} -- ${entry.revision}] Deployed on: ${entry.updated} - Status: ${entry.status}"
        
      }
      def selectedVersion = input (
         message: "Select Helm version  for rollback",
         parameters : [
         choice(name: 'Versions', choices: history.collect {it.revision as String }, description: "choose the Helm version to  rollback")
                
            ]
         )   
    return selectedVersion.toInteger()
}


def selectedVersion = selectHelmVersion(releaseName, namespace)
println("Rolling back to version: ${selectedVersion}")
rollbackHelm(releaseName, namespace, selectedVersion)
