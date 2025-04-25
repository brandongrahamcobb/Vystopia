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
   public void onPlayerJoin(PlayerJoinEvent event) {
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getHand() != EquipmentSlot.OFF_HAND) {
         if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            String itemName = item.getType().toString().toLowerCase(Locale.ROOT);
            if (FoodRestriction.plugin.foodList.contains(itemName)) {
               if (!player.hasPermission("foodrestriction.eat." + itemName)) {
                  player.sendMessage("You are not allowed to eat this food: " + itemName.replace("_", " ") + ".");
                  event.setCancelled(true);
               }

            }
         }
      }
   }
}
