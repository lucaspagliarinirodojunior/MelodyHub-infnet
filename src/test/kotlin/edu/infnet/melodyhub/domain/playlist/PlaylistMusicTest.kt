package edu.infnet.melodyhub.domain.playlist

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class PlaylistMusicTest {

    @Test
    fun `should create playlist music with default position`() {
        val playlistId = UUID.randomUUID()
        val musicId = "music123"

        val playlistMusic = PlaylistMusic(
            playlistId = playlistId,
            musicId = musicId
        )

        assertEquals(playlistId, playlistMusic.playlistId)
        assertEquals(musicId, playlistMusic.musicId)
        assertEquals(0, playlistMusic.position)
    }

    @Test
    fun `should create playlist music with specified position`() {
        val playlistId = UUID.randomUUID()
        val musicId = "music123"
        val position = 5

        val playlistMusic = PlaylistMusic(
            playlistId = playlistId,
            musicId = musicId,
            position = position
        )

        assertEquals(position, playlistMusic.position)
    }

    @Test
    fun `should update position successfully`() {
        val playlistMusic = PlaylistMusic(
            playlistId = UUID.randomUUID(),
            musicId = "music123",
            position = 0
        )

        playlistMusic.updatePosition(3)

        assertEquals(3, playlistMusic.position)
    }

    @Test
    fun `should throw exception when updating position to negative value`() {
        val playlistMusic = PlaylistMusic(
            playlistId = UUID.randomUUID(),
            musicId = "music123",
            position = 0
        )

        val exception = assertThrows<IllegalArgumentException> {
            playlistMusic.updatePosition(-1)
        }

        assertEquals("Posição não pode ser negativa", exception.message)
    }

    @Test
    fun `should update position to zero`() {
        val playlistMusic = PlaylistMusic(
            playlistId = UUID.randomUUID(),
            musicId = "music123",
            position = 5
        )

        playlistMusic.updatePosition(0)

        assertEquals(0, playlistMusic.position)
    }

    @Test
    fun `should maintain equality based on id`() {
        val id = UUID.randomUUID()
        val playlistMusic1 = PlaylistMusic(
            id = id,
            playlistId = UUID.randomUUID(),
            musicId = "music123",
            position = 0
        )
        val playlistMusic2 = PlaylistMusic(
            id = id,
            playlistId = UUID.randomUUID(),
            musicId = "music456",
            position = 1
        )

        assertEquals(playlistMusic1, playlistMusic2)
        assertEquals(playlistMusic1.hashCode(), playlistMusic2.hashCode())
    }

    @Test
    fun `should have different equality for different ids`() {
        val playlistMusic1 = PlaylistMusic(
            id = UUID.randomUUID(),
            playlistId = UUID.randomUUID(),
            musicId = "music123",
            position = 0
        )
        val playlistMusic2 = PlaylistMusic(
            id = UUID.randomUUID(),
            playlistId = UUID.randomUUID(),
            musicId = "music123",
            position = 0
        )

        assertNotEquals(playlistMusic1, playlistMusic2)
    }
}
