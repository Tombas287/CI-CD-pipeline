def nonProdEnvs = ["dev", "preprod", "qa"]

if (environment == "prod") {
    if (imageExists) {
        echo "✅ Image exists. Deploying to PROD..."
        sh """
            helm upgrade --install my-release myrelease \
                --set image.repository=${dockerImage} \
                --set image.tag=${imageTag}
        """
    } else {
        error "❌ Image not found in the registry. Deployment to PROD is not allowed!"
    }
} else if (nonProdEnvs.contains(environment)) {  // ✅ Correct condition
    if (imageExists) {
        echo "✅ Image exists. Performing Helm upgrade..."
        sh """
            helm upgrade --install my-release myrelease \
                --set image.repository=${dockerImage} \
                --set image.tag=${imageTag}
        """
    } else {
        echo "🚀 Image not found. Proceeding with Helm install..."
        sh """
            helm install my-release myrelease \
            --set image.repository=${dockerImage} \
            --set image.tag=${imageTag}
        """
    }
} else {
    error "❌ Invalid environment specified: ${environment}"
}
