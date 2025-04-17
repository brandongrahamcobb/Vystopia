package com.brandongcobb.vyrtuous.utils.handlers;

import org.javacord.api.entity.permission.Role;
import com.brandongcobb.vyrtuous.Config;
import com.brandongcobb.vyrtuous.bots.DiscordBot;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.DiscordApi;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

public class Predicator {

    private final Config config;
    private final DiscordBot bot; // Assuming DiscordBot is your main bot class

    public Predicator(DiscordBot bot) {
        this.bot = bot;
        this.config = bot.config; // Assuming getConfig() returns a Map of your configuration
    }

    public boolean atHome(Server server) {
        return server != null && server.getId() == (long) config.getConfigValue("discord_testing_guild_id");
    }

    public boolean releaseMode(User user, ServerTextChannel channel) {
        return user.getId() == 154749533429956608L || // Your developer ID
               (boolean) config.getConfigValue("discord_release_mode") ||
               (channel instanceof PrivateChannel);
    }

    public boolean isDeveloper(User user) {
        return user != null && user.getId() == (long) config.getConfigValue("discord_owner_id");
    }

    public boolean isVeganUser(User user) {
        List<Long> serverIds = (List<Long>) config.getConfigValue("discord_testing_guild_ids");
        for (Long serverId : serverIds) {
            Server server = getServerById(serverId);
            if (server != null) {
                // Find the "vegan" role from the server's roles
                Optional<Role> veganRoleOpt = server.getRoles().stream()
                    .filter(role -> role.getName().equals("vegan"))
                    .findFirst(); // Find the first role with the name "vegan"
    
                // Check if the role exists
                if (veganRoleOpt.isPresent()) {
                    Role veganRole = veganRoleOpt.get(); // Get the role if present
                    // Check if the user is in that role
                    if (veganRole.getUsers().contains(user)) { // Now call getUsers() on the Role object
                        return true; // User has the vegan role
                    }
                }
            }
        }
        return false;
    }

    public Server getServerById(long serverId) {
        Set<Server> servers = bot.getApi().getServers(); // Get all servers
        for (Server server : servers) {
            if (server.getId() == serverId) {
                return server; // Return the server wrapped in an Optional
            }
        }
    }
    public boolean isReleaseMode(ServerTextChannel channel, User user) {
        return user.getId() == 154749533429956608L || // Your developer ID
               (boolean) config.getConfigValue("discord_release_mode") ||
               (channel instanceof PrivateChannel);
    }
}
