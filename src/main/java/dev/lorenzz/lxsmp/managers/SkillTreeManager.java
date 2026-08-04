package dev.lorenzz.lxsmp.managers;

import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manages player skill trees — skills that boost progression.
 * Skills cost Shards to unlock/upgrade and grant passive bonuses.
 */
public class SkillTreeManager {

    private static SkillTreeManager INSTANCE;

    private final LXSMP plugin;
    private final Logger logger;

    // skill id -> SkillDefinition
    private final Map<String, SkillDefinition> skillDefs = new LinkedHashMap<>();
    // uuid -> skill id -> level
    private final Map<UUID, Map<String, Integer>> playerSkills = new HashMap<>();

    private File dataFile;
    private YamlConfiguration dataConfig;

    public record SkillDefinition(
            String id,
            String displayName,
            String description,
            String icon, // Material name
            int maxLevel,
            int baseCost,
            int costPerLevel,
            String effectType, // POTION, MULTIPLIER
            String potionEffect, // e.g. SPEED, HASTE, HEALTH_BOOST
            int potionAmplifierPerLevel,
            double multiplierPerLevel // e.g. 0.1 = 10% per level
    ) {}

    public SkillTreeManager(LXSMP plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        INSTANCE = this;
    }

    public static SkillTreeManager getInstance() { return INSTANCE; }

    public void init() {
        loadDefinitions();
        loadPlayerData();
    }

    private void loadDefinitions() {
        skillDefs.clear();
        ConfigurationSection section = plugin.getConfigFile().getConfigurationSection("skilltree.skills");
        if (section == null) {
            logger.warning("[Solum] No skill tree definitions found in config.");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            skillDefs.put(key, new SkillDefinition(
                    key,
                    s.getString("display-name", "&f" + key),
                    s.getString("description", "A skill."),
                    s.getString("icon", "BOOK"),
                    s.getInt("max-level", 5),
                    s.getInt("base-cost", 100),
                    s.getInt("cost-per-level", 50),
                    s.getString("effect-type", "POTION"),
                    s.getString("potion-effect", "SPEED"),
                    s.getInt("potion-amplifier-per-level", 0),
                    s.getDouble("multiplier-per-level", 0.1)
            ));
        }
        logger.info("[Solum] Loaded " + skillDefs.size() + " skill definitions.");
    }

    private void loadPlayerData() {
        playerSkills.clear();

        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
        } catch (IOException e) { e.printStackTrace(); }

