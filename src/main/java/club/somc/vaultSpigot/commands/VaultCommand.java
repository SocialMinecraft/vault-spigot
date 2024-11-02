package club.somc.vaultSpigot.commands;

import club.somc.vaultSpigot.uis.VaultUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VaultCommand implements CommandExecutor {

    private final VaultUI ui;

    public VaultCommand(VaultUI ui) {
        this.ui = ui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        ui.open(player);

        return true;
    }
}
