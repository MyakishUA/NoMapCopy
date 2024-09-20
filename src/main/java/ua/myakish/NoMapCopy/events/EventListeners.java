package ua.myakish.NoMapCopy.events;

import ua.myakish.NoMapCopy.NoMapCopy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class EventListeners implements Listener {

    public static final NamespacedKey ownerKey = new NamespacedKey(NoMapCopy.getPlugin(NoMapCopy.class), "map-owner");
    public static final NamespacedKey lockedKey = new NamespacedKey(NoMapCopy.getPlugin(NoMapCopy.class), "is-copy-locked");

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemCraft(PrepareItemCraftEvent event) {
        ItemStack map = null;
        boolean hasGlassPane = false;
        boolean hasEmptyMap = false;

        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item == null) continue;
            if (item.getType() == Material.FILLED_MAP && map == null) {
                map = item.clone();
            } else if (item.getType() == Material.getMaterial(NoMapCopy.config.getString("config.locker-item"))) {
                hasGlassPane = true;
            } else if (item.getType() == Material.MAP) {
                hasEmptyMap = true;
            }
        }

        if (map == null) return;

        List<HumanEntity> viewers = event.getViewers();
        String player = null;
        String playerUUID = null;
        if (!viewers.isEmpty()) {
            HumanEntity user = viewers.get(0);
            player = user.getName();
            playerUUID = user.getUniqueId().toString();
        }

        ItemMeta meta = map.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        String mapExistingAuthor = container.get(ownerKey, PersistentDataType.STRING);
        @Nullable Integer isExistingLocked = container.get(lockedKey, PersistentDataType.INTEGER);

        if (hasGlassPane) {
            meta.lore(List.of(
                    Component.text("Копіювання заборонено: " + player, NamedTextColor.DARK_PURPLE)
            ));

            container.set(ownerKey, PersistentDataType.STRING, playerUUID);
            container.set(lockedKey, PersistentDataType.INTEGER, 1);

            if (NoMapCopy.config.getBoolean("config.rename-item")) {
                meta.displayName(meta.hasDisplayName()
                        ? Objects.requireNonNull(meta.displayName()).color(NamedTextColor.DARK_RED)
                        : Component.text("Карта", NamedTextColor.DARK_RED));
            }

            map.setItemMeta(meta);
            event.getInventory().setResult(map);

        } else if (hasEmptyMap && !Objects.equals(mapExistingAuthor, playerUUID)) {
            event.getInventory().setResult(null);
            for (HumanEntity viewer : viewers) {
                viewer.closeInventory();
                viewer.sendMessage(Component.text("Ви не можете скопіювати цей предмет.", NamedTextColor.RED));
            }
        } else if (hasEmptyMap && (isExistingLocked != null && isExistingLocked == 1 && Objects.equals(mapExistingAuthor, playerUUID))) {
            map.setAmount(2);
            event.getInventory().setResult(map);
        } else if (hasEmptyMap) {
            map.setAmount(2);
            event.getInventory().setResult(map);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (NoMapCopy.config.getBoolean("config.close-cartography-attempted-use")) return;

        if (event.getInventory().getType() == InventoryType.CARTOGRAPHY) {
            CartographyInventory inventory = (CartographyInventory) event.getInventory();

            if (inventory.getSize() < 2) return; 

            ItemStack mapItem = inventory.getItem(0);

            if (mapItem != null && mapItem.hasItemMeta()) {
                ItemMeta mapMeta = mapItem.getItemMeta();
                PersistentDataContainer container = mapMeta.getPersistentDataContainer();

                if (container.has(lockedKey, PersistentDataType.INTEGER)) {
                    String mapOwner = container.get(ownerKey, PersistentDataType.STRING);

                    HumanEntity player = event.getWhoClicked();
                    String playerUUID = player.getUniqueId().toString();

                    if (!Objects.equals(mapOwner, playerUUID)) {
                        player.sendMessage(Component.text("Ви не можете скопіювати цей предмет.", NamedTextColor.RED));
                        inventory.clear();
                        player.closeInventory();
                    }
                }
            }
        }
    }
}
