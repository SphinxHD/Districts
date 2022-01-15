package com.wasteofplastic.districts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Tracks the following info on the player
 */
public class Players {
    private Districts plugin;
    private YamlConfiguration playerInfo;
    private boolean hasDistricts;
    private UUID uuid;
    private String playerName;
    private DistrictRegion inDistrict;
    private boolean visualize;
    // The number of blocks I have to use on districts
    private int blocks;

    /**
     * @param uuid
     *            Constructor - initializes the state variables
     * 
     */
    public Players(final Districts districts, final UUID uuid) {
	this.plugin = districts;
	this.uuid = uuid;
	this.hasDistricts = false;
	this.playerName = "";
	this.inDistrict = null;
	this.blocks = Settings.beginningBlocks;
	this.visualize = true;
	load(uuid);
    }

    /**
     * Loads a player from file system and if they do not exist, then it is created
     * @param uuid
     */
    public void load(UUID uuid) {
	playerInfo = Utils.loadYamlFile("players/" + uuid.toString() + ".yml");
	// Load in from YAML file
	this.playerName = playerInfo.getString("playerName", "");
	if (playerName.isEmpty()) {
	    try {
		playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
	    } catch (Exception e) {
		plugin.getLogger().severe("Could not obtain a name for the player with UUID " + uuid.toString());
		playerName = "";
	    }
	    if (playerName == null) {
		plugin.getLogger().severe("Could not obtain a name for the player with UUID " + uuid.toString());
		playerName = "";		
	    }
	}
	Utils.logger(2,"Loading player..." + playerName);
	this.hasDistricts = playerInfo.getBoolean("hasDistricts", false);
	this.visualize = playerInfo.getBoolean("visualize",true);
	// Get how many blocks I have to use
	this.blocks = playerInfo.getInt("blocks",Settings.beginningBlocks);
	ConfigurationSection myDists = playerInfo.getConfigurationSection("districts");
	if (myDists != null) {
	    // Get a list of all the districts
	    for (String key : myDists.getKeys(false)) {
		try {
		    // Load all the values
		    Location pos1 = getLocationString(playerInfo.getString("districts." + key + ".pos-one"));
		    Location pos2 = getLocationString(playerInfo.getString("districts." + key + ".pos-two"));
		    if (pos1 == null || pos2 == null) {
			// We have a problem with the district
			plugin.getLogger().severe("Problem loading a district owned by " + playerName);
			plugin.getLogger().severe("District " + key + " location has a location reference that does not exist:");
			if (pos1 == null) {
			    plugin.getLogger().severe("Something is wrong in this: '" + playerInfo.getString("districts." + key + ".pos-one") + "'");
			}
			if (pos2 == null) {
			    plugin.getLogger().severe("Something is wrong in this: '" + playerInfo.getString("districts." + key + ".pos-two") + "'");
			}		
		    } else {
			// Check if this district already exists
			if (plugin.getGrid(pos1.getWorld().getName()) != null && plugin.getGrid(pos1.getWorld().getName()).districtAtLocation(pos1)) {
			//if (!plugin.checkDistrictIntersection(pos1, pos2)) {
			    Utils.logger(2,"DEBUG: District already exists or overlaps - ignoring");

			   } else {
			    DistrictRegion d = new DistrictRegion(plugin, pos1, pos2, uuid);
			    d.setId(UUID.fromString(playerInfo.getString("districts." + key + ".id")));
			    // Load all the flags
			    HashMap<String,Object> flags = (HashMap<String, Object>) playerInfo.getConfigurationSection("districts." + key + ".flags").getValues(false);
			    //d.setEnterMessage(playerInfo.getString("districts." + key + ".entermessage",""));
			    //d.setFarewellMessage(playerInfo.getString("districts." + key + ".farewellmessage",""));
			    d.setFlags(flags);
			    // Load the various other flags here
			    String tempUUID = playerInfo.getString("districts." + key + ".renter");
			    if (tempUUID != null) {
				d.setRenter(UUID.fromString(tempUUID));
			    }
			    d.setForSale(playerInfo.getBoolean("districts." + key + ".forSale", false));
			    d.setForRent(playerInfo.getBoolean("districts." + key + ".forRent", false));
			    d.setPrice(playerInfo.getDouble("districts." + key + ".price", 0D));
			    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			    String dateInString = playerInfo.getString("districts." + key + ".lastPayment");
			    if (dateInString != null) {		 
				try {		 
				    Date date = formatter.parse(dateInString);
				    d.setLastPayment(date);
				} catch (ParseException e) {
				    e.printStackTrace();
				}
			    }
			    // Get the trusted players
			    List<UUID> ownerTrustedUUID = new ArrayList<UUID>();
			    List<String> ownerTrusted = playerInfo.getStringList("districts." + key + ".ownerTrusted");
			    if (ownerTrusted != null) {
				for (String temp : ownerTrusted) {
				    try {
					ownerTrustedUUID.add(UUID.fromString(temp));
				    } catch (Exception e) {
					e.printStackTrace();
				    }
				}
				d.setOwnerTrusted(ownerTrustedUUID);
			    } 
			    List<UUID> renterTrustedUUID = new ArrayList<UUID>();
			    List<String> renterTrusted = playerInfo.getStringList("districts." + key + ".renterTrusted");
			    if (renterTrusted != null) {
				for (String temp : renterTrusted) {
				    try {
					renterTrustedUUID.add(UUID.fromString(temp));
				    } catch (Exception e) {
					e.printStackTrace();
				    }
				}
				d.setRenterTrusted(renterTrustedUUID);
			    }	    
			    plugin.getDistricts().add(d);
			    //plugin.getLogger().info("DEBUG: " + d.getPos1());
			    //plugin.getLogger().info("DEBUG: " + d.getPos1().getWorld());
			    //plugin.getLogger().info("DEBUG: " + d.getPos1().getWorld().getName());
			    plugin.getGrid(d.getPos1().getWorld().getName()).addToGrid(d);
			}
		    }
		} catch (IllegalArgumentException iae) { 
		    plugin.getLogger().severe("Problem loading the district owned by " + playerName);
		    plugin.getLogger().severe("District " + key + " location has a world reference that does not exist."); 
		}
		catch (Exception e) {
		    plugin.getLogger().severe("Problem loading player files");
		    e.printStackTrace();
		}

	    }
	}
    }
    /**
     * Saves the player info to the file system
     */
    public void save() {
	Utils.logger(2,"Saving player..." + playerName + " [" + uuid + "]");
	// Save the variables
	playerInfo.set("playerName", playerName);
	playerInfo.set("hasDistricts", hasDistricts);
	playerInfo.set("blocks", blocks);
	playerInfo.set("visualize", isVisualize());
	// Wipe out any old districts in the file
	playerInfo.createSection("districts");
	if (!plugin.getDistricts().isEmpty()) {
	    // Get a list of all my districts
	    int index = 0;
	    for (DistrictRegion district : plugin.getDistricts()) {
		Utils.logger(2,"District = " + district.getId().toString());
		if (district.getOwner() != null && district.getOwner().equals(uuid)) {
		    Utils.logger(2,"Owner = " + district.getOwner().toString());
		    // Save all the values
		    playerInfo.set("districts." + index + ".id", district.getId().toString());
		    playerInfo.set("districts." + index + ".pos-one", getStringLocation(district.getPos1()));
		    playerInfo.set("districts." + index + ".pos-two", getStringLocation(district.getPos2()));
		    /*
		    private World world;
		    private UUID owner;
		    private UUID renter;
		    private List<UUID> ownerTrusted;
		    private List<UUID> renterTrusted;
		    private boolean forSale = false;
		    private boolean forRent = false;
		    private Double price = 0D;
		    private Date lastPayment;
		     */
		    if (district.getRenter() != null)
			playerInfo.set("districts." + index + ".renter", district.getRenter().toString());
		    playerInfo.set("districts." + index + ".forSale", district.isForSale());
		    playerInfo.set("districts." + index + ".forRent", district.isForRent());
		    playerInfo.set("districts." + index + ".price",  district.getPrice());
		    if (district.getLastPayment() != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");		    
			playerInfo.set("districts." + index + ".lastPayment",  formatter.format(district.getLastPayment()));
		    }
		    // Get the trusted players
		    playerInfo.set("districts." + index + ".ownerTrusted", district.getOwnerTrustedUUIDString());
		    playerInfo.set("districts." + index + ".renterTrusted", district.getRenterTrustedUUIDString());		   		    
		    // Save the various other flags here
		    playerInfo.createSection("districts." + index + ".flags", district.getFlags());		    
		    // TODO
		    index++;
		}
	    }
	}
	Utils.saveYamlFile(playerInfo, "players/" + uuid.toString() + ".yml");
    }


