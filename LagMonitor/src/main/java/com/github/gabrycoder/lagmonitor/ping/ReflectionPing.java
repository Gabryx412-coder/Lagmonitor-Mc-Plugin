package com.github.gabrycoder.lagmonitor.ping;

import com.github.gabrycoder.lagmonitor.LagMonitor;
import com.github.gabrycoder.lagmonitor.traffic.Reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ReflectionPing implements PingFetcher {

    private static final MethodHandle pingFromPlayerHandle;

    static {
        MethodHandle localPing = null;
        Class<?> craftPlayerClass = Reflection.getCraftBukkitClass("entity.CraftPlayer");
        String playerClazz = "EntityPlayer";
        Class<?> entityPlayer = Reflection.getMinecraftClass(playerClazz, "level." + playerClazz);

        Lookup lookup = MethodHandles.publicLookup();
        try {
            MethodType type = MethodType.methodType(entityPlayer);
            MethodHandle getHandle = lookup.findVirtual(craftPlayerClass, "getHandle", type);
            MethodHandle pingField = lookup.findGetter(entityPlayer, "ping", Integer.TYPE);

            // combine the handles to invoke it only once
            // *getPing(getHandle*) -> add the result of getHandle to the next getPing call
            // a call to this handle will get the ping from a player instance
            localPing = MethodHandles.collectArguments(pingField, 0, getHandle)
                    // allow interface with invokeExact
                    .asType(MethodType.methodType(int.class, Player.class));
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException reflectiveEx) {
            Logger logger = JavaPlugin.getPlugin(LagMonitor.class).getLogger();
            logger.log(Level.WARNING, "Cannot find ping field/method", reflectiveEx);
        }

        pingFromPlayerHandle = localPing;
    }

    @Override
    public boolean isAvailable() {
        return pingFromPlayerHandle != null;
    }

    @Override
    public int getPing(Player player) {
        try {
            return (int) pingFromPlayerHandle.invokeExact(player);
        } catch (Exception ex) {
            return -1;
        } catch (Throwable throwable) {
            throw (Error) throwable;
        }
    }
}
