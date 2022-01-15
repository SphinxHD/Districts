package com.wasteofplastic.districts;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import jdk.vm.ci.meta.Local;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ExactMatchConversationCanceller;
import org.bukkit.conversations.InactivityConversationCanceller;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;

/**
 * Provides protection to islands
 * @author tastybento
 */
public class DistrictGuard implements Listener {
    private final Districts plugin;

    public DistrictGuard(final Districts plugin) {
	this.plugin = plugin;

    }

    // Vehicle damage
    @EventHandler(priority = EventPriority.LOW)
    void vehicleDamageEvent(VehicleDamageEvent e){
	Utils.logger(3,e.getEventName());

	if (e.getVehicle() instanceof Boat) {
	    // Boats can always be hit
	    return;
	}
	if (!(e.getAttacker() instanceof Player)) {
	    return;
	}
	Player p = (Player)e.getAttacker();

	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(p.getWorld().getName())) {
	    return;
	}
	if (p.isOp()) {
	    // You can do anything if you are Op
	    return;
	}
	// Get the district that this block is in (if any)
	DistrictRegion d = plugin.getInDistrict(e.getVehicle().getLocation());
	//DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null) {
	    // Not in a district
	    return;
	}
	if (!d.getAllowBreakBlocks(p.getUniqueId())) {
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
	    e.setCancelled(true);
	}

    }



    /**
     * Tracks player movement
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
	Utils.logger(3,event.getEventName());
	Player player = event.getPlayer();
	World world = player.getWorld();
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(world.getName())) {
	    return;
	}
	if (player.getVehicle() != null) {
	    return; // handled in vehicle listener
	}
	// Check if the player has a compass in their hand
	ItemStack holding = player.getItemInHand();
	if (holding != null) {
	    if (holding.getType().equals(Material.COMPASS)) {
		Location closest = plugin.getClosestDistrict(player);
		if (closest != null) {
		    player.setCompassTarget(closest);
		    Utils.logger(2,"DEBUG: Compass " + closest.getBlockX() + "," + closest.getBlockZ());
		}
	    }
	}
	// Did we move a block? Only check in x and z
	if (event.getFrom().getBlockX() != event.getTo().getBlockX()
		|| event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
	    boolean result = checkMove(player, event.getFrom(), event.getTo());
	    if (result) {
		Location newLoc = event.getFrom();
		newLoc.setX(newLoc.getBlockX() + 0.5);
		newLoc.setY(newLoc.getBlockY());
		newLoc.setZ(newLoc.getBlockZ() + 0.5);
		event.setTo(newLoc);
	    }
	}
	/*
	if (!plugin.getPos1s().containsKey(player.getUniqueId())) {
	    // Check if visualizations are turned on for this player
	    if (plugin.players.getVisualize(player.getUniqueId())) {
		// If the player has visualizations running, then
		if (plugin.getVisualizations().containsKey(player.getUniqueId())) {
		    return;
		}
		DistrictRegion d = plugin.players.getInDistrict(player.getUniqueId());
		if (d != null) {
		    Visualization.visualize(d,player);
		} else {
		    plugin.logger(2,"Removing viz during move event");
		    Visualization.devisualize(player);
		}
	    } else {
		if (plugin.getVisualizations().containsKey(player.getUniqueId())) {
		    Visualization.devisualize(player);
		}
	    }
	}*/
	// Check if they are wielding a golden hoe
	if (player.getItemInHand() != null) {
	    Utils.logger(2,"Item in hand");
	    if (!player.getItemInHand().getType().equals(Material.GOLDEN_HOE)) {
		// no longer holding a golden hoe
		Utils.logger(2,"No longer holding hoe");
		if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		    // Remove the point
		    player.sendMessage(ChatColor.GOLD + "Cancelling district mark");
		    plugin.getPos1s().remove(player.getUniqueId());
		}
	    }
	} else {
	    // Empty hand
	    if (plugin.getPos1s().containsKey(player.getUniqueId())) {
		// Remove the point
		player.sendMessage(ChatColor.GOLD + "Cancelling district mark");
		plugin.getPos1s().remove(player.getUniqueId());
	    }
	}
    }


    /**
     * @param player
     * @param from
     * @param to
     * @return false if the player can move into that area, true if not allowed
     */
    private boolean checkMove(Player player, Location from, Location to) {
	DistrictRegion fromDistrict = plugin.getGrid(from.getWorld().getName()).getDistrictRegionAt(from);
	DistrictRegion toDistrict = plugin.getGrid(to.getWorld().getName()).getDistrictRegionAt(to);
	if (plugin.getDistricts().isEmpty()) {
	    // No districts yet
	    return false;
	}
	Utils.logger(2,"Checking districts");
	Utils.logger(2,"From : " + from.toString());
	Utils.logger(2,"From: " + from.getBlockX() + "," + from.getBlockZ());
	Utils.logger(2,"To: " + to.getBlockX() + "," + to.getBlockZ());
	// If player is trying to make a district, then we need to check if the proposed district overlaps with any others
	if (plugin.getPos1s().containsKey(player.getUniqueId())) {
	    //plugin.getLogger().info("DEBUG: trying to make a district");
	    // Just check their current location
	    if (plugin.getGrid(player.getLocation().getWorld().getName()).districtAtLocation(player.getLocation())) {
		player.sendMessage(ChatColor.RED + "Districts cannot overlap!");
	    }
	}
	// No district interaction
	if (fromDistrict == null && toDistrict == null) {
	    // Clear the district flag (the district may have been deleted while they were offline)
	    plugin.players.setInDistrict(player.getUniqueId(), null);
	    return false;	    
	} else if (fromDistrict == toDistrict) {
	    // Set the district - needs to be done if the player teleports too (should be done on a teleport event)
	    plugin.players.setInDistrict(player.getUniqueId(), toDistrict);
	    // Check player's visualization setting
	    if (plugin.players.getVisualize(player.getUniqueId())) {
		Visualization.visualize(toDistrict, player);
	    }
	    return false;
	}
	if (fromDistrict != null && toDistrict == null) {
	    // leaving a district
	    if (!fromDistrict.getFarewellMessage().isEmpty()) {
	    	if (Settings.actionbarDistmessage) {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(fromDistrict.getFarewellMessage()));
			} else {
				player.sendMessage(fromDistrict.getFarewellMessage());
			}
	    }
	    // Stop visualization
	    Visualization.devisualize(player);
	    plugin.players.setInDistrict(player.getUniqueId(), null);
	} else if (fromDistrict == null && toDistrict != null){
	    // Going into a district
	    if (!toDistrict.getEnterMessage().isEmpty()) {
			if (Settings.actionbarDistmessage) {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(toDistrict.getEnterMessage()));
			} else {
				player.sendMessage(toDistrict.getEnterMessage());
			}
	    }
	    // Check player's visualization setting
	    if (plugin.players.getVisualize(player.getUniqueId())) {
		Visualization.visualize(toDistrict, player);
	    }
	    if (VaultHelper.checkPerm(player, "districts.advancedplayer")) {
		if (toDistrict.isForSale()) {
		    player.sendMessage("This district is for sale for " + VaultHelper.econ.format(toDistrict.getPrice()) + "!");
		} else if (toDistrict.isForRent() && toDistrict.getRenter() == null) {
		    player.sendMessage("This district is for rent for " + VaultHelper.econ.format(toDistrict.getPrice()) + " per week.");
		} 
	    }
	    plugin.players.setInDistrict(player.getUniqueId(), toDistrict);	    

	} else if (fromDistrict != null && toDistrict != null){
	    // Leaving one district and entering another district
	    if (!fromDistrict.getFarewellMessage().isEmpty()) {
			if (Settings.actionbarDistmessage) {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(fromDistrict.getFarewellMessage()));
			} else {
				player.sendMessage(fromDistrict.getFarewellMessage());
			}
	    }
	    // Check player's visualization setting
	    if (plugin.players.getVisualize(player.getUniqueId())) {
		Visualization.visualize(toDistrict, player);
	    } 
	    if (!toDistrict.getEnterMessage().isEmpty()) {
			if (Settings.actionbarDistmessage) {
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(toDistrict.getEnterMessage()));
			} else {
				player.sendMessage(toDistrict.getEnterMessage());
			}
	    }
	    if (VaultHelper.checkPerm(player, "districts.advancedplayer")) {
		if (toDistrict.isForSale()) {
		    player.sendMessage("This district is for sale for " + VaultHelper.econ.format(toDistrict.getPrice()) + "!");
		} else if (toDistrict.isForRent()) {
		    player.sendMessage("This district is for rent for " + VaultHelper.econ.format(toDistrict.getPrice()) + "!");
		}
	    }
	    plugin.players.setInDistrict(player.getUniqueId(), toDistrict);	    
	}  
	return false;
    }


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onClick(PlayerInteractEvent e) {
	Utils.logger(3,e.getEventName());
	// Find out who is doing the clicking
	final Player p = e.getPlayer();
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(p.getWorld().getName())) {
	    return;
	}
	final UUID playerUUID = p.getUniqueId();
	// Get the item in their hand
	ItemStack itemInHand = p.getItemInHand();
	if (itemInHand == null || !itemInHand.getType().equals(Material.GOLDEN_HOE)) {
	    Utils.logger(2,"No hoe");
	    return;
	}
	if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
	    if (plugin.getPos1s().containsKey(p.getUniqueId())) {
		// Remove the point
		p.sendMessage(ChatColor.GOLD + "Cancelling last position");
		plugin.getPos1s().remove(p.getUniqueId());
		e.setCancelled(true);
		return;
	    }
	}
	// Fast return if this is not a left click
	if (e.getAction() != Action.LEFT_CLICK_BLOCK)
	    return;

	// Find out what block is being clicked
	final Block b = e.getClickedBlock();
	if (b == null) {
	    Utils.logger(2,"No block");
	    return;
	}
	DistrictRegion currentDistrict = plugin.players.getInDistrict(playerUUID);
	if (currentDistrict != null) {
	    p.sendMessage(ChatColor.RED + "You cannot do that here. You are in a district!");
	    if (currentDistrict.getOwner().equals(playerUUID)) {
		p.sendMessage(ChatColor.RED + "To remove this district type /d remove");
	    }
	    e.setCancelled(true);
	    return;
	}

	if (plugin.getPos1s().containsKey(playerUUID)) {
	    Location origin = plugin.getPos1s().get(playerUUID);
	    Location to = b.getLocation();
	    // Check for overlapping districts (you can reach with the hoe)
	    for (DistrictRegion d : plugin.getDistricts()) {
		//plugin.getLogger().info("DEBUG: district info pos1 " + d.getPos1().getBlockX() + "," + d.getPos1().getBlockZ());
		//plugin.getLogger().info("DEBUG: district info pos2 " + d.getPos1().getBlockX() + "," + d.getPos1().getBlockZ());
		// Check the advancing lines
		for (int x = Math.min(to.getBlockX(),origin.getBlockX()); x <= Math.max(to.getBlockX(),origin.getBlockX()); x++) {
		    if (d.intersectsDistrict(new Location(to.getWorld(),x,0,to.getBlockZ()))) {
			//plugin.getLogger().info("DEBUG: overlap found in x at " + x + "," + to.getBlockZ());
			p.sendMessage(ChatColor.RED + "Districts cannot overlap!");
			e.setCancelled(true);
			return;	
		    }
		}
		for (int z = Math.min(to.getBlockZ(),origin.getBlockZ()); z <= Math.max(to.getBlockZ(),origin.getBlockZ()); z++) {
		    if (d.intersectsDistrict(new Location(to.getWorld(),to.getBlockX(),0,z))) {
			//plugin.getLogger().info("DEBUG: overlap found in z at " + to.getBlockX() + "," + z);
			p.sendMessage(ChatColor.RED + "Districts cannot overlap!");
			e.setCancelled(true);
			return;	
		    }
		}
	    }
	    // If they hit the same place twice
	    if (to.getBlockX() == origin.getBlockX() && to.getBlockZ()==origin.getBlockZ()) {
		p.sendMessage("Setting position 1 : " + to.getBlockX() + ", " + to.getBlockZ());
		p.sendMessage("Click on the opposite corner of the district");
		Visualization.visualize(b.getLocation(),p);
		e.setCancelled(true);
		return;
	    }
	    Location pos = plugin.getPos1s().get(playerUUID);
	    // Check the player has enough blocks
	    // TODO
	    // Check minimum size
	    int side1 = Math.abs(to.getBlockX()-pos.getBlockX()) + 1;
	    //p.sendMessage("Side 1 is " + side1);
	    int side2 = Math.abs(to.getBlockZ()-pos.getBlockZ()) + 1;
	    //p.sendMessage("Side 2 is " + side2);
	    if (side1 < 5 || side2 < 5) {
		p.sendMessage("Minimum district size is 5 x 5");
		e.setCancelled(true);
		return;		
	    }
	    int balance = plugin.players.removeBlocks(playerUUID, (side1*side2));
	    if (balance < 0) {
	    	if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.notenoughblocks));
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.conversationsblocksrequired.replace("[number]", String.valueOf(Math.abs(balance)))));
			} else {
				p.sendMessage(ChatColor.RED + Locale.notenoughblocks);
				p.sendMessage(ChatColor.RED + Locale.conversationsblocksrequired.replace("[number]", String.valueOf(Math.abs(balance))));
			}
		e.setCancelled(true);
		return;		
	    } else {
		p.sendMessage("Setting position 2 : " + to.getBlockX() + ", " + to.getBlockZ());
		if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p,Locale.conversationsyounowhave.replace("[number]", String.valueOf(balance))));

		} else {
			p.sendMessage(ChatColor.GREEN + Locale.conversationsyounowhave.replace("[number]", String.valueOf(balance)));
		}
	    }
	    //p.sendMessage("Position 1 : " + pos.getBlockX() + ", " + pos.getBlockZ());
	    //p.sendMessage("Position 2 : " + to.getBlockX() + ", " + to.getBlockZ());
	    if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(PlaceholderAPI.setPlaceholders(p, Locale.conversationsdistrictcreated));
		} else {
	    	p.sendMessage(Locale.conversationsdistrictcreated);
		}
	    plugin.createNewDistrict(pos, b.getLocation(), p);
	    e.setCancelled(true);
	} else {
	    // Check if this position is in a district
	    if (plugin.getGrid(b.getLocation().getWorld().getName()).districtAtLocation(b.getLocation())) {
		p.sendMessage(ChatColor.RED + "That spot is in a district!");
		if (plugin.getGrid(b.getLocation().getWorld().getName()).getDistrictRegionAt(b.getLocation()).getOwner().equals(playerUUID)) {
		    p.sendMessage(ChatColor.RED + "To remove that district go into it and type /d remove");
		}
		e.setCancelled(true);
		return;
	    }
	    plugin.getPos1s().put(playerUUID, b.getLocation());
	    p.sendMessage("Setting position 1 : " + b.getLocation().getBlockX() + ", " + b.getLocation().getBlockZ());
	    p.sendMessage("Click on the opposite corner of the district");
	    // Start the visualization in a bit
	    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
		@Override
		public void run() {
		    Visualization.visualize(b.getLocation(),p);
		}
	    }, 10L);
	    e.setCancelled(true);
	}

    }


    /**
     * Prevents blocks from being broken
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(final BlockBreakEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// Get the district that this block is in (if any)
	DistrictRegion d = plugin.getInDistrict(e.getBlock().getLocation());
	//DistrictRegion d = plugin.players.getInDistrict(e.getPlayer().getUniqueId());
	if (d == null || e.getPlayer().isOp()) {
	    // Not in a district
	    return;
	}
	if (!d.getAllowBreakBlocks(e.getPlayer().getUniqueId())) {
		Player p = e.getPlayer();
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
	    e.setCancelled(true);
	}
    }

    /**
     * This method protects players from PVP if it is not allowed and from arrows fired by other players
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getEntity().getWorld().getName())) {
	    return;
	}
	// Get the district that this block is in (if any)
	DistrictRegion d = plugin.getInDistrict(e.getEntity().getLocation());
	if (d == null) {
	    Utils.logger(2,"Not in a district");
	    return;	    
	}
	Utils.logger(2,"D is something " + d.getEnterMessage());
	// Ops can do anything
	if (e.getDamager() instanceof Player) {
	    if (((Player)e.getDamager()).isOp()) {
		return;
	    }
	}
	// Check to see if it's an item frame
	if (e.getEntity() instanceof ItemFrame) {
	    if (e.getDamager() instanceof Player) {
		if (!d.getAllowBreakBlocks(e.getDamager().getUniqueId())) {
			Player dp = (Player) e.getDamager();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				dp.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(dp, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				dp.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(dp, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				dp.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				dp.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return;
		}
	    } else if (e.getDamager() instanceof Projectile) {
		// Prevent projectiles shot by players from removing items from frames
		Projectile p = (Projectile)e.getDamager();
		if (p.getShooter() instanceof Player) {
		    if (!d.getAllowBreakBlocks(((Player)p.getShooter()).getUniqueId())) {
		    	Player sp = (Player) p.getShooter();
				if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
					sp.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(sp, Locale.errordistrictProtected)));
				} else if (Districts.getPlugin().placeholderapi) {
					sp.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(sp, Locale.errordistrictProtected));
				}
				if (Settings.actionbarProtection) {
					sp.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
				} else {
					sp.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
				}
			e.setCancelled(true);
			return;
		    }		    
		}
	    } 

	}
	// If the attacker is non-human and not an arrow then everything is okay
	if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) {
	    return;
	}
	Utils.logger(2,"Entity is " + e.getEntity().toString());
	// Check for player initiated damage
	if (e.getDamager() instanceof Player) {
	    Utils.logger(2,"Damager is " + ((Player)e.getDamager()).getName());
	    // If the target is not a player check if mobs can be hurt
	    if (!(e.getEntity() instanceof Player)) {
		if (e.getEntity() instanceof Monster) {
		    Utils.logger(2,"Entity is a monster - ok to hurt"); 
		    return;
		} else {
		    Utils.logger(2,"Entity is a non-monster - check if ok to hurt"); 
		    UUID playerUUID = e.getDamager().getUniqueId();
		    if (playerUUID == null) {
			Utils.logger(2,"player ID is null");
		    }
		    if (!d.getAllowHurtMobs(playerUUID)) {
		    	Player dp = (Player) e.getDamager();
				if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
					dp.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(dp, Locale.errordistrictProtected)));
				} else if (Districts.getPlugin().placeholderapi) {
					dp.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(dp, Locale.errordistrictProtected));
				}
				if (Settings.actionbarProtection) {
					dp.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
				} else {
					dp.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
				}
			e.setCancelled(true);
			return;
		    }
		    return;
		}
	    } else {
		// PVP
		// If PVP is okay then return
		// Target is in a district
		if (d.getAllowPVP()) {
		    Utils.logger(2,"PVP allowed");
		    return;
		}
		Utils.logger(2,"PVP not allowed");

	    }
	}

	Utils.logger(2,"Player attack (or arrow)");
	// Only damagers who are players or arrows are left
	// If the projectile is anything else than an arrow don't worry about it in this listener
	// Handle splash potions separately.
	if (e.getDamager() instanceof Arrow) {
	    Utils.logger(2,"Arrow attack");
	    Arrow arrow = (Arrow)e.getDamager();
	    // It really is an Arrow
	    if (arrow.getShooter() instanceof Player) {
		Player shooter = (Player)arrow.getShooter();
		Utils.logger(2,"Player arrow attack");
		if (e.getEntity() instanceof Player) {
		    Utils.logger(2,"Player vs Player!");
		    // Arrow shot by a player at another player
		    if (!d.getAllowPVP()) {
			Utils.logger(2,"Target player is in a no-PVP district!");
			((Player)arrow.getShooter()).sendMessage("Target is in a no-PVP district!");
			e.setCancelled(true);
			return;
		    } 
		} else {
		    if (!(e.getEntity() instanceof Monster)) {
			Utils.logger(2,"Entity is a non-monster - check if ok to hurt"); 
			UUID playerUUID = shooter.getUniqueId();
			if (!d.getAllowHurtMobs(playerUUID)) {
				if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
					shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(shooter, Locale.errordistrictProtected)));
				} else if (Districts.getPlugin().placeholderapi) {
					shooter.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(shooter, Locale.errordistrictProtected));
				}
				if (Settings.actionbarProtection) {
					shooter.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
				} else {
					shooter.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
				}
				e.setCancelled(true);
			    return;
			}
			return;
		    }
		}
	    }
	} else if (e.getDamager() instanceof Player){
	    Utils.logger(2,"Player attack");
	    // Just a player attack
	    if (!d.getAllowPVP()) {
		((Player)e.getDamager()).sendMessage("Target is in a no-PVP district!");
		e.setCancelled(true);
		return;
	    } 
	}
	return;
    }


    /**
     * Prevents placing of blocks
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
	Utils.logger(3,e.getEventName());
	//plugin.getLogger().info("DEBUG: place block event called");
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	//plugin.getLogger().info("DEBUG: correct world");
	// If the offending block is not in a district, forget it!
	DistrictRegion d = plugin.getInDistrict(e.getBlock().getLocation());
	if (d == null) {
	    //plugin.getLogger().info("DEBUG: district is null!");
	    return;
	}
	if (!d.getAllowPlaceBlocks(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
		Player p = e.getPlayer();
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
	    e.setCancelled(true);
	}

    }
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBlockPlace(final HangingPlaceEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending block is not in a district, forget it!
	DistrictRegion d = plugin.getInDistrict(e.getBlock().getLocation());
	if (d == null) {
	    return;
	}
	if (!d.getAllowPlaceBlocks(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
		Player p = e.getPlayer();
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
		e.setCancelled(true);
	}
    }

    // Prevent sleeping in other beds
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending bed is not in a district, forget it!
	DistrictRegion d = plugin.getInDistrict(e.getBed().getLocation());
	if (d == null) {
	    return;
	}
	if (!d.getAllowBedUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
		Player p = e.getPlayer();
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
	    e.setCancelled(true);
	}
    }
    /**
     * Prevents the breakage of hanging items
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getRemover().getWorld().getName())) {
	    return;
	}
	if (!(e.getRemover() instanceof Player)) {
	    // Enderman?
	    return;
	}
	// If the offending item is not in a district, forget it!
	DistrictRegion d = plugin.getInDistrict(e.getEntity().getLocation());
	if (d == null) {
	    return;
	}
	Player p = (Player)e.getRemover();
	if (!d.getAllowBreakBlocks(e.getRemover().getUniqueId()) && !p.isOp()) {
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
		e.setCancelled(true);
	}
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending item is not in a district, forget it!
	DistrictRegion d = plugin.getInDistrict(e.getBlockClicked().getLocation());
	if (d == null) {
	    return;
	}
	if (!d.getAllowBucketUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
		Player p = e.getPlayer();
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
	    e.setCancelled(true);
	}
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(final PlayerBucketFillEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending item is not in a district, forget it!
	DistrictRegion d = plugin.getInDistrict(e.getBlockClicked().getLocation());
	if (d == null) {
	    return;
	}

	if (!d.getAllowBucketUse(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) {
		Player p = e.getPlayer();
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
	    e.setCancelled(true);
	}
    }

    // Protect sheep
    @EventHandler(priority = EventPriority.LOW)
    public void onShear(final PlayerShearEntityEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// If the offending item is not in a district, forget it!
	DistrictRegion d = plugin.getInDistrict(e.getEntity().getLocation());
	if (d == null) {
	    return;
	}
	if (!d.getAllowShearing(e.getPlayer().getUniqueId())) {
		Player p = e.getPlayer();
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
	    e.setCancelled(true);
	}
    }

    // Stop lava flow or water into a district
    @EventHandler(priority = EventPriority.LOW)
    public void onFlow(final BlockFromToEvent e) {
	Utils.logger(3,e.getEventName());
	// Flow may be allowed anyway
	if (Settings.allowFlowIn && Settings.allowFlowOut) {
	    return;
	}
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getBlock().getWorld().getName())) {
	    return;
	}
	// Only check lateral movement
	if (e.getBlock().getLocation().getBlockX() == e.getToBlock().getLocation().getBlockX()
		&& e.getBlock().getLocation().getBlockZ() == e.getToBlock().getLocation().getBlockZ()) {
	    return;
	}
	// Ignore flows within flow
	if (e.getBlock().getType().equals(e.getToBlock().getType())) {
	    return;
	}
	// Ignore stationary to non-stationary
	if (e.getBlock().getType().equals(Material.LEGACY_STATIONARY_WATER) && e.getToBlock().getType().equals(Material.WATER) ) {
	    return;
	}
	if (e.getBlock().getType().equals(Material.LEGACY_STATIONARY_LAVA) && e.getToBlock().getType().equals(Material.LAVA) ) {
	    return;
	}
	    // Get To and From Districts
	//plugin.getLogger().info("DEBUG: " + e.getBlock().getType() + " to " + e.getToBlock().getType());
	DistrictRegion to = plugin.getInDistrict(e.getToBlock().getLocation());
	DistrictRegion from = plugin.getInDistrict(e.getBlock().getLocation());
	
	// Scenarios
	// 1. inside district or outside - always ok
	// 2. inside to outside - allowFlowOut determines
	// 3. outside to inside - allowFlowIn determines
	if (to == null && from == null) {
	    return;
	}
	if (to !=null && from != null && to.equals(from)) {
	    return;
	}
	// to or from or both are districts, NOT the same and flow is across a boundary
	// if to is a district, flow in is allowed 
	if (to != null && Settings.allowFlowIn) {
	    return;
	}
	// if from is a district, flow may allowed
	if (from != null && Settings.allowFlowOut) {
	    return;
	}
	// Otherwise cancel - the flow is not allowed
	e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractEntityEvent e) {
	Utils.logger(3,e.getEventName());
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	Utils.logger(3,"Frame right click!");
	Entity entity = e.getRightClicked();
	Utils.logger(3,entity.getType().toString());
	if (entity.getType() != EntityType.ITEM_FRAME) {
	    return;
	}
	ItemFrame frame = (ItemFrame)entity;
	if ((frame.getItem() == null || frame.getItem().getType() == Material.AIR)) {
	    Utils.logger(3,"Nothing in frame!");
	    return;
	}
	// If the offending item is not in a district, forget it!
	DistrictRegion d = plugin.getInDistrict(entity.getLocation());
	if (d == null) {
	    Utils.logger(3,"Not in a district!");
	    return;
	}
	if (!d.getAllowChestAccess(e.getPlayer().getUniqueId())) {
		Player p = e.getPlayer();
		if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
		} else if (Districts.getPlugin().placeholderapi) {
			p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
		}
		if (Settings.actionbarProtection) {
			p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
		} else {
			p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
		}
	    e.setCancelled(true);
	}
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(final PlayerInteractEvent e) {
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getPlayer().getWorld().getName())) {
	    return;
	}
	// Check for disallowed clicked blocks
	if (e.getClickedBlock() != null) {
	    // If the offending item is not in a district, forget it!
	    DistrictRegion d = plugin.getInDistrict(e.getClickedBlock().getLocation());
	    if (d == null) {
		return;
	    }

	    Utils.logger(2,"DEBUG: clicked block " + e.getClickedBlock());
	    Utils.logger(2,"DEBUG: Material " + e.getMaterial());

	    switch (e.getClickedBlock().getType()) {
		case CRIMSON_DOOR:
		case WARPED_DOOR:
		case OAK_DOOR:
	    case SPRUCE_DOOR:
	    case ACACIA_DOOR:
	    case DARK_OAK_DOOR:
	    case BIRCH_DOOR:
	    case JUNGLE_DOOR:
		case CRIMSON_TRAPDOOR:
		case WARPED_TRAPDOOR:
		case OAK_TRAPDOOR:
		case SPRUCE_TRAPDOOR:
		case ACACIA_TRAPDOOR:
		case DARK_OAK_TRAPDOOR:
		case BIRCH_TRAPDOOR:
		case JUNGLE_TRAPDOOR:
		if (!d.getAllowDoorUse(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
		case CRIMSON_FENCE_GATE:
		case WARPED_FENCE_GATE:
	    case OAK_FENCE_GATE:
	    case SPRUCE_FENCE_GATE:
	    case ACACIA_FENCE_GATE:
	    case DARK_OAK_FENCE_GATE:
	    case BIRCH_FENCE_GATE:
	    case JUNGLE_FENCE_GATE:
		if (!d.getAllowGateUse(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return;  
		}
		break;
	    case CHEST:
	    case TRAPPED_CHEST:
	    case ENDER_CHEST:
	    case DISPENSER:
	    case DROPPER:
	    case HOPPER:
	    case HOPPER_MINECART:
		case CHEST_MINECART:
	    case WHITE_SHULKER_BOX:
	    case ORANGE_SHULKER_BOX:
	    case MAGENTA_SHULKER_BOX:
	    case LIGHT_BLUE_SHULKER_BOX:
	    case YELLOW_SHULKER_BOX:
	    case LIME_SHULKER_BOX:
	    case PINK_SHULKER_BOX:
	    case GRAY_SHULKER_BOX:
		case LIGHT_GRAY_SHULKER_BOX:
	    case CYAN_SHULKER_BOX:
	    case PURPLE_SHULKER_BOX:
	    case BLUE_SHULKER_BOX:
	    case BROWN_SHULKER_BOX:
	    case GREEN_SHULKER_BOX:
	    case RED_SHULKER_BOX:
	    case BLACK_SHULKER_BOX:
		case SHULKER_BOX:
		if (!d.getAllowChestAccess(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
		case FARMLAND:
		if (!d.getAllowCropTrample(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case BREWING_STAND:
	    case CAULDRON:
		if (!d.getAllowBrewing(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
		case CAKE:
		case CANDLE_CAKE:
		case BLACK_CANDLE_CAKE:
		case BLUE_CANDLE_CAKE:
		case BROWN_CANDLE_CAKE:
		case CYAN_CANDLE_CAKE:
		case GRAY_CANDLE_CAKE:
		case GREEN_CANDLE_CAKE:
		case LIGHT_GRAY_CANDLE_CAKE:
		case LIGHT_BLUE_CANDLE_CAKE:
		case LIME_CANDLE_CAKE:
		case MAGENTA_CANDLE_CAKE:
		case ORANGE_CANDLE_CAKE:
		case PINK_CANDLE_CAKE:
		case PURPLE_CANDLE_CAKE:
		case RED_CANDLE_CAKE:
		case WHITE_CANDLE_CAKE:
		case YELLOW_CANDLE_CAKE:
		break;
		case REPEATER:
	    case COMPARATOR:
		if (!d.getAllowRedStone(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
		case ENCHANTING_TABLE:
		break;
	    case FURNACE:
		case BLAST_FURNACE:
		case SMOKER:
		if (!d.getAllowFurnaceUse(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case ICE:
		break;
	    case ITEM_FRAME:
		if (!d.getAllowPlaceBlocks(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case JUKEBOX:
	    case NOTE_BLOCK:
		if (!d.getAllowMusic(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
	    case PACKED_ICE:
		break;
	    case STONE_BUTTON:
		case POLISHED_BLACKSTONE_BUTTON:
		case CRIMSON_BUTTON:
		case WARPED_BUTTON:
		case OAK_BUTTON:
		case SPRUCE_BUTTON:
		case ACACIA_BUTTON:
		case DARK_OAK_BUTTON:
		case BIRCH_BUTTON:
		case JUNGLE_BUTTON:
	    case LEVER:
		if (!d.getAllowLeverButtonUse(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}	
		break;
	    case TNT:
		break;
		case CRAFTING_TABLE:
		if (!d.getAllowCrafting(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		    return; 
		}
		break;
	    default:
		break;
	    }
	}
	// Check for disallowed in-hand items
	if (e.getMaterial() != null) {
	    // If the player is not in a district, forget it!
	    DistrictRegion d = plugin.getInDistrict(e.getPlayer().getLocation());
	    if (d == null) {
		return;
	    }

	    if (e instanceof Boat && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
		// Trying to put a boat on non-liquid
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		e.setCancelled(true);
		return;
	    }
	    if (e.getMaterial().equals(Material.ENDER_PEARL)) {
		if (!d.getAllowEnderPearls(e.getPlayer().getUniqueId())) {
			Player p = e.getPlayer();
			if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected)));
			} else if (Districts.getPlugin().placeholderapi) {
				p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errordistrictProtected));
			}
			if (Settings.actionbarProtection) {
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
			} else {
				p.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
			}
		    e.setCancelled(true);
		}
		return;
	    } else if (e.getMaterial().equals(Material.POTION) && e.getItem().getDurability() != 0) {
		// Potion
		Utils.logger(2,"DEBUG: potion");
		try {
		    Potion p = Potion.fromItemStack(e.getItem());
		    if (!p.isSplash()) {
			Utils.logger(2,"DEBUG: not a splash potion");
			return;
		    } else {
			// Splash potions are allowed only if PVP is allowed
			if (!d.getAllowPVP()) {
				Player ap = e.getPlayer();
				if (Settings.actionbarProtection && Districts.getPlugin().placeholderapi) {
					ap.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + PlaceholderAPI.setPlaceholders(ap, Locale.errordistrictProtected)));
				} else if (Districts.getPlugin().placeholderapi) {
					ap.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(ap, Locale.errordistrictProtected));
				}
				if (Settings.actionbarProtection) {
					ap.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + Locale.errordistrictProtected));
				} else {
					ap.sendMessage(ChatColor.RED + Locale.errordistrictProtected);
				}
			    e.setCancelled(true);
			}
		    }
		} catch (Exception ex) {
		}
	    }
	    // Everything else is okay
	}
    }
    // Check for inventory clicking (Info Panel)
    @EventHandler(priority = EventPriority.LOW)
    public void onInfoPanelClick(final InventoryClickEvent e) {
	// Check that it is a control panel
	Inventory panel = e.getInventory();
	String title = e.getView().getTitle();
	if (Districts.getPlugin().placeholderapi) {
		if (!title.equals(PlaceholderAPI.setPlaceholders((Player) e.getWhoClicked(), Locale.infoPanelTitle))) {
			return;
		}
	} else {
		if (!title.equals(Locale.infoPanelTitle)) {
			return;
		}
	}
	// Check the right worlds
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getWhoClicked().getWorld().getName())) {
	    return;
	}
	Player player = (Player)e.getWhoClicked();
	// Check for clicks outside
	if (e.getSlot() < 0) {
	    player.closeInventory();
	    return;
	}
	// Get the items in the panel for this player
	List<IPItem> ipitems = plugin.getInfoPanel((Player)e.getWhoClicked());
	Utils.logger(2,"DEBUG: slot = " + e.getSlot());
	if (e.getSlot() > ipitems.size()) {
	    e.setCancelled(true);
	    return;
	}

	IPItem clickedItem = null;
	for (IPItem item : ipitems) {
	    if (item.getSlot() == e.getSlot()) {
		Utils.logger(2,"DEBUG: item slot found, item is " + item.getItem().toString());
		Utils.logger(2,"DEBUG: clicked item is " + e.getCurrentItem().toString());
		// Check it was the same item and not an item in the player's part of the inventory
		if (e.getCurrentItem().equals(item.getItem())) {
		    Utils.logger(2,"DEBUG: item matched!");
		    clickedItem = item;
		    break;
		}
	    }
	}
	if (clickedItem == null) {
	    // Not one of our items
	    Utils.logger(2,"DEBUG: not a recognized item");
	    e.setCancelled(true);
	    return;
	}
	switch (clickedItem.getType()) {
	case RENT:
	    player.performCommand("district rent");
	    player.closeInventory();
	    break;
	case BUY:
	    player.performCommand("district buy");
	    player.closeInventory();
	    break;
	case BUYBLOCKS:
	    ConversationFactory factory = new ConversationFactory(plugin);
	    Conversation buyConv = factory.withFirstPrompt(new ConversationBlocks(plugin,ConversationBlocks.Type.BUY)).withLocalEcho(false)
		    .withTimeout(10).buildConversation(player);
	    buyConv.addConversationAbandonedListener(new ConversationAbandonedListener() {
		@Override
		public void conversationAbandoned(ConversationAbandonedEvent event) {
		    if (event.getCanceller() instanceof InactivityConversationCanceller) {
			event.getContext().getForWhom().sendRawMessage(ChatColor.RED + "Cancelling - time out.");
			return;
		    }  
		}});
	    buyConv.begin();
	    player.closeInventory();
	    break;
	case INFO:
	    break;
	case NAME:
	    break;
	default:
	    break;
	}
	e.setCancelled(true);
    }


    // Check for Inventory Clicking (Control Panel)

    /**
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onControlPanelClick(final InventoryClickEvent e) {
	// Check that it is a control panel
	Inventory panel = e.getInventory();
	String title = e.getView().getTitle();
	if (Districts.getPlugin().placeholderapi) {
		if (!title.equals(ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders((Player) e.getWhoClicked(), Locale.controlpaneltitle)))) {
			return;
		}
	} else {
		if (!title.equals(ChatColor.translateAlternateColorCodes('&', Locale.controlpaneltitle))) {
			return;
		}
	}
	// Check the right worlds
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(e.getWhoClicked().getWorld().getName())) {
	    return;
	}
	Player player = (Player)e.getWhoClicked();
	UUID playerUUID = player.getUniqueId();
	DistrictRegion d = plugin.players.getInDistrict(player.getUniqueId());

	// Check for clicks outside
	if (e.getSlot() < 0) {
	    player.closeInventory();
	    return;
	}
	// Get the items in the panel for this player
	List<CPItem> cpitems = plugin.getControlPanel((Player)e.getWhoClicked());
	Utils.logger(2,"DEBUG: slot = " + e.getSlot());
	if (e.getSlot() > cpitems.size()) {
	    e.setCancelled(true);
	    return;
	}
	Utils.logger(2,"DEBUG: find out what was clicked");
	// Find out which item was clicked
	CPItem clickedItem = null;
	for (CPItem item : cpitems) {
	    if (item.getSlot() == e.getSlot()) {
		Utils.logger(2,"DEBUG: item slot found, item is " + item.getItem().toString());
		Utils.logger(2,"DEBUG: clicked item is " + e.getCurrentItem().toString());
		// Check it was the same item and not an item in the player's part of the inventory
		if (e.getCurrentItem().equals(item.getItem())) {
		    Utils.logger(2,"DEBUG: item matched!");
		    clickedItem = item;
		    break;
		}
	    }
	}
	if (clickedItem == null) {
	    // Not one of our items
	    Utils.logger(2,"DEBUG: not a recognized item");
	    e.setCancelled(true);
	    return;
	}
	// Store the district that this is about
	final HashMap<Object,Object> map = new HashMap<Object,Object>();
	map.put("District", plugin.players.getInDistrict(player.getUniqueId()));
	ConversationFactory factory = new ConversationFactory(plugin);

	switch (clickedItem.getType()) {
	case TEXT:
	    // Enter some text
	    Conversation conv = factory.withFirstPrompt(new ConversationNaming(plugin)).withLocalEcho(false).withInitialSessionData(map)
	    .withEscapeSequence("").withTimeout(10).buildConversation(player);
	    conv.addConversationAbandonedListener(new ConversationAbandonedListener() {

		@Override
		public void conversationAbandoned(ConversationAbandonedEvent event) {
		    if (event.getCanceller() instanceof InactivityConversationCanceller) {
			event.getContext().getForWhom().sendRawMessage(ChatColor.RED + "Cancelling naming - time out.");
			return;
		    }  
		    if (event.getCanceller() instanceof ExactMatchConversationCanceller) {
			event.getContext().getForWhom().sendRawMessage(ChatColor.RED + "Leaving it as it is.");
			return;
		    }
		}});
	    conv.begin();
	    player.closeInventory();
	    break;
	case TOGGLE:
	    Utils.logger(2,"DEBUG: toggling settings");
	    // Now toggle the setting
	    clickedItem.setFlagValue(!clickedItem.isFlagValue(), player);
	    // Set the district value
	    d.setFlag(clickedItem.getName(), clickedItem.isFlagValue());
	    // Change the item in this inventory
	    panel.setItem(e.getSlot(), clickedItem.getItem());
	    break;
	case BUYBLOCKS:
	    Conversation buyConv = factory.withFirstPrompt(new ConversationBlocks(plugin,ConversationBlocks.Type.BUY)).withLocalEcho(false).withInitialSessionData(map)
	    .withTimeout(10).buildConversation(player);
	    buyConv.addConversationAbandonedListener(new ConversationAbandonedListener() {
		@Override
		public void conversationAbandoned(ConversationAbandonedEvent event) {
		    if (event.getCanceller() instanceof InactivityConversationCanceller) {
			event.getContext().getForWhom().sendRawMessage(ChatColor.RED + "Cancelling - time out.");
			return;
		    }  
		}});
	    buyConv.begin();
	    player.closeInventory();
	    break;
	case CLAIM:
	    Conversation claimConv = factory.withFirstPrompt(new ConversationBlocks(plugin,ConversationBlocks.Type.CLAIM)).withLocalEcho(false).withInitialSessionData(map)
	    .withTimeout(10).buildConversation(player);
	    claimConv.addConversationAbandonedListener(new ConversationAbandonedListener() {
		@Override
		public void conversationAbandoned(ConversationAbandonedEvent event) {
		    if (event.getCanceller() instanceof InactivityConversationCanceller) {
			event.getContext().getForWhom().sendRawMessage(ChatColor.RED + "Cancelling - time out.");
			return;
		    }  
		}});
	    claimConv.begin();
	    player.closeInventory();
	    break;
	case CANCEL:
	    player.performCommand("district cancel");
	    player.closeInventory();
	    break;
	case REMOVE:
	    player.performCommand("district remove");
	    player.closeInventory();
	    break;
	case RENT:
	    getPrice(d,player,ConversationSellBuy.Type.RENT);
	    player.closeInventory();
	    break;
	case SELL:
	    getPrice(d,player,ConversationSellBuy.Type.SELL);
	    player.closeInventory();
	    break;
	case TRUST:
	    getPlayers(d,player,GetPlayers.Type.TRUST);
	    player.closeInventory();
	    break;
	case UNTRUST:
	    getPlayers(d,player,GetPlayers.Type.UNTRUST);
	    player.closeInventory();	    
	    break;
	case VISUALIZE:
	    // Toggle the visualization setting
	    if (plugin.players.getVisualize(playerUUID)) {
		Visualization.devisualize(player);
		clickedItem.setFlagValue(false, player);
		//player.sendMessage(ChatColor.YELLOW + "Switching district boundary off");
	    } else {
		//player.sendMessage(ChatColor.YELLOW + "Switching district boundary on");
		clickedItem.setFlagValue(true, player);
		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d != null)
		    Visualization.visualize(d, player);
	    }
	    plugin.players.setVisualize(playerUUID, !plugin.players.getVisualize(playerUUID));		
	    // Change the item in this inventory
	    panel.setItem(e.getSlot(), clickedItem.getItem());
	    break;
	default:
	    break;
	}
	e.setCancelled(true);
	return;
    }

    private void getPrice(DistrictRegion d, Player player, ConversationSellBuy.Type type) {
	final HashMap<Object,Object> map = new HashMap<Object,Object>();
	map.put("District", plugin.players.getInDistrict(player.getUniqueId()));

	ConversationFactory factory = new ConversationFactory(plugin);
	Conversation conv = factory.withFirstPrompt(new ConversationSellBuy(plugin,type)).withLocalEcho(false).withInitialSessionData(map)
		.withEscapeSequence("").withTimeout(10).buildConversation(player);
	conv.addConversationAbandonedListener(new ConversationAbandonedListener() {

	    @Override
	    public void conversationAbandoned(ConversationAbandonedEvent event) {
		/* 
		    if (event.gracefulExit())
	            {
	                plugin.logger(2,"graceful exit");
	                return;
	            }*/
		if (event.getCanceller() instanceof InactivityConversationCanceller) {
		    event.getContext().getForWhom().sendRawMessage(ChatColor.RED + "Cancelling - time out.");
		    return;
		}  
	    }});
	conv.begin();

    }

    private void getPlayers(DistrictRegion d, Player player, GetPlayers.Type type) {
	final HashMap<Object,Object> map = new HashMap<Object,Object>();
	map.put("District", plugin.players.getInDistrict(player.getUniqueId()));

	ConversationFactory factory = new ConversationFactory(plugin);
	Conversation conv = factory.withFirstPrompt(new GetPlayers(plugin,type)).withLocalEcho(false).withInitialSessionData(map)
		.withEscapeSequence("").withTimeout(10).buildConversation(player);
	conv.addConversationAbandonedListener(new ConversationAbandonedListener() {

	    @Override
	    public void conversationAbandoned(ConversationAbandonedEvent event) {
		if (event.getCanceller() instanceof InactivityConversationCanceller) {
		    event.getContext().getForWhom().sendRawMessage(ChatColor.RED + "Cancelling - time out.");
		    return;
		}  
	    }});
	conv.begin();

    }



}

