package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.TokenParty
import org.springframework.stereotype.Component

@Component
class IdentityRegistryImpl : IdentityRegistry {
    override fun getTokenParty(name: String): TokenParty {
        return name
    }

    override fun getArtworkParty(name: String): ArtworkParty {
        return name
    }
}