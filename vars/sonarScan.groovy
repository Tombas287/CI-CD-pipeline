/**
 * SonarQube Scanner - Runs SonarScanner using Docker
 * 
 * @param projectKey (Required) - The SonarQube Project Key
 * @param sonarHost (Optional) - SonarQube Server URL (Default: http://localhost:9000)
 * @param sonarToken (Required) - Authentication Token for SonarQube (Use Jenkins Credentials)
 */
def call(Map config = [:]) {
    def projectKey = config.projectKey ?: error("🚨 Missing required parameter: projectKey")
    def sonarHost = config.sonarHost ?: 'http://host.docker.internal:9000'

    script {
        withCredentials([string(credentialsId: 'sonar-token-id', variable: 'SONAR_TOKEN')]) {
            // Check if SonarQube is running
            def sonarStatus = sh(script: "docker inspect -f '{{.State.Running}}' sonar || echo 'not running'", returnStdout: true).trim()
        
            if (sonarStatus != 'true') {
                error("🚨 SonarQube is NOT running! Start it first using: docker start sonarqube")
            } else {
                echo "✅ SonarQube is running. Proceeding with code analysis..."
            }

            // Run SonarScanner in Docker
            sh """
            docker run --rm -v "\$WORKSPACE:/usr/src" sonarsource/sonar-scanner-cli \
                -Dsonar.projectKey=${projectKey} \
                -Dsonar.exclusions="**/node_modules/**, **/tests/**, **/*.spec.js, **/*.min.js, **/build/**" \
                -Dsonar.sources=. \
                -Dsonar.host.url=${sonarHost} \
                -Dsonar.login=\$SONAR_TOKEN \
                -Dsonar.scm.disabled=true \
                -Dsonar.verbose=true
            """
        }
    }
}

// Pipeline Stage

