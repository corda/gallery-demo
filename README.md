# gallery-demo

A demo of swaps in an auction scenario with three distinct Corda Networks. This worked example is an implementation of the design presented at [CordaCon2021](https://www.cordacon.com/agenda/session/630202). The slides from this presentation outlining the architecture are available here in the [repo](Cross%20Chain%20Swaps%20-%20CordaCon2021.pdf).

The example simulates an Auction scenario with potentially distrusting parties spread across distinct Cordapp domains (Corda Networks). For brevity, the example uses a single Cordapp to encapsulate the artwork and consideration states (ArtworkState, TokenType-GBP, TokenType-CBDC) and their associated contracts and flows; additionally, a single Spring/Orchestration layer is used to coordinate and execute cross-network interactions between an organization's Auction Network identity, and their associated consideration network identity.

In a more "REAL" deployment you can imagine the structure of the total solution would include three distinct Cordapps (Auction, GBP, CBDC) as well as individual off-ledger orchestration services managed, and controlled exclusively by each party.

The key to this 'simulation' is that we have limited these actions to operate completely independently and disjoint from each other. Whereby, the Gallery can only receive a result or instruction from its own identity on another network, and the same holds for the Bidding parties. Additionally, in the Cordapp a ContractState and its associated flows cannot transact or interact directly with another ContractState or its flows. Any evolutions based on data from an asset represented on another network will receive its instruction or payload via the orchestration service triggered from the source network.
### Participants: 

---

- Alice: an art seller
- Bob: buyer with CBDC consideration
- Charlie: buyer with GBP (fiat) consideration

### Networks and Identities:

---

#### Art/Auction Network:
 - Alice (as Gallery)
 - Bob (Bidder)
 - Charlie (Bidder)

#### Consideration Network #1 - CBDC
 - Alice (Seller)
 - Bob (Buyer)

#### Consideration Network #2 - GBP
 - Alice (Seller)
 - Charlie (Buyer)

** Note: That each identity across distinct networks with the same identifier is owned/controlled by the same organization. The 

### Modules:

---

**gallery-contracts** - ContractState, Contract, and asset def/models

**gallery-workflows** - Business logic and services

**buildSrc** - Docker and K8s deployment builder classes

**deploy** - Local Cordformation configuration/tasks, Azure K8s configuration and Dockerfiles

**frontend** - Frontend sources and associated proxies

**spring-api** - Spring api controller and orchestration services

## Running the Demo

---

Warning: Because the solution requires three separate networks running in parallel, we recommend for best performance to use a machine which has **32GB** of Ram. You may still run the demo with less, but if you encounter problems you may have to adjust or tweak your docker resource settings or additionally
reinitialize a downed node periodically.

### Prerequisites
- Docker and docker-compose installed. See /deploy/[README.md](/deploy/README.md) for further details.
- Java 1.8

### Network Initialization Steps (can be executed from Gradle pane in IntelliJ or via command-line)
1. `./gradlew prepareAllDockerNodes`
    1. Generates persistent node file structures and docker-compose yaml configuration.
2. `./gradlew allNetworksUp`
    1. Brings up Auction, GBP, and CBDC networks locally via docker-compose.
3. `./gradlew runSpringApp`
    1. Launches Spring Orchestration service and REST controller.

### Launching Frontend

See /frontend/galleryapp/[README.md](/frontend/galleryapp/README.md)
