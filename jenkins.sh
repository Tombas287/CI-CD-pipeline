#!/bin/bash

set -e  # Exit script on error

# Update packages
echo "🔄 Updating package list..."
sudo apt update -y
sudo apt upgrade -y

# Install Java (required for Jenkins)
echo "☕ Installing Java..."
sudo apt install -y openjdk-11-jdk

# Add Jenkins repository and key
echo "🔑 Adding Jenkins repository..."
wget -q -O - https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo gpg --dearmor -o /usr/share/keyrings/jenkins-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/jenkins-archive-keyring.gpg] https://pkg.jenkins.io/debian-stable binary/" | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

# Install Jenkins
echo "🛠️ Installing Jenkins..."
sudo apt update -y
sudo apt install -y jenkins

# Start and enable Jenkins service
echo "🚀 Starting and enabling Jenkins service..."
sudo systemctl start jenkins
sudo systemctl enable jenkins

# Install Docker
echo "🐳 Installing Docker..."
sudo apt install -y docker.io

# Add Jenkins user to Docker group
echo "👤 Adding Jenkins user to Docker group..."
sudo usermod -aG docker jenkins

# Add current user to Docker group
echo "👤 Adding current user to Docker group..."
sudo usermod -aG docker $USER

# Restart Docker and Jenkins services
echo "🔄 Restarting Docker and Jenkins services..."
sudo systemctl restart docker
sudo systemctl restart jenkins

# Apply group changes (requires re-login to take effect)
echo "🔄 Applying group changes for Docker access (you may need to re-login)..."
newgrp docker

echo "✅ Installation Complete!"
echo "🔑 Access Jenkins at: http://localhost:8080"
