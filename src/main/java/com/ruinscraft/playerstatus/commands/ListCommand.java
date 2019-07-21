package com.ruinscraft.playerstatus.commands;

import com.google.common.collect.Multimap;
import com.ruinscraft.playerstatus.PlayerStatusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCommand implements CommandExecutor {

    private static final int MAX_PLAYERS_IN_LIST_PER_SERVER = 35;

    /* Includes "Staff online" as a key */
    private static Map<String, String> concatListView = new HashMap<>();
    private static Map<String, String> fullListView = new HashMap<>();

    public ListCommand() {
        PlayerStatusPlugin.get().getServer().getScheduler()
                .runTaskTimerAsynchronously(PlayerStatusPlugin.get(), new ListCacheUpdater(), 20L, 40L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean showAll = label.toLowerCase().equals("listeveryonewhoisontheserver");
        Multimap<String, String> players = PlayerStatusPlugin.get().getAPI().getOnline();
        for (String server : players.keySet()) {
            /* Check if the player has permission to view the server */
            if (!players.get(server).contains(sender.getName()) // allow if they are currently on the server
                    && !sender.hasPermission("slashserver." + server)) {
                continue;
            }

            if (showAll) {
                sender.sendMessage(fullListView.get(server));
            } else {
                sender.sendMessage(concatListView.get(server));
            }
        }
        sender.sendMessage(concatListView.get("Staff online"));
        sender.sendMessage(ChatColor.GOLD + "Total players online: " + ChatColor.YELLOW + PlayerStatusPlugin.get().getAPI().getOnlyPlayers().size());
        return true;
    }

    private static final class ListCacheUpdater implements Runnable {
        @Override
        public void run() {
            if (PlayerStatusPlugin.get() == null
                    || !PlayerStatusPlugin.get().isEnabled()
                    || Bukkit.getOnlinePlayers().size() < 1) {
                return;
            }

            Multimap<String, String> players = PlayerStatusPlugin.get().getAPI().getOnline();

            if (players == null || players.isEmpty()) {
                return;
            }

            List<String> staffOnline = new ArrayList<>();

            main:
            for (String server : players.keySet()) {
                String serverName = ChatColor.GOLD + server + ChatColor.YELLOW + " (" + players.get(server).size() + ")" + ChatColor.GOLD + ": ";
                List<String> concatServerPlayers = new ArrayList<>();
                List<String> allServerPlayers = new ArrayList<>();

                players:
                for (String username : players.get(server)) {
                    /* Formatting */
                    String group = null;
                    try {
                        group = PlayerStatusPlugin.get().getPlayerStorage().getGroup(username).call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (group == null) {
                        group = "unknown";
                    }

                    formatting:
                    switch (group) {
                        case "owner":
                            username = ChatColor.DARK_RED + username;
                            staffOnline.add(username);
                            break formatting;
                        case "admin":
                            username = ChatColor.GOLD + username;
                            staffOnline.add(username);
                            break formatting;
                        case "moderator":
                            username = ChatColor.BLUE + username;
                            staffOnline.add(username);
                            break formatting;
                        case "helper":
                            username = ChatColor.AQUA + username;
                            staffOnline.add(username);
                            break;
                        case "builder":
                            username = ChatColor.GREEN + username;
                            break formatting;
                        default:
                            if (group.startsWith("vip") || group.contains("sponsor")) {
                                username = ChatColor.DARK_PURPLE + username;
                            } else {
                                username = ChatColor.GRAY + username;
                            }
                            break formatting;
                    }
                    /* End formatting */

                    /* Generate lists */
                    if (concatServerPlayers.size() < MAX_PLAYERS_IN_LIST_PER_SERVER) {
                        concatServerPlayers.add(username);
                    } else if (concatServerPlayers.size() == MAX_PLAYERS_IN_LIST_PER_SERVER) {
                        concatServerPlayers.add(ChatColor.GRAY + "and " + (players.get(server).size() - MAX_PLAYERS_IN_LIST_PER_SERVER) + " more");
                    }

                    allServerPlayers.add(username);
                }

                concatListView.put(server, serverName + String.join(ChatColor.GRAY + ", ", concatServerPlayers));
                concatListView.put("Staff online", ChatColor.GOLD + "Staff online: " + ChatColor.YELLOW + "(" + staffOnline.size() + ")" + ChatColor.GOLD + ": " + String.join(ChatColor.GRAY + ", ", staffOnline));
                fullListView.put(server, serverName + String.join(ChatColor.GRAY + ", ", allServerPlayers));
                fullListView.put("Staff online", ChatColor.GOLD + "Staff online: " + ChatColor.YELLOW + "(" + staffOnline.size() + ")" + ChatColor.GOLD + ": " + String.join(ChatColor.GRAY + ", ", staffOnline));
            }
        }
    }

}
