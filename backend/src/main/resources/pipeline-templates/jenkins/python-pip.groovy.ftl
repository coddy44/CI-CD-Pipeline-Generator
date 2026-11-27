pipeline {
    agent {
        docker { image 'python:[=languageVersion]' }
    }

    options {
        skipDefaultCheckout()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install') {
            steps {
                sh '''
                    if [ -f requirements.txt ]; then
                        python -m pip install -r requirements.txt
                    else
                        python -m pip install .
                    fi
                '''
            }
        }
<#if hasTests>

        stage('Test') {
            steps {
                sh 'python -m pip install pytest'
                sh 'python -m pytest'
            }
        }
</#if>
    }
}