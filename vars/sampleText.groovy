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

            // Read pipeline.json and extract image details
            def configFile = readFile(pipeline)
            def jsonSlurper = new JsonSlurper()
            def jsonObj = jsonSlurper.parseText(configFile)

            if (!jsonObj.docker_registry) {
                error("❌ 'docker_registry' missing in pipeline.json.")
            }

            def dockerRegistry = jsonObj.docker_registry
            def imageName = dockerRegistry.imageName ?: "default/nginx"
            def imageTag = dockerRegistry.imageTag ?: "latest"

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
