/**
 * Python Pip Builder - Installs dependencies and builds a package inside a virtual environment (venv)
 * 
 * @param pythonVersion (Optional) - Python version (default: python3)
 * @param requirementsFile (Optional) - Path to requirements.txt (default: requirements.txt)
 * @param outputDir (Optional) - Directory for built packages (default: dist/)
 * @param venvDir (Optional) - Virtual environment directory (default: venv/)
 */
def call(Map config = [:]) {
    def pythonVersion = config.pythonVersion ?: 'python3'
    def requirementsFile = config.requirementsFile ?: 'requirements.txt'
    def outputDir = config.outputDir ?: 'dist/'
    def venvDir = config.venvDir ?: 'venv/'

    script {
        echo "🐍 Using Python: ${pythonVersion}"

        // Check if Python is installed
        sh "${pythonVersion} --version || { echo '🚨 Python not found!'; exit 1; }"

        // Create virtual environment
        echo "🔹 Creating virtual environment in ${venvDir}..."
        sh "${pythonVersion} -m venv ${venvDir}"

        // Activate virtual environment
        def activateCmd = ". ${venvDir}/bin/activate"

        // Install dependencies
        if (fileExists(requirementsFile)) {
            echo "📦 Installing dependencies from ${requirementsFile}..."
            sh "${activateCmd} && pip install --upgrade pip"
            sh "${activateCmd} && pip install -r ${requirementsFile}"
        } else {
            echo "⚠️ No ${requirementsFile} found, skipping dependency installation."
        }

        // Build the package inside venv
        echo "🛠️ Building the package..."
        sh "${activateCmd} && pip install setuptools wheel"
        sh "${activateCmd} && python setup.py sdist bdist_wheel"

        // Move artifacts to Jenkins workspace
        echo "📤 Moving built packages to ${outputDir}"
        sh "mkdir -p ${outputDir} && mv dist/* ${outputDir}"

        echo "✅ Build completed! Artifacts saved in: ${outputDir}"
    }
}
