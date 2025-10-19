package net.patyhank.bplugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BackpackListenerPlugin extends JavaPlugin implements Listener {
    private static InventoryListener inventoryListener = new InventoryListener();
    public static BackpackListenerPlugin instance;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

        instance = this;
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(inventoryListener, PacketListenerPriority.HIGHEST);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    @EventHandler
    public void onPlayerJoined(PlayerJoinEvent event) {
        getServer().getScheduler().runTaskLater(this, () -> {
            if (event.getPlayer() != null && event.getPlayer().isOnline()) {
                inventoryListener.barrierCursor(event.getPlayer());
                inventoryListener.addBarrierRecipe(event.getPlayer());
            }
        }, 10L);
    }

}
