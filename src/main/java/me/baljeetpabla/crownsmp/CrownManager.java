package me.baljeetpabla.crownsmp;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CrownManager {

    private final CrownSMP plugin;
    private final NamespacedKey crownItemKey;
    private final NamespacedKey crownBuffMarkerKey;
    private final Map<UUID, Integer> comboHits = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastHitAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> rageCooldownUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> executionCooldownUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> rageUntil = new ConcurrentHashMap<>();

    private double maxHealth;
    private int resistanceAmplifier;
    private int speedAmplifier;
    private double knockbackResistance;

    private int rageHitsRequired;
    private long comboTimeoutMs;
    private long rageDurationMs;
    private long rageCooldownMs;
    private int rageStrengthAmplifier;
    private int rageSpeedAmplifier;
    private int rageResistanceAmplifier;

    private int bloodlustRegenerationAmplifier;
    private long bloodlustDurationTicks;

    private long executionCooldownMs;
    private double executionRange;
    private double executionHealthPercent;
    private double executionDamage;

    public CrownManager(CrownSMP plugin) {
        this.plugin = plugin;
        this.crownItemKey = new NamespacedKey(plugin, "crown_item");
        this.crownBuffMarkerKey = new NamespacedKey(plugin, "crown_buffs");
        reloadValues();
    }

    public void reloadValues() {
        maxHealth = plugin.getConfig().getDouble("crown.max-health", 40.0);
        resistanceAmplifier = plugin.getConfig().getInt("crown.resistance-amplifier", 1);
        speedAmplifier = plugin.getConfig().getInt("crown.speed-amplifier", 1);
        knockbackResistance = plugin.getConfig().getDouble("crown.knockback-resistance", 1.0);

        rageHitsRequired = Math.max(1, plugin.getConfig().getInt("rage.hits-required", 5));
        comboTimeoutMs = Math.max(100L, (long) (plugin.getConfig().getDouble("rage.combo-timeout-seconds", 1.75) * 1000L));
        rageDurationMs = Math.max(100L, (long) (plugin.getConfig().getDouble("rage.duration-seconds", 8.0) * 1000L));
        rageCooldownMs = Math.max(0L, (long) (plugin.getConfig().getDouble("rage.cooldown-seconds", 12.0) * 1000L));
        rageStrengthAmplifier = plugin.getConfig().getInt("rage.strength-amplifier", 2);
        rageSpeedAmplifier = plugin.getConfig().getInt("rage.speed-amplifier", 2);
        rageResistanceAmplifier = plugin.getConfig().getInt("rage.resistance-amplifier", 2);

        bloodlustRegenerationAmplifier = plugin.getConfig().getInt("bloodlust.regeneration-amplifier", 1);
        bloodlustDurationTicks = Math.max(1L, (long) (plugin.getConfig().getDouble("bloodlust.duration-seconds", 5.0) * 20L));

        executionCooldownMs = Math.max(0L, (long) (plugin.getConfig().getDouble("execution.cooldown-seconds", 20.0) * 1000L));
        executionRange = Math.max(0.1, plugin.getConfig().getDouble("execution.range", 4.0));
        executionHealthPercent = Math.max(0.0, Math.min(1.0, plugin.getConfig().getDouble("execution.target-max-health-percent", 0.20)));
        executionDamage = Math.max(0.0, plugin.getConfig().getDouble("execution.damage", 10.0));
    }

    public void applyBaseBuffs(Player player) {
        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(maxHealth);
            if (player.getHealth() > maxHealth) {
                player.setHealth(maxHealth);
            }
        }

        AttributeInstance knockback = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(knockbackResistance);
        }

        player.getPersistentDataContainer().set(crownBuffMarkerKey, PersistentDataType.BYTE, (byte) 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, resistanceAmplifier, false, false, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedAmplifier, false, false, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, true), true);
    }

    public void removeCrownBuffs(Player player) {
        removeAllCrownEffects(player);
        player.getPersistentDataContainer().remove(crownBuffMarkerKey);
        comboHits.remove(player.getUniqueId());
        lastHitAt.remove(player.getUniqueId());
        rageUntil.remove(player.getUniqueId());
        rageCooldownUntil.remove(player.getUniqueId());
        executionCooldownUntil.remove(player.getUniqueId());

        AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(20.0);
            if (player.getHealth() > 20.0) {
                player.setHealth(20.0);
            }
        }

        AttributeInstance knockback = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(0.0);
        }
    }

    public void cleanUpMarkedPlayer(Player player) {
        if (!player.getPersistentDataContainer().has(crownBuffMarkerKey, PersistentDataType.BYTE)) return;
        removeCrownBuffs(player);
        removeCrownItem(player);
    }

    private void removeAllCrownEffects(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }

    public void giveCrownItem(Player player) {
        removeCrownItem(player);
        ItemStack crown = createCrownItem();
        EntityEquipment equipment = player.getEquipment();
        if (equipment != null && equipment.getHelmet() == null) {
            equipment.setHelmet(crown);
        } else {
            player.getInventory().addItem(crown).values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
        }

        player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().add(0, 2.0, 0), 20, 0.4, 0.2, 0.4, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_0, 1.0f, 1.25f);
    }

    public ItemStack createCrownItem() {
        ItemStack crown = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = crown.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CrownSMP.color("&6&lCrown"));
            meta.getPersistentDataContainer().set(crownItemKey, PersistentDataType.BYTE, (byte) 1);
            crown.setItemMeta(meta);
        }
        return crown;
    }

    public boolean isCrownItem(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_HELMET || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(crownItemKey, PersistentDataType.BYTE);
    }

    public void updateCrownItem(Player player) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment != null && isCrownItem(equipment.getHelmet())) return;

        for (ItemStack item : player.getInventory().getContents()) {
            if (isCrownItem(item)) return;
        }

        giveCrownItem(player);
    }

    public void removeCrownItem(Player player) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment != null && isCrownItem(equipment.getHelmet())) {
            equipment.setHelmet(null);
        }

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isCrownItem(contents[slot])) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    public void resetCombatState(Player player) {
        UUID uuid = player.getUniqueId();
        comboHits.remove(uuid);
        lastHitAt.remove(uuid);
        rageUntil.remove(uuid);
        rageCooldownUntil.remove(uuid);
        executionCooldownUntil.remove(uuid);
        player.removePotionEffect(PotionEffectType.STRENGTH);
    }

    public void onCrownedHit(Player crown, Player target) {
        UUID uuid = crown.getUniqueId();
        long now = System.currentTimeMillis();

        Long previous = lastHitAt.get(uuid);
        if (previous != null && now - previous > comboTimeoutMs) {
            comboHits.put(uuid, 0);
        }

        int hits = comboHits.getOrDefault(uuid, 0) + 1;
        comboHits.put(uuid, hits);
        lastHitAt.put(uuid, now);

        if (hits >= rageHitsRequired && !isRageActive(crown) && !isRageOnCooldown(crown)) {
            activateRage(crown);
        }
    }

    public void onCrownedDamaged(Player crown) {
        resetCombo(crown);
    }

    private void resetCombo(Player crown) {
        comboHits.put(crown.getUniqueId(), 0);
        lastHitAt.remove(crown.getUniqueId());
    }

    public void activateRage(Player crown) {
        UUID uuid = crown.getUniqueId();
        long now = System.currentTimeMillis();

        comboHits.put(uuid, 0);
        lastHitAt.put(uuid, now);
        rageUntil.put(uuid, now + rageDurationMs);
        rageCooldownUntil.put(uuid, now + rageDurationMs + rageCooldownMs);

        int ticks = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, rageDurationMs / 50L));
        crown.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, rageStrengthAmplifier, false, true, true), true);
        crown.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, rageSpeedAmplifier, false, true, true), true);
        crown.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks, rageResistanceAmplifier, false, true, true), true);

        crown.getWorld().spawnParticle(Particle.FLAME, crown.getLocation().add(0, 1.0, 0), 45, 0.7, 1.0, 0.7, 0.03);
        crown.getWorld().spawnParticle(Particle.CRIT, crown.getLocation().add(0, 1.0, 0), 35, 0.6, 0.8, 0.6, 0.25);
        crown.getWorld().playSound(crown.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f);
        plugin.send(crown, "rage-active");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.isCrowned(crown)) {
                removeRageEffects(crown);
            }
        }, ticks);
    }

    private void removeRageEffects(Player crown) {
        if (!isRageActive(crown)) return;
        rageUntil.remove(crown.getUniqueId());
        crown.removePotionEffect(PotionEffectType.STRENGTH);
        applyBaseBuffs(crown);
    }

    public boolean isRageActive(Player player) {
        Long until = rageUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    private boolean isRageOnCooldown(Player player) {
        Long until = rageCooldownUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public boolean tryExecution(Player crown, Player target) {
        if (!plugin.isCrowned(crown) || target == crown || !crown.isSneaking()) return false;
        if (!crown.getWorld().equals(target.getWorld())) return false;
        if (crown.getLocation().distanceSquared(target.getLocation()) > executionRange * executionRange) return false;
        if (target.isDead() || target.getHealth() <= 0) return false;

        AttributeInstance targetHealth = target.getAttribute(Attribute.MAX_HEALTH);
        double maxTargetHealth = targetHealth != null ? targetHealth.getValue() : 20.0;
        if (target.getHealth() > maxTargetHealth * executionHealthPercent) return false;
        if (isExecutionOnCooldown(crown)) return false;

        executionCooldownUntil.put(crown.getUniqueId(), System.currentTimeMillis() + executionCooldownMs);
        target.damage(executionDamage, crown);

        crown.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0), 30, 0.4, 0.5, 0.4, 0.3);
        crown.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1.0, 0), 3, 0.2, 0.2, 0.2, 0);
        crown.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.7f);
        plugin.send(crown, "execution");
        return true;
    }

    private boolean isExecutionOnCooldown(Player player) {
        Long until = executionCooldownUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public void onKill(Player crown) {
        crown.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,
                (int) Math.min(Integer.MAX_VALUE, bloodlustDurationTicks),
                bloodlustRegenerationAmplifier,
                false, true, true), true);

        crown.getWorld().spawnParticle(Particle.HEART, crown.getLocation().add(0, 1.2, 0), 10, 0.4, 0.6, 0.4, 0.05);
        crown.getWorld().playSound(crown.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
        plugin.send(crown, "bloodlust");
    }
}
