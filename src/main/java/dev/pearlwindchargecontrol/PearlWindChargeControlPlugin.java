package dev.pearlwindchargecontrol;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class PearlWindChargeControlPlugin extends JavaPlugin implements Listener {

    private NamespacedKey infinitePearlKey;
    private NamespacedKey infiniteWindKey;

    @Override
    public void onEnable() {
        infinitePearlKey = new NamespacedKey(this, "infinite_pearl");
        infiniteWindKey = new NamespacedKey(this, "infinite_wind_charge");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private boolean hasMarker(ItemStack item, NamespacedKey key) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private ItemStack createInfiniteItem(Material material, String miniName, String lore, NamespacedKey key) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(miniName));
        meta.lore(List.of(Component.text(lore)));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createInfinitePearl() {
        return createInfiniteItem(Material.ENDER_PEARL,
                "<!italic><gradient:#9B5DE5:#00F5D4>Infinite Pearl</gradient>",
                "A pearl that never runs out.", infinitePearlKey);
    }

    public ItemStack createInfiniteWindCharge() {
        return createInfiniteItem(Material.WIND_CHARGE,
                "<!italic><gradient:#7DE8FF:#FFFFFF>Infinite Wind Charge</gradient>",
                "A wind charge that never runs out.", infiniteWindKey);
    }

    /** Infinite items are never used up; normal pearls / wind charges obey the /toggleitem setting. */
    @EventHandler
    public void onLaunch(PlayerLaunchProjectileEvent e) {
        ItemStack item = e.getItemStack();
        Material type = item.getType();

        if (type == Material.ENDER_PEARL) {
            if (hasMarker(item, infinitePearlKey)) {
                e.setShouldConsume(false);
            } else if (!getConfig().getBoolean("pearls-enabled", true)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(Component.text("Ender pearls are currently disabled.", NamedTextColor.RED));
            }
        } else if (type == Material.WIND_CHARGE) {
            if (hasMarker(item, infiniteWindKey)) {
                e.setShouldConsume(false);
            } else if (!getConfig().getBoolean("windcharges-enabled", true)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(Component.text("Wind charges are currently disabled.", NamedTextColor.RED));
            }
        }
    }

    private void give(CommandSender sender, String[] args, ItemStack item, String usage) {
        Player target = null;
        if (args.length > 0) target = Bukkit.getPlayer(args[0]);
        else if (sender instanceof Player p) target = p;
        if (target == null) {
            sender.sendMessage(usage);
            return;
        }
        target.getInventory().addItem(item);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "infinitepearl" -> give(sender, args, createInfinitePearl(), "Usage: /infinitepearl [player]");
            case "infinitewindcharge" -> give(sender, args, createInfiniteWindCharge(), "Usage: /infinitewindcharge [player]");
            case "toggleitem" -> {
                if (args.length < 1 || !(args[0].equalsIgnoreCase("pearls") || args[0].equalsIgnoreCase("windcharges"))) {
                    sender.sendMessage("Usage: /toggleitem <pearls|windcharges> [on|off]");
                    return true;
                }
                String path = args[0].equalsIgnoreCase("pearls") ? "pearls-enabled" : "windcharges-enabled";
                String name = args[0].equalsIgnoreCase("pearls") ? "Ender pearls" : "Wind charges";
                boolean enabled;
                if (args.length >= 2) {
                    if (args[1].equalsIgnoreCase("on")) enabled = true;
                    else if (args[1].equalsIgnoreCase("off")) enabled = false;
                    else {
                        sender.sendMessage("Usage: /toggleitem <pearls|windcharges> [on|off]");
                        return true;
                    }
                } else {
                    enabled = !getConfig().getBoolean(path, true);
                }
                getConfig().set(path, enabled);
                saveConfig();
                sender.sendMessage(name + " are now " + (enabled ? "ENABLED" : "DISABLED") + " for players.");
            }
            default -> { return false; }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("toggleitem")) {
            if (args.length == 1) return List.of("pearls", "windcharges");
            if (args.length == 2) return List.of("on", "off");
        }
        return List.of();
    }
}
