package com.r3.gallery.broker.services

import com.r3.gallery.broker.corda.client.api.ArtworkParty
import com.r3.gallery.broker.corda.client.api.TokenParty

interface IdentityRegistry {

    fun getTokenParty(name: String): TokenParty
    fun getArtworkParty(name: String): ArtworkParty

}