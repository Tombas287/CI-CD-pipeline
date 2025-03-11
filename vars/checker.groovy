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

    // ✅ Ensure JsonSlurper is not stored as a variable to avoid serialization issues
    def jsonObj = new JsonSlurper().parseText(fileContent)
    def imageName = jsonObj.imageName
    def imageTag = jsonObj.imageTag

    if (!imageName?.trim() || !imageTag?.trim()) {
        echo "❌ Missing imageName or imageTag in JSON file."
        return false
    }

    // ✅ Improved error handling
    try {
        def status = sh(script: "curl -s -f -o /dev/null https://hub.docker.com/v2/repositories/${imageName}/tags/${imageTag}", returnStatus: true)

        if (status == 0) {
            echo "✅ Docker image ${imageName}:${imageTag} exists."
            return true
        } else {
            echo "❌ Docker image ${imageName}:${imageTag} not found."
            return false
        }
    } catch (Exception e) {
        echo "❌ Error checking Docker image: ${e.getMessage()}"
        return false
    }
}
