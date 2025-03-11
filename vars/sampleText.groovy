import groovy.json.JsonSlurper

def call(filePath) {
    withCredentials([usernamePassword(credentialsId: 'docker_login', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
        try {
            if (!DOCKER_USER?.trim() || !DOCKER_PASSWORD?.trim()) {
                error("❌ Docker credentials are missing! Check your Jenkins credentials.")
            }

            echo "🔑 Logging in to Docker..."
            sh(script: """
                echo '${DOCKER_PASSWORD}' | docker login -u '${DOCKER_USER}' --password-stdin
            """)

            echo "✅ Docker login successful."

            // ✅ Call checkImage only if login succeeds
            return checkImage(filePath)

        } catch (Exception e) {
            echo "❌ Failed: ${e.message}"
            return false
        }
    }
}

// ✅ Function to check image in Docker Hub
def checkImage(filePath) {
    def fileContent = readFile(filePath).trim()
    echo "📄 JSON File Content: ${fileContent.inspect()}"

    def jsonSlurper = new JsonSlurper()
    def jsonObj = jsonSlurper.parseText(fileContent)

    def dockerImage = jsonObj.imageName
    def imageTag = jsonObj.imageTag

    if (!dockerImage || !imageTag) {
        error("❌ 'imageName' or 'imageTag' is missing in JSON.")
    }

    echo "🔍 Extracted imageName: ${dockerImage}"
    echo "🔍 Extracted imageTag: ${imageTag}"

    // ✅ Proper cURL command formatting
    def curlCommand = "curl -s -o /dev/null -w '%{http_code}' https://hub.docker.com/v2/repositories/${dockerImage}/tags/${imageTag}"
    def httpCode = sh(script: curlCommand, returnStdout: true).trim()

    if (httpCode == "200") {
        echo "✅ Docker image ${dockerImage}:${imageTag} exists."
        return true
    } else if (httpCode == "404") {
        echo "❌ Docker image ${dockerImage}:${imageTag} does NOT exist."
        return false
    } else {
        echo "❌ Unexpected error. HTTP code: ${httpCode}"
        return false
    }
}
