package edu.infnet.melodyhub.domain.playlist

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "playlists")
class Playlist(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    var name: String,

    @Column
    var description: String? = null,

    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    var isDefault: Boolean = false, // Para playlists padrão do sistema (ex: Favoritos)

    @Column(nullable = false)
    var isPrivate: Boolean = false, // Playlists privadas só podem ser vistas pelo dono ou ADMIN

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun updateName(newName: String) {
        require(!isFavoritesPlaylist()) {
            "O nome da playlist de Favoritos não pode ser alterado"
        }
        require(newName.isNotBlank()) { "Nome da playlist não pode ser vazio" }
        this.name = newName
        this.updatedAt = LocalDateTime.now()
    }

    fun updateDescription(newDescription: String?) {
        require(!isFavoritesPlaylist()) {
            "A descrição da playlist de Favoritos não pode ser alterada"
        }
        this.description = newDescription
        this.updatedAt = LocalDateTime.now()
    }

    fun isFavoritesPlaylist(): Boolean = isDefault && name == "Favoritos"

    fun isOwnedBy(userId: UUID): Boolean = this.userId == userId

    fun canBeDeleted(): Boolean = !isFavoritesPlaylist()

    fun canBeEdited(): Boolean = !isFavoritesPlaylist()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Playlist) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Playlist(id=$id, name='$name', userId=$userId, isDefault=$isDefault)"
    }
}
