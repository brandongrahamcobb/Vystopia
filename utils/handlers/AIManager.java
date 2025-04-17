package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.utils.handlers.MessageContent;
import com.brandongcobb.vyrtuous.utils.include.Helpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.yaml.snakeyaml.Yaml;
import java.util.concurrent.CompletableFuture;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIManager {

    private static Config config;
    private static Map<String, List<Map<String, String>>> conversations;
    private static String openaiApiKey;
    private static Helpers helpers = new Helpers();
    private int n = helpers.OPENAI_CHAT_N;
    private long customID;
    private CompletableFuture<List<Map<String, Object>>> inputArray;
    private String model = helpers.OPENAI_CHAT_MODEL;
    private int maxTokens = helpers.OPENAI_CHAT_MODEL_OUTPUT_LIMITS.get(model);
    private Map<String, Object> responseFormat = helpers.OPENAI_CHAT_RESPONSE_FORMAT;
    private String stop = helpers.OPENAI_CHAT_STOP;
    private boolean store = helpers.OPENAI_CHAT_STORE;
    private boolean stream = helpers.OPENAI_CHAT_STREAM;
    private String sysInput = helpers.OPENAI_CHAT_SYS_INPUT;
    private float temperature = helpers.OPENAI_CHAT_TEMPERATURE;
    private float topP = helpers.OPENAI_CHAT_TOP_P;
    private boolean addCompletionToHistory = helpers.OPENAI_CHAT_MODERATION_ADD_COMPLETION_TO_HISTORY;
    private int i;
    private long customId;

    public AIManager(Config config) throws IOException {
        // Load configuration
        this.config = config;
        this.helpers = helpers;
        this.conversations = new HashMap<>();
        this.openaiApiKey = this.config.getNestedConfigValue("api_keys", "OpenAI").getStringValue("api_key");
    }

    private Map<String, Object> loadConfig(String path) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(path)) {
            return yaml.load(inputStream);
        }
    }

    public CompletableFuture<String> getChatCompletion(
            int n,
            long customId,
            CompletableFuture<List<MessageContent>> inputArray,
            int maxTokens,
            String model,
            Map<String, Object> responseFormat,
            String stop,
            boolean stream,
            String sysInput,
            float temperature,
            float top_p,
            boolean store,
            boolean addCompletionToHistory) throws IOException {
    
        return inputArray.thenApply(messages -> {
            String apiUrl = "https://api.openai.com/v1/chat/completions";
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(apiUrl);
                post.setHeader("Authorization", "Bearer " + openaiApiKey);
                post.setHeader("Content-Type", "application/json");
    
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("add_completions_to_history", addCompletionToHistory);
                requestBody.put("completions", n);
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("temperature", temperature);
                requestBody.put("model", model);
                requestBody.put("stop", stop);
                requestBody.put("store", store);
                requestBody.put("top_p", top_p);
                
                // Create the messages array for the API request
                List<Map<String, Object>> messagesList = new ArrayList<>();
                for (MessageContent messageContent : messages) {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("role", messageContent.getType()); // Assuming getType() returns the role ("user" or "assistant")
                    messageMap.put("content", messageContent.getText()); // Assuming getText() returns the message content
                    messagesList.add(messageMap);
                }
                requestBody.put("messages", messagesList); // Add the constructed messages list to the request body
    
                // If storing is enabled, add metadata
                if (store) {
                    LocalDateTime now = LocalDateTime.now();
                    requestBody.put("metadata", new Object[]{
                            Map.of("user", customId, "timestamp", now)
                    });
                }
    
                trimConversationHistory(model, String.valueOf(customId));
                // Convert the request body to JSON
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                post.setEntity(new StringEntity(jsonBody));
    
                // Execute the request
                try (CloseableHttpResponse response = httpClient.execute(post)) {
                    HttpEntity entity = response.getEntity();
                    String result = EntityUtils.toString(entity);
                    return extractCompletion(result);
                }
            } catch (IOException e) {
                // Handle exceptions appropriately
                throw new RuntimeException("Failed to get chat completion", e);
            }
        });
    }

   // Convert List<MessageContent> to List<Map<String, Object>>
    private List<Map<String, Object>> convertMessageContentToMap(List<MessageContent> messages) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (MessageContent msgContent : messages) {
            Map<String, Object> map = new HashMap<>();
            map.put("role", msgContent.getType()); // Assuming getType() returns the role
            map.put("content", msgContent.getText()); // Assuming getText() returns the content
            mapList.add(map);
        }
        return mapList;
    }

    public CompletableFuture<String> getCompletion(long customId, CompletableFuture<List<MessageContent>> inputArray) {
//        return inputArray.thenApply(messages -> {
//            List<Map<String, Object>> mapList = new ArrayList<>();
  //          for (MessageContent msgContent : messages) {
    //            Map<String, Object> map = new HashMap<>();
      //          map.put("role", msgContent.getType()); // Assuming getType() returns the role
        //        map.put("content", msgContent.getText()); // Assuming getText() returns the content
          //      mapList.add(map);
  //          return messages;
    //    }).thenCompose(messages -> {
      //      // Use the correct types and ensure you call the right CompletableFuture
        try {
            return getChatCompletion(
                helpers.OPENAI_CHAT_N,
                customId,
                inputArray,
                helpers.OPENAI_CHAT_MODEL_OUTPUT_LIMITS.get(config.getStringValue("openai_chat_model")),
                config.getStringValue("openai_chat_model"),
                helpers.OPENAI_CHAT_RESPONSE_FORMAT,
                config.getStringValue("openai_chat_stop"), // Handle stop as boolean
                config.getBooleanValue("openai_chat_stream"), // Handle stream as boolean
                helpers.OPENAI_CHAT_SYS_INPUT,
                config.getFloatValue("openai_chat_temperature"),
                config.getFloatValue("openai_chat_top_p"),
                helpers.OPENAI_CHAT_USE_HISTORY,
                helpers.OPENAI_CHAT_ADD_COMPLETION_TO_HISTORY
            );
        } catch (IOException ioe) {}
        return null;
    }

    public CompletableFuture<String> getChatModerationCompletion(long customId, CompletableFuture<List<MessageContent>> inputArray) throws IOException {
        return inputArray.thenApply(messages -> {
            //List<Map<String, Object>> mapList = new ArrayList<>();
            //for (MessageContent msgContent : messages) {
              //   Map<String, Object> map = new HashMap<>();
                //map.put("role", msgContent.getType()); // or whatever you use for the role
               // map.put("content", msgContent.getText()); // assuming MessageContent has a method getText()
                //mapList.add(map);
                return messages;
        }).thenCompose(messages -> {
            try {
                return getChatCompletion(helpers.OPENAI_CHAT_N, customId, inputArray, helpers.OPENAI_CHAT_MODEL_OUTPUT_LIMITS.get(config.getStringValue("openai_chat_model")), config.getStringValue("openai_chat_model"), helpers.OPENAI_CHAT_MODERATION_RESPONSE_FORMAT, config.getStringValue("openai_chat_stop"), config.getBooleanValue("openai_chat_stream"), helpers.OPENAI_CHAT_MODERATION_SYS_INPUT, config.getFloatValue("openai_chat_temperature"), config.getFloatValue("openai_chat_top_p"), helpers.OPENAI_CHAT_MODERATION_USE_HISTORY, helpers.OPENAI_CHAT_MODERATION_ADD_COMPLETION_TO_HISTORY);
            } catch (IOException ioe) {}
            return null;
        });
    }

    private String extractCompletion(String jsonResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
    
        // Check if "choices" exists and is not null
        if (!responseMap.containsKey("choices") || responseMap.get("choices") == null) {
            throw new IllegalArgumentException("Response does not contain 'choices'. Response: " + jsonResponse);
        }
    
        // Get the choices list from the response
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
    
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("The 'choices' list is empty. Response: " + jsonResponse);
        }
    
        // Get the first choice
        Map<String, Object> firstChoice = choices.get(0);
    
        // Get the message map
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
    
        // Get the content from the message
        return (String) message.get("content"); // Cast to String
    }

    public static List<String> splitLongResponse(String response, int limit) {
        List<String> outputChunks = new ArrayList<>();
        String[] parts = response.split("(?<=```)|(?=```)");
        boolean inCode = false;

        for (String part : parts) {
            if (part.equals("```")) {
                inCode = !inCode; // toggle code mode
                continue;
            }

            if (inCode) {
                outputChunks.add("```" + part + "```");
                inCode = false; // code block closed
            } else {
                // Split plain text into chunks
                for (int i = 0; i < part.length(); i += limit) {
                    int end = Math.min(i + limit, part.length());
                    outputChunks.add(part.substring(i, end));
                }
            }
        }

        return outputChunks;
    }
    // Additional methods for handling status tracking, API request, etc., can be added here
    public void trimConversationHistory(String model, String customId) {
        int maxContextLength = helpers.OPENAI_CHAT_MODEL_CONTEXT_LIMITS.get(model);
    
        // Check if the conversations map contains the key
        List<Map<String, String>> history = conversations.get(customId);
        if (history == null) {
            // Handle the situation when there is no conversation history
            System.out.println("No conversation history found for customId: " + customId);
            return; // or handle it as appropriate
        }
    
        int totalTokens = history.stream()
            .mapToInt(msg -> msg.get("content").length())
            .sum();
    
        while (totalTokens > maxContextLength && !history.isEmpty()) {
            Map<String, String> removedMessage = history.remove(0);
            totalTokens -= removedMessage.get("content").length();
        }
    
        // Save trimmed history back
        conversations.put(customId, history);
    }
}



