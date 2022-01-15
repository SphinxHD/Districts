
package com.wasteofplastic.districts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.UUID;

import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Handles the /d command
 * @author tastybento
 *
 */
public class DistrictCmd implements CommandExecutor {
    public boolean busyFlag = true;
    public Location Islandlocation;
    private Districts plugin;
    private PlayerCache players;

    /**
     * Constructor
     * 
     * @param plugin
     * @param players 
     */
    public DistrictCmd(Districts plugin, PlayerCache players) {

	// Plugin instance
	this.plugin = plugin;
	this.players = players;
    }
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender
     * , org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] split) {
	if (!(sender instanceof Player)) {
	    return false;
	}
	final Player player = (Player) sender;
	// Check we are in the right world
	if (!Settings.worldName.isEmpty() && !Settings.worldName.contains(player.getWorld().getName())) {
		if (Districts.getPlugin().placeholderapi) {
			player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.generalnotavailable));
		} else {
			player.sendMessage(Locale.generalnotavailable);
		}
	    return true;
	}
	// Basic permissions check to even use /" + label + "
	if (!VaultHelper.checkPerm(player, "districts.player")) {
		if (Districts.getPlugin().placeholderapi) {
			player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
		} else {
			player.sendMessage(ChatColor.RED + Locale.errornoPermission);
		}
	    return true;
	}
	/*
	 * Grab data for this player - may be null or empty
	 * playerUUID is the unique ID of the player who issued the command
	 */
	final UUID playerUUID = player.getUniqueId();
	DistrictRegion d = players.getInDistrict(playerUUID);
	switch (split.length) {
	// /" + label + " command by itself
	case 0:
	    // The basic command - show the relevant GUI

	    // The following control panels are available:
	    // 1. Owner's - shown when the owner is in a district they own or an Op is in a district
	    // 2. Info - shown when a non-owner is in a district
	    // 3. Claim - shown when anyone is not in a district

	    // Owner's or ops or admins
	    if (d != null && (d.getOwner().equals(playerUUID) || player.isOp() 
		    || VaultHelper.checkPerm(player, "districts.admin")
		    || (d.getRenter() != null && d.getRenter().equals(playerUUID)))) {
		Inventory i = plugin.controlPanel(player);
		if (i != null)
		    player.openInventory(i);
		return true;
	    }
	    // Renter

	    // Info
	    if (d != null) {
		player.openInventory(plugin.infoPanel(player));
		return true;
	    } else {
		// Claim
		Inventory i = plugin.controlPanel(player);
		if (i != null)
		    player.openInventory(i);
		return true;			    
	    }
	case 1:
	    // /" + label + " <command>
	    if (split.length == 0 || split[0].equalsIgnoreCase("help")) { 
		player.sendMessage(ChatColor.GREEN + "Districts " + plugin.getDescription().getVersion() + " help:");

		player.sendMessage(ChatColor.YELLOW + "/" + label + " claim <radius>: " + ChatColor.WHITE + "Claims a square district with you in the middle of it");
		player.sendMessage(ChatColor.YELLOW + "/" + label + " view: " + ChatColor.WHITE + "Toggles the red district boundary visualization on or off");
		player.sendMessage(ChatColor.YELLOW + "/" + label + " pos: " + ChatColor.WHITE + "Sets a position for a district corner");
		player.sendMessage(ChatColor.YELLOW + "/" + label + " balance: " + ChatColor.WHITE + "Shows you how many blocks you have to use for districts");
		player.sendMessage(ChatColor.YELLOW + "/" + label + " remove: " + ChatColor.WHITE + "Removes a district that you are standing in if you are the owner");
		player.sendMessage(ChatColor.YELLOW + "/" + label + " info: " + ChatColor.WHITE + "Shows info on the district you are in");
		if (VaultHelper.setupEconomy() && Settings.blockPrice > 0D && VaultHelper.checkPerm(player,"districts.buyblocks")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " buyblocks: " + ChatColor.WHITE + "Shows the price of blocks");
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " buyblocks <number>: " + ChatColor.WHITE + "Tries to buy some blocks");
		}
		if (VaultHelper.checkPerm(player, "districts.trustplayer") || VaultHelper.checkPerm(player, "districts.advancedplayer")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " trust <playername>: " + ChatColor.WHITE + "Gives player full access to your district");
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " untrust <playername>: " + ChatColor.WHITE + "Revokes trust to your district");
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " untrustall: " + ChatColor.WHITE + "Removes all trusted parties from your district");
		}
		if (VaultHelper.checkPerm(player, "districts.advancedplayer")) {
		    player.sendMessage(ChatColor.YELLOW + "/" + label + " cp: " + ChatColor.WHITE + "Opens the control panel for this district");
		    if (VaultHelper.setupEconomy()) {
			player.sendMessage(ChatColor.YELLOW + "/" + label + " buy: " + ChatColor.WHITE + "Attempts to buy the district you are in");
			player.sendMessage(ChatColor.YELLOW + "/" + label + " rent: " + ChatColor.WHITE + "Attempts to rent the district you are in");
			player.sendMessage(ChatColor.YELLOW + "/" + label + " rent <price>: " + ChatColor.WHITE + "Puts the district you are in up for rent for a weekly rent");
			player.sendMessage(ChatColor.YELLOW + "/" + label + " sell <price>: " + ChatColor.WHITE + "Puts the district you are in up for sale");
			player.sendMessage(ChatColor.YELLOW + "/" + label + " cancel: " + ChatColor.WHITE + "Cancels a For Sale, For Rent or a Lease");
		    }
		}
		return true;
		} else if (split[0].equalsIgnoreCase("cp")) {
		if (VaultHelper.checkPerm(player,"districts.advancedplayer")) {
		    //DistrictRegion d = players.getInDistrict(playerUUID);
		    if (!player.isOp() && (d == null || !d.getOwner().equals(playerUUID))) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errormove));
				} else {
					player.sendMessage(ChatColor.RED + Locale.errormove);
				}
			return true;
		    } else {
			Inventory i = plugin.controlPanel(player);
			if (i != null)
			    player.openInventory(i);
			return true;			    
		    }

		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
			return true;
		}
	    } else if (split[0].equalsIgnoreCase("buyblocks")) {
		if (!VaultHelper.setupEconomy()) {
		    return false;
		}
		if (Settings.blockPrice > 0D && VaultHelper.checkPerm(player,"districts.buyblocks")) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.YELLOW + PlaceholderAPI.setPlaceholders(player, Locale.conversationsblockscost.replace("[cost]", VaultHelper.econ.format(Settings.blockPrice))));
			} else {
				player.sendMessage(ChatColor.YELLOW + Locale.conversationsblockscost.replace("[cost]", VaultHelper.econ.format(Settings.blockPrice)));
			}
		    return true;
		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("untrustall")) {
		if (!VaultHelper.checkPerm(player, "districts.advancedplayer")
			&& !VaultHelper.checkPerm(player, "districts.trustplayer")) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}
		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d == null) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errormove));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errormove);
			}
		    return true;
		}
		if (d.getOwner().equals(playerUUID) || d.getRenter().equals(playerUUID)) {
		    if (d.getOwner().equals(playerUUID)) {
			if (!d.getOwnerTrusted().isEmpty()) {
			    // Tell everyone
			    for (UUID n : d.getOwnerTrustedUUID()) {
				Player p = plugin.getServer().getPlayer(n);
				if (p != null) {
					if (Districts.getPlugin().placeholderapi) {
						p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.trustuntrust.replace("[player]", player.getDisplayName())));
					} else {
						p.sendMessage(ChatColor.RED + Locale.trustuntrust.replace("[player]", player.getDisplayName()));
					}
				}
			    }
			    // Blank it out
			    d.setOwnerTrusted(new ArrayList<UUID>());
			}
		    } else {
			if (!d.getRenterTrusted().isEmpty()) {
			    for (UUID n : d.getRenterTrustedUUID()) {
				Player p = plugin.getServer().getPlayer(n);
				if (p != null) {
					if (Districts.getPlugin().placeholderapi) {
						p.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.trustuntrust.replace("[player]", player.getDisplayName())));
					} else {
						p.sendMessage(ChatColor.RED + Locale.trustuntrust.replace("[player]", player.getDisplayName()));
					}
				}
			    }
			    // Blank it out
			    d.setRenterTrusted(new ArrayList<UUID>());
			}
		    }
		    if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders(player, Locale.trusttitle));
				player.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(player, Locale.trustowners));
			} else {
				player.sendMessage(ChatColor.GOLD + Locale.trusttitle);
				player.sendMessage(ChatColor.GREEN + Locale.trustowners);
			}
		    if (d.getOwnerTrusted().isEmpty()) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.trustnone));
				} else {
					player.sendMessage(Locale.trustnone);
				}
		    } else for (String name : d.getOwnerTrusted()) {
			player.sendMessage(name);
		    }
		    if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(player, Locale.trustrenters));
			} else {
				player.sendMessage(ChatColor.GREEN + Locale.trustrenters);
			}
		    if (d.getRenterTrusted().isEmpty()) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.trustnone));
				} else {
					player.sendMessage(Locale.trustnone);
				}
		    } else for (String name : d.getRenterTrusted()) {
			player.sendMessage(name);
		    }
		    return true;
		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotowner));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotowner);
			}
		    return true;
		}

	    } else if (split[0].equalsIgnoreCase("view")) {
		// Toggle the visualization setting
		if (players.getVisualize(playerUUID)) {
		    Visualization.devisualize(player);
		    player.sendMessage(ChatColor.YELLOW + "Switching district boundary off");
		} else {
		    player.sendMessage(ChatColor.YELLOW + "Switching district boundary on");
		    //DistrictRegion d = players.getInDistrict(playerUUID);
		    if (d != null)
			Visualization.visualize(d, player);
		}
		players.setVisualize(playerUUID, !players.getVisualize(playerUUID));		
		return true;
	    } else if (split[0].equalsIgnoreCase("pos")) {
		// TODO: Put more checks into the setting of a district
		if (players.getInDistrict(playerUUID) != null) {
		    player.sendMessage(ChatColor.RED + "Move out of this district to create another.");
		    return true;
		}
		if (plugin.getPos1s().containsKey(playerUUID)) {
		    int height = Math.abs(plugin.getPos1s().get(playerUUID).getBlockX() - player.getLocation().getBlockX()) + 1;
		    int width = Math.abs(plugin.getPos1s().get(playerUUID).getBlockX() - player.getLocation().getBlockX()) + 1;
		    int blocks = height * width;

		    // Check if they have enough blocks
		    if (blocks > players.getBlockBalance(playerUUID)) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.notenoughblocks));
				} else {
					player.sendMessage(ChatColor.RED + Locale.notenoughblocks);
				}
			player.sendMessage(ChatColor.RED + "Blocks available: " + players.getBlockBalance(playerUUID));
			player.sendMessage(ChatColor.RED + "Blocks required: " + blocks);
			return true;  
		    }
		    if (height < 5 || width < 5) {
			player.sendMessage(ChatColor.RED + "The minimum side distance is 5 blocks");
			return true;		    
		    }
		    // Find the corners of this district
		    Location pos1 = new Location(player.getWorld(),plugin.getPos1s().get(playerUUID).getBlockX(),0,plugin.getPos1s().get(playerUUID).getBlockZ());
		    Location pos2 = new Location(player.getWorld(),player.getLocation().getBlockX(),0,player.getLocation().getBlockZ());
		    if (!plugin.checkDistrictIntersection(pos1, pos2)) {
			plugin.createNewDistrict(pos1, pos2, player);
			players.removeBlocks(playerUUID, blocks);
			players.save(playerUUID);
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders(player, Locale.conversationsdistrictcreated));
			} else {
				player.sendMessage(ChatColor.GOLD + Locale.conversationsdistrictcreated);
			}
			player.sendMessage(ChatColor.GOLD + "You have " + players.getBlockBalance(playerUUID) + " blocks left.");
		    } else {
			player.sendMessage(ChatColor.RED + "That sized district could not be made because it overlaps another district");		    		    
		    }
		} else {
		    plugin.getPos1s().put(playerUUID, player.getLocation());
		    player.sendMessage("Setting position 1 : " + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockZ());
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("remove")) {

		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID) || player.isOp() || VaultHelper.checkPerm(player, "districts.admin")) {
			UUID owner = d.getOwner();
			// If this is an Op or Admin load up the owner
			if (!owner.equals(playerUUID)) {
			    plugin.players.get(owner);
			}
			if (d.getRenter() != null) {
			    player.sendMessage(ChatColor.RED + "District is being rented! Wait until lease is finished first!");
			    return true;
			}
			player.sendMessage(ChatColor.GREEN + "Removing district!");
			// Return blocks
			int height = Math.abs(d.getPos1().getBlockX() - d.getPos2().getBlockX()) + 1;
			int width = Math.abs(d.getPos1().getBlockX() - d.getPos2().getBlockX()) + 1;
			int blocks = height * width;
			// Remove the district
			HashSet<DistrictRegion> ds = plugin.getDistricts();
			ds.remove(d);
			plugin.setDistricts(ds);
			// Delete from the grid
			plugin.getGrid(d.getPos1().getWorld().getName()).deleteDistrictRegion(d);
			// Find everyone who is in this district and remove them
			for (Player p : plugin.getServer().getOnlinePlayers()) {
			    if (d.intersectsDistrict(p.getLocation())) {
				players.setInDistrict(p.getUniqueId(), null);
				Visualization.devisualize(p);
				if (!player.equals(p)) {
				    p.sendMessage(player.getDisplayName() + ChatColor.RED + " removed the district!");
				}
			    }
			}
			// Tell the player what happened and fix any overages if applicable
			if (owner.equals(playerUUID)) {
			    // Owner removing their own district
			    plugin.players.addBlocks(playerUUID, blocks);
			    plugin.showBalance(player);
			} else {
			    // Admin removing district
			    Player o = plugin.getServer().getPlayer(owner);
			    if (o != null) {
				// Online player
				o.sendMessage(ChatColor.RED + "Admin removed a district of yours.");
				plugin.players.addBlocks(playerUUID, blocks);
				plugin.showBalance(o);	    
			    } else {
				plugin.setMessage(owner, "Admin removed a district of yours.");
				plugin.players.addBlocks(owner, blocks);
			    }
			}
			// Save owner
			plugin.players.save(owner);
			return true;
		    }
		    if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotyours));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotyours);
			}
		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotinside));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotinside);
			}
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("balance")) {
		plugin.showBalance(player);
		return true;
	    } else if (split[0].equalsIgnoreCase("buy")) {
		if (!VaultHelper.setupEconomy()) {
		    return false;
		}
		if (!VaultHelper.checkPerm(player, "districts.advancedplayer")) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}
		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d != null) {
		    if (!d.isForSale()) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.sellnotforsale));
				} else {
					player.sendMessage(ChatColor.RED + Locale.sellnotforsale);
				}
			return true;
		    }
		    if (d.getOwner().equals(playerUUID)) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.sellyouareowner));
				} else {
					player.sendMessage(ChatColor.RED + Locale.sellyouareowner);
				}
			return true;
		    } 
		    // See if the player can afford it
		    if (!VaultHelper.econ.has(player, d.getPrice())) {
			player.sendMessage(ChatColor.RED + "You cannot afford " + VaultHelper.econ.format(d.getPrice()));
			return true;
		    }
		    // It's for sale, the player can afford it and it's not the owner - sell!
		    EconomyResponse resp = VaultHelper.econ.withdrawPlayer(player, d.getPrice());
		    if (resp.transactionSuccess()) {
			// Pay the owner
			OfflinePlayer owner = plugin.getServer().getOfflinePlayer(d.getOwner());
			EconomyResponse r = VaultHelper.econ.depositPlayer(owner, d.getPrice());
			if (!r.transactionSuccess()) {
			    plugin.getLogger().severe("Could not pay " + owner.getName() + " " + d.getPrice() + " for district they sold to " + player.getName());
			}
			// Check if owner is online
			if (owner.isOnline()) {
			    Visualization.devisualize((Player)owner);
			    ((Player)owner).sendMessage("You successfully sold a district for " + VaultHelper.econ.format(d.getPrice()) + " to " + player.getDisplayName());
			} else {
			    plugin.setMessage(owner.getUniqueId(), "You successfully sold a district for " + VaultHelper.econ.format(d.getPrice()) + " to " + player.getDisplayName());
			}
			Location pos1 = d.getPos1();
			Location pos2 = d.getPos2();
			player.sendMessage("You purchased the district for "+ VaultHelper.econ.format(d.getPrice()) + "!");
			// Remove the district
			HashSet<DistrictRegion> ds = plugin.getDistricts();
			ds.remove(d);
			plugin.setDistricts(ds);
			// Remove from the grid
			plugin.getGrid(d.getPos1().getWorld().getName()).deleteDistrictRegion(d);
			// Recreate the district for this player
			DistrictRegion newDistrict = plugin.createNewDistrict(pos1, pos2, player);
			players.save(owner.getUniqueId());
			// Add to grid
			plugin.getGrid(newDistrict.getPos1().getWorld().getName()).addToGrid(newDistrict);
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + "There was an economy problem trying to purchase the district for "+ VaultHelper.econ.format(d.getPrice()) + "!");
			player.sendMessage(ChatColor.RED + resp.errorMessage);
			return true;
		    }
		}
		if (Districts.getPlugin().placeholderapi) {
			player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotyours));
		} else {
			player.sendMessage(ChatColor.RED + Locale.errornotyours);
		}
	    } else if (split[0].equalsIgnoreCase("rent")) {
		if (!VaultHelper.setupEconomy()) {
		    return false;
		}
		if (!VaultHelper.checkPerm(player, "districts.advancedplayer")) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}
		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d != null) {
		    if (!d.isForRent()) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.rentnotforrent));
				} else {
					player.sendMessage(ChatColor.RED + Locale.rentnotforrent);
				}
			return true;
		    }
		    if (d.getOwner() != null && d.getOwner().equals(playerUUID)) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.sellyouareowner));
				} else {
					player.sendMessage(ChatColor.RED + Locale.sellyouareowner);
				}
			return true;
		    }
		    if (d.getRenter() != null && d.getRenter().equals(playerUUID)) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.rentalreadyrenting));
				} else {
					player.sendMessage(ChatColor.RED + Locale.rentalreadyrenting);
				}
			return true;			
		    }
		    if (d.isForRent() && d.getRenter() != null) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.rentalreadyleased));
				} else {
					player.sendMessage(ChatColor.RED + Locale.rentalreadyleased);
				}
			return true;						
		    }
		    // See if the player can afford it
		    if (!VaultHelper.econ.has(player, d.getPrice())) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errortooexpensive.replace("[amount]", VaultHelper.econ.format(d.getPrice()))));
				} else {
					player.sendMessage(ChatColor.RED + Locale.errortooexpensive.replace("[amount]", VaultHelper.econ.format(d.getPrice())));
				}
			return true;
		    }
		    // It's for rent, the player can afford it and it's not the owner - rent!
		    EconomyResponse resp = VaultHelper.econ.withdrawPlayer(player, d.getPrice());
		    if (resp.transactionSuccess()) {
			// Check if owner is online
			Player owner = plugin.getServer().getPlayer(d.getOwner());
			if (owner != null) {
			    Visualization.devisualize(owner);
			    owner.sendMessage("You successfully rented a district for " + VaultHelper.econ.format(d.getPrice()) + " to " + player.getDisplayName());
			} else {
			    plugin.setMessage(d.getOwner(), "You successfully rented a district for " + VaultHelper.econ.format(d.getPrice()) + " to " + player.getDisplayName());
			}
			// It will stay for rent until the landlord cancels the lease
			//d.setForRent(false);
			d.setRenter(playerUUID);
			Calendar currentDate = Calendar.getInstance();
			// Only work in days
			currentDate.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
			currentDate.set(Calendar.MINUTE, 0);                 // set minute in hour
			currentDate.set(Calendar.SECOND, 0);                 // set second in minute
			currentDate.set(Calendar.MILLISECOND, 0);            // set millisecond in second
			d.setLastPayment(currentDate.getTime());
			player.sendMessage("You rented the district for "+ VaultHelper.econ.format(d.getPrice()) + " 1 week!");
			d.setEnterMessage("Welcome to " + player.getDisplayName() + "'s rented district!");
			d.setFarewellMessage("Now leaving " + player.getDisplayName() + "'s rented district.");
			players.save(d.getOwner());
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + "There was an economy problem trying to rent the district for "+ VaultHelper.econ.format(d.getPrice()) + "!");
			player.sendMessage(ChatColor.RED + resp.errorMessage);
			return true;
		    }
		}
		if (Districts.getPlugin().placeholderapi) {
			player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotyours));
		} else {
			player.sendMessage(ChatColor.RED + Locale.errornotyours);
		}
	    } else if (split[0].equalsIgnoreCase("cancel")) {
		if (!VaultHelper.setupEconomy()) {
		    return false;
		}
		if (!VaultHelper.checkPerm(player, "districts.advancedplayer") && !player.isOp()) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}
		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// If no one has rented the district yet
			if (d.getRenter() == null) {
				if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders(player, Locale.cancelcancelled));
				} else {
					player.sendMessage(ChatColor.GOLD + Locale.cancelcancelled);
				}
			    d.setForSale(false);
			    d.setForRent(false);
			    d.setPrice(0D);
			    return true;
			} else {
			    player.sendMessage(ChatColor.GOLD + "District is currently leased by " + players.getName(d.getRenter()) + ".");
			    player.sendMessage(ChatColor.GOLD + "Lease will not renew and will terminate in " + plugin.daysToEndOfLease(d) + " days.");
			    if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders(player, Locale.cancelleasestatus3));
				} else {
					player.sendMessage(ChatColor.GOLD + Locale.cancelleasestatus3);
				}
			    if (plugin.getServer().getPlayer(d.getRenter()) != null) {
				plugin.getServer().getPlayer(d.getRenter()).sendMessage( players.getName(d.getOwner()) + " ended a lease you have on a district. It will end in " + plugin.daysToEndOfLease(d) + " days.");
			    } else {
				plugin.setMessage(d.getRenter(), players.getName(d.getOwner()) + " ended a lease you have on a district!");
			    }

			    d.setForSale(false);
			    d.setForRent(false);
			    d.setPrice(0D);
			    return true;

			}
		    } else if (d.getRenter() != null && d.getRenter().equals(player.getUniqueId())) {
			// Check if lease is already cancelled
			if (d.isForRent()) {
			    // Renter wanting to cancel the lease
			    player.sendMessage(ChatColor.GOLD + "Lease renewal cancelled. Lease term finishes in " + plugin.daysToEndOfLease(d) + " days.");
			    if (plugin.getServer().getPlayer(d.getOwner()) != null) {
				plugin.getServer().getPlayer(d.getOwner()).sendMessage( player.getDisplayName() + " canceled a lease with you. It will end in " + plugin.daysToEndOfLease(d) + " days.");
			    } else {
				plugin.setMessage(d.getOwner(), player.getDisplayName() + " canceled a lease with you. It will end in " + plugin.daysToEndOfLease(d) + " days.");
			    }
			    d.setForSale(false);
			    d.setForRent(false);
			    d.setPrice(0D);
			    // Save renter
			    plugin.players.save(playerUUID);
			    plugin.players.save(d.getOwner());
			} else {
			    player.sendMessage(ChatColor.GOLD + "Lease renewal is already cancelled. Lease term finishes in " + plugin.daysToEndOfLease(d) + " days."); 
			}
			return true;
		    } else {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotyours));
				} else {
					player.sendMessage(ChatColor.RED + Locale.errornotyours);
				}
		    }
		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotinside));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotinside);
			}
		}
		return true;


	    } else if (split[0].equalsIgnoreCase("trust") || split[0].equalsIgnoreCase("info")) {
		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d == null) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.infoMove));
			} else {
				player.sendMessage(ChatColor.RED + Locale.infoMove);
			}
		    return true;
		}
		player.openInventory(plugin.infoPanel(player));
		if (Districts.getPlugin().placeholderapi) {
			player.sendMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders(player, Locale.infoinfo));
		} else {
			player.sendMessage(ChatColor.GOLD + Locale.infoinfo);
		}
		if (d.getOwner() != null) {
		    Player owner = plugin.getServer().getPlayer(d.getOwner());
		    if (owner != null) {
			player.sendMessage(ChatColor.YELLOW + "Owner: " + owner.getDisplayName() + " (" + owner.getName() + ")");
		    } else {
			player.sendMessage(ChatColor.YELLOW + "Owner: " + players.getName(d.getOwner()));
		    }
		    if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(player, Locale.infoownerstrusted));
			} else {
				player.sendMessage(ChatColor.GREEN + Locale.infoownerstrusted);
			}
		    if (d.getOwnerTrusted().isEmpty()) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.trustnone));
				} else {
					player.sendMessage(Locale.trustnone);
				}
		    } else for (String name : d.getOwnerTrusted()) {
			player.sendMessage(name);
		    }
		}
		if (d.getRenter() != null) {
		    if (d.isForRent()) {
			player.sendMessage(ChatColor.YELLOW + "Next rent of " + VaultHelper.econ.format(d.getPrice()) + " due in " + plugin.daysToEndOfLease(d) + " days.");
		    } else {
			player.sendMessage(ChatColor.RED + "Lease will end in " + plugin.daysToEndOfLease(d) + " days!");
		    }
		    Player renter = plugin.getServer().getPlayer(d.getRenter());
		    if (renter != null) {
			player.sendMessage(ChatColor.YELLOW + "Renter: " + renter.getDisplayName() + " (" + renter.getName() + ")");
		    } else {
			player.sendMessage(ChatColor.YELLOW + "Renter: " + players.getName(d.getRenter()));
		    }
		    player.sendMessage(ChatColor.GREEN + "[Renter's trusted players]");
		    if (d.getRenterTrusted().isEmpty()) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.trustnone));
				} else {
					player.sendMessage(Locale.trustnone);
				}
		    } else for (String name : d.getRenterTrusted()) {
			player.sendMessage(name);
		    }
		} else {
		    if (d.isForRent()) {
			player.sendMessage(ChatColor.YELLOW + "This district can be leased for " + VaultHelper.econ.format(d.getPrice()));
		    }
		}
		return true;

	    }
	    break;
	case 2:
	    if (split[0].equalsIgnoreCase("buyblocks")) {
		if (!VaultHelper.setupEconomy()) {
		    return false;
		}
		if (Settings.blockPrice > 0D && VaultHelper.checkPerm(player,"districts.buyblocks")) {
		    int num = 0;
		    try {
			num = Integer.valueOf(split[1]);
			if (num < 1) {
			    throw new java.lang.IllegalArgumentException();
			}
		    } catch (Exception e) {
			player.sendMessage(ChatColor.RED + "How many blocks do you want to buy?");
			player.sendMessage(ChatColor.RED + "Blocks cost " + VaultHelper.econ.format(Settings.blockPrice));
			return true;
		    }
		    double cost = Settings.blockPrice * (double)num;
		    if (VaultHelper.econ.has(player, cost)) {
			VaultHelper.econ.withdrawPlayer(player, cost);
			players.addBlocks(player.getUniqueId(), num);
			player.sendMessage(ChatColor.YELLOW + "You bought " + num + " blocks for " + VaultHelper.econ.format(cost));
			return true;
		    }
		    player.sendMessage(ChatColor.RED + "You do not have enough money to buy that many blocks!");
		    player.sendMessage(ChatColor.RED + "Blocks cost " + VaultHelper.econ.format(Settings.blockPrice));
		    return true;
		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}
	    } else if (split[0].equalsIgnoreCase("untrust")) {
		if (!VaultHelper.checkPerm(player, "districts.advancedplayer")
			&&  !VaultHelper.checkPerm(player, "districts.trustplayer")) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}

		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d == null) {
		    player.sendMessage(ChatColor.RED + "Move to a district you own or rent first.");
		    return true;
		}
		if (d.getOwner().equals(playerUUID) || d.getRenter().equals(playerUUID)) {
		    // Check that we know this person
		    UUID trusted = players.getUUID(split[1]);
		    if (trusted == null) {
			player.sendMessage(ChatColor.RED + "Unknown player.");
			return true;			
		    }

		    if (d.getOwner().equals(playerUUID)) {
			if (d.getOwnerTrusted().isEmpty()) {
			    player.sendMessage(ChatColor.RED + "No one is trusted in this district.");
			} else {
			    // Remove trusted player
			    d.removeOwnerTrusted(trusted);
			    Player p = plugin.getServer().getPlayer(trusted);
			    if (p != null) {
				p.sendMessage(ChatColor.RED + player.getDisplayName() + " untrusted you in a district.");
			    }


			}
		    } else {
			if (d.getRenterTrusted().isEmpty()) {
			    player.sendMessage(ChatColor.RED + "No one is trusted in this district.");
			} else {
			    Player p = plugin.getServer().getPlayer(trusted);
			    if (p != null) {
				p.sendMessage(ChatColor.RED + player.getDisplayName() + " untrusted you in a district.");
			    }
			    // Blank it out
			    d.removeRenterTrusted(trusted);
			}
		    }
		    players.save(d.getOwner());
		    if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders(player, Locale.trusttitle));
			} else {
				player.sendMessage(ChatColor.GOLD + Locale.trusttitle);
			}
		    player.sendMessage(ChatColor.GREEN + "[Owner's]");
		    if (d.getOwnerTrusted().isEmpty()) {
		    	if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.trustnone));
				} else {
					player.sendMessage(Locale.trustnone);
				}
		    } else for (String name : d.getOwnerTrusted()) {
			player.sendMessage(name);
		    }
		    if (VaultHelper.checkPerm(player, "districts.advancedplayer")) { 
			player.sendMessage(ChatColor.GREEN + "[Renter's]");
			if (d.getRenterTrusted().isEmpty()) {
				if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.trustnone));
				} else {
					player.sendMessage(Locale.trustnone);
				}
			} else for (String name : d.getRenterTrusted()) {
			    player.sendMessage(name);
			}	
		    }
		    return true;
		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotowner));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotowner);
			}
		    return true;
		}

	    } else if (split[0].equalsIgnoreCase("trust")) {
		if (!VaultHelper.checkPerm(player, "districts.advancedplayer")
			&& !VaultHelper.checkPerm(player, "districts.trustplayer")) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}

		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d == null) {
		    player.sendMessage(ChatColor.RED + "Move to your district first.");
		    return true;
		}
		if ((d.getOwner() != null && d.getOwner().equals(playerUUID)) || (d.getRenter() != null && d.getRenter().equals(playerUUID))) {
		    // Check that we know this person
		    UUID trusted = players.getUUID(split[1]);
		    if (trusted != null) {
			/*
			 * TODO: ADD IN AFTER TESTING!
			if (d.getOwner() != null && d.getOwner().equals(trusted)) {
			    player.sendMessage(ChatColor.RED + "That player is the owner and so trusted already.");
				return true;
			}
			if (d.getRenter() != null && d.getRenter().equals(trusted)) {
			    player.sendMessage(ChatColor.RED + "That player is the renter and so trusted already.");
				return true;
			}*/			
			// This is a known player, name is OK
			if (d.getOwner().equals(playerUUID)) {
			    if (!d.addOwnerTrusted(trusted)) {
				player.sendMessage(ChatColor.RED + "That player is already trusted.");
				return true;
			    }
			} else {
			    if (!d.addRenterTrusted(trusted)) {
				player.sendMessage(ChatColor.RED + "That player is already trusted.");
				return true;
			    } 			    
			}
			Player p = plugin.getServer().getPlayer(trusted);
			if (p != null) {
			    p.sendMessage(ChatColor.RED + player.getDisplayName() + " trusts you in a district.");
			}
			players.save(d.getOwner());
			player.sendMessage(ChatColor.GOLD + "[District Info]");
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(player, Locale.infoownerstrusted));
			} else {
				player.sendMessage(ChatColor.GREEN + Locale.infoownerstrusted);
			}
			if (d.getOwnerTrusted().isEmpty()) {
				if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.trustnone));
				} else {
					player.sendMessage(Locale.trustnone);
				}
			} else for (String name : d.getOwnerTrusted()) {
			    player.sendMessage(name);
			}
			player.sendMessage(ChatColor.GREEN + "[Renter's trusted players]");
			if (d.getRenterTrusted().isEmpty()) {
				if (Districts.getPlugin().placeholderapi) {
					player.sendMessage(PlaceholderAPI.setPlaceholders(player, Locale.trustnone));
				} else {
					player.sendMessage(Locale.trustnone);
				}
			} else for (String name : d.getRenterTrusted()) {
			    player.sendMessage(name);
			}
			return true;
		    } else {
			player.sendMessage(ChatColor.RED + "Unknown player.");
			return true;
		    }
		} else {
		    player.sendMessage(ChatColor.RED + "You must be the owner or renter to add them to this district.");
		    return true;
		}

	    } else if (split[0].equalsIgnoreCase("claim")) {
		// TODO: Put more checks into the setting of a district
		if (players.getInDistrict(playerUUID) != null) {
		    player.sendMessage(ChatColor.RED + "Move out of this district first.");
		    return true;
		}
		int blocks = 0;
		try {
		    blocks = Integer.parseInt(split[1]);
		} catch (Exception e) {
		    player.sendMessage(ChatColor.RED + "/" + label + " claim <number of blocks radius>");
		    return true;		    
		}
		// Check if they have enough blocks
		int blocksRequired = (blocks*2+1)*(blocks*2+1);
		if (blocksRequired > players.getBlockBalance(playerUUID)) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.notenoughblocks));
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.blocksavailable.replace("[number]", String.valueOf(players.getBlockBalance(playerUUID)))));
			} else {
				player.sendMessage(ChatColor.RED + Locale.notenoughblocks);
				player.sendMessage(ChatColor.RED + Locale.blocksavailable.replace("[number]", String.valueOf(players.getBlockBalance(playerUUID))));
			}
		    player.sendMessage(ChatColor.RED + "Blocks required: " + blocksRequired);
		    return true;  
		}
		if (blocks < 2) {
		    player.sendMessage(ChatColor.RED + "The minimum radius is 2 blocks");
		    return true;		    
		}
		// Find the corners of this district
		Location pos1 = new Location(player.getWorld(),player.getLocation().getBlockX()-blocks,0,player.getLocation().getBlockZ()-blocks);
		Location pos2 = new Location(player.getWorld(),player.getLocation().getBlockX()+blocks,0,player.getLocation().getBlockZ()+blocks);
		if (!plugin.checkDistrictIntersection(pos1, pos2)) {
		    plugin.createNewDistrict(pos1, pos2, player);
		    players.removeBlocks(playerUUID, blocksRequired);
		    if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders(player, Locale.conversationsdistrictcreated));
			} else {
				player.sendMessage(ChatColor.GOLD + Locale.conversationsdistrictcreated);
			}
		    player.sendMessage(ChatColor.GOLD + "You have " + players.getBlockBalance(playerUUID) + " blocks left.");
		    players.save(playerUUID);
		} else {
		    player.sendMessage(ChatColor.RED + "That sized district could not be made because it overlaps another district");		    		    
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("sell")) { 
		if (!VaultHelper.setupEconomy()) {
		    return false;
		}
		if (!VaultHelper.checkPerm(player, "districts.advancedplayer")) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}

		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// Check to see if it is being rented right now
			if (d.getRenter() != null) {
			    player.sendMessage(ChatColor.RED + "The district is being rented at this time. Wait until the lease expires.");
			    return true;
			}
			double price = 0D;
			try {
			    price = Double.parseDouble(split[1]);
			} catch (Exception e) {
			    player.sendMessage(ChatColor.RED+"The price is invalid (must be >= "+ VaultHelper.econ.format(1D)+")");
			    return true;
			}
			if (price <1D) {
			    player.sendMessage(ChatColor.RED+"The price is invalid (must be >= "+ VaultHelper.econ.format(1D)+")");
			    return true;  
			}
			player.sendMessage(ChatColor.GOLD + "Putting district up for sale for " + VaultHelper.econ.format(price));
			d.setForSale(true);
			d.setPrice(price);
			d.setForRent(false);
			return true;
		    }
		    if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotyours));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotyours);
			}
		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotinside));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotinside);
			}
		}
		return true;

	    } else if (split[0].equalsIgnoreCase("rent")) {
		if (!VaultHelper.setupEconomy()) {
		    return false;
		}
		if (!VaultHelper.checkPerm(player, "districts.advancedplayer")) {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornoPermission));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornoPermission);
			}
		    return true;
		}

		//DistrictRegion d = players.getInDistrict(playerUUID);
		if (d != null) {
		    if (d.getOwner().equals(playerUUID)) {
			// Check to see if it is being rented right now
			if (d.getRenter() != null) {
			    player.sendMessage(ChatColor.RED+"The district is currently rented!");
			    player.sendMessage(ChatColor.RED+"To end the renter's lease at the next due date, use /d cancel.");
			    return true;
			}
			double price = 0D;
			try {
			    price = Double.parseDouble(split[1]);
			} catch (Exception e) {
			    player.sendMessage(ChatColor.RED+"The rent is invalid (must be >= "+ VaultHelper.econ.format(1D)+")");
			    return true;
			}
			if (price <1D) {
			    player.sendMessage(ChatColor.RED+"The rent is invalid (must be >= "+ VaultHelper.econ.format(1D)+")");
			    return true;  
			}
			player.sendMessage(ChatColor.GOLD + "Putting district up for rent for " + VaultHelper.econ.format(price));
			d.setForRent(true);
			d.setForSale(false);
			d.setPrice(price);
			return true;
		    }
		    if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotyours));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotyours);
			}
		} else {
			if (Districts.getPlugin().placeholderapi) {
				player.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(player, Locale.errornotinside));
			} else {
				player.sendMessage(ChatColor.RED + Locale.errornotinside);
			}
		}
		return true;
	    }

	}
	return false;
    }
}