    public boolean hasADistrict() {
	return hasDistricts;
    }

    /**
     * Converts a serialized location string to a Bukkit Location
     * 
     * @param s
     *            - a serialized Location
     * @return a new Location based on string or null if it cannot be parsed
     */
    private static Location getLocationString(final String s) {
	if (s == null || s.trim() == "") {
	    return null;
	}
	final String[] parts = s.split(":");
	if (parts.length == 4) {
	    final World w = Bukkit.getServer().getWorld(parts[0]);
	    if (w == null) {
		return null;
	    }
	    final int x = Integer.parseInt(parts[1]);
	    final int y = Integer.parseInt(parts[2]);
	    final int z = Integer.parseInt(parts[3]);
	    return new Location(w, x, y, z);
	}
	return null;
    }

    public Player getPlayer() {
	return Bukkit.getPlayer(uuid);
    }

    public UUID getPlayerUUID() {
	return uuid;
    }

    public String getPlayerName() {
	return playerName;
    }

    public void setPlayerN(String playerName) {
	this.playerName = playerName;
    }

    /**
     * Converts a Bukkit location to a String
     * 
     * @param l
     *            a Bukkit Location
     * @return String of the floored block location of l or "" if l is null
     */

    private String getStringLocation(final Location l) {
	if (l == null) {
	    return "";
	}
	return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    public void setHasDistricts(final boolean b) {
	hasDistricts = b;
    }


    /**
     * @param s
     *            a String name of the player
     */
    public void setPlayerUUID(final UUID s) {
	uuid = s;
    }

    /**
     * @return the inDistrict
     */
    public DistrictRegion getInDistrict() {
	return inDistrict;
    }

    /**
     * @param inDistrict the inDistrict to set
     */
    public void setInDistrict(DistrictRegion inDistrict) {
	this.inDistrict = inDistrict;
    }

    /**
     * @return how many blocks a player has
     */
    public int getBlockBalance() {
	return blocks;
    }

    /**
     * @param blocks the blocks to set
     * @return 
     */
    public int setBlocks(int blocks) {
	this.blocks = blocks;
	return blocks;
    }

    /**
     * Adds blocks to the player's balance
     * @param blocks
     * @return the player's balance
     */
    public int addBlocks(int blocks) {
	// If they are online - do not add if they are at the maximum allowed
	Player o = plugin.getServer().getPlayer(uuid);
	int balance = this.blocks;
	if (o != null) {
	    // Online player
	    int maxBalance = plugin.getMaxBlockBalance(o);
	    int blocksInDistricts = 0;
	    if (Settings.maxBlockLimit) {
		// Increase the balance by how many blocks they have in districts
		blocksInDistricts = plugin.getBlocksInDistricts(o);
	    }
	    if (balance + blocks + blocksInDistricts > maxBalance) {
		this.blocks = Math.max(maxBalance - blocksInDistricts, 0);
	    } else {
		this.blocks += blocks;
	    }
	} else {
	    // Offline - any overage will be handled at login
	    this.blocks += blocks;
	}
	    return this.blocks;
	}

	/**
	 * Removes a number of blocks from a player's balance.
	 * If the balance becomes negative, the blocks are not removed
	 * and instead the number required are returned as a negative number
	 * @param blocks
	 * @return
	 */
	public int removeBlocks(int blocks) {
	    int balance = this.blocks - blocks;
	    if (balance < 0) {
		return balance;
	    }
	    this.blocks -= blocks;
	    return this.blocks;
	}

	/**
	 * @return the visualize
	 */
	public boolean isVisualize() {
	    return visualize;
	}

	/**
	 * @param visualize the visualize to set
	 */
	public void setVisualize(boolean visualize) {
	    this.visualize = visualize;
	}

    }