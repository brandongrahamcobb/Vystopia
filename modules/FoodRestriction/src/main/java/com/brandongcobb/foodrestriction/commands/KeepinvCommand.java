    package com.brandongcobb.foodrestriction.Commands;

import com.brandongcobb.foodrestriction.FoodRestriction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KeepinvCommand implements CommandExecutor {
   private final FoodRestriction plugin;

   public KeepinvCommand(FoodRestriction plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("Only players can use this command.");
         return false;
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("keepinv.toggle")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command.");
            return true;
         } else {
            if (player.hasPermission("essentials.keepinv")) {
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manuaddp " + player.getName() + " -essentials.keepinv");
               player.sendMessage(String.valueOf(ChatColor.RED) + "Keep inventory disabled.");
            } else {
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manudelp " + player.getName() + " -essentials.keepinv");
               player.sendMessage(String.valueOf(ChatColor.GREEN) + "Keep inventory enabled.");
            }

            return true;
         }
      }
   }
}
