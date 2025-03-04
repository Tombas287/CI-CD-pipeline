def call(Map config = [:]) {
    def projectKey = config.projectKey ?: error("Missing required parameter: projectKey")
    def sonarHost = config.sonarHost ?: 'http://localhost:9000'
    def sonarToken = config.sonarToken ?: error("Missing required parameter: sonarToken")

    script {
        sh """
        docker run --rm \
        -v "$(pwd):/usr/src" \
        sonarsource/sonar-scanner-cli \
        -Dsonar.projectKey=${projectKey} \
        -Dsonar.sources=. \
        -Dsonar.host.url=${sonarHost} \
        -Dsonar.login=${sonarToken}

        """
    }
}
