/*******************************************************************************
 * This file is part of Districts.
 *
 *     Districts is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Districts is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Districts.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.wasteofplastic.districts;


import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;


/**
 * Provides protection to districts - handles newer events that may not exist in older servers
 */
public class DistrictGuardNew implements Listener {
    private final Districts plugin;

    protected DistrictGuardNew(final Districts plugin) {
	this.plugin = plugin;

    }

    /**
     * Handle interaction with armor stands V1.8
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractAtEntityEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	if (e.getRightClicked() == null) {
	    return;
	}
	// Get the district that this block is in (if any)
	DistrictRegion d = plugin.getInDistrict(e.getRightClicked().getLocation());
	//DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}

	if (e.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
	    //plugin.getLogger().info("DEBUG: Armor stand clicked off island");
	    if (!d.getAllowChestAccess(e.getPlayer().getUniqueId())) {
	    	Player p = e.getPlayer();
	    	if (Districts.getPlugin().placeholderapi) {
				e.getPlayer().sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p,Locale.errordistrictProtected));
			} else {
				e.getPlayer().sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
	    	e.setCancelled(true);
		return; 
	    }
	}
    }
    // Armor stand events
    @EventHandler(priority = EventPriority.LOWEST)
    void placeArmorStandEvent(PlayerInteractEvent e){
	Utils.logger(3,e.getEventName());
	Player p = e.getPlayer();
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(p.getWorld().getName())) {
	    return;
	}
	// Get the district that this block is in (if any)
	DistrictRegion d = plugin.getInDistrict(e.getPlayer().getLocation());
	//DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	// Check if they are holding armor stand
	ItemStack inHand = e.getPlayer().getItemInHand();
	if (inHand != null && inHand.getType().equals(Material.ARMOR_STAND)) {
	    //plugin.getLogger().info("DEBUG: stand place cancelled");
	    if (!d.getAllowPlaceBlocks(e.getPlayer().getUniqueId())) {
	    	Player ap = e.getPlayer();
	    	if (Districts.getPlugin().placeholderapi) {
				e.getPlayer().sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(ap, Locale.errordistrictProtected));
			} else {
				e.getPlayer().sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		e.setCancelled(true);
		e.getPlayer().updateInventory();
	    }
	}

    }

    @EventHandler(priority = EventPriority.LOW)
    public void ArmorStandDestroy(EntityDamageByEntityEvent e){
	Utils.logger(3,e.getEventName());
	if (!(e.getEntity() instanceof LivingEntity)) {
	    return;
	}
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getEntity().getWorld().getName())) {
	    return;
	}
	final LivingEntity livingEntity = (LivingEntity)e.getEntity();
	if(!livingEntity.getType().equals(EntityType.ARMOR_STAND)){
	    return;
	}
	if(e.getDamager() instanceof Player){
	    Player p = (Player) e.getDamager();
	    if (p.isOp()) {
		return;
	    }
		// Get the district that this block is in (if any)
		DistrictRegion d = plugin.getInDistrict(e.getEntity().getLocation());
		//DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
		if (d == null) {
		    // Not in a district
		    return;
		}
		if (!d.getAllowBreakBlocks(p.getUniqueId())) {
			if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		}
	}
    }

}


