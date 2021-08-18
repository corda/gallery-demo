package com.r3.gallery.broker.corda.client.config

import org.springframework.beans.factory.annotation.Value

/**
 * Connection and credential properties for Gallery
 */
class GalleryClientProperties {
    var auctionAlice = object : NetworkClientConfig() {
        override var nodeName: String = "alice"
        override var networkName: String = "auction"
        @Value("auction.alice.rpc.url")
        override lateinit var nodeUrl: String
        @Value("auction.alice.rpc.username")
        override lateinit var nodeUsername: String
        @Value("auction.alice.rpc.password")
        override lateinit var nodePassword: String
    }
    var gbpAlice = object : NetworkClientConfig() {
        override var nodeName: String = "alice"
        override var networkName: String = "gbp"
        @Value("gbp.alice.rpc.url")
        override lateinit var nodeUrl: String
        @Value("gbp.alice.rpc.username")
        override lateinit var nodeUsername: String
        @Value("gbp.alice.rpc.password")
        override lateinit var nodePassword: String
    }
    var cbdcAlice = object : NetworkClientConfig() {
        override var nodeName: String = "alice"
        override var networkName: String = "cdbc"
        @Value("cdbc.alice.rpc.url")
        override lateinit var nodeUrl: String
        @Value("cbdc.alice.rpc.username")
        override lateinit var nodeUsername: String
        @Value("cbdc.alice.rpc.password")
        override lateinit var nodePassword: String
    }
}