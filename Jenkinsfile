// Jenkins Pipeline script for building, testing, and deploying a Java application.
// This script assumes the use of Docker, SonarQube, OWASP Dependency Check, Maven, and Kubernetes.

pipeline {
    agent any

    environment {
        // Define environment variables
        SONAR_TOKEN = credentials('sonarqube-token') // Store SonarQube token in Jenkins credentials
        NEXUS_URL = 'http://nexus.example.com/repository/releases/' // Replace with your Nexus URL
        REPO_URL = 'docker.io' // Replace with the actual ECR repository URL
        APP_NAME = 'redisdemo' // Replace with the name of your ECR application
        IMAGE_REPO = "$REPO_URL/$APP_NAME"
        IMAGE_NAME = "v${env.BUILD_NUMBER}"
    }

    stages {

        stage('Compile Source Code') {
            steps {
                echo 'Compiling Source code...'
                // Compile the source code
                sh 'mvn compile'
            }
        }

        stage('Unit Test') {
            steps {
                echo 'Testing the code...'
                // Run unit tests (skipping tests for now)
                sh 'mvn test -DskipTests=true'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'SonarQube Analysis started...'
                script {
                    // Run SonarQube analysis
                    withSonarQubeEnv('sonarqube') {
                        sh "mvn sonar:sonar -Dsonar.projectKey=devsecops -Dsonar.projectName=devsecops -Dsonar.host.url=http://sonarqube-sonarqube.devops.svc.cluster.local -Dsonar.login=${SONAR_TOKEN} -Dsonar.sources=src/main/java -Dsonar.java.binaries=target/classes"
                
                    }
                }
            }
        }

        stage('Build Source Code') {
            steps {
                echo 'Building Source code...'
                // Build the source code (skipping tests)
                sh 'mvn package -DskipTests=true'
            }
        }

        stage('Build Image with BuildKit') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docklogin', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                sh '''
                    mkdir -p ~/.docker
                    echo '{"auths": {"https://index.docker.io/v1/": {"auth": "'$(echo -n "$DOCKER_USER:$DOCKER_PASS" | base64)'"}}}' > ~/.docker/config.json

                    ./buildctl \
                    --addr tcp://buildkitd.default.svc.cluster.local:1234 \
                    build \
                    --frontend=dockerfile.v0 \
                    --local context=. \
                    --local dockerfile=. \
                    --output type=image,name=myrepo/myapp:latest,push=true
                '''
                }
            }
        }

    }
}