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
        Player crown = event.getPlayer();
        if (!(event.getRightClicked() instanceof Player target)) return;
        manager.tryExecution(crown, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrownDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        if (!plugin.isCrowned(dead)) return;

        Player killer = dead.getKiller();
        manager.removeCrownItem(dead);

        // The Crown transfers only when another player personally kills the Crowned player.
        // This prevents the Crown from disappearing due to accidental/environmental deaths.
        if (killer != null && killer != dead) {
            plugin.setCrown(killer);
            plugin.send(dead, "crown-lost");
            plugin.send(killer, "crown-won");

            // Do not leave a duplicate Crown item in the death drops.
            event.getDrops().removeIf(manager::isCrownItem);
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
        }
    }
}
