package com.r3.gallery.broker.corda.client

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.api.*
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import net.corda.core.contracts.Amount
import net.corda.core.crypto.random63BitValue
import net.corda.core.crypto.secureRandomBytes
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.Arrays.asList

/**
 * REST endpoints for Gallery parties on Auction Network
 *
 * required endpoints:
 *
 */
@CrossOrigin
@RestController
@ConditionalOnProperty(prefix = "mock.controller", name = ["enabled"], havingValue = "true")
@RequestMapping("/")
class MockController {
    companion object {
        private val logger = LoggerFactory.getLogger(MockController::class.java)
        const val TIMEOUT = ConnectionServiceImpl.TIMEOUT
    }

    //--- MOCK GALLERY ENDPOINTS

    @PutMapping("/gallery/issue-artwork")
    fun issueArtwork(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: String
    ) : ResponseEntity<ArtworkOwnership> {
        logger.info("MOCK Request by $galleryParty to issue artwork of id $artworkId")
        return asResponse(
            ArtworkOwnership(
                UUID.randomUUID() as CordaReference,
                UUID.randomUUID() as ArtworkId,
                "O=Alice,L=London,C=GB"
            )
        )
    }

    @GetMapping("/gallery/list-available-artworks")
    fun listAvailableArtworks(
        @RequestParam("galleryParty") galleryParty: ArtworkParty
    ) : ResponseEntity<List<AvailableArtworksResponse>> {
        logger.info("MOCK Request by $galleryParty to list available artworks")
        return asResponse(
            asList(
                AvailableArtworksResponse(
                    UUID.randomUUID() as ArtworkId,
                    "Mock artwork 1",
                    "http://test.com",
                    true,
                    listOf(
                        AvailableArtworksResponse.BidRecord(
                            UUID.randomUUID() as CordaReference,
                            bidderPublicKey = "0xdfe3d63278d3282a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d",
                            bidderDisplayName = "O=Bob,L=San Francisco,C=US",
                            GBP(300),
                            "O=DN Notary,L=London,C=GB",
                            Date(),
                            accepted = false
                        ),
                        AvailableArtworksResponse.BidRecord(
                            UUID.randomUUID() as CordaReference,
                            bidderPublicKey = "0x2b4632d08485ff1df2db55b9dafd23347d1c47a457072a1e87be26896549a873",
                            bidderDisplayName = "O=Bob,L=San Francisco,C=US",
                            GBP(300),
                            "O=DN Notary,L=London,C=GB",
                            Date(),
                            accepted = false
                        )
                    )
                )
            )
        )
    }

    @PutMapping("/gallery/create-artwork-transfer-tx")
    fun createArtworkTransferTx(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestParam("bidderParty") bidderParty: ArtworkParty,
        @RequestParam("artworkId") artworkId: String
    ) : ResponseEntity<UnsignedArtworkTransferTx> {
        // TODO: Is the DTO for ownership to be provided in full? or shall artworkId be used as is here?
        logger.info("MOCK Request to create artwork transfer transaction seller: $galleryParty, bidder: $bidderParty, art: $artworkId")
        return asResponse(UnsignedArtworkTransferTx(secureRandomBytes(8)))
    }

