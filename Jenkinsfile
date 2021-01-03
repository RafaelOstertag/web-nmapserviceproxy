pipeline {
    agent {
        label 'freebsd&&kotlin'
    }

    triggers {
        pollSCM '@daily'
        cron '@daily'
    }

    tools {
        maven 'Latest Maven'
    }

    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Build & Test') {
            steps {
                sh 'mvn -B verify'
            }

            post {
                always {
                    junit '**/failsafe-reports/*.xml,**/surefire-reports/*.xml'
                }
            }
        }

        stage('Sonarcloud') {
            steps {
                withSonarQubeEnv(installationName: 'Sonarcloud', credentialsId: 'e8795d01-550a-4c05-a4be-41b48b22403f') {
                    sh label: 'sonarcloud', script: "mvn -Dsonar.branch.name=${env.BRANCH_NAME} $SONAR_MAVEN_GOAL"
                }
            }
        }

        stage("Check Dependencies") {
            steps {
                sh 'mvn -Psecurity-scan dependency-check:check'
                dependencyCheckPublisher failedTotalCritical: 1, failedTotalHigh: 5, failedTotalLow: 8, failedTotalMedium: 8, pattern: '**/dependency-check-report.xml', unstableTotalCritical: 0, unstableTotalHigh: 4, unstableTotalLow: 8, unstableTotalMedium: 8
            }
        }

        stage('Deploy to Nexus') {
            when {
                branch "master"
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                configFileProvider([configFile(fileId: '96a603cc-e1a4-4d5b-a7e9-ae1aa566cdfc', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" -DskipTests deploy'
                }
            }
        }

        stage('Build & Push Docker Image') {
            agent {
                label "arm64&&docker"
            }

            environment {
                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
            }

            when {
                branch 'master'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                sh "docker build --build-arg 'VERSION=${env.VERSION}' -t rafaelostertag/nmap-service-proxy:${env.VERSION} docker"
                withCredentials([usernamePassword(credentialsId: '750504ce-6f4f-4252-9b2b-5814bd561430', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    sh 'docker login --username "$USERNAME" --password "$PASSWORD"'
                    sh "docker push rafaelostertag/nmap-service-proxy:${env.VERSION}"
                }
            }
        }

        stage('Deploy to k8s') {
            agent {
                label "helm"
            }

            environment {
                VERSION = sh returnStdout: true, script: "mvn -B help:evaluate '-Dexpression=project.version' | grep -v '\\[' | tr -d '\\n'"
            }

            when {
                branch 'master'
                not {
                    triggeredBy "TimerTrigger"
                }
            }

            steps {
                withKubeConfig(credentialsId: 'a9fe556b-01b0-4354-9a65-616baccf9cac') {
                    sh "helm upgrade -n portscanner -i --set image.tag=${env.VERSION} nmapserviceproxy helm/nmapserviceproxy"
                }
            }
        }
    }

    post {
        unsuccessful {
            mail to: "rafi@guengel.ch",
                    subject: "${JOB_NAME} (${BRANCH_NAME};${env.BUILD_DISPLAY_NAME}) -- ${currentBuild.currentResult}",
                    body: "Refer to ${currentBuild.absoluteUrl}"
        }
    }
}
