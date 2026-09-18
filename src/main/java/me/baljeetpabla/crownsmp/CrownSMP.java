package me.baljeetpabla.crownsmp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class CrownSMP extends JavaPlugin implements org.bukkit.command.CommandExecutor, org.bukkit.command.TabCompleter {

    private UUID crownedUuid;
    private UUID darkCrownedUuid;
    private CrownManager crownManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        crownManager = new CrownManager(this);
        loadCrown();
        loadDarkCrown();

        CrownListener listener = new CrownListener(this, crownManager);
        getServer().getPluginManager().registerEvents(listener, this);

        PluginCommand crownCommand = getCommand("crown");
        if (crownCommand != null) {
            crownCommand.setExecutor(this);
            crownCommand.setTabCompleter(this);
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Player crown = getCrownedPlayer();
            if (crown != null && crown.isOnline() && !crown.isDead()) {
                crownManager.applyBaseBuffs(crown);
                crownManager.updateCrownItem(crown);
            }
            Player darkCrown = getDarkCrownedPlayer();
            if (darkCrown != null && darkCrown.isOnline() && !darkCrown.isDead()) {
                crownManager.applyBaseBuffs(darkCrown, true);
                crownManager.updateDarkCrownItem(darkCrown);
            }
        }, 1L, 40L);

        getLogger().info("CrownSMP enabled.");
    }

    @Override
    public void onDisable() {
        loadDarkCrown();
        Player crown = getCrownedPlayer();
        if (crown != null) {
            crownManager.removeCrownBuffs(crown);
        }
        getLogger().info("CrownSMP disabled.");
    }

    public Player getCrownedPlayer() {
        if (crownedUuid == null) return null;
        return Bukkit.getPlayer(crownedUuid);
    }

    public UUID getCrownedUuid() {
        return crownedUuid;
    }

    public Player getDarkCrownedPlayer() {
        if (darkCrownedUuid == null) return null;
        return Bukkit.getPlayer(darkCrownedUuid);
    }

    public UUID getDarkCrownedUuid() { return darkCrownedUuid; }

    public boolean isDarkCrowned(Player player) {
        return player != null && darkCrownedUuid != null && darkCrownedUuid.equals(player.getUniqueId());
    }

    public boolean isCrowned(Player player) {
        return player != null && crownedUuid != null && crownedUuid.equals(player.getUniqueId());
    }

    public void setCrown(Player player) {
        if (isDarkCrowned(player)) removeDarkCrown();
        Player oldCrown = getCrownedPlayer();
        if (oldCrown != null && !oldCrown.getUniqueId().equals(player.getUniqueId())) {
            crownManager.removeCrownBuffs(oldCrown);
            crownManager.removeCrownItem(oldCrown);
        }

        crownedUuid = player.getUniqueId();
        getConfig().set("crowned-uuid", crownedUuid.toString());
        saveConfig();

        crownManager.resetCombatState(player);
        crownManager.applyBaseBuffs(player);
        crownManager.giveCrownItem(player);

        send(player, "crown-given");
    }

    public void setDarkCrown(Player player) {
        Player old = getDarkCrownedPlayer();
        if (old != null && !old.getUniqueId().equals(player.getUniqueId())) {
            crownManager.removeCrownBuffs(old);
            crownManager.removeDarkCrownItem(old);
        }
        if (isCrowned(player)) removeCrown();
        darkCrownedUuid = player.getUniqueId();
        getConfig().set("dark-crowned-uuid", darkCrownedUuid.toString());
        saveConfig();
        crownManager.resetCombatState(player);
        crownManager.applyBaseBuffs(player, true);
        crownManager.giveDarkCrownItem(player);
        send(player, "dark-crown-given");
    }

    public void removeDarkCrown() {
        Player old = getDarkCrownedPlayer();
        if (old != null) {
            crownManager.removeCrownBuffs(old);
            crownManager.removeDarkCrownItem(old);
        }
        darkCrownedUuid = null;
        getConfig().set("dark-crowned-uuid", null);
        saveConfig();
    }

    public void removeCrown() {
        Player oldCrown = getCrownedPlayer();
        if (oldCrown != null) {
            crownManager.removeCrownBuffs(oldCrown);
            crownManager.removeCrownItem(oldCrown);
        }

        crownedUuid = null;
        getConfig().set("crowned-uuid", null);
        saveConfig();
    }

    public void loadCrown() {
        String value = getConfig().getString("crowned-uuid");
        if (value == null || value.isBlank()) {
            crownedUuid = null;
            return;
        }

        try {
            crownedUuid = UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Invalid crowned-uuid in config.yml; clearing it.");
            crownedUuid = null;
            getConfig().set("crowned-uuid", null);
            saveConfig();
        }
    }

    public void loadDarkCrown() {
        String value = getConfig().getString("dark-crowned-uuid");
        if (value == null || value.isBlank()) { darkCrownedUuid = null; return; }
        try { darkCrownedUuid = UUID.fromString(value); }
        catch (IllegalArgumentException ex) {
            getLogger().warning("Invalid dark-crowned-uuid in config.yml; clearing it.");
            darkCrownedUuid = null;
            getConfig().set("dark-crowned-uuid", null);
            saveConfig();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        crownManager.reloadValues();
        loadCrown();
        Player crown = getCrownedPlayer();
        if (crown != null) {
            crownManager.applyBaseBuffs(crown);
            crownManager.updateCrownItem(crown);
        }
        Player darkCrown = getDarkCrownedPlayer();
        if (darkCrown != null) {
            crownManager.applyBaseBuffs(darkCrown, true);
            crownManager.updateDarkCrownItem(darkCrown);
        }
    }

    public void send(CommandSender sender, String key) {
        String prefix = color(getConfig().getString("messages.prefix", "&6&lCROWN &8» &r"));
        String message = getConfig().getString("messages." + key, key);
        sender.sendMessage(prefix + color(message));
    }

    public void send(CommandSender sender, String key, String playerName) {
        String prefix = color(getConfig().getString("messages.prefix", "&6&lCROWN &8» &r"));
        String message = getConfig().getString("messages." + key, key).replace("%player%", playerName);
        sender.sendMessage(prefix + color(message));
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(color("&6&lCrownSMP &8- &7Commands"));
        sender.sendMessage(color("&e/crown set <player> &7- Crown a player"));
        sender.sendMessage(color("&e/crown darkset <player> &7- Give the Dark Crown"));
        sender.sendMessage(color("&e/crown remove &7- Remove the Crown"));
        sender.sendMessage(color("&e/crown darkremove &7- Remove the Dark Crown"));
        sender.sendMessage(color("&e/crown give <player> &7- Crown a player"));
        sender.sendMessage(color("&e/crown darkgive <player> &7- Give the Dark Crown"));
        sender.sendMessage(color("&e/crown info &7- View current Crown info"));
        sender.sendMessage(color("&e/crown reload &7- Reload config.yml"));
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.hasPermission("crownsmp.admin")) {
            sender.sendMessage(color("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set", "give", "darkset", "darkgive" -> {
                if (args.length < 2) {
                    sender.sendMessage(color("&cUsage: /crown " + args[0] + " <player>"));
                    return true;
                }

                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(color("&cThat player must be online."));
                    return true;
                }

                if (args[0].toLowerCase(Locale.ROOT).startsWith("dark")) {
                    setDarkCrown(target);
                    if (!sender.equals(target)) send(sender, "dark-crown-set", target.getName());
                } else {
                    setCrown(target);
                    if (!sender.equals(target)) send(sender, "crown-set", target.getName());
                }
                return true;
            }
            case "darkremove" -> {
                Player current = getDarkCrownedPlayer();
                if (current == null) { send(sender, "no-dark-crown"); return true; }
                String name = current.getName();
                removeDarkCrown();
                send(sender, "dark-crown-removed", name);
                return true;
            }
            case "remove" -> {
                Player current = getCrownedPlayer();
                if (current == null) {
                    send(sender, "no-crown");
                    return true;
                }
                String name = current.getName();
                removeCrown();
                send(sender, "crown-removed", name);
                return true;
            }
            case "info" -> {
                Player current = getCrownedPlayer();
                if (current == null) {
                    send(sender, "no-crown");
                } else {
                    sender.sendMessage(color("&6&lCrown &8» &fCurrent Crowned player: &e" + current.getName()));
                    sender.sendMessage(color("&6&lCrown &8» &fCrown Rage: &c5 hits &7(default)"));
                    sender.sendMessage(color("&6&lCrown &8» &fExecution: &620s cooldown &7(default)"));
                }
                return true;
            }
            case "reload" -> {
                reloadPlugin();
                sender.sendMessage(color("&aCrownSMP configuration reloaded."));
                return true;
            }
            default -> {
                sendUsage(sender);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> values = List.of("set", "give", "darkset", "darkgive", "remove", "darkremove", "info", "reload", "help");
            String input = args[0].toLowerCase(Locale.ROOT);
            return values.stream().filter(value -> value.startsWith(input)).toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("darkset") || args[0].equalsIgnoreCase("darkgive"))) {
            String input = args[1].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(input)) {
                    names.add(player.getName());
                }
            }
            return names;
        }

        return List.of();
    }
}
