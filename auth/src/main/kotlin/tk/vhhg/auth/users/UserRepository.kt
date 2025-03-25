package tk.vhhg.auth.users

import tk.vhhg.auth.model.TokenPair

interface UserRepository {
    suspend fun register(username: String, password: String): TokenPair?
    suspend fun login(username: String, password: String): TokenPair?
    suspend fun refresh(accessToken: String, refreshToken: String): TokenPair?
}