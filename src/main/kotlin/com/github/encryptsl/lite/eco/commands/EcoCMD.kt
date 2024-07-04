package com.github.encryptsl.lite.eco.commands

import com.github.encryptsl.lite.eco.LiteEco
import com.github.encryptsl.lite.eco.api.ComponentPaginator
import com.github.encryptsl.lite.eco.api.enums.CheckLevel
import com.github.encryptsl.lite.eco.api.enums.PurgeKey
import com.github.encryptsl.lite.eco.api.events.admin.*
import com.github.encryptsl.lite.eco.api.objects.ModernText
import com.github.encryptsl.lite.eco.common.config.Locales
import com.github.encryptsl.lite.eco.common.extensions.convertInstant
import com.github.encryptsl.lite.eco.common.extensions.getRandomString
import com.github.encryptsl.lite.eco.common.extensions.positionIndexed
import com.github.encryptsl.lite.eco.utils.ConvertEconomy
import com.github.encryptsl.lite.eco.utils.ConvertEconomy.Economies
import com.github.encryptsl.lite.eco.utils.Helper
import com.github.encryptsl.lite.eco.utils.MigrationTool
import com.github.encryptsl.lite.eco.utils.MigrationTool.MigrationKey
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotation.specifier.Range
import org.incendo.cloud.annotations.*
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.system.measureTimeMillis

@Suppress("UNUSED")
@CommandDescription("Provided plugin by LiteEco")
class EcoCMD(private val liteEco: LiteEco) {

    private val helper: Helper = Helper(liteEco)
    private val convertEconomy: ConvertEconomy = ConvertEconomy(liteEco)

    @Command("eco help")
    @Permission("lite.eco.admin.help")
    fun adminHelp(commandSender: CommandSender) {
        liteEco.locale.getList("messages.admin-help")?.forEach { s ->
            commandSender.sendMessage(ModernText.miniModernText(s.toString()))
        }
    }

    @Command("eco add <player> <amount> [currency]")
    @Permission("lite.eco.admin.add")
    fun onAddMoney(
        commandSender: CommandSender,
        @Argument(value = "player", suggestions = "players") offlinePlayer: OfflinePlayer,
        @Argument(value = "amount") @Range(min = "1.00", max = "") amountStr: String,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String,
        @Flag(value = "silent", aliases = ["s"]) silent: Boolean
    ) {
        val amount = helper.validateAmount(amountStr, commandSender) ?: return
        liteEco.pluginManager.callEvent(EconomyMoneyDepositEvent(commandSender, offlinePlayer, currency, amount, silent))
    }

    @Command("eco global add <amount> [currency]")
    @Permission("lite.eco.admin.gadd", "lite.eco.admin.global.add")
    fun onGlobalAddMoney(
        commandSender: CommandSender,
        @Argument("amount") @Range(min = "1.0", max = "") amountStr: String,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String
    ) {
        val amount = helper.validateAmount(amountStr, commandSender) ?: return
        liteEco.pluginManager.callEvent(EconomyGlobalDepositEvent(commandSender, currency, amount))
    }

    @Command("eco set <player> [amount] [currency]")
    @Permission("lite.eco.admin.set")
    fun onSetBalance(
        commandSender: CommandSender,
        @Argument(value = "player", suggestions = "players") offlinePlayer: OfflinePlayer,
        @Default("0.00") @Argument(value = "amount") amountStr: String,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String
    ) {
        val amount = helper.validateAmount(amountStr, commandSender, CheckLevel.ONLY_NEGATIVE) ?: return
        liteEco.pluginManager.callEvent(EconomyMoneySetEvent(commandSender, offlinePlayer, currency, amount))
    }

    @Command("eco global set <amount> [currency]")
    @Permission("lite.eco.admin.gset", "lite.eco.admin.global.withdraw")
    fun onGlobalSetMoney(
        commandSender: CommandSender,
        @Argument("amount") @Range(min = "1.0", max = "") amountStr: String,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String,
    ) {
        val amount = helper.validateAmount(amountStr, commandSender, CheckLevel.ONLY_NEGATIVE) ?: return
        liteEco.pluginManager.callEvent(EconomyGlobalSetEvent(commandSender, currency, amount))
    }

