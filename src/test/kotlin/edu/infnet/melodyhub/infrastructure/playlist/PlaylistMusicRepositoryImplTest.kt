package edu.infnet.melodyhub.infrastructure.playlist

import edu.infnet.melodyhub.domain.playlist.PlaylistMusic
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.*

class PlaylistMusicRepositoryImplTest {

    private lateinit var jpaPlaylistMusicRepository: JpaPlaylistMusicRepository
    private lateinit var playlistMusicRepository: PlaylistMusicRepositoryImpl

    @BeforeEach
    fun setup() {
        jpaPlaylistMusicRepository = mock()
        playlistMusicRepository = PlaylistMusicRepositoryImpl(jpaPlaylistMusicRepository)
    }

    @Test
    fun `should save playlist music`() {
        val playlistMusic = PlaylistMusic(
            id = UUID.randomUUID(),
            playlistId = UUID.randomUUID(),
            musicId = "music123",
            position = 0
        )

        whenever(jpaPlaylistMusicRepository.save(playlistMusic)).thenReturn(playlistMusic)

        val result = playlistMusicRepository.save(playlistMusic)

        assertEquals(playlistMusic, result)
        verify(jpaPlaylistMusicRepository).save(playlistMusic)
    }

    @Test
    fun `should find playlist musics by playlist id`() {
        val playlistId = UUID.randomUUID()
        val playlistMusics = listOf(
            PlaylistMusic(UUID.randomUUID(), playlistId, "music1", 0),
            PlaylistMusic(UUID.randomUUID(), playlistId, "music2", 1)
        )

        whenever(jpaPlaylistMusicRepository.findByPlaylistId(playlistId)).thenReturn(playlistMusics)

        val result = playlistMusicRepository.findByPlaylistId(playlistId)

        assertEquals(2, result.size)
        assertEquals(playlistMusics, result)
        verify(jpaPlaylistMusicRepository).findByPlaylistId(playlistId)
    }

    @Test
    fun `should find playlist musics ordered by position`() {
        val playlistId = UUID.randomUUID()
        val playlistMusics = listOf(
            PlaylistMusic(UUID.randomUUID(), playlistId, "music1", 0),
            PlaylistMusic(UUID.randomUUID(), playlistId, "music2", 1)
        )

        whenever(jpaPlaylistMusicRepository.findByPlaylistIdOrderByPosition(playlistId))
            .thenReturn(playlistMusics)

        val result = playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)

        assertEquals(2, result.size)
        assertEquals(0, result[0].position)
        assertEquals(1, result[1].position)
        verify(jpaPlaylistMusicRepository).findByPlaylistIdOrderByPosition(playlistId)
    }

    @Test
    fun `should find playlist music by playlist id and music id`() {
        val playlistId = UUID.randomUUID()
        val musicId = "music123"
        val playlistMusic = PlaylistMusic(UUID.randomUUID(), playlistId, musicId, 0)

        whenever(jpaPlaylistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId))
            .thenReturn(playlistMusic)

        val result = playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)

        assertEquals(playlistMusic, result)
        verify(jpaPlaylistMusicRepository).findByPlaylistIdAndMusicId(playlistId, musicId)
    }

    @Test
    fun `should return null when playlist music not found`() {
        val playlistId = UUID.randomUUID()
        val musicId = "nonexistent"

        whenever(jpaPlaylistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId))
            .thenReturn(null)

        val result = playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)

        assertNull(result)
        verify(jpaPlaylistMusicRepository).findByPlaylistIdAndMusicId(playlistId, musicId)
    }

    @Test
    fun `should delete playlist music`() {
        val playlistMusic = PlaylistMusic(
            id = UUID.randomUUID(),
            playlistId = UUID.randomUUID(),
            musicId = "music123",
            position = 0
        )

        doNothing().whenever(jpaPlaylistMusicRepository).delete(playlistMusic)

        playlistMusicRepository.delete(playlistMusic)

        verify(jpaPlaylistMusicRepository).delete(playlistMusic)
    }

    @Test
    fun `should delete all playlist musics by playlist id`() {
        val playlistId = UUID.randomUUID()

        doNothing().whenever(jpaPlaylistMusicRepository).deleteAllByPlaylistId(playlistId)

        playlistMusicRepository.deleteAllByPlaylistId(playlistId)

        verify(jpaPlaylistMusicRepository).deleteAllByPlaylistId(playlistId)
    }

    @Test
    fun `should check if playlist music exists`() {
        val playlistId = UUID.randomUUID()
        val musicId = "music123"

        whenever(jpaPlaylistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, musicId))
            .thenReturn(true)

        val result = playlistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, musicId)

        assertTrue(result)
        verify(jpaPlaylistMusicRepository).existsByPlaylistIdAndMusicId(playlistId, musicId)
    }

    @Test
    fun `should return false when playlist music does not exist`() {
        val playlistId = UUID.randomUUID()
        val musicId = "nonexistent"

        whenever(jpaPlaylistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, musicId))
            .thenReturn(false)

        val result = playlistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, musicId)

        assertFalse(result)
        verify(jpaPlaylistMusicRepository).existsByPlaylistIdAndMusicId(playlistId, musicId)
    }

    @Test
    fun `should count playlist musics by playlist id`() {
        val playlistId = UUID.randomUUID()

        whenever(jpaPlaylistMusicRepository.countByPlaylistId(playlistId)).thenReturn(5L)

        val result = playlistMusicRepository.countByPlaylistId(playlistId)

        assertEquals(5L, result)
        verify(jpaPlaylistMusicRepository).countByPlaylistId(playlistId)
    }
}
