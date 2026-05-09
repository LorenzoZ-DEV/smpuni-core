package dev.lorenzz.solum.modules.adaptive;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdaptiveToolListener implements Listener {

    private final Solum plugin;
    private final NamespacedKey ID_KEY;
    private final NamespacedKey USES_KEY;
    private final NamespacedKey MAX_USES_KEY;
    private final NamespacedKey FORM_KEY;

    public AdaptiveToolListener(Solum plugin) {
        this.plugin = plugin;
        this.ID_KEY = new NamespacedKey(plugin, "adaptive_tool");
        this.USES_KEY = new NamespacedKey(plugin, "adaptive_uses");
        this.MAX_USES_KEY = new NamespacedKey(plugin, "adaptive_max_uses");
        this.FORM_KEY = new NamespacedKey(plugin, "adaptive_form");
    }

    public ItemStack createTool() {
        String defaultForm = plugin.getConfigFile().getString("adaptive-tool.default-form", "pickaxe");
        int maxUses = plugin.getConfigFile().getInt("adaptive-tool.max-uses", 1500);

        ConfigurationSection formSec = getFormSection(defaultForm);
        if (formSec == null) return null;

        org.bukkit.Material mat = parseMaterial(formSec.getString("material"), org.bukkit.Material.NETHERITE_PICKAXE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ID_KEY, PersistentDataType.STRING, "adaptive");
        pdc.set(USES_KEY, PersistentDataType.INTEGER, maxUses);
        pdc.set(MAX_USES_KEY, PersistentDataType.INTEGER, maxUses);
        pdc.set(FORM_KEY, PersistentDataType.STRING, defaultForm);

        applyAppearance(meta, defaultForm, maxUses, maxUses);
        applyEnchants(meta, formSec);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isAdaptiveTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ID_KEY, PersistentDataType.STRING);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isAdaptiveTool(tool)) return;

        String desired = bestFormFor(event.getBlock());
        morphIfNeeded(player, tool, desired);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isAdaptiveTool(tool)) return;
        decrementUses(player, tool);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPreAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isAdaptiveTool(tool)) return;

        morphIfNeeded(player, tool, "sword");

        if (event.willAttack()) {
            decrementUses(player, player.getInventory().getItemInMainHand());
        }
    }

    private void morphIfNeeded(Player player, ItemStack item, String desiredForm) {
        if (desiredForm == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String currentForm = pdc.getOrDefault(FORM_KEY, PersistentDataType.STRING, "");
        if (desiredForm.equalsIgnoreCase(currentForm)) return;

        ConfigurationSection formSec = getFormSection(desiredForm);
        if (formSec == null) return;

        org.bukkit.Material newMat = parseMaterial(formSec.getString("material"), item.getType());

        int uses = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0);
        int maxUses = pdc.getOrDefault(MAX_USES_KEY, PersistentDataType.INTEGER, uses);

        item.setType(newMat);
        meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer newPdc = meta.getPersistentDataContainer();
        newPdc.set(ID_KEY, PersistentDataType.STRING, "adaptive");
        newPdc.set(USES_KEY, PersistentDataType.INTEGER, uses);
        newPdc.set(MAX_USES_KEY, PersistentDataType.INTEGER, maxUses);
        newPdc.set(FORM_KEY, PersistentDataType.STRING, desiredForm.toLowerCase(Locale.ROOT));

        for (Enchantment e : new ArrayList<>(meta.getEnchants().keySet())) {
            meta.removeEnchant(e);
        }
        applyEnchants(meta, formSec);
        applyAppearance(meta, desiredForm, uses, maxUses);

        item.setItemMeta(meta);

        playMorphFx(player, desiredForm);
    }

    private void playMorphFx(Player player, String form) {
        ConfigurationSection part = plugin.getConfigFile().getConfigurationSection("adaptive-tool.particles");
        Particle particle = Particle.DRAGON_BREATH;
        int count = 30;
        double ox = 0.5, oy = 1.0, oz = 0.5, speed = 0.05;
        if (part != null) {
            try {
                particle = Particle.valueOf(part.getString("type", "DRAGON_BREATH").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
            count = part.getInt("count", count);
            ox = part.getDouble("offset-x", ox);
            oy = part.getDouble("offset-y", oy);
            oz = part.getDouble("offset-z", oz);
            speed = part.getDouble("speed", speed);
        }
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.0, 0), count, ox, oy, oz, speed);

        ConfigurationSection snd = plugin.getConfigFile().getConfigurationSection("adaptive-tool.sound");
        Sound sound = Sound.ENTITY_ENDER_DRAGON_FLAP;
        float volume = 0.6f, pitch = 1.5f;
        if (snd != null) {
            try {
                sound = Sound.valueOf(snd.getString("type", "ENTITY_ENDER_DRAGON_FLAP").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
            volume = (float) snd.getDouble("volume", volume);
            pitch = (float) snd.getDouble("pitch", pitch);
        }
        player.playSound(player.getLocation(), sound, volume, pitch);

        String msg = plugin.getConfigFile().getString("adaptive-tool.messages.morphed", "");
        if (!msg.isEmpty()) {
            Messages.send(player, msg.replace("{form}", form));
        }
    }

    private String bestFormFor(Block block) {
        org.bukkit.Material type = block.getType();
        if (Tag.MINEABLE_PICKAXE.isTagged(type)) return "pickaxe";
        if (Tag.MINEABLE_AXE.isTagged(type)) return "axe";
        if (Tag.MINEABLE_SHOVEL.isTagged(type)) return "shovel";
        if (Tag.MINEABLE_HOE.isTagged(type)) return "hoe";
        if (type == org.bukkit.Material.COBWEB || type == org.bukkit.Material.BAMBOO) return "sword";
        return null;
    }

    private void decrementUses(Player player, ItemStack item) {
        if (!isAdaptiveTool(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        int uses = pdc.getOrDefault(USES_KEY, PersistentDataType.INTEGER, 0) - 1;
        int maxUses = pdc.getOrDefault(MAX_USES_KEY, PersistentDataType.INTEGER, uses);
        String form = pdc.getOrDefault(FORM_KEY, PersistentDataType.STRING, "pickaxe");

        if (uses <= 0) {
            player.getInventory().removeItem(item);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0),
                    60, 0.6, 1.0, 0.6, 0.08);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 0.4f, 1.8f);
            String msg = plugin.getConfigFile().getString("adaptive-tool.messages.destroyed", "");
            if (!msg.isEmpty()) Messages.send(player, msg);
            return;
        }

        pdc.set(USES_KEY, PersistentDataType.INTEGER, uses);
        applyAppearance(meta, form, uses, maxUses);
        item.setItemMeta(meta);
    }

    private void applyAppearance(ItemMeta meta, String form, int uses, int maxUses) {
        String name = plugin.getConfigFile().getString("adaptive-tool.name", "&5&lAdaptive Tool");
        meta.displayName(Messages.colorComponent(name.replace("{form}", form)));

        List<String> raw = plugin.getConfigFile().getStringList("adaptive-tool.lore");
        List<Component> lore = new ArrayList<>(raw.size());
        for (String line : raw) {
            lore.add(Messages.colorComponent(line
                    .replace("{form}", form)
                    .replace("{uses}", String.valueOf(uses))
                    .replace("{max}", String.valueOf(maxUses))));
        }
        meta.lore(lore);
    }

    private void applyEnchants(ItemMeta meta, ConfigurationSection formSec) {
        ConfigurationSection enchants = formSec.getConfigurationSection("enchantments");
        if (enchants == null) return;
        for (String key : enchants.getKeys(false)) {
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
            if (ench != null) {
                meta.addEnchant(ench, enchants.getInt(key, 1), true);
            }
        }
    }

    private ConfigurationSection getFormSection(String form) {
        if (form == null) return null;
        return plugin.getConfigFile().getConfigurationSection(
                "adaptive-tool.forms." + form.toLowerCase(Locale.ROOT));
    }

    private org.bukkit.Material parseMaterial(String name, org.bukkit.Material fallback) {
        if (name == null) return fallback;
        try {
            return org.bukkit.Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}

