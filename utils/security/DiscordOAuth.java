package com.brandongcobb.vyrtuous.utils.security;

import com.brandongcobb.vyrtuous.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class DiscordOAuth {

    private Config config;
    private String clientId = config.getNestedConfigValue("api_keys", "Discord").get("api_key");
    private String clientSecret = config.getNestedConfigValue("api_keys", "Discord").get("client_secret");
    private String redirectUri = config.getNestedConfigValue("api_keys", "Discord").get("redirect_uri");

    public DiscordOAuth(Config config) {
        this.config = config;
    }


    public static void main(String[] args) {
        SpringApplication.run(DiscordOAuth.class, args);
    }

    @GetMapping("/discord_authorize")
    public ResponseEntity<Void> discordAuthorize() {
        String authUrl = getAuthorizationUrl();
        System.out.println("Redirecting to Discord OAuth URL: " + authUrl); // For logging
        return ResponseEntity.status(302).location(URI.create(authUrl)).build(); // Redirecting
    }

    private String getAuthorizationUrl() {
        return "https://discord.com/api/oauth2/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&permissions=2141842833124" +
                "&integration_type=0" +
                "&scope=bot";
    }

    @GetMapping("/discord_callback")
    public String discordCallback(@RequestParam("code") String code) {
        if (code == null || code.isEmpty()) {
            System.err.println("Missing Discord authorization code in callback.");
            return "Missing authorization code";
        }
        System.out.println("Discord authorization code received: " + code); // For logging
        boolean success = exchangeToken(code);
        if (!success) {
            return "Discord token exchange failed.";
        }
        return "Discord authentication successful! You can close this window.";
    }

    private boolean exchangeToken(String code) {
        String tokenUrl = "https://discord.com/api/oauth2/token";

        // Prepare request data
        Map<String, String> data = new HashMap<>();
        data.put("client_id", clientId);
        data.put("client_secret", clientSecret);
        data.put("code", code);
        data.put("grant_type", "authorization_code");
        data.put("redirect_uri", redirectUri);
        data.put("scope", "identify email");

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(data, headers);

        // Send POST request to exchange token
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, requestEntity, Map.class);

        Map<String, Object> tokenData = response.getBody();
        if (tokenData != null && tokenData.containsKey("access_token")) {
            String accessToken = (String) tokenData.get("access_token");
            // You can store the access token, refresh token, etc. as needed
            System.out.println("Access Token: " + accessToken);
            return true;
        } else {
            System.err.println("Discord token exchange failed: " + tokenData);
            return false;
        }
    }
}
