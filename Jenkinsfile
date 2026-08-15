// =============================================================================
// Course-End Project 2 — CI/CD Pipeline for the Pet Clinic application
//
// Builds the Spring Boot PetClinic app with Maven and deploys the resulting WAR
// to a standalone Tomcat server running on the same EC2 instance as Jenkins.
//
// Pipeline: Checkout -> Build -> Test -> Package -> Deploy -> Verify
//
// Prerequisites on the EC2 instance (see setup-jenkins-ec2.sh):
//   - Jenkins on port 8090, Tomcat on port 8080
//   - Maven + JDK installed
//   - The 'jenkins' user is in the 'tomcat' group with write access to webapps/
// =============================================================================

pipeline {
    agent any

    environment {
        // Deployment target. Tomcat auto-deploys anything dropped in webapps/.
        TOMCAT_HOME = '/opt/tomcat'
        WAR_NAME    = 'petclinic.war'
        APP_CONTEXT = 'petclinic'

        // Keep Maven's repo inside the workspace-independent Jenkins home so it
        // survives between builds (otherwise every build re-downloads the world).
        MAVEN_OPTS  = '-Dmaven.repo.local=/var/lib/jenkins/.m2/repository'

        // These two integration tests require Docker/Docker-Compose-backed
        // databases. Excluded so the build stays fast and deterministic; the
        // remaining ~40 unit/slice tests still run on every commit.
        SKIP_DB_ITS = '-Dtest=!MySqlIntegrationTests,!PostgresIntegrationTests -Dsurefire.failIfNoSpecifiedTests=false'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {

        stage('Checkout') {
            steps {
                echo "Cloning source from Git..."
                checkout scm
                sh 'git log -1 --pretty=format:"Building commit %h by %an: %s"'
            }
        }

        stage('Build') {
            steps {
                echo "Compiling the application..."
                sh 'mvn -B clean compile'
            }
        }

        stage('Test') {
            steps {
                echo "Running unit tests..."
                sh "mvn -B test ${SKIP_DB_ITS}"
            }
            post {
                always {
                    // Surface test results in the Jenkins UI even when they fail.
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo "Packaging as a WAR..."
                sh "mvn -B package -DskipTests"
                sh 'ls -lh target/*.war'
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                echo "Deploying ${WAR_NAME} to local Tomcat..."
                sh """
                    # Remove the previously exploded directory so Tomcat does a
                    # clean redeploy rather than merging into stale classes.
                    rm -rf ${TOMCAT_HOME}/webapps/${APP_CONTEXT}
                    rm -f  ${TOMCAT_HOME}/webapps/${WAR_NAME}

                    cp target/${WAR_NAME} ${TOMCAT_HOME}/webapps/${WAR_NAME}
                    echo "WAR copied to ${TOMCAT_HOME}/webapps/"
                    ls -lh ${TOMCAT_HOME}/webapps/
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                echo "Waiting for Tomcat to auto-deploy and the app to answer..."
                sh """
                    # Spring Boot needs a moment to start inside Tomcat. Poll
                    # rather than sleeping a fixed amount so a fast start is not
                    # penalised and a slow one is not falsely failed.
                    for i in \$(seq 1 30); do
                        CODE=\$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/${APP_CONTEXT}/ || echo 000)
                        if [ "\$CODE" = "200" ]; then
                            echo "Application is UP (HTTP 200) after \$((i*5))s"
                            exit 0
                        fi
                        echo "  attempt \$i: HTTP \$CODE - waiting..."
                        sleep 5
                    done
                    echo "Application did not return HTTP 200 within 150s"
                    tail -50 ${TOMCAT_HOME}/logs/catalina.out || true
                    exit 1
                """
            }
        }
    }

    post {
        success {
            echo "SUCCESS - Pet Clinic deployed."
            echo "Access it at: http://<EC2-PUBLIC-IP>:8080/${APP_CONTEXT}/"
        }
        failure {
            echo "FAILED - see the stage log above for the first error."
        }
        always {
            echo "Build #${env.BUILD_NUMBER} finished with status: ${currentBuild.currentResult}"
        }
    }
}
