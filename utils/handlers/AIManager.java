package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Config;
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
    private CompletableFuture inputArray;
    private String model = helpers.OPENAI_CHAT_MODEL;
    private int maxTokens = helpers.OPENAI_CHAT_MODEL_OUTPUT_LIMITS.get(model);
    private String responseFormat = helpers.OPENAI_CHAT_RESPONSE_FORMAT;
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
        this.openaiApiKey = (String) ((Map<String, Object>) this.config.getNestedConfigValue("api_keys", "OpenAI")).get("api_key").toString();
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
            CompletableFuture<List<Map<String, Object>>> inputArray,
            int maxTokens,
            String model,
            String responseFormat,
            boolean stop,
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
                requestBody.put("top_p", top_p); // Note: Ensure variable name matches correctly
                requestBody.put("messages", messages);
    
                if (store) {
                    LocalDateTime now = LocalDateTime.now();
                    requestBody.put("metadata", new Object[]{
                            Map.of("user", customId, "timestamp", now)
                    });
                }
    
                // Process each message in the messages list
                for (Map<String, Object> message : messages) {
                    String role = (String) message.get("role");
                    String content = (String) message.get("content");
                    conversations
                            .computeIfAbsent(String.valueOf(customId), k -> new ArrayList<>())
                            .add(Map.of("role", role, "content", content));
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

    public String getCompletion(long customId, CompletableFuture inputArray) throws IOException {

        return getChatCompletion(config.getConfigValue("openai_chat_completion"), customId, inputArray, helpers.OPENAI_CHAT_MODEL_OUTPUT_LIMITS.get(config.getConfigValue("openai_chat_model")), config.getConfigValue("openai_chat_model"), helpers.OPENAI_CHAT_RESPONSE_FORMAT, config.getConfigValue("openai_chat_stop"), config.getConfigValue("openai_chat_stream"), config.getConfigValue("openai_chat_sys_input"), config.getConfigValue("openai_chat_temperature"), config.getConfigValue("openai_chat_top_p"), helpers.OPENAI_CHAT_USE_HISTORY, helpers.OPENAI_CHAT_ADD_COMPLETION_TO_HISTORY);
    }

    public String getChatModerationCompletion(long customId, CompletableFuture inputArray) throws IOException {

        return getChatCompletion(config.getConfigValue("openai_chat_completion"), customId, inputArray, helpers.OPENAI_CHAT_MODEL_OUTPUT_LIMITS.get(config.getConfigValue("openai_chat_model")), config.getConfigValue("openai_chat_model"), helpers.OPENAI_CHAT_MODERATION_RESPONSE_FORMAT, config.getConfigValue("openai_chat_stop"), config.getConfigValue("openai_chat_stream"), config.getConfigValue("openai_chat_sys_input"), config.getConfigValue("openai_chat_temperature"), config.getConfigValue("openai_chat_top_p"), helpers.OPENAI_CHAT_MODERATION_USE_HISTORY, helpers.OPENAI_CHAT_MODERATION_ADD_COMPLETION_TO_HISTORY);
    }

    private String extractCompletion(String jsonResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
        return ((Map<String, String>) ((List<Map<String, Object>>) responseMap.get("choices")).get(0)).get("message").get("content");
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
        int maxContextLength = OPENAI_MODEL_CONTEXT_LIMITS.get(model, 4096);

        List<Map<String, String>> history = conversations.get(customId, new ArrayList<>());
        int totalTokens = history.stream()
            .mapToInt(msg -> msg.get("content", "").length())
            .sum();

        while (totalTokens > maxContextLength && !history.isEmpty()) {
            Map<String, String> removedMessage = history.remove(0);
            totalTokens -= removedMessage.get("content", "").length();
        }

        // Save trimmed history back
        conversations.put(customId, history);
    }
}



