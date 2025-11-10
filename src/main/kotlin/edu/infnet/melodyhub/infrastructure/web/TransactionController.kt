package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.transaction.TransactionService
import edu.infnet.melodyhub.application.transaction.dto.CreateTransactionRequest
import edu.infnet.melodyhub.application.transaction.dto.TransactionResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/transactions")
class TransactionController(
    private val transactionService: TransactionService
) {

    @PostMapping
    fun createTransaction(
        @Valid @RequestBody request: CreateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val response = transactionService.createTransaction(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response)
    }

    @GetMapping("/{id}")
    fun getTransactionById(@PathVariable id: UUID): ResponseEntity<TransactionResponse> {
        val response = transactionService.getTransactionById(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getAllTransactions(): ResponseEntity<List<TransactionResponse>> {
        val response = transactionService.getAllTransactions()
        return ResponseEntity.ok(response)
    }

    @GetMapping("/user/{userId}")
    fun getTransactionsByUserId(@PathVariable userId: UUID): ResponseEntity<List<TransactionResponse>> {
        val response = transactionService.getTransactionsByUserId(userId)
        return ResponseEntity.ok(response)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(ex.message ?: "Invalid request"))
    }
}
