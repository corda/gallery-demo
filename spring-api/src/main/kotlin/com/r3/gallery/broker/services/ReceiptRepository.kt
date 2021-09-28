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
        val currency = receipt.currency
        receipts[(bidderName + artworkId + currency).hashCode()] = receipt
    }

    fun retrieve(bidderName: String, artworkId: ArtworkId, currency: String): T {
        return receipts[(bidderName + artworkId + currency).hashCode()] ?:
            throw ReceiptNotFoundException(bidderName, artworkId, currency)
    }

    fun remove(bidderName: String, artworkId: ArtworkId, currency: String) {
        receipts.remove((bidderName + artworkId + currency).hashCode()) ?:
            throw ReceiptNotFoundException(bidderName, artworkId, currency)
    }

    fun retrieveAllForId(artworkId: ArtworkId): List<T> {
        return receipts.values.filter { it.artworkId == artworkId }
    }

    fun allReceipts(): List<T> {
        return receipts.values.toList()
    }

    class ReceiptNotFoundException(bidderName: String, artworkId: ArtworkId /* = java.util.UUID */, currency: String)
        : IllegalArgumentException("Receipt not found for $bidderName, $artworkId, $currency")
}