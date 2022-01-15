package com.wasteofplastic.districts;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

/**
 * Handles the naming conversation thread
 * @author tastybento
 *
 */
public class ConversationNaming implements Prompt {

    private Districts plugin;

    /**
     * @param plugin
     */
    public ConversationNaming(Districts plugin) {
	this.plugin = plugin;
    }

    @Override
    public String getPromptText(ConversationContext context) {
	return ChatColor.AQUA + Locale.conversationsentername;
    }

    @Override
    public boolean blocksForInput(ConversationContext context) {
	return true;
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
	DistrictRegion d = (DistrictRegion) context.getSessionData("District");
	if (d != null) {
	    UUID o = d.getOwner();
	    UUID r = d.getRenter();
	    // Find out if these guys are online
	    Player owner = plugin.getServer().getPlayer(o);
	    Player renter = plugin.getServer().getPlayer(r);
	    if (input.equalsIgnoreCase("default")) {
		// No renter, just owner
		if (r == null) {
		    context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.conversationssettingownerdefault);
		    if (owner != null) {
			d.setEnterMessage(Locale.messagesenter.replace("[owner]", owner.getDisplayName()));
			d.setFarewellMessage(Locale.messagesleave.replace("[owner]", owner.getDisplayName()));
		    } else {
			d.setEnterMessage(Locale.messagesenter.replace("[owner]", plugin.players.getName(o)));
			d.setFarewellMessage(Locale.messagesleave.replace("[owner]", plugin.players.getName(o)));			
		    }
		} else {
		    context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.conversationssettingrenterdefault);
		    if (renter != null) {
			d.setEnterMessage(Locale.messagesenter.replace("[renter]", renter.getDisplayName()));
			d.setFarewellMessage(Locale.messagesleave.replace("[renter]", renter.getDisplayName()));
		    } else {
			d.setEnterMessage(Locale.messagesenter.replace("[renter]", plugin.players.getName(r)));
			d.setFarewellMessage(Locale.messagesleave.replace("[renter]", plugin.players.getName(r)));			
		    }   
		}
	    } else if (input.equalsIgnoreCase("none")) {
		context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.conversationsnomessage);
		d.setEnterMessage("");
		d.setFarewellMessage("");
	    } else {
		context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.conversationssettingto.replace("[name]", input));
		d.setEnterMessage(ChatColor.translateAlternateColorCodes('&', Locale.conversationsentermessage.replace("[name]", input)));
		d.setFarewellMessage(ChatColor.translateAlternateColorCodes('&', Locale.conversationsleavingmessage.replace("[name]", input)));
	    }
	}
	return END_OF_CONVERSATION;
    }

}
