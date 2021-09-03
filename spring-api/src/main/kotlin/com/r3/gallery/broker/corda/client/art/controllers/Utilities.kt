package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.broker.corda.client.exceptions.InvalidArtworkIdException
import java.util.*

internal fun String.toUUID() : UUID {
    try {
        return UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        throw InvalidArtworkIdException(this)
    }
}