package edu.infnet.melodyhub.infrastructure.transaction

import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.transaction.Transaction
import edu.infnet.melodyhub.domain.transaction.TransactionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

interface JpaTransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByUserId(userId: UUID): List<Transaction>

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.status = 'APPROVED'")
    fun findApprovedByUserId(userId: UUID): List<Transaction>

    fun countByUserIdAndCreatedAtAfter(userId: UUID, since: LocalDateTime): Long

    fun countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
        userId: UUID,
        amount: BigDecimal,
        subscriptionType: SubscriptionType,
        since: LocalDateTime
    ): Long
}
