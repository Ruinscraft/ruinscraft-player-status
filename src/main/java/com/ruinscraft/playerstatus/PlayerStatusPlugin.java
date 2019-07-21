package com.ruinscraft.playerstatus;

import com.ruinscraft.playerstatus.commands.ListCommand;
import com.ruinscraft.playerstatus.commands.MBListCommand;
import com.ruinscraft.playerstatus.commands.VanishCommand;
import com.ruinscraft.playerstatus.storage.PlayerStorage;
import com.ruinscraft.playerstatus.storage.RedisPlayerStorage;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
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

        is_112 = Bukkit.getVersion().contains("1.12") ? true : false;

        if (is_112) {
            pluginChannel = "RedisBungee";
        } else {
            pluginChannel = "legacy:redisbungee";
        }

        saveDefaultConfig();

        /* Setup Vault */
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            warning("Vault required");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else {
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
        getServer().getMessenger().registerOutgoingPluginChannel(this, pluginChannel);
        getServer().getMessenger().registerIncomingPluginChannel(this, pluginChannel, api);

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

        VanishCommand vanishCommand = new VanishCommand();

        /* Register listeners */
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(vanishCommand, this);

        /* Register commands */
        getCommand("list").setExecutor(new ListCommand());
        getCommand("vanish").setExecutor(vanishCommand);
        getCommand("mblist").setExecutor(new MBListCommand());
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, pluginChannel);
        getServer().getMessenger().unregisterIncomingPluginChannel(this, pluginChannel, api);
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
    private static boolean is_112;
    private static String pluginChannel;

    private static boolean setupVault(Plugin plugin) {
        RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
        return (vaultPerms = rsp.getProvider()) != null;
    }

    public static Permission getVaultPerms() {
        return vaultPerms;
    }

    public static boolean is_112() {
        return is_112;
    }

    public static String getPluginChannel() {
        return pluginChannel;
    }

    public static PlayerStatusPlugin get() {
        return singleton;
    }

    public static void warning(String message) {
        singleton.getLogger().warning(message);
    }
    /* static =========================== */

}
