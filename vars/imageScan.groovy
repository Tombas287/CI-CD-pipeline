def call(String dockerImage){
    echo "Running Trivy image scan on: ${dockerImage}"
    sh """
    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image --quiet --exit-code 0 --severity HIGH,CRITICAL ${dockerImage}

    """



}