    @Command("eco withdraw <player> <amount> [currency]")
    @Permission("lite.eco.admin.withdraw", "lite.eco.admin.remove")
    fun onWithdrawMoney(
        commandSender: CommandSender,
        @Argument(value = "player", suggestions = "players") offlinePlayer: OfflinePlayer,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String,
        @Argument(value = "amount") @Range(min = "1.00", max = "") amountStr: String,
        @Flag(value = "silent", aliases = ["s"]) silent: Boolean
    ) {
        val amount = helper.validateAmount(amountStr, commandSender) ?: return
        liteEco.pluginManager.callEvent(EconomyMoneyWithdrawEvent(commandSender, offlinePlayer, currency, amount, silent))
    }

    @Command("eco global withdraw <amount> [currency]")
    @Permission("lite.eco.admin.gremove", "lite.eco.admin.global.withdraw")
    fun onGlobalWithdrawMoney(
        commandSender: CommandSender,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String,
        @Argument("amount") @Range(min = "1.0", max = "") amountStr: String
    ) {
        val amount = helper.validateAmount(amountStr, commandSender) ?: return

        liteEco.pluginManager.callEvent(
            EconomyGlobalWithdrawEvent(commandSender, currency, amount)
        )
    }

    @Command("eco create <player> [amount] [currency]")
    @Permission("lite.eco.admin.create")
    fun onCreateWalletAccount(
        commandSender: CommandSender,
        @Argument("player", suggestions = "players") offlinePlayer: OfflinePlayer,
        @Argument("amount") @Default("30.00") amount: Double,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String
    ) {
        val message = if (liteEco.api.createAccount(offlinePlayer, currency, amount.toBigDecimal())) {
            "messages.admin.create_account"
        } else {
            "messages.error.account_now_exist"
        }
        commandSender.sendMessage(liteEco.locale.translation(message, Placeholder.parsed("account", offlinePlayer.name.toString())))
    }

    @Command("eco delete <player>")
    @Permission("lite.eco.admin.delete")
    fun onDeleteWalletAccount(
        commandSender: CommandSender,
        @Argument("player", suggestions = "players") offlinePlayer: OfflinePlayer
    ) {
        val message = if (liteEco.api.deleteAccount(offlinePlayer)) {
            "messages.admin.delete_account"
        } else {
            "messages.error.account_not_exist"
        }
        commandSender.sendMessage(liteEco.locale.translation(message, Placeholder.parsed("account", offlinePlayer.name.toString())))
    }

    @Command("eco monolog [page] [player]")
    @Permission("lite.eco.admin.monolog")
    fun onLogView(commandSender: CommandSender, @Argument("page") @Default(value = "1") page: Int, @Argument("player") player: String?) {

        val log = helper.validateLog(player).map {
            liteEco.locale.translation("messages.admin.monolog_format", TagResolver.resolver(
                Placeholder.parsed("level", it.level),
                Placeholder.parsed("timestamp", convertInstant(it.timestamp)),
                Placeholder.parsed("log", it.log)
            ))
        }
        val pagination = ComponentPaginator(log) { itemsPerPage = 10 }.apply { page(page) }

        if (pagination.isAboveMaxPage(page))
            return commandSender.sendMessage(liteEco.locale.translation("messages.error.maximum_page",
                Placeholder.parsed("max_page", pagination.maxPages.toString()))
            )

        for (content in pagination.display()) {
            commandSender.sendMessage(content)
        }
    }

    @Command("eco lang <isoKey>")
    @Permission("lite.eco.admin.lang")
    fun onLangSwitch(
        commandSender: CommandSender,
        @Argument(value = "isoKey", suggestions = "langKeys") isoKey: Locales.LangKey
    ) {
        try {
            val langKey = Locales.LangKey.valueOf(isoKey.name)
            liteEco.locale.setLocale(langKey)
            commandSender.sendMessage(
                liteEco.locale.translation("messages.admin.translation_switch", Placeholder.parsed("locale", langKey.name))
            )
        } catch (_: IllegalArgumentException) {
            commandSender.sendMessage(
                ModernText.miniModernText(
                    "That translation doesn't exist."
                )
            )
        }
    }

