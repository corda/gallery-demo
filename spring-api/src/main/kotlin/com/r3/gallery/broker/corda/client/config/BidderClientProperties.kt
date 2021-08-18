package com.r3.gallery.broker.corda.client.config

import org.springframework.beans.factory.annotation.Value

/**
 * Connection and credential properties for Bidders
 */
class BidderClientProperties {
    var auctionBob = object : NetworkClientConfig() {
        override var nodeName: String = "bob"
        override var networkName: String = "auction"
        @Value("auction.bob.rpc.url")
        override lateinit var nodeUrl: String
        @Value("auction.bob.rpc.username")
        override lateinit var nodeUsername: String
        @Value("auction.bob.rpc.password")
        override lateinit var nodePassword: String
    }
    var auctionCharlie = object : NetworkClientConfig() {
        override var nodeName: String = "charlie"
        override var networkName: String = "auction"
        @Value("auction.charlie.rpc.url")
        override lateinit var nodeUrl: String
        @Value("auction.charlie.rpc.username")
        override lateinit var nodeUsername: String
        @Value("auction.charlie.rpc.password")
        override lateinit var nodePassword: String
    }
    var gbpBob = object : NetworkClientConfig() {
        override var nodeName: String = "bob"
        override var networkName: String = "gbp"
        @Value("gbp.bob.rpc.url")
        override lateinit var nodeUrl: String
        @Value("gbp.bob.rpc.username")
        override lateinit var nodeUsername: String
        @Value("gbp.bob.rpc.password")
        override lateinit var nodePassword: String
    }
    var cbdcCharlie = object : NetworkClientConfig() {
        override var nodeName: String = "charlie"
        override var networkName: String = "cbdc"
        @Value("cbdc.charlie.rpc.url")
        override lateinit var nodeUrl: String
        @Value("cbdc.charlie.rpc.username")
        override lateinit var nodeUsername: String
        @Value("cbdc.charlie.rpc.password")
        override lateinit var nodePassword: String
    }
}