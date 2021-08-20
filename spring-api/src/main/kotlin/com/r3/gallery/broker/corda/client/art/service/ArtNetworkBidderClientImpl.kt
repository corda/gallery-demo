package com.r3.gallery.broker.corda.client.art.service

import com.r3.gallery.broker.corda.client.art.api.ArtNetworkBidderClient
import com.r3.gallery.broker.corda.client.config.ClientProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class ArtNetworkBidderClientImpl(
    @Autowired
    @Qualifier("ArtNetworkBidderProperties")
    clientProperties: ClientProperties
) : NodeClient(clientProperties), ArtNetworkBidderClient {

}