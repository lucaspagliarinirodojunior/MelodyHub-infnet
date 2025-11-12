package edu.infnet.melodyhub.domain.playlist

import java.util.UUID

interface PlaylistMusicRepository {
    fun save(playlistMusic: PlaylistMusic): PlaylistMusic
    fun findByPlaylistId(playlistId: UUID): List<PlaylistMusic>
    fun findByPlaylistIdOrderByPosition(playlistId: UUID): List<PlaylistMusic>
    fun findByPlaylistIdAndMusicId(playlistId: UUID, musicId: String): PlaylistMusic?
    fun delete(playlistMusic: PlaylistMusic)
    fun deleteAllByPlaylistId(playlistId: UUID)
    fun existsByPlaylistIdAndMusicId(playlistId: UUID, musicId: String): Boolean
    fun countByPlaylistId(playlistId: UUID): Long
}
