package edu.infnet.melodyhub.application.transaction.dto

import edu.infnet.melodyhub.domain.transaction.SubscriptionType
import jakarta.validation.constraints.NotNull
import java.util.*

data class CreateTransactionRequest(
    @field:NotNull(message = "User ID is required")
    val userId: UUID,

    @field:NotNull(message = "Subscription type is required")
    val subscriptionType: SubscriptionType,

    @field:NotNull(message = "Credit card ID is required")
    val creditCardId: Long
)
