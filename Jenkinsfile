pipeline {
    agent {
        label 'freebsd&&kotlin'
    }

    triggers {
        pollSCM ''
    }

    tools {
        maven 'Maven 3.5.4'
    }

    options {
        ansiColor('xterm')
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')
    }

    stages {
        stage('clean') {
            steps {
                sh 'mvn -B clean'
            }
        }

        stage('build') {
            steps {
                sh 'mvn -B compile'
            }
        }

        stage('test') {
            steps {
                sh 'mvn -B test'
            }
        }

        stage('deploy') {
            when {
                branch "release/v*"
            }

            steps {
                sh 'mvn versions:set -DnewVersion=${BRANCH_NAME#release/v}'
                configFileProvider([configFile(fileId: '96a603cc-e1a4-4d5b-a7e9-ae1aa566cdfc', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh 'mvn -B -s "$MAVEN_SETTINGS_XML" -DskipTests deploy'
                }
                script {
                    def version = env.BRANCH_NAME - 'release/v'
                    step([$class: "RundeckNotifier",
                          includeRundeckLogs: true,
                          jobId: "21c1a435-3931-4cf8-7ec-1d61609be089",
                          options: "version=$version",
                          rundeckInstance: "gizmo",
                          shouldFailTheBuild: true,
                          shouldWaitForRundeckJob: true,
                          tailLog: true])
                }
            }
        }
    }

    post {
        always {
            mail to: "rafi@guengel.ch",
                    subject: "${JOB_NAME} (${BRANCH_NAME};${env.BUILD_DISPLAY_NAME}) -- ${currentBuild.currentResult}",
                    body: "Refer to ${currentBuild.absoluteUrl}"
        }
    }
}
