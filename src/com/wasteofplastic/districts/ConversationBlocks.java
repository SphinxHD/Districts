package com.wasteofplastic.districts;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

/**
 * Implements a negotiation over how many blocks to buy or claim
 * @author ben
 *
 */
public class ConversationBlocks implements Prompt {
    public enum Type { BUY, CLAIM };

    private Districts plugin;
    private Type type;

    /**
     * @param plugin
     */
    public ConversationBlocks(Districts plugin, Type type) {
	this.plugin = plugin;
	this.type = type;
    }

    @Override
    public String getPromptText(ConversationContext context) {
	switch (type) {
	case BUY:
	    return ChatColor.AQUA + Locale.conversationsenterblocknum.replace("[price]", VaultHelper.econ.format(Settings.blockPrice));
	case CLAIM:
	    return ChatColor.AQUA + Locale.conversationsenterradius;
	default:
	}
	return ChatColor.AQUA + Locale.conversationsenterblocks;
    }

    @Override
    public boolean blocksForInput(ConversationContext context) {
	return true;
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
	if (input.isEmpty()) {
	    context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsended);
	    return END_OF_CONVERSATION;
	}
	DistrictRegion d = (DistrictRegion) context.getSessionData("District");
	if (d != null && type.equals(Type.CLAIM)) {
	    context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsmove);
	    return END_OF_CONVERSATION;
	}
	Player player = ((Player)context.getForWhom());
	UUID playerUUID = player.getUniqueId();
	// Get the input
	int blocks = 0;
	try {
	    blocks = Integer.parseInt(input);
	} catch (Exception e) {
	    context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationshowmany);
	    return this;
	}
	if (blocks == 0) {
	    context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsended);
	    return END_OF_CONVERSATION;
	}
	switch (type) {
	case BUY:
	    
	    // Player wants to buy blocks
	    if (Settings.blockPrice > 0D && VaultHelper.checkPerm((Player)context.getForWhom(), "districts.buyblocks")) {
		double cost = Settings.blockPrice * (double)blocks;
		double balance = VaultHelper.econ.getBalance(player);
		if (balance >= cost) {
		    VaultHelper.econ.withdrawPlayer(player, cost);
		    plugin.players.addBlocks(player.getUniqueId(), blocks);
		    context.getForWhom().sendRawMessage(ChatColor.YELLOW + Locale.conversationsyoubought.replace("[number]", String.valueOf(blocks)).replace("[cost]", VaultHelper.econ.format(cost)));
		    return END_OF_CONVERSATION;
		}
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.errortooexpensive.replace("[price]",VaultHelper.econ.format(cost)));
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsblockscost.replace("[cost]", VaultHelper.econ.format(Settings.blockPrice)));
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsyouhave.replace("[balance]", VaultHelper.econ.format(balance)));
		return this;
	    } else {
		player.sendMessage(ChatColor.RED + Locale.errornoPermission);
		return END_OF_CONVERSATION;
	    }
	case CLAIM:
	    // Check if they have enough blocks	
	    int blocksRequired = (blocks*2+1)*(blocks*2+1);
	    if (blocksRequired > plugin.players.getBlockBalance(playerUUID)) {
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.notenoughblocks);
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.blocksavailable.replace("[number]", String.valueOf(plugin.players.getBlockBalance(playerUUID))));
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsblocksrequired.replace("[number]", String.valueOf(blocksRequired)));
		return this;  
	    }
	    if (blocks < 2) {
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsminimumradius);
		return this;		    
	    }
	    // Find the corners of this district
	    Location pos1 = new Location(player.getWorld(),player.getLocation().getBlockX()-blocks,0,player.getLocation().getBlockZ()-blocks);
	    Location pos2 = new Location(player.getWorld(),player.getLocation().getBlockX()+blocks,0,player.getLocation().getBlockZ()+blocks);
	    if (!plugin.checkDistrictIntersection(pos1, pos2)) {
		plugin.createNewDistrict(pos1, pos2, player);
		plugin.players.removeBlocks(playerUUID, blocksRequired);
		context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.conversationsdistrictcreated);
		context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.conversationsyounowhave.replace("[number]", String.valueOf(plugin.players.getBlockBalance(playerUUID))));
		plugin.players.save(playerUUID);
		return END_OF_CONVERSATION;
	    } else {
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsoverlap);		    		    
		return this;
	    }
	default:
	    break;
	}
	return END_OF_CONVERSATION;
    }
}
