package edu.infnet.melodyhub.application.playlist.dto

import edu.infnet.melodyhub.domain.playlist.Playlist
import java.time.LocalDateTime
import java.util.UUID

data class PlaylistResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val userId: UUID,
    val isDefault: Boolean,
    val musicCount: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(playlist: Playlist, musicCount: Long): PlaylistResponse {
            return PlaylistResponse(
                id = playlist.id!!,
                name = playlist.name,
                description = playlist.description,
                userId = playlist.userId,
                isDefault = playlist.isDefault,
                musicCount = musicCount,
                createdAt = playlist.createdAt,
                updatedAt = playlist.updatedAt
            )
        }
    }
}
