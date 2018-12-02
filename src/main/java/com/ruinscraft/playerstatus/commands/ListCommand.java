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

	private static Map<String, String> serverView = new HashMap<>();
	private static List<String> staffView = new ArrayList<>();

	public ListCommand() {
		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskTimerAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			/* This is actually run sync, it is always cached */
			Multimap<String, String> players = PlayerStatusPlugin.getAPI().getOnline();

			staffView.clear();
			
			for (String server : players.keySet()) {
				String serverName = ChatColor.GOLD + server + ChatColor.YELLOW + " (" + players.get(server).size() + ")" + ChatColor.GOLD + ": ";
				List<String> serverPlayers = new ArrayList<>();

				int maxPlayers = 25;

				for (String player : players.get(server)) {
					if (maxPlayers-- == 1) {
						serverPlayers.add(ChatColor.GRAY + "and " + (players.get(server).size() - 25) + " more");
						break;
					}

					if (PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.owner")) {
						serverPlayers.add(ChatColor.DARK_RED + player);
						staffView.add(ChatColor.DARK_RED + player);
					}

					else if (PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.admin")) {
						serverPlayers.add(ChatColor.GOLD + player);
						staffView.add(ChatColor.GOLD + player);
					}

					else if (PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.mod")) {
						serverPlayers.add(ChatColor.BLUE + player);
						staffView.add(ChatColor.BLUE + player);
					}

					else if (PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.helper")) {
						serverPlayers.add(ChatColor.AQUA + player);
						staffView.add(ChatColor.AQUA + player);
					}

					else if (PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.builder")) {
						serverPlayers.add(ChatColor.GREEN + player);
					}

					else if (PlayerStatusPlugin.getVaultPermissions().playerHas(null, Bukkit.getOfflinePlayer(player), "group.vip1")) {
						serverPlayers.add(ChatColor.DARK_PURPLE + player);
					} 

					else {
						serverPlayers.add(ChatColor.GRAY + player);
					}
				}

				serverView.put(server, serverName + String.join(", ", serverPlayers));
			}
		}, 20L, 20L);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Multimap<String, String> players = PlayerStatusPlugin.getAPI().getOnline();
		for (String server : players.keySet()) {
			if (!sender.hasPermission("server." + server)) {
				continue;
			}
			sender.sendMessage(serverView.get(server));
		}
		sender.sendMessage(ChatColor.GOLD + "Staff online " + ChatColor.YELLOW + "(" + staffView.size() + ")" + ChatColor.GOLD + ": " + String.join(", ", staffView));
		sender.sendMessage(ChatColor.GOLD + "Total players online: " + ChatColor.YELLOW + PlayerStatusPlugin.getAPI().getOnlyPlayers().size());
		return true;
	}

}
