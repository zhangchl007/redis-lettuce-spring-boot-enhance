// Jenkins Pipeline script for building, testing, and deploying a Java application.

pipeline {
    
    agent any

    environment {
        // Define environment variables
        SONAR_TOKEN = credentials('sonarqube-token') // Store SonarQube token in Jenkins credentials
        NEXUS_URL = 'http://nexus.example.com/repository/releases/' // Replace with your Nexus URL
        REPO_URL = 'docker.io/zhangchl007' // Replace with your Docker repository URL
        APP_NAME = 'redisdemo' // Replace with your application name
        IMAGE_REPO = "$REPO_URL/$APP_NAME"
        IMAGE_TAG = "v2${env.BUILD_NUMBER}"
        NEXUS_RELEASE_REPO = 'redisdemo-release' // Replace with your Nexus release repository
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

        stage('SonarQube Quality Gate') {
            steps {
                script {
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                        error "Pipeline aborted due to quality gate failure: ${qg.status}"
                    }
                }
            }
        }

        stage('Publish to Nexus') {
            steps {
                echo 'Publishing to Nexus...'
                withCredentials([usernamePassword(credentialsId: 'nexus-auth', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                    sh """
                        mvn deploy:deploy-file \\
                            -DgroupId=com.rkdevblog \\
                            -DartifactId=${APP_NAME} \\
                            -Dversion=${env.BUILD_NUMBER} \\
                            -Dpackaging=jar \\
                            -Dfile=target/${APP_NAME}-0.0.1-SNAPSHOT.jar \\
                            -DrepositoryId=${NEXUS_RELEASE_REPO} \\
                            -Durl=${NEXUS_URL} \\
                            -Dusername=${NEXUS_USER} \\
                            -Dpassword=${NEXUS_PASS}
                    """
                }
            }
        }

        stage('Build Image with BuildKit') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docklogin', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        mkdir -p ~/.docker
                        echo '{"auths": {"https://index.docker.io/v1/": {"auth": "'\$(echo -n "\$DOCKER_USER:\$DOCKER_PASS" | base64)'"}}}' > ~/.docker/config.json
                        buildctl \\
                        --addr tcp://buildkitd-non-tls.jenkins.svc.cluster.local:1234 \\
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