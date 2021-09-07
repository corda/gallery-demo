# Freighter Test Setup
For the freighter tests you must have the following:
	
- Docker installed https://docs.docker.com/desktop/
- Your r3 artifactory username and API key (get api key here https://software.r3.com/ui/admin/artifactory/user_profile)

1. If you are on **Mac OS** you will need to increase your docker memory to 1.5x #nodes being tested. Most tests in this project will use 3 nodes (agent, requester, notary) so a safe number is 6GB+. You can change this setting in DockerDesktop->Preferences->Resources
2. Setup your artifactory credentials (https://engineering.r3.com/docs/developer-tips/add-artifactory-cred-local/)
3. Install docker dependency images by the following commands in terminal
	- `docker pull postgres:9.6`
	- `docker pull roastario/freighter-base:latest`
	- `docker pull roastario/notary-and-network-map:4.0`
4. Run freighterTest task
