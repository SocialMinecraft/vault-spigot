package club.somc.vaultSpigot.items;

import club.somc.vaultSpigot.VaultSpigot;
import club.somc.vaultSpigot.uis.VaultUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

public class VaultItem implements Listener {

    private NamespacedKey vaultKey  = new NamespacedKey("vault", "item");
    private final VaultSpigot plugin;
    private final VaultUI vaultUI;

    public VaultItem(VaultSpigot plugin, VaultUI vaultUI) {
        this.plugin = plugin;
        this.vaultUI = vaultUI;
        this.createRecipe();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("vault.use")) {
            return;
        }

        if (!player.hasDiscoveredRecipe(vaultKey)) {
            player.discoverRecipe(vaultKey);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(vaultKey, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("vault.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use the vault.");
            return;
        }

        vaultUI.open(player);
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        URL urlObject;
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWU1MmY3OTYwZmYzY2VjMmY1MTlhNjM1MzY0OGM2ZTMzYmM1MWUxMzFjYzgwOTE3Y2YxMzA4MWRlY2JmZjI0ZCJ9fX0="));
            String url = decoded.substring(decoded.indexOf("http"), decoded.lastIndexOf("\""));
            urlObject = new URL(url);
        } catch (Exception e) {
            plugin.getLogger().warning(e.getMessage());
            return null;
        }

        textures.setSkin(urlObject);
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        meta.setDisplayName("Vault");
        meta.setLore(Arrays.asList("Right-click to open vault"));

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(vaultKey, PersistentDataType.BYTE, (byte)1);

        item.setItemMeta(meta);

        return item;
    }

    private void createRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(vaultKey, createItem());

        recipe.shape("OWO","OEO","CCC");

        recipe.setIngredient('O', Material.OBSIDIAN);
        recipe.setIngredient('C', Material.CRYING_OBSIDIAN);
        recipe.setIngredient('E', Material.ENDER_EYE);
        recipe.setIngredient('W', Material.ELYTRA);

        Bukkit.addRecipe(recipe);
    }
}
