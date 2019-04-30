package com.ruinscraft.playerstatus;

import com.ruinscraft.playerstatus.commands.ListCommand;
import com.ruinscraft.playerstatus.commands.VanishCommand;
import com.ruinscraft.playerstatus.storage.PlayerStorage;
import com.ruinscraft.playerstatus.storage.RedisPlayerStorage;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PlayerStatusPlugin extends JavaPlugin {

    private PlayerStorage playerStorage;
    private PlayerStatusAPI api;

    @Override
    public void onEnable() {
        singleton = this;

        saveDefaultConfig();

        /* Setup Vault */
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            warning("Vault required");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        else {
            if (!setupVault(this)) {
                warning("Error setting up Vault");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        /* Setup player storage */
        if (getConfig().getBoolean("storage.redis.use")) {
            playerStorage = new RedisPlayerStorage(getConfig().getConfigurationSection("storage.redis"));
        }

        if (playerStorage == null) {
            warning("Player storage not configured");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        api = new PlayerStatusAPI();

        /* Register plugin channels */
        getServer().getMessenger().registerOutgoingPluginChannel(this, "RedisBungee");
        getServer().getMessenger().registerIncomingPluginChannel(this, "RedisBungee", api);

        /* Handle vanished players in case of a reload */
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

        /* Register listeners */
        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        /* Register commands */
        getCommand("list").setExecutor(new ListCommand());
        getCommand("vanish").setExecutor(new VanishCommand());
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "RedisBungee");
        getServer().getMessenger().unregisterIncomingPluginChannel(this, "RedisBungee", api);
        getServer().getScheduler().cancelTasks(this);

        api.close();
        playerStorage.close();

        singleton = null;
    }

    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    public PlayerStatusAPI getAPI() {
        return api;
    }

    /* static =========================== */
    private static PlayerStatusPlugin singleton;
    private static Permission vaultPerms;

    private static boolean setupVault(Plugin plugin) {
        RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        return (vaultPerms = rsp.getProvider()) != null;
    }

    public static Permission getVaultPerms() {
        return vaultPerms;
    }

    public static PlayerStatusPlugin get() {
        return singleton;
    }

    public static void warning(String message) {
        singleton.getLogger().warning(message);
    }
    /* static =========================== */

}
