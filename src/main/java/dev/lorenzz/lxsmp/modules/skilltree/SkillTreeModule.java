package dev.lorenzz.lxsmp.modules.skilltree;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.SkillTreeManager;

public class SkillTreeModule extends AbstractModule {

    private SkillTreeManager skillTreeManager;

    public SkillTreeModule(LXSMP plugin) {
        super(plugin, "skilltree");
    }

    @Override
    public void onEnable() {
        skillTreeManager = new SkillTreeManager(plugin);
        skillTreeManager.init();

        plugin.getCommandManager().register(new SkillTreeCommand());
        registerListener(new SkillTreeListener(plugin));

        logger.info("[Solum] SkillTreeModule loaded — " + skillTreeManager.getAllSkills().size() + " skills defined.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (skillTreeManager != null) skillTreeManager.shutdown();
    }

    public SkillTreeManager getSkillTreeManager() { return skillTreeManager; }
}
