package dev.lorenzz.solum.modules.tools;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class AmethystToolsListener implements Listener {

    private final Solum plugin;
    private final NamespacedKey AMETHYST_KEY;
    private final NamespacedKey USES_KEY;
    private final NamespacedKey MAX_USES_KEY;

    public AmethystToolsListener(Solum plugin) {
        this.plugin = plugin;
        this.AMETHYST_KEY = new NamespacedKey(plugin, "amethyst_tool");
        this.USES_KEY = new NamespacedKey(plugin, "amethyst_uses");
        this.MAX_USES_KEY = new NamespacedKey(plugin, "amethyst_max_uses");
    }

    public ItemStack createAmethystTool(String toolId) {
        ConfigurationSection section = plugin.getConfigFile().getConfigurationSection("tools.tools-list." + toolId);
        if (section == null) return null;

        Material material = Material.valueOf(section.getString("material", "DIAMOND_SWORD"));
        String name = section.getString("name", "&dAmethyst Tool");
        int maxUses = plugin.getConfigFile().getInt("tools.durability-uses", 500);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        meta.displayName(Messages.colorComponent(name));

        List<String> loreConfig = section.getStringList("lore");
        List<Component> lore = loreConfig.stream()
                .map(line -> line.replace("%uses%", String.valueOf(maxUses)))
                .map(Messages::colorComponent)
                .collect(Collectors.toList());
        meta.lore(lore);

        ConfigurationSection enchants = section.getConfigurationSection("enchantments");
        if (enchants != null) {
            for (String enchName : enchants.getKeys(false)) {
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchName.toLowerCase()));
                if (ench != null) {
                    meta.addEnchant(ench, enchants.getInt(enchName), true);
                }
            }
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(AMETHYST_KEY, PersistentDataType.STRING, toolId);
        pdc.set(USES_KEY, PersistentDataType.INTEGER, maxUses);
        pdc.set(MAX_USES_KEY, PersistentDataType.INTEGER, maxUses);

        item.setItemMeta(meta);
        return item;
    }

    private boolean isAmethystTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(AMETHYST_KEY, PersistentDataType.STRING);
    }

    private void decrementUses(Player player, ItemStack item) {
        if (!isAmethystTool(item)) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        int uses = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0);
        uses--;

        if (uses <= 0) {
            player.getInventory().remove(item);
            Messages.send(player, "&d&lYour amethyst tool has been destroyed!");
            return;
        }

        pdc.set(USES_KEY, PersistentDataType.INTEGER, uses);

        String toolId = pdc.get(AMETHYST_KEY, PersistentDataType.STRING);
        ConfigurationSection section = plugin.getConfigFile().getConfigurationSection("tools.tools-list." + toolId);
        if (section != null) {
            int finalUses = uses;
            List<Component> lore = section.getStringList("lore").stream()
                    .map(line -> line.replace("%uses%", String.valueOf(finalUses)))
                    .map(Messages::colorComponent)
                    .map(this::stripDefaults)
                    .collect(Collectors.toList());
            meta.lore(lore);
        }

        item.setItemMeta(meta);
    }

    private Component stripDefaults(Component component) {
        return component
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, false);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (isAmethystTool(tool)) {
            decrementUses(event.getPlayer(), tool);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (isAmethystTool(tool)) {
            decrementUses(player, tool);
        }
    }
}
