package edu.infnet.melodyhub.application.transaction.dto

import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.transaction.Transaction
import edu.infnet.melodyhub.domain.transaction.TransactionStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class TransactionResponse(
    val id: UUID,
    val userId: UUID,
    val amount: BigDecimal,
    val subscriptionType: SubscriptionType,
    val status: TransactionStatus,
    val fraudReason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(transaction: Transaction): TransactionResponse {
            return TransactionResponse(
                id = transaction.id,
                userId = transaction.userId,
                amount = transaction.amount,
                subscriptionType = transaction.subscriptionType,
                status = transaction.status,
                fraudReason = transaction.fraudReason,
                createdAt = transaction.createdAt,
                updatedAt = transaction.updatedAt
            )
        }
    }
}
