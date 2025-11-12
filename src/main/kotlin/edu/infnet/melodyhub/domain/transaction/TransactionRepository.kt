package edu.infnet.melodyhub.domain.transaction

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

interface TransactionRepository {
    fun save(transaction: Transaction): Transaction
    fun findById(id: UUID): Transaction?
    fun findAll(): List<Transaction>
    fun findByUserId(userId: UUID): List<Transaction>
    fun findApprovedByUserId(userId: UUID): List<Transaction>
    fun countByUserIdAndCreatedAtAfter(userId: UUID, since: LocalDateTime): Long
    fun countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
        userId: UUID,
        amount: BigDecimal,
        subscriptionType: SubscriptionType,
        since: LocalDateTime
    ): Long
}
