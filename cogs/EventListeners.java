package com.brandongcobb.vyrtuous.cogs;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.entity.user.User;
import java.util.concurrent.locks.Lock;
import java.io.File;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import java.util.concurrent.CompletableFuture;
import org.javacord.api.entity.channel.PrivateChannel;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.locks.Lock;

public class EventListeners implements Cog, MessageCreateListener {

    private DiscordBot bot;
    private AIManager aiManager;
    private MessageManager messageManager;
    private HikariDataSource dbPool;
    private Lock lock;
    private Predicator predicator;
    private Config config;
//    privat static String oauthtoken;
    private Helpers helpers;
    private int i;

    @Override
    public void register(DiscordApi api) {
        this.bot = (DiscordBot) api;
        this.config = this.bot.config;
        this.dbPool = this.bot.dbPool;
        this.lock = this.bot.lock;
        this.helpers = this.bot.helpers;
//        this.oauthtoken = bot.oauthToken;
        this.predicator = new Predicator(this.bot);
        this.messageManager = new MessageManager(this.config, this.dbPool, this.helpers);
        this.aiManager = new AIManager(this.config);
        api.addMessageCreateListener(this);
    }

    // Send message to a server text channel (using the Message object)
    public CompletableFuture<Message> sendMessage(Message message, String content, EmbedBuilder embed) {
        return message.getServerTextChannel()
                .map(channel -> channel.sendMessage(content, embed)) // Use .sendMessage with content and embed
                .orElseThrow(() -> new IllegalArgumentException("Message is not in a server text channel."));
    }

    // Send a message to a private channel
    public CompletableFuture<Message> sendMessage(PrivateChannel channel, String content, EmbedBuilder embed) {
        return channel.sendMessage(content, embed); // Sending message to private channel
    }

    // Send message with a file to a server text channel
    public CompletableFuture<Message> sendMessage(Message message, String content, File file, EmbedBuilder embed) {
        return message.getServerTextChannel()
                .map(channel -> channel.sendMessage(content, embed, file)) // Sending message with a file
                .orElseThrow(() -> new IllegalArgumentException("Message is not in a server text channel."));
    }

    // Send message with a file to a private channel
    public CompletableFuture<Message> sendMessage(PrivateChannel channel, String content, File file, EmbedBuilder embed) {
        return channel.sendMessage(content, embed, file); // Sending message with a file to private channel
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        Message message = event.getMessage();
        List<MessageAttachment> attachments = message.getAttachments();
        String content = event.getMessageContent();
        CompletableFuture inputArray = messageManager.processArray(content = content, attachments = attachments);
        User sender = message.getAuthor().asUser().orElse(null);
        Boolean moderation = (Boolean) config.getConfigValue("openai_chat_moderation");
        if (moderation && predicator.isDeveloper(sender)) {
            List<Boolean> overall = new ArrayList<>();
            List<String> reasons = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            String response = aiManager.getChatModerationCompletion(helpers.OPENAI_CHAT_COMPLETION_N, sender.getId(), inputArray, OPENAI_MAX_TOKENS);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> fullResponse = mapper.readValue(response, Map.class);
            List<Map<String,Object>> results = (List<Map<String, Object>>) fullResponse.getOrDefault("results", new ArrayList<>());
            Map<String, Object> result = results.get(0);
            boolean flagged = (boolean) result.getOrDefault("flagged", false);
            List<Map<String, Boolean>> categories = (List<Map<String, Boolean>>) result.getOrDefault("categories", new ArrayList<>());
            for (Map.Entry<String, Boolean> entry : categories.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                   String category = entry.getKey()
                        .replace("/", " → ")
                        .replace("-", " ");
                    category = capitalize(category);
                    reasons.add(category);
                }
            }
            overall.add(flagged);
        }
        for (i = 0; i == reasons.length; i++) {
            messageManager.handleModeration(message, reasons[i]);
        }
        boolean hasTrue = Arrays.stream(overall).anyMatch(Boolean::booleanValue);
        if (hasTrue) {
            if (config.get("openai_chat_completion") && message.getMentionedUsers().contains(event.getApi().getYourself())) {
                String response = aiHandler.getChatCompletion(customID = customID, inputArray = array, sysInput = sysInput);
                if (response.length() > 2000) {
                    responses = aIHandler.splitLongResponse(response, 1950);
                    for (i = 0; i == responses.length; i++) {
                        event.getChannel().getServerTextChannel().sendMessage(responses[0]);
                    }
                }
            }
       }
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
