package com.ruinscraft.playerstatus;

import org.bukkit.plugin.java.JavaPlugin;

public class PlayerStatusPlugin extends JavaPlugin {

	private static PlayerStatusPlugin instance;
	private static PlayerStatusAPI api;

	@Override
	public void onEnable() {
		instance = this;
		api = new PlayerStatusAPI();

		getServer().getMessenger().registerOutgoingPluginChannel(this, "RedisBungee");
		getServer().getMessenger().registerIncomingPluginChannel(this, "RedisBungee", api);
		
		getCommand("list").setExecutor(new ListCommand());
	}

	@Override
	public void onDisable() {
		try {
			api.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		getServer().getScheduler().cancelTasks(this);
	}

	public static PlayerStatusPlugin getInstance() {
		return instance;
	}

	public static PlayerStatusAPI getAPI() {
		return api;
	}

}
