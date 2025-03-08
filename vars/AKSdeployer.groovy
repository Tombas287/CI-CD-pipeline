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
            def nonProdEnv = ["dev", "preprod", "qa"]
            if (environment == "prod") {
                if (imageExist) {
                    echo "✅ Image exists.deploying to ${environment}"
                    sh """
                        helm install my-release myrelease \
                            --set image.repository=${dockerImage} \
                            --set image.tag=${imageTag}
                    """

                }
               else {
                  error "❌ Image not found in the registry. Deployment to PROD is not allowed!"

               }
            } else if (nonProdEnv.contains(environment)) {
                if (imageExists){
                echo "✅ Image exists. Deploying existing image to ${environment}."
                sh """
                    helm install my-app-release  \
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

            }

        }
    }
}
