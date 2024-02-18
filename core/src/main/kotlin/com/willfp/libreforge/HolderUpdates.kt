package com.willfp.libreforge

import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.events.ArmorChangeEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
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

    @EventHandler(priority = EventPriority.LOWEST)
    fun onItemPickup(event: EntityPickupItemEvent) {
        Bukkit.getServer().regionScheduler.execute(plugin, event.entity.location) {
            if (plugin.configYml.getBool("refresh.pickup.require-meta")) {
                if (!event.item.itemStack.hasItemMeta()) {
                    return@execute
                }
            }

            event.entity.toDispatcher().refreshHolders()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getServer().regionScheduler.execute(plugin, event.player.location) {
            Bukkit.getServer().onlinePlayers.forEach {
                it.toDispatcher().refreshHolders()
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrop(event: PlayerDropItemEvent) {
        Bukkit.getServer().regionScheduler.execute(plugin, event.player.location) {
            event.player.toDispatcher().refreshHolders()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChangeSlot(event: PlayerItemHeldEvent) {
        Bukkit.getServer().regionScheduler.execute(plugin, event.player.location) {
            val player = event.player

            if (plugin.configYml.getBool("refresh.held.require-meta")) {
                val oldItem = player.inventory.getItem(event.previousSlot)
                val newItem = player.inventory.getItem(event.newSlot)
                if (((oldItem == null) || !oldItem.hasItemMeta()) && ((newItem == null) || !newItem.hasItemMeta())) {
                    return@execute
                }
            }

            val dispatcher = player.toDispatcher()

            Bukkit.getServer().regionScheduler.execute(plugin, player.location) {
                dispatcher.refreshHolders()
            }
        }
    }

    @EventHandler
    fun onArmorChange(event: ArmorChangeEvent) {
        Bukkit.getServer().regionScheduler.execute(plugin, event.player.location) {
            event.player.toDispatcher().refreshHolders()
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        Bukkit.getServer().regionScheduler.execute(plugin, player.location) {
            if (inventoryClickTimeouts.getIfPresent(player.uniqueId) != null) {
                return@execute
            }

            inventoryClickTimeouts.put(player.uniqueId, Unit)

            player.toDispatcher().refreshHolders()
        }
    }
}
