package edu.infnet.melodyhub.application.music

import com.mongodb.client.gridfs.model.GridFSFile
import edu.infnet.melodyhub.domain.music.Music
import edu.infnet.melodyhub.domain.music.MusicRepository
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.*

class MusicServiceTest {

    private lateinit var gridFsTemplate: GridFsTemplate
    private lateinit var musicRepository: MusicRepository
    private lateinit var musicService: MusicService

    @BeforeEach
    fun setup() {
        gridFsTemplate = mock()
        musicRepository = mock()
        musicService = MusicService(gridFsTemplate, musicRepository)
    }

    private fun createMockFile(
        contentType: String = "audio/mpeg",
        filename: String = "test.mp3",
        size: Long = 1024L
    ): MultipartFile {
        val file = mock<MultipartFile>()
        whenever(file.contentType).thenReturn(contentType)
        whenever(file.originalFilename).thenReturn(filename)
        whenever(file.size).thenReturn(size)
        whenever(file.inputStream).thenReturn(ByteArrayInputStream(ByteArray(size.toInt())))
        return file
    }

    @Test
    fun `should upload MP3 file successfully`() {
        val file = createMockFile("audio/mpeg", "song.mp3", 2048L)
        val fileId = ObjectId()

        whenever(gridFsTemplate.store(any(), any(), any(), any())).thenReturn(fileId)

        doAnswer { invocation ->
            val music = invocation.getArgument<Music>(0)
            music.copy(id = "generatedId")
        }.whenever(musicRepository).save(any())

        val result = musicService.uploadMusic(file)

        assertNotNull(result)
        assertEquals("generatedId", result)
        verify(gridFsTemplate).store(any(), eq("song.mp3"), eq("audio/mpeg"), any())
        verify(musicRepository).save(argThat {
            this.fileName == "song.mp3" && this.contentType == "audio/mpeg"
        })
    }

    @Test
    fun `should upload AAC file successfully`() {
        val file = createMockFile("audio/aac", "song.aac", 1024L)
        val fileId = ObjectId()

        whenever(gridFsTemplate.store(any(), any(), any(), any())).thenReturn(fileId)

        doAnswer { invocation ->
            val music = invocation.getArgument<Music>(0)
            music.copy(id = "generatedId")
        }.whenever(musicRepository).save(any())

        val result = musicService.uploadMusic(file)

        assertNotNull(result)
        assertEquals("generatedId", result)
        verify(musicRepository).save(any())
    }

    @Test
    fun `should upload FLAC file successfully`() {
        val file = createMockFile("audio/flac", "song.flac", 4096L)
        val fileId = ObjectId()

        whenever(gridFsTemplate.store(any(), any(), any(), any())).thenReturn(fileId)

        doAnswer { invocation ->
            val music = invocation.getArgument<Music>(0)
            music.copy(id = "generatedId")
        }.whenever(musicRepository).save(any())

        val result = musicService.uploadMusic(file)

        assertNotNull(result)
        assertEquals("generatedId", result)
        verify(musicRepository).save(any())
    }

    @Test
    fun `should accept audio x-flac content type`() {
        val file = createMockFile("audio/x-flac", "song.flac", 2048L)
        val fileId = ObjectId()

        whenever(gridFsTemplate.store(any(), any(), any(), any())).thenReturn(fileId)

        doAnswer { invocation ->
            val music = invocation.getArgument<Music>(0)
            music.copy(id = "generatedId")
        }.whenever(musicRepository).save(any())

        val result = musicService.uploadMusic(file)

        assertNotNull(result)
        assertEquals("generatedId", result)
        verify(musicRepository).save(any())
    }

    @Test
    fun `should reject invalid file type`() {
        val file = createMockFile("video/mp4", "video.mp4")

        val exception = assertThrows<IllegalArgumentException> {
            musicService.uploadMusic(file)
        }

        assertTrue(exception.message!!.contains("Invalid file type"))
        verify(gridFsTemplate, never()).store(any(), any(), any(), any())
        verify(musicRepository, never()).save(any())
    }

    @Test
    fun `should reject PDF file`() {
        val file = createMockFile("application/pdf", "document.pdf")

        val exception = assertThrows<IllegalArgumentException> {
            musicService.uploadMusic(file)
        }

        assertTrue(exception.message!!.contains("Invalid file type"))
    }

    @Test
    fun `should reject WAV file`() {
        val file = createMockFile("audio/wav", "song.wav")

        val exception = assertThrows<IllegalArgumentException> {
            musicService.uploadMusic(file)
        }

        assertTrue(exception.message!!.contains("Invalid file type"))
    }

    @Test
    fun `should download music successfully`() {
        val musicId = "music123"
        val fileId = ObjectId()
        val music = Music(
            id = musicId,
            fileName = "song.mp3",
            contentType = "audio/mpeg",
            size = 2048L,
            fileId = fileId.toHexString()
        )

        val gridFsFile = mock<GridFSFile>()
        val gridFsResource = mock<GridFsResource>()

        whenever(musicRepository.findById(musicId)).thenReturn(Optional.of(music))
        whenever(gridFsTemplate.findOne(any<Query>())).thenReturn(gridFsFile)
        whenever(gridFsTemplate.getResource(gridFsFile)).thenReturn(gridFsResource)

        val (resource, downloadedMusic) = musicService.downloadMusic(musicId)

        assertEquals(gridFsResource, resource)
        assertEquals(music, downloadedMusic)
        verify(musicRepository).findById(musicId)
        verify(gridFsTemplate).findOne(any<Query>())
        verify(gridFsTemplate).getResource(gridFsFile)
    }

    @Test
    fun `should throw exception when downloading non-existent music`() {
        val musicId = "nonexistent"

        whenever(musicRepository.findById(musicId)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            musicService.downloadMusic(musicId)
        }

        assertTrue(exception.message!!.contains("Music not found"))
        verify(gridFsTemplate, never()).findOne(any<Query>())
    }

    @Test
    fun `should get all music`() {
        val musics = listOf(
            Music(
                id = "music1",
                fileName = "song1.mp3",
                contentType = "audio/mpeg",
                size = 1024L,
                fileId = ObjectId().toHexString()
            ),
            Music(
                id = "music2",
                fileName = "song2.aac",
                contentType = "audio/aac",
                size = 2048L,
                fileId = ObjectId().toHexString()
            )
        )

        whenever(musicRepository.findAll()).thenReturn(musics)

        val result = musicService.getAllMusic()

        assertEquals(2, result.size)
        assertEquals("song1.mp3", result[0].fileName)
        assertEquals("song2.aac", result[1].fileName)
        verify(musicRepository).findAll()
    }

    @Test
    fun `should return empty list when no music exists`() {
        whenever(musicRepository.findAll()).thenReturn(emptyList())

        val result = musicService.getAllMusic()

        assertTrue(result.isEmpty())
        verify(musicRepository).findAll()
    }
}
