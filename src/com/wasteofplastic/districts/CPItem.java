package com.wasteofplastic.districts;

import java.util.ArrayList;
import java.util.List;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Holds all the control panel item info
 * @author tastybento
 *
 */
public class CPItem {
    private ItemStack item;
    private List<String> description = new ArrayList<String>();
    private String name;
    private int slot;
    private boolean flagValue;
    public enum Type { TOGGLE, TEXT, INFO, REMOVE, BUYBLOCKS, SELL, RENT, CANCEL, TRUST, UNTRUST, VISUALIZE, CLAIM, TOGGLEINFO };
    private Type type;

    /**
     * //@param item
     * @param material
     * @param durability 
     * @param name
     * //@param b
     * //@param nextSection
     */
    public CPItem(Material material, int durability, String name, boolean flagValue, int slot, List<String> desc, Type type, Player p) {
	item = new ItemStack(material);
	item.setDurability((short) durability);
	createItem(item, name, flagValue,slot,desc,type,p);
    }

    /**
     * @param item
     * @param name
     * @param flagValue
     * @param slot
     * @param desc
     * @param type
     */
    public CPItem(ItemStack item, String name, boolean flagValue, int slot, List<String> desc, Type type, Player p) {
	createItem(item, name, flagValue, slot,desc, type, p);
    }
    
    private void createItem(ItemStack item, String name, boolean flagValue, int slot, List<String> desc, Type type, Player p) {
	this.item = item;
	this.flagValue = flagValue;
	this.slot = slot;
	this.name = name;
	this.type = type;
	description.clear();
	ItemMeta meta = item.getItemMeta();
	switch (type) {
	case TOGGLEINFO:
	    meta.setDisplayName(name.substring(5));
	    if (flagValue) {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpallowed));
			} else {
				description.add(ChatColor.GREEN + Locale.cpallowed);
			}
	    } else {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpdisallowed));
			} else {
				description.add(ChatColor.RED + Locale.cpdisallowed);
			}
	    }
	    meta.setLore(description);
	    item.setItemMeta(meta);
	    break;
	case BUYBLOCKS:
	    meta.setDisplayName(name);
	    if (Districts.getPlugin().placeholderapi) {
			description.add(ChatColor.GOLD + PlaceholderAPI.setPlaceholders(p, Locale.conversationsenterblocknum.replace("[price]", VaultHelper.econ.format(Settings.blockPrice))));
			description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktobuy));
		} else {
			description.add(ChatColor.GOLD + Locale.conversationsenterblocknum.replace("[price]", VaultHelper.econ.format(Settings.blockPrice)));
			description.add(ChatColor.GREEN + Locale.cpclicktobuy);
		}
	    break;
	case SELL:
	case RENT:
	    meta.setDisplayName(name);
	    if (Districts.getPlugin().placeholderapi) {
			description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktoenteramount));
		} else {
			description.add(ChatColor.GREEN + Locale.cpclicktoenteramount);
		}
	    break;
	case CLAIM:
	    meta.setDisplayName(name);
	    if (Districts.getPlugin().placeholderapi) {
			description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktoenter));
		} else {
			description.add(ChatColor.GREEN + Locale.cpclicktoenter);
		}
	    break;
	case CANCEL:
	    meta.setDisplayName(name);
	    if (desc != null)
		description = desc;
	    if (Districts.getPlugin().placeholderapi) {
			description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktocancel));
		} else {
			description.add(ChatColor.GREEN + Locale.cpclicktocancel);
		}
	    break;	    
	case TEXT:
	    meta.setDisplayName(name);
	    if (Districts.getPlugin().placeholderapi) {
			description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktoenter));
		} else {
			description.add(ChatColor.GREEN + Locale.cpclicktoenter);
		}
	    break;
	case VISUALIZE:
	    meta.setDisplayName(name);
	    if (flagValue) {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpvisible));
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktotoggle));
			} else {
				description.add(ChatColor.GREEN + Locale.cpvisible);
				description.add(ChatColor.RED + Locale.cpclicktotoggle);
			}
	    } else {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpinvisible));
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktotoggle));
			} else {
				description.add(ChatColor.RED + Locale.cpinvisible);
				description.add(ChatColor.GREEN + Locale.cpclicktotoggle);
			}
	    }
	    break;

	case TOGGLE:
	    meta.setDisplayName(name.substring(5));
	    if (flagValue) {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpallowed));
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktotoggle));
			} else {
				description.add(ChatColor.GREEN + Locale.cpallowed);
				description.add(ChatColor.RED + Locale.cpclicktotoggle);
			}
	    } else {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpdisallowed));
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktotoggle));
			} else {
				description.add(ChatColor.RED + Locale.cpdisallowed);
				description.add(ChatColor.GREEN + Locale.cpclicktotoggle);
			}
	    }
	    break;
	default:
	    meta.setDisplayName(name);
	    if (desc != null)
		description = desc;
	    break;
	}
	meta.setLore(description);
	item.setItemMeta(meta);
    }

    public void setLore(List<String> lore) {
	ItemMeta meta = item.getItemMeta();
	meta.setLore(lore);
	item.setItemMeta(meta);
    }

    public ItemStack getItem() {
	return item;
    }

    /**
     * @return the slot
     */
    public int getSlot() {
	return slot;
    }

    /**
     * @return the flagValue
     */
    public boolean isFlagValue() {
		return flagValue;
    }

    /**
     * @param flagValue the flagValue to set
     */
    public void setFlagValue(boolean flagValue, Player p) {
	this.flagValue = flagValue;
	description.clear();
	ItemMeta meta = item.getItemMeta();
	switch (type) {
	case TOGGLE:
	    if (flagValue) {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpallowed));
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktotoggle));
			} else {
				description.add(ChatColor.GREEN + Locale.cpallowed);
				description.add(ChatColor.RED + Locale.cpclicktotoggle);
			}
	    } else {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpdisallowed));
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktotoggle));
			} else {
				description.add(ChatColor.RED + Locale.cpdisallowed);
				description.add(ChatColor.GREEN + Locale.cpclicktotoggle);
			}
	    }

	    break;
	case VISUALIZE:
	    if (flagValue) {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpvisible));
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktotoggle));
			} else {
				description.add(ChatColor.GREEN + Locale.cpvisible);
				description.add(ChatColor.RED + Locale.cpclicktotoggle);
			}
	    } else {
	    	if (Districts.getPlugin().placeholderapi) {
				description.add(ChatColor.RED + PlaceholderAPI.setPlaceholders(p, Locale.cpinvisible));
				description.add(ChatColor.GREEN + PlaceholderAPI.setPlaceholders(p, Locale.cpclicktotoggle));
			} else {
				description.add(ChatColor.RED + Locale.cpinvisible);
				description.add(ChatColor.GREEN + Locale.cpclicktotoggle);
			}
	    }
	    break;
	default:
	    break;

	}
	meta.setLore(description);
	item.setItemMeta(meta);
    }




    public String getName() {
	return name;
    }

    /**
     * @return the type
     */
    public Type getType() {
	return type;
    }

}