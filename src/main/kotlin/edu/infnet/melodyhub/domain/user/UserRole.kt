package edu.infnet.melodyhub.domain.user

enum class UserRole(val description: String) {
    ADMIN("Administrador do sistema"),
    PREMIUM("Usuário com plano premium"),
    BASIC("Usuário com plano básico"),
    SEM_PLANO("Usuário sem plano ativo")
}
