package com.wasteofplastic.districts;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This class handles commands for admins
 * 
 */
public class AdminCmd implements CommandExecutor {
    private Districts plugin;
    private PlayerCache players;
    public AdminCmd(Districts districts, PlayerCache players) {
	this.plugin = districts;
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
	// Check for permissions
	if (sender instanceof Player) {
	    if (!VaultHelper.checkPerm(((Player)sender), "districts.admin")) {
		sender.sendMessage(ChatColor.RED + Locale.errornoPermission);
		return true;
	    }
	}
	Player p = (Player) sender;
	// Check for zero parameters e.g., /dadmin
	switch (split.length) {
	case 0:
		if (Districts.getPlugin().placeholderapi) {
			sender.sendMessage(ChatColor.YELLOW + "/dadmin reload:" + ChatColor.WHITE + " " + PlaceholderAPI.setPlaceholders(p, Locale.adminHelpreload));
			sender.sendMessage(ChatColor.YELLOW + "/dadmin balance <player>:" + ChatColor.WHITE + " " + PlaceholderAPI.setPlaceholders(p, Locale.adminHelpbalance));
			sender.sendMessage(ChatColor.YELLOW + "/dadmin info <player>:" + ChatColor.WHITE + " " + PlaceholderAPI.setPlaceholders(p, Locale.adminHelpinfo));
			sender.sendMessage(ChatColor.YELLOW + "/dadmin info:" + ChatColor.WHITE + " " + PlaceholderAPI.setPlaceholders(p, Locale.adminHelpinfo2));
			//sender.sendMessage(ChatColor.YELLOW + "/dadmin delete:" + ChatColor.WHITE + " " + Locale.adminHelpdelete);
			sender.sendMessage(ChatColor.YELLOW + "/dadmin give <player> <blocks>:" + ChatColor.WHITE + " " + PlaceholderAPI.setPlaceholders(p, Locale.adminHelpgive));
			sender.sendMessage(ChatColor.YELLOW + "/dadmin take <player> <blocks>:" + ChatColor.WHITE + " " + PlaceholderAPI.setPlaceholders(p, Locale.adminHelptake));
			sender.sendMessage(ChatColor.YELLOW + "/dadmin set <player> <blocks>:" + ChatColor.WHITE + " " + PlaceholderAPI.setPlaceholders(p, Locale.adminHelpset));
			sender.sendMessage(ChatColor.YELLOW + "/dadmin evict:" + ChatColor.WHITE + " " + PlaceholderAPI.setPlaceholders(p, Locale.adminHelpevict));
			return true;
		} else {
			sender.sendMessage(ChatColor.YELLOW + "/dadmin reload:" + ChatColor.WHITE + " " + Locale.adminHelpreload);
			sender.sendMessage(ChatColor.YELLOW + "/dadmin balance <player>:" + ChatColor.WHITE + " " + Locale.adminHelpbalance);
			sender.sendMessage(ChatColor.YELLOW + "/dadmin info <player>:" + ChatColor.WHITE + " " + Locale.adminHelpinfo);
			sender.sendMessage(ChatColor.YELLOW + "/dadmin info:" + ChatColor.WHITE + " " + Locale.adminHelpinfo2);
			//sender.sendMessage(ChatColor.YELLOW + "/dadmin delete:" + ChatColor.WHITE + " " + Locale.adminHelpdelete);
			sender.sendMessage(ChatColor.YELLOW + "/dadmin give <player> <blocks>:" + ChatColor.WHITE + " " + Locale.adminHelpgive);
			sender.sendMessage(ChatColor.YELLOW + "/dadmin take <player> <blocks>:" + ChatColor.WHITE + " " + Locale.adminHelptake);
			sender.sendMessage(ChatColor.YELLOW + "/dadmin set <player> <blocks>:" + ChatColor.WHITE + " " + Locale.adminHelpset);
			sender.sendMessage(ChatColor.YELLOW + "/dadmin evict:" + ChatColor.WHITE + " " + Locale.adminHelpevict);
			return true;
		}
	case 1:
	    if (split[0].equalsIgnoreCase("reload")) {
		plugin.reloadConfig();
		plugin.loadPluginConfig();
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.YELLOW + PlaceholderAPI.setPlaceholders(p, Locale.reloadconfigReloaded));
			} else {
				sender.sendMessage(ChatColor.YELLOW + Locale.reloadconfigReloaded);
			}
			return true;
	    } else if (split[0].equalsIgnoreCase("evict")) {
		if (!(sender instanceof Player)) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errorInGameCommand));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errorInGameCommand);
			}
		    return true;
		}
		Player player = (Player)sender;
		DistrictRegion d = players.getInDistrict(player.getUniqueId());
		if (d == null || d.getRenter() == null) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errormove));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errormove);
			}
			return true;
		}
		// Remove the lease and renter
		d.setForRent(false);
		d.setRenter(null);
		d.setRenterTrusted(new ArrayList<UUID>());
		d.setLastPayment(null);
		d.setPrice(0D);
		d.setForSale(false);
		players.save(d.getOwner());
		if (Districts.getPlugin().placeholderapi) {
			sender.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.eventsrenterEvicted));
		} else {
			sender.sendMessage(ChatColor.GREEN + Locale.eventsrenterEvicted);
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		if (!(sender instanceof Player)) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errorInGameCommand));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errorInGameCommand);
			}
		    return true;
		}
		Player player = (Player)sender;
		DistrictRegion d = players.getInDistrict(player.getUniqueId());
		if (d == null) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.infoMove));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.infoMove);
			}
		    return true;
		}
		if (Districts.getPlugin().placeholderapi) {
			sender.sendMessage(ChatColor.GREEN + "[" + PlaceholderAPI.setPlaceholders(p, Locale.infoPanelTitle) + "]");
			sender.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.generalowner) + ":" + players.getName(d.getOwner()));
		} else {
			sender.sendMessage(ChatColor.GREEN + "[" + Locale.infoPanelTitle + "]");
			sender.sendMessage(ChatColor.GREEN + Locale.generalowner + ":" + players.getName(d.getOwner()));
		}
		String trusted = "";
		for (String name : d.getOwnerTrusted()) {
		    trusted += name + ",";
		}
		if (!trusted.isEmpty()) {
		    if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.infoownerstrusted) + " " + ChatColor.WHITE + trusted.substring(0, trusted.length() - 1));
			} else {
				sender.sendMessage(ChatColor.GREEN + Locale.infoownerstrusted + " " + ChatColor.WHITE + trusted.substring(0, trusted.length() - 1));
			}
		}
		if (d.getRenter() != null) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.generalrenter) + ":" + players.getName(d.getRenter()));
			} else {
				sender.sendMessage(ChatColor.GREEN + Locale.generalrenter + ":" + players.getName(d.getRenter()));
			}
		}
		trusted = "";
		for (String name : d.getRenterTrusted()) {
		    trusted += name + ",";
		}
		if (!trusted.isEmpty()) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.inforenterstrusted) + " " + ChatColor.WHITE + trusted.substring(0, trusted.length() - 1));
			} else {
				sender.sendMessage(ChatColor.GREEN + Locale.inforenterstrusted + " " + ChatColor.WHITE + trusted.substring(0, trusted.length() - 1));
			}
		}
		sender.sendMessage(ChatColor.GREEN + "District Flags:");
		for (String flag : d.getFlags().keySet()) {
		    sender.sendMessage(flag + ": " + d.getFlags().get(flag));
		}
		return true;
	    } else {
	    	if (Districts.getPlugin().placeholderapi) {
	    		sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errorunknownCommand));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errorunknownCommand);
			}
		return false;
	    }
	case 2:
	    if (split[0].equalsIgnoreCase("balance")) {
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errorunknownPlayer));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errorunknownPlayer);
			}
		    return true;
		} else {	
		    sender.sendMessage(ChatColor.GOLD + "Block balance: " + players.getBlockBalance(playerUUID));
		}
		return true;
	    } else if (split[0].equalsIgnoreCase("info")) {
		// Convert name to a UUID
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errorunknownPlayer));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errorunknownPlayer);
			}
		    return true;
		} else {	
		    sender.sendMessage(ChatColor.GREEN + players.getName(playerUUID));
		    sender.sendMessage(ChatColor.WHITE + "UUID:" + playerUUID.toString());
		    try {
			Date d = new Date(plugin.getServer().getOfflinePlayer(playerUUID).getLastPlayed() * 1000);
			sender.sendMessage(ChatColor.WHITE + "Last login:" + d.toString());
		    } catch (Exception e) {}
		    sender.sendMessage(ChatColor.GOLD + "Block balance: " + players.getBlockBalance(playerUUID));
		}
		return true;
	    } else {
		// Unknown command
		return false;
	    }
	case 3:
	    if (split[0].equalsIgnoreCase("give")) {
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errorunknownPlayer));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errorunknownPlayer);
			}
		    return true;
		} else {	
		    try {
			int blocks = Integer.parseInt(split[2]);
			sender.sendMessage(ChatColor.GOLD + "New block balance: " +players.addBlocks(playerUUID, blocks));
		    } catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Unrecognized number of blocks");
			return true;
		    }

		}
		return true;
	    } else if (split[0].equalsIgnoreCase("take")) {
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errorunknownPlayer));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errorunknownPlayer);
			}
		    return true;
		} else {	
		    try {
			int blocks = Integer.parseInt(split[2]);
			sender.sendMessage(ChatColor.GOLD + "New block balance: " +players.removeBlocks(playerUUID, blocks));
		    } catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Unrecognized number of blocks");
			return true;
		    }

		}
		return true;
	    } else if (split[0].equalsIgnoreCase("set")) {
		final UUID playerUUID = players.getUUID(split[1]);
		if (playerUUID == null) {
			if (Districts.getPlugin().placeholderapi) {
				sender.sendMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.errorunknownPlayer));
			} else {
				sender.sendMessage(ChatColor.RED + Locale.errorunknownPlayer);
			}
		    return true;
		} else {	
		    try {
			int blocks = Integer.parseInt(split[2]);
			sender.sendMessage(ChatColor.GOLD + "New block balance: " + players.setBlocks(playerUUID, blocks));
		    } catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Unrecognized number of blocks");
			return true;
		    }

		}
		return true;
	    } 
	    return false;
	default:
	    return false;
	}
    }
}