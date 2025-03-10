import groovy.json.JsonSlurper

def call(pipeline) {
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

            // Read and parse JSON file
            def configFile = readFile(pipeline)
            def jsonSlurper = new JsonSlurper()
            def jsonObj = jsonSlurper.parseText(configFile)

            // Debugging: Print JSON structure
            echo "🔍 Parsed JSON: ${jsonObj.toString()}"

            // Ensure 'docker_registry' exists and is a valid Map
            if (!(jsonObj.docker_registry instanceof Map)) {
                error("❌ 'docker_registry' is missing or not an object.")
            }

            def dockerRegistry = jsonObj.docker_registry

            if (!dockerRegistry.containsKey('imageName') || !dockerRegistry.containsKey('imageTag')) {
                error("❌ 'imageName' or 'imageTag' is missing in docker_registry.")
            }

            def imageName = dockerRegistry.imageName
            def imageTag = dockerRegistry.imageTag

            echo "🔍 Checking if image exists: ${imageName}:${imageTag}"

            def curlCommand = "curl -s -o /dev/null -w '%{http_code}' 'https://hub.docker.com/v2/repositories/${imageName}/tags/${imageTag}/'"
            def httpCode = sh(script: curlCommand, returnStdout: true).trim()

            if (httpCode == "200") {
                echo "✅ Docker image ${imageName}:${imageTag} exists."
                return true
            } else if (httpCode == "404") {
                echo "❌ Docker image ${imageName}:${imageTag} does NOT exist."
                return false
            } else {
                echo "❌ Unexpected error. HTTP code: ${httpCode}"
                return false
            }

        } catch (Exception e) {
            echo "❌ Failed to check image: ${e.message}"
            return false
        }
    }
}