        dataFile = new File(plugin.getDataFolder(), "data/skilltree.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection players = dataConfig.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { continue; }

            ConfigurationSection skills = players.getConfigurationSection(uuidStr);
            if (skills == null) continue;

            Map<String, Integer> map = new HashMap<>();
            for (String skillId : skills.getKeys(false)) {
                map.put(skillId, skills.getInt(skillId, 0));
            }
            playerSkills.put(uuid, map);
        }
    }

    public void save() {
        if (dataConfig == null) return;
        for (Map.Entry<UUID, Map<String, Integer>> entry : playerSkills.entrySet()) {
            String base = "players." + entry.getKey().toString();
            for (Map.Entry<String, Integer> skill : entry.getValue().entrySet()) {
                dataConfig.set(base + "." + skill.getKey(), skill.getValue());
            }
        }
        try { dataConfig.save(dataFile); } catch (IOException e) {
            logger.severe("[Solum] Failed to save skilltree data: " + e.getMessage());
        }
    }

    public Collection<SkillDefinition> getAllSkills() {
        return Collections.unmodifiableCollection(skillDefs.values());
    }

    public SkillDefinition getSkill(String id) {
        return skillDefs.get(id);
    }

    public int getPlayerLevel(UUID uuid, String skillId) {
        Map<String, Integer> map = playerSkills.get(uuid);
        if (map == null) return 0;
        return map.getOrDefault(skillId, 0);
    }

    public int getUpgradeCost(SkillDefinition skill, int currentLevel) {
        return skill.baseCost() + (skill.costPerLevel() * currentLevel);
    }

    /**
     * Attempts to upgrade a skill for a player. Returns true on success.
     */
    public boolean upgradeSkill(UUID uuid, String skillId) {
        SkillDefinition skill = skillDefs.get(skillId);
        if (skill == null) return false;

        int currentLevel = getPlayerLevel(uuid, skillId);
        if (currentLevel >= skill.maxLevel()) return false;

        int cost = getUpgradeCost(skill, currentLevel);

        ShardManager sm = ShardManager.getInstance();
        if (sm == null) return false;
        if (!sm.removeShards(uuid, cost)) return false;

        playerSkills.computeIfAbsent(uuid, k -> new HashMap<>()).put(skillId, currentLevel + 1);
        save();

        // Apply effects immediately if player is online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applySkillEffects(player);
        }

        return true;
    }

    /**
     * Resets all skills for a player, refunding a percentage of shards.
     */
    public int resetSkills(UUID uuid) {
        Map<String, Integer> skills = playerSkills.get(uuid);
        if (skills == null || skills.isEmpty()) return 0;

        int totalRefund = 0;
        double refundPercent = plugin.getConfigFile().getDouble("skilltree.reset-refund-percent", 50.0) / 100.0;

        for (Map.Entry<String, Integer> entry : skills.entrySet()) {
            SkillDefinition def = skillDefs.get(entry.getKey());
            if (def == null) continue;
            int level = entry.getValue();
            for (int i = 0; i < level; i++) {
                totalRefund += getUpgradeCost(def, i);
            }
        }

        int refund = (int) (totalRefund * refundPercent);
        skills.clear();
        save();

        if (refund > 0) {
            ShardManager sm = ShardManager.getInstance();
            if (sm != null) sm.addShards(uuid, refund);
        }

        // Remove potion effects from player
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            removeSkillEffects(player);
        }

        return refund;
    }

    /**
     * Applies all active potion effects from skills to a player.
     * Called on join and after upgrades.
     */
    public void applySkillEffects(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, Integer> skills = playerSkills.getOrDefault(uuid, Collections.emptyMap());

        for (Map.Entry<String, Integer> entry : skills.entrySet()) {
            SkillDefinition def = skillDefs.get(entry.getKey());
            if (def == null || entry.getValue() <= 0) continue;

            if ("POTION".equalsIgnoreCase(def.effectType())) {
                PotionEffectType type = PotionEffectType.getByName(def.potionEffect());
                if (type == null) continue;
                int amplifier = def.potionAmplifierPerLevel() * entry.getValue() - 1;
                if (amplifier < 0) amplifier = 0;
                // Apply infinite duration potion effect
                player.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, true, false, true));
            }
        }
    }

    /**
     * Removes all skill-based potion effects from a player.
     */
    public void removeSkillEffects(Player player) {
        for (SkillDefinition def : skillDefs.values()) {
            if ("POTION".equalsIgnoreCase(def.effectType())) {
                PotionEffectType type = PotionEffectType.getByName(def.potionEffect());
                if (type != null) {
                    player.removePotionEffect(type);
                }
            }
        }
    }

    /**
     * Gets the multiplier for a specific skill for a player.
     * e.g. mining_boost level 3 with 0.1 per level = 1.3x
     */
    public double getMultiplier(UUID uuid, String skillId) {
        SkillDefinition def = skillDefs.get(skillId);
        if (def == null) return 1.0;
        int level = getPlayerLevel(uuid, skillId);
        return 1.0 + (def.multiplierPerLevel() * level);
    }

    public Map<String, Integer> getPlayerSkills(UUID uuid) {
        return Collections.unmodifiableMap(playerSkills.getOrDefault(uuid, Collections.emptyMap()));
    }

    public void shutdown() {
        save();
    }
}
