# Deploy Module - Running Local Nodes

### DeployNodes / Cordform Task

```
./gradlew deployNodes
./deploy/build/nodes/runnodes
```
- Spawns local SINGLE NETWORK node instances with CRaSH interfaces via Xterm.
- Use and modify node blocks, for simple initial Cordapp testing

### Network PrepareDockerNodes / Dockerform Tasks

#### Prereqs:
1. Docker Desktop or Docker *NIX
2. Docker-Compose

```shell
./gradlew prepareAllDockerNodes     # generate config for all networks
./gradlew prepareAuctionDockerNodes # config for Auction Network - Alice, Bob, Charlie
./gradlew prepareGbpDockerNodes     # config for GBP Network - Alice, Bob
./gradlew prepareCbdcDockerNodes    # config for CBDC Network - Alice, Charlie
```
- generates a docker compose file for associated networks

Run the networks

```shell
docker-compose -f ./deploy/build/GBP-Nodes/docker-compose.yml up -d
docker-compose -f ./deploy/build/GBP-Nodes/docker-compose.yml up -d
docker-compose -f ./deploy/build/GBP-Nodes/docker-compose.yml up -d
```