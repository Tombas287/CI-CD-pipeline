def call(Map config = [:]) {
    def pythonVersion = config.pythonVersion ?: 'python3'
    def requirementsFile = config.requirementsFile ?: 'requirements.txt'
    def outputDir = config.outputDir ?: 'dist/'
    def venvDir = config.venvDir ?: 'venv'

    script {
        echo "🐍 Using Python: ${pythonVersion}"
        
        // Ensure Python is installed
        sh "${pythonVersion} --version || { echo '🚨 Python not found!'; exit 1; }"

        // Create virtual environment if not exists
        if (!fileExists(venvDir)) {
            echo "🔧 Creating virtual environment in ${venvDir}..."
            sh "${pythonVersion} -m venv ${venvDir}"
        }

        // Activate virtual environment
        def activateVenv = ". ${venvDir}/bin/activate"

        // Upgrade pip and install dependencies
        sh "${activateVenv} && pip install --upgrade pip setuptools wheel"

        if (fileExists(requirementsFile)) {
            echo "📦 Installing dependencies from ${requirementsFile}..."
            sh "${activateVenv} && pip install -r ${requirementsFile}"
        } else {
            echo "⚠️ No ${requirementsFile} found, skipping dependency installation."
        }

        // Build the package
        echo "🛠️ Building the package..."
        sh "${activateVenv} && python setup.py sdist bdist_wheel"

        // Move artifacts to the specified output directory
        echo "📤 Moving built packages to ${outputDir}"
        sh "mkdir -p ${outputDir} && mv dist/* ${outputDir}"

        echo "✅ Build completed! Artifacts saved in: ${outputDir}"
    }
}
