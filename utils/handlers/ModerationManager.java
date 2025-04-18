package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.channel.PrivateChannel;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.locks.Lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public class ModerationManager {

    private DiscordBot bot;
    private AIManager aiManager;
    private MessageManager messageManager;
    private final HikariDataSource dbPool;
    private Lock lock;
    private Predicator predicator;
    private final Config config;
//    privat static String oauthtoken;
    private Logger logger;
    private Helpers helpers;

    public ModerationManager(DiscordBot bot) {
        this.bot = bot;
        this.config = bot.config;
        this.dbPool = bot.dbPool;
        this.logger = bot.logger;
    }

    public Server getServerById(long serverId) {
        Set<Server> servers = bot.getApi().getServers(); // Get all servers
        for (Server server : servers) {
            if (server.getId() == serverId) {
                return server; // Return the server wrapped in an Optional
            }
        }
        return null;
    }

    public void handleModeration(Message message, String reasonStr) {
        Server server = message.getServer().orElse(null);
        User author = message.getAuthor().asUser().orElse(null);

        if (server == null || author == null) {
            return; // Invalid message context
        }

        // Check if the user has the unfiltered role
        if (author.getRoles(server).stream().anyMatch(role -> role.getName().equals(config.getConfigValue("discord_role_pass")))) {
            return; // User is exempt from moderation
        }

        long userId = author.getId();

        try (Connection connection = dbPool.getConnection()) {
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT flagged_count FROM moderation_counts WHERE user_id = ?");
            statement.setLong(1, userId);
            ResultSet row = statement.executeQuery();

            int flaggedCount;

            if (row.next()) {
                flaggedCount = row.getInt("flagged_count") + 1;
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE moderation_counts SET flagged_count = ? WHERE user_id = ?");
                updateStatement.setInt(1, flaggedCount);
                updateStatement.setLong(2, userId);
                updateStatement.executeUpdate();
                logger.info("Updated flagged count for user " + userId + ": " + flaggedCount);
            } else {
                flaggedCount = 1;
                PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO moderation_counts (user_id, flagged_count) VALUES (?, ?)");
                insertStatement.setLong(1, userId);
                insertStatement.setInt(2, flaggedCount);
                insertStatement.executeUpdate();
                logger.info("Inserted new flagged count for user " + userId + ": " + flaggedCount);
            }

            connection.commit(); // Commit transaction

            // Delete the message
            message.delete();

            // Send moderation warnings/messages
            String moderationWarning = (String) config.getConfigValue("discord_moderation_warning");
            if (flaggedCount == 1) {
                messageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
            } else if (flaggedCount >= 2 && flaggedCount <= 4) {
                if (flaggedCount == 4) {
                    messageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
                }
            } else if (flaggedCount >= 5) {
                messageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
                messageManager.sendDiscordMessage(message, "You have been timed out for 5 minutes due to repeated violations.");
                
                author.timeout(server, Duration.ofSeconds(300), reasonStr); // Timeout for 5 minutes (300 seconds)

                // Reset the flagged count
                PreparedStatement resetStatement = connection.prepareStatement(
                        "UPDATE moderation_counts SET flagged_count = 0 WHERE user_id = ?");
                resetStatement.setLong(1, userId);
                resetStatement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.severe("Error processing moderation for user " + userId + ": " + e.getMessage());
        }
    }

}
