package edu.infnet.melodyhub.infrastructure.playlist

import edu.infnet.melodyhub.domain.playlist.Playlist
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.*

class PlaylistRepositoryImplTest {

    private lateinit var jpaPlaylistRepository: JpaPlaylistRepository
    private lateinit var playlistRepository: PlaylistRepositoryImpl

    @BeforeEach
    fun setup() {
        jpaPlaylistRepository = mock()
        playlistRepository = PlaylistRepositoryImpl(jpaPlaylistRepository)
    }

    @Test
    fun `should save playlist`() {
        val playlist = Playlist(
            id = UUID.randomUUID(),
            name = "My Playlist",
            description = "Test playlist",
            userId = UUID.randomUUID(),
            isDefault = false
        )

        whenever(jpaPlaylistRepository.save(playlist)).thenReturn(playlist)

        val result = playlistRepository.save(playlist)

        assertEquals(playlist, result)
        verify(jpaPlaylistRepository).save(playlist)
    }

    @Test
    fun `should find playlist by id`() {
        val playlistId = UUID.randomUUID()
        val playlist = Playlist(
            id = playlistId,
            name = "My Playlist",
            description = "Test playlist",
            userId = UUID.randomUUID(),
            isDefault = false
        )

        whenever(jpaPlaylistRepository.findById(playlistId)).thenReturn(Optional.of(playlist))

        val result = playlistRepository.findById(playlistId)

        assertEquals(playlist, result)
        verify(jpaPlaylistRepository).findById(playlistId)
    }

    @Test
    fun `should return null when playlist not found by id`() {
        val playlistId = UUID.randomUUID()

        whenever(jpaPlaylistRepository.findById(playlistId)).thenReturn(Optional.empty())

        val result = playlistRepository.findById(playlistId)

        assertNull(result)
        verify(jpaPlaylistRepository).findById(playlistId)
    }

    @Test
    fun `should find playlists by user id`() {
        val userId = UUID.randomUUID()
        val playlists = listOf(
            Playlist(UUID.randomUUID(), "Playlist 1", "Desc 1", userId, false),
            Playlist(UUID.randomUUID(), "Playlist 2", "Desc 2", userId, true)
        )

        whenever(jpaPlaylistRepository.findByUserId(userId)).thenReturn(playlists)

        val result = playlistRepository.findByUserId(userId)

        assertEquals(2, result.size)
        assertEquals(playlists, result)
        verify(jpaPlaylistRepository).findByUserId(userId)
    }

    @Test
    fun `should find playlists by user id and is default`() {
        val userId = UUID.randomUUID()
        val defaultPlaylists = listOf(
            Playlist(UUID.randomUUID(), "Favorites", "Default favorites", userId, true)
        )

        whenever(jpaPlaylistRepository.findByUserIdAndIsDefault(userId, true)).thenReturn(defaultPlaylists)

        val result = playlistRepository.findByUserIdAndIsDefault(userId, true)

        assertEquals(1, result.size)
        assertTrue(result[0].isDefault)
        verify(jpaPlaylistRepository).findByUserIdAndIsDefault(userId, true)
    }

    @Test
    fun `should delete playlist`() {
        val playlist = Playlist(
            id = UUID.randomUUID(),
            name = "My Playlist",
            description = "Test playlist",
            userId = UUID.randomUUID(),
            isDefault = false
        )

        doNothing().whenever(jpaPlaylistRepository).delete(playlist)

        playlistRepository.delete(playlist)

        verify(jpaPlaylistRepository).delete(playlist)
    }

    @Test
    fun `should check if playlist exists by user id and name`() {
        val userId = UUID.randomUUID()
        val name = "My Playlist"

        whenever(jpaPlaylistRepository.existsByUserIdAndName(userId, name)).thenReturn(true)

        val result = playlistRepository.existsByUserIdAndName(userId, name)

        assertTrue(result)
        verify(jpaPlaylistRepository).existsByUserIdAndName(userId, name)
    }

    @Test
    fun `should return false when playlist does not exist by user id and name`() {
        val userId = UUID.randomUUID()
        val name = "Nonexistent Playlist"

        whenever(jpaPlaylistRepository.existsByUserIdAndName(userId, name)).thenReturn(false)

        val result = playlistRepository.existsByUserIdAndName(userId, name)

        assertFalse(result)
        verify(jpaPlaylistRepository).existsByUserIdAndName(userId, name)
    }
}
