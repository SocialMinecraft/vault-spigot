package club.somc.vaultSpigot.uis;

import club.somc.protos.vault.*;
import club.somc.vaultSpigot.VaultSpigot;
import com.google.protobuf.InvalidProtocolBufferException;
import io.nats.client.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class VaultUI implements Listener {

    private final VaultSpigot plugin;
    private static final NamespacedKey cooldownKey = new NamespacedKey("vault", "cooldown");
    private static final NamespacedKey lockKey = new NamespacedKey("vault", "lock");
    private static final NamespacedKey brokenKey = new NamespacedKey("vault", "broken");


    public VaultUI(VaultSpigot plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String name = "Vault";

        GetVault req = GetVault.newBuilder()
                        .setUuid(player.getUniqueId().toString())
                                .build();

        Message msg;
        try {
            msg = plugin.nc.request("vault.get", req.toByteArray(), Duration.ofMillis(300));
        } catch (InterruptedException e) {
            player.sendMessage(ChatColor.RED + "Error connecting to vault.");
            plugin.getLogger().warning(e.getMessage());
            return;
        }

        if (msg == null) {
            player.sendMessage(ChatColor.RED + "Error connecting to vault.");
            return;
        }

        GetVaultResponse resp;
        try {
            resp = GetVaultResponse.parseFrom(msg.getData());
        } catch (InvalidProtocolBufferException e) {
            player.sendMessage(ChatColor.RED + "Error connecting to vault.");
            plugin.getLogger().warning(e.getMessage());
            return;
        }

        if (!resp.hasVault()) {
            player.sendMessage(ChatColor.RED + "Vault not found.");
            return;
        }

        int size = resp.getVault().getSlotsCount();
        size = (int) (Math.ceil(size / 9.0) * 9); // this should never happen as the server should return in a multiple of 9.
        size = Math.min(size, 54);
        Inventory inventory = Bukkit.createInventory(player,  size, name);

        for (int i = 0; i < resp.getVault().getSlotsCount(); i++) {
            VaultSlot slot = resp.getVault().getSlots(i);

            if (slot.getIsLocked()) {
                inventory.setItem(i, createLock());
            } else if (slot.getCooldownSeconds() > 0) {
                inventory.setItem(i, createCooldown(slot.getCooldownSeconds()));
            } else if (slot.hasItem()) {
                inventory.setItem(i, deserializeItem(slot.getItem()));
            } else {
                // available.
                inventory.setItem(i, null);
            }
        }
        for (int i = resp.getVault().getSlotsCount(); i < size; i++) {
            inventory.setItem(i, createLock());
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().startsWith("Vault")) return;
        event.setCancelled(true);
        return;

        /*Player player = (Player) event.getWhoClicked();
        player.sendMessage(ChatColor.RED + "Vault drag started.");

        ItemStack draggedItem = event.getOldCursor();
        ItemStack newItem = event.getCursor();

        Inventory top = event.getView().getTopInventory();
        Inventory bottom = event.getView().getBottomInventory();*/
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("Vault")) return;
        if (isSpecial(event.getCurrentItem()) || isSpecial(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (overStackSize(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() != ClickType.LEFT) {
            event.setCancelled(true);
            return;
        }
        if (event.getRawSlot() < 0 ) {
            event.setCancelled(true);
            return;
        }
        boolean isTop = event.getRawSlot() < event.getView().getTopInventory().getSize();
        boolean cursorEmpty = event.getCursor() == null || event.getCursor().getType() == Material.AIR;

        Player player = (Player) event.getWhoClicked();
        /*if (event.getCurrentItem() != null)
            player.sendMessage("Current " + event.getCurrentItem().getType().name());
        if (event.getCursor() != null)
            player.sendMessage("Cursor " + event.getCursor().getType().name());*/

        if (isTop && cursorEmpty && event.getView().getItem(event.getRawSlot()).getType() != Material.AIR) {
            player.sendMessage(ChatColor.GREEN + "Withdraw " + event.getCurrentItem().getType().name());
        } else if (isTop && !cursorEmpty) {
            player.sendMessage(ChatColor.GREEN + "Deposit " + event.getCursor().getType().name());
        }



        //event.setCancelled(true);


        //player.sendMessage(isTop ? "top" : "bottom");
        //player.sendMessage("slot" + event.getRawSlot());
        //player.sendMessage(event.getClick().toString());
    }

    public static ItemStack deserializeItem(VaultItem vItem) {
        Material material = Material.valueOf(vItem.getType());

        ItemStack item = new ItemStack(material);

        if (material == Material.AIR) {
            return item;
        }

        // Basic properties
        item.setAmount(vItem.getAmount());
        item.setDurability((short) vItem.getDurability());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Display name
            if (vItem.hasDisplayName()) {
                meta.setDisplayName(vItem.getDisplayName());
            }

            // Lore
            if (vItem.getLoreCount() > 0) {
                meta.setLore(vItem.getLoreList().stream().toList());
            }

            // Enchantments
            if (vItem.getEnchantsCount() > 0) {
                for (VaultItemEnchantment vEnchant : vItem.getEnchantsList()) {
                    Enchantment enchant = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(vEnchant.getName()));
                    if (enchant != null) {
                        meta.addEnchant(enchant, vEnchant.getLevel(), true);
                    }
                }
            }

            // Item flags
            if (vItem.getFlagsCount() > 0) {
                for (String flag : vItem.getFlagsList()) {
                    meta.addItemFlags(ItemFlag.valueOf(flag));
                }
            }

            // Custom model data
            if (vItem.hasCustomModelData()) {
                meta.setCustomModelData(vItem.getCustomModelData());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static VaultItem serializeItem(ItemStack item) {


        VaultItem.Builder builder = VaultItem.newBuilder();

        // Basic item properties
        builder.setType(item.getType().name());
        builder.setAmount(item.getAmount());
        builder.setDurability(item.getDurability());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Display name
            if (meta.hasDisplayName()) {
                builder.setDisplayName(meta.getDisplayName());
            }

            // Lore
            if (meta.hasLore()) {
                builder.addAllLore(meta.getLore());
            }

            // Enchantments
            if (!meta.getEnchants().isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    //enchants.set(entry.getKey().getKey().getKey(), entry.getValue());
                    builder.addEnchants(
                      VaultItemEnchantment.newBuilder()
                              .setName(entry.getKey().toString())
                              .setLevel(entry.getValue())
                    );
                }
            }

            // Item flags
            if (!meta.getItemFlags().isEmpty()) {
                for (ItemFlag flag : meta.getItemFlags()) {
                    builder.addFlags(flag.name());
                }
            }

            // Custom model data
            if (meta.hasCustomModelData()) {
                builder.setCustomModelData(meta.getCustomModelData());
            }
        }

        return builder.build();
    }

    public ItemStack createCooldown(int seconds) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        URL urlObject;
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjYwZmE2MzIyNzRlODliMDY0YzMwOTBjMWFiMTEwMTQ3YzgzZWM3M2VjNGMzMWNhNzUwODE0NTdlYmI4YjI2OSJ9fX0="));
            String url = decoded.substring(decoded.indexOf("http"), decoded.lastIndexOf("\""));
            urlObject = new URL(url);
        } catch (Exception e) {
            plugin.getLogger().warning(e.getMessage());
            return null;
        }

        textures.setSkin(urlObject);
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        meta.setDisplayName("Cooldown: " + seconds + " seconds");

        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(cooldownKey, PersistentDataType.BYTE, (byte)1);

        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createBroken() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        URL urlObject;
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWU3NzAwMDk2YjVhMmE4NzM4NmQ2MjA1YjRkZGNjMTRmZDMzY2YyNjkzNjJmYTY4OTM0OTk0MzFjZTc3YmY5In19fQ=="));
            String url = decoded.substring(decoded.indexOf("http"), decoded.lastIndexOf("\""));
            urlObject = new URL(url);
        } catch (Exception e) {
            plugin.getLogger().warning(e.getMessage());
            return null;
        }

        textures.setSkin(urlObject);
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        meta.setDisplayName("Item Broken");

        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(brokenKey, PersistentDataType.BYTE, (byte)1);

        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createLock() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        URL urlObject;
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGY0NzJhZDgwMWUzM2U5ZmJmNTQyMmZiOTBhYTM1NjAyODc1MTE4YzEyOWFmZWFkZWM1ZmE2ODdjODhmODI4OSJ9fX0="));
            String url = decoded.substring(decoded.indexOf("http"), decoded.lastIndexOf("\""));
            urlObject = new URL(url);
        } catch (Exception e) {
            plugin.getLogger().warning(e.getMessage());
            return null;
        }

        textures.setSkin(urlObject);
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        meta.setDisplayName("Locked");

        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        meta.getPersistentDataContainer().set(lockKey, PersistentDataType.BYTE, (byte)1);

        item.setItemMeta(meta);

        return item;
    }

    public boolean isSpecial(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        return (meta.getPersistentDataContainer().has(cooldownKey, PersistentDataType.BYTE) ||
                meta.getPersistentDataContainer().has(lockKey, PersistentDataType.BYTE) ||
                meta.getPersistentDataContainer().has(brokenKey, PersistentDataType.BYTE));
    }

    public boolean overStackSize(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return item.getAmount() > 1;
    }
}
