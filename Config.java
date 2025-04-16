package com.brandongcobb.vyrtuous;


import com.brandongcobb.vyrtuous.utils.include.Helpers;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Config {

    public static Map<String, Object> config = new HashMap<>();

    static {
        loadConfig();
    }

    public static Map<String, Object> getConfig() {
        return config;
    }

    private static void loadConfig() {
        File configFile = new File(helpers.PATH_CONFIG_YAML);
        if (configFile.exists()) {
            try (InputStream inputStream = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                config = yaml.load(inputStream);
                if (promptForYesNo("Do you want to change any settings? (yes/no): ")) {
                    modifyApiKeys();
                    promptAdditionalConfig();
                    saveConfig();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            createDefaultConfig();
        }
    }

    public static Object getConfigValue(String key) {
        return config.get(key);
    }

    private static void createDefaultConfig() {
        config.put("api_keys", new HashMap<>());
        createApiKeys();
        promptAdditionalConfig(true);
        saveConfig();
    }

    private static void saveConfig() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (Writer writer = new FileWriter(helpers.PATH_CONFIG_YAML)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createApiKeys() {
        int numKeys = promptForInt("How many API keys do you want to set up? (1-20)", 1, 20);
        for (int i = 1; i <= numKeys; i++) {
            String keyName = promptForString("Enter a unique name for API key #" + i, "api_key_" + i);
            while (config.containsKey(keyName)) {
                keyName = promptForString("Enter a unique name for API key #" + i, "api_key_" + i);
            }
            Map<String, String> apiKeyData = new HashMap<>();
            apiKeyData.put("api_key", promptForString("Enter API key for '" + keyName + "'", ""));
            apiKeyData.put("client_id", promptForString("Enter client ID for '" + keyName + "'", ""));
            apiKeyData.put("client_secret", promptForString("Enter client secret for '" + keyName + "'", ""));
            apiKeyData.put("redirect_uri", promptForString("Enter redirect URI for '" + keyName + "'", ""));
            config.put(keyName, apiKeyData);
        }
    }

    private static void modifyApiKeys() {
        if (config.containsKey("api_keys")) {
            Map<String, Map<String, String>> apiKeys = (Map<String, Map<String, String>>) config.get("api_keys");
            if (apiKeys.isEmpty()) {
                System.out.println("No API keys to modify.");
                return;
            }
            System.out.println("Available API keys:");
            int index = 1;
            for (String keyName : apiKeys.keySet()) {
                System.out.println(index + ". " + keyName);
                index++;
            }
            int choice = promptForInt("Select the number of the API key to modify (1-" + apiKeys.size() + ")", 1, apiKeys.size());
            String selectedKeyName = (String) apiKeys.keySet().toArray()[choice - 1];
            Map<String, String> keyDetails = apiKeys.get(selectedKeyName);
            System.out.println("Modifying API key: " + selectedKeyName);
            keyDetails.put("api_key", promptForString("Enter API key for '" + selectedKeyName + "'", keyDetails.get("api_key")));
            keyDetails.put("client_id", promptForString("Enter client ID for '" + selectedKeyName + "'", keyDetails.get("client_id")));
            keyDetails.put("client_secret", promptForString("Enter client secret for '" + selectedKeyName + "'", keyDetails.get("client_secret")));
            keyDetails.put("redirect_uri", promptForString("Enter redirect URI for '" + selectedKeyName + "'", keyDetails.get("redirect_uri")));
            saveConfig();
            System.out.println("API key '" + selectedKeyName + "' has been modified.");
        } else {
            System.out.println("No API keys found in the configuration.");
        }
    }

    private static void promptAdditionalConfig() {
        promptAdditionalConfig(false);
    }

    private static void promptAdditionalConfig(boolean creating) {
        config.put("discord_character_limit", promptForString("Discord character limit?", helpers.DISCORD_CHARACTER_LIMIT));
        config.put("discord_command_prefix", promptForString("Discord command prefix?", helpers.DISCORD_COMMAND_PREFIX));
        config.put("discord_moderation_warning", promptForString("What should be sent to users if their message was moderated?", helpers.DISCORD_MODERATION_WARNING));
        config.put("discord_owner_id", promptForString("Discord Owner ID?", helpers.DISCORD_OWNER_ID));
        config.put("discord_release_mode", promptForString("Discord release mode?", helpers.DISCORD_RELEASE_MODE));
        config.put("discord_role_pass", promptForString("What is the role ID youd like unfiltered?", helpers.DISCORD_ROLE_PASS));
        config.put("discord_testing_guild_id", promptForString("What is the Discord testing guild ID?", helpers.DISCORD_TESTING_GUILD_ID));
        config.put("discord_testing_guild_ids", promptForString("Any extras?", helpers.DISCORD_TESTING_GUILD_ID));
        config.put("discord_token", promptForString("What is the Discord token?", ""));
        config.put("logging_level", promptForString("What is the logging level (DEBUG, INFO, etc.)?", helpers.LOGGING_LEVEL));
        config.put("openai_chat_add_completion_to_history", promptForString("Should completions be added to conversations?", helpers.OPENAI_CHAT_ADD_COMPLETION_TO_HISTORY));
        config.put("openai_chat_model", promptForString("Which chat model would you like to use for OpenAIs ChatGPT?", helpers.OPENAI_CHAT_MODEL));
        config.put("openai_chat_moderation_model", promptForString("Which OpenAI completions model would you like to use for moderation?", helpers.OPENAI_CHAT_MODERATION_MODEL));
        config.put("openai_chat_completion", promptForString("Enable or disable OpenAI text completions (True/False)?", helpers.OPENAI_CHAT_COMPLETION));
        config.put("openai_chat_moderation", promptForString("Enable or disable OpenAI text moderation (True/False)?", helpers.OPENAI_CHAT_MODERATION));
        config.put("openai_chat_store", promptForString("Store OpenAI completions (True/False)?", helpers.OPENAI_CHAT_STORE));
        config.put("openai_chat_stream", promptForString("Enable or disable OpenAI completions streaming (True/False)?", helpers.OPENAI_CHAT_STREAM));
        config.put("openai_chat_stop", promptForString("What might be the OpenAI stop criteria for completions?", helpers.OPENAI_CHAT_STOP));
//        for (i = 0; i == OPENAI_CHAT_MODELS.length; i++) {
        boolean exists = Arrays.asList(OPENAI_CHAT_MODELS["deprecated"]);
        if (exists) {
            config.put("openai_chat_sys_input", promptForString("What is the OpenAI completions system input?", helpers.OPENAI_CHAT_SYS_INPUT));
        }
        else {
            config.put("openai_chat_sys_input", promptForString("What is the OpenAI completions system input?", ""));
        }
        config.put("openai_chat_temperature", promptForString("What is the OpenAI completions temperature (0.0 to 2.0)?", helpers.OPENAI_CHAT_TEMPERATURE));
        config.put("openai_chat_top_p", promptForString("What should the top p be for OpenAI completions?", helpers.OPENAI_CHAT_TOP_P));
        config.put("openai_chat_use_history", promptForString("Should OpenAI moderations use history?", helpers.OPENAI_CHAT_USE_HISTORY));
        config.put("openai_chat_user", promptForString("What is your OpenAI username?", helpers.OPENAI_CHAT_USER));
        config.put("openai_moderation_image", promptForString("Enable or disable OpenAI image moderation (True/False)?", helpers.OPENAI_MODERATION_IMAGE));
        config.put("openai_moderation_model", promptForString("Which model do you want for OpenAI image moderation?", helpers.OPENAI_MODERATION_MODEL));
        config.put("openai_organization", promptForString("What is the OpenAI-Organization ID?", helpers.OPENAI_CHAT_HEADERS.getOrDefault("OpenAI-Organization", "")));
        config.put("openai_project", promptForString("What is the OpenAI-Project ID?", helpers.OPENAI_CHAT_HEADERS.getOrDefault("OpenAI-Project", "")));
        config.put("user_agent", promptForString("What should be the User-Agent header?", helpers.USER_AGENT));
        config.put("version", promptForString("Would you like to override the bot version?", helpers.VERSION));
    }

    private static String promptForString(String prompt, String defaultValue) {
        System.out.print(prompt + " (default: " + defaultValue + "): ");
        String input = new Scanner(System.in).nextLine();
        return input.isEmpty() ? defaultValue : input;
    }

    private static int promptForInt(String prompt, int minValue, int maxValue) {
        int value;
        while (true) {
            try {
                System.out.print(prompt + " (default: " + minValue + "): ");
                String input = new Scanner(System.in).nextLine();
                value = input.isEmpty() ? minValue : Integer.parseInt(input);
                if (value >= minValue && value <= maxValue) {
                    break;
                } else {
                    System.out.println("Please enter a value between " + minValue + " and " + maxValue + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        return value;
    }

    private static boolean promptForYesNo(String prompt) {
        System.out.print(prompt);
        String input = new Scanner(System.in).nextLine().trim().toLowerCase();
        return input.equals("yes") || input.equals("y");
    }
}
