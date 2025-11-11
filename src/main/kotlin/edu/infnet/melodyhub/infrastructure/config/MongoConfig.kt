package edu.infnet.melodyhub.infrastructure.config

import com.mongodb.client.gridfs.GridFSBucket
import com.mongodb.client.gridfs.GridFSBuckets
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.gridfs.GridFsTemplate

@Configuration
class MongoConfig(
    private val mongoDbFactory: MongoDatabaseFactory,
    private val mappingMongoConverter: MappingMongoConverter
) {

    @Bean
    fun gridFsTemplate(): GridFsTemplate {
        return GridFsTemplate(mongoDbFactory, mappingMongoConverter)
    }

    @Bean
    fun gridFSBucket(): GridFSBucket {
        val db = mongoDbFactory.mongoDatabase
        return GridFSBuckets.create(db)
    }
}
