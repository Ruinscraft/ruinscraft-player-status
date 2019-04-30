package com.ruinscraft.playerstatus.storage;

import java.util.List;
import java.util.concurrent.Callable;

public interface PlayerStorage extends AutoCloseable {

    Callable<Void> setVanished(String username, boolean vanished);

    Callable<Boolean> isVanished(String username);

    Callable<List<String>> getVanished();

    Callable<Void> setGroup(String username, String group);

    Callable<String> getGroup(String username);

    @Override
    default void close() {
    }

}
