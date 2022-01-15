package com.wasteofplastic.districts;

import java.util.UUID;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

/**
 * Obtains trusted or untrusted players
 * @author ben
 *
 */
public class GetPlayers implements Prompt {
    public enum Type { TRUST, UNTRUST };

    private Districts plugin;
    private Type type;

    /**
     * @param plugin
     */
    public GetPlayers(Districts plugin, Type type) {
	this.plugin = plugin;
	this.type = type;
    }

    @Override
    public String getPromptText(ConversationContext context) {
	if (type.equals(Type.TRUST))
	    return ChatColor.AQUA + "Enter player names one by one or END";
	else
	    return ChatColor.AQUA + "Enter player names one by one, ALL or END";
    }

    @Override
    public boolean blocksForInput(ConversationContext context) {
	return true;
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
	if (input.isEmpty() || input.equalsIgnoreCase("END")) {
	    context.getForWhom().sendRawMessage(ChatColor.RED + "Ended.");
	    return END_OF_CONVERSATION;
	}
	DistrictRegion d = (DistrictRegion) context.getSessionData("District");
	if (d == null) {
	    context.getForWhom().sendRawMessage(ChatColor.RED + "Move to a district you own or rent first.");
	    return END_OF_CONVERSATION;
	}
	UUID playerUUID = ((Player)context.getForWhom()).getUniqueId();
	if (type.equals(Type.UNTRUST)) {
	    if (input.equalsIgnoreCase("ALL")) {
		((Player)context.getForWhom()).performCommand("district untrustall");
		context.getForWhom().sendRawMessage(ChatColor.GREEN + "All trusted players removed!");
		return END_OF_CONVERSATION;
		/*
		if (d.getOwner().equals(playerUUID) || d.getRenter().equals(playerUUID)) {
		    if (d.getOwner().equals(playerUUID)) {
			if (!d.getOwnerTrusted().isEmpty()) {
			    // Tell everyone
			    for (UUID n : d.getOwnerTrustedUUID()) {
				Player p = plugin.getServer().getPlayer(n);
				if (p != null) {
				    p.sendMessage(ChatColor.RED + ((Player)context.getForWhom()).getDisplayName() + " untrusted you in a district.");
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
				    p.sendMessage(ChatColor.RED + ((Player)context.getForWhom()).getDisplayName() + " untrusted you in a district.");
				}
			    }
			    // Blank it out
			    d.setRenterTrusted(new ArrayList<UUID>());
			}
		    }
		}
		context.getForWhom().sendRawMessage(ChatColor.GREEN + "All trusted players removed!");
		return END_OF_CONVERSATION;
		 */
	    } else {
		//((Player)context.getForWhom()).performCommand("district untrust " + input);
		//return this;
		// Untrust individual players
		if (d.getOwner().equals(playerUUID) || (d.getRenter() != null && d.getRenter().equals(playerUUID))) {
		    // Check that we know this person
		    UUID trusted = plugin.players.getUUID(input);
		    if (trusted == null) {
			context.getForWhom().sendRawMessage(ChatColor.RED + "Unknown player.");
			return this;			
		    }

		    if (d.getOwner().equals(playerUUID)) {
			if (d.getOwnerTrusted().isEmpty()) {
			    context.getForWhom().sendRawMessage(ChatColor.RED + "No one is trusted in this district.");
			} else {
			    // Remove trusted player
			    d.removeOwnerTrusted(trusted);
			    Player p = plugin.getServer().getPlayer(trusted);
			    if (p != null) {
				p.sendMessage(ChatColor.RED + ((Player)context.getForWhom()).getDisplayName() + " untrusted you in a district.");
			    }


			}
		    } else {
			if (d.getRenterTrusted().isEmpty()) {
			    context.getForWhom().sendRawMessage(ChatColor.RED + "No one is trusted in this district.");
			} else {
			    Player p = plugin.getServer().getPlayer(trusted);
			    if (p != null) {
				p.sendMessage(ChatColor.RED + ((Player)context.getForWhom()).getDisplayName() + " untrusted you in a district.");
			    }
			    // Blank it out
			    d.removeRenterTrusted(trusted);
			}
		    }
		    plugin.players.save(d.getOwner());
		    if (Districts.getPlugin().placeholderapi) {
				context.getForWhom().sendRawMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.trusttitle));
				context.getForWhom().sendRawMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.trustowners));
			} else {
				context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.trusttitle);
				context.getForWhom().sendRawMessage(ChatColor.GREEN + Locale.trustowners);
			}
		    if (d.getOwnerTrusted().isEmpty()) {
		    	if (Districts.getPlugin().placeholderapi) {
					context.getForWhom().sendRawMessage(PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.trustnone));
				} else {
					context.getForWhom().sendRawMessage(Locale.trustnone);
				}
		    } else for (String name : d.getOwnerTrusted()) {
			context.getForWhom().sendRawMessage(name);
		    }
		    if (VaultHelper.checkPerm((Player)context.getForWhom(), "districts.advancedplayer")) {
		    	if (Districts.getPlugin().placeholderapi) {
					context.getForWhom().sendRawMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.trustrenters));
				} else {
					context.getForWhom().sendRawMessage(ChatColor.GREEN + Locale.trustrenters);
				}
			if (d.getRenterTrusted().isEmpty()) {
				if (Districts.getPlugin().placeholderapi) {
					context.getForWhom().sendRawMessage(PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.trustnone));
				} else {
					context.getForWhom().sendRawMessage(Locale.trustnone);
				}
			} else for (String name : d.getRenterTrusted()) {
			    context.getForWhom().sendRawMessage(name);
			}
		    }
		    return this;
		} else {
			if (Districts.getPlugin().placeholderapi) {
				context.getForWhom().sendRawMessage(ChatColor.RED + PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.errornotowner));
			} else {
				context.getForWhom().sendRawMessage(ChatColor.RED + Locale.errornotowner);
			}
		    return END_OF_CONVERSATION;
		}
	    }

	} else if (type.equals(Type.TRUST)) {
	    //Wish I could do this...
	    //((Player)context.getForWhom()).performCommand("district trust " + input);
	    //return this;
	    if ((d.getOwner() != null && d.getOwner().equals(playerUUID)) || (d.getRenter() != null && d.getRenter().equals(playerUUID))) {
		// Check that we know this person
		UUID trusted = plugin.players.getUUID(input);
		if (trusted != null) {
		    if (d.getOwner() != null && d.getOwner().equals(trusted)) {
			context.getForWhom().sendRawMessage(ChatColor.RED + "That player is the owner and so trusted already.");
			return this;
		    }
		    if (d.getRenter() != null && d.getRenter().equals(trusted)) {
			context.getForWhom().sendRawMessage(ChatColor.RED + "That player is the renter and so trusted already.");
			return this;
		    }			
		    // This is a known player, name is OK
		    if (d.getOwner().equals(playerUUID)) {
			if (!d.addOwnerTrusted(trusted)) {
			    context.getForWhom().sendRawMessage(ChatColor.RED + "That player is already trusted.");
			    return this;
			}
		    } else {
			if (!d.addRenterTrusted(trusted)) {
			    context.getForWhom().sendRawMessage(ChatColor.RED + "That player is already trusted.");
			    return this;
			} 			    
		    }
		    Player p = plugin.getServer().getPlayer(trusted);
		    if (p != null) {
			p.sendMessage(ChatColor.RED + ((Player)context.getForWhom()).getDisplayName() + " trusts you in a district.");
		    }
		    plugin.players.save(d.getOwner());
		    if (Districts.getPlugin().placeholderapi) {
		    	context.getForWhom().sendRawMessage(ChatColor.GOLD + PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.infotitle));
				context.getForWhom().sendRawMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.infoownerstrusted));
			} else {
				context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.infotitle);
				context.getForWhom().sendRawMessage(ChatColor.GREEN + Locale.infoownerstrusted);
			}
		    if (d.getOwnerTrusted().isEmpty()) {
		    	if (Districts.getPlugin().placeholderapi) {
					context.getForWhom().sendRawMessage(PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.infonone));
				} else {
					context.getForWhom().sendRawMessage(Locale.infonone);
				}
		    } else for (String name : d.getOwnerTrusted()) {
			context.getForWhom().sendRawMessage(name);
		    }
		    if (VaultHelper.checkPerm((Player)context.getForWhom(), "districts.advancedplayer")) {
		    	if (Districts.getPlugin().placeholderapi) {
					context.getForWhom().sendRawMessage(ChatColor.GREEN + PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.inforenterstrusted));
				} else {
					context.getForWhom().sendRawMessage(ChatColor.GREEN + Locale.inforenterstrusted);
				}
			if (d.getRenterTrusted().isEmpty()) {
				if (Districts.getPlugin().placeholderapi) {
					context.getForWhom().sendRawMessage(PlaceholderAPI.setPlaceholders((Player) context.getForWhom(), Locale.infonone));
				} else {
					context.getForWhom().sendRawMessage(Locale.infonone);
				}
			} else for (String name : d.getRenterTrusted()) {
			    context.getForWhom().sendRawMessage(name);
			}
		    }
		    return this;
		} else {
		    context.getForWhom().sendRawMessage(ChatColor.RED + "Unknown player.");
		    return this;
		}
	    } else {
		context.getForWhom().sendRawMessage(ChatColor.RED + "You must be the owner or renter to add them to this district.");
		return this;
	    }
	}
	return END_OF_CONVERSATION;
    }
}
