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
import org.javacord.api.entity.intent.Intent;

import java.util.logging.Logger;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class DiscordBot implements MessageCreateListener {

    public static AIManager aiManager;
    public Config config;
    public static Map<String, List<Map<String, String>>> completions;
    public HikariDataSource dbPool;
    public Helpers helpers;
    public Lock lock;
    public static final Logger logger = Logger.getLogger("Vyrtuous");
    private String apiKey;
    public static MessageManager messageManager;
    public static Predicator predicator;
    private static DiscordApi api;

    public DiscordBot(Helpers helpers, Logger logger, Config config, HikariDataSource dbPool, AIManager aiManager, Lock lock, MessageManager messageManager, Predicator predicator, String apiKey) {
        this.config = config;
        this.apiKey = apiKey;
        this.aiManager = aiManager;
        this.helpers = helpers;
        this.messageManager = messageManager;
        this.dbPool = dbPool;
        this.completions = completions;
        this.lock = lock;
        this.predicator = predicator;

        // Initialize the bot
        initializeBot(apiKey);
    }

    private void initializeBot(String apiKey) {
        this.api = new DiscordApiBuilder().setToken(apiKey).addIntents(Intent.MESSAGE_CONTENT).login().join();

        this.api.addMessageCreateListener(this);

        loadCogs();
    }

    private void loadCogs() {
        List<Cog> cogs = new ArrayList<>();
//        cogs.add(new HybridCommands()); // Add your cogs here
//        cogs.add(new AdministratorCommands()); // Add your cogs here
        cogs.add(new EventListeners(this)); // Add your cogs here
//        cogs.add(new ScheduledTasks()); // Add your cogs here

        for (Cog cog : cogs) {
            cog.register(this.api);
        }
        // Load your cogs (modules) here if needed
    }

    public DiscordApi getApi() {
        return this.api;
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        // Handle the incoming message event
    }

    public void start() {
        logger.info("Discord bot started!");
    }
}
