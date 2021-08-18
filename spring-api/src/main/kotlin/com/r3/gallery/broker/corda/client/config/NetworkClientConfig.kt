package com.r3.gallery.broker.corda.client.config

abstract class NetworkClientConfig {
    abstract var nodeName: String
    abstract var networkName: String
    abstract var nodeUrl: String
    abstract var nodeUsername: String
    abstract var nodePassword: String
}