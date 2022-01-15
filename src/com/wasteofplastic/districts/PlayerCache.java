/**
 * 
 */
package com.wasteofplastic.districts;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author ben
 * Provides a memory cache of online player information
 * This is the one-stop-shop of player info
 * If the player is not cached, then a request is made to Players to obtain it
 */
public class PlayerCache {
    private HashMap<UUID, Players> playerCache = new HashMap<UUID, Players>();
    private final Districts plugin;

    public PlayerCache(Districts plugin) {
	this.plugin = plugin;
	final Collection<? extends Player> serverPlayers = Bukkit.getServer().getOnlinePlayers();
	for (Player p : serverPlayers) {
	    if (p.isOnline()) {
		final Players playerInf = new Players(plugin, p.getUniqueId());
		// Add this player to the online cache
		playerCache.put(p.getUniqueId(), playerInf);
	    }
	}
    }

    /*
     * Cache control methods
     */

    public void addPlayer(final UUID playerUUID) {
	//if (!plugin.getServer().getOfflinePlayer(playerUUID).hasPlayedBefore()) {
	//    plugin.getLogger().severe("Asked to create a player that does not exist!");
	//} else {
	if (!playerCache.containsKey(playerUUID)) {
	    final Players player = new Players(plugin, playerUUID);
	    playerCache.put(playerUUID,player);
	}
	//}
    }

    /**
     * Stores the player's info to a file and removes the player from the list
     * of currently online players
     * 
     * @param player
     *            - name of player
     */
    public void removeOnlinePlayer(final UUID player) {
	if (playerCache.containsKey(player)) {
	    playerCache.get(player).save();
	    playerCache.remove(player);
	    Utils.logger(2,"Removing player from cache: " + player);
	}
    }

    /**
     * Removes all players on the server now from cache and saves their info
     */
    public void removeAllPlayers() {
	for (UUID p : playerCache.keySet()) {
	    playerCache.get(p).save();
	}
	playerCache.clear();
    }

    /*
     * Player info query methods
     */
    /**
     * Returns location of player's districts from cache if available
     * @param playerUUID
     * @return List of districts
     */
    /*
    public List<DistrictRegion> getPlayersDistricts(final UUID playerUUID) {
	if (playerCache.containsKey(playerUUID)) {
	    return playerCache.get(playerUUID).get;
	}
	final Players player = new Players(plugin, playerUUID);
	return player.getIslandLocation();
    }
     */
    /**
     * Checks if the player is known or not by looking through the filesystem
     * 
     * @param uniqueID
     * @return true if player is know, otherwise false
     */
    public boolean isAKnownPlayer(final UUID uniqueID) {
	if (uniqueID == null) {
	    return false;
	}
	if (playerCache.containsKey(uniqueID)) {
	    return true;
	} else {
	    // Get the file system
	    final File folder = plugin.playersFolder;
	    final File[] files = folder.listFiles();
	    // Go through the native YAML files
	    for (final File f : files) {
		// Need to remove the .yml suffix
		if (f.getName().endsWith(".yml")) {
		    if (UUID.fromString(f.getName().substring(0, f.getName().length() - 4)).equals(uniqueID)) {
			return true;
		    }
		}
	    }
	}
	// Not found, sorry.
	return false;
    }

    /**
     * Returns the player object for the named player
     * @param playerUUID - String name of player
     * @return - player object
     */
    public Players get(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID);
    }

    /**
     * @return a list of all known players UUID's
     */
    public List<UUID> getAllPlayers() {
	// Get the file system
	final File folder = plugin.playersFolder;
	final File[] files = folder.listFiles();
	List<UUID> result = new ArrayList<UUID>();
	// Go through the native YAML files
	for (final File f : files) {
	    // Need to remove the .yml suffix
	    if (f.getName().endsWith(".yml")) {
		UUID player = UUID.fromString(f.getName().substring(0, f.getName().length() - 4));
		if (player != null) {
		    result.add(player);
		}
	    }
	}
	return result;

    }

    /**
     * Checks if player has a district from cache if available
     * @param playerUUID - string name of player
     * @return true if player has a district
     */
    public boolean hasADistrict(final UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).hasADistrict();
    }


    public void removeDistrict(UUID playerUUID) {
	addPlayer(playerUUID);
	// TODO Remove a district
	playerCache.get(playerUUID).save(); // Needed?
    }

    public void setHasDistricts(UUID playerUUID, boolean b) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setHasDistricts(b);
    }

    public void newDistrict(UUID playerUUID, Location islandLocation) {
	addPlayer(playerUUID);
	// TODO Add a district here	playerCache.get(playerUUID).setIslandLocation(islandLocation);	
    }



    /**
     * Saves the player's info to the file system
     * @param playerUUID
     */
    public void save(UUID playerUUID) {
	if (playerCache.containsKey(playerUUID)) {
	    playerCache.get(playerUUID).save();
	}
    }

    /**
     * Attempts to return a UUID for a given player's name
     * @param string
     * @return
     */
    public UUID getUUID(String string) {
	for (UUID id : playerCache.keySet()) {
	    String name = playerCache.get(id).getPlayerName();
	    //plugin.logger(2,"DEBUG: Testing name " + name);
	    if (name != null && name.equalsIgnoreCase(string)) {
		return id;
	    }
	}
	// Look in the file system
	for (final File f : plugin.playersFolder.listFiles()) {
	    // Need to remove the .yml suffix
	    String fileName = f.getName();
	    if (fileName.endsWith(".yml")) {
		try {
		    final UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
		    if (plugin.getServer().getOfflinePlayer(playerUUID).getName().equalsIgnoreCase(string)) {
			return playerUUID;
		    }
		} catch (Exception e) {
		}
	    }
	}
	return null;
    }

    public void setPlayerName(UUID uniqueId, String name) {
	addPlayer(uniqueId);
	playerCache.get(uniqueId).setPlayerN(name);
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     * @param playerUUID
     * @return String - playerName
     */
    public String getName(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getPlayerName();
    }

    public void setInDistrict(UUID playerUUID, DistrictRegion inDistrict) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setInDistrict(inDistrict);
    }

    /**
     * @param playerUUID
     * @return the district the player is in or null if no district
     */
    public DistrictRegion getInDistrict(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getInDistrict();
    }
    /**
     * @return how many blocks a player has
     */
    public int getBlockBalance(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).getBlockBalance();
    }

    /**
     * @param blocks the blocks to set
     */
    public int setBlocks(UUID playerUUID, int blocks) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).setBlocks(blocks);
    }

    /**
     * Adds blocks to the player's balance
     * @param blocks
     * @return the player's balance
     */
    public int addBlocks(UUID playerUUID, int blocks) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).addBlocks(blocks);
    }

    /**
     * Removes a number of blocks from a player's balance.
     * If the balance becomes negative, the blocks are not removed
     * and instead the number required are returned as a negative number
     * @param blocks
     * @return
     */
    public int removeBlocks(UUID playerUUID, int blocks) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).removeBlocks(blocks);
    }

    public boolean getVisualize(UUID playerUUID) {
	addPlayer(playerUUID);
	return playerCache.get(playerUUID).isVisualize();
    }

    public void setVisualize(UUID playerUUID, boolean visualize) {
	addPlayer(playerUUID);
	playerCache.get(playerUUID).setVisualize(visualize);
    }
}



