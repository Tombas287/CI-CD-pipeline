import groovy.json.JsonSlurperClassic

def call(pipeline) {
    def imageExist = checkImageExist(pipeline)
    return imageExist
}

def checkImageExist(pipeline) {
    withCredentials([usernamePassword(credentialsId: 'docker_login', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
        try {
            if (!DOCKER_USER?.trim() || !DOCKER_PASSWORD?.trim()) {
                error("❌ Docker credentials not found. Check Jenkins credentials store.")
            }

            def configFile = readFile("${WORKSPACE}/${pipeline}")
            def jsonSlurper = new JsonSlurperClassic()
            def jsonObj = jsonSlurper.parseText(configFile)

            if (!jsonObj.docker_registry) {
                error("❌ docker_registry is missing or empty in pipeline.json.")
            }

            if (!(jsonObj.docker_registry instanceof Map)) {
                error("❌ Invalid format for docker_registry in pipeline.json.")
            }

            def dockerRegistry = new HashMap(jsonObj.docker_registry)
            def imageName = dockerRegistry['imageName'] ?: "7002370412/nginx"
            def imageTag = dockerRegistry['imageTag'] ?: "latest"

            echo "🔍 Checking image: ${imageName}:${imageTag}"
            sh """
                 docker login --username ${DOCKER_USER} --password-stdin
            """
                echo "Login successful." 

            def curlCommand = "curl -s -o /dev/null -w '%{http_code}' 'https://hub.docker.com/v2/repositories/${imageName}/tags/${imageTag}/'"
            def httpCode = sh(script: curlCommand, returnStdout: true).trim()

            if (httpCode == "200") {
                echo "✅ Docker image ${imageName}:${imageTag} exists in Docker Hub."
                return true
            } else if (httpCode == "404") {
                echo "❌ Docker image ${imageName}:${imageTag} does NOT exist in Docker Hub."
                return false
            } else {
                echo "❌ Failed to check image. HTTP code: ${httpCode}"
                return false
            }
        } catch (Exception e) {
            echo "❌ Failed to check image: ${e.message}"
            return false
        }
    }
}
