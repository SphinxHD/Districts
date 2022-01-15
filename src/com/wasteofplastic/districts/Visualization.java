package com.wasteofplastic.districts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Provides visualization of the district border if requested by the player
 * @author tastybento
 *
 */
public class Visualization {
    // Where visualization blocks are kept
    private static HashMap<UUID, List<Location>> visualizations = new HashMap<UUID, List<Location>>();

    /**
     * Visualize district d for player
     * @param d
     * @param player
     */
    @SuppressWarnings("deprecation")
    public static void visualize(DistrictRegion d, Player player) {
	World world = player.getWorld();
	Vector playerLoc = player.getLocation().toVector().multiply(new Vector(1,0,1));
	// Deactivate any previous visualization

	//if (visualizations.containsKey(player.getUniqueId())) {
	//    devisualize(player);
	//}
	//logger(1,"DEBUG: visualize pos1 = " + d.getPos1() + " pos2 = " + d.getPos2());
	// Get the four corners
	int minx = Math.min(d.getPos1().getBlockX(), d.getPos2().getBlockX());
	int maxx = Math.max(d.getPos1().getBlockX(), d.getPos2().getBlockX());
	int minz = Math.min(d.getPos1().getBlockZ(), d.getPos2().getBlockZ());
	int maxz = Math.max(d.getPos1().getBlockZ(), d.getPos2().getBlockZ());

	// Draw the lines - we do not care in what order
	List<Location> positions = visualizations.get(player.getUniqueId());
	if (positions == null) {
	    positions = new ArrayList<Location>();
	}
	// If the distance of the block is too far away, don't visualize it because it does weird things
	// on the client side
	int distanceSquared = Settings.vizRange*Settings.vizRange; // TODO: Make this a settings
	for (int x = minx; x<= maxx; x++) {
	    Location v = new Location(world,x,0,minz);
	    if (!positions.contains(v) && v.toVector().distanceSquared(playerLoc) <= distanceSquared) {
		v = world.getHighestBlockAt(v).getLocation().subtract(new Vector(0,0,0));
		player.sendBlockChange(v, Settings.visualization, (byte)0);
		positions.add(v);
	    }
	}
	for (int x = minx; x<= maxx; x++) {
	    Location v = new Location(world,x,0,maxz);
	    if (!positions.contains(v) && v.toVector().distanceSquared(playerLoc) <= distanceSquared) {
		v = world.getHighestBlockAt(v).getLocation().subtract(new Vector(0,0,0));
		player.sendBlockChange(v, Settings.visualization, (byte)0);
		positions.add(v);
	    }
	}
	for (int z = minz; z<= maxz; z++) {
	    Location v = new Location(world,minx,0,z);
	    if (!positions.contains(v) && v.toVector().distanceSquared(playerLoc) <= distanceSquared) {
		v = world.getHighestBlockAt(v).getLocation().subtract(new Vector(0,0,0));
		player.sendBlockChange(v, Settings.visualization, (byte)0);
		positions.add(v);
	    }
	}
	for (int z = minz; z<= maxz; z++) {
	    Location v = new Location(world,maxx,0,z);
	    if (!positions.contains(v) && v.toVector().distanceSquared(playerLoc) <= distanceSquared) {
		v = world.getHighestBlockAt(v).getLocation().subtract(new Vector(0,0,0));
		player.sendBlockChange(v, Settings.visualization, (byte)0);
		positions.add(v);
	    }
	}
	// Save these locations
	visualizations.put(player.getUniqueId(), positions);
    }

    @SuppressWarnings("deprecation")
    static void visualize(Location l, Player player) {
	//plugin.logger(2,"Visualize location");
	// Deactivate any previous visualization
	if (visualizations.containsKey(player.getUniqueId())) {
	    devisualize(player);
	}
	player.sendBlockChange(l, Settings.visualization, (byte)0);
	// Save these locations
	List<Location> pos = new ArrayList<Location>();
	pos.add(l);
	visualizations.put(player.getUniqueId(), pos);
    }

    @SuppressWarnings("deprecation")
    public static void devisualize(Player player) {
	Utils.logger(2,"Removing visualization");
	if (!visualizations.containsKey(player.getUniqueId())) {
	    return;
	}
	for (Location pos: visualizations.get(player.getUniqueId())) {
	    Block b = pos.getBlock();
	    if (b.getWorld().equals(player.getWorld())) {
		player.sendBlockChange(pos, b.getType(), b.getData());
	    }
	}
	visualizations.remove(player.getUniqueId());
    }
}
