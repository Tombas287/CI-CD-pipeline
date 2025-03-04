/**
 * Python Pip Builder - Installs dependencies and builds a package
 * 
 * @param pythonVersion (Optional) - Python version (default: python3)
 * @param requirementsFile (Optional) - Path to requirements.txt (default: requirements.txt)
 * @param outputDir (Optional) - Directory for built packages (default: dist/)
 */
def call(Map config = [:]) {
    def pythonVersion = config.pythonVersion ?: 'python3'
    def requirementsFile = config.requirementsFile ?: 'requirements.txt'
    def outputDir = config.outputDir ?: 'dist/'

    script {
        echo "🐍 Using Python: ${pythonVersion}"
        
        // Check if Python is installed
        sh "${pythonVersion} --version || { echo '🚨 Python not found!'; exit 1; }"

        // Install dependencies
        if (fileExists(requirementsFile)) {
            echo "📦 Installing dependencies from ${requirementsFile}..."
            sh "${pythonVersion} -m pip install --upgrade pip"
            sh "${pythonVersion} -m pip install -r ${requirementsFile}"
        } else {
            echo "⚠️ No ${requirementsFile} found, skipping dependency installation."
        }

        // Build the package
        echo "🛠️ Building the package..."
        sh "${pythonVersion} -m pip install setuptools wheel"
        sh "${pythonVersion} setup.py sdist bdist_wheel"

        // Move artifacts to Jenkins workspace
        echo "📤 Moving built packages to ${outputDir}"
        sh "mkdir -p ${outputDir} && mv dist/* ${outputDir}"

        echo "✅ Build completed! Artifacts saved in: ${outputDir}"
    }
}
