package com.brandongcobb.vyrtuous.bots;

import com.brandongcobb.vyrtuous.cogs.Cog;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import com.zaxxer.hikari.HikariDataSource;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import java.util.logging.Logger;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class DiscordBot implements MessageCreateListener {

    public static AIManager aiManager;
    public static final Config config;
    public static Map<String, List<Map<String, String>>> conversations;
    public static final HikariDataSource dbPool;
    //private final String oauthToken;
    public static final Helpers helpers;
    public static final Lock lock;
    public static final Logger logger = Logger.getLogger("Vyrtuous");
    public static Predicator predicator;

    public DiscordBot(Logger logger, Config config, HikariDataSource dbPool, AIManager aiManager, Lock lock, MessageManager messageManager, Predicator predicator) {//, String oauthToken) {
        this.config = config.getConfig();
        this.aiManager = aiManager;
        this.messageManager = messageManager;
        this.dbPool = dbPool;
        this.completions = completions;
        this.lock = lock;
        this.predicator = predicator;
//        this.oauthToken = oauthToken;

        // Initialize the bot
        initializeBot();
    }

    private void initializeBot() {
        String token = config.get("api_keys").get("Discord").get("api_key").toString(); // Adjust to your config structure
        DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

        // Register the message listener
        api.addMessageCreateListener(this);

        // Load and register cogs
    }

    private void loadCogs(DiscordApi api) {
        List<Cog> cogs = new ArrayList<>();
//        cogs.add(new HybridCommands()); // Add your cogs here
//        cogs.add(new AdministratorCommands()); // Add your cogs here
        cogs.add(new EventListeners()); // Add your cogs here
//        cogs.add(new ScheduledTasks()); // Add your cogs here

        for (Cog cog : cogs) {
            cog.register(api);
        }
        // Load your cogs (modules) here if needed
    }
}
