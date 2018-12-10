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

				int maxPlayers = 25;

				for (String username : players.get(server)) {
					if (maxPlayers-- == 1) {
						serverPlayers.add(ChatColor.GRAY + "and " + (players.get(server).size() - 25) + " more");
						break;
					}

					String group = null;
					try {
						group = PlayerStatusPlugin.getInstance().getPlayerStorage().getGroup(username).call();
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (group != null && !group.equals("member")) {
						if (group.equals("owner")) {
							serverPlayers.add(ChatColor.DARK_RED + username);
							staffOnline.add(ChatColor.DARK_RED + username);
						}

						else if (group.equals("admin")) {
							serverPlayers.add(ChatColor.GOLD + username);
							staffOnline.add(ChatColor.GOLD + username);
						}

						else if (group.equals("moderator")) {
							serverPlayers.add(ChatColor.BLUE + username);
							staffOnline.add(ChatColor.BLUE + username);
						}

						else if (group.equals("helper")) {
							serverPlayers.add(ChatColor.AQUA + username);
							staffOnline.add(ChatColor.AQUA + username);
						}

						else if (group.equals("builder")) {
							serverPlayers.add(ChatColor.GREEN + username);
						}

						else if (group.startsWith("vip")) {
							serverPlayers.add(ChatColor.DARK_PURPLE + username);
						} 
					} else {
						serverPlayers.add(ChatColor.GRAY + username);
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
