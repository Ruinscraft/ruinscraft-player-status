package com.ruinscraft.playerstatus;

import org.bukkit.plugin.java.JavaPlugin;

import com.ruinscraft.playerstatus.storage.PlayerStorage;
import com.ruinscraft.playerstatus.storage.redis.RedisPlayerStorage;

public class PlayerStatusPlugin extends JavaPlugin {

	private static PlayerStatusPlugin instance;
	private static PlayerStatusAPI api;

	private PlayerStorage playerStorage;
	
	@Override
	public void onEnable() {
		instance = this;
		
		api = new PlayerStatusAPI();

		saveDefaultConfig();
		
		if (getConfig().getBoolean("storage.redis.use")) {
			playerStorage = new RedisPlayerStorage(getConfig().getConfigurationSection("storage.redis"));
		}
		
		getServer().getMessenger().registerOutgoingPluginChannel(this, "RedisBungee");
		getServer().getMessenger().registerIncomingPluginChannel(this, "RedisBungee", api);
		
		getCommand("list").setExecutor(new ListCommand());
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

	public static PlayerStatusAPI getAPI() {
		return api;
	}

}
