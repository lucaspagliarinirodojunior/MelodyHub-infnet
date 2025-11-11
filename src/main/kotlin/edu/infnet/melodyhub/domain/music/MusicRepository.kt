package edu.infnet.melodyhub.domain.music

import org.springframework.data.mongodb.repository.MongoRepository

interface MusicRepository : MongoRepository<Music, String>
