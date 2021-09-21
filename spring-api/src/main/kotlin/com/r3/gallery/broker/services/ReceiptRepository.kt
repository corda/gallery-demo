package com.r3.gallery.broker.services

import com.r3.gallery.api.ArtworkId
import com.r3.gallery.broker.services.api.Receipt
import org.springframework.stereotype.Component

/**
 * Stores in memory Receipts
 */
@Component
class BidReceiptRepository: ReceiptRepository<Receipt.BidReceipt>()
@Component
class SaleReceiptRepository: ReceiptRepository<Receipt.SaleReceipt>()
@Component
class CancelReceiptRepository: ReceiptRepository<Receipt.CancellationReceipt>()

abstract class ReceiptRepository<T: Receipt>() {
    private val receipts: MutableMap<Int, T> = HashMap()

    fun store(receipt: T) {
        val bidderName = receipt.bidderName
        val artworkId = receipt.artworkId
        receipts[(bidderName + artworkId).hashCode()] = receipt
    }

    fun retrieve(bidderName: String, artworkId: ArtworkId): T {
        return receipts[(bidderName + artworkId).hashCode()] ?:
            throw ReceiptNotFoundException(bidderName, artworkId)
    }

    fun remove(bidderName: String, artworkId: ArtworkId) {
        receipts.remove((bidderName + artworkId).hashCode()) ?:
            throw ReceiptNotFoundException(bidderName, artworkId)
    }

    fun retrieveAllForId(artworkId: ArtworkId): List<T> {
        return receipts.values.filter { it.artworkId == artworkId }
    }

    fun allReceipts(): List<T> {
        return receipts.values.toList()
    }

    class ReceiptNotFoundException(bidderName: String, artworkId: ArtworkId /* = java.util.UUID */)
        : IllegalArgumentException("Receipt not found for $bidderName, $artworkId")
}