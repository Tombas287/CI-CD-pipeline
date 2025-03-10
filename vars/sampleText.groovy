import groovy.json.JsonSlurper

def call(pipeline) {
  def imageExist = checkImageExist(pipeline)
  return imageExist  
  
}

def checkImageExist(String pipeline) {
    withCredentials([usernamePassword(credentialsId: 'docker_login', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
        try {

            if (!DOCKER_USER?.trim() || !DOCKER_PASSWORD?.trim()) {
              error("❌ Docker credentials not found. Check Jenkins credentials store.")
            }
            // Read and parse pipeline.json 
            def configFile = readFile("${WORKSPACE}/${pipeline}")
            def jsonSlurper = new JsonSlurper()
            def jsonObj = jsonSlurper.parseText(configFile)

            // Check if "docker_registry" exists; if missing, use an empty map to avoid errors
            def dockerRegistry = jsonObj?.docker_registry ?: [:]

            // Use defaults if missing
            def imageName = dockerRegistry.imageName.toString() ?: "7002370412/nginx"
            def imageTag = dockerRegistry.imageTag.toString() ?: "latest"

            echo "🔍 Checking image: ${imageName}:${imageTag}"

            // Log in to Docker Hub
            // sh(script: "docker login -u '${DOCKER_USER}' --password-stdin")
            sh(script: "echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USER --password-stdin", returnStdout: true)

          

            // Check if the image exists
            def response = sh(
                script: "curl -s -o https://hub.docker.com/v2/repositories/${imageName}/tags/${imageTag}/",
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
