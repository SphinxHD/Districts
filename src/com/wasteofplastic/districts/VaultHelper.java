package com.wasteofplastic.districts;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Helper class for Vault Economy and Permissions
 */
public class VaultHelper {
    public static Economy econ = null;
    public static Permission permission = null;

    /**
     * Sets up the economy instance
     * @return
     */
    public static boolean setupEconomy() {
	RegisteredServiceProvider<Economy> economyProvider = Districts.getPlugin().getServer().getServicesManager()
		.getRegistration(net.milkbowl.vault.economy.Economy.class);
	if (economyProvider != null) {
	    econ = economyProvider.getProvider();
	}
	return econ != null;
    }

    /**
     * Sets up the permissions instance
     * @return
     */
    public static boolean setupPermissions() {
	RegisteredServiceProvider<Permission> permissionProvider = Districts.getPlugin().getServer().getServicesManager()
		.getRegistration(net.milkbowl.vault.permission.Permission.class);
	if (permissionProvider != null) {
	    permission = permissionProvider.getProvider();
	}
	return (permission != null);
    }
    

    /**
     * Checks permission of player in world or in any world
     * @param player
     * @param perm
     * @return
     */
    public static boolean checkPerm(final Player player, final String perm) {
	return permission.has(player, perm);
   }
    
    /**
     * Adds permission to player
     * @param player
     * @param perm
     */
    public static void addPerm(final Player player, final String perm) {
	permission.playerAdd(player, perm);
    }

    /**
     * Removes a player's permission
     * @param player
     * @param perm
     */
    public static void removePerm(final Player player, final String perm) {
	permission.playerRemove(player, perm);
    }


}