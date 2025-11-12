package edu.infnet.melodyhub.infrastructure.playlist

import edu.infnet.melodyhub.domain.playlist.PlaylistMusic
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaPlaylistMusicRepository : JpaRepository<PlaylistMusic, UUID> {
    fun findByPlaylistId(playlistId: UUID): List<PlaylistMusic>
    fun findByPlaylistIdOrderByPosition(playlistId: UUID): List<PlaylistMusic>
    fun findByPlaylistIdAndMusicId(playlistId: UUID, musicId: String): PlaylistMusic?
    fun deleteAllByPlaylistId(playlistId: UUID)
    fun existsByPlaylistIdAndMusicId(playlistId: UUID, musicId: String): Boolean
    fun countByPlaylistId(playlistId: UUID): Long
}
