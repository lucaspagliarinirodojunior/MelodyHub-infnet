package edu.infnet.melodyhub.infrastructure.playlist

import edu.infnet.melodyhub.domain.playlist.Playlist
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaPlaylistRepository : JpaRepository<Playlist, UUID> {
    fun findByUserId(userId: UUID): List<Playlist>
    fun findByUserIdAndIsDefault(userId: UUID, isDefault: Boolean): List<Playlist>
    fun existsByUserIdAndName(userId: UUID, name: String): Boolean
}
