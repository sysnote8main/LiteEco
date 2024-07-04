package com.github.encryptsl.lite.eco.api.interfaces

import com.github.encryptsl.lite.eco.common.database.entity.User
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.CompletableFuture

interface PlayerSQL {
    fun createPlayerAccount(username: String, uuid: UUID, currency: String, money: BigDecimal)
    fun getUserByUUID(uuid: UUID, currency: String): CompletableFuture<User>
    fun deletePlayerAccount(uuid: UUID, currency: String)
    fun getExistPlayerAccount(uuid: UUID, currency: String): CompletableFuture<Boolean>
    fun getTopBalance(currency: String): MutableMap<String, BigDecimal>
    fun getPlayersIds(currency: String): CompletableFuture<MutableCollection<UUID>>
    fun depositMoney(uuid: UUID, currency: String, money: BigDecimal)
    fun withdrawMoney(uuid: UUID, currency: String, money: BigDecimal)
    fun setMoney(uuid: UUID, currency: String, money: BigDecimal)
    fun purgeAccounts(currency: String)
    fun purgeDefaultAccounts(defaultMoney: BigDecimal, currency: String)
    fun purgeInvalidAccounts(currency: String)
}