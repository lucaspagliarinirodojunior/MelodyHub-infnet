package edu.infnet.melodyhub.application.playlist.dto

import jakarta.validation.constraints.NotBlank

data class AddMusicToPlaylistRequest(
    @field:NotBlank(message = "ID da música é obrigatório")
    val musicId: String
)
