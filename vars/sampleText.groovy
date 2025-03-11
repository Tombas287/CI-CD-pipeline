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

            // ✅ FIX: Read JSON file properly using Jenkins readFile
            def fileContent = readFile(pipeline).trim()
            def jsonSlurper = new JsonSlurper()
            def jsonObj = jsonSlurper.parseText(fileContent)

            def dockerImage = jsonObj?.docker_registry?.imageName
            def imageTag = jsonObj?.docker_registry?.imageTag

            if (!dockerImage || !imageTag) {
                error("❌ 'imageName' or 'imageTag' is missing in docker_registry.")
            }

            echo "🔍 Extracted imageName: ${dockerImage}"
            echo "🔍 Extracted imageTag: ${imageTag}"

            echo "🔍 Checking if image exists: ${dockerImage}:${imageTag}"

            // ✅ FIX: Corrected curl syntax
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

        } catch (Exception e) {
            echo "❌ Failed to check image: ${e.message}"
            return false
        }
    }
}
