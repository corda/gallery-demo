package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.broker.services.api.Receipt
import org.springframework.stereotype.Component

/**
 * Stores in memory Receipts
 */
interface ReceiptRepository<T: Receipt> {
    fun store(receipt: T)
    fun retrieve(bidderName: String, artworkId: ArtworkId): T
    fun remove(bidderName: String, artworkId: ArtworkId)
    fun getBidsFor(artworkId: ArtworkId): List<T>
    fun allReceipts(): List<T>
}

@Component
class ReceiptRepositoryImpl<T: Receipt>: ReceiptRepository<T> {
    private val receipts: MutableMap<Int, T> = HashMap()

    override fun store(receipt: T) {
        val bidderName = receipt.bidderName
        val artworkId = receipt.artworkId
        receipts[(bidderName + artworkId).hashCode()] = receipt
    }

    override fun retrieve(bidderName: String, artworkId: ArtworkId): T {
        return receipts[(bidderName + artworkId).hashCode()] ?:
            throw ReceiptNotFoundException(bidderName, artworkId)
    }

    override fun remove(bidderName: String, artworkId: ArtworkId) {
        receipts.remove((bidderName + artworkId).hashCode()) ?:
            throw ReceiptNotFoundException(bidderName, artworkId)
    }

    override fun getBidsFor(artworkId: ArtworkId): List<T> {
        return receipts.values.filter { it.artworkId == artworkId }
    }

    override fun allReceipts(): List<T> {
        return receipts.values.toList()
    }

    class ReceiptNotFoundException(bidderName: String, artworkId: ArtworkId /* = java.util.UUID */)
        : IllegalArgumentException("Receipt not found for $bidderName, $artworkId")
}