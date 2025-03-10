import groovy.json.JsonSlurper

def call(String pipeline) {
    def imageExist = checkImageExist(pipeline)
    return imageExist
}

def checkImageExist(String pipeline) {
    withCredentials([usernamePassword(credentialsId: 'docker_login', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
        try {
            // Ensure credentials are retrieved correctly
            if (!DOCKER_USER?.trim() || !DOCKER_PASSWORD?.trim()) {
                error("❌ Docker credentials not found. Check Jenkins credentials store.")
            }

            // Read and parse pipeline.json
            def configFile = readFile("${WORKSPACE}/${pipeline}")
            def jsonSlurper = new JsonSlurper()
            def jsonObj = jsonSlurper.parseText(configFile)

            // Ensure docker_registry is a Map to avoid LazyMap issues
            if (!(jsonObj.docker_registry instanceof Map)) {
                error("❌ Invalid format for docker_registry in pipeline.json.")
            }

            def dockerRegistry = jsonObj.docker_registry
            def imageName = dockerRegistry.imageName ?: "7002370412/nginx"
            def imageTag = dockerRegistry.imageTag ?: "latest"

            echo "🔍 Checking image: ${imageName}:${imageTag}"

            // Securely log in to Docker Hub
            sh(script: "echo '${DOCKER_PASSWORD}' | docker login -u '${DOCKER_USER}' --password-stdin")

            // Check if the image exists
            def response = sh(
                script: "curl -s -o /dev/null -w '%{http_code}' https://hub.docker.com/v2/repositories/${imageName}/tags/${imageTag}/",
                returnStdout: true
            ).trim()

            if (response == "200") {
                echo "✅ Docker image ${imageName}:${imageTag} exists in Docker Hub."
                return true
            } else {
                echo "❌ Docker image ${imageName}:${imageTag} does NOT exist in Docker Hub."
                return false
            }
        } catch (Exception e) {
            echo "❌ Failed to check image: ${e.message}"
            return false
        }
    }
}
