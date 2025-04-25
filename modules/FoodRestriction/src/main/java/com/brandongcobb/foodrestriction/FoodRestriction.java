package com.brandongcobb.foodrestriction;

import com.brandongcobb.foodrestriction.Commands.KeepinvCommand;
import com.brandongcobb.foodrestriction.Commands.LeaderboardCommand;
import com.brandongcobb.foodrestriction.Events.PlayerInteract;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoodRestriction extends JavaPlugin {
   public static FoodRestriction plugin;
   public List<String> foodList;
   private HikariDataSource dataSource;
   private File dataF;
   public FileConfiguration data;
   public FileConfiguration config;
   public FileConfiguration foods;

   public void onEnable() {
      plugin = this;
      this.createConfig();
      this.createData();
      this.createFoods();
      this.connectDatabase();
      this.foodList = this.foods.getStringList("foods");
      this.getCommand("leaderboard").setExecutor(new LeaderboardCommand(this));
      this.getCommand("togglekeepinv").setExecutor(new KeepinvCommand(this));
      this.getServer().getPluginManager().registerEvents(new PlayerInteract(), this);
   }

   private void connectDatabase() {
      this.getLogger().log(Level.INFO, "Initializing PostgreSQL connection pool...");
      String host = this.getConfig().getString("postgres_host", "jdbc:postgresql://localhost");
      String db = this.getConfig().getString("postgres_db", "lucy");
      String user = this.getConfig().getString("postgres_user", "postgres");
      String password = this.getConfig().getString("postgres_password", "");
      String port = this.getConfig().getString("postgres_port", "5432");
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(String.format("%s:%s/%s", host, port, db));
      config.setUsername(user);
      config.setPassword(password);
      config.setDriverClassName("org.postgresql.Driver");
      this.dataSource = new HikariDataSource(config);
      this.getLogger().log(Level.INFO, "PostgreSQL connection pool initialized.");
   }

   public Connection getConnection() throws SQLException {
      if (this.dataSource == null) {
         throw new SQLException("DataSource not initialized");
      } else {
         return this.dataSource.getConnection();
      }
   }

   public void closeDatabase() {
      if (this.dataSource != null && !this.dataSource.isClosed()) {
         this.dataSource.close();
      }

   }

   private void createData() {
      this.dataF = new File(this.getDataFolder(), "data.yml");
      if (!this.dataF.exists()) {
         this.dataF.getParentFile().mkdirs();
         this.saveResource("data.yml", false);
      }

      this.data = new YamlConfiguration();

      try {
         this.data.load(this.dataF);
      } catch (InvalidConfigurationException | IOException var2) {
         var2.printStackTrace();
      }

   }

   public void saveData() {
      try {
         this.data.save(this.dataF);
      } catch (IOException var2) {
         var2.printStackTrace();
      }

   }

   private void createConfig() {
      File configf = new File(this.getDataFolder(), "config.yml");
      if (!configf.exists()) {
         configf.getParentFile().mkdirs();
         this.saveResource("config.yml", false);
      }

      this.config = new YamlConfiguration();

      try {
         this.config.load(configf);
      } catch (InvalidConfigurationException | IOException var3) {
         var3.printStackTrace();
      }

   }

   private void createFoods() {
      File foodsF = new File(this.getDataFolder(), "all_foods.yml");
      if (!foodsF.exists()) {
         foodsF.getParentFile().mkdirs();
         this.saveResource("all_foods.yml", false);
      }

      this.foods = new YamlConfiguration();

      try {
         this.foods.load(foodsF);
      } catch (InvalidConfigurationException | IOException var3) {
         var3.printStackTrace();
      }

   }

   public void onDisable() {
      this.closeDatabase();
      this.getLogger().log(Level.INFO, "PostgreSQL Example plugin disabled.");
   }
}
