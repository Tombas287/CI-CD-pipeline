import groovy.json.JsonSlurper

def call(filePath) {
    withCredentials([usernamePassword(credentialsId: 'docker_login', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
        try {
            if (!DOCKER_USER?.trim() || !DOCKER_PASSWORD?.trim()) {
                error("❌ Docker credentials are missing! Check your Jenkins credentials.")
            }

            echo "🔑 Logging in to Docker..."
            sh(script: "echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USER --password-stdin")

            echo "✅ Docker login successful."

            // ✅ Call checkImage only if login succeeds
            def output = checkImage(filePath)
            echo "Image check result: ${output}"
            return output  // ✅ Return the result

        } catch (Exception e) {
            echo "❌ Failed: ${e.message}"
            return false  // ✅ Return false on failure
        }
    }
}

def checkImage(filePath) {
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
    def jsonObj

    try {
        jsonObj = jsonSlurper.parseText(fileContent)
    } catch (Exception e) {
        echo "❌ Invalid JSON format: ${e.message}"
        return false
    }

    def imageName = jsonObj.imageName
    def imageTag = jsonObj.imageTag

    if (!imageName?.trim() || !imageTag?.trim()) {
        echo "❌ Missing imageName or imageTag in JSON file."
        return false
    }

    def status = sh(script: "curl -s -f https://hub.docker.com/v2/repositories/${imageName}/tags/${imageTag}", returnStatus: true)
    if (status == 0) {
        echo "✅ Image exists in Docker Hub."
        return true
    } else {
        echo "❌ Image not found in Docker Hub."
        return false
    }
}
