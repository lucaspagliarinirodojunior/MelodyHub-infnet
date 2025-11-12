package edu.infnet.melodyhub.infrastructure.playlist

import edu.infnet.melodyhub.domain.playlist.PlaylistMusic
import edu.infnet.melodyhub.domain.playlist.PlaylistMusicRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PlaylistMusicRepositoryImpl(
    private val jpaPlaylistMusicRepository: JpaPlaylistMusicRepository
) : PlaylistMusicRepository {

    override fun save(playlistMusic: PlaylistMusic): PlaylistMusic {
        return jpaPlaylistMusicRepository.save(playlistMusic)
    }

    override fun findByPlaylistId(playlistId: UUID): List<PlaylistMusic> {
        return jpaPlaylistMusicRepository.findByPlaylistId(playlistId)
    }

    override fun findByPlaylistIdOrderByPosition(playlistId: UUID): List<PlaylistMusic> {
        return jpaPlaylistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)
    }

    override fun findByPlaylistIdAndMusicId(playlistId: UUID, musicId: String): PlaylistMusic? {
        return jpaPlaylistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)
    }

    override fun delete(playlistMusic: PlaylistMusic) {
        jpaPlaylistMusicRepository.delete(playlistMusic)
    }

    override fun deleteAllByPlaylistId(playlistId: UUID) {
        jpaPlaylistMusicRepository.deleteAllByPlaylistId(playlistId)
    }

    override fun existsByPlaylistIdAndMusicId(playlistId: UUID, musicId: String): Boolean {
        return jpaPlaylistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, musicId)
    }

    override fun countByPlaylistId(playlistId: UUID): Long {
        return jpaPlaylistMusicRepository.countByPlaylistId(playlistId)
    }
}
