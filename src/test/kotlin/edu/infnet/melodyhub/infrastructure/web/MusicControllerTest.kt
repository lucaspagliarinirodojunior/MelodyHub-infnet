package edu.infnet.melodyhub.infrastructure.web

import edu.infnet.melodyhub.application.music.MusicService
import edu.infnet.melodyhub.domain.music.Music
import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRole
import edu.infnet.melodyhub.infrastructure.security.JwtService
import edu.infnet.melodyhub.infrastructure.user.JpaUserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.*

class MusicControllerTest {

    private lateinit var musicService: MusicService
    private lateinit var userRepository: JpaUserRepository
    private lateinit var jwtService: JwtService
    private lateinit var musicController: MusicController

    @BeforeEach
    fun setup() {
        musicService = mock()
        userRepository = mock()
        jwtService = mock()
        musicController = MusicController(musicService, userRepository, jwtService)
    }

    private fun createUser(id: UUID = UUID.randomUUID(), role: UserRole = UserRole.BASIC): User {
        return User(
            id = id,
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = role
        )
    }

    private fun createMusic(
        id: String = "music123",
        fileName: String = "song.mp3",
        contentType: String = "audio/mpeg",
        size: Long = 1024L
    ): Music {
        return Music(
            id = id,
            fileName = fileName,
            contentType = contentType,
            size = size,
            fileId = "fileId123"
        )
    }

    @Test
    fun `should upload music as admin`() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, role = UserRole.ADMIN)
        val file = mock<MultipartFile>()
        val token = "validToken"
        val authHeader = "Bearer $token"

        whenever(jwtService.validateToken(token)).thenReturn(true)
        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(musicService.uploadMusic(file)).thenReturn("music123")

        val result = musicController.uploadMusic(file, authHeader)

        assertEquals(HttpStatus.OK, result.statusCode)
        verify(musicService).uploadMusic(file)
    }

    @Test
    fun `should reject upload for non-admin user`() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, role = UserRole.BASIC)
        val file = mock<MultipartFile>()
        val token = "validToken"
        val authHeader = "Bearer $token"

        whenever(jwtService.validateToken(token)).thenReturn(true)
        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        val result = musicController.uploadMusic(file, authHeader)

        assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
        verify(musicService, never()).uploadMusic(any())
    }

    @Test
    fun `should reject upload without authorization header`() {
        val file = mock<MultipartFile>()

        val result = musicController.uploadMusic(file, null)

        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
        verify(musicService, never()).uploadMusic(any())
    }

    @Test
    fun `should reject upload with invalid token`() {
        val file = mock<MultipartFile>()
        val token = "invalidToken"
        val authHeader = "Bearer $token"

        whenever(jwtService.validateToken(token)).thenReturn(false)

        val result = musicController.uploadMusic(file, authHeader)

        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
        verify(musicService, never()).uploadMusic(any())
    }

    @Test
    fun `should download music as premium user`() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, role = UserRole.PREMIUM)
        val music = createMusic()
        val resource = mock<GridFsResource>()
        val token = "validToken"
        val authHeader = "Bearer $token"

        whenever(jwtService.validateToken(token)).thenReturn(true)
        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(musicService.downloadMusic("music123")).thenReturn(Pair(resource, music))
        whenever(resource.inputStream).thenReturn(ByteArrayInputStream(ByteArray(1024)))

        val result = musicController.downloadMusic("music123", authHeader)

        assertEquals(HttpStatus.OK, result.statusCode)
        verify(musicService).downloadMusic("music123")
    }

    @Test
    fun `should reject download for user without plan trying to download MP3`() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, role = UserRole.SEM_PLANO)
        val music = createMusic(contentType = "audio/mpeg")
        val resource = mock<GridFsResource>()
        val token = "validToken"
        val authHeader = "Bearer $token"

        whenever(jwtService.validateToken(token)).thenReturn(true)
        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(musicService.downloadMusic("music123")).thenReturn(Pair(resource, music))

        val result = musicController.downloadMusic("music123", authHeader)

        assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    fun `should reject download FLAC for basic user`() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, role = UserRole.BASIC)
        val music = createMusic(contentType = "audio/flac")
        val resource = mock<GridFsResource>()
        val token = "validToken"
        val authHeader = "Bearer $token"

        whenever(jwtService.validateToken(token)).thenReturn(true)
        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(musicService.downloadMusic("music123")).thenReturn(Pair(resource, music))

        val result = musicController.downloadMusic("music123", authHeader)

        assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    fun `should stream music as basic user`() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, role = UserRole.BASIC)
        val music = createMusic()
        val resource = mock<GridFsResource>()
        val token = "validToken"
        val authHeader = "Bearer $token"

        whenever(jwtService.validateToken(token)).thenReturn(true)
        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(musicService.downloadMusic("music123")).thenReturn(Pair(resource, music))
        whenever(resource.inputStream).thenReturn(ByteArrayInputStream(ByteArray(1024)))

        val result = musicController.streamMusic("music123", authHeader)

        assertEquals(HttpStatus.OK, result.statusCode)
        verify(musicService).downloadMusic("music123")
    }

    @Test
    fun `should reject stream FLAC for user without plan`() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId, role = UserRole.SEM_PLANO)
        val music = createMusic(contentType = "audio/flac")
        val resource = mock<GridFsResource>()
        val token = "validToken"
        val authHeader = "Bearer $token"

        whenever(jwtService.validateToken(token)).thenReturn(true)
        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(musicService.downloadMusic("music123")).thenReturn(Pair(resource, music))

        val result = musicController.streamMusic("music123", authHeader)

        assertEquals(HttpStatus.FORBIDDEN, result.statusCode)
    }

    @Test
    fun `should list music for authenticated user`() {
        val userId = UUID.randomUUID()
        val user = createUser(id = userId)
        val token = "validToken"
        val authHeader = "Bearer $token"
        val musicList = listOf(createMusic(), createMusic(id = "music456", fileName = "song2.mp3"))

        whenever(jwtService.validateToken(token)).thenReturn(true)
        whenever(jwtService.getUserIdFromToken(token)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(musicService.getAllMusic()).thenReturn(musicList)

        val result = musicController.listMusic(authHeader)

        assertEquals(HttpStatus.OK, result.statusCode)
        verify(musicService).getAllMusic()
    }

    @Test
    fun `should reject listing music without authorization`() {
        val result = musicController.listMusic(null)

        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
        verify(musicService, never()).getAllMusic()
    }
}
