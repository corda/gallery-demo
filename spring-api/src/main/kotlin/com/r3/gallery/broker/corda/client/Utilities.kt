package com.r3.gallery.broker.corda.client

import com.r3.gallery.broker.corda.client.exceptions.InvalidArtworkIdException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.async.DeferredResult
import java.util.*
import kotlin.concurrent.thread

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

/** creates a Deferred result using kotlin thread and wrapped in ResponseEntity */
internal fun <T> deferredResult(block: () -> T): DeferredResult<ResponseEntity<T>> {
    val output = DeferredResult<ResponseEntity<T>>()
    thread {
        output.setResult(asResponse(block.invoke()))
    }
    return  output
}