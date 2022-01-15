package com.wasteofplastic.districts;

import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Allows WorldGuard to trump District settings
 * @author tastybento
 *
 */
public class WorldGuardPVPListener implements Listener {
    private Districts plugin;
    
    /**
     * @param plugin
     */
    public WorldGuardPVPListener(Districts plugin) {
	this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void allowPVPEvent(DisallowedPVPEvent event) {
	Utils.logger(3, event.getEventName());
	Player defender = event.getDefender();
	// We only care about the defender
	// Find out where the defender is and if they are in a district that allows PVP then allow it
	DistrictRegion d = plugin.players.getInDistrict(defender.getUniqueId());
	if (d != null && d.getFlag("allowPVP")) {
	    Utils.logger(3, "Overriding WorldGuard PVP!");
	    // Override WorldGuard settings
	    event.setCancelled(true);
	}
    }
  
}
