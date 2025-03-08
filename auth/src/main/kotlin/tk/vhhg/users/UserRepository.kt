package tk.vhhg.users

import tk.vhhg.model.TokenPair

interface UserRepository {
    suspend fun register(username: String, password: String): TokenPair?
    suspend fun login(username: String, password: String): TokenPair?
    suspend fun refresh(accessToken: String, refreshToken: String): TokenPair?
}