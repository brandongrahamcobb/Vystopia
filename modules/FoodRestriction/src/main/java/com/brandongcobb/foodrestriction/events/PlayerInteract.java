package com.brandongcobb.foodrestriction.Events;

import com.brandongcobb.foodrestriction.FoodRestriction;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteract implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only process if the event is using main hand
        if (event.getHand() != EquipmentSlot.OFF_HAND) {
            // Trigger only when right-clicking (air or block)
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                Player player = event.getPlayer();

                // Get item in main hand
                ItemStack item = player.getInventory().getItemInMainHand();

                if (item == null || item.getType() == null) return;

                String itemName = item.getType().toString().toLowerCase(Locale.ROOT);

                // Check against the nonVeganList
                if (FoodRestriction.plugin.nonVeganList.contains(itemName)) {
                    player.sendMessage("You are not allowed to eat this non-vegan food: " + itemName.replace("_", " ") + ".");
                    event.setCancelled(true);
                }
            }
        }
    }
}
