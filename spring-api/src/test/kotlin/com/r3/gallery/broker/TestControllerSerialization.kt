package com.r3.gallery.broker

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer

class TestControllerSerialization {
    companion object {
        @JvmStatic
        private val mockServer: ClientAndServer = ClientAndServer.startClientAndServer(1080)

        @AfterAll
        fun stopServer() {
            mockServer.stop()
        }
    }

    @Test
    fun blank() {

    }
}