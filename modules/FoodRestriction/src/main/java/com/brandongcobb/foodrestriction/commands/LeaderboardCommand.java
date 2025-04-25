    package com.brandongcobb.foodrestriction.Commands;

import com.brandongcobb.foodrestriction.FoodRestriction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaderboardCommand implements CommandExecutor {
   private final FoodRestriction plugin;

   public LeaderboardCommand(FoodRestriction plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("Only players can use this command.");
         return false;
      } else {
         Player player = (Player)sender;
         player.sendMessage("Fetching leaderboard...");
         this.fetchLeaderboard(player);
         return true;
      }
   }

   private void fetchLeaderboard(Player player) {
      String query = "SELECT id, name, level, exp FROM users ORDER BY level DESC, exp DESC LIMIT 10";

      try {
         Connection connection = this.plugin.getConnection();

         try {
            PreparedStatement statement = connection.prepareStatement(query);

            try {
               ResultSet resultSet = statement.executeQuery();

               try {
                  StringBuilder leaderboardMessage = new StringBuilder("=== Leaderboard ===\n");
                  int rank = 1;

                  while(true) {
                     if (!resultSet.next()) {
                        player.sendMessage(leaderboardMessage.toString());
                        break;
                     }

                     double id = resultSet.getDouble("id");
                     String name = resultSet.getString("name");
                     int level = resultSet.getInt("level");
                     int exp = resultSet.getInt("exp");
                     leaderboardMessage.append(rank).append(". ").append(name).append(" - Level: ").append(level).append(", EXP: ").append(exp).append("\n");
                     ++rank;
                  }
               } catch (Throwable var16) {
                  if (resultSet != null) {
                     try {
                        resultSet.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (resultSet != null) {
                  resultSet.close();
               }
            } catch (Throwable var17) {
               if (statement != null) {
                  try {
                     statement.close();
                  } catch (Throwable var14) {
                     var17.addSuppressed(var14);
                  }
               }

               throw var17;
            }

            if (statement != null) {
               statement.close();
            }
         } catch (Throwable var18) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var13) {
                  var18.addSuppressed(var13);
               }
            }

            throw var18;
         }

         if (connection != null) {
            connection.close();
         }
      } catch (SQLException var19) {
         player.sendMessage("Error fetching leaderboard: " + var19.getMessage());
      }

   }
}
