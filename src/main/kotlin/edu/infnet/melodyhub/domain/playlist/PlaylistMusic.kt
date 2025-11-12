package edu.infnet.melodyhub.domain.playlist

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "playlist_music",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["playlist_id", "music_id"])
    ]
)
class PlaylistMusic(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "playlist_id", nullable = false)
    val playlistId: UUID,

    @Column(name = "music_id", nullable = false)
    val musicId: String, // MongoDB ObjectId as String

    @Column(nullable = false)
    var position: Int = 0, // Ordem da música na playlist

    @Column(nullable = false)
    val addedAt: LocalDateTime = LocalDateTime.now()
) {
    fun updatePosition(newPosition: Int) {
        require(newPosition >= 0) { "Posição não pode ser negativa" }
        this.position = newPosition
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlaylistMusic) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "PlaylistMusic(id=$id, playlistId=$playlistId, musicId='$musicId', position=$position)"
    }
}
