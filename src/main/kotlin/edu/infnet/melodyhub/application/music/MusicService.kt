package edu.infnet.melodyhub.application.music

import edu.infnet.melodyhub.domain.music.Music
import edu.infnet.melodyhub.domain.music.MusicRepository
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

@Service
class MusicService(
    private val gridFsTemplate: GridFsTemplate,
    private val musicRepository: MusicRepository
) {

    fun uploadMusic(file: MultipartFile): String {
        val allowedTypes = listOf("audio/mpeg", "audio/aac", "audio/flac")
        if (file.contentType !in allowedTypes) {
            throw IllegalArgumentException("Invalid file type. Only MP3, AAC, and FLAC are allowed.")
        }

        val metadata = Document()
        metadata["contentType"] = file.contentType
        metadata["size"] = file.size
        metadata["originalFilename"] = file.originalFilename

        val fileId = gridFsTemplate.store(
            file.inputStream,
            file.originalFilename,
            file.contentType,
            metadata
        )

        val music = Music(
            fileName = file.originalFilename!!,
            contentType = file.contentType!!,
            size = file.size,
            fileId = fileId.toHexString()
        )

        val savedMusic = musicRepository.save(music)

        return savedMusic.id!!
    }

    fun downloadMusic(musicId: String): Pair<GridFsResource, Music> {
        val music = musicRepository.findById(musicId)
            .orElseThrow { IllegalArgumentException("Music not found with id: $musicId") }

        val gridFsFile = gridFsTemplate.findOne(Query(Criteria.where("_id").`is`(ObjectId(music.fileId))))

        val resource = gridFsTemplate.getResource(gridFsFile)

        return Pair(resource, music)
    }

    fun getAllMusic(): List<Music> {
        return musicRepository.findAll()
    }
}
