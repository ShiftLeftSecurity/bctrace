#!groovy

env.REPO_NAME="github.com/ShiftLeftSecurity/bctrace"
env.REGISTRY_NAME="docker.dev.sltr.io"

def my_job="${env.JOB_NAME}"
if (my_job == "CI-MASTER-BCTrace-TAG") {
    my_tag="${env.RELEASE_TAG}"
} else {
    my_tag = null
    properties([disableConcurrentBuilds(), pipelineTriggers([pollSCM('H/3 * * * *')])])
}

node {
	if (my_tag) {
        	git poll: false, url: "ssh://git@${env.REPO_NAME}"
	} else {
        	git poll: true, url: "ssh://git@${env.REPO_NAME}"
	}
        try {
            stage('cleanUp') {
                try {
                        deleteDir()
                } catch (err) {
                        println("WARNING: Failed to delete directory: " + err)
                }
            }
            stage('getSrc') { // for display purposes
            // Get code from GitHub repository
		if (my_tag) {
			echo "Building on master branch with tag ${my_tag}"
                	checkout([$class: 'GitSCM', branches: [[name: "refs/tags/${my_tag}"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '4b3482c3-735f-4c31-8d1b-d8d3bd889348', url: "ssh://git@${env.REPO_NAME}"]]])
		} else {
			echo "Building on master branch with NO tag"
                	checkout([$class: 'GitSCM', branches: [[name: "*/master"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '4b3482c3-735f-4c31-8d1b-d8d3bd889348', url: "ssh://git@${env.REPO_NAME}"]]])
		}
            }
            stage('preBuild') {
            }
            stage('runBuild') {
		withEnv(["JAVA_HOME=${ tool 'JDK8u121' }","PATH+MAVEN=${tool 'Maven-3.3.9'}/bin:${env.JAVA_HOME}/bin"]) {
			sh "mvn compile package deploy"
 		}
            }
            stage('archiveBuild') {
                archiveArtifacts '/**/target/*.jar'
            }
	    stage('runTests') {
	    }
            stage('checkFixed') {
                myBuildNumber = sh(returnStdout: true, script: 'echo $(($BUILD_NUMBER-1))').trim()
                withEnv(["PREV_BUILD_NUMBER=${myBuildNumber}"]) {
                        myResult = sh(returnStatus: true, script: 'curl --silent --user admin:1ea44cdc86eefbf888dc2d480a9c9493 http://localhost:8080/job/$JOB_NAME/$PREV_BUILD_NUMBER/api/json | grep -q \"FAILURE\"')
                }
                if (myResult) {
                // placeholder for future use cases
                } else {  // send fixed email
                        slackSend (channel: '#team-java-runtime', color: '#22FF00', message: "FIXED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                        emailext body: 'Build URL: $BUILD_URL (to view full results, click on "Console Output")', attachLog: true, recipientProviders: [[$class: 'CulpritsRecipientProvider']], subject: 'Notice: Jenkins $JOB_NAME #$BUILD_NUMBER FIXED!', to: 'build-notify-java-runtime@shiftleft.io'
                }
             }
        } catch (e) {
                currentBuild.result = "FAILED"
                notifyFailed()
        }
}

def notifyFailed() {
        slackSend (channel: '#team-java-runtime', color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        emailext body: 'Build URL: $BUILD_URL (to view full results, click on "Console Output")', attachLog: true, recipientProviders: [[$class: 'CulpritsRecipientProvider']], subject: 'Action Required: Jenkins $JOB_NAME #$BUILD_NUMBER FAILED', to: 'build-notify-java-runtime@shiftleft.io'
}
