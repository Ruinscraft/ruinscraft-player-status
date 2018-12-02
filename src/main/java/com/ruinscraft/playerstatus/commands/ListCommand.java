package com.ruinscraft.playerstatus.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.google.common.collect.Multimap;
import com.ruinscraft.playerstatus.PlayerStatusPlugin;

import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.manager.UserManager;

public class ListCommand implements CommandExecutor {

	private static Map<String, String> serverView = new HashMap<>();
	private static List<String> staffView = new ArrayList<>();

	public ListCommand() {
		PlayerStatusPlugin.getInstance().getServer().getScheduler().runTaskTimerAsynchronously(PlayerStatusPlugin.getInstance(), () -> {
			if (PlayerStatusPlugin.getInstance() == null) {
				return;
			}
			
			staffView.clear();
			
			/* This is actually run sync, it is always cached */
			Multimap<String, String> players = PlayerStatusPlugin.getAPI().getOnline();
			
			UserManager userManager = PlayerStatusPlugin.getLuckPermsApi().getUserManager();
			
			for (String server : players.keySet()) {
				String serverName = ChatColor.GOLD + server + ChatColor.YELLOW + " (" + players.get(server).size() + ")" + ChatColor.GOLD + ": ";
				List<String> serverPlayers = new ArrayList<>();

				int maxPlayers = 25;

				for (String player : players.get(server)) {
					if (maxPlayers-- == 1) {
						serverPlayers.add(ChatColor.GRAY + "and " + (players.get(server).size() - 25) + " more");
						break;
					}

					UUID playerUUID = userManager.lookupUuid(player).join();
					User lpUser = userManager.loadUser(playerUUID).join();

					if (lpUser.getPrimaryGroup().equals("owner")) {
						serverPlayers.add(ChatColor.DARK_RED + player);
						staffView.add(ChatColor.DARK_RED + player);
					}

					else if (lpUser.getPrimaryGroup().equals("admin")) {
						serverPlayers.add(ChatColor.GOLD + player);
						staffView.add(ChatColor.GOLD + player);
					}

					else if (lpUser.getPrimaryGroup().equals("moderator")) {
						serverPlayers.add(ChatColor.BLUE + player);
						staffView.add(ChatColor.BLUE + player);
					}

					else if (lpUser.getPrimaryGroup().equals("helper")) {
						serverPlayers.add(ChatColor.AQUA + player);
						staffView.add(ChatColor.AQUA + player);
					}

					else if (lpUser.getPrimaryGroup().equals("builder")) {
						serverPlayers.add(ChatColor.GREEN + player);
					}

					else if (lpUser.getPrimaryGroup().contains("vip")) {
						serverPlayers.add(ChatColor.DARK_PURPLE + player);
					} 

					else {
						serverPlayers.add(ChatColor.GRAY + player);
					}
				}

				serverView.put(server, serverName + String.join(ChatColor.GRAY + ", ", serverPlayers));
			}
		}, 20L, 20L);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Multimap<String, String> players = PlayerStatusPlugin.getAPI().getOnline();
		for (String server : players.keySet()) {
			if (!players.get(server).contains(sender.getName())) {
				if (!sender.hasPermission("slashserver." + server)) {
					continue;
				}
			}

			sender.sendMessage(serverView.get(server));
		}
		sender.sendMessage(ChatColor.GOLD + "Staff online " + ChatColor.YELLOW + "(" + staffView.size() + ")" + ChatColor.GOLD + ": " + String.join(ChatColor.GRAY + ", ", staffView));
		sender.sendMessage(ChatColor.GOLD + "Total players online: " + ChatColor.YELLOW + PlayerStatusPlugin.getAPI().getOnlyPlayers().size());
		return true;
	}

}
