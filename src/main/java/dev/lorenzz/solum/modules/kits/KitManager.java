package dev.lorenzz.solum.modules.kits;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Config;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.Color;

import java.util.*;
import java.util.logging.Logger;

public class KitManager {

    private static KitManager INSTANCE;

    private final Solum plugin;
    private final Logger logger;
    private final Map<String, Kit> kits = new LinkedHashMap<>();
    private Config kitsConfig;

    public KitManager(Solum plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        INSTANCE = this;
    }

    public static KitManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        kits.clear();
        kitsConfig = new Config(plugin, "kits.yml");

        ConfigurationSection section = kitsConfig.getConfigurationSection("kits");
        if (section == null) {
            logger.warning("[Solum] No kits found in kits.yml — creating defaults.");
            createDefaults();
            section = kitsConfig.getConfigurationSection("kits");
            if (section == null) return;
        }

        for (String kitId : section.getKeys(false)) {
            ConfigurationSection kitSec = section.getConfigurationSection(kitId);
            if (kitSec == null) continue;

            String displayName = kitSec.getString("display-name", "&f" + kitId);
            int cooldown = kitSec.getInt("cooldown", 0);
            boolean oneTime = kitSec.getBoolean("one-time", false);
            String permission = kitSec.getString("permission", "");
            Material icon = Material.matchMaterial(kitSec.getString("icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;

            List<String> lore = kitSec.getStringList("lore");

            List<ItemStack> items = new ArrayList<>();
            ConfigurationSection itemsSec = kitSec.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String itemKey : itemsSec.getKeys(false)) {
                    ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemKey);
                    if (itemSec == null) continue;
                    ItemStack stack = deserializeItem(itemSec);
                    if (stack != null) items.add(stack);
                }
            }

