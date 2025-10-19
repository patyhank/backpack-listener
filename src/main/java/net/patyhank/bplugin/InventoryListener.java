package net.patyhank.bplugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.book.BookType;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.mapper.MappedEntitySet;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.recipe.RecipeBookSettings;
import com.github.retrooper.packetevents.protocol.recipe.RecipeBookType;
import com.github.retrooper.packetevents.protocol.recipe.RecipeDisplayEntry;
import com.github.retrooper.packetevents.protocol.recipe.RecipeDisplayId;
import com.github.retrooper.packetevents.protocol.recipe.category.RecipeBookCategories;
import com.github.retrooper.packetevents.protocol.recipe.display.ShapedCraftingRecipeDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.EmptySlotDisplay;
import com.github.retrooper.packetevents.protocol.recipe.display.slot.ItemStackSlotDisplay;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSetDisplayedRecipe;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSetRecipeBookState;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import com.google.common.collect.Maps;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import it.unimi.dsi.fastutil.Pair;
import net.patyhank.bplugin.event.PlayerOpenBackpackEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Listener class that handles inventory-related packet events.
 * Manages recipe book settings and handles special barrier item interactions.
 */
public class InventoryListener implements PacketListener {
    /**
     * Map storing recipe book settings (open state and filter state) for each player
     */
    private final Map<UUID, Pair<Boolean, Boolean>> settingsMap = Maps.newConcurrentMap();

    /**
     * Static recipe ID used for barrier recipe identification
     */
    Integer id = -1;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.SET_DISPLAYED_RECIPE) {
            WrapperPlayClientSetDisplayedRecipe packet = new WrapperPlayClientSetDisplayedRecipe(event);
            int recipe = packet.getRecipeId().getId();
            if (recipe != id) {
                return;
            }
            if (removeBarrierRecipe(event.getPlayer())) {
                fallbackCursor(event.getPlayer());
            }
            Bukkit.getScheduler().runTask(BackpackListenerPlugin.instance, () -> Bukkit.getPluginManager().callEvent(new PlayerOpenBackpackEvent(event.getPlayer())));
            event.setCancelled(true);
        }
        if (event.getPacketType() == PacketType.Play.Client.SET_RECIPE_BOOK_STATE) {
            WrapperPlayClientSetRecipeBookState packet = new WrapperPlayClientSetRecipeBookState(event);
            if (packet.getBookType() == BookType.CRAFTING) {
                settingsMap.put(event.getUser().getUUID(), Pair.of(packet.isBookOpen(), packet.isFilterActive()));
            }
        }
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            if (addBarrierRecipe(event.getPlayer())) {
                barrierCursor(event.getPlayer());
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            if (removeBarrierRecipe(event.getPlayer())) {
                fallbackCursor(event.getPlayer());
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            if (addBarrierRecipe(event.getPlayer())) {
                barrierCursor(event.getPlayer());
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(event);
            if (setSlot.getWindowId() == 0 && setSlot.getSlot() == 1) {
                setSlot.setItem(createBarrier());
                event.markForReEncode(true);
            }

            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
            if (packet.getWindowId() == 0) {
                List<ItemStack> items = packet.getItems();
                items.set(1, createBarrier());
            }

            event.markForReEncode(true);
            return;
        }
        PacketListener.super.onPacketSend(event);
    }

    /**
     * Adds a barrier recipe to the player's recipe book
     *
     * @param player The player to add the recipe to
     * @return true if the recipe was successfully added
     */
    public boolean addBarrierRecipe(Player player) {
        ShapedCraftingRecipeDisplay display = new ShapedCraftingRecipeDisplay(1, 1, List.of(new ItemStackSlotDisplay(createBarrier())), new ItemStackSlotDisplay(createBarrier()), EmptySlotDisplay.INSTANCE);
        MappedEntitySet<ItemType> set = new MappedEntitySet<>(List.of(ItemTypes.STRUCTURE_VOID));
        WrapperPlayServerRecipeBookAdd.AddEntry entry = new WrapperPlayServerRecipeBookAdd.AddEntry(new RecipeDisplayEntry(new RecipeDisplayId(id), display, null, RecipeBookCategories.CRAFTING_MISC, List.of(set)), false, true);
        WrapperPlayServerRecipeBookAdd packet = new WrapperPlayServerRecipeBookAdd(List.of(entry), false);

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        return true;
    }

    /**
     * Removes the barrier recipe from the player's recipe book
     *
     * @param player The player to remove the recipe from
     * @return true if the recipe was successfully removed
     */
    public boolean removeBarrierRecipe(Player player) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerRecipeBookRemove(List.of(new RecipeDisplayId(id))));
        return true;
    }

    /**
     * Creates a special barrier item stack with custom NBT data
     *
     * @return The created barrier ItemStack
     */
    private ItemStack createBarrier() {
        NBTCompound nbt = new NBTCompound();
        nbt.setTag("open-inventory-plugin", new NBTString("barrier"));
        return ItemStack.builder().type(ItemTypes.STRUCTURE_VOID).component(ComponentTypes.CUSTOM_DATA, nbt).amount(1).build();
    }

    /**
     *
     * */
    /**
     * Restores the player's cursor to its original state
     *
     * @param player The player whose cursor needs to be restored
     */
    public void fallbackCursor(Player player) {
        int stateId = getStateId(player);
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, new WrapperPlayServerSetSlot(0, stateId, 1, ItemStack.EMPTY));
        Pair<Boolean, Boolean> pair = settingsMap.getOrDefault(player.getUniqueId(), Pair.of(true, false));

        Map<RecipeBookType, RecipeBookSettings.TypeState> crafting = new HashMap<>();
        crafting.put(RecipeBookType.CRAFTING, new RecipeBookSettings.TypeState(pair.first(), pair.second()));

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerRecipeBookSettings(new RecipeBookSettings(crafting)));
    }

    /**
     * Sets the player's cursor to display the barrier item
     *
     * @param player The player whose cursor needs to be updated
     */
    public void barrierCursor(Player player) {
        ItemStack itemStack = createBarrier();

        int stateId = getStateId(player);
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, new WrapperPlayServerSetSlot(0, stateId, 1, itemStack));

        Map<RecipeBookType, RecipeBookSettings.TypeState> crafting = new HashMap<>();
        crafting.put(RecipeBookType.CRAFTING, new RecipeBookSettings.TypeState(true, true));
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerRecipeBookSettings(new RecipeBookSettings(crafting)));
    }

    /**
     * Retrieves the inventory state ID for a player
     *
     * @param player The player to get the state ID for
     * @return The current inventory state ID
     */
    public static int getStateId(Player player) {
        int stateID = -1;
        Object entityPlayer = SpigotReflectionUtil.getEntityPlayer(player);
        if (entityPlayer != null) {
            Field inventoryMenu;
            try {
                inventoryMenu = getFieldByName(entityPlayer.getClass(), "inventoryMenu");
                inventoryMenu.setAccessible(true);
                Object container = inventoryMenu.get(entityPlayer);
                if (container != null) {
                    Field windowId = getFieldByName(container.getClass(), "stateId");
                    windowId.setAccessible(true);
                    stateID = windowId.getInt(container);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return stateID;
    }

    /**
     * Utility method to get a field by name from a class or its superclasses
     *
     * @param clazz     The class to search in
     * @param fieldName The name of the field to find
     * @return The found Field object or null if not found
     */
    public static Field getFieldByName(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;

        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
}
