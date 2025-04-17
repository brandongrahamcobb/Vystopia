package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.DiscordApi;

import java.util.List;
import java.util.Map;

public class Predicator {
    private final DiscordBot bot; // Assuming DiscordBot is your main bot class

    public Predicator(DiscordBot bot) {
        this.bot = bot;
        this.config = bot.config; // Assuming getConfig() returns a Map of your configuration
    }

    public boolean atHome(Server server) {
        return server != null && server.getId() == (long) config.getConfigVelue("discord_testing_guild_id");
    }

    public boolean releaseMode(User user, ServerTextChannel channel) {
        return user.getId() == 154749533429956608L || // Your developer ID
               (boolean) config.getOrDefault("discord_release_mode", false) ||
               (channel instanceof PrivateChannel);
    }

    public boolean isDeveloper(User user) {
        return user != null && user.getId() == (long) config.getConfigValue("discord_owner_id");
    }

    public boolean isVeganUser(User user) {
        List<Long> serverIds = (List<Long>) config.getConfigValue("discord_testing_guild_ids");
        for (Long serverId : serverIds) {
            Server server = bot.getApi().getServerById(serverId).join();
            if (guild != null) {
                // Assuming you have a method to check member roles
                if (server.getRoleById(server.getRoles().stream().filter(role -> role.getName().equals("vegan")).findFirst().get().getId()).join().getUsers().contains(user)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isReleaseMode(ServerTextChannel channel, User user) {
        return user.getId() == 154749533429956608L || // Your developer ID
               (boolean) config.getOrDefault("discord_release_mode", false) ||
               (channel instanceof PrivateChannel);
    }
}
