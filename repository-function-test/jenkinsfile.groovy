@Library('jenkins-shared-library@master') _
import junzhang919.ep.share.*
ep_tools = new ep_public_lib()

def generateStage(repo) {
	return {
		stage("Test ${repo}") {
			ep_tools.highlight_info("info", "Test Repo: ${repo}")
			sh """
				cd function_test/${test_env}/repos/${repo}
				bash demo.sh
				exit \$?
			"""
		}
	}
}

pipeline{
	agent {
		label 'slave-artifactory'
	}
	options {
		timestamps()
		timeout(time: 60, unit: 'MINUTES')
	}
	environment {
		GRAB_CREDS = credentials("$credentials_id")
		GRAB_USER = "$GRAB_CREDS_USR"
		GRAB_API_KEY = "$GRAB_CREDS_PSW"
		githubCredId = "jenkins-github"
		test_repo_path = "ep/demo/alpine:1"
	}
	stages{
		stage('Test Docker Pull') {
			steps{
				script{
					sh """
                        set +ex
                        docker pull arti.private-domain/ep/demo/alpine:latest
                        docker tag arti.private-domain/ep/demo/alpine:latest arti.private-domain/ep/demo/alpine:${BUILD_NUMBER}
                    """
					ep_tools.auto_inject_release_env_parameters()
				}
			}
		}

		stage('Test Artifact Plugin'){
			steps {
				script {
					ep_tools.highlight_info("info", "Test Artifact Plugins")
					ARTIFACT_TAG_2 = "arti.private-domain/ep/demo/alpine:${BUILD_NUMBER}"
					ARTIFACTORY = "artiv2_plugin_${test_env}_test"
					ARTIFACT_DOCKER_REPO = "docker-local"
					def server = Artifactory.server ARTIFACTORY
					def rtDocker = Artifactory.docker server: server
					print(server.serverName)

					def buildInfo2 = rtDocker.push("$ARTIFACT_TAG_2", "$ARTIFACT_DOCKER_REPO")
					server.publishBuildInfo(buildInfo2)
					sh """
						grab rt config jfrog
						jfrog rt set-props docker/\$(echo "${test_repo_path}"|sed 's#:#/#') "team=ep;info=${BUILD_URL} test arti;release_version=${DEV_MAJOR_RELEASE}" --include-dirs
						echo "package.0.url=arti.private-domain/ep/demo/alpine:${BUILD_NUMBER}" > ${WORKSPACE}/env.props
						echo "package.subsystem=ep" >> ${WORKSPACE}/env.props
						echo "package.dev.version=$DEV_MAJOR_RELEASE" >> ${WORKSPACE}/env.props
					"""
				}
			}
		}

		stage('Test Common Repo Function'){
			steps {
				script {
					common_repo_list = ['docker', 'generic', 'go', 'helm', 'rpm']
					def parallelStagesMap = common_repo_list.collectEntries {
						["${it}" : generateStage(it)]
					}
					parallel parallelStagesMap
				}
			}
		}
		stage('Test UnCommon Repo Function'){
			steps {
				script {
					uncommon_repo_list = ['debian', 'fw-npm','gems','maven', 'npm', 'pypi', 'sbt']
					def parallelStagesMap = uncommon_repo_list.collectEntries {
						["${it}" : generateStage(it)]
					}
					parallel parallelStagesMap
				}
			}
		}
	}
	post {
		always {
			archiveArtifacts allowEmptyArchive: true, artifacts: 'env.props'
			cleanWs deleteDirs: true, disableDeferredWipeout: true, notFailBuild: true
		}

		success {
			slackSend channel: "#jenkins_slack_test",color: "good", message: "Artifactory Function Test Build PASS! :0-0: <${BUILD_URL}|${BUILD_DISPLAY_NAME}>"
		}
		failure {
			slackSend channel: "#ep-alerts",color: "danger", message: "Artifactory Function Test Build FAIL! :bomb: <${BUILD_URL}|${BUILD_DISPLAY_NAME}>"
		}
	}
}
