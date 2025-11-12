package edu.infnet.melodyhub.domain.creditcard

import java.util.UUID

interface CreditCardRepository {
    fun save(creditCard: CreditCard): CreditCard
    fun findById(id: Long): CreditCard?
    fun findByUserId(userId: UUID): List<CreditCard>
    fun findActiveByUserId(userId: UUID): List<CreditCard>
    fun existsById(id: Long): Boolean
    fun delete(creditCard: CreditCard)
}
