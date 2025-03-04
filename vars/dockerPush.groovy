def call(String dockerImage) {
    echo "Pushing Docker image to registry: ${dockerImage}"
    
    // Log in to Docker registry
    withCredentials([usernamePassword(credentialsId: 'docker_login', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
       sh '''
        echo docker login -u "${DOCKER_USERNAME}" --password-stdin
        '''
    }
    
    // Push the Docker image
    sh "docker push ${dockerImage}"
}
