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
	private static LuckPermsApi luckPermsApi;

	private PlayerStorage playerStorage;
	private PlayerStatusAPI api;

	@Override
	public void onEnable() {
		instance = this;

		saveDefaultConfig();

		/* Check for LuckPerms */
		if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
			warning("LuckPerms required");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		if (getConfig().getBoolean("storage.redis.use")) {
			playerStorage = new RedisPlayerStorage(getConfig().getConfigurationSection("storage.redis"));
		}

		api = new PlayerStatusAPI();

		getServer().getMessenger().registerOutgoingPluginChannel(this, "RedisBungee");
		getServer().getMessenger().registerIncomingPluginChannel(this, "RedisBungee", api);
		getServer().getScheduler().runTaskAsynchronously(this, () -> {
			try {
				List<String> vanished = api.getVanished().call();
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

		RegisteredServiceProvider<LuckPermsApi> provider = Bukkit.getServicesManager().getRegistration(LuckPermsApi.class);
		if (provider != null) {
			luckPermsApi = provider.getProvider();
		}
	}

	@Override
	public void onDisable() {
		getServer().getMessenger().unregisterOutgoingPluginChannel(this, "RedisBungee");
		getServer().getMessenger().unregisterIncomingPluginChannel(this, "RedisBungee", api);
		getServer().getScheduler().cancelTasks(this);

		api.close();
		playerStorage.close();
	}

	public PlayerStorage getPlayerStorage() {
		return playerStorage;
	}

	public PlayerStatusAPI getAPI() {
		return api;
	}

	public static PlayerStatusPlugin getInstance() {
		return instance;
	}

	public static LuckPermsApi getLuckPermsApi() {
		return luckPermsApi;
	}

	/* Logging */
	public static void info(String message) {
		instance.getLogger().info(message);
	}

	public static void warning(String message) {
		instance.getLogger().warning(message);
	}

}
