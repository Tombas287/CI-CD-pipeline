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
                sh """
                    helm upgrade --install my-release myrelease \
                        --set image.repository=${dockerImage} \
                        --set image.tag=${imageTag}
           """
            } else {
                echo "🚀 Image not found..."
                sh """
                helm install  my-release  \
                --set image.repository=${dockerImage} \
                --set image.tag=${imageTag}

                """
            }
            // Run Helm with the correct image tag

        }
    }
}
