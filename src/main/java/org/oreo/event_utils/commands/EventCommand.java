package org.oreo.event_utils.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.oreo.event_utils.EventUtils;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Nation;

import java.util.*;
import java.util.stream.Collectors;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final EventUtils plugin;

    private final Map<Nation, Integer> nationKills = new HashMap<>();

    public static List<Player> eventPlayers = new ArrayList<>();
    private boolean eventTimerRunning = true;

    public static boolean trackingKills = false;
    public static boolean permaDeath = false;

    public EventCommand(EventUtils plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        List<String> eventAdmins = plugin.getConfig().getStringList("eventAdmins");

        if (!(commandSender instanceof Player)){ // Make sure it's being set by a player
            commandSender.sendMessage("You can only use this command in game");
            return true;
        }

        Player sender = (Player)commandSender;

        if (!eventAdmins.contains(sender.getName()) && !sender.isOp()){
            sender.sendMessage(ChatColor.RED + "You aren't allowed to use this command :(");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "You need to specify a command to use");
            return true;
        }

        String firstParam = args[0].toLowerCase();
        switch (firstParam){ //Handle all sub-commands
            case "help":
                showCommands(sender);
                break;

            case "add":
                addPlayersToEvent(args);
                sender.sendMessage(ChatColor.AQUA + "Player added to event successfully");
                break;

            case "remove":
                removePlayersFromEvent(args,sender);
                break;

            case "clear":
                eventPlayers.clear();
                sender.sendMessage(ChatColor.AQUA + "Event players list cleared");
                break;

            case "timer":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /timer <start|end> [time]");
                    break;
                }

                if (args[1].equalsIgnoreCase("start")) {
                    if (args.length < 3) {
                        sender.sendMessage(ChatColor.RED + "Usage: /timer start <time>");
                        break;
                    }

                    if (isInteger(args[2])) {
                        int timer = Integer.parseInt(args[2]);
                        startEventTimer(timer, sender);
                        sender.sendMessage(ChatColor.AQUA + "Event timer started for " + timer + " minutes.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "The third parameter needs to be a number");
                    }
                } else if (args[1].equalsIgnoreCase("end")) {
                    eventTimerRunning = false;
                    for (Player player : eventPlayers) {
                        player.setLevel(0);
                    }
                    sender.sendMessage(ChatColor.AQUA + "Event timer stopped");
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid parameter");
                }
                break;

            case "teleport":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Invalid parameters for teleport");
                    break;
                }
                if (args[1].equalsIgnoreCase("nation")) {
                    teleportNation(args, sender);
                } else {
                    teleportPlayers(args, sender);
                }
                break;

            case "trackkills":
                if (args.length > 1) {
                    String subCommand = args[1].toLowerCase();

                    switch (subCommand) {
                        case "start":
                            if (!trackingKills) {
                                if (!getEventNations().isEmpty()) {
                                    trackKills();
                                    sender.sendMessage(ChatColor.AQUA + "Kills tracking started");
                                } else {
                                    sender.sendMessage(ChatColor.RED + "No event nations available to track kills.");
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "Kills are already being tracked");
                            }
                            break;

                        case "end":
                            if (trackingKills) {
                                trackingKills = false;
                                sender.sendMessage(ChatColor.AQUA + "---KILL TRACKING RESULTS---");
                                for (Nation nation : nationKills.keySet()) {
                                    sender.sendMessage(ChatColor.AQUA + nation.getName() + " - " + nationKills.get(nation).toString());
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "Kills tracking is not currently active.");
                            }
                            break;

                        default:
                            sender.sendMessage(ChatColor.RED + "Invalid sub-command. Use 'start' or 'end'.");
                            break;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /event trackkills <start|end>");
                    break;
                }
                break;



            case "getplayers":

                if (eventPlayers.isEmpty()){
                    sender.sendMessage(ChatColor.RED + "The event list is empty");
                }

                for (Player player : eventPlayers){
                    sender.sendMessage(ChatColor.AQUA + "- " + ChatColor.AQUA + player.getName());
                }
                break;

            case "permadeath":

                if (args.length > 1) {
                    String subCommand = args[1].toLowerCase();

                    switch (subCommand) {
                        case "enable":
                            if (!permaDeath) {
                                permaDeath = true;
                                sender.sendMessage(ChatColor.DARK_AQUA + "PermaDeath is now enabled");
                                for (Player player : eventPlayers){
                                    player.playSound(player.getLocation(),Sound.ENTITY_WITHER_SPAWN,1f,1f);
                                }
                            } else {
                                sender.sendMessage(ChatColor.RED + "PermaDeath is already enabled");
                            }
                            break;

                        case "disable":
                            if (permaDeath) {
                                permaDeath = false;
                                sender.sendMessage(ChatColor.DARK_AQUA + "PermaDeath is now disabled");
                            } else {
                                sender.sendMessage(ChatColor.RED + "PermaDeath is already disabled");
                            }
                            break;

                        default:
                            sender.sendMessage(ChatColor.RED + "Invalid sub-command. Use 'enable' or 'disable'.");
                            break;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /event permaDeath <enable|disable>");
                    break;
                }
                break;


            default:
                sender.sendMessage(ChatColor.RED + "Unknown command");
                showCommands(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String alias, String[] args) {
        if (!commandSender.isOp()) {
            return Collections.emptyList();
        }

        List<String> availableCommands = new ArrayList<>();

        List<String> eventAdmins = plugin.getConfig().getStringList("eventAdmins");

        if (!(commandSender instanceof Player)){
            return Collections.emptyList();
        }

        Player sender = (Player) commandSender;

        if (!eventAdmins.contains(sender.getName()) && !sender.isOp()){
            return Collections.emptyList();
        }

        if (args.length == 1) { // Adds all the sub-commands to tab to autocomplete
            availableCommands.add("help");
            availableCommands.add("add");
            availableCommands.add("remove");
            availableCommands.add("clear");
            availableCommands.add("timer");
            availableCommands.add("trackKills");
            availableCommands.add("teleport");
            availableCommands.add("getPlayers");
            availableCommands.add("permaDeath");

            return availableCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "timer":
                    availableCommands.add("start");
                    availableCommands.add("end");
                    break;

                case "teleport":
                    availableCommands.add("players");
                    availableCommands.add("nation");
                    break;

                case "trackkills":
                    availableCommands.add("start");
                    availableCommands.add("end");
                    break;

                case "permadeath":
                    availableCommands.add("enable");
                    availableCommands.add("disable");
                    break;

                case "add":
                    Bukkit.getOnlinePlayers().forEach(p -> availableCommands.add(p.getName()));
                    break;

                case "remove":
                    eventPlayers.forEach(p -> availableCommands.add(p.getName()));
                    break;
            }

            return availableCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("timer") && args.length == 3 && args[1].equalsIgnoreCase("start")) {
            availableCommands.add("10");
            availableCommands.add("60");
            availableCommands.add("3600");

            return availableCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("teleport") && args[1].equalsIgnoreCase("players")) {
            // Tab completion for player names after "teleport players"
            Bukkit.getOnlinePlayers().forEach(p -> availableCommands.add(p.getName()));

            return availableCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("teleport") && args[1].equalsIgnoreCase("nation") && args.length == 3) {

            getEventNations().forEach(n -> availableCommands.add(n.getName()));

            return availableCommands.stream()
                    .filter(cmd -> cmd.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void addPlayersToEvent(String[] args){
        for (int i = 1; i < args.length; i++) { // Loops through all arguments after 0 so all players

            Player player = Bukkit.getPlayer(args[i]);
            if (player != null && !eventPlayers.contains(player)) {
                eventPlayers.add(player);
                player.sendMessage(ChatColor.DARK_AQUA + "You have been added to the event");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME,2,1);
            }
        }
    }

    private void removePlayersFromEvent(String[] args, Player sender){
        for (int i = 1; i < args.length; i++) { // Loops through all arguments after 0 so all players

            Player player = Bukkit.getPlayer(args[i]);
            if (player != null) {
                eventPlayers.remove(player);
                player.sendMessage(ChatColor.DARK_RED + "You have removed from the event");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY,1,3);

                sender.sendMessage(ChatColor.AQUA + "Removed " + player.getName() + " successfully");
            }
        }
    }

    private void teleportPlayers(String[] args, Player sender){
        for (int i = 2; i < args.length; i++) { // Loops through all arguments after 1 so all players

            Player player = Bukkit.getPlayer(args[i]);
            if (player != null && eventPlayers.contains(player)) {
                player.teleport(sender);
            } else {
                sender.sendMessage(ChatColor.RED + "Player " + args[i] + " is not in the event or is not online");
            }
        }
        sender.sendMessage(ChatColor.AQUA + "Players were teleported successfully");
    }

    private void teleportNation(String[] args, Player sender){
        Nation nation = Nodes.INSTANCE.getNationFromName(args[2]);

        if (nation != null){
            for (Player player : nation.getPlayersOnline()){
                if (eventPlayers.contains(player)){
                    player.teleport(sender);
                }
            }
            sender.sendMessage(ChatColor.AQUA + "Teleported nation successfully");
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid nation name");
        }
    }

    private void startEventTimer(int seconds, Player sender){
        sender.sendMessage(ChatColor.AQUA + "Timer started successfully");

        eventTimerRunning = true;

        for (Player player : eventPlayers) {
            player.setLevel(0);
        }

        new BukkitRunnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (timeLeft <= 0 || !eventTimerRunning) {
                    for (Player player : eventPlayers) {
                        player.sendMessage(ChatColor.AQUA + "The timer has run out");
                        player.playSound(player, Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 1f);
                        player.setLevel(0);
                    }
                    cancel();
                    return;
                }

                for (Player player : eventPlayers) {
                    player.setLevel(timeLeft);
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Runs every second
    }

    private void trackKills(){
        trackingKills = true;
        for (Nation nation : getEventNations()){
            nationKills.put(nation,0);
        }
        for (Player player : eventPlayers){
            player.sendMessage(ChatColor.DARK_AQUA + "Kill tracking has begun");
            player.playSound(player.getLocation(),Sound.ITEM_GOAT_HORN_SOUND_1,1f,1f);
        }
    }

    private void showCommands(Player sender){
        sender.sendMessage(ChatColor.AQUA + "add : Adds a player to the event list");
        sender.sendMessage(ChatColor.AQUA + "remove : Removes a player from the event list");
        sender.sendMessage(ChatColor.AQUA + "clear : Removes everyone from the event list");
        sender.sendMessage(ChatColor.AQUA + "getPlayers : Shows you all the players currently in the event");
        sender.sendMessage(ChatColor.AQUA + "timer : Controls the event timer");
    }

    /**
     * @return gets the list of all nations in an event
     */
    public List<Nation> getEventNations(){
        List<Nation> nationList = new ArrayList<>();
        for (Player player : eventPlayers) {
            Nation nation = Objects.requireNonNull(Nodes.INSTANCE.getResident(player)).getNation();
            if (!nationList.contains(nation)) {
                nationList.add(nation);
            }
        }
        return nationList;
    }

    /**
     * @return The list of all players in the event
     */
    public List<Player> getEventList(){
        return eventPlayers;
    }

    /**
     * @param player Adds the player to the event
     */
    public void addPlayerToEvent(Player player){
        eventPlayers.add(player);
    }

    /**
     * @param player Adds the player to the event
     */
    public void removePlayerFromEvent(Player player){
        eventPlayers.remove(player);
    }
}