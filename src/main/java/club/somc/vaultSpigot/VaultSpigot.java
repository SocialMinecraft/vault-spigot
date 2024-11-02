package club.somc.vaultSpigot;

import club.somc.vaultSpigot.uis.VaultUI;
import io.nats.client.Connection;
import io.nats.client.Nats;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class VaultSpigot extends JavaPlugin {

    public Connection nc;

    @Override
    public void onEnable() {
        super.onEnable();
        this.saveDefaultConfig();

        try {
            this.nc = Nats.connect(getConfig().getString("natsUrl"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        getServer().getPluginManager().registerEvents(new VaultUI(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}