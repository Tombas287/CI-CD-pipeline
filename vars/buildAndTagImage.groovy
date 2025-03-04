def call(String dockerTag) {
    echo "Building Docker image and tagging it as: ${dockerTag}"
    sh 'docker build -t ${dockerTag} .'
}