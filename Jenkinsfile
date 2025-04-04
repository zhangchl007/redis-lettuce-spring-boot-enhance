// Jenkins Pipeline script for building, testing, and deploying a Java application.

pipeline {
    
    agent any

    environment {
        // Define environment variables
        SONAR_TOKEN = credentials('sonarqube-token') // Store SonarQube token in Jenkins credentials
        NEXUS_URL = 'http://nexus.example.com/repository/releases/' // Replace with your Nexus URL
        REPO_URL = 'docker.io/zhangchl' // Replace with the actual ECR repository URL
        APP_NAME = 'redisdemo' // Replace with the name of your ECR application
        IMAGE_REPO = "$REPO_URL/$APP_NAME"
        IMAGE_TAG = "v${env.BUILD_NUMBER}"
    }

    stages {

        stage('Compile Source Code') {
            steps {
                echo 'Compiling Source code...'
                sh 'mvn compile'
            }
        }

        stage('Unit Test') {
            steps {
                echo 'Testing the code...'
                sh 'mvn test -DskipTests=true'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'SonarQube Analysis started...'
                // Directly call Maven sonar:sonar with required properties
                sh "mvn sonar:sonar -Dsonar.projectKey=devsecops -Dsonar.projectName=devsecops -Dsonar.host.url=http://sonarqube-sonarqube.devops.svc.cluster.local:9000 -Dsonar.login=${SONAR_TOKEN} -Dsonar.sources=src/main/java -Dsonar.java.binaries=target/classes"
            }
        }

        stage('Build Source Code') {
            steps {
                echo 'Building Source code...'
                sh 'mvn package -DskipTests=true'
            }
        }

        stage('Build Image with BuildKit') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docklogin', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        mkdir -p ~/.docker
                        echo '{"auths": {"https://index.docker.io/v1/": {"auth": "'\$(echo -n "\$DOCKER_USER:\$DOCKER_PASS" | base64)'"}}}' > ~/.docker/config.json

                        ./buildctl \\
                        --addr tcp://buildkitd.default.svc.cluster.local:1234 \\
                        build \\
                        --frontend=dockerfile.v0 \\
                        --local context=. \\
                        --local dockerfile=. \\
                        --output type=image,name=${IMAGE_REPO}:${IMAGE_TAG},push=true
                    """
                }
            }
        }
    }
}