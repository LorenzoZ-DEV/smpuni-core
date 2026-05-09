package dev.lorenzz.solum.modules.skilltree;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.SkillTreeManager;

public class SkillTreeModule extends AbstractModule {

    private SkillTreeManager skillTreeManager;

    public SkillTreeModule(Solum plugin) {
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
