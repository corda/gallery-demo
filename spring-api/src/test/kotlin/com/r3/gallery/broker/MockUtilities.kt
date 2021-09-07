package com.r3.gallery.broker

import org.mockito.Mockito

/**
 * Mock helper that allows class checks to run on non-nullable fields
 */
fun <T> any(): T {
    Mockito.any<T>()
    return uninitialized()
}

@Suppress("unchecked_cast")
private fun <T> uninitialized(): T = null as T