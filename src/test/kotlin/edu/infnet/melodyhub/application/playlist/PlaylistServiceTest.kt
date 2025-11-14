package edu.infnet.melodyhub.application.playlist

import edu.infnet.melodyhub.application.playlist.dto.*
import edu.infnet.melodyhub.domain.music.Music
import edu.infnet.melodyhub.domain.music.MusicRepository
import edu.infnet.melodyhub.domain.playlist.Playlist
import edu.infnet.melodyhub.domain.playlist.PlaylistMusic
import edu.infnet.melodyhub.domain.playlist.PlaylistMusicRepository
import edu.infnet.melodyhub.domain.playlist.PlaylistRepository
import edu.infnet.melodyhub.domain.user.User
import edu.infnet.melodyhub.domain.user.UserRepository
import edu.infnet.melodyhub.domain.user.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.*

class PlaylistServiceTest {

    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var playlistMusicRepository: PlaylistMusicRepository
    private lateinit var userRepository: UserRepository
    private lateinit var musicRepository: MusicRepository
    private lateinit var playlistService: PlaylistService

    @BeforeEach
    fun setup() {
        playlistRepository = mock()
        playlistMusicRepository = mock()
        userRepository = mock()
        musicRepository = mock()
        playlistService = PlaylistService(
            playlistRepository,
            playlistMusicRepository,
            userRepository,
            musicRepository
        )
    }

    private fun createTestUser(userId: UUID = UUID.randomUUID(), role: UserRole = UserRole.BASIC): User {
        return User(
            id = userId,
            name = "Test User",
            email = "test@example.com",
            password = "hashedPassword",
            role = role
        )
    }

    private fun createTestPlaylist(
        playlistId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        name: String = "Test Playlist",
        isDefault: Boolean = false,
        isPrivate: Boolean = false
    ): Playlist {
        return Playlist(
            id = playlistId,
            name = name,
            description = "Test Description",
            userId = userId,
            isDefault = isDefault,
            isPrivate = isPrivate
        )
    }

    @Test
    fun `should create playlist successfully`() {
        val userId = UUID.randomUUID()
        val user = createTestUser(userId)
        val request = CreatePlaylistRequest(
            userId = userId,
            name = "My Playlist",
            description = "My Description"
        )

        val savedPlaylist = createTestPlaylist(
            playlistId = UUID.randomUUID(),
            userId = userId,
            name = request.name
        )

        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistRepository.existsByUserIdAndName(userId, request.name)).thenReturn(false)
        whenever(playlistRepository.save(any())).thenReturn(savedPlaylist)
        whenever(playlistMusicRepository.countByPlaylistId(savedPlaylist.id!!)).thenReturn(0)

        val response = playlistService.createPlaylist(request)

