package com.brandongcobb.vyrtuous.bots;

import com.brandongcobb.vyrtuous.cogs.EventListeners;
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
    public final Config config;
    public static Map<String, List<Map<String, String>>> completions;
    public final HikariDataSource dbPool;
    //private final String oauthToken;
    public static final Helpers helpers;
    public Lock lock;
    public static final Logger logger = Logger.getLogger("Vyrtuous");
    private static final String apiKey;
    public static MessageManager messageManager;
    public static Predicator predicator;
    private static DiscordApi api;

    public DiscordBot(Logger logger, Config config, HikariDataSource dbPool, AIManager aiManager, Lock lock, MessageManager messageManager, Predicator predicator, String apiKey) {//, String oauthToken) {
        this.config = config;
        this.aiManager = aiManager;
        this.messageManager = messageManager;
        this.dbPool = dbPool;
        this.completions = completions;
        this.lock = lock;
        this.predicator = predicator;
//        this.oauthToken = oauthToken;

        // Initialize the bot
        initializeBot(apiKey);
    }

    private void initializeBot(String apiKey) {
        this.api = new DiscordApiBuilder().setToken(apiKey).login().join();

        // Register the message listener
        this.api.addMessageCreateListener(this);

        // Load and register cogs
        loadCogs();
    }

    private void loadCogs() {
        List<Cog> cogs = new ArrayList<>();
//        cogs.add(new HybridCommands()); // Add your cogs here
//        cogs.add(new AdministratorCommands()); // Add your cogs here
        cogs.add(new EventListeners()); // Add your cogs here
//        cogs.add(new ScheduledTasks()); // Add your cogs here

        for (Cog cog : cogs) {
            cog.register(this.api);
        }
        // Load your cogs (modules) here if needed
    }

    public DiscordApi getApi() {
        return this.api;
    }
}
