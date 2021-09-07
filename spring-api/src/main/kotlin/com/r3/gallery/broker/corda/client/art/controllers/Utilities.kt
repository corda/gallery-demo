package com.r3.gallery.broker.corda.client.art.controllers

import com.r3.gallery.broker.corda.client.exceptions.InvalidArtworkIdException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.*

/** Simple parser for UUID from string with custom error */
internal fun String.toUUID() : UUID {
    try {
        return UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        throw InvalidArtworkIdException(this)
    }
}

/** wraps a RESTful response in an entity with OK status */
internal fun <T> asResponse(obj: T) : ResponseEntity<T> = ResponseEntity.status(HttpStatus.OK).body(obj)