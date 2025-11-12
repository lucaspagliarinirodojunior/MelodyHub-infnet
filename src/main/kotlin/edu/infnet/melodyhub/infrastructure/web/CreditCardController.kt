package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.creditcard.CreateCreditCardDTO
import edu.infnet.melodyhub.application.creditcard.CreditCardResponseDTO
import edu.infnet.melodyhub.application.creditcard.CreditCardService
import edu.infnet.melodyhub.application.creditcard.UpdateCreditCardStatusDTO
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/credit-cards")
class CreditCardController(
    private val creditCardService: CreditCardService
) {

    @PostMapping
    fun create(@RequestBody dto: CreateCreditCardDTO): ResponseEntity<Any> {
        return try {
            val creditCard = creditCardService.create(dto)
            ResponseEntity.status(HttpStatus.CREATED).body(creditCard)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Erro ao criar cartão"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse("Erro interno do servidor"))
        }
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<Any> {
        return try {
            val creditCard = creditCardService.findById(id)
            ResponseEntity.ok(creditCard)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Cartão não encontrado"))
        }
    }

    @GetMapping("/user/{userId}")
    fun findByUserId(@PathVariable userId: java.util.UUID): ResponseEntity<List<CreditCardResponseDTO>> {
        val creditCards = creditCardService.findByUserId(userId)
        return ResponseEntity.ok(creditCards)
    }

    @GetMapping("/user/{userId}/active")
    fun findActiveByUserId(@PathVariable userId: java.util.UUID): ResponseEntity<List<CreditCardResponseDTO>> {
        val creditCards = creditCardService.findActiveByUserId(userId)
        return ResponseEntity.ok(creditCards)
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody dto: UpdateCreditCardStatusDTO
    ): ResponseEntity<Any> {
        return try {
            val creditCard = creditCardService.updateStatus(id, dto)
            ResponseEntity.ok(creditCard)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Cartão não encontrado"))
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Any> {
        return try {
            creditCardService.delete(id)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Cartão não encontrado"))
        }
    }
}