    @PostMapping("/gallery/finalise-artwork-transfer", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun finaliseArtworkTransfer(
        @RequestParam("galleryParty") galleryParty: ArtworkParty,
        @RequestBody unsignedArtworkTransferTx: UnsignedArtworkTransferTx
    ) : ResponseEntity<ProofOfTransferOfOwnership> {
        logger.info("MOCK Request to finalise artwork transfer by $galleryParty for tx: $unsignedArtworkTransferTx")
        return asResponse(
            ProofOfTransferOfOwnership(
                UUID.randomUUID() as CordaReference,
                random63BitValue().toString(),
                TransactionSignature(secureRandomBytes(8)),
                TransactionSignature(secureRandomBytes(8))
            )
        )
    }


    //--- MOCK BIDDER ENDPOINTS

    @PutMapping("/bidder/issue-tokens")
    fun issueTokens(
        @RequestParam("bidderParty") bidderParty: ArtworkParty,
        @RequestParam("amount") amount: Long,
        @RequestParam("currency") currency: String
    ) : ResponseEntity<Unit> {
        logger.info("MOCK Request by $bidderParty to issue tokens for $amount $currency")
        return asResponse(Unit)
    }

    //--- MOCK NETWORK ENDPOINTS

    @GetMapping("/network/balance")
    fun balance(
        @RequestParam("party", required = false) party: ArtworkParty?
    ) : ResponseEntity<List<NetworkBalancesResponse>> {
        logger.info("MOCK Request for balance of parties across network")
        val balances: Map<String, List<NetworkBalancesResponse.Balance>> =
            mapOf(
                Pair("O=Alice,L=London,C=GB", listOf(
                    NetworkBalancesResponse.Balance(
                        GBP.tokenIdentifier,
                        GBP(0),
                        GBP(80)
                    ),
                    NetworkBalancesResponse.Balance(
                        CBDC().tokenIdentifier,
                        Amount(0, CBDC()),
                        Amount(30, CBDC())
                    )
                )),
                Pair("O=Bob,L=San Francisco,C=US", listOf(
                    NetworkBalancesResponse.Balance(
                        GBP.tokenIdentifier,
                        GBP(80),
                        GBP(100)
                    )
                )),
                Pair("O=Charlie,L=Mumbai,C=IN", listOf(
                    NetworkBalancesResponse.Balance(
                        CBDC().tokenIdentifier,
                        Amount(0, CBDC()),
                        Amount(3000, CBDC())
                    )
                ))
            )

        return asResponse(
            if (party != null) { // filter if party provided
                listOf(NetworkBalancesResponse(
                    x500 = party,
                    partyBalances = balances[party]!!
                ))
            } else {
                balances.map {
                    NetworkBalancesResponse(
                        x500 = it.key,
                        partyBalances = it.value
                    )
                }
            }
        )
    }
    class CBDC : TokenType("CBDC", 2)

    @GetMapping("/network/participants")
    fun participants(
        @RequestParam("networks", required = false) networks: List<String>?
    ): ResponseEntity<List<Participant>> {
        logger.info("MOCK Request for all participants")
        return asResponse(
            listOf(
                Participant(
                    displayName = "Alice",
                    x500 = "O=Alice,L=London,C=GB",
                    networkIds = listOf(
                        Participant.NetworkId(
                            CordaRPCNetwork.AUCTION.name,
                            "0xdfe3d63278d3282a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d"
                        ),
                        Participant.NetworkId(
                            CordaRPCNetwork.GBP.name,
                            "0xb4bc263278d3882a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d"
                        ),
                        Participant.NetworkId(
                            CordaRPCNetwork.CBDC.name,
                            "0xb4bc263278d3882a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d"
                        )
                    ),
                    type = Participant.AuctionRole.GALLERY
                ),
                Participant(
                    displayName = "Bob GBP",
                    x500 = "O=Bob,L=San Francisco,C=US",
                    networkIds = listOf(
                        Participant.NetworkId(
                            CordaRPCNetwork.AUCTION.name,
                            "0xd8ec0ba1a63923bbb4f38147fb8a943da26ddfe3d63278d3282a652a8d73a6bf"
                        ),
                        Participant.NetworkId(
                            CordaRPCNetwork.GBP.name,
                            "0x82a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26db4bc263278d38"
                        )
                    ),
                    type = Participant.AuctionRole.BIDDER
                ),
                Participant(
                    displayName = "Charlie CBDC",
                    x500 = "O=Charlie,L=Mumbai,C=IN",
                    networkIds = listOf(
                        Participant.NetworkId(
                            CordaRPCNetwork.AUCTION.name,
                            "0xa8d73a6bfd8ec0ba1a63923bbb4f38147fdfe3d63278d3282a652b8a943da26d"
                        ),
                        Participant.NetworkId(
                            CordaRPCNetwork.CBDC.name,
                            "0xb4bc2ba1a63923bbb4f38147fb8a943da26d63278d3882a652a8d73a6bfd8ec0"
                        )
                    ),
                    type = Participant.AuctionRole.BIDDER
                )
            )
        )
    }

    /**
     * Log returns progressUpdates for Node Level state-machine updates
     */
    @GetMapping("/network/log")
    fun log(): ResponseEntity<List<LogUpdateEntry>> {
        logger.info("MOCK Request for logs")
        return asResponse(
            listOf(
                LogUpdateEntry(
                    associatedFlow = "com.r3.gallery.workflows.IssuedArtworkFlow",
                    network = CordaRPCNetwork.AUCTION.name,
                    x500 = "O=Alice,L=London,C=GB",
                    logRecordId =  "d145ac7f-6e16-49dc-99c2-f72ca0f39eb01",
                    timestamp = "15:17:08.132263",
                    message =  "[<Locked>300 GBP|83HFJKF8736YHG09SDJ] <- [Wallet|SDF7SDF8G9H00ME8569]"
                ),
                LogUpdateEntry(
                    associatedFlow = "com.r3.gallery.workflows.BidOnArtworkFlow",
                    network = CordaRPCNetwork.GBP.name,
                    x500 = "O=Bob,L=San Francisco,C=US",
                    logRecordId =  "d145ac7f-6e16-49dc-99c2-f72ca0f39eb02",
                    timestamp = "16:17:08.132333",
                    message = "Something has happened here on [83HFJKF8736YHG09SDJ]"
                ),
                LogUpdateEntry(
                    associatedFlow = "com.r3.gallery.workflows.BidOnArtworkFlow",
                    network = CordaRPCNetwork.CBDC.name,
                    x500 = "O=Charles,L=London,C=GB",
                    logRecordId =  "e555ac7f-6e16-49dc-99c2-f39eb01f72ca0",
                    timestamp = "19:17:13.163322",
                    message =  "[<Unlocked>300 GBP|83HFJKF8736YHG09SDJ] -> [Wallet|SDF7SDF8G9H00ME8569]"
                ),
                LogUpdateEntry(
                    associatedFlow = "com.r3.gallery.workflows.FinaliseArtworkTransfer",
                    network = CordaRPCNetwork.AUCTION.name,
                    x500 = "O=Alice,L=London,C=GB",
                    logRecordId =  "ac7fd145-6e16-49dc-99c2-f39eb2ca001f7",
                    timestamp = "15:17:08.132263",
                    message =  "[<Locked>300 GBP|83HFJKF8736YHG09SDJ] <- [Wallet|SDF7SDF8G9H00ME8569]"
                ),
            )
        )
    }
}