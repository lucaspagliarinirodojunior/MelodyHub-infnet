package edu.infnet.melodyhub.domain.music

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date

@Document(collection = "music")
data class Music(
    @Id
    val id: String? = null,
    val fileName: String,
    val contentType: String,
    val size: Long,
    val fileId: String, // ID of the file in GridFS
    val uploadDate: Date = Date()
)
