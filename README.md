CI-CD Jenkins automation:- 

![CI_CD Process Flowchart](https://github.com/user-attachments/assets/bbc6c644-473d-4751-8eae-3617d75b402c)

This project involves building, testing, and deploying a Dockerized application using Jenkins. The project uses Groovy for scripting, Python for dependencies, and JSON for configuration.  
pipeline.json
The pipeline.json file contains configuration details for the deployment pipeline, such as the Docker image name, tag, and scaling options. This file is crucial for defining how the application should be deployed and scaled in different environments.
Jenkinsfile Steps
Checkout: Retrieves the source code from the version control system.
Pip Builder: Installs Python dependencies listed in requirements.txt using a specified Python version and virtual environment.
SonarQube Analysis: Runs static code analysis using SonarQube to ensure code quality.
Build and Tag Docker Image: Builds a Docker image and tags it with the current Git commit SHA.
Image Scan: Scans the Docker image for vulnerabilities.
Docker Push to Registry: Pushes the Docker image to a Docker registry.
AKS Deployer Dev: Deploys the Docker image to the development environment using Azure Kubernetes Service (AKS).
AKS Deployer Prod: Prompts the user for confirmation before deploying the Docker image to the production environment using AKS.
Post Actions
Success: Removes the Docker image from the local machine and cleans the workspace.
Failure: (Commented out) Would send an email notification if the build fails.
