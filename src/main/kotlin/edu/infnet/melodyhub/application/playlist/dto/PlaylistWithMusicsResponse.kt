package edu.infnet.melodyhub.application.playlist.dto

import edu.infnet.melodyhub.domain.music.Music
import edu.infnet.melodyhub.domain.playlist.Playlist
import edu.infnet.melodyhub.domain.playlist.PlaylistMusic
import java.time.LocalDateTime
import java.util.UUID

data class PlaylistWithMusicsResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val userId: UUID,
    val isDefault: Boolean,
    val musics: List<MusicInPlaylistResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(
            playlist: Playlist,
            playlistMusics: List<PlaylistMusic>,
            musics: List<Music>
        ): PlaylistWithMusicsResponse {
            val musicMap = musics.associateBy { it.id }

            val musicResponses = playlistMusics.mapNotNull { pm ->
                musicMap[pm.musicId]?.let { music ->
                    MusicInPlaylistResponse(
                        musicId = music.id!!,
                        fileName = music.fileName,
                        contentType = music.contentType,
                        size = music.size,
                        position = pm.position,
                        addedAt = pm.addedAt
                    )
                }
            }.sortedBy { it.position }

            return PlaylistWithMusicsResponse(
                id = playlist.id!!,
                name = playlist.name,
                description = playlist.description,
                userId = playlist.userId,
                isDefault = playlist.isDefault,
                musics = musicResponses,
                createdAt = playlist.createdAt,
                updatedAt = playlist.updatedAt
            )
        }
    }
}

data class MusicInPlaylistResponse(
    val musicId: String,
    val fileName: String,
    val contentType: String,
    val size: Long,
    val position: Int,
    val addedAt: LocalDateTime
)
