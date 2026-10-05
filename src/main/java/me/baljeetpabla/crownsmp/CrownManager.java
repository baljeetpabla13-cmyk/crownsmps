package me.baljeetpabla.crownsmp;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.*;

public final class CrownManager {
    private final CrownSMP plugin;
    private final NamespacedKey typeKey;
    private UUID lightHolder, darkHolder;
    private final Map<UUID,Integer> darkHits = new HashMap<>();
    private final Map<UUID,Long> lastDarkHit = new HashMap<>();

    public CrownManager(CrownSMP plugin) {
        this.plugin = plugin;
        typeKey = new NamespacedKey(plugin, "crown_type");
        load();
    }

    private void load() {
        lightHolder = parse(plugin.getConfig().getString("holders.light"));
        darkHolder = parse(plugin.getConfig().getString("holders.dark"));
    }

    private UUID parse(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException e) { return null; }
    }

    private void save() {
        plugin.getConfig().set("holders.light", lightHolder == null ? null : lightHolder.toString());
        plugin.getConfig().set("holders.dark", darkHolder == null ? null : darkHolder.toString());
        plugin.saveConfig();
    }

    public boolean isLightCrownHolder(Player p) { return lightHolder != null && lightHolder.equals(p.getUniqueId()); }
    public boolean isDarkCrownHolder(Player p) { return darkHolder != null && darkHolder.equals(p.getUniqueId()); }
    public boolean isCrownHolder(Player p) { return isLightCrownHolder(p) || isDarkCrownHolder(p); }

    public void setLightCrown(Player player) {
        if (isDarkCrownHolder(player)) removeDarkCrown();
        Player old = lightHolder == null ? null : Bukkit.getPlayer(lightHolder);
        if (old != null) removeCrownItems(old);
        lightHolder = player.getUniqueId();
        save();
        giveCrown(player, false);
        applyBaseEffects(player);
    }

    public void setDarkCrown(Player player) {
        if (isLightCrownHolder(player)) removeLightCrown();
        Player old = darkHolder == null ? null : Bukkit.getPlayer(darkHolder);
        if (old != null) removeCrownItems(old);
        darkHolder = player.getUniqueId();
        darkHits.remove(player.getUniqueId());
        lastDarkHit.remove(player.getUniqueId());
        save();
        giveCrown(player, true);
        applyBaseEffects(player);
    }

    public void removeLightCrown() {
        Player old = lightHolder == null ? null : Bukkit.getPlayer(lightHolder);
        if (old != null) removeCrownItems(old);
        lightHolder = null; save();
    }

    public void removeDarkCrown() {
        Player old = darkHolder == null ? null : Bukkit.getPlayer(darkHolder);
        if (old != null) removeCrownItems(old);
        darkHolder = null; save();
    }

    public void applyBaseEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 2, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 1, false, false, true));
    }

    public void ensureCrownHelmet(Player player) {
        boolean dark = isDarkCrownHolder(player);
        ItemStack helmet = player.getInventory().getHelmet();
        if ((dark && isCrownItem(helmet, true)) || (!dark && isCrownItem(helmet, false))) return;
        removeCrownItems(player);
        player.getInventory().setHelmet(createCrown(dark));
    }

    private ItemStack createCrown(boolean dark) {
        ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(dark ? "§5§lDark Crown" : "§6§lLight Crown");
        meta.setUnbreakable(true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, 10, true);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, dark ? "dark" : "light");
        item.setItemMeta(meta);
        return item;
    }

    private void giveCrown(Player player, boolean dark) {
        removeCrownItems(player);
        player.getInventory().setHelmet(createCrown(dark));
        player.sendMessage(dark ? "§5§lYou now wield the Dark Crown." : "§6§lYou now wield the Light Crown.");
    }

    private boolean isCrownItem(ItemStack item, boolean dark) {
        if (item == null || item.getType() != Material.GOLDEN_HELMET || !item.hasItemMeta()) return false;
        String type = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        return dark ? "dark".equals(type) : "light".equals(type);
    }

    private void removeCrownItems(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (isCrownItem(helmet, true) || isCrownItem(helmet, false)) player.getInventory().setHelmet(null);
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isCrownItem(item, true) || isCrownItem(item, false)) player.getInventory().setItem(slot, null);
        }
    }

    public void onDarkCrownHit(Player crown) {
        if (!isDarkCrownHolder(crown)) return;
        long now = System.currentTimeMillis();
        long timeout = plugin.getConfig().getLong("dark-crown.combo-timeout-ms", 1750L);
        Long last = lastDarkHit.get(crown.getUniqueId());
        if (last != null && now - last > timeout) darkHits.put(crown.getUniqueId(), 0);
        int hits = darkHits.getOrDefault(crown.getUniqueId(), 0) + 1;
        darkHits.put(crown.getUniqueId(), hits);
        lastDarkHit.put(crown.getUniqueId(), now);
        if (hits >= 5) {
            crown.sendActionBar("§5§lDARK ULT READY §7— activate your ultimate");
            crown.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, crown.getLocation().add(0, 1.2, 0), 12, .3, .5, .3, .02);
        }
    }

    public boolean isDarkUltReady(Player player) { return darkHits.getOrDefault(player.getUniqueId(), 0) >= 5; }
    public void consumeDarkUlt(Player player) { darkHits.put(player.getUniqueId(), 0); lastDarkHit.remove(player.getUniqueId()); }
}
