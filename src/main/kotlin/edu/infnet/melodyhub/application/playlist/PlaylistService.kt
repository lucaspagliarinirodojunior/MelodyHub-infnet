package edu.infnet.melodyhub.application.playlist

import edu.infnet.melodyhub.application.playlist.dto.*
import edu.infnet.melodyhub.domain.music.MusicRepository
import edu.infnet.melodyhub.domain.playlist.Playlist
import edu.infnet.melodyhub.domain.playlist.PlaylistMusic
import edu.infnet.melodyhub.domain.playlist.PlaylistMusicRepository
import edu.infnet.melodyhub.domain.playlist.PlaylistRepository
import edu.infnet.melodyhub.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class PlaylistService(
    private val playlistRepository: PlaylistRepository,
    private val playlistMusicRepository: PlaylistMusicRepository,
    private val userRepository: UserRepository,
    private val musicRepository: MusicRepository
) {

    @Transactional
    fun createPlaylist(request: CreatePlaylistRequest): PlaylistResponse {
        // Validar se usuário existe
        userRepository.findById(request.userId)
            ?: throw IllegalArgumentException("Usuário não encontrado com ID: ${request.userId}")

        // Validar se já existe playlist com o mesmo nome para este usuário
        if (playlistRepository.existsByUserIdAndName(request.userId, request.name)) {
            throw IllegalArgumentException("Já existe uma playlist com o nome '${request.name}' para este usuário")
        }

        // Criar playlist
        val playlist = Playlist(
            name = request.name,
            description = request.description,
            userId = request.userId,
            isDefault = false
        )

        val savedPlaylist = playlistRepository.save(playlist)

        return PlaylistResponse.from(savedPlaylist, 0)
    }

    @Transactional(readOnly = true)
    fun getPlaylistById(playlistId: UUID, requestingUserId: UUID? = null): PlaylistResponse {
        val playlist = playlistRepository.findById(playlistId)
            ?: throw NoSuchElementException("Playlist não encontrada com ID: $playlistId")

        // Validar acesso a playlists privadas
        if (playlist.isPrivate && requestingUserId != null) {
            val requestingUser = userRepository.findById(requestingUserId)
            val isAdmin = requestingUser?.isAdmin() ?: false
            val isOwner = playlist.isOwnedBy(requestingUserId)

            if (!isAdmin && !isOwner) {
                throw IllegalArgumentException("Você não tem permissão para visualizar esta playlist")
            }
        }

        val musicCount = playlistMusicRepository.countByPlaylistId(playlistId)

        return PlaylistResponse.from(playlist, musicCount)
    }

    @Transactional(readOnly = true)
    fun getPlaylistWithMusics(playlistId: UUID, requestingUserId: UUID? = null): PlaylistWithMusicsResponse {
        val playlist = playlistRepository.findById(playlistId)
            ?: throw NoSuchElementException("Playlist não encontrada com ID: $playlistId")

        // Validar acesso a playlists privadas
        if (playlist.isPrivate && requestingUserId != null) {
            val requestingUser = userRepository.findById(requestingUserId)
            val isAdmin = requestingUser?.isAdmin() ?: false
            val isOwner = playlist.isOwnedBy(requestingUserId)

            if (!isAdmin && !isOwner) {
                throw IllegalArgumentException("Você não tem permissão para visualizar esta playlist")
            }
        }

        val playlistMusics = playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)

        // Buscar informações das músicas no MongoDB
        val musicIds = playlistMusics.map { it.musicId }
        val musics = musicRepository.findAllById(musicIds)

        return PlaylistWithMusicsResponse.from(playlist, playlistMusics, musics)
    }

    @Transactional(readOnly = true)
    fun getUserPlaylists(userId: UUID): List<PlaylistResponse> {
        // Validar se usuário existe
        userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuário não encontrado com ID: $userId")

        val playlists = playlistRepository.findByUserId(userId)

        return playlists.map { playlist ->
            val musicCount = playlistMusicRepository.countByPlaylistId(playlist.id!!)
            PlaylistResponse.from(playlist, musicCount)
        }
    }

    @Transactional
    fun updatePlaylist(playlistId: UUID, userId: UUID, request: UpdatePlaylistRequest): PlaylistResponse {
        val playlist = playlistRepository.findById(playlistId)
            ?: throw NoSuchElementException("Playlist não encontrada com ID: $playlistId")

        // Verificar se é ADMIN (tem bypass total)
        val requestingUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuário não encontrado com ID: $userId")
        val isAdmin = requestingUser.isAdmin()

        // Validar se a playlist pertence ao usuário (ADMIN tem bypass)
        if (!isAdmin && !playlist.isOwnedBy(userId)) {
            throw IllegalArgumentException("Você não tem permissão para editar esta playlist")
        }

        // Validar se a playlist pode ser editada (Favoritos NUNCA podem ser editados, nem por ADMIN)
        if (!playlist.canBeEdited()) {
            throw IllegalArgumentException("A playlist de Favoritos não pode ser editada")
        }

        // Atualizar campos
        request.name?.let {
            if (it.isNotBlank()) {
                // Verificar se já existe outra playlist com o mesmo nome
                if (playlistRepository.existsByUserIdAndName(userId, it) &&
                    playlist.name != it
                ) {
                    throw IllegalArgumentException("Já existe uma playlist com o nome '$it'")
                }
                playlist.updateName(it)
            }
        }

        request.description?.let {
            playlist.updateDescription(it)
        }

        val updatedPlaylist = playlistRepository.save(playlist)
        val musicCount = playlistMusicRepository.countByPlaylistId(playlistId)

        return PlaylistResponse.from(updatedPlaylist, musicCount)
    }

    @Transactional
    fun deletePlaylist(playlistId: UUID, userId: UUID) {
        val playlist = playlistRepository.findById(playlistId)
            ?: throw NoSuchElementException("Playlist não encontrada com ID: $playlistId")

        // Verificar se é ADMIN (tem bypass total)
        val requestingUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuário não encontrado com ID: $userId")
        val isAdmin = requestingUser.isAdmin()

        // Validar se a playlist pertence ao usuário (ADMIN tem bypass)
        if (!isAdmin && !playlist.isOwnedBy(userId)) {
            throw IllegalArgumentException("Você não tem permissão para deletar esta playlist")
        }

        // Validar se a playlist pode ser deletada (Favoritos NUNCA podem ser deletados, nem por ADMIN)
        if (!playlist.canBeDeleted()) {
            throw IllegalArgumentException("A playlist de Favoritos não pode ser deletada")
        }

        // Deletar todas as músicas da playlist primeiro
        playlistMusicRepository.deleteAllByPlaylistId(playlistId)

        // Deletar a playlist
        playlistRepository.delete(playlist)
    }

    @Transactional
    fun addMusicToPlaylist(playlistId: UUID, userId: UUID, request: AddMusicToPlaylistRequest) {
        val playlist = playlistRepository.findById(playlistId)
            ?: throw NoSuchElementException("Playlist não encontrada com ID: $playlistId")

        // Verificar se é ADMIN (tem bypass total)
        val requestingUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuário não encontrado com ID: $userId")
        val isAdmin = requestingUser.isAdmin()

        // Validar se a playlist pertence ao usuário (ADMIN tem bypass)
        if (!isAdmin && !playlist.isOwnedBy(userId)) {
            throw IllegalArgumentException("Você não tem permissão para adicionar músicas nesta playlist")
        }

        // Validar se a música existe
        musicRepository.findById(request.musicId).orElse(null)
            ?: throw NoSuchElementException("Música não encontrada com ID: ${request.musicId}")

        // Validar se a música já está na playlist
        if (playlistMusicRepository.existsByPlaylistIdAndMusicId(playlistId, request.musicId)) {
            throw IllegalArgumentException("Esta música já está na playlist")
        }

        // Calcular próxima posição
        val currentCount = playlistMusicRepository.countByPlaylistId(playlistId)
        val nextPosition = currentCount.toInt()

        // Adicionar música à playlist
        val playlistMusic = PlaylistMusic(
            playlistId = playlistId,
            musicId = request.musicId,
            position = nextPosition
        )

        playlistMusicRepository.save(playlistMusic)
    }

    @Transactional
    fun removeMusicFromPlaylist(playlistId: UUID, musicId: String, userId: UUID) {
        val playlist = playlistRepository.findById(playlistId)
            ?: throw NoSuchElementException("Playlist não encontrada com ID: $playlistId")

        // Verificar se é ADMIN (tem bypass total)
        val requestingUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuário não encontrado com ID: $userId")
        val isAdmin = requestingUser.isAdmin()

        // Validar se a playlist pertence ao usuário (ADMIN tem bypass)
        if (!isAdmin && !playlist.isOwnedBy(userId)) {
            throw IllegalArgumentException("Você não tem permissão para remover músicas desta playlist")
        }

        // Buscar a relação playlist-música
        val playlistMusic = playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)
            ?: throw NoSuchElementException("Música não encontrada na playlist")

        // Remover a música
        playlistMusicRepository.delete(playlistMusic)

        // Reorganizar posições das músicas restantes
        val remainingMusics = playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)
        remainingMusics.forEachIndexed { index, pm ->
            if (pm.position != index) {
                pm.updatePosition(index)
                playlistMusicRepository.save(pm)
            }
        }
    }

    @Transactional
    fun reorderMusic(playlistId: UUID, musicId: String, newPosition: Int, userId: UUID) {
        val playlist = playlistRepository.findById(playlistId)
            ?: throw NoSuchElementException("Playlist não encontrada com ID: $playlistId")

        // Verificar se é ADMIN (tem bypass total)
        val requestingUser = userRepository.findById(userId)
            ?: throw IllegalArgumentException("Usuário não encontrado com ID: $userId")
        val isAdmin = requestingUser.isAdmin()

        // Validar se a playlist pertence ao usuário (ADMIN tem bypass)
        if (!isAdmin && !playlist.isOwnedBy(userId)) {
            throw IllegalArgumentException("Você não tem permissão para reordenar músicas nesta playlist")
        }

        val playlistMusic = playlistMusicRepository.findByPlaylistIdAndMusicId(playlistId, musicId)
            ?: throw NoSuchElementException("Música não encontrada na playlist")

        val allMusics = playlistMusicRepository.findByPlaylistIdOrderByPosition(playlistId)

        // Validar posição
        if (newPosition < 0 || newPosition >= allMusics.size) {
            throw IllegalArgumentException("Posição inválida: $newPosition. Deve estar entre 0 e ${allMusics.size - 1}")
        }

        val oldPosition = playlistMusic.position

        if (oldPosition == newPosition) {
            return // Nada a fazer
        }

        // Reordenar: mover música para nova posição
        if (oldPosition < newPosition) {
            // Movendo para baixo: decrementar posições entre old e new
            allMusics.forEach { pm ->
                if (pm.position in (oldPosition + 1)..newPosition) {
                    pm.updatePosition(pm.position - 1)
                    playlistMusicRepository.save(pm)
                }
            }
        } else {
            // Movendo para cima: incrementar posições entre new e old
            allMusics.forEach { pm ->
                if (pm.position in newPosition until oldPosition) {
                    pm.updatePosition(pm.position + 1)
                    playlistMusicRepository.save(pm)
                }
            }
        }

        // Atualizar posição da música movida
        playlistMusic.updatePosition(newPosition)
        playlistMusicRepository.save(playlistMusic)
    }

    // ========== MÉTODOS ESPECÍFICOS PARA FAVORITOS ==========

    @Transactional(readOnly = true)
    fun getFavoritesPlaylist(userId: UUID): PlaylistWithMusicsResponse {
        // Buscar playlist de favoritos do usuário
        val favoritesPlaylist = playlistRepository.findByUserIdAndIsDefault(userId, true)
            .firstOrNull { it.name == "Favoritos" }
            ?: throw NoSuchElementException("Playlist de favoritos não encontrada para o usuário")

        return getPlaylistWithMusics(favoritesPlaylist.id!!, userId)
    }

    @Transactional
    fun addToFavorites(userId: UUID, musicId: String) {
        // Buscar playlist de favoritos do usuário
        val favoritesPlaylist = playlistRepository.findByUserIdAndIsDefault(userId, true)
            .firstOrNull { it.name == "Favoritos" }
            ?: throw NoSuchElementException("Playlist de favoritos não encontrada para o usuário")

        // Usar método existente para adicionar música
        addMusicToPlaylist(favoritesPlaylist.id!!, userId, AddMusicToPlaylistRequest(musicId))
    }

    @Transactional
    fun removeFromFavorites(userId: UUID, musicId: String) {
        // Buscar playlist de favoritos do usuário
        val favoritesPlaylist = playlistRepository.findByUserIdAndIsDefault(userId, true)
            .firstOrNull { it.name == "Favoritos" }
            ?: throw NoSuchElementException("Playlist de favoritos não encontrada para o usuário")

        // Usar método existente para remover música
        removeMusicFromPlaylist(favoritesPlaylist.id!!, musicId, userId)
    }
}
