def call(String dockerImage, String imageTag){
    // ${env.ENVIRONMENT}-${env.GIT_COMMIT_SHA}

    def imageExist = false
    try {

        def status = sh(script: "curl -s -f https://hub.docker.com/v2/repositories/${dockerImage}/tags/${imageTag}", returnStatus: true)
        if (status == 0) {
            echo "Image exist"
            imageExist = true

        }
        else {
            echo "Image not found in ${env} environment."
        }

    }
    catch (Exception e) {
    echo "Error checking image in environment: ${e.getMessage()}"


    }
    return imageExist

}