    @Command("eco purge <argument> [currency]")
    @Permission("lite.eco.admin.purge")
    fun onPurge(
        commandSender: CommandSender,
        @Argument(value = "argument", suggestions = "purgeKeys") purgeKey: PurgeKey,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String
        ) {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        when (purgeKey) {
            PurgeKey.ACCOUNTS -> {
                liteEco.databaseEcoModel.purgeAccounts(currency)
                commandSender.sendMessage(liteEco.locale.translation("messages.admin.purge_accounts"))
            }
            PurgeKey.NULL_ACCOUNTS -> {
                liteEco.databaseEcoModel.purgeInvalidAccounts(currency)
                commandSender.sendMessage(liteEco.locale.translation("messages.admin.purge_null_accounts"))
            }
            PurgeKey.DEFAULT_ACCOUNTS -> {
                liteEco.databaseEcoModel.purgeDefaultAccounts(liteEco.currencyImpl.getCurrencyStartBalance(currency), currency)
                commandSender.sendMessage(liteEco.locale.translation("messages.admin.purge_default_accounts"))
            }
            PurgeKey.MONO_LOG -> {
                liteEco.loggerModel.getLog().thenApply { el ->
                    if (el.isEmpty()) {
                        throw Exception("You can't remove monolog because is empty.")
                    }
                    return@thenApply el
                }.thenApply { el ->
                    liteEco.loggerModel.clearLogs()
                    commandSender.sendMessage(liteEco.locale.translation("messages.admin.purge_monolog_success", Placeholder.parsed("deleted", el.size.toString())))
                }.exceptionally { el ->
                    commandSender.sendMessage(liteEco.locale.translation("messages.error.purge_monolog_fail"))
                    liteEco.logger.severe(el.message ?: el.localizedMessage)
                }
            }
            else -> {
                commandSender.sendMessage(liteEco.locale.translation("messages.error.purge_argument"))
            }
        }
    }

    @Command("eco migration <argument>")
    @Permission("lite.eco.admin.migration")
    fun onMigration(commandSender: CommandSender, @Argument(value = "argument", suggestions = "migrationKeys") migrationKey: MigrationKey) {
        val migrationTool = MigrationTool(liteEco)
        val output = liteEco.api.getTopBalance().toList().positionIndexed { index, k -> MigrationTool.MigrationData(index, Bukkit.getOfflinePlayer(k.first).name.toString(), k.first, k.second) }

        val result = when(migrationKey) {
            MigrationKey.CSV -> migrationTool.migrateToCSV(output, "economy_migration")
            MigrationKey.SQL -> migrationTool.migrateToSQL(output, "economy_migration")
        }

        val messageKey = if (result) {
            "messages.admin.migration_success"
        } else {
            "messages.error.migration_failed"
        }

        commandSender.sendMessage(
            liteEco.locale.translation(messageKey,
            TagResolver.resolver(
                Placeholder.parsed("type", migrationKey.name)
            )
        ))
    }

    @Command("eco convert <economy> [currency]")
    @Permission("lite.eco.admin.convert")
    fun onEconomyConvert(
        commandSender: CommandSender,
        @Argument("economy", suggestions = "economies") economy: Economies,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String
    ) {
        try {
            when (economy) {
                Economies.EssentialsX -> {
                    convertEconomy.convertEssentialsXEconomy(currency)
                }

                Economies.BetterEconomy -> {
                    convertEconomy.convertBetterEconomy(currency)
                }
            }
            val (converted, balances) = convertEconomy.getResult()
            commandSender.sendMessage(
                liteEco.locale.translation("messages.admin.convert_success",
                TagResolver.resolver(
                    Placeholder.parsed("economy", economy.name),
                    Placeholder.parsed("converted", converted.toString()),
                    Placeholder.parsed("balances", balances.toString())
                )
            ))
            convertEconomy.convertRefresh()
        } catch (e : Exception) {
            liteEco.logger.info(e.message ?: e.localizedMessage)
            commandSender.sendMessage(liteEco.locale.translation("messages.error.convert_fail"))
        }
    }

    @Command("eco debug create accounts <amount> [currency]")
    @Permission("lite.eco.admin.debug.create.accounts")
    fun onDebugCreateAccounts(
        commandSender: CommandSender,
        @Argument("amount") @Range(min = "1", max = "100") amountStr: Int,
        @Default("dollar") @Argument("currency", suggestions = "currencies") currency: String
    ) {
        val random = ThreadLocalRandom.current()

        val time = measureTimeMillis {
            for (i in 1 .. amountStr) {
                liteEco.databaseEcoModel.createPlayerAccount(getRandomString(10), UUID.randomUUID(), currency, BigDecimal.valueOf(random.nextDouble(1000.0, 500000.0)))
            }
        }

        commandSender.sendMessage("Into database was insterted $amountStr fake accounts in time $time ms")
    }

    @Command("eco reload")
    @Permission("lite.eco.admin.reload")
    fun onReload(commandSender: CommandSender) {
        liteEco.reloadConfig()
        commandSender.sendMessage(liteEco.locale.translation("messages.admin.config_reload"))
        liteEco.logger.info("Config.yml was reloaded [!]")
        liteEco.saveConfig()
        liteEco.locale.loadCurrentTranslation()
    }
}