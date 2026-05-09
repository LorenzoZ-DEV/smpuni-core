package dev.lorenzz.solum.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.modules.nametag.NametagModule;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.command.CommandSender;

@CommandAlias("solum")
@CommandPermission("solum.admin")
@Description("Solum admin command")
public class SolumCommand extends BaseCommand {

    @Default
    @HelpCommand
    @CatchUnknown
    public void root(CommandSender sender) {
        Messages.sendRaw(sender, "&8&m---------&8[ &b&lSolum &8]&8&m---------");
        Messages.sendRaw(sender, "&b/solum reload &7- Reload config");
        Messages.sendRaw(sender, "&b/solum version &7- Show version");
        Messages.sendRaw(sender, "&8&m----------------------------");
    }

    @Subcommand("reload")
    @Description("Reload configuration")
    public void reload(CommandSender sender) {
        Solum.getInstance().reloadAllConfigs();
        Messages.send(sender, "&aConfiguration reloaded successfully!");
    }

    @Subcommand("version")
    @Description("Show version")
    public void version(CommandSender sender) {
        Messages.send(sender, "&bSolum &7v" + Solum.getInstance().getDescription().getVersion() + " &7by &blorenzz");
    }

    @Subcommand("nametag offset")
    @Syntax("<value>")
    @Description("Live-tune the floating-stats line vertical offset (relative to eye height).")
    public void nametagOffset(CommandSender sender, String valueStr) {
        NametagModule mod = Solum.getInstance().getModuleManager().getModule("nametag");
        if (mod == null || mod.getManager() == null) {
            Messages.send(sender, "&cNametag module is not enabled.");
            return;
        }
        double v;
        try { v = Double.parseDouble(valueStr); }
        catch (NumberFormatException e) {
            Messages.send(sender, "&cInvalid number: " + valueStr);
            return;
        }
        mod.getManager().setExtraAboveEyes(v);
        // No reload() needed: followAll() teleports every tick using the live value,
        // and setTeleportDuration(3) interpolates the move smoothly client-side.
        Messages.send(sender, "&aNametag offset set to &e" + v
                + " &7(text top ≈ eye + " + String.format("%.2f", v + 0.5) + ").");
        Messages.send(sender, "&7Tip: with &ehide-from-owner: true&7 you cannot see your own line. Ask another player or look at a second account.");
    }

    @Subcommand("nametag info")
    @Description("Show current nametag offset.")
    public void nametagInfo(CommandSender sender) {
        NametagModule mod = Solum.getInstance().getModuleManager().getModule("nametag");
        if (mod == null || mod.getManager() == null) {
            Messages.send(sender, "&cNametag module is not enabled.");
            return;
        }
        double v = mod.getManager().getExtraAboveEyes();
        Messages.send(sender, "&7extra-above-eyes: &e" + v
                + " &7| text top ≈ eye + " + String.format("%.2f", v + 0.5));
        Messages.send(sender, "&7hide-from-owner: &e" + mod.getManager().isHideFromOwner());
    }

    @Subcommand("nametag self")
    @Syntax("<true|false>")
    @Description("Toggle whether you see your own nametag line (useful for live tuning in F5).")
    public void nametagSelf(CommandSender sender, String visible) {
        NametagModule mod = Solum.getInstance().getModuleManager().getModule("nametag");
        if (mod == null || mod.getManager() == null) {
            Messages.send(sender, "&cNametag module is not enabled.");
            return;
        }
        boolean v = visible.equalsIgnoreCase("true") || visible.equalsIgnoreCase("on") || visible.equalsIgnoreCase("yes");
        // 'visible=true' means we should NOT hide it from the owner → hideFromOwner = false
        mod.getManager().setHideFromOwner(!v);
        Messages.send(sender, "&aSelf-visible nametag: &e" + v
                + " &7(hide-from-owner=" + (!v) + ")");
    }
}
