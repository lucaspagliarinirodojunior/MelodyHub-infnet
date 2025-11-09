package edu.infnet.melodyhub.infrastructure.transaction

import edu.infnet.melodyhub.domain.transaction.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface JpaTransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByUserId(userId: UUID): List<Transaction>
    fun countByUserIdAndCreatedAtAfter(userId: UUID, since: LocalDateTime): Long
}
