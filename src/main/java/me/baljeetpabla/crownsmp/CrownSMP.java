package me.baljeetpabla.crownsmp;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public final class CrownSMP extends JavaPlugin implements CommandExecutor, TabCompleter {
    private CrownManager crowns;

    @Override public void onEnable() {
        saveDefaultConfig();
        crowns = new CrownManager(this);
        getServer().getPluginManager().registerEvents(new CrownListener(this, crowns), this);
        PluginCommand command = getCommand("crown");
        if (command != null) { command.setExecutor(this); command.setTabCompleter(this); }
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (crowns.isCrownHolder(player)) {
                    crowns.applyBaseEffects(player);
                    crowns.ensureCrownHelmet(player);
                }
            }
        }, 1L, 20L);
        getLogger().info("CrownSMP enabled.");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crownsmp.admin")) { sender.sendMessage("§cNo permission."); return true; }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            boolean dark = args[1].equalsIgnoreCase("dark");
            if (!dark && !args[1].equalsIgnoreCase("light")) { sender.sendMessage("§cChoose light or dark."); return true; }
            if (dark) crowns.removeDarkCrown(); else crowns.removeLightCrown();
            sender.sendMessage("§aRemoved " + (dark ? "Dark" : "Light") + " Crown.");
            return true;
        }
        if (args.length != 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("§e/crown give <light|dark> <player>");
            sender.sendMessage("§e/crown remove <light|dark>");
            return true;
        }
        boolean dark = args[1].equalsIgnoreCase("dark");
        if (!dark && !args[1].equalsIgnoreCase("light")) { sender.sendMessage("§cChoose light or dark."); return true; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { sender.sendMessage("§cPlayer must be online."); return true; }
        if (dark) crowns.setDarkCrown(target); else crowns.setLightCrown(target);
        sender.sendMessage("§aGave the " + (dark ? "Dark" : "Light") + " Crown to " + target.getName() + ".");
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("give", "remove");
        if (args.length == 2) return List.of("light", "dark");
        return List.of();
    }
}
