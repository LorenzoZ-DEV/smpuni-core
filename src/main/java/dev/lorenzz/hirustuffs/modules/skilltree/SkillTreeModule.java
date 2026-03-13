package dev.lorenzz.hirustuffs.modules.skilltree;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.SkillTreeManager;

public class SkillTreeModule extends AbstractModule {

    private SkillTreeManager skillTreeManager;

    public SkillTreeModule(Hiru plugin) {
        super(plugin, "skilltree");
    }

    @Override
    public void onEnable() {
        skillTreeManager = new SkillTreeManager(plugin);
        skillTreeManager.init();

        plugin.getCommandManager().register(new SkillTreeCommand());
        registerListener(new SkillTreeListener(plugin));

        logger.info("[Hiru] SkillTreeModule loaded — " + skillTreeManager.getAllSkills().size() + " skills defined.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (skillTreeManager != null) skillTreeManager.shutdown();
    }

    public SkillTreeManager getSkillTreeManager() { return skillTreeManager; }
}
