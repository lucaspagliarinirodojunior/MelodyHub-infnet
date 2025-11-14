package edu.infnet.melodyhub.domain.playlist

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class PlaylistTest {

    @Test
    fun `should create playlist with default values`() {
        val userId = UUID.randomUUID()
        val playlist = Playlist(
            name = "My Playlist",
            userId = userId
        )

        assertEquals("My Playlist", playlist.name)
        assertEquals(userId, playlist.userId)
        assertFalse(playlist.isDefault)
        assertFalse(playlist.isPrivate)
        assertNull(playlist.description)
    }

    @Test
    fun `should update playlist name successfully`() {
        val playlist = Playlist(
            name = "Original Name",
            userId = UUID.randomUUID()
        )

        playlist.updateName("New Name")

        assertEquals("New Name", playlist.name)
    }

    @Test
    fun `should throw exception when updating name with blank string`() {
        val playlist = Playlist(
            name = "Original Name",
            userId = UUID.randomUUID()
        )

        val exception = assertThrows<IllegalArgumentException> {
            playlist.updateName("")
        }

        assertEquals("Nome da playlist não pode ser vazio", exception.message)
    }

    @Test
    fun `should throw exception when updating name of Favorites playlist`() {
        val playlist = Playlist(
            name = "Favoritos",
            userId = UUID.randomUUID(),
            isDefault = true
        )

        val exception = assertThrows<IllegalArgumentException> {
            playlist.updateName("New Name")
        }

        assertEquals("O nome da playlist de Favoritos não pode ser alterado", exception.message)
    }

    @Test
    fun `should update description successfully`() {
        val playlist = Playlist(
            name = "My Playlist",
            userId = UUID.randomUUID()
        )

        playlist.updateDescription("New description")

        assertEquals("New description", playlist.description)
    }

    @Test
    fun `should throw exception when updating description of Favorites playlist`() {
        val playlist = Playlist(
            name = "Favoritos",
            userId = UUID.randomUUID(),
            isDefault = true
        )

        val exception = assertThrows<IllegalArgumentException> {
            playlist.updateDescription("New description")
        }

        assertEquals("A descrição da playlist de Favoritos não pode ser alterada", exception.message)
    }

    @Test
    fun `should identify Favorites playlist`() {
        val favoritesPlaylist = Playlist(
            name = "Favoritos",
            userId = UUID.randomUUID(),
            isDefault = true
        )

        val regularPlaylist = Playlist(
            name = "My Playlist",
            userId = UUID.randomUUID(),
            isDefault = false
        )

        val defaultButNotFavorites = Playlist(
            name = "Other Default",
            userId = UUID.randomUUID(),
            isDefault = true
        )

        assertTrue(favoritesPlaylist.isFavoritesPlaylist())
        assertFalse(regularPlaylist.isFavoritesPlaylist())
        assertFalse(defaultButNotFavorites.isFavoritesPlaylist())
    }

    @Test
    fun `should check if playlist is owned by user`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val playlist = Playlist(
            name = "My Playlist",
            userId = userId
        )

        assertTrue(playlist.isOwnedBy(userId))
        assertFalse(playlist.isOwnedBy(otherUserId))
    }

    @Test
    fun `should not allow deletion of Favorites playlist`() {
        val favoritesPlaylist = Playlist(
            name = "Favoritos",
            userId = UUID.randomUUID(),
            isDefault = true
        )

        val regularPlaylist = Playlist(
            name = "My Playlist",
            userId = UUID.randomUUID()
        )

        assertFalse(favoritesPlaylist.canBeDeleted())
        assertTrue(regularPlaylist.canBeDeleted())
    }

    @Test
    fun `should not allow editing of Favorites playlist`() {
        val favoritesPlaylist = Playlist(
            name = "Favoritos",
            userId = UUID.randomUUID(),
            isDefault = true
        )

        val regularPlaylist = Playlist(
            name = "My Playlist",
            userId = UUID.randomUUID()
        )

        assertFalse(favoritesPlaylist.canBeEdited())
        assertTrue(regularPlaylist.canBeEdited())
    }

    @Test
    fun `should maintain equality based on id`() {
        val id = UUID.randomUUID()
        val playlist1 = Playlist(
            id = id,
            name = "Playlist 1",
            userId = UUID.randomUUID()
        )
        val playlist2 = Playlist(
            id = id,
            name = "Playlist 2",
            userId = UUID.randomUUID()
        )

        assertEquals(playlist1, playlist2)
        assertEquals(playlist1.hashCode(), playlist2.hashCode())
    }

    @Test
    fun `should create private playlist`() {
        val playlist = Playlist(
            name = "Private Playlist",
            userId = UUID.randomUUID(),
            isPrivate = true
        )

        assertTrue(playlist.isPrivate)
    }
}
