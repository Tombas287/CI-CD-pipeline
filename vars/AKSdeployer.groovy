def call(String environment, String credentials) {
    withCredentials([file(credentialsId: credentials, variable: 'KUBECONFIG')]) {
        script {
            echo "✅ Setting KUBECONFIG..."
            sh """
            export KUBECONFIG=\$KUBECONFIG
            kubectl config get-contexts
            helm --version
            """
        }
    }
}
