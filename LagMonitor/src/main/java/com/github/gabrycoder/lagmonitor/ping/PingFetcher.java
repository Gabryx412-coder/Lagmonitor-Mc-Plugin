package com.github.gabrycoder.lagmonitor.ping;

import org.bukkit.entity.Player;

public interface PingFetcher {

    boolean isAvailable();

    int getPing(Player player);
}
