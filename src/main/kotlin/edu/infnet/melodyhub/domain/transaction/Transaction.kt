package edu.infnet.melodyhub.domain.transaction

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "transactions")
class Transaction(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val subscriptionType: SubscriptionType,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: TransactionStatus = TransactionStatus.PENDING,

    @Column
    var fraudReason: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    fun approve() {
        require(status == TransactionStatus.PENDING) {
            "Only pending transactions can be approved"
        }
        status = TransactionStatus.APPROVED
        updatedAt = LocalDateTime.now()
    }

    fun reject(reason: String) {
        require(status == TransactionStatus.PENDING) {
            "Only pending transactions can be rejected"
        }
        status = TransactionStatus.REJECTED
        fraudReason = reason
        updatedAt = LocalDateTime.now()
    }

    fun isApproved(): Boolean = status == TransactionStatus.APPROVED

    fun isRejected(): Boolean = status == TransactionStatus.REJECTED
}

enum class SubscriptionType(val monthlyPrice: BigDecimal) {
    BASIC(BigDecimal("9.90")),
    PREMIUM(BigDecimal("19.90"))
}

enum class TransactionStatus {
    PENDING,
    APPROVED,
    REJECTED
}
