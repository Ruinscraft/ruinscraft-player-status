package com.ruinscraft.playerstatus;

public class PlayerStatus {

	public static final long ONLINE		= 0L;
	public static final long OFFLINE	= -1L;
	public static final long VANISHED	= -2L;
	
	private long value;
	
	public long getValue() {
		return value;
	}
	
	public void setValue(long value) {
		this.value = value;
	}
	
}
