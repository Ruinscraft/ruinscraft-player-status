package com.ruinscraft.playerstatus.commands;

import com.google.common.collect.Multimap;
import com.ruinscraft.playerstatus.PlayerStatusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MBListCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Multimap<String, String> players = PlayerStatusPlugin.get().getAPI().getOnline();

        List<String> mbers = new ArrayList<>();

        for (String server : players.keySet()) {
            for (String username : players.get(server)) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
                boolean is_mber = PlayerStatusPlugin.getVaultPerms().playerHas(Bukkit.getWorlds().get(0).getName(), offlinePlayer, "group.mb");

                if (is_mber) {
                    mbers.add(username);
                }
            }
        }

        sender.sendMessage(ChatColor.GREEN + "[MBLIST]" + ChatColor.RED + "(" + mbers.size() + ") " + String.join(", ", mbers));

        return true;
    }

}
