package com.wasteofplastic.districts;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

/**
 * Handles the conversation to buy/sell
 * @author tastybento
 *
 */
public class ConversationSellBuy implements Prompt {
    public enum Type { RENT, SELL };

    private Districts plugin;
    private Type type;

    /**
     * @param plugin
     */
    public ConversationSellBuy(Districts plugin, Type type) {
	this.plugin = plugin;
	this.type = type;
    }

    @Override
    public String getPromptText(ConversationContext context) {
	switch (type) {
	case RENT:
		return ChatColor.AQUA + Locale.conversationsenterrent;
	case SELL:
		return ChatColor.AQUA + Locale.conversationsenterprice;
	default:
	}
		return ChatColor.AQUA + Locale.conversationsenteramount;
    }

    @Override
    public boolean blocksForInput(ConversationContext context) {
	return true;
    }

    @Override
    public Prompt acceptInput(ConversationContext context, String input) {
	if (input.isEmpty()) {
	    return END_OF_CONVERSATION;
	}
	DistrictRegion d = (DistrictRegion) context.getSessionData("District");
	double price = 0D;
	if (d != null) {
	    try {
		price = Double.valueOf(input);
		if (price <= 1D) {
		    context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationsmustbemore.replace("[price]",VaultHelper.econ.format(1D)));
		    return this;
		}
	    } catch (Exception e) {
		context.getForWhom().sendRawMessage(ChatColor.RED + Locale.conversationshowmuch);
		return this;
	    }
	    switch (type) {
	    case RENT:
		context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.rentforrent.replace("[price]", VaultHelper.econ.format(price)));
		d.setForRent(true);
		d.setForSale(false);
		d.setPrice(price);
		break;
	    case SELL:
		context.getForWhom().sendRawMessage(ChatColor.GOLD + Locale.sellforsale.replace("[price]", VaultHelper.econ.format(price)));
		d.setForSale(true);
		d.setPrice(price);
		d.setForRent(false);
		break;
	    default:
		break;
	    }
	}
	return END_OF_CONVERSATION;
    }
}
