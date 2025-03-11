import groovy.json.JsonSlurper

def call(filePath) {
    if (!fileExists(filePath)) {
        echo "❌ JSON file does not exist: ${filePath}"
        return false
    }

    def fileContent = readFile(filePath).trim()
    if (!fileContent) {
        echo "❌ JSON file is empty."
        return false
    }

    def jsonSlurper = new JsonSlurper()
    def jsonObj = jsonSlurper.parseText(fileContent)
    def imageName = jsonObj.imageName
    def imageTag = jsonObj.imageTag

    if (!imageName?.trim() || !imageTag?.trim()) {
        echo "❌ Missing imageName or imageTag in JSON file."
        return false
    }

    // ✅ Suppress output & return only true/false
    def status = sh(script: "curl -s -f https://hub.docker.com/v2/repositories/${imageName}/tags/${imageTag}", returnStatus: true)

    if (status == 0) {
        return true  // ✅ Image exists
    } else {
        return false  // ❌ Image not found
    }
}
