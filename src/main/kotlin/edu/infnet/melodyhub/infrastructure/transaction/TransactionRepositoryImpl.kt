package edu.infnet.melodyhub.infrastructure.transaction

import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import edu.infnet.melodyhub.domain.transaction.Transaction
import edu.infnet.melodyhub.domain.transaction.TransactionRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Repository
class TransactionRepositoryImpl(
    private val jpaRepository: JpaTransactionRepository
) : TransactionRepository {

    override fun save(transaction: Transaction): Transaction {
        return jpaRepository.save(transaction)
    }

    override fun findById(id: UUID): Transaction? {
        return jpaRepository.findById(id).orElse(null)
    }

    override fun findAll(): List<Transaction> {
        return jpaRepository.findAll()
    }

    override fun findByUserId(userId: UUID): List<Transaction> {
        return jpaRepository.findByUserId(userId)
    }

    override fun findApprovedByUserId(userId: UUID): List<Transaction> {
        return jpaRepository.findApprovedByUserId(userId)
    }

    override fun countByUserIdAndCreatedAtAfter(userId: UUID, since: LocalDateTime): Long {
        return jpaRepository.countByUserIdAndCreatedAtAfter(userId, since)
    }

    override fun countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
        userId: UUID,
        amount: BigDecimal,
        subscriptionType: SubscriptionType,
        since: LocalDateTime
    ): Long {
        return jpaRepository.countByUserIdAndAmountAndSubscriptionTypeAndCreatedAtAfter(
            userId,
            amount,
            subscriptionType,
            since
        )
    }
}
