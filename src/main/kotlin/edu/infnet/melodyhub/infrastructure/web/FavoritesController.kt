package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.playlist.PlaylistService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/favorites")
class FavoritesController(
    private val playlistService: PlaylistService
) {

    @GetMapping
    fun getFavorites(@RequestParam userId: UUID): ResponseEntity<Any> {
        return try {
            val response = playlistService.getFavoritesPlaylist(userId)
            ResponseEntity.ok(response)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Favoritos não encontrados"))
        }
    }

    @PostMapping
    fun addToFavorites(
        @RequestParam userId: UUID,
        @RequestParam musicId: String
    ): ResponseEntity<Any> {
        return try {
            playlistService.addToFavorites(userId, musicId)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(mapOf("message" to "Música adicionada aos favoritos"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Não encontrado"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao adicionar aos favoritos"))
        }
    }

    @DeleteMapping("/{musicId}")
    fun removeFromFavorites(
        @PathVariable musicId: String,
        @RequestParam userId: UUID
    ): ResponseEntity<Any> {
        return try {
            playlistService.removeFromFavorites(userId, musicId)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Não encontrado"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao remover dos favoritos"))
        }
    }
}
