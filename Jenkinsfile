// filepath: /home/zhangchl007/azure/aks/gitops/argocd-practice/redis-lettuce-spring-boot-enhance/Jenkinsfile
pipeline {

    agent {
        kubernetes {
            label 'jenkins-agent'
        }
    }
    
    environment {
        SONARQUBE_ENV = 'sonarqube'
        SONAR_TOKEN = credentials('sonarqube-token') // Store SonarQube token in Jenkins credentials
        NEXUS_URL = 'nexus-service.devops.svc.cluster.local' // Replace with your Nexus URL
        REPO_URL = 'docker.io/zhangchl007' // Replace with your Docker repository URL
        APP_NAME = 'redisdemo' // Replace with your application name
        IMAGE_REPO = "$REPO_URL/$APP_NAME"
        IMAGE_TAG = "v2${env.BUILD_NUMBER}-prod"
        NEXUS_RELEASE_REPO = 'redisdemo-release' // Replace with your Nexus release repository
    }

    stages {
        stage('Parallel Tasks') {
            parallel {
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
                        withSonarQubeEnv("${SONARQUBE_ENV}") {
                            echo 'Running SonarQube Analysis...'
                            sh 'mvn sonar:sonar -Dsonar.projectKey=devsecops -Dsonar.projectName=devsecops -Dsonar.host.url=http://sonarqube-sonarqube.devops.svc.cluster.local:9000 -Dsonar.login=${SONAR_TOKEN} -Dsonar.sources=src/main/java -Dsonar.java.binaries=target/classes'
                        }
                    }
                }
                
                stage('Build Source Code') {
                    steps {
                        echo 'Building Source code...'
                        sh 'mvn clean install'
                    }
                }
            }
        }
        stage('SonarQube Quality Gate') {
            steps {
                script {
                    timeout(time: 10, unit: 'MINUTES') {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to SonarQube quality gate failure: ${qg.status}"
                        }
                    }
                }
            }
        }
        stage('Publish to Nexus') {
            steps {
                echo 'Publishing to Nexus...'
                script {
                    def readPomVersion = readMavenPom file: 'pom.xml'
                    nexusArtifactUploader (
                        nexusVersion: 'nexus3', 
                        protocol: 'http', 
                        nexusUrl: "${NEXUS_URL}", 
                        repository: "${NEXUS_RELEASE_REPO}",
                        groupId: 'com.rkdevblog', 
                        version: "${readPomVersion.version}",
                        credentialsId: 'nexus-auth', 
                        artifacts: [
                            [
                                artifactId: "${APP_NAME}", 
                                classifier: '',
                                file: "target/${APP_NAME}-${readPomVersion.version}.jar",
                                type: 'jar'
                            ]
                        ]
                    )
                }
            }
        }
        stage('Approval to Deploy to Production') {
            steps {
                input 'Approve deployment to production?'
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