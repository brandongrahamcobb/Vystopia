package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.MessageContent;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.user.User;

import org.javacord.api.entity.channel.PrivateChannel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.io.InputStream;
import java.net.URL;

public class MessageManager {

    private DiscordBot bot;
    private AIManager aiManager;
    private final HikariDataSource dbPool;
    private Lock lock;
    private Predicator predicator;
    private final Config config;
//    privat static String oauthtoken;
    private Helpers helpers;
    private Logger logger;

    public MessageManager (Config config, HikariDataSource dbPool, Helpers helpers) {
        this.config = config;
        this.dbPool = dbPool;
        this.helpers = helpers;
    }

    public CompletableFuture<List<MessageContent>> processArray(String content, List<MessageAttachment> attachments) {
        List<MessageContent> array = new ArrayList<>();
        if (content != null && !content.trim().isEmpty()) {
            // Process the text message
            return processTextMessage(content).thenCompose(processedText -> {
                array.addAll(processedText);
                // Process attachments if any
                if (attachments != null && !attachments.isEmpty()) {
                    return processAttachments(attachments).thenApply(processedAttachments -> {
                        array.addAll(processedAttachments);
                        return array;
                    });
                }
                return CompletableFuture.completedFuture(array);
            });
        }
        return CompletableFuture.completedFuture(array);
    }

    public CompletableFuture<List<MessageContent>> processAttachments(List<MessageAttachment> attachments) {
        List<MessageContent> processedAttachments = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (MessageAttachment attachment : attachments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                File file = new File(helpers.DIR_TEMP, attachment.getFileName());
                try (InputStream in = attachment.getUrl().openStream()) {
                    Files.copy(in, file.toPath()); // Download the file from the URL
                    String contentType = getContentTypeFromFileName(attachment.getFileName());
                    if (contentType.startsWith("image/")) {
                        byte[] imageBytes = Files.readAllBytes(file.toPath());
                        String imageBase64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                        processedAttachments.add(new MessageContent("image_url", "data:" + contentType + ";base64," + imageBase64));
                    } else if (contentType.startsWith("text/")) {
                        String textContent = new String(Files.readAllBytes(file.toPath()));
                        processedAttachments.add(new MessageContent("text", textContent));
                    }
                } catch (IOException e) {
                    logger.severe("Error processing file " + attachment.getFileName() + ": " + e.getMessage());
                }
            });
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> processedAttachments);
    }

   private String getContentTypeFromFileName(String fileName) {
       if (fileName.endsWith(".png")) {
           return "image/png";
       } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
           return "image/jpeg";
       } else if (fileName.endsWith(".gif")) {
           return "image/gif";
       } else if (fileName.endsWith(".txt")) {
           return "text/plain";
       }
       // Add more content types as needed
       return "application/octet-stream"; // Default type
   }

    public CompletableFuture<List<MessageContent>> processTextMessage(String content) {
        return CompletableFuture.completedFuture(List.of(new MessageContent("user", content.replace("<@1318597210119864385>", ""))));
    }

    public boolean validateArray(List<MessageContent> array) {
        boolean valid = true;
        for (MessageContent item : array) {
            if ("image_base64".equals(item.getType())) {
                if (item.getImageData() == null || item.getContentType() == null) {
                    logger.severe("Invalid Base64 image data: " + item);
                    valid = false;
                }
            } else if ("text".equals(item.getType())) {
                if (item.getText() == null || item.getText().trim().isEmpty()) {
                    logger.severe("Invalid text content: " + item);
                    valid = false;
                }
            }
        }
        return valid;
    }

    public CompletableFuture<Message> sendDM(User user, String content) {
        return user.openPrivateChannel().thenCompose(channel -> channel.sendMessage(content));
    }

    // Send message to a server text channel (using the Message object)
    public CompletableFuture<Message> sendDiscordMessage(Message message, String content, EmbedBuilder embed) {
        return message.getServerTextChannel()
                .map(channel -> channel.sendMessage(content, embed)) // Use .sendMessage with content and embed
                .orElseThrow(() -> new IllegalArgumentException("Message is not in a server text channel."));
    }

    public CompletableFuture<Message> sendDiscordMessage(Message message, String content) {
        return message.getServerTextChannel()
                .map(channel -> channel.sendMessage(content)) // Send message content only
                .orElseThrow(() -> new IllegalArgumentException("Message is not in a server text channel."));
    }

    public CompletableFuture<Message> sendDiscordMessage(Message message, String content, File file) {
        return message.getChannel().sendMessage(content, file);
    }

    private CompletableFuture<Message> sendDiscordMessage(PrivateChannel channel, String content, File file) {
        return channel.sendMessage(content, file);
    }

    public CompletableFuture<Message> sendDiscordMessage(PrivateChannel channel, String content, EmbedBuilder embed) {
        return channel.sendMessage(content, embed); // Sending message to private channel
    }

    public boolean hasAttachments(Message message) {
        return !message.getAttachments().isEmpty();
    }

    public String getMessageContent(Message message) {
        return message.getContent();
    }

}
