package com.github.gabrycoder.lagmonitor.traffic;

import com.github.gabrycoder.lagmonitor.traffic.Reflection.FieldAccessor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * This is modified version of TinyProtocol from the ProtocolLib authors (dmulloy2 and aadnk). The not relevant things
 * are removed like player channel injection and the serverChannelHandler is modified so we can read the raw input of
 * the incoming and outgoing packets
 *
 * Original can be found here:
 * https://github.com/dmulloy2/ProtocolLib/blob/master/modules/TinyProtocol/src/main/java/com/comphenix/tinyprotocol/TinyProtocol.java
 */
public abstract class TinyProtocol {

    // Looking up ServerConnection
    private static final Class<Object> SERVER_CLASS = (Class<Object>) Reflection.getMinecraftClass("MinecraftServer", "MinecraftServer");
    private static final Class<Object> CONNECTION_CLASS = (Class<Object>) Reflection.getMinecraftClass("ServerConnection", "network.ServerConnection");
    private static final Reflection.MethodInvoker GET_SERVER = Reflection.getMethod("{obc}.CraftServer", "getServer");
    private static final FieldAccessor<Object> GET_CONNECTION = Reflection.getField(SERVER_CLASS, CONNECTION_CLASS, 0);

    // Injected channel handlers
    private final Collection<Channel> serverChannels = new ArrayList<>();
    private ChannelInboundHandlerAdapter serverChannelHandler;

    private volatile boolean closed;
    protected final Plugin plugin;

    /**
     * Construct a new instance of TinyProtocol, and start intercepting packets for all connected clients and future
     * clients.
     * <p>
     * You can construct multiple instances per plugin.
     *
     * @param plugin - the plugin.
     */
    public TinyProtocol(final Plugin plugin) {
        this.plugin = plugin;

        try {
            registerChannelHandler();
        } catch (IllegalArgumentException illegalArgumentException) {
            // Damn you, late bind
            plugin.getLogger().info("[TinyProtocol] Delaying server channel injection due to late bind.");

            // Damn you, late bind
            Bukkit.getScheduler().runTask(plugin, () -> {
                registerChannelHandler();
                plugin.getLogger().info("[TinyProtocol] Late bind injection successful.");
            });
        }
    }

    private void createServerChannelHandler() {
        serverChannelHandler = new ChannelInboundHandlerAdapter() {

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                Channel channel = (Channel) msg;

                channel.pipeline().addLast(new ChannelDuplexHandler() {
                    @Override
                    public void channelRead(ChannelHandlerContext handlerContext, Object object) throws Exception {
                        onChannelRead(handlerContext, object);
                        super.channelRead(handlerContext, object);
                    }

                    @Override
                    public void write(ChannelHandlerContext handlerContext, Object object, ChannelPromise promise)
                            throws Exception {
                        onChannelWrite(handlerContext, object, promise);
                        super.write(handlerContext, object, promise);
                    }
                });

                ctx.fireChannelRead(msg);
            }
        };
    }

    public abstract void onChannelRead(ChannelHandlerContext handlerContext, Object object);

    public abstract void onChannelWrite(ChannelHandlerContext handlerContext, Object object, ChannelPromise promise);

    @SuppressWarnings("unchecked")
    private void registerChannelHandler() {
        Object mcServer = GET_SERVER.invoke(Bukkit.getServer());
        Object serverConnection = GET_CONNECTION.get(mcServer);

        createServerChannelHandler();

        // Find the correct list, or implicitly throw an exception
        boolean looking = true;
        for (int i = 0; looking; i++) {
            List<Object> list = Reflection.getField(serverConnection.getClass(), List.class, i).get(serverConnection);

            for (Object item : list) {
                if (!(item instanceof ChannelFuture)) {
                    break;
                }

                // Channel future that contains the server connection
                Channel serverChannel = ((ChannelFuture) item).channel();

                serverChannels.add(serverChannel);
                serverChannel.pipeline().addFirst(serverChannelHandler);
                looking = false;
            }
        }
    }

    private void unregisterChannelHandler() {
        if (serverChannelHandler == null) {
            return;
        }

        serverChannels.forEach(serverChannel -> {
            // Remove channel handler
            ChannelPipeline pipeline = serverChannel.pipeline();
            serverChannel.eventLoop().execute(new CleanUpTask(pipeline, serverChannelHandler));
        });
    }

    /**
     * Cease listening for packets. This is called automatically when your plugin is disabled.
     */
    public final void close() {
        if (!closed) {
            closed = true;

            unregisterChannelHandler();
        }
    }
}
