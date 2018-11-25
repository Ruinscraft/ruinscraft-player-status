package com.ruinscraft.playerstatus;

public class PlayerStatus {

	public final String username;
	public final long lastSeen;

	public PlayerStatus(String username, long lastSeen) {
		this.username = username;
		this.lastSeen = lastSeen;
	}

	public boolean isOnline() {
		return lastSeen == 0;
	}

	public boolean hasJoined() {
		return lastSeen != -1;
	}

}
