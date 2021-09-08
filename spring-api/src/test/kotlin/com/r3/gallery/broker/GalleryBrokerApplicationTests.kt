package com.r3.gallery.broker

import com.r3.gallery.broker.corda.client.art.api.ArtNetworkGalleryClientImpl
import com.r3.gallery.broker.corda.client.art.controllers.ArtNetworkGalleryController
import com.r3.gallery.broker.corda.client.art.controllers.asResponse
import com.r3.gallery.broker.corda.rpc.config.ClientProperties
import com.r3.gallery.broker.corda.rpc.service.ConnectionService

import net.minidev.json.JSONArray
import net.minidev.json.JSONObject
import net.minidev.json.JSONValue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*
import javax.annotation.PostConstruct
import kotlin.math.exp

@WebMvcTest(
	controllers = [
		ArtNetworkGalleryController::class,
		ArtNetworkGalleryClientImpl::class,
		ClientProperties::class
	]
)
class GalleryBrokerApplicationTests {

	private val mockConnectionService: ConnectionService = Mockito.mock(ConnectionService::class.java)

	@Autowired
	lateinit var artNetworkGalleryClientImpl: ArtNetworkGalleryClientImpl

	@PostConstruct
	fun setMocks() {
		ReflectionTestUtils.setField(artNetworkGalleryClientImpl, "artNetworkGalleryCS", mockConnectionService)
	}

	@Autowired
	lateinit var mockMvc: MockMvc

//	@Test
//	fun `connection service provides connections`() {
//		val expectedResult = listOf(UUID.randomUUID(), UUID.randomUUID())
//		Mockito.`when`(mockConnectionService.startFlow(any<String>(), any<Class<ListAvailableArtworks>>(), any<UUID>()))
//			.thenReturn(expectedResult)
//
//		val result = mockMvc.perform(
//			get("/gallery/list-available-artworks")
//				.param("galleryParty", "O=Alice,L=London,C=GB")
//		)
//		result.andExpect(status().isOk)
//			.andDo {
//				val json = JSONValue.parse(it.response.contentAsString) as JSONArray
//				assert(json.contains(expectedResult[0].toString()))
//				assert(json.contains(expectedResult[1].toString()))
//			}
//	}

}