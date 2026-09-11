package me.baljeetpabla.crownsmp;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class CrownListener implements Listener {

    private final CrownSMP plugin;
    private final CrownManager manager;

    public CrownListener(CrownSMP plugin, CrownManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player damager = getPlayerDamager(event.getDamager());

        if (plugin.isCrowned(victim)) {
            manager.onCrownedDamaged(victim);
        }

        if (damager != null && plugin.isCrowned(damager) && victim != damager) {
            manager.onCrownedHit(damager, victim);
        }
    }

    private Player getPlayerDamager(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof org.bukkit.entity.Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            return source instanceof Player player ? player : null;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExecution(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        manager.tryExecution(event.getPlayer(), target);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        if (killer != null && killer != dead && plugin.isCrowned(killer)) {
            manager.onKill(killer);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrownDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        if (!plugin.isCrowned(dead)) return;

        Player killer = dead.getKiller();
        manager.removeCrownItem(dead);
        event.getDrops().removeIf(manager::isCrownItem);

        // The Crown transfers only when another player personally kills the Crowned player.
        if (killer != null && killer != dead) {
            plugin.setCrown(killer);
            plugin.send(dead, "crown-lost");
            plugin.send(killer, "crown-won");
        } else {
            plugin.removeCrown();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.isCrowned(player)) {
            manager.applyBaseBuffs(player);
            manager.updateCrownItem(player);
        } else {
            manager.cleanUpMarkedPlayer(player);
        }
    }
}
