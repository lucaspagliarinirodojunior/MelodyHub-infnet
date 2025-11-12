package edu.infnet.melodyhub.infrastructure.creditcard

import edu.infnet.melodyhub.domain.creditcard.CreditCard
import edu.infnet.melodyhub.domain.creditcard.CreditCardRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class CreditCardRepositoryImpl(
    private val jpaRepository: JpaCreditCardRepository
) : CreditCardRepository {

    override fun save(creditCard: CreditCard): CreditCard {
        return jpaRepository.save(creditCard)
    }

    override fun findById(id: Long): CreditCard? {
        return jpaRepository.findById(id).orElse(null)
    }

    override fun findByUserId(userId: UUID): List<CreditCard> {
        return jpaRepository.findByUserId(userId)
    }

    override fun findActiveByUserId(userId: UUID): List<CreditCard> {
        return jpaRepository.findActiveByUserId(userId)
    }

    override fun existsById(id: Long): Boolean {
        return jpaRepository.existsById(id)
    }

    override fun delete(creditCard: CreditCard) {
        jpaRepository.delete(creditCard)
    }
}
