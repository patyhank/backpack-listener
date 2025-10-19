package net.patyhank.bplugin.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerInventoryOpenEvent extends PlayerEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PlayerInventoryOpenEvent(Player player) {
        super(player);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
