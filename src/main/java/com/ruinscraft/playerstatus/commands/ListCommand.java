package com.ruinscraft.playerstatus.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.google.common.collect.Multimap;
import com.ruinscraft.playerstatus.PlayerStatusPlugin;

public class ListCommand implements CommandExecutor {

	private static final int MAX_PLAYERS_IN_LIST_PER_SERVER = 25;

	/* Includes "Staff online" */
	private static Map<String, String> currentListView = new HashMap<>();

	public ListCommand() {
		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskTimerAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			if (PlayerStatusPlugin.getInstance() == null) {
				return;
			}

			if (!PlayerStatusPlugin.getInstance().isEnabled()) {
				return;
			}

			if (Bukkit.getOnlinePlayers().size() < 1) {
				return;
			}

			Multimap<String, String> players = PlayerStatusPlugin.getInstance().getAPI().getOnline();

			if (players == null || players.isEmpty()) {
				return;
			}

			List<String> staffOnline = new ArrayList<>();

			for (String server : players.keySet()) {
				String serverName = ChatColor.GOLD + server + ChatColor.YELLOW + " (" + players.get(server).size() + ")" + ChatColor.GOLD + ": ";
				List<String> serverPlayers = new ArrayList<>();

				for (String username : players.get(server)) {
					/* Formatting */
					String group = null;
					try {
						group = PlayerStatusPlugin.getInstance().getPlayerStorage().getGroup(username).call();
					} catch (Exception e) {
						e.printStackTrace();
					}

					switch (group) {
					case "owner":
						username = ChatColor.DARK_RED + username;
						staffOnline.add(username);
						break;
					case "admin":
						username = ChatColor.GOLD + username;
						staffOnline.add(username);
						break;
					case "moderator":
						username = ChatColor.BLUE + username;
						staffOnline.add(username);
						break;
					case "helper":
						username = ChatColor.AQUA + username;
						staffOnline.add(username);
						break;
					case "builder":
						username = ChatColor.GREEN + username;
						break;
					default:
						if (group.startsWith("vip")) {
							username = ChatColor.DARK_PURPLE + username;
						} else {
							username = ChatColor.GRAY + username;
						}
						break;
					}
					/* End formatting */

					/* Generate list */
					if (serverPlayers.size() <= MAX_PLAYERS_IN_LIST_PER_SERVER) {
						serverPlayers.add(username);
					}

					else if (serverPlayers.size() == MAX_PLAYERS_IN_LIST_PER_SERVER) {
						serverPlayers.add(ChatColor.GRAY + "and " + (players.get(server).size() - MAX_PLAYERS_IN_LIST_PER_SERVER) + " more");
					}
				}

				currentListView.put(server, serverName + String.join(ChatColor.GRAY + ", ", serverPlayers));
				currentListView.put("Staff online", ChatColor.GOLD + "Staff online: " + ChatColor.YELLOW + "(" + staffOnline.size() + ")" + ChatColor.GOLD + ": " + String.join(ChatColor.GRAY + ", ", staffOnline));
			}
		}, 40L, 40L);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Multimap<String, String> players = PlayerStatusPlugin.getInstance().getAPI().getOnline();
		for (String server : players.keySet()) {
			if (!players.get(server).contains(sender.getName())) {
				if (!sender.hasPermission("slashserver." + server)) {
					continue;
				}
			}

			sender.sendMessage(currentListView.get(server));
		}
		sender.sendMessage(currentListView.get("Staff online"));
		sender.sendMessage(ChatColor.GOLD + "Total players online: " + ChatColor.YELLOW + PlayerStatusPlugin.getInstance().getAPI().getOnlyPlayers().size());
		return true;
	}

}
