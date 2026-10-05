package me.baljeetpabla.crownsmp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class CrownListener implements Listener {
    private final CrownManager crowns;
    public CrownListener(CrownSMP plugin, CrownManager crowns) { this.crowns = crowns; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player) {
            crowns.onDarkCrownHit(attacker);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrownDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        boolean light = crowns.isLightCrownHolder(dead);
        boolean dark = crowns.isDarkCrownHolder(dead);
        if (!light && !dark) return;
        Player killer = dead.getKiller();
        if (killer != null && killer != dead) {
            if (light) crowns.setLightCrown(killer); else crowns.setDarkCrown(killer);
        } else {
            if (light) crowns.removeLightCrown(); else crowns.removeDarkCrown();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (crowns.isCrownHolder(player)) {
            crowns.applyBaseEffects(player);
            crowns.ensureCrownHelmet(player);
        }
    }
}
