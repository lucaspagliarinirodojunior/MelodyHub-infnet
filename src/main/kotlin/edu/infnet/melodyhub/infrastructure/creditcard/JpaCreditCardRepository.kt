package edu.infnet.melodyhub.infrastructure.creditcard

import edu.infnet.melodyhub.domain.creditcard.CreditCard
import edu.infnet.melodyhub.domain.creditcard.CreditCardStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface JpaCreditCardRepository : JpaRepository<CreditCard, Long> {
    fun findByUserId(userId: UUID): List<CreditCard>

    @Query("SELECT c FROM CreditCard c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    fun findActiveByUserId(userId: UUID): List<CreditCard>
}
