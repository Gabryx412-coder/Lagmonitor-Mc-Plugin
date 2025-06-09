package com.github.gabrycoder.lagmonitor.listener;

import com.github.gabrycoder.lagmonitor.graph.GraphRenderer;
import com.github.gabrycoder.lagmonitor.util.LagUtils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

public class GraphListener implements Listener {

    private final boolean mainHandSupported;

    public GraphListener() {
        boolean mainHandMethodEx = false;
        try {
            MethodType type = MethodType.methodType(ItemStack.class);
            MethodHandles.publicLookup().findVirtual(PlayerInventory.class, "getItemInMainHand", type);
            mainHandMethodEx = true;
        } catch (ReflectiveOperationException notFoundEx) {
            //default to false
        }

        this.mainHandSupported = mainHandMethodEx;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent clickEvent) {
        Player player = clickEvent.getPlayer();
        PlayerInventory inventory = player.getInventory();

        ItemStack mainHandItem;
        if (mainHandSupported) {
            mainHandItem = inventory.getItemInMainHand();
        } else {
            mainHandItem = inventory.getItemInHand();
        }

        if (isOurGraph(mainHandItem)) {
            inventory.setItemInMainHand(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent dropItemEvent) {
        Item itemDrop = dropItemEvent.getItemDrop();
        ItemStack mapItem = itemDrop.getItemStack();
        if (isOurGraph(mapItem)) {
            mapItem.setAmount(0);
        }
    }

    private boolean isOurGraph(ItemStack item) {
        if (!LagUtils.isFilledMapSupported()) {
            return isOurGraphLegacy(item);
        }

        if (item.getType() != Material.FILLED_MAP) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof MapMeta)) {
            return false;
        }

        MapMeta mapMeta = (MapMeta) meta;
        MapView mapView = mapMeta.getMapView();
        return mapView != null && isOurRenderer(mapView);
    }

    private boolean isOurGraphLegacy(ItemStack mapItem) {
        if (mapItem.getType() != Material.MAP)
            return false;

        short mapId = mapItem.getDurability();
        MapView mapView = Bukkit.getMap(mapId);
        return mapView != null && isOurRenderer(mapView);
    }

    private boolean isOurRenderer(MapView mapView) {
        return mapView.getRenderers().stream()
                .anyMatch(GraphRenderer.class::isInstance);
    }
}
