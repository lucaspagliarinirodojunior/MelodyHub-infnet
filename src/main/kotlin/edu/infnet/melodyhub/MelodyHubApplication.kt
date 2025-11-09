package edu.infnet.melodyhub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MelodyHubApplication

fun main(args: Array<String>) {
    runApplication<MelodyHubApplication>(*args)
}
