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
            output = checkImage(filePath)
            println(output)

        } catch (Exception e) {
            echo "❌ Failed: ${e.message}"

        }
    }
}

// ✅ Function to check image in Docker Hub
def checkImage(filePath) {
    def fileContent = readFile(filePath).trim()
    def jsonslurper = new JsonSlurper()
    def jsonObj = jsonslurper.parseText(fileContent)
    def imageName = jsonObj.imageName
    def imageTag = jsonObj.imageTag

    def imageExist = true
    if (imageName?.trim() && imageTag?.trim()){
        def status = sh(script: "curl -s -f https://hub.docker.com/v2/repositories/${imageName}/tags/${imageTag}", returnStatus: true)
        if (status == 0) {
            echo "Image exist"
            imageExist = true

        } else {
            echo "Image not found in environment."
            imageExist = false
        }
        return imageExist
    }

}