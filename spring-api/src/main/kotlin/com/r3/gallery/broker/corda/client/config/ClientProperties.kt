package com.r3.gallery.broker.corda.client.config

interface ClientProperties {
    var clients: List<NetworkClientConfig>

    fun getById(id: String) : NetworkClientConfig? =
        clients.first {
            it.id == id
        }

    fun getByNodeName(name: String) : List<NetworkClientConfig>? =
        clients.filter {
            it.nodeName == name
        }

    fun getByNetworkName(networkName: String) : List<NetworkClientConfig>? =
        clients.filter {
            it.networkName == networkName
        }
}