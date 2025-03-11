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
