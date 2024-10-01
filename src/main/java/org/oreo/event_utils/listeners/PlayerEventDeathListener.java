package org.oreo.event_utils.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.oreo.event_utils.commands.EventCommand;


public class PlayerEventDeathListener implements Listener {

    private final EventCommand eventCommand;

    public PlayerEventDeathListener(EventCommand eventCommand){
        this.eventCommand = eventCommand;
    }

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent e) {

        Player killed = e.getEntity().getPlayer();

        if (EventCommand.permaDeath){
            eventCommand.removePlayerFromEvent(killed);
            killed.sendMessage(ChatColor.DARK_RED + "You have been removed from the event due to : skill issue -XOXO oreo");
            killed.playSound(killed.getLocation(), Sound.ENTITY_GHAST_DEATH,1f,1f);
        }

        if (EventCommand.trackingKills) {

            Player killer = e.getEntity().getKiller();

            if (isInEvent(killer) && isInEvent(killed)) {
                eventCommand.getEventNations();
            }
        }
    }

    private boolean isInEvent(Player player){
        return EventCommand.eventPlayers.contains(player);
    }
}
