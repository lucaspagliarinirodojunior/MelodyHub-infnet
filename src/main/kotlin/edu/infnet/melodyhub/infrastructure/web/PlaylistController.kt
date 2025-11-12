package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.playlist.PlaylistService
import edu.infnet.melodyhub.application.playlist.dto.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/playlists")
class PlaylistController(
    private val playlistService: PlaylistService
) {

    @PostMapping
    fun createPlaylist(@Valid @RequestBody request: CreatePlaylistRequest): ResponseEntity<Any> {
        return try {
            val response = playlistService.createPlaylist(request)
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao criar playlist"))
        }
    }

    @GetMapping("/{id}")
    fun getPlaylistById(
        @PathVariable id: UUID,
        @RequestParam(required = false) requestingUserId: UUID?
    ): ResponseEntity<Any> {
        return try {
            val response = playlistService.getPlaylistById(id, requestingUserId)
            ResponseEntity.ok(response)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Playlist não encontrada"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse(e.message ?: "Acesso negado"))
        }
    }

    @GetMapping("/{id}/with-musics")
    fun getPlaylistWithMusics(
        @PathVariable id: UUID,
        @RequestParam(required = false) requestingUserId: UUID?
    ): ResponseEntity<Any> {
        return try {
            val response = playlistService.getPlaylistWithMusics(id, requestingUserId)
            ResponseEntity.ok(response)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Playlist não encontrada"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse(e.message ?: "Acesso negado"))
        }
    }

    @GetMapping("/user/{userId}")
    fun getUserPlaylists(@PathVariable userId: UUID): ResponseEntity<Any> {
        return try {
            val response = playlistService.getUserPlaylists(userId)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao buscar playlists"))
        }
    }

    @PatchMapping("/{id}")
    fun updatePlaylist(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdatePlaylistRequest
    ): ResponseEntity<Any> {
        return try {
            val response = playlistService.updatePlaylist(id, userId, request)
            ResponseEntity.ok(response)
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Playlist não encontrada"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao atualizar playlist"))
        }
    }

    @DeleteMapping("/{id}")
    fun deletePlaylist(
        @PathVariable id: UUID,
        @RequestParam userId: UUID
    ): ResponseEntity<Any> {
        return try {
            playlistService.deletePlaylist(id, userId)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Playlist não encontrada"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao deletar playlist"))
        }
    }

    @PostMapping("/{id}/musics")
    fun addMusicToPlaylist(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: AddMusicToPlaylistRequest
    ): ResponseEntity<Any> {
        return try {
            playlistService.addMusicToPlaylist(id, userId, request)
            ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "Música adicionada à playlist"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Não encontrado"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao adicionar música"))
        }
    }

    @DeleteMapping("/{playlistId}/musics/{musicId}")
    fun removeMusicFromPlaylist(
        @PathVariable playlistId: UUID,
        @PathVariable musicId: String,
        @RequestParam userId: UUID
    ): ResponseEntity<Any> {
        return try {
            playlistService.removeMusicFromPlaylist(playlistId, musicId, userId)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Não encontrado"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao remover música"))
        }
    }

    @PatchMapping("/{playlistId}/musics/{musicId}/reorder")
    fun reorderMusic(
        @PathVariable playlistId: UUID,
        @PathVariable musicId: String,
        @RequestParam userId: UUID,
        @RequestParam newPosition: Int
    ): ResponseEntity<Any> {
        return try {
            playlistService.reorderMusic(playlistId, musicId, newPosition, userId)
            ResponseEntity.ok(mapOf("message" to "Música reordenada com sucesso"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Não encontrado"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Erro ao reordenar música"))
        }
    }
}