            Kit kit = new Kit(kitId, displayName, cooldown, oneTime, permission, icon, lore, items);
            kits.put(kitId.toLowerCase(), kit);
        }

        logger.info("[Solum] Loaded " + kits.size() + " kit(s).");
    }

    public void reload() {
        load();
    }

    public void saveKit(Kit kit) {
        String path = "kits." + kit.id();
        kitsConfig.set(path + ".display-name", kit.displayName());
        kitsConfig.set(path + ".cooldown", kit.cooldown());
        kitsConfig.set(path + ".one-time", kit.oneTime());
        kitsConfig.set(path + ".permission", kit.permission());
        kitsConfig.set(path + ".icon", kit.icon().name());
        kitsConfig.set(path + ".lore", kit.lore());

        ConfigurationSection itemsSec = kitsConfig.createSection(path + ".items");
        for (int i = 0; i < kit.items().size(); i++) {
            ItemStack stack = kit.items().get(i);
            String itemPath = "item" + i;
            serializeItem(itemsSec.createSection(itemPath), stack);
        }

        kitsConfig.saveFile();
    }

    public void deleteKit(String kitId) {
        kits.remove(kitId.toLowerCase());
        kitsConfig.set("kits." + kitId, null);
        kitsConfig.saveFile();
    }

    public Kit getKit(String name) {
        return kits.get(name.toLowerCase());
    }

    public Collection<Kit> getKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public void registerKit(Kit kit) {
        kits.put(kit.id().toLowerCase(), kit);
        saveKit(kit);
    }

    private void serializeItem(ConfigurationSection sec, ItemStack stack) {
        sec.set("material", stack.getType().name());
        sec.set("amount", stack.getAmount());

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        if (meta.hasDisplayName()) {
            sec.set("name", meta.getDisplayName());
        }
        if (meta.hasLore()) {
            sec.set("lore", meta.getLore());
        }

        if (!meta.getEnchants().isEmpty()) {
            ConfigurationSection enchSec = sec.createSection("enchantments");
            for (Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
                enchSec.set(e.getKey().getKey().getKey(), e.getValue());
            }
        }

        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            if (!bookMeta.getStoredEnchants().isEmpty()) {
                ConfigurationSection storedSec = sec.createSection("stored-enchantments");
                for (Map.Entry<Enchantment, Integer> e : bookMeta.getStoredEnchants().entrySet()) {
                    storedSec.set(e.getKey().getKey().getKey(), e.getValue());
                }
            }
        }

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            Color color = leatherMeta.getColor();
            sec.set("leather-color", String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack deserializeItem(ConfigurationSection sec) {
        String matName = sec.getString("material");
        if (matName == null) return null;
        Material material = Material.matchMaterial(matName);
        if (material == null) return null;

        int amount = sec.getInt("amount", 1);
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        if (sec.contains("name")) {
            meta.setDisplayName(dev.lorenzz.solum.utils.Messages.color(sec.getString("name")));
        }
        if (sec.contains("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : sec.getStringList("lore")) {
                lore.add(dev.lorenzz.solum.utils.Messages.color(line));
            }
            meta.setLore(lore);
        }

        ConfigurationSection enchSec = sec.getConfigurationSection("enchantments");
        if (enchSec != null) {
            for (String key : enchSec.getKeys(false)) {
                Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(key.toLowerCase()));
                if (ench != null) {
                    meta.addEnchant(ench, enchSec.getInt(key), true);
                }
            }
        }

        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            ConfigurationSection storedSec = sec.getConfigurationSection("stored-enchantments");
            if (storedSec != null) {
                for (String key : storedSec.getKeys(false)) {
                    Enchantment ench = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(key.toLowerCase()));
                    if (ench != null) {
                        bookMeta.addStoredEnchant(ench, storedSec.getInt(key), true);
                    }
                }
            }
        }

        if (meta instanceof LeatherArmorMeta leatherMeta && sec.contains("leather-color")) {
            String hex = sec.getString("leather-color", "#A06540").replace("#", "");
            try {
                int rgb = Integer.parseInt(hex, 16);
                leatherMeta.setColor(Color.fromRGB(rgb));
            } catch (NumberFormatException ignored) {}
        }

        stack.setItemMeta(meta);
        return stack;
    }

    private void createDefaults() {
        String base = "kits.starter";
        kitsConfig.set(base + ".display-name", "&#55FF55&lStarter Kit");
        kitsConfig.set(base + ".cooldown", 300);
        kitsConfig.set(base + ".one-time", false);
        kitsConfig.set(base + ".permission", "solum.kit.starter");
        kitsConfig.set(base + ".icon", "CHEST");
        kitsConfig.set(base + ".lore", Arrays.asList(
                "&7A basic kit for new players.",
                "&7Contains starter gear and food.",
                "",
                "&#55FF55&l➥ Click to claim"
        ));

        kitsConfig.set(base + ".items.sword.material", "IRON_SWORD");
        kitsConfig.set(base + ".items.sword.amount", 1);
        kitsConfig.set(base + ".items.sword.name", "&#AAAAAA&lStarter Sword");

        kitsConfig.set(base + ".items.food.material", "BREAD");
        kitsConfig.set(base + ".items.food.amount", 64);

        kitsConfig.set(base + ".items.helmet.material", "IRON_HELMET");
        kitsConfig.set(base + ".items.helmet.amount", 1);

        kitsConfig.set(base + ".items.chestplate.material", "IRON_CHESTPLATE");
        kitsConfig.set(base + ".items.chestplate.amount", 1);

        kitsConfig.set(base + ".items.leggings.material", "IRON_LEGGINGS");
        kitsConfig.set(base + ".items.leggings.amount", 1);

        kitsConfig.set(base + ".items.boots.material", "IRON_BOOTS");
        kitsConfig.set(base + ".items.boots.amount", 1);

        kitsConfig.set(base + ".items.book.material", "ENCHANTED_BOOK");
        kitsConfig.set(base + ".items.book.amount", 1);
        kitsConfig.set(base + ".items.book.stored-enchantments.sharpness", 1);

        kitsConfig.saveFile();
    }
}
