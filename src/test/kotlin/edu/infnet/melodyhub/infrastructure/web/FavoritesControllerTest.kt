package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.playlist.PlaylistService
import edu.infnet.melodyhub.application.playlist.dto.PlaylistWithMusicsResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.util.*

class FavoritesControllerTest {

    private lateinit var playlistService: PlaylistService
    private lateinit var favoritesController: FavoritesController

    @BeforeEach
    fun setup() {
        playlistService = mock()
        favoritesController = FavoritesController(playlistService)
    }

    private fun createFavoritesResponse(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID()
    ): PlaylistWithMusicsResponse {
        return PlaylistWithMusicsResponse(
            id = id,
            name = "Favoritos",
            description = "Músicas favoritas",
            userId = userId,
            isDefault = true,
            musics = emptyList(),
            createdAt = java.time.LocalDateTime.now(),
            updatedAt = java.time.LocalDateTime.now()
        )
    }

    @Test
    fun `should get favorites playlist`() {
        val userId = UUID.randomUUID()
        val response = createFavoritesResponse(userId = userId)

        whenever(playlistService.getFavoritesPlaylist(userId)).thenReturn(response)

        val result = favoritesController.getFavorites(userId)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body)
        verify(playlistService).getFavoritesPlaylist(userId)
    }

    @Test
    fun `should handle favorites not found`() {
        val userId = UUID.randomUUID()

        whenever(playlistService.getFavoritesPlaylist(userId))
            .thenThrow(NoSuchElementException("Favoritos não encontrados"))

        val result = favoritesController.getFavorites(userId)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `should add music to favorites`() {
        val userId = UUID.randomUUID()
        val musicId = "music123"

        doNothing().whenever(playlistService).addToFavorites(userId, musicId)

        val result = favoritesController.addToFavorites(userId, musicId)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        verify(playlistService).addToFavorites(userId, musicId)
    }

    @Test
    fun `should handle error when adding to favorites - not found`() {
        val userId = UUID.randomUUID()
        val musicId = "music123"

        whenever(playlistService.addToFavorites(userId, musicId))
            .thenThrow(NoSuchElementException("Música não encontrada"))

        val result = favoritesController.addToFavorites(userId, musicId)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `should handle error when adding to favorites - illegal argument`() {
        val userId = UUID.randomUUID()
        val musicId = "music123"

        whenever(playlistService.addToFavorites(userId, musicId))
            .thenThrow(IllegalArgumentException("Música já está nos favoritos"))

        val result = favoritesController.addToFavorites(userId, musicId)

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
    }

    @Test
    fun `should remove music from favorites`() {
        val userId = UUID.randomUUID()
        val musicId = "music123"

        doNothing().whenever(playlistService).removeFromFavorites(userId, musicId)

        val result = favoritesController.removeFromFavorites(musicId, userId)

        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
        verify(playlistService).removeFromFavorites(userId, musicId)
    }

    @Test
    fun `should handle error when removing from favorites - not found`() {
        val userId = UUID.randomUUID()
        val musicId = "music123"

        whenever(playlistService.removeFromFavorites(userId, musicId))
            .thenThrow(NoSuchElementException("Música não encontrada nos favoritos"))

        val result = favoritesController.removeFromFavorites(musicId, userId)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
    }

    @Test
    fun `should handle error when removing from favorites - illegal argument`() {
        val userId = UUID.randomUUID()
        val musicId = "music123"

        whenever(playlistService.removeFromFavorites(userId, musicId))
            .thenThrow(IllegalArgumentException("Erro ao remover"))

        val result = favoritesController.removeFromFavorites(musicId, userId)

        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
    }
}
