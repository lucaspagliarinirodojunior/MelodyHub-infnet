package edu.infnet.melodyhub

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@Disabled("Requires PostgreSQL and MongoDB running - use for integration tests only")
class MelodyHubApplicationTests {

    @Test
    fun contextLoads() {
    }

}
