package com.brandongcobb.foodrestriction;

import com.brandongcobb.foodrestriction.Events.PlayerInteract;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoodRestriction extends JavaPlugin {
   public static FoodRestriction plugin;
   public List<String> nonVeganList;
   private File dataF;
   public FileConfiguration animalProducts;

   public void onEnable() {
      plugin = this;
      this.createFoods();
      this.nonVeganList = this.animalProducts.getStringList("nonVeganList");
      this.getServer().getPluginManager().registerEvents(new PlayerInteract(), this);
   }

   private void createFoods() {
      File nonVeganFile = new File(this.getDataFolder(), "animal_products.yml");
      if (!nonVeganFile.exists()) {
         nonVeganFile.getParentFile().mkdirs();
         this.saveResource("animal_products.yml", false);
      }

      this.animalProducts = new YamlConfiguration();

      try {
         this.animalProducts.load(nonVeganFile);
      } catch (InvalidConfigurationException | IOException var3) {
         var3.printStackTrace();
      }

   }
}
