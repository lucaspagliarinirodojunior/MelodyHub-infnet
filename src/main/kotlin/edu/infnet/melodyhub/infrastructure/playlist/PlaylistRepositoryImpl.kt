package edu.infnet.melodyhub.infrastructure.playlist

import edu.infnet.melodyhub.domain.playlist.Playlist
import edu.infnet.melodyhub.domain.playlist.PlaylistRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PlaylistRepositoryImpl(
    private val jpaPlaylistRepository: JpaPlaylistRepository
) : PlaylistRepository {

    override fun save(playlist: Playlist): Playlist {
        return jpaPlaylistRepository.save(playlist)
    }

    override fun findById(id: UUID): Playlist? {
        return jpaPlaylistRepository.findById(id).orElse(null)
    }

    override fun findByUserId(userId: UUID): List<Playlist> {
        return jpaPlaylistRepository.findByUserId(userId)
    }

    override fun findByUserIdAndIsDefault(userId: UUID, isDefault: Boolean): List<Playlist> {
        return jpaPlaylistRepository.findByUserIdAndIsDefault(userId, isDefault)
    }

    override fun delete(playlist: Playlist) {
        jpaPlaylistRepository.delete(playlist)
    }

    override fun existsByUserIdAndName(userId: UUID, name: String): Boolean {
        return jpaPlaylistRepository.existsByUserIdAndName(userId, name)
    }
}
