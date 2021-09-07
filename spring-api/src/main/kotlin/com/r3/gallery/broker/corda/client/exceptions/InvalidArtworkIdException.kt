package com.r3.gallery.broker.corda.client.exceptions

class InvalidArtworkIdException(artworkId: String)
    : IllegalArgumentException("$artworkId is not a valid UUID.")