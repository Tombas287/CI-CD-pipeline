/**
 * SonarQube Scanner - Runs SonarScanner using Docker
 * 
 * @param projectKey (Required) - The SonarQube Project Key
 * @param sonarHost (Optional) - SonarQube Server URL (Default: http://localhost:9000)
 * @param sonarToken (Required) - Authentication Token for SonarQube
 */
def call(Map config = [:]) {
    def projectKey = config.projectKey ?: error("Missing required parameter: projectKey")
    def sonarHost = config.sonarHost ?: 'http://localhost:9000'
    def sonarToken = config.sonarToken ?: error("Missing required parameter: sonarToken")

    script {
        // Check if SonarQube is running
        def sonarStatus = sh(script: "docker ps --filter 'name=sonar' --format '{{.Names}}'", returnStdout: true).trim()

        if (!sonarStatus) {
            error("🚨 SonarQube is NOT running! Start it first using: docker start sonar")
        } else {
            echo "✅ SonarQube is running. Proceeding with code analysis..."
        }

        // Run SonarScanner in Docker
        sh """
        docker run --rm -v "\$(pwd):/usr/src" sonarsource/sonar-scanner-cli \\
            -Dsonar.projectKey=${projectKey} \\
            -Dsonar.sources=. \\
            -Dsonar.host.url=${sonarHost} \\
            -Dsonar.login=${sonarToken}
        """
    }
}
