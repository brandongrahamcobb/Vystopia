package com.brandongcobb.vyrtuous.cogs;

import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;


public class HybridCommands implements Cog, MessageCreateListener {

    @Override
    public void register(DiscordApi api) {
        api.addMessageCreateListener(this);
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        String content = event.getMessageContent();
        if (content.equalsIgnoreCase("!admin")) {
            event.getChannel().sendMessage("Admin command executed!");
        }
    }
}
