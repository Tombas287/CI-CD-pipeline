def call(String environment, String credentials, String dockerImage , String imageTag) {
    withCredentials([file(credentialsId: credentials, variable: 'KUBECONFIG')]) {
        script {
            echo "✅ Setting KUBECONFIG..."
            sh """
            export KUBECONFIG=\$KUBECONFIG
            kubectl config get-contexts
            helm version
            """
            // Check if image exists
            def imageExists = imageExist(dockerImage, imageTag)

            if (imageExists) {
                echo "✅ Image exists. Deploying existing image..."
            } else {
                echo "🚀 Image not found. Deploying latest build..."
            }
            // Run Helm with the correct image tag
            sh """
            helm upgrade --install my-release ./helm-chart \
                --set image.repository=${dockerImage} \
                --set image.tag=${imageTag}
           """
        }
    }
}
