package com.ruinscraft.playerstatus;

import java.util.List;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.ruinscraft.playerstatus.commands.ListCommand;
import com.ruinscraft.playerstatus.commands.VanishCommand;
import com.ruinscraft.playerstatus.storage.PlayerStorage;
import com.ruinscraft.playerstatus.storage.RedisPlayerStorage;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;

public class PlayerStatusPlugin extends JavaPlugin {

	private static PlayerStatusPlugin instance;
	private static PlayerStatusAPI api;
	private static Chat vaultChat;
	private static Permission vaultPermissions;
	
	private PlayerStorage playerStorage;

	@Override
	public void onEnable() {
		instance = this;

		/* Check for Vault */
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			warning("Vault required");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		saveDefaultConfig();

		if (getConfig().getBoolean("storage.redis.use")) {
			playerStorage = new RedisPlayerStorage(getConfig().getConfigurationSection("storage.redis"));
		}

		api = new PlayerStatusAPI();
		
		getServer().getMessenger().registerOutgoingPluginChannel(this, "RedisBungee");
		getServer().getMessenger().registerIncomingPluginChannel(this, "RedisBungee", api);

		getServer().getPluginManager().registerEvents(new JoinListener(), this);
		
		getCommand("list").setExecutor(new ListCommand());
		getCommand("vanish").setExecutor(new VanishCommand());
		
		/* Setup Vault Chat */
		RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(Chat.class);
		if (chatProvider != null) {
			vaultChat = chatProvider.getProvider();
		}
		/* Setup Vault Permissions */
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		vaultPermissions = rsp.getProvider();
		
		getServer().getScheduler().runTaskAsynchronously(this, () -> {
			try {
				List<String> vanished = PlayerStatusPlugin.getAPI().getVanished().call();
				getServer().getScheduler().runTask(PlayerStatusPlugin.this, () -> {
					JoinListener.handleVanished(vanished);
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void onDisable() {
		api.close();
		playerStorage.close();

		getServer().getScheduler().cancelTasks(this);
	}

	public PlayerStorage getPlayerStorage() {
		return playerStorage;
	}

	public static PlayerStatusPlugin getInstance() {
		return instance;
	}

	public static void info(String message) {
		instance.getLogger().info(message);
	}

	public static void warning(String message) {
		instance.getLogger().warning(message);
	}

	public static PlayerStatusAPI getAPI() {
		return api;
	}
	
	public static Chat getVaultChat() {
		return vaultChat;
	}
	
	public static Permission getVaultPermissions() {
		return vaultPermissions;
	}

}
