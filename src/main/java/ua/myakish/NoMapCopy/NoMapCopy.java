package ua.myakish.NoMapCopy;

import ua.myakish.NoMapCopy.events.EventListeners;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;

import java.util.logging.Logger;

public final class NoMapCopy extends JavaPlugin {

    public static FileConfiguration config = null;
    public static String version = "1.0-SNAPSHOT";
    public Logger logger = getLogger();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        getServer().getPluginManager().registerEvents(new EventListeners(), this);

        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (config.getBoolean("config.rename-item")) {
            meta.displayName(Component.text("Карта"));
        }
        item.setItemMeta(meta);

        NamespacedKey key = new NamespacedKey(this, "lock_filled_map");
        ShapelessRecipe recipe = new ShapelessRecipe(key, item);
        recipe.addIngredient(Material.FILLED_MAP);
        recipe.addIngredient(Material.getMaterial(config.getString("config.locker-item")));
        Bukkit.addRecipe(recipe);
    }

    @Override
    public void onDisable() {
        logger.info("NoMapCopy був відключений.");
    }
}
