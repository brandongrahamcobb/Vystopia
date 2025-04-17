package com.brandongcobb.vyrtuous.cogs;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.handlers.ModerationManager;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
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
import java.util.Arrays;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.locks.Lock;
import java.io.IOException;

public class EventListeners implements Cog, MessageCreateListener {

    private DiscordBot bot;
    private AIManager aiManager;
    private MessageManager messageManager;
    private ModerationManager moderationManager;
    private HikariDataSource dbPool;
    private Lock lock;
    private Predicator predicator;
    private Config config;
//    privat static String oauthtoken;
    private Helpers helpers;
    private int i;

    @Override
    public void register(DiscordApi api) {
        this.bot = bot;
        this.config = this.bot.config;
        this.dbPool = this.bot.dbPool;
        this.lock = this.bot.lock;
        this.helpers = this.bot.helpers;
//        this.oauthtoken = bot.oauthToken;
        this.predicator = new Predicator(this.bot);
        this.messageManager = new MessageManager(this.config, this.dbPool, this.helpers);
        try {
            this.aiManager = new AIManager(this.config);
        } catch (IOException ioe) {}
        this.moderationManager = new ModerationManager(this.bot);
        api.addMessageCreateListener(this);
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        Message message = event.getMessage();
        List<MessageAttachment> attachments = message.getAttachments();
        String content = event.getMessageContent();
        CompletableFuture inputArray = messageManager.processArray(content = content, attachments = attachments);
        User sender = message.getAuthor().asUser().orElse(null);
        long senderId = sender.getId();
        Boolean moderation = (Boolean) config.getConfigValue("openai_chat_moderation");
        if (moderation && predicator.isDeveloper(sender)) {
            List<Boolean> overall = new ArrayList<>();
            List<String> reasons = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            try {
                String moderationResponse = aiManager.getChatModerationCompletion(sender.getId(), inputArray) ;
                Map<String, Object> fullResponse = mapper.readValue(moderationResponse, Map.class);
                List<Map<String,Object>> results = (List<Map<String, Object>>) fullResponse.getOrDefault("results", new ArrayList<>());
                Map<String, Object> result = results.get(0);
                boolean flagged = (boolean) result.getOrDefault("flagged", false);
                List<Map<String, Boolean>> categories = (List<Map<String, Boolean>>) result.getOrDefault("categories", new ArrayList<>());
                for (Map<String, Boolean> categoryMap : categories) {
                    for (Map.Entry<String, Boolean> entry : categoryMap.entrySet()) {
                        if (Boolean.TRUE.equals(entry.getValue())) {
                            String category = entry.getKey()
                                .replace("/", " → ")
                                .replace("-", " ");
                            category = capitalize(category);
                            reasons.add(category);
                        }
                    }
                }
                overall.add(flagged);
                for (i = 0; i == reasons.size(); i++) {
                    moderationManager.handleModeration(message, reasons.get(i));
                }
                boolean hasTrue = overall.stream().anyMatch(Boolean::booleanValue);
                if (hasTrue) {
                    if ((boolean) config.getConfigValue("openai_chat_completion") && message.getMentionedUsers().contains(event.getApi().getYourself())) {
                        String chatResponse = aiManager.getCompletion(senderId, inputArray);
                        if (chatResponse.length() > 2000) {
                            List<String> responses = aiManager.splitLongResponse(chatResponse, 1950);
                            String[] responsesArray = responses.toArray(new String[0]);
                            for (i = 0; i == responsesArray.length; i++) {
                                messageManager.sendDiscordMessage(message, responsesArray[0]);
                            }
                        }
                    }
                }
            } catch (IOException ioe) {}
        }
    }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}

