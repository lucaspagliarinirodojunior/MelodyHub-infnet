package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.playlist.PlaylistService
import edu.infnet.melodyhub.application.playlist.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.util.*

class PlaylistControllerTest {

    private lateinit var playlistService: PlaylistService
    private lateinit var playlistController: PlaylistController

    @BeforeEach
    fun setup() {
        playlistService = mock()
        playlistController = PlaylistController(playlistService)
    }

    private fun createPlaylistResponse(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        name: String = "My Playlist",
        musicCount: Long = 0
    ): PlaylistResponse {
        return PlaylistResponse(
            id = id,
            name = name,
            description = "Test playlist",
            userId = userId,
            isDefault = false,
            musicCount = musicCount,
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )
    }

    @Test
    fun `should create playlist successfully`() {
        val userId = UUID.randomUUID()
        val request = CreatePlaylistRequest(
            name = "My Playlist",
            description = "Test",
            userId = userId
        )
        val response = createPlaylistResponse(userId = userId)

        whenever(playlistService.createPlaylist(request)).thenReturn(response)

        val result = playlistController.createPlaylist(request)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(response, result.body)
    }

    @Test
    fun `should handle error when creating playlist`() {
        val request = CreatePlaylistRequest(
            name = "My Playlist",
            description = "Test",
            userId = UUID.randomUUID()
        )

        whenever(playlistService.createPlaylist(request))
            .thenThrow(IllegalArgumentException("Nome inválido"))

        val result = playlistController.createPlaylist(request)

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
    }

    @Test
    fun `should get playlist by id`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val response = createPlaylistResponse(id = playlistId)

        whenever(playlistService.getPlaylistById(playlistId, userId)).thenReturn(response)

        val result = playlistController.getPlaylistById(playlistId, userId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
    }

    @Test
    fun `should handle playlist not found`() {
        val playlistId = UUID.randomUUID()

        whenever(playlistService.getPlaylistById(playlistId, null))
            .thenThrow(NoSuchElementException("Playlist não encontrada"))

        val result = playlistController.getPlaylistById(playlistId, null)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `should handle forbidden access to private playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        whenever(playlistService.getPlaylistById(playlistId, userId))
            .thenThrow(IllegalArgumentException("Acesso negado"))

        val result = playlistController.getPlaylistById(playlistId, userId)

        assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    fun `should get user playlists`() {
        val userId = UUID.randomUUID()
        val playlists = listOf(
            createPlaylistResponse(userId = userId),
            createPlaylistResponse(userId = userId)
        )

        whenever(playlistService.getUserPlaylists(userId)).thenReturn(playlists)

        val result = playlistController.getUserPlaylists(userId)

        assertEquals(HttpStatus.OK, result.statusCode)
    }

    @Test
    fun `should update playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val request = UpdatePlaylistRequest(name = "Updated Name")
        val response = createPlaylistResponse(id = playlistId, name = "Updated Name")

        whenever(playlistService.updatePlaylist(playlistId, userId, request)).thenReturn(response)

        val result = playlistController.updatePlaylist(playlistId, userId, request)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
    }

    @Test
    fun `should delete playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        doNothing().whenever(playlistService).deletePlaylist(playlistId, userId)

        val result = playlistController.deletePlaylist(playlistId, userId)

        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
    }

    @Test
    fun `should add music to playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val request = AddMusicToPlaylistRequest("music123")

        doNothing().whenever(playlistService).addMusicToPlaylist(playlistId, userId, request)

        val result = playlistController.addMusicToPlaylist(playlistId, userId, request)

        assertEquals(HttpStatus.CREATED, result.statusCode)
    }

    @Test
    fun `should remove music from playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"

        doNothing().whenever(playlistService).removeMusicFromPlaylist(playlistId, musicId, userId)

        val result = playlistController.removeMusicFromPlaylist(playlistId, musicId, userId)

        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
    }
}
