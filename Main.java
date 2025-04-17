package com.brandongcobb.vyrtuous;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
//import com.brandongcobb.vyrtuous.Bots.LinkedInBot;
//import com.brandongcobb.vyrtuous.Bots.TwitchBot;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import com.brandongcobb.vyrtuous.utils.security.DiscordOAuth;
//import com.brandongcobb.vyrtuous.Security.LinkedInOAuth;
//import com.brandongcobb.vyrtuous.Security.PatreonOAuth;
//import com.brandongcobb.vyrtuous.Security.TwitchOAuth;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class Main {

    public static DiscordBot bot;
    public static Config config;
    private static DiscordOAuth discordOAuth;
    public static HikariDataSource dbPool;
    public static AIManager aiManager = new AIManager(config);
    private static Helpers helpers;
    public static MessageManager messageManager = new MessageManager(config, dbPool, helpers);
    public static Predicator predicator = new Predicator(bot);
    public static Lock lock;
    public static final String oauthToken;
    public static Map<String, List<Map<String, String>>> conversations;
    public static final Logger logger = Logger.getLogger("Vyrtuous");

    public static void main(String[] args) {
        try {
            config = new Config();
            helpers = new Helpers();
            setupLogging(config);
            initializeDatabase();

//            DiscordOAuth discordOAuth = new DiscordOAuth(config);
//            LinkedInOAuth linkedInOAuth = new LinkedInOAuth(config);
//            PatreonOAuth patreonOAuth = new PatreonOAuth(config);
//            TwitchOAuth twitchOAuth = new TwitchOAuth(config);

            // Print authorization URLs
//            System.out.println("Please authenticate Discord by visiting the following URL:");
//            System.out.println(discordOAuth.getAuthorizationUrl());
//            System.out.println("Please authenticate LinkedIn by visiting the following URL:");
//            System.out.println(linkedInOAuth.getAuthorizationUrl());
//            System.out.println("Please authenticate Patreon by visiting the following URL:");
//            System.out.println(patreonOAuth.getAuthorizationUrl());
//            System.out.println("Please authenticate Twitch by visiting the following URL:");
//            System.out.println(twitchOAuth.getAuthorizationUrl());

            // Create bot instances
            Map<String, Object> apiKeysConfig = (Map<String, Object>) config.getConfigValue("api_keys"); // Retrieve "api_keys"
            Map<String, Object> discordApiKeys = (Map<String, Object>) apiKeysConfig.get("Discord"); // Retrieve "Discord"
            DiscordBot discordBot = new DiscordBot(logger, config, dbPool, aiManager, lock, messageManager, predicator, (String) discordApiKeys.get("api_key"));
//            LinkedInBot linkedInBot = new LinkedInBot(config, linkedInOAuth.getAccessToken(), dataSource);
//            TwitchBot twitchBot = new TwitchBot(config, twitchOAuth.getAccessToken(), dataSource);

            // Start bots asynchronously
            CompletableFuture<Void> discordTask = CompletableFuture.runAsync(() -> discordBot.start());
//            CompletableFuture<Void> linkedInTask = CompletableFuture.runAsync(() -> linkedInBot.start());
//            CompletableFuture<Void> twitchTask = CompletableFuture.runAsync(() -> twitchBot.start());

            CompletableFuture<Void> allTasks = CompletableFuture.allOf(discordTask);//, linkedInTask, twitchTask);
            allTasks.join(); // Wait for all to finish

        } catch (Exception e) {
            logger.severe("Error initializing the application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void setupLogging(Config config) {
        // Implement your logging setup based on the configuration
    }

    private static void initializeDatabase() {
        HikariConfig sqlConfig = new HikariConfig();
        sqlConfig.setJdbcUrl("jdbc:postgresql://localhost:5432/lucy");
        sqlConfig.setUsername("postgres");
        sqlConfig.setPassword("");
        sqlConfig.setDriverClassName("org.postgresql.Driver");

        HikariDataSource dataSource = new HikariDataSource(sqlConfig);

        // Test the connection
        try (Connection connection = dataSource.getConnection()) {
            logger.info("Database connection established.");
        } catch (SQLException e) {
            logger.severe("Failed to connect to the database: " + e.getMessage());
        }
    }

    public static HikariDataSource getDataSource() {
        return dbPool;
    }
}
