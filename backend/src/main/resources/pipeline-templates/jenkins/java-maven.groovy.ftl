<#-- Шаблон: Jenkins (декларативный Jenkinsfile) / Java / Maven.
     Переменные: languageVersion, branch, hasTests, hasDockerfile, frameworks (см. README.md рядом).
     Подстановка — [=имя]; конструкции Groovy вида ${env.X} остаются как есть. -->
pipeline {
    agent {
        docker { image 'maven:3.9-eclipse-temurin-[=languageVersion]' }
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B package -DskipTests'
            }
        }
<#if hasTests>

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }
</#if>
<#if hasDockerfile>

        stage('Docker') {
            when { branch '[=branch]' }
            steps {
                sh "docker build -t app:${env.BUILD_NUMBER} ."
            }
        }
</#if>
    }
}
