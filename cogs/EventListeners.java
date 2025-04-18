package com.brandongcobb.vyrtuous.cogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.MessageContent;
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

    private DiscordApi api;
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
    private boolean flagged;

    public EventListeners (DiscordBot bot) {
        this.bot = bot;
        this.api = this.bot.getApi();
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
    }

    @Override
    public void register (DiscordApi apix) {
        this.api.addMessageCreateListener(this);
    }

   @Override
   public void onMessageCreate(MessageCreateEvent event) {
       Message message = event.getMessage();
       List<MessageAttachment> attachments = message.getAttachments();
       String content = event.getMessageContent();
       CompletableFuture<List<MessageContent>> inputArray = messageManager.processArray(content, attachments);
       User sender = message.getAuthor().asUser().orElse(null);
       long senderId = sender.getId();
       boolean moderation = config.getBooleanValue("openai_chat_moderation");
   
       if (moderation && predicator.isDeveloper(sender)) {
           List<Boolean> overall = new ArrayList<>();
           List<String> reasons = new ArrayList<>();
           ObjectMapper mapper = new ObjectMapper();
   
           try {
               CompletableFuture<String> moderationResponse = aiManager.getChatModerationCompletion(sender.getId(), inputArray);
               moderationResponse.thenAccept(response -> {
                   try {
                       // Assuming response is a JSON string representation
                       Map<String, Object> responseMap = mapper.readValue(response, new HashMap<String, Object>().getClass());
               
                       // Extract results from the response
                       List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
               
                       // Check for results being present and not empty
                       if (results != null && !results.isEmpty()) {
                           Map<String, Object> result = results.get(0); // Get the first result
                           
                           // Extract flagged value
                           boolean flagged = (boolean) result.get("flagged");
               
                           // Extract categories
                           Map<String, Boolean> categories = (Map<String, Boolean>) result.get("categories");
                           for (Map.Entry<String, Boolean> entry : categories.entrySet()) {
                               if (Boolean.TRUE.equals(entry.getValue())) {
                                   String category = entry.getKey()
                                       .replace("/", " → ")
                                       .replace("-", " ");
                                   category = capitalize(category);
                                   reasons.add(category); // Assuming reasons is defined in your scope
                               }
                           }
                       }
                   } catch (IOException e) {
                       e.printStackTrace(); // Log the exception
                   }
               }).exceptionally(e -> {
                   e.printStackTrace(); // Handle any exception that occurs in the CompletableFuture
                   return null;
               });
   
               overall.add(flagged);
               for (int i = 0; i < reasons.size(); i++) { // Use < instead of ==
                   moderationManager.handleModeration(message, reasons.get(i));
               }
               boolean hasTrue = overall.stream().anyMatch(Boolean::booleanValue);
               if (hasTrue) {
                   if (Boolean.parseBoolean(config.getConfigValue("openai_chat_completion").toString()) && message.getMentionedUsers().contains(event.getApi().getYourself())) {
                       CompletableFuture<String> chatResponse = aiManager.getCompletion(senderId, inputArray);
                       chatResponse.thenAccept(response -> {
                           if (response.length() > 2000) {
                               List<String> responses = aiManager.splitLongResponse(response, 1950);
                               String[] responsesArray = responses.toArray(new String[0]);
                               for (String resp : responsesArray) {
                                   messageManager.sendDiscordMessage(message, resp);
                               }
                           } else {
                               messageManager.sendDiscordMessage(message, response);
                           }
                       });
                   }
               }
           } catch (IOException e) {
               e.printStackTrace(); // Handle exception appropriately
           }
       }
   }

   public static List<Map<String, Object>> convertStringToList(String jsonString) throws IOException {
       ObjectMapper objectMapper = new ObjectMapper();
       return objectMapper.readValue(jsonString, new TypeReference<List<Map<String, Object>>>() {});
   }

    private static String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}