        assertEquals(savedPlaylist.id, response.id)
        assertEquals(request.name, response.name)
        verify(userRepository).findById(userId)
        verify(playlistRepository).save(any())
    }

    @Test
    fun `should throw exception when creating playlist for non-existent user`() {
        val userId = UUID.randomUUID()
        val request = CreatePlaylistRequest(
            userId = userId,
            name = "My Playlist",
            description = "My Description"
        )

        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.createPlaylist(request)
        }

        assertTrue(exception.message!!.contains("Usuário não encontrado"))
        verify(playlistRepository, never()).save(any())
    }

    @Test
    fun `should throw exception when playlist name already exists`() {
        val userId = UUID.randomUUID()
        val user = createTestUser(userId)
        val request = CreatePlaylistRequest(
            userId = userId,
            name = "Existing Playlist",
            description = "Description"
        )

        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistRepository.existsByUserIdAndName(userId, request.name)).thenReturn(true)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.createPlaylist(request)
        }

        assertTrue(exception.message!!.contains("Já existe uma playlist"))
        verify(playlistRepository, never()).save(any())
    }

    @Test
    fun `should get playlist by id`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, userId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(playlistMusicRepository.countByPlaylistId(playlistId)).thenReturn(5)

        val response = playlistService.getPlaylistById(playlistId)

        assertEquals(playlistId, response.id)
        assertEquals(5, response.musicCount)
        verify(playlistRepository).findById(playlistId)
    }

    @Test
    fun `should throw exception when playlist not found`() {
        val playlistId = UUID.randomUUID()

        whenever(playlistRepository.findById(playlistId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            playlistService.getPlaylistById(playlistId)
        }

        assertTrue(exception.message!!.contains("Playlist não encontrada"))
    }

    @Test
    fun `should deny access to private playlist for non-owner`() {
        val playlistId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val requesterId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, ownerId, isPrivate = true)
        val requester = createTestUser(requesterId, UserRole.BASIC)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(requesterId)).thenReturn(requester)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.getPlaylistById(playlistId, requesterId)
        }

        assertTrue(exception.message!!.contains("não tem permissão"))
    }

    @Test
    fun `should allow admin to access private playlist`() {
        val playlistId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val adminId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, ownerId, isPrivate = true)
        val admin = createTestUser(adminId, UserRole.ADMIN)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(adminId)).thenReturn(admin)
        whenever(playlistMusicRepository.countByPlaylistId(playlistId)).thenReturn(3)

        val response = playlistService.getPlaylistById(playlistId, adminId)

        assertEquals(playlistId, response.id)
        verify(playlistRepository).findById(playlistId)
    }

    @Test
    fun `should get user playlists`() {
        val userId = UUID.randomUUID()
        val user = createTestUser(userId)
        val playlists = listOf(
            createTestPlaylist(UUID.randomUUID(), userId, "Playlist 1"),
            createTestPlaylist(UUID.randomUUID(), userId, "Playlist 2")
        )

        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistRepository.findByUserId(userId)).thenReturn(playlists)
        whenever(playlistMusicRepository.countByPlaylistId(any())).thenReturn(0)

        val response = playlistService.getUserPlaylists(userId)

        assertEquals(2, response.size)
        verify(playlistRepository).findByUserId(userId)
    }

    @Test
    fun `should update playlist successfully`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, userId, "Old Name")
        val user = createTestUser(userId)
        val request = UpdatePlaylistRequest(name = "New Name", description = "New Description")

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistRepository.existsByUserIdAndName(userId, "New Name")).thenReturn(false)
        whenever(playlistRepository.save(any())).thenReturn(playlist)
        whenever(playlistMusicRepository.countByPlaylistId(playlistId)).thenReturn(0)

        val response = playlistService.updatePlaylist(playlistId, userId, request)

        assertNotNull(response)
        verify(playlistRepository).save(any())
    }

    @Test
    fun `should not allow updating Favoritos playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val favoritesPlaylist = createTestPlaylist(playlistId, userId, "Favoritos", isDefault = true)
        val user = createTestUser(userId)
        val request = UpdatePlaylistRequest(name = "New Name")

        whenever(playlistRepository.findById(playlistId)).thenReturn(favoritesPlaylist)
        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.updatePlaylist(playlistId, userId, request)
        }

        assertTrue(exception.message!!.contains("Favoritos não pode ser editada"))
        verify(playlistRepository, never()).save(any())
    }

    @Test
    fun `should delete playlist successfully`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)

        playlistService.deletePlaylist(playlistId, userId)

        verify(playlistMusicRepository).deleteAllByPlaylistId(playlistId)
        verify(playlistRepository).delete(playlist)
    }

    @Test
    fun `should not allow deleting Favoritos playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val favoritesPlaylist = createTestPlaylist(playlistId, userId, "Favoritos", isDefault = true)
        val user = createTestUser(userId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(favoritesPlaylist)
        whenever(userRepository.findById(userId)).thenReturn(user)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.deletePlaylist(playlistId, userId)
        }

        assertTrue(exception.message!!.contains("Favoritos não pode ser deletada"))
        verify(playlistRepository, never()).delete(any())
    }

    @Test
    fun `should add music to playlist successfully`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)
        val music = mock<Music>()
        val request = AddMusicToPlaylistRequest(musicId = musicId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(musicRepository.findById(musicId)).thenReturn(Optional.of(music))
        whenever(playlistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(false)
        whenever(playlistMusicRepository.countByPlaylistId(playlistId)).thenReturn(0)

        playlistService.addMusicToPlaylist(playlistId, userId, request)

        verify(playlistMusicRepository).save(argThat {
            this is PlaylistMusic && this.musicId == musicId && this.position == 0
        })
    }

    @Test
    fun `should throw exception when adding duplicate music`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)
        val music = mock<Music>()
        val request = AddMusicToPlaylistRequest(musicId = musicId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(musicRepository.findById(musicId)).thenReturn(Optional.of(music))
        whenever(playlistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(true)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.addMusicToPlaylist(playlistId, userId, request)
        }

        assertTrue(exception.message!!.contains("já está na playlist"))
        verify(playlistMusicRepository, never()).save(any())
    }

    @Test
    fun `should remove music from playlist successfully`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)
        val playlistMusic = PlaylistMusic(
            id = UUID.randomUUID(),
            playlistId = playlistId,
            musicId = musicId,
            position = 0
        )

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(playlistMusic)
        whenever(playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)).thenReturn(emptyList())

        playlistService.removeMusicFromPlaylist(playlistId, musicId, userId)

        verify(playlistMusicRepository).delete(playlistMusic)
    }

    @Test
    fun `should get favorites playlist`() {
        val userId = UUID.randomUUID()
        val playlistId = UUID.randomUUID()
        val favoritesPlaylist = createTestPlaylist(playlistId, userId, "Favoritos", isDefault = true)

        whenever(playlistRepository.findByUserIdAndIsDefault(userId, true)).thenReturn(listOf(favoritesPlaylist))
        whenever(playlistRepository.findById(playlistId)).thenReturn(favoritesPlaylist)
        whenever(playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)).thenReturn(emptyList())
        whenever(musicRepository.findAllById(any())).thenReturn(emptyList())

        val response = playlistService.getFavoritesPlaylist(userId)

        assertEquals("Favoritos", response.name)
        verify(playlistRepository).findByUserIdAndIsDefault(userId, true)
    }

    @Test
    fun `should add to favorites`() {
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlistId = UUID.randomUUID()
        val favoritesPlaylist = createTestPlaylist(playlistId, userId, "Favoritos", isDefault = true)
        val music = mock<Music>()

        whenever(playlistRepository.findByUserIdAndIsDefault(userId, true)).thenReturn(listOf(favoritesPlaylist))
        whenever(playlistRepository.findById(playlistId)).thenReturn(favoritesPlaylist)
        whenever(userRepository.findById(userId)).thenReturn(createTestUser(userId))
        whenever(musicRepository.findById(musicId)).thenReturn(Optional.of(music))
        whenever(playlistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(false)
        whenever(playlistMusicRepository.countByPlaylistId(playlistId)).thenReturn(0)

        playlistService.addToFavorites(userId, musicId)

        verify(playlistMusicRepository).save(any())
    }

    @Test
    fun `should remove from favorites`() {
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlistId = UUID.randomUUID()
        val favoritesPlaylist = createTestPlaylist(playlistId, userId, "Favoritos", isDefault = true)
        val playlistMusic = PlaylistMusic(UUID.randomUUID(), playlistId, musicId, 0)

        whenever(playlistRepository.findByUserIdAndIsDefault(userId, true)).thenReturn(listOf(favoritesPlaylist))
        whenever(playlistRepository.findById(playlistId)).thenReturn(favoritesPlaylist)
        whenever(userRepository.findById(userId)).thenReturn(createTestUser(userId))
        whenever(playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(playlistMusic)
        whenever(playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)).thenReturn(emptyList())

        playlistService.removeFromFavorites(userId, musicId)

        verify(playlistMusicRepository).delete(playlistMusic)
    }

    @Test
    fun `should throw exception when getting playlist with musics for non-existent playlist`() {
        val playlistId = UUID.randomUUID()

        whenever(playlistRepository.findById(playlistId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            playlistService.getPlaylistWithMusics(playlistId)
        }

        assertTrue(exception.message!!.contains("não encontrada"))
    }

    @Test
    fun `should throw exception when updating playlist for non-existent user`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, userId)
        val request = UpdatePlaylistRequest(name = "New Name")

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.updatePlaylist(playlistId, userId, request)
        }

        assertTrue(exception.message!!.contains("Usuário não encontrado"))
    }

    @Test
    fun `should throw exception when user tries to update another user's playlist`() {
        val playlistId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, ownerId)
        val otherUser = createTestUser(otherUserId, UserRole.BASIC)
        val request = UpdatePlaylistRequest(name = "New Name")

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(otherUserId)).thenReturn(otherUser)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.updatePlaylist(playlistId, otherUserId, request)
        }

        assertTrue(exception.message!!.contains("não tem permissão"))
    }

    @Test
    fun `should allow admin to update another user's playlist`() {
        val playlistId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val adminId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, ownerId)
        val admin = createTestUser(adminId, UserRole.ADMIN)
        val request = UpdatePlaylistRequest(name = "New Name")

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(adminId)).thenReturn(admin)
        whenever(playlistRepository.existsByUserIdAndName(adminId, "New Name")).thenReturn(false)
        whenever(playlistRepository.save(any())).thenReturn(playlist)
        whenever(playlistMusicRepository.countByPlaylistId(playlistId)).thenReturn(0)

        playlistService.updatePlaylist(playlistId, adminId, request)

        verify(playlistRepository).save(any())
    }

    @Test
    fun `should throw exception when deleting playlist for non-existent user`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, userId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.deletePlaylist(playlistId, userId)
        }

        assertTrue(exception.message!!.contains("Usuário não encontrado"))
    }

    @Test
    fun `should throw exception when user tries to delete another user's playlist`() {
        val playlistId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, ownerId)
        val otherUser = createTestUser(otherUserId, UserRole.BASIC)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(otherUserId)).thenReturn(otherUser)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.deletePlaylist(playlistId, otherUserId)
        }

        assertTrue(exception.message!!.contains("não tem permissão"))
    }

    @Test
    fun `should allow admin to delete another user's playlist`() {
        val playlistId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val adminId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, ownerId)
        val admin = createTestUser(adminId, UserRole.ADMIN)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(adminId)).thenReturn(admin)

        playlistService.deletePlaylist(playlistId, adminId)

        verify(playlistMusicRepository).deleteAllByPlaylistId(playlistId)
        verify(playlistRepository).delete(playlist)
    }

    @Test
    fun `should throw exception when adding music to non-existent playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val request = AddMusicToPlaylistRequest("music123")

        whenever(playlistRepository.findById(playlistId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            playlistService.addMusicToPlaylist(playlistId, userId, request)
        }

        assertTrue(exception.message!!.contains("não encontrada"))
    }

    @Test
    fun `should throw exception when adding music for non-existent user`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val playlist = createTestPlaylist(playlistId, userId)
        val request = AddMusicToPlaylistRequest("music123")

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.addMusicToPlaylist(playlistId, userId, request)
        }

        assertTrue(exception.message!!.contains("Usuário não encontrado"))
    }

    @Test
    fun `should throw exception when adding non-existent music`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "nonexistent"
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)
        val request = AddMusicToPlaylistRequest(musicId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(musicRepository.findById(musicId)).thenReturn(Optional.empty())

        val exception = assertThrows<NoSuchElementException> {
            playlistService.addMusicToPlaylist(playlistId, userId, request)
        }

        assertTrue(exception.message!!.contains("Música não encontrada"))
    }

    @Test
    fun `should throw exception when user tries to add music to another user's playlist`() {
        val playlistId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val musicId = "music123"
        val playlist = createTestPlaylist(playlistId, ownerId)
        val otherUser = createTestUser(otherUserId, UserRole.BASIC)
        val request = AddMusicToPlaylistRequest(musicId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(otherUserId)).thenReturn(otherUser)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.addMusicToPlaylist(playlistId, otherUserId, request)
        }

        assertTrue(exception.message!!.contains("não tem permissão"))
    }

    @Test
    fun `should throw exception when removing music from non-existent playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"

        whenever(playlistRepository.findById(playlistId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            playlistService.removeMusicFromPlaylist(playlistId, musicId, userId)
        }

        assertTrue(exception.message!!.contains("não encontrada"))
    }

    @Test
    fun `should throw exception when removing music for non-existent user`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlist = createTestPlaylist(playlistId, userId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.removeMusicFromPlaylist(playlistId, musicId, userId)
        }

        assertTrue(exception.message!!.contains("Usuário não encontrado"))
    }

    @Test
    fun `should throw exception when removing non-existent music from playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "nonexistent"
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            playlistService.removeMusicFromPlaylist(playlistId, musicId, userId)
        }

        assertTrue(exception.message!!.contains("não encontrada na playlist"))
    }

    @Test
    fun `should throw exception when reordering music in non-existent playlist`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"

        whenever(playlistRepository.findById(playlistId)).thenReturn(null)

        val exception = assertThrows<NoSuchElementException> {
            playlistService.reorderMusic(playlistId, musicId, 0, userId)
        }

        assertTrue(exception.message!!.contains("não encontrada"))
    }

    @Test
    fun `should throw exception when getting favorites for user without favorites playlist`() {
        val userId = UUID.randomUUID()

        whenever(playlistRepository.findByUserIdAndIsDefault(userId, true)).thenReturn(emptyList())

        val exception = assertThrows<NoSuchElementException> {
            playlistService.getFavoritesPlaylist(userId)
        }

        assertTrue(exception.message!!.contains("favoritos não encontrada"))
    }

    @Test
    fun `should reorder music successfully`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)
        val playlistMusic = PlaylistMusic(UUID.randomUUID(), playlistId, musicId, 0)
        val allMusics = listOf(
            playlistMusic,
            PlaylistMusic(UUID.randomUUID(), playlistId, "music2", 1),
            PlaylistMusic(UUID.randomUUID(), playlistId, "music3", 2)
        )

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(playlistMusic)
        whenever(playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)).thenReturn(allMusics)

        playlistService.reorderMusic(playlistId, musicId, 2, userId)

        verify(playlistMusicRepository, atLeastOnce()).save(any())
    }

    @Test
    fun `should not reorder when music is already at target position`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)
        val playlistMusic = PlaylistMusic(UUID.randomUUID(), playlistId, musicId, 1)
        val allMusics = listOf(
            PlaylistMusic(UUID.randomUUID(), playlistId, "music1", 0),
            playlistMusic,
            PlaylistMusic(UUID.randomUUID(), playlistId, "music3", 2)
        )

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(playlistMusic)
        whenever(playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)).thenReturn(allMusics)

        playlistService.reorderMusic(playlistId, musicId, 1, userId)

        verify(playlistMusicRepository, never()).save(any())
    }

    @Test
    fun `should throw exception when reordering with invalid position`() {
        val playlistId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val musicId = "music123"
        val playlist = createTestPlaylist(playlistId, userId)
        val user = createTestUser(userId)
        val playlistMusic = PlaylistMusic(UUID.randomUUID(), playlistId, musicId, 0)
        val allMusics = listOf(playlistMusic)

        whenever(playlistRepository.findById(playlistId)).thenReturn(playlist)
        whenever(userRepository.findById(userId)).thenReturn(user)
        whenever(playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)).thenReturn(playlistMusic)
        whenever(playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)).thenReturn(allMusics)

        val exception = assertThrows<IllegalArgumentException> {
            playlistService.reorderMusic(playlistId, musicId, 10, userId)
        }

        assertTrue(exception.message!!.contains("Posição inválida"))
    }
}
