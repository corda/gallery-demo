package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkParty
import com.r3.gallery.api.TokenParty

interface IdentityRegistry {

    fun getTokenParty(name: String): TokenParty
    fun getArtworkParty(name: String): ArtworkParty

}