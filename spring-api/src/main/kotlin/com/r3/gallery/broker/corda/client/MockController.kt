package com.r3.gallery.broker.corda.client

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.GBP
import com.r3.gallery.api.*
import com.r3.gallery.api.AvailableArtwork.BidRecord
import com.r3.gallery.broker.corda.rpc.service.ConnectionServiceImpl
import net.corda.core.contracts.Amount
import net.corda.core.crypto.secureRandomBytes
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * TODO: Refresh this class
 * MOCK REST endpoints for Gallery parties on Auction Network
 *
 * See application.properties, mock.controller.enabled property for setting this controller to active.
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

//    //--- MOCK GALLERY ENDPOINTS
//
//    @PutMapping("/gallery/issue-artwork")
//    fun issueArtwork(
//        @RequestParam("galleryParty") galleryParty: ArtworkParty,
//        @RequestParam("artworkId") artworkId: String
//    ) : ResponseEntity<ArtworkOwnership> {
//        logger.info("MOCK Request by $galleryParty to issue artwork of id $artworkId")
//        return asResponse(
//            ArtworkOwnership(
//                UUID.fromString("7be3fd81-f293-40f9-be8b-5e341d20639a") as CordaReference,
//                UUID.fromString("1b7c6f62-0add-4c1e-bb54-ad22829c59c2") as ArtworkId,
//                "O=Alice, L=London, C=GB"
//            )
//        )
//    }
//
//    @GetMapping("/gallery/list-available-artworks")
//    fun listAvailableArtworks(
//        @RequestParam("galleryParty", required = false) galleryParty: ArtworkParty?
//    ) : ResponseEntity<List<AvailableArtwork>> {
//        logger.info("MOCK Request by $galleryParty to list available artworks")
//        return asResponse(
//            listOf(
//                AvailableArtwork(
//                    artworkId = UUID.fromString("d9ecd08f-2b65-4ad8-921b-e0b48790e17f") as ArtworkId,
//                    description = "AppleMan",
//                    "https://thumbor.forbes.com/thumbor/960x0/https%3A%2F%2Fblogs-images.forbes.com%2Fchaddscott%2Ffiles%2F2018%2F10%2F12.-Son-of-Man-1200x1575.jpg",
//                    true,
//                    expiryDate = Date(Calendar.getInstance().apply { set(2021,11,31) }.timeInMillis),
//                    listOf(
//                        BidRecord(
//                            cordaReference = "d1364c88-55d8-47c2-9587-079aca2caf7e",
//                            bidderPublicKey = "0xdfe3d63278d3282a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d",
//                            bidderDisplayName = "Bob",
//                            amountAndCurrency = GBP(300),
//                            notary = "O=GBP Notary, L=London, C=GB",
//                            accepted = true
//                        ),
//                        BidRecord(
//                            cordaReference = "446404fb-e093-43e2-9664-9555bd8497ff",
//                            bidderPublicKey = "0x2b4632d08485ff1df2db55b9dafd23347d1c47a457072a1e87be26896549a873",
//                            bidderDisplayName = "Charles",
//                            amountAndCurrency = Amount(299, CBDC()),
//                            notary = "O=CBDC Notary, L=London, C=GB",
//                            accepted = false
//                        )
//                    )
//                ),
//                AvailableArtwork(
//                    artworkId = UUID.fromString("12d7859a-f425-47d4-9c12-aa3700efd963") as ArtworkId,
//                    description = "Summer set on the Beach",
//                    url = "https://render.fineartamerica.com/images/rendered/default/print/8/6.5/break/images-medium-5/summerset-sailboats-paul-brent.jpg",
//                    listed = true,
//                    expiryDate = Date(Calendar.getInstance().apply { set(2021,11,31) }.timeInMillis),
//                    bids = listOf(
//                        BidRecord(
//                            cordaReference = "b80e93dd-1a6b-4678-81fe-84b27acdd951",
//                            bidderPublicKey = "0x2b4632d08485ff1df2db55b9dafd23347d1c47a457072a1e87be26896549a873",
//                            bidderDisplayName = "Charles",
//                            amountAndCurrency = Amount(3999, CBDC()),
//                            notary = "O=CBDC Notary, L=London, C=GB",
//                            accepted = false
//                        )
//                    )
//                ),
//                AvailableArtwork(
//                    artworkId = UUID.fromString("4f7f68b5-a143-4eb5-b8ab-27a9cf645eb9") as ArtworkId,
//                    description = "American Gothic",
//                    url = "https://www.galerie-sakura.com/media/main/produit/32f978fb9eb0a9685bfe4031af7b98dc6faf23c2.jpg",
//                    listed = true,
//                    expiryDate = Date(Calendar.getInstance().apply { set(2021,11,31) }.timeInMillis),
//                    bids = listOf( // multiple bob bids
//                        BidRecord(
//                            cordaReference = "939d6b8c-c0b6-4a95-983c-87e7fb003084",
//                            bidderPublicKey = "0xdfe3d63278d3282a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d",
//                            bidderDisplayName = "Bob",
//                            amountAndCurrency = GBP(300),
//                            notary = "O=GBP Notary, L=London, C=GB",
//                            accepted = false
//                        ),
//                        BidRecord(
//                            cordaReference = "e70e16fd-b648-416b-b8a1-8bd795f7ec81",
//                            bidderPublicKey = "0xdfe3d63278d3282a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d",
//                            bidderDisplayName = "Bob",
//                            amountAndCurrency = GBP(493),
//                            notary = "O=GBP Notary, L=London, C=GB",
//                            accepted = false
//                        ),
//                        BidRecord(
//                            cordaReference = "fb3c71bc-bad3-4a48-9585-41a3a9c8e5b2",
//                            bidderPublicKey = "0x2b4632d08485ff1df2db55b9dafd23347d1c47a457072a1e87be26896549a873",
//                            bidderDisplayName = "Charles",
//                            amountAndCurrency = Amount(3999, CBDC()),
//                            notary = "O=CBDC Notary, L=London, C=GB",
//                            accepted = false
//                        )
//                    )
//                ),
//                AvailableArtwork(
//                    artworkId = UUID.fromString("b10e7602-348f-4257-85aa-311915347931") as ArtworkId,
//                    description = "In the car",
//                    url = "https://www.invaluable.com/blog/wp-content/uploads/2017/10/Invaluable-Roy-Lichtenstein-Hero.jpg",
//                    listed = true,
//                    expiryDate = Date(Calendar.getInstance().apply { set(2021,11,31) }.timeInMillis),
//                    bids = emptyList()
//                )
//            )
//        )
//    }
//
//    @PutMapping("/gallery/create-artwork-transfer-tx")
//    fun createArtworkTransferTx(
//        @RequestParam("galleryParty") galleryParty: ArtworkParty,
//        @RequestParam("bidderParty") bidderParty: ArtworkParty,
//        @RequestParam("artworkId") artworkId: String
//    ) : ResponseEntity<UnsignedArtworkTransferTx> {
//        // TODO: Is the DTO for ownership to be provided in full? or shall artworkId be used as is here?
//        logger.info("MOCK Request to create artwork transfer transaction seller: $galleryParty, bidder: $bidderParty, art: $artworkId")
//        return asResponse(UnsignedArtworkTransferTx(secureRandomBytes(8)))
//    }
//
//    @PostMapping("/accept-bid", consumes = [MediaType.APPLICATION_JSON_VALUE])
//    fun acceptBid(
//        @RequestParam("galleryParty") galleryParty: ArtworkParty,
//        @RequestParam("cordaReference") cordaReference: CordaReference
//    ) : ResponseEntity<Unit> {
//        logger.info("MOCK Request by $galleryParty to accept bid from $cordaReference")
//        return asResponse(Unit)
//    }
//
//    //--- MOCK BIDDER ENDPOINTS
//
//    @PutMapping("/bidder/issue-tokens")
//    fun issueTokens(
//        @RequestParam("bidderParty") bidderParty: ArtworkParty,
//        @RequestParam("amount") amount: Long,
//        @RequestParam("currency") currency: String
//    ) : ResponseEntity<Unit> {
//        logger.info("MOCK Request by $bidderParty to issue tokens for $amount $currency")
//        return asResponse(Unit)
//    }
//
//    @PutMapping("/bidder/bid")
//    fun bid(
//        @RequestParam("bidderParty") bidderParty: ArtworkParty,
//        @RequestParam("artworkId") artworkId: ArtworkId,
//        @RequestParam("amount") amount: Long,
//        @RequestParam("currency") currency: String = "GBP",
//        @RequestParam("expiryDate") expiry: String
//    ) : ResponseEntity<Unit> {
//        logger.info("MOCK Request by $bidderParty to bid on $artworkId in amount of $amount $currency")
//        return asResponse(Unit)
//    }
//
//    //--- MOCK NETWORK ENDPOINTS
//
//    @GetMapping("/network/balance")
//    fun balance(
//        @RequestParam("party", required = false) party: ArtworkParty?
//    ) : ResponseEntity<List<NetworkBalancesResponse>> {
//        logger.info("MOCK Request for balance of parties across network")
//        val balances: Map<String, List<NetworkBalancesResponse.Balance>> =
//            mapOf(
//                Pair("O=Alice, L=London, C=GB", listOf(
//                    NetworkBalancesResponse.Balance(
//                        GBP.tokenIdentifier,
//                        GBP(0),
//                        GBP(80)
//                    ),
//                    NetworkBalancesResponse.Balance(
//                        CBDC().tokenIdentifier,
//                        Amount(0, CBDC()),
//                        Amount(30, CBDC())
//                    )
//                )),
//                Pair("O=Bob, L=San Francisco, C=US", listOf(
//                    NetworkBalancesResponse.Balance(
//                        GBP.tokenIdentifier,
//                        GBP(80),
//                        GBP(100)
//                    )
//                )),
//                Pair("O=Charlie, L=Mumbai, C=IN", listOf(
//                    NetworkBalancesResponse.Balance(
//                        CBDC().tokenIdentifier,
//                        Amount(0, CBDC()),
//                        Amount(3000, CBDC())
//                    )
//                ))
//            )
//
//        return asResponse(
//            if (party != null) { // filter if party provided
//                listOf(NetworkBalancesResponse(
//                    x500 = party,
//                    partyBalances = balances[party]!!
//                ))
//            } else {
//                balances.map {
//                    NetworkBalancesResponse(
//                        x500 = it.key,
//                        partyBalances = it.value
//                    )
//                }
//            }
//        )
//    }
//    class CBDC : TokenType("CBDC", 2)
//
//    @GetMapping("/network/participants")
//    fun participants(
//        @RequestParam("networks", required = false) networks: List<String>?
//    ): ResponseEntity<List<Participant>> {
//        logger.info("MOCK Request for all participants")
//        return asResponse(
//            listOf(
//                Participant(
//                    displayName = "Alice",
//                    x500 = "O=Alice, L=London, C=GB",
//                    networkIds = listOf(
//                        Participant.NetworkId(
//                            CordaRPCNetwork.AUCTION.name,
//                            "0xdfe3d63278d3282a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d"
//                        ),
//                        Participant.NetworkId(
//                            CordaRPCNetwork.GBP.name,
//                            "0xb4bc263278d3882a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d"
//                        ),
//                        Participant.NetworkId(
//                            CordaRPCNetwork.CBDC.name,
//                            "0xb4bc263278d3882a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26d"
//                        )
//                    ),
//                    type = Participant.AuctionRole.GALLERY
//                ),
//                Participant(
//                    displayName = "Bob",
//                    x500 = "O=Bob, L=San Francisco, C=US",
//                    networkIds = listOf(
//                        Participant.NetworkId(
//                            CordaRPCNetwork.AUCTION.name,
//                            "0xd8ec0ba1a63923bbb4f38147fb8a943da26ddfe3d63278d3282a652a8d73a6bf"
//                        ),
//                        Participant.NetworkId(
//                            CordaRPCNetwork.GBP.name,
//                            "0x82a652a8d73a6bfd8ec0ba1a63923bbb4f38147fb8a943da26db4bc263278d38"
//                        )
//                    ),
//                    type = Participant.AuctionRole.BIDDER
//                ),
//                Participant(
//                    displayName = "Charlie",
//                    x500 = "O=Charlie, L=Mumbai, C=IN",
//                    networkIds = listOf(
//                        Participant.NetworkId(
//                            CordaRPCNetwork.AUCTION.name,
//                            "0xa8d73a6bfd8ec0ba1a63923bbb4f38147fdfe3d63278d3282a652b8a943da26d"
//                        ),
//                        Participant.NetworkId(
//                            CordaRPCNetwork.CBDC.name,
//                            "0xb4bc2ba1a63923bbb4f38147fb8a943da26d63278d3882a652a8d73a6bfd8ec0"
//                        )
//                    ),
//                    type = Participant.AuctionRole.BIDDER
//                )
//            )
//        )
//    }
//
//    /**
//     * Log returns progressUpdates for Node Level state-machine updates
//     */
//    @GetMapping("/network/log")
//    fun log(): ResponseEntity<List<LogUpdateEntry>> {
//        logger.info("MOCK Request for logs")
//        return asResponse(
//            listOf(
//                LogUpdateEntry(
//                    associatedFlow = "com.r3.gallery.workflows.IssuedArtworkFlow",
//                    network = CordaRPCNetwork.AUCTION.name,
//                    x500 = "O=Alice, L=London, C=GB",
//                    logRecordId =  "fbba958b-8837-4216-aede-9e7313ba82e0",
//                    timestamp = "15:17:08.132263",
//                    message =  "[<Locked>300 GBP|83HFJKF8736YHG09SDJ] <- [Wallet|SDF7SDF8G9H00ME8569]"
//                ),
//                LogUpdateEntry(
//                    associatedFlow = "com.r3.gallery.workflows.BidOnArtworkFlow",
//                    network = CordaRPCNetwork.GBP.name,
//                    x500 = "O=Bob, L=San Francisco, C=US",
//                    logRecordId =  "a18a3bb7-1da9-4e72-b595-417f61451a84",
//                    timestamp = "16:17:08.132333",
//                    message = "Something has happened here on [83HFJKF8736YHG09SDJ]"
//                ),
//                LogUpdateEntry(
//                    associatedFlow = "com.r3.gallery.workflows.BidOnArtworkFlow",
//                    network = CordaRPCNetwork.CBDC.name,
//                    x500 = "O=Charles, L=London, C=GB",
//                    logRecordId =  "e555ac7f-6e16-49dc-99c2-f39eb01f72ca0",
//                    timestamp = "19:17:13.163322",
//                    message =  "[<Unlocked>300 GBP|83HFJKF8736YHG09SDJ] -> [Wallet|SDF7SDF8G9H00ME8569]"
//                ),
//                LogUpdateEntry(
//                    associatedFlow = "com.r3.gallery.workflows.FinaliseArtworkTransfer",
//                    network = CordaRPCNetwork.AUCTION.name,
//                    x500 = "O=Alice, L=London, C=GB",
//                    logRecordId =  "50a7c868-c9c7-406c-8d98-14606734aac5",
//                    timestamp = "15:17:08.132263",
//                    message =  "[<Locked>300 GBP|83HFJKF8736YHG09SDJ] <- [Wallet|SDF7SDF8G9H00ME8569]"
//                ),
//            )
//        )
//    }
}