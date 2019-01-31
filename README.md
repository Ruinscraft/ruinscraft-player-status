# ruinscraft-player-status
A Bukkit plugin to interface with RedisBungee via Plugin Messaging which provides information about which players are online on a BungeeCord proxy.

# Why?
ruinscraft-player-status is being used as a dependency for various projects such as ruinscraft-chat. It provides useful functionality such as being able to tell if a player is online when you privately message them (over a BungeeCord proxy).

ruinscraft-player-status also provides a player list command. This is specifically implemented for Ruinscraft but can be altered with ease. With a list command also comes a vanish command. The vanish command will remove you from the player listing and also hide your character in game via Player#hidePlayer in Bukkit.
