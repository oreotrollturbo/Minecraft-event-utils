package org.oreo.event_utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.oreo.event_utils.commands.EventCommand;
import org.oreo.event_utils.listeners.PlayerEventDeathListener;

public final class EventUtils extends JavaPlugin {

    @Override
    public void onEnable() {

        // Save the default config.yml if it doesn't exist
        this.saveDefaultConfig();

        EventCommand eventCommand = new EventCommand(this);

        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new PlayerEventDeathListener(eventCommand ),this);

        getCommand("event_utils").setExecutor(eventCommand);

    }
}