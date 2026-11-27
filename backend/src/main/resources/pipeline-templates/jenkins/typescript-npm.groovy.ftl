pipeline {
    agent {
        docker { image 'node:[=languageVersion]' }
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

        stage('Build') {
            steps {
                sh '''
                    if [ -f package-lock.json ] || [ -f npm-shrinkwrap.json ]; then
                        npm ci
                    else
                        npm install
                    fi

                    npm run build --if-present
                '''
            }
        }
<#if hasTests>

        stage('Test') {
            steps {
                sh 'npm run test --if-present'
            }
        }
</#if>
    }

}