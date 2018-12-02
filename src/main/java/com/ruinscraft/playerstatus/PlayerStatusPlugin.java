package com.ruinscraft.playerstatus;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.ruinscraft.playerstatus.commands.ListCommand;
import com.ruinscraft.playerstatus.commands.VanishCommand;
import com.ruinscraft.playerstatus.storage.PlayerStorage;
import com.ruinscraft.playerstatus.storage.RedisPlayerStorage;

import me.lucko.luckperms.api.LuckPermsApi;

public class PlayerStatusPlugin extends JavaPlugin {

	private static PlayerStatusPlugin instance;
	private static PlayerStatusAPI api;
	private static LuckPermsApi luckPermsApi;

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
		
		RegisteredServiceProvider<LuckPermsApi> provider = Bukkit.getServicesManager().getRegistration(LuckPermsApi.class);
		if (provider != null) {
		    luckPermsApi = provider.getProvider();
		}

		getServer().getMessenger().registerOutgoingPluginChannel(this, "RedisBungee");
		getServer().getMessenger().registerIncomingPluginChannel(this, "RedisBungee", api);

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

		getServer().getPluginManager().registerEvents(new JoinListener(), this);

		getCommand("list").setExecutor(new ListCommand());
		getCommand("vanish").setExecutor(new VanishCommand());
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
	
	public static LuckPermsApi getLuckPermsApi() {
		return luckPermsApi;
	}

}
