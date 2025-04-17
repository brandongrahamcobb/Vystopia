package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
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

public class MessageManager {

    private DiscordBot bot;
    private AIManager aiManager;
    private MessageManager messageManager;
    private final HikariDataSource dbPool;
    private Lock lock;
    private Predicator predicator;
    private final Config config;
//    privat static String oauthtoken;
    private Helpers helpers;
    
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

        for (Attachment attachment : attachments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                File file = new File(helpers.DIR_TEMP, attachment.getFileName());
                try {
                    // Save the attachment to a file
                    attachment.download(file).join(); // Blocking call to download the file
                    if (attachment.getContentType().startsWith("image/")) {
                        byte[] imageBytes = Files.readAllBytes(file.toPath());
                        String imageBase64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                        processedAttachments.add(new MessageContent("image_url", "data:" + attachment.getContentType() + ";base64," + imageBase64));
                    } else if (attachment.getContentType().startsWith("text/")) {
                        String textContent = new String(Files.readAllBytes(file.toPath()));
                        processedAttachments.add(new MessageContent("text", textContent));
                    }
                } catch (IOException e) {
                    logger.severe("Error processing file " + attachment.getFileName() + ": " + e.getMessage());
                }
            });
            futures.add(future);
        }

        // Wait for all futures to complete
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> processedAttachments);
    }

    public CompletableFuture<List<MessageContent>> processTextMessage(String content) {
        return CompletableFuture.completedFuture(List.of(new MessageContent("text", content.replace("<@1318597210119864385>", ""))));
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

    public CompletableFuture<Void> sendDM(User user, String content, File file, EmbedBuilder embed) {
        return user.openPrivateChannel().thenCompose(channel -> sendMessage(channel, content, file, embed));
    }

    public CompletableFuture<Message> sendMessage(Message message, String content) {
        return message.getServerTextChannel()
                .map(channel -> channel.sendMessage(content)) // Send message content only
                .orElseThrow(() -> new IllegalArgumentException("Message is not in a server text channel."));
    }

    public CompletableFuture<Void> sendMessage(Message message, String content, File file, EmbedBuilder embed) {
        return message.getChannel().sendMessage(content, file, embed);
    }

    private CompletableFuture<Void> sendMessage(PrivateChannel channel, String content, File file, EmbedBuilder embed) {
        return channel.sendMessage(content, file, embed);
    }

    public boolean hasAttachments(Message message) {
        return !message.getAttachments().isEmpty();
    }

    public String getMessageContent(Message message) {
        return message.getContent();
    }

    public static class MessageContent {
        private final String type;
        private final String text;
        private final String imageData; // Only for images
        private final String contentType; // Content type for attachments

        public MessageContent(String type, String content) {
            this.type = type;
            this.text = content;
            this.imageData = null;
            this.contentType = null;
        }

        public MessageContent(String type, String imageData, String contentType) {
            this.type = type;
            this.text = null;
            this.imageData = imageData;
            this.contentType = contentType;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public String getImageData() {
            return imageData;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
