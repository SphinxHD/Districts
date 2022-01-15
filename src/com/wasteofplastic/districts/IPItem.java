package com.wasteofplastic.districts;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author ben
 * Info panel item
 */
public class IPItem {
    private ItemStack item;
    private List<String> description = new ArrayList<String>();
    private String name;
    private int slot;
    private boolean flagValue;
    private Type type;
    public enum Type {BUY, INFO, RENT, NAME, BUYBLOCKS};

    /**
     * //@param item
     * @param material
     * @param name
     * //@param b
     * //@param nextSection
     */
    public IPItem(Material material, int durability, String name, boolean flagValue, int slot, List<String> text, Type type) {
	this.flagValue = flagValue;
	this.slot = slot;
	this.name = name;
	this.type = type;
	description.clear();
	item = new ItemStack(material);
	item.setDurability((short)durability);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name);
	description.addAll(text);
	meta.setLore(description);
	item.setItemMeta(meta);
    }

    public IPItem(Material material, int durability, String name, boolean flagValue, int slot) {
	this.flagValue = flagValue;
	this.slot = slot;
	this.name = name;
	this.type = Type.INFO;
	description.clear();
	item = new ItemStack(material);
	item.setDurability((short)durability);
	ItemMeta meta = item.getItemMeta();
	meta.setDisplayName(name.substring(5));
	if (flagValue) {
	    description.add(ChatColor.GREEN + "Allowed by anyone");
	} else {
	    description.add(ChatColor.RED + "Disallowed for outsiders");	    
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
    public void setFlagValue(boolean flagValue) {
	this.flagValue = flagValue;
	description.clear();
	ItemMeta meta = item.getItemMeta();
	if (flagValue) {
	    description.add(ChatColor.GREEN + "Allowed by all");
	} else {
	    description.add(ChatColor.RED + "Disallowed for outsiders");	    
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