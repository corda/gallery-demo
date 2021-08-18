package com.r3.gallery.broker.corda.client.config

abstract class NetworkClientConfig {
    val id: String by lazy { nodeName+networkName }
    abstract val nodeName: String
    abstract val networkName: String
    abstract var nodeUrl: String
    abstract var nodeUsername: String
    abstract var nodePassword: String
}