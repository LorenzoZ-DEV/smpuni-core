package dev.lorenzz.hirustuffs.modules.keyall;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class KeyAllCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "keyall", description = "Dai chiavi a tutti i giocatori online")
    @Permission("hiru.admin")
    public void keyall(@Sender CommandSender sender, @Named("tipo") String keyType, @Range(min = 1) @Named("quantità") int amount) {
        List<String> validTypes = plugin.getConfigFile().getStringList("keyall.valid-types");
        if (!validTypes.contains(keyType.toLowerCase())) {
            Messages.send(sender, "&cTipo non valido! Tipi: " + String.join(", ", validTypes));
            return;
        }

        ItemStack key = createKey(keyType.toLowerCase(), amount);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().addItem(key.clone());
            Messages.send(player, plugin.getConfigFile().getString("keyall.received", "&7Hai ricevuto &b%amount%x %type% Key!")
                    .replace("%amount%", String.valueOf(amount)).replace("%type%", keyType));
        }

        Bukkit.broadcast(Messages.colorComponent(
                plugin.getConfigFile().getString("keyall.broadcast", "&a&l[KeyAll] &eTutti hanno ricevuto &b%amount%x %type% Key&e!")
                        .replace("%amount%", String.valueOf(amount)).replace("%type%", keyType)));
    }

    private ItemStack createKey(String type, int amount) {
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, amount);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            String color = switch (type) {
                case "common" -> "&f";
                case "rare" -> "&9";
                case "epic" -> "&5";
                case "legendary" -> "&6";
                default -> "&7";
            };
            meta.displayName(Messages.colorComponent(color + "&l" + type.toUpperCase() + " Key"));
            meta.lore(List.of(
                    Messages.colorComponent("&7Tipo: " + color + type),
                    Messages.colorComponent("&7Usa su una crate per aprirla!")
            ));
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            // PDC tags per identificare la chiave
            NamespacedKey crateKey = new NamespacedKey(plugin, "crate_key");
            NamespacedKey crateType = new NamespacedKey(plugin, "crate_key_type");
            meta.getPersistentDataContainer().set(crateKey, PersistentDataType.BOOLEAN, true);
            meta.getPersistentDataContainer().set(crateType, PersistentDataType.STRING, type);

            key.setItemMeta(meta);
        }
        return key;
    }
}
