package dev.lorenzz.solum.modules.auctionhouse;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

/**
 * ItemStack <-> Base64 string serialization. Uses Bukkit's ConfigurationSerializable
 * via YamlConfiguration so it handles Paper's full ItemMeta (NBT, attributes, components).
 */
public final class ItemSerialization {

    private ItemSerialization() {}

    /**
     * Serialize via YamlConfiguration: most reliable across versions; preserves all meta.
     */
    public static String serialize(ItemStack item) {
        if (item == null) return null;
        try {
            YamlConfiguration tmp = new YamlConfiguration();
            tmp.set("i", item);
            return Base64.getEncoder().encodeToString(tmp.saveToString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            return fallbackSerialize(item);
        }
    }

    public static ItemStack deserialize(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            String yaml = new String(Base64.getDecoder().decode(data), java.nio.charset.StandardCharsets.UTF_8);
            YamlConfiguration tmp = new YamlConfiguration();
            tmp.loadFromString(yaml);
            return tmp.getItemStack("i");
        } catch (Exception e) {
            return fallbackDeserialize(data);
        }
    }

    /* ---- Java serialization fallback (best-effort) ---- */

    private static String fallbackSerialize(ItemStack item) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(item);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack fallbackDeserialize(String data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (ItemStack) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}

