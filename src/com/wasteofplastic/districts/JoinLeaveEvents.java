package com.wasteofplastic.districts;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveEvents implements Listener {
    private Districts plugin;
    private PlayerCache players;

    public JoinLeaveEvents(Districts districts, PlayerCache onlinePlayers) {
	this.plugin = districts;
	this.players = onlinePlayers;
    }

    /**
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(final PlayerJoinEvent event) {
	final Player p = event.getPlayer();
	final UUID playerUUID = p.getUniqueId();
	players.addPlayer(playerUUID);
	// Set the player's name (it may have changed)
	players.setPlayerName(playerUUID, p.getName());
	players.save(playerUUID);
	Utils.logger(2,"Cached " + p.getName());
	// TODO: Check leases and expire any old ones.
	// Check to see if the player is in a district - one may have cropped up around them while they were logged off
	if (Settings.worldName.contains(p.getWorld().getName())) {
	    final DistrictRegion dr = plugin.getGrid(p.getLocation().getWorld().getName()).getDistrictRegionAt(p.getLocation());
	    if (dr != null) {
		if (plugin.players.getVisualize(p.getUniqueId())) {
		    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

			@Override
			public void run() {
			    //plugin.logger(2,"visualizing tick");
			    Visualization.visualize(dr, p);

			}},20L);
		}
	    }
	}
	// Check to see how many blocks they have
	int maxBlocks = plugin.getMaxBlockBalance(p);
	if (maxBlocks > 0 && Settings.maxBlockLimit) {
	    int actualBlocks = plugin.getBlocksInDistricts(p);
	    //int balance = 0;
	    if (actualBlocks > maxBlocks) {
		p.sendMessage(ChatColor.RED + "Your districts area is larger than your maximum allowed blocks ["+maxBlocks+"]! You will have to remove districts before claiming more blocks.");
		plugin.getLogger().warning(p.getName() + " has more blocks in their districts [" + actualBlocks + "] than they are allowed by permissions!");
	    }
	    /*
	    else {
		balance =  maxBlocks-actualBlocks;
	    }
	    plugin.players.setBlocks(playerUUID, balance);
	    
	    Utils.logger(2, p.getName() + " logged in and has " + actualBlocks + " blocks inside districts, is allowed " + maxBlocks + " blocks and has a balance of " + balance + " blocks.");
*/
	}
	// Load any messages for the player
	final List<String> messages = plugin.getMessages(playerUUID);
	if (!messages.isEmpty()) {
	    plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
		@Override
		public void run() {
		    event.getPlayer().sendMessage(ChatColor.AQUA + Locale.newsheadline);
		    int i = 1;
		    for (String message : messages) {
			event.getPlayer().sendMessage(i++ + ": " + message);
		    }
		}
	    }, 40L);
	}
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(final PlayerQuitEvent event) {
	// Remove any lingering positions
	plugin.getPos1s().remove(event.getPlayer().getUniqueId());
	//plugin.setMessage(event.getPlayer().getUniqueId(), "Hello! This is a test. You logged out");
	players.removeOnlinePlayer(event.getPlayer().getUniqueId());
    }
}