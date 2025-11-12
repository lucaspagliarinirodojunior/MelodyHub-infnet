package edu.infnet.melodyhub.domain.playlist

import java.util.UUID

interface PlaylistRepository {
    fun save(playlist: Playlist): Playlist
    fun findById(id: UUID): Playlist?
    fun findByUserId(userId: UUID): List<Playlist>
    fun findByUserIdAndIsDefault(userId: UUID, isDefault: Boolean): List<Playlist>
    fun delete(playlist: Playlist)
    fun existsByUserIdAndName(userId: UUID, name: String): Boolean
}
