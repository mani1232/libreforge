package com.willfp.libreforge

import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.events.ArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("unused", "UNUSED_PARAMETER")
class ItemRefreshListener(
    private val plugin: EcoPlugin
) : Listener {
    private val inventoryClickTimeouts = Caffeine.newBuilder()
        .expireAfterWrite(
            plugin.configYml.getInt("refresh.inventory-click.timeout").toLong(),
            TimeUnit.MILLISECONDS
        )
        .build<UUID, Unit>()

    @EventHandler
    fun onItemPickup(event: EntityPickupItemEvent) {
        plugin.scheduler.runNow({
            event.entity.toDispatcher().refreshHolders()
        }, event.entity.location)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getServer().onlinePlayers.forEach {
            plugin.scheduler.runNow({
                it.toDispatcher().refreshHolders()
            }, it.location)
        }
    }

    @EventHandler
    fun onInventoryDrop(event: PlayerDropItemEvent) {
        plugin.scheduler.runNow({
            event.player.toDispatcher().refreshHolders()
        }, event.player.location)
    }

    @EventHandler
    fun onChangeSlot(event: PlayerItemHeldEvent) {
        val dispatcher = event.player.toDispatcher()
        plugin.scheduler.runNow({
            dispatcher.refreshHolders()
        }, event.player.location)
        plugin.scheduler.run({
            dispatcher.refreshHolders()
        }, event.player.location)
    }

    @EventHandler
    fun onArmorChange(event: ArmorChangeEvent) {
        plugin.scheduler.runNow({
            event.player.toDispatcher().refreshHolders()
        }, event.player.location)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        if (inventoryClickTimeouts.getIfPresent(player.uniqueId) != null) {
            return
        }

        inventoryClickTimeouts.put(player.uniqueId, Unit)

        plugin.scheduler.runNow({
            player.toDispatcher().refreshHolders()
        }, player.location)
    }
}
