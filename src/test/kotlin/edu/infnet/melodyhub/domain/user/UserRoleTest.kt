package edu.infnet.melodyhub.domain.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UserRoleTest {

    @Test
    fun `should have correct descriptions for all roles`() {
        assertEquals("Administrador do sistema", UserRole.ADMIN.description)
        assertEquals("Usuário com plano premium", UserRole.PREMIUM.description)
        assertEquals("Usuário com plano básico", UserRole.BASIC.description)
        assertEquals("Usuário sem plano ativo", UserRole.SEM_PLANO.description)
    }

    @Test
    fun `should contain all expected roles`() {
        val roles = UserRole.values()
        assertEquals(4, roles.size)
        assertTrue(roles.contains(UserRole.ADMIN))
        assertTrue(roles.contains(UserRole.PREMIUM))
        assertTrue(roles.contains(UserRole.BASIC))
        assertTrue(roles.contains(UserRole.SEM_PLANO))
    }

    @Test
    fun `should convert string to UserRole`() {
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"))
        assertEquals(UserRole.PREMIUM, UserRole.valueOf("PREMIUM"))
        assertEquals(UserRole.BASIC, UserRole.valueOf("BASIC"))
        assertEquals(UserRole.SEM_PLANO, UserRole.valueOf("SEM_PLANO"))
    }
}
