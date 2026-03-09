package dev.lorenzz.hirustuffs.modules.billford;
import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.BillfordManager;
import dev.lorenzz.hirustuffs.managers.BillfordManager.BillfordTrade;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;
public class BillfordCommands {
    private final Hiru plugin = Hiru.getInstance();
    @Command(name = "billford", aliases = {"trader"}, description = "Open Billford's trades", async = false)
    @Permission("hiru.billford")
    public void billford(@Sender Player player) {
        BillfordManager bm = BillfordManager.getInstance();
        if (bm == null) { Messages.send(player, "&cBillford not available."); return; }
        List<BillfordTrade> trades = bm.getActiveTrades();
        long nextRotation = bm.getNextRotation();
        long remaining = Math.max(0, (nextRotation - System.currentTimeMillis()) / 1000);
        String timeStr = (remaining / 3600) + "h " + ((remaining % 3600) / 60) + "m";
        GUI.Builder builder = GUI.builder("&#333333&lBillford's Trades", 3)
                .border(Material.LIME_STAINED_GLASS_PANE);
        builder.item(4, Material.CLOCK, "&#FFD700&lNext Rotation",
                List.of("&#8C8C8CNew trades in: &#E0E0E0" + timeStr));
        int slot = 10;
        for (BillfordTrade trade : trades) {
            String inName = formatMat(trade.inputMaterial());
            String outName = formatMat(trade.outputMaterial());
            builder.item(slot, trade.outputMaterial(), Messages.color(trade.displayName()),
                    List.of("", "&#8C8C8CGive: &#FF6B6B" + trade.inputAmount() + "x " + inName,
                            "&#8C8C8CReceive: &#55FF55" + trade.outputAmount() + "x " + outName,
                            "", "&#FFD700&lClick to trade"),
                    p -> {
                        if (!p.getInventory().containsAtLeast(new ItemStack(trade.inputMaterial()), trade.inputAmount())) {
                            Messages.send(p, "&#FF4444You need &#FFD700" + trade.inputAmount() + "x " + inName);
                            return;
                        }
                        p.getInventory().removeItem(new ItemStack(trade.inputMaterial(), trade.inputAmount()));
                        p.getInventory().addItem(new ItemStack(trade.outputMaterial(), trade.outputAmount())).values()
                                .forEach(leftover -> p.getWorld().dropItemNaturally(p.getLocation(), leftover));
                        Messages.send(p, "&#55FF55Trade complete! Received &#FFD700" + trade.outputAmount() + "x " + outName);
                    });
            slot += 2;
        }
        builder.open(player);
    }
    private String formatMat(Material material) {
        String name = material.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String w : name.split(" ")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(' ');
        }
        return sb.toString().trim();
    }
}
