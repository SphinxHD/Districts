/**
 * 
 */
package com.wasteofplastic.districts;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Location;


/**
 * This class manages the district map. It knows where every district is, and
 * where new
 * ones should go. It can handle any size of district.
 * The districtGrid is stored in a YML file.
 * 
 * @author tastybento
 */
public class GridManager {
    private Districts plugin = Districts.getPlugin();
    // 2D Grid of districts, x first
    private TreeMap<Integer, TreeMap<Integer, DistrictRegion>> districtGrid = new TreeMap<Integer, TreeMap<Integer, DistrictRegion>>();

    /**
     * @param plugin
     */
    public GridManager(Districts plugin) {
	this.plugin = plugin;
    }

    /**
     * Returns the district at the location or null if there is none
     * 
     * @param location
     * @return PlayerDistrictRegion object
     */
    public DistrictRegion getDistrictRegionAt(Location location) {
	return getDistrictRegionAt(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Returns the district at the x,z location or null if there is none
     * 
     * @param x
     * @param z
     * @return PlayerDistrictRegion or null
     */
    public DistrictRegion getDistrictRegionAt(int x, int z) {
	//
	// Check xgrid
	// Try excat values first
	//int count = 0;
	//plugin.getLogger().info("***************************************************");
	//plugin.getLogger().info("DEBUG: get region at " + x + "," + z);
	// Loop x search
	int searchTerm = x;
	Entry<Integer, TreeMap<Integer, DistrictRegion>> en = null;
	do {
	    //count++;
	    //plugin.getLogger().info("DEBUG: Trying x = " + searchTerm); 
	    en = districtGrid.floorEntry(searchTerm);
	    if (en != null) {
		//plugin.getLogger().info("DEBUG: something found in x at " + en.getKey());
		int searchTermZ = z;
		Entry<Integer, DistrictRegion> ent = null;
		do {
		    //count++;
		    //plugin.getLogger().info("DEBUG: Trying z = " + searchTermZ); 
		    ent = en.getValue().floorEntry(searchTermZ);
		    if (ent != null) {
			// Check if in the district range
			DistrictRegion district = ent.getValue();
			//plugin.getLogger().info("DEBUG: x grid - z entry found");
			if (district.inDistrict(x, z)) {
			    //plugin.getLogger().info("DEBUG: Player is in district - x grid");
			    //plugin.getLogger().info("DEBUG: Player is in district with coords:" + district.getMinX()
				//    + "," + district.getMinZ() + " " + district.getMaxX() + "," + district.getMaxZ());
			    //plugin.getLogger().info("DEBUG: " + count);
			    //plugin.getLogger().info("DEBUG: in district!");
			    return district;
			} else {
			    //plugin.getLogger().info("DEBUG: Player is not in district with coords:" + district.getMinX()
			//	    + "," + district.getMinZ() + " " + district.getMaxX() + "," + district.getMaxZ());
			}
			// Try next lowest z
			searchTermZ = ent.getKey() - 1;
		    }
		} while (ent != null);
		// Try next lowest x
		searchTerm = en.getKey() - 1;
	    }
	} while (en != null);
	//plugin.getLogger().info("DEBUG: " + count);
	//plugin.getLogger().info("DEBUG: not in district - grid manager");
	return null;
    }

    /**
     * Adds district to the grid using the stored information
     * 
     * @param districtSerialized
     */
    public DistrictRegion addDistrictRegion(String world, String districtSerialized) {
	// Format is ownerUUID}pos1x:pos1y:pos1z}pos2x:pos2y:pos2z
	//plugin.getLogger().info("DEBUG: adding district " + districtSerialized);
	// Split the string
	String[] split = districtSerialized.split("}");
	if (split.length == 3) {
	    try {
		UUID owner = UUID.fromString(split[0]);
		DistrictRegion newDistrictRegion = new DistrictRegion(plugin,Utils.getLocationString(world + ":" + split[1]),
			Utils.getLocationString(world + ":" + split[2]), owner);
		addToGrid(newDistrictRegion);
		return newDistrictRegion;
	    } catch (Exception e) {
		plugin.getLogger().severe("Could not process district in districts.yml, skipping...");
		plugin.getLogger().severe("Error: '"+districtSerialized +"'");
	    }    
	}
	return null;
    }

    /**
     * Adds the region to the grid and ownership list
     * @param newDistrictRegion
     */
    public void addToGrid(DistrictRegion newDistrictRegion) {
	//plugin.getLogger().info("DEBUG: adding district to grid");
	if (districtGrid.containsKey(newDistrictRegion.getMinX())) {
	    //plugin.getLogger().info("DEBUG: min x is in the grid :" + newDistrictRegion.getMinX());
	    TreeMap<Integer, DistrictRegion> zEntry = districtGrid.get(newDistrictRegion.getMinX());
	    // Add district
	    zEntry.put(newDistrictRegion.getMinZ(), newDistrictRegion);
	    districtGrid.put(newDistrictRegion.getMinX(), zEntry);
	} else {
	    // Add district
	    //plugin.getLogger().info("DEBUG: min x is not in the grid :" + newDistrictRegion.getMinX());
	    //plugin.getLogger().info("DEBUG: min Z is not in the grid :" + newDistrictRegion.getMinZ());
	    // X grid first
	    TreeMap<Integer, DistrictRegion> zEntry = new TreeMap<Integer, DistrictRegion>();
	    zEntry.put(newDistrictRegion.getMinZ(), newDistrictRegion);
	    districtGrid.put(newDistrictRegion.getMinX(), zEntry);
	}
    }

    /**
     * Removes the district from the grid and removes the owner
     * from the ownership map
     * @param district
     */
    public void deleteDistrictRegion(DistrictRegion district) {
	if (district != null) {
	    int x = district.getMinX();
	    int z = district.getMinZ();
	    // plugin.getLogger().info("DEBUG: x = " + x + " z = " + z);
	    if (districtGrid.containsKey(x)) {
		// plugin.getLogger().info("DEBUG: x found");
		TreeMap<Integer, DistrictRegion> zEntry = districtGrid.get(x);
		if (zEntry.containsKey(z)) {
		    // plugin.getLogger().info("DEBUG: z found - deleting the district");
		    // DistrictRegion exists - delete it
		    DistrictRegion deletedDistrictRegion = zEntry.get(z);
		    deletedDistrictRegion.setOwner(null);
		    zEntry.remove(z);
		    districtGrid.put(x, zEntry);
		} // else {
		// plugin.getLogger().info("DEBUG: could not find z");
		// }
	    }
	}	
    }

    /**
     * Removes the district at location loc from the grid and removes the player
     * from the ownership map
     * 
     * @param loc
     */
    public void deleteDistrictRegion(Location loc) {
	// plugin.getLogger().info("DEBUG: deleting district at " + loc);
	DistrictRegion district = getDistrictRegionAt(loc);
	deleteDistrictRegion(district);
    }

    /**
     * Determines if an district is at a location in this area
     * location. Also checks if the spawn district is in this area.
     * Used for creating new districts ONLY
     * 
     * @param loc
     * @return true if found, otherwise false
     */
    public boolean districtAtLocation(final Location loc) {
	if (loc == null) {
	    return true;
	}
	// getLogger().info("DEBUG checking districtAtLocation for location " +
	// loc.toString());
	// Check the district grid
	if (getDistrictRegionAt(loc) != null) {
	    // This checks if loc is inside the district spawn radius too
	    return true;
	}
	return false;
    }

}