package edu.infnet.melodyhub.application.playlist.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreatePlaylistRequest(
    @field:NotBlank(message = "Nome da playlist é obrigatório")
    val name: String,

    val description: String? = null,

    @field:NotNull(message = "ID do usuário é obrigatório")
    val userId: UUID
)
