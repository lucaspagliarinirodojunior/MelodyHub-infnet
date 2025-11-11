package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.music.MusicService
import edu.infnet.melodyhub.domain.user.UserRole
import edu.infnet.melodyhub.infrastructure.security.JwtService
import edu.infnet.melodyhub.infrastructure.user.JpaUserRepository
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

enum class AudioFormat {
    MP3, AAC, FLAC;

    companion object {
        fun fromContentType(contentType: String): AudioFormat {
            return when {
                contentType.contains("mpeg", ignoreCase = true) -> MP3
                contentType.contains("aac", ignoreCase = true) -> AAC
                contentType.contains("flac", ignoreCase = true) -> FLAC
                else -> MP3
            }
        }
    }
}

enum class AccessType {
    DOWNLOAD,  // Baixar arquivo
    STREAM     // Tocar online (playback)
}

@RestController
@RequestMapping("/music")
class MusicController(
    private val musicService: MusicService,
    private val userRepository: JpaUserRepository,
    private val jwtService: JwtService
) {

    /**
     * Extrai e valida o token JWT do header Authorization
     * Retorna o userId ou null se inválido
     */
    private fun extractUserIdFromToken(authHeader: String?): UUID? {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null
        }

        val token = authHeader.substring(7) // Remove "Bearer "

        return if (jwtService.validateToken(token)) {
            jwtService.getUserIdFromToken(token)
        } else {
            null
        }
    }

    @PostMapping("/upload")
    fun uploadMusic(
        @RequestParam("file") file: MultipartFile,
        @RequestHeader("Authorization") authHeader: String?
    ): ResponseEntity<*> {
        return try {
            // Extrai userId do token JWT
            val userId = extractUserIdFromToken(authHeader)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse("Token inválido ou ausente. Faça login para continuar."))

            // Busca usuário
            val user = userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("User not found") }

            // Valida se é ADMIN
            if (user.role != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse("Apenas administradores podem fazer upload de músicas."))
            }

            val musicId = musicService.uploadMusic(file)
            ResponseEntity.ok().body(mapOf("id" to musicId))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Invalid request"))
        }
    }

    @GetMapping("/download/{id}")
    fun downloadMusic(
        @PathVariable id: String,
        @RequestHeader("Authorization") authHeader: String?
    ): ResponseEntity<*> {
        return try {
            // Extrai userId do token JWT
            val userId = extractUserIdFromToken(authHeader)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse("Token inválido ou ausente. Faça login para continuar."))

            // Busca usuário
            val user = userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("User not found") }

            // Busca música
            val (resource, music) = musicService.downloadMusic(id)
            val audioFormat = AudioFormat.fromContentType(music.contentType)

            // Valida permissão de DOWNLOAD baseado na role e formato
            val canDownload = checkPermission(user.role, audioFormat, AccessType.DOWNLOAD)

            if (!canDownload) {
                val message = when {
                    user.role == UserRole.SEM_PLANO -> "Você precisa de um plano para fazer download. Assine BASIC ou PREMIUM!"
                    user.role == UserRole.BASIC && audioFormat == AudioFormat.FLAC ->
                        "Downloads FLAC são exclusivos para plano PREMIUM. Você (BASIC) pode baixar apenas MP3."
                    else -> "Você não tem permissão para fazer download desta música."
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse(message))
            }

            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(music.contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${music.fileName}\"")
                .contentLength(music.size)
                .body(InputStreamResource(resource.inputStream))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Invalid request"))
        }
    }

    @GetMapping("/stream/{id}")
    fun streamMusic(
        @PathVariable id: String,
        @RequestHeader("Authorization") authHeader: String?
    ): ResponseEntity<*> {
        return try {
            // Extrai userId do token JWT
            val userId = extractUserIdFromToken(authHeader)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse("Token inválido ou ausente. Faça login para continuar."))

            // Busca usuário
            val user = userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("User not found") }

            // Busca música
            val (resource, music) = musicService.downloadMusic(id)
            val audioFormat = AudioFormat.fromContentType(music.contentType)

            // Valida permissão de STREAM baseado na role e formato
            val canStream = checkPermission(user.role, audioFormat, AccessType.STREAM)

            if (!canStream) {
                val message = when {
                    user.role == UserRole.SEM_PLANO && audioFormat == AudioFormat.FLAC ->
                        "Playback FLAC requer plano BASIC ou superior."
                    else -> "Você não tem permissão para fazer playback desta música."
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse(message))
            }

            // Para stream, usamos inline ao invés de attachment
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(music.contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${music.fileName}\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(music.size)
                .body(InputStreamResource(resource.inputStream))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Invalid request"))
        }
    }

    @GetMapping("/list")
    fun listMusic(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<*> {
        return try {
            // Extrai userId do token JWT
            val userId = extractUserIdFromToken(authHeader)
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse("Token inválido ou ausente. Faça login para continuar."))

            // Verifica se usuário existe
            userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("User not found") }

            // Se passou nas validações, lista as músicas
            val musicList = musicService.getAllMusic()
            println("🎵 Hot-reload funcionando! Listando ${musicList.size} músicas")
            ResponseEntity.ok().body(musicList)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Error listing music"))
        }
    }

    /**
     * Valida se o usuário tem permissão para acessar a música
     *
     * Regras:
     * - SEM_PLANO: Pode fazer STREAM apenas de MP3/AAC, NÃO pode DOWNLOAD
     * - BASIC: Pode fazer STREAM de MP3/AAC/FLAC, pode DOWNLOAD apenas MP3/AAC
     * - PREMIUM: Pode fazer STREAM e DOWNLOAD de todos os formatos
     * - ADMIN: Acesso total
     */
    private fun checkPermission(role: UserRole, format: AudioFormat, accessType: AccessType): Boolean {
        return when (role) {
            UserRole.ADMIN -> true  // Admin tem acesso total

            UserRole.PREMIUM -> true  // Premium pode tudo

            UserRole.BASIC -> {
                when (accessType) {
                    AccessType.STREAM -> true  // BASIC pode fazer stream de tudo
                    AccessType.DOWNLOAD -> format != AudioFormat.FLAC  // Só não pode baixar FLAC
                }
            }

            UserRole.SEM_PLANO -> {
                when (accessType) {
                    AccessType.STREAM -> format in listOf(AudioFormat.MP3, AudioFormat.AAC)  // Só MP3/AAC
                    AccessType.DOWNLOAD -> false  // Não pode baixar nada
                }
            }
        }
    }
}
