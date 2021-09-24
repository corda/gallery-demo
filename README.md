[//]: # (TODO: Update with diagrams and latest arch description and module mapping)
[//]: # (TODO: Git squash redundant commit msgs)

# gallery-demo

A demo of swaps in an auction scenario with three distinct Corda Networks

### Participants: 

---

- Alice: an art seller
- Bob: buyer with CBDC consideration
- Charlie: buyer with GBP (fiat) consideration

### Networks and Identities:

---

#### Art/Auction Network:
 - Alice (as dealer)
 - Bob (buyer)
 - Charlie (buyer)

#### Consideration Network #1 - CBDC
 - Alice (payee account)
 - Bob (payer account)

#### Consideration Network #2 - GBP
 - Alice (payee account)
 - Charlie (payer account)

### Modules:

---

**gallery-contracts** - ContractState, Contract, and asset def/models

**gallery-workflows** - Business logic and services

**buildSrc** - Docker and K8s deployment builder classes

**deploy** - Local Cordformation configuration/tasks, Azure K8s configuration and Dockerfiles

**frontend** - Frontend sources and associated proxies

**spring-api** - Spring api controller and orchestration services

**freighter-tests** - Docker based e2e testing module
  - You must add the following images to your local docker cache to run `freighterTest` gradle task locally.
    - `docker pull postgres:9.6`
    - `docker pull roastario/freighter-base:latest`
    - `docker pull roastario/notary-and-network-map:4.0`