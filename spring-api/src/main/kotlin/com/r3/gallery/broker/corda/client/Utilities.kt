package com.r3.gallery.broker.corda.client

import com.r3.gallery.broker.corda.client.exceptions.InvalidArtworkIdException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

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

/** Simultaneously resolves a list of Completable Futures and returns a list of results using Rx join to fetch. */
fun <T> joinFuturesFromList(futures: List<CompletableFuture<out T>>, returnUnit: Boolean = false): List<T> {
    return CompletableFuture.allOf(*futures.toTypedArray())
            .let { _ ->
                futures.stream()
                        .map { future ->
                            // if requesting Unit values, then transform
                            if (returnUnit) future.thenApply { it.let {  } }
                            future.join()
                        }.collect(Collectors.toList()).toList()
            }
}