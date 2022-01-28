package com.wasteofplastic.districts;

import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;


/**
 * This plugin offers protection for areas of blocks (districts). Districts are rectangular
 * and created based on how many blocks a player had as a balance.
 * Players can claim areas using a golden hoe, with a command, or via a GUI
 * @author tastybento
 */
public class Districts extends JavaPlugin {
    // This plugin
    private static Districts plugin;
    // Player YAMLs
    public YamlConfiguration playerFile;
    public File playersFolder;
    // Localization Strings
    private FileConfiguration locale = null;
    private File localeFile = null;
    // Players object
    public PlayerCache players;
    // Districts
    private HashSet<DistrictRegion> districts = new HashSet<DistrictRegion>();
    // Offline Messages
    private HashMap<UUID, List<String>> messages = new HashMap<UUID, List<String>>();
    private YamlConfiguration messageStore;
    // A map of where pos1's are stored
    private HashMap<UUID,Location> pos1s = new HashMap<UUID,Location>();
    // Perm list
    List<permBlock> permList = new ArrayList<permBlock>();
    // Database of control panels for players
    HashMap<UUID, List<CPItem>> controlPanel = new HashMap<UUID, List<CPItem>>();
    HashMap<UUID, List<IPItem>> infoPanel = new HashMap<UUID, List<IPItem>>();
    // Grid manager
    private Map<String, GridManager> grid = new HashMap<String, GridManager>();
    // Whether WG is used
    private boolean worldGuard;

    /**
     * @return plugin object instance
     */
    public static Districts getPlugin() {
	return plugin;
    }




    /**
     * Loads the various settings from the config.yml file into the plugin
     */
    public void loadPluginConfig() {
	try {
	    getConfig();
	} catch (final Exception e) {
	    e.printStackTrace();
	}
	// Get the localization strings
	getLocale();
	/*
	Locale.adminHelpdelete = getLocale().getString("adminHelp.delete", "deletes the district you are standing in.");
	Locale.errorUnknownPlayer = getLocale().getString("error.unknownPlayer","That player is unknown.");
	Locale.errorNoPermission = getLocale().getString("error.noPermission", "You don't have permission to use that command!");
	Locale.errorCommandNotReady = getLocale().getString("error.commandNotReady", "You can't use that command right now.");
	Locale.errorOfflinePlayer = getLocale().getString("error.offlinePlayer", "That player is offline or doesn't exist.");
	Locale.errorUnknownCommand = getLocale().getString("error.unknownCommand","Unknown command.");
	Locale.districtProtected = getLocale().getString("error.districtProtected", "District protected");
	Locale.newsHeadline = getLocale().getString("news.headline", "[District News]");
	Locale.adminHelpreload = getLocale().getString("adminHelp.reload","reload configuration from file.");
	Locale.adminHelpdelete = getLocale().getString("adminHelp.delete","deletes the district you are standing in.");
	Locale.adminHelpinfo = getLocale().getString("adminHelp.info","display information for the given player.");
	Locale.reloadconfigReloaded = getLocale().getString("reload.configurationReloaded", "Configuration reloaded from file.");	//delete
	Locale.deleteremoving = getLocale().getString("delete.removing","District removed.");
	Locale.controlPanelTitle = getLocale().getString("general.controlpaneltitle", "District Control Panel");
	 */
	Locale.infoPanelTitle = getLocale().getString("general.infopaneltitle", "District Info");

	Locale.generalnotavailable = getLocale().getString("general.notavailable", "Districts are not available in this world");
	Locale.generaldistricts = getLocale().getString("general.districts", "Districts");
	Locale.generalowner = getLocale().getString("general.owner", "Owner");
	Locale.generalrenter = getLocale().getString("general.renter", "Renter");
	Locale.helphelp = getLocale().getString("help.help", "help");
	Locale.helpcreate = getLocale().getString("help.create", "Tries to make a district");
	Locale.helpremove = getLocale().getString("help.remove", "Removes a district that you are standing in if you are the owner");
	Locale.helpinfo = getLocale().getString("help.info", "Shows info on the district you and general info");
	Locale.helptrust = getLocale().getString("help.trust", "Gives player full access to your district");
	Locale.helpuntrust = getLocale().getString("help.untrust", "Revokes trust to your district");
	Locale.helpuntrustall = getLocale().getString("help.untrustall", "Removes all trusted parties from your district");
	Locale.helpbuy = getLocale().getString("help.buy", "Attempts to buy the district you are in");
	Locale.helprent = getLocale().getString("help.rent", "Attempts to rent the district you are in");
	Locale.helprentprice = getLocale().getString("help.rentprice", "Puts the district you are in up for rent for a weekly rent");
	Locale.helpsell = getLocale().getString("help.sell", "Puts the district you are in up for sale");
	Locale.helpcancel = getLocale().getString("help.cancel", "Cancels a For Sale, For Rent or a Lease");
	Locale.errorunknownPlayer = getLocale().getString("error.unknownPlayer", "That player is unknown.");
	Locale.errornoPermission = getLocale().getString("error.noPermission", "You don't have permission to use that command!");
	Locale.errorcommandNotReady = getLocale().getString("error.commandNotReady", "You can't use that command right now.");
	Locale.errorofflinePlayer = getLocale().getString("error.offlinePlayer", "That player is offline or doesn't exist.");
	Locale.errorunknownCommand = getLocale().getString("error.unknownCommand", "Unknown command.");
	Locale.errordistrictProtected = getLocale().getString("error.districtProtected", "District protected");
	Locale.errormove = getLocale().getString("error.move", "Move to a district you own or rent first.");
	Locale.errornotowner = getLocale().getString("error.notowner", "You must be the owner or renter of this district to do that.");
	Locale.errorremoving = getLocale().getString("error.removing", "Removing district!");
	Locale.errornotyours = getLocale().getString("error.notyours", "This is not your district!");
	Locale.errornotinside = getLocale().getString("error.notinside", "You are not in a district!");
	Locale.errortooexpensive = getLocale().getString("error.tooexpensive", "You cannot afford [price]" );
	Locale.erroralreadyexists = getLocale().getString("error.alreadyexists", "District already exists!");
	Locale.errornorecipe = getLocale().getString("error.norecipe", "This does not meet any district recipe!");
	Locale.errornoPVP = getLocale().getString("error.noPVP", "Target is in a no-PVP district!");
	Locale.trusttrust = getLocale().getString("trust.trust", "[player] trusts you in a district.");
	Locale.trustuntrust = getLocale().getString("trust.untrust", "[player] untrusted you in a district.");
	Locale.trusttitle = getLocale().getString("trust.title", "[District Trusted Players]");
	Locale.trustowners = getLocale().getString("trust.owners", "[Owner's]");
	Locale.trustrenters = getLocale().getString("trust.renters", "[Renter's]");
	Locale.trustnone = getLocale().getString("trust.none", "None");
	Locale.trustnotrust = getLocale().getString("trust.notrust", "No one is trusted in this district.");
	Locale.trustalreadytrusted = getLocale().getString("trust.alreadytrusted", "That player is already trusted.");
	Locale.sellnotforsale = getLocale().getString("sell.notforsale", "This district is not for sale!");
	Locale.sellyouareowner = getLocale().getString("sell.youareowner", "You already own this district!" );
	Locale.sellsold = getLocale().getString("sell.sold", "You successfully sold a district for [price] to [player]");
	Locale.sellbought = getLocale().getString("sell.bought", "You purchased the district for [price]!");
	Locale.sellecoproblem = getLocale().getString("sell.ecoproblem", "There was an economy problem trying to purchase the district for [price]!");
	Locale.sellbeingrented = getLocale().getString("sell.beingrented", "The district is being rented at this time. Wait until the lease expires.");
	Locale.sellinvalidprice = getLocale().getString("sell.invalidprice", "The price is invalid (must be >= [price])");
	Locale.sellforsale = getLocale().getString("sell.forsale", "Putting district up for sale for [price]");
	Locale.sellad = getLocale().getString("sell.ad", "This district is for sale for [price]!");
	Locale.rentnotforrent = getLocale().getString("rent.notforrent", "This district is not for rent!");
	Locale.rentalreadyrenting = getLocale().getString("rent.alreadyrenting", "You are already renting this district!");
	Locale.rentalreadyleased = getLocale().getString("rent.alreadyleased", "This district is already being leased.");
	Locale.renttip = getLocale().getString("rent.tip", "To end the renter's lease at the next due date, use the cancel command.");
	Locale.rentleased = getLocale().getString("rent.leased", "You successfully leased a district for [price] to [player]");
	Locale.rentrented = getLocale().getString("rent.rented", "You rented the district for [price] for 1 week!");
	Locale.renterror = getLocale().getString("rent.error", "There was an economy problem trying to rent the district for [price]!");
	Locale.rentinvalidrent = getLocale().getString("rent.invalidrent", "The rent is invalid (must be >= [price])");
	Locale.rentforrent = getLocale().getString("rent.forrent", "Putting district up for rent for [price]");
	Locale.rentad = getLocale().getString("rent.ad", "This district is for rent for [price] per week.");
	Locale.messagesenter = getLocale().getString("messages.enter", "Entering [owner]'s [biome] district!");
	Locale.messagesleave = getLocale().getString("messages.leave", "Now leaving [owner]'s district.");
	Locale.messagesrententer = getLocale().getString("messages.rententer", "Entering [player]'s rented [biome] district!");
	Locale.messagesrentfarewell = getLocale().getString("messages.rentfarewell", "Now leaving [player]'s rented district.");
	Locale.messagesyouarein = getLocale().getString("messages.youarein", "You are now in [owner]'s [biome] district!");
	Locale.cancelcancelled = getLocale().getString("cancel.cancelled", "District is no longer for sale or rent.");
	Locale.cancelleasestatus1 = getLocale().getString("cancel.leasestatus1", "District is currently leased by [player].");
	Locale.cancelleasestatus2 = getLocale().getString("cancel.leasestatus2", "Lease will not renew and will terminate in [time] days.");
	Locale.cancelleasestatus3 = getLocale().getString("cancel.leasestatus3", "You can put it up for rent again after that date.");
	Locale.cancelcancelmessage = getLocale().getString("cancel.cancelmessage", "[owner] ended a lease you have on a district. It will end in [time] days.");
	Locale.cancelleaserenewalcancelled = getLocale().getString("cancel.leaserenewalcancelled", "Lease renewal cancelled. Lease term finishes in [time] days.");
	Locale.cancelrenewalcancelmessage = getLocale().getString("cancel.renewalcancelmessage", "[renter] canceled a lease with you. It will end in [time] days.");

	Locale.infotitle = getLocale().getString("info.title", "&A[District Construction]");
	Locale.infoinfo = getLocale().getString("info.info", "[District Info]");
	Locale.infoownerstrusted = getLocale().getString("info.ownerstrusted", "[Owner's trusted players]");
	Locale.infonone = getLocale().getString("info.none", "None");
	Locale.infonextrent = getLocale().getString("info.nextrent", "Next rent of [price] due in [time] days.");
	Locale.infoleasewillend = getLocale().getString("info.leasewillend", "Lease will end in [time] days!");
	Locale.inforenter = getLocale().getString("info.renter", "Renter [nickname] ([name])");
	Locale.inforenterstrusted = getLocale().getString("info.renterstrusted", "[Renter's trusted players]");
	Locale.infoad = getLocale().getString("info.ad", "This district can be leased for [price]");
	Locale.infoMove = getLocale().getString("info.move","Move to a district to see its info");

	Locale.adminHelpreload = getLocale().getString("adminHelp.reload", "reload configuration from file.");
	Locale.adminHelpinfo = getLocale().getString("adminHelp.info", "provides info on the district you are in");
	Locale.reloadconfigReloaded = getLocale().getString("reload.configReloaded", "Configuration reloaded from file.");
	Locale.admininfoerror = getLocale().getString("admininfo.error", "District info only available in-game");
	Locale.admininfoerror2 = getLocale().getString("admininfo.error2", "Put yourself in a district to see info.");
	Locale.admininfoflags = getLocale().getString("admininfo.flags", "[District Flags]");
	Locale.newsheadline = getLocale().getString("news.headline", "[District News]");
	Locale.controlpaneltitle = getLocale().getString("controlpanel.title", "&ADistrict Control Panel");

	Locale.adminHelpbalance = getLocale().getString("adminHelp.balance", "show how many blocks player has.");
	Locale.adminHelpinfo = getLocale().getString("adminHelp.info", "display information for the given player.");
	Locale.adminHelpinfo2 = getLocale().getString("adminHelp.info2", "provides info on the district you are in.");
	Locale.adminHelpgive = getLocale().getString("adminHelp.give", "give player some blocks.");
	Locale.adminHelptake = getLocale().getString("adminHelp.take", "take blocks from player.");
	Locale.adminHelpset = getLocale().getString("adminHelp.set", "set blocks player has.");
	Locale.adminHelpevict = getLocale().getString("adminHelp.evict", "removes renter from this district.");
	Locale.errorInGameCommand = getLocale().getString("error.ingamecommands", "This command only available in-game");
	Locale.eventsrenterEvicted = getLocale().getString("events.renterevicted", "Renter evicted");
	// Lease
	Locale.leaserentpaid = getLocale().getString("lease.rentpaid", "You paid a rent of [price] to [owner].");
	Locale.leaserentpaidowner = getLocale().getString("lease.rentpaidowner",  "[player] paid you a rent of [price].");
	Locale.leasecannotpay = getLocale().getString("lease.cannot pay",  "You could not pay a rent of [price] so you were evicted from [owner]'s district");
	Locale.leasecannotpayowner = getLocale().getString("lease.cannotpayowner",  "[player] could not pay rent of [price] so was evicted");
	Locale.leaseleaseended = getLocale().getString("lease.leaseended",  "The lease on a district you were renting from [owner] ended.");
	Locale.leaseleaseendedowner = getLocale().getString("lease.leaseendedowner",  "[player]'s lease ended.");


	// Conversations
	Locale.conversationsenterrent = getLocale().getString("conversations.enterrent","Enter the rent amount");
	Locale.conversationsenterprice = getLocale().getString("conversations.enterprice","Enter the district price");
	Locale.conversationsenteramount = getLocale().getString("conversations.enteramount","Enter the amount");
	Locale.conversationsmustbemore = getLocale().getString("conversations.mustbemore","Amount must be more than [price]");
	Locale.conversationshowmuch = getLocale().getString("conversations.howmuch","How much?");
	Locale.conversationsenterblocknum = getLocale().getString("conversations.enterblocknum", "Enter the number of blocks to buy ([price] each)");
	Locale.conversationsenterradius = getLocale().getString("conversations.enterradius","Enter the radius to claim");
	Locale.conversationsenterblocks = getLocale().getString("conversations.enterblocks","Enter the number of blocks");
	Locale.conversationsended = getLocale().getString("conversations.ended","Ended");
	Locale.conversationsmove = getLocale().getString("conversations.move","Move out of a district to claim an area");
	Locale.conversationshowmany = getLocale().getString("conversations.howmany","How many?");
	Locale.conversationsyoubought = getLocale().getString("conversations.youbought","You bought [number] blocks for [cost]");
	Locale.conversationsblockscost = getLocale().getString("conversations.blockscost","Blocks cost [cost]");
	Locale.conversationsyouhave = getLocale().getString("conversations.youhave","You have [balance]");
	Locale.notenoughblocks = getLocale().getString("conversations.notenoughblocks","You do not have enough blocks!");
	Locale.blocksavailable = getLocale().getString("conversations.blocksavailable","Blocks available: [number]");
	Locale.conversationsblocksrequired = getLocale().getString("conversations.blocksrequired","Blocks required: [number]");
	Locale.conversationsminimumradius = getLocale().getString("conversations.minimumradius", "The minimum radius is 2 blocks");
	Locale.conversationsdistrictcreated= getLocale().getString("conversations.districtcreated",  "District created!");
	Locale.conversationsyounowhave= getLocale().getString("conversations.younowhave",  "You now have [number] blocks left.");
	Locale.conversationsoverlap= getLocale().getString("conversations.overlap",  "That size would overlaps another district");
	Locale.conversationsentername= getLocale().getString("conversations.entername",  "Enter the name of this district, 'none' or 'default' to use the default");
	Locale.conversationssettingownerdefault= getLocale().getString("conversations.settingownerdefault",  "Setting to the default owner's message");
	Locale.conversationssettingrenterdefault= getLocale().getString("conversations.settingrenterdefault",  "Setting to the default renter's message");
	Locale.conversationsnomessage= getLocale().getString("conversations.nomessage",  "No message will be shown when entering or leaving.");
	Locale.conversationssettingto= getLocale().getString("conversations.settingto",  "Setting to: [name]");
	Locale.conversationsentermessage= getLocale().getString("conversations.entermessage",  "&6Entering '&f[name]&6'");
	Locale.conversationsleavingmessage= getLocale().getString("conversations.leavingmessage", "&6Leaving '&f[name]&6'");


	// Control panel
	Locale.cpallowed = getLocale().getString("cp.allowed","Allowed by anyone");
	Locale.cpdisallowed = getLocale().getString("cp.disallowed","Disallowed for outsiders");
	Locale.cpclicktobuy = getLocale().getString("cp.clicktobuy","Click to buy");
	Locale.cpclicktotoggle = getLocale().getString("cp.clicktotoggle", "Click to change");
	Locale.cpclicktoenter = getLocale().getString("cp.clicktoenter","Click to enter text");
	Locale.cpclicktoenteramount = getLocale().getString("cp.clicktoenteramount","Click to enter amount");
	Locale.cpclicktocancel = getLocale().getString("cp.cancel","Click to cancel");
	Locale.cpvisible = getLocale().getString("cp.visible","Visible");
	Locale.cpinvisible = getLocale().getString("cp.invisible","Invisible");

	// Assign settings
	Settings.allowPvP = getConfig().getBoolean("districts.allowPvP",false);
	Settings.allowBreakBlocks = getConfig().getBoolean("districts.allowbreakblocks", false);
	Settings.allowPlaceBlocks= getConfig().getBoolean("districts.allowplaceblocks", false);
	Settings.allowBedUse= getConfig().getBoolean("districts.allowbeduse", false);
	Settings.allowBucketUse = getConfig().getBoolean("districts.allowbucketuse", false);
	Settings.allowShearing = getConfig().getBoolean("districts.allowshearing", false);
	Settings.allowEnderPearls = getConfig().getBoolean("districts.allowenderpearls", false);
	Settings.allowDoorUse = getConfig().getBoolean("districts.allowdooruse", false);
	Settings.allowLeverButtonUse = getConfig().getBoolean("districts.allowleverbuttonuse", false);
	Settings.allowCropTrample = getConfig().getBoolean("districts.allowcroptrample", false);
	Settings.allowChestAccess = getConfig().getBoolean("districts.allowchestaccess", false);
	Settings.allowFurnaceUse = getConfig().getBoolean("districts.allowfurnaceuse", false);
	Settings.allowRedStone = getConfig().getBoolean("districts.allowredstone", false);
	Settings.allowMusic = getConfig().getBoolean("districts.allowmusic", false);
	Settings.allowCrafting = getConfig().getBoolean("districts.allowcrafting", false);
	Settings.allowBrewing = getConfig().getBoolean("districts.allowbrewing", false);
	Settings.allowGateUse = getConfig().getBoolean("districts.allowgateuse", false);
	Settings.allowMobHarm = getConfig().getBoolean("districts.allowmobharm", false);
	Settings.allowFlowIn = getConfig().getBoolean("districts.allowflowin", false);
	Settings.allowFlowOut = getConfig().getBoolean("districts.allowflowout", false);
	// Other settings
	Settings.worldName = getConfig().getStringList("districts.worldName");
	Settings.beginningBlocks = getConfig().getInt("districts.beginningblocks",25);
	Settings.actionbarProtection = getConfig().getBoolean("districts.actionbar.protection", true);
	Settings.actionbarDistmessage = getConfig().getBoolean("districts.actionbar.distmessage", true);
	if (Settings.beginningBlocks < 0) {
	    Settings.beginningBlocks = 0;
	    getLogger().warning("Beginning Blocks in config.yml was set to a negative value!");
	}
	Settings.blockTick = getConfig().getInt("districts.blocktick",0);
	if (Settings.blockTick < 0) {
	    Settings.blockTick = 0;
	    getLogger().warning("blocktick in config.yml was set to a negative value! Setting to 0. No blocks given out.");



	}

	Settings.maxBlockLimit = getConfig().getBoolean("districts.maxblocklimit",false);
	Settings.checkLeases = getConfig().getInt("districts.checkleases",12);
	if (Settings.checkLeases < 0) {
	    Settings.checkLeases = 0;
	    getLogger().warning("Checkleases in config.yml was set to a negative value! Setting to 0. No lease checking.");	    
	} else if (Settings.checkLeases > 24) {
	    Settings.checkLeases = 24;
	    getLogger().warning("Maximum value for Checkleases in config.yml is 24 hours. Setting to 24.");	    
	}
	String vizMat = getConfig().getString("districts.visualblock", "REDSTONE_BLOCK");
	Material m = Material.REDSTONE_BLOCK;
	try {
	    m = Material.valueOf(vizMat);
	    if (!m.isSolid()) {
		m = Material.REDSTONE_BLOCK;
	    }
	} catch (Exception e) {
	    getLogger().severe("Visualization block material (districts.visualblock) is invalid in config.yml! Defaulting to REDSTONE_BLOCK."); 

	}
	Settings.visualization = m;
	Settings.vizRange = getConfig().getInt("districts.visualrange",20);
	if (Settings.vizRange < 10) {
	    Settings.vizRange = 10;
	}

	Settings.blockPrice = getConfig().getDouble("districts.blockprice", 0D);
	if (Settings.blockPrice < 0D) {
	    Settings.blockPrice = 0D;
	    getLogger().warning("config.yml issue: blockprice cannot be negative, setting to 0 (disabled).");
	}
	Utils.setDebug(getConfig().getInt("districts.debug",1));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
	try {
	    // Remove players from memory
	    players.removeAllPlayers();
	    //saveConfig();
	    saveMessages();
	} catch (final Exception e) {
	    plugin.getLogger().severe("Something went wrong saving files!");
	    e.printStackTrace();
	}
    }

    public boolean placeholderapi = false;

    /*
     * (non-Javadoc)
     * 
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
	// instance of this plugin
	plugin = this;
	saveDefaultConfig();
	saveDefaultLocale();
	// Metrics via bStats won't function at the moment
	Metrics metrics = new Metrics(this, 13965);
	if (!VaultHelper.setupEconomy()) {
	    getLogger().severe("Could not set up economy - will run without one.");
	}
	if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
		getLogger().severe("Cloud not find PlaceholderAPI - running districts without it.");
		placeholderapi = false;
	} else {
		getLogger().info("Found PlaceholderAPI - Activating PlacehodlerAPI Support.");
		placeholderapi = true;
	}
	loadPluginConfig();
	// Set and make the player's directory if it does not exist and then load players into memory
	playersFolder = new File(getDataFolder() + File.separator + "players");
	if (!playersFolder.exists()) {
	    playersFolder.mkdir();
	}
	// Load grid
	for (String worldName: Settings.worldName) {
	    grid.put(worldName,new GridManager(this));
	}
	// Load players
	players = new PlayerCache(this);
	// Set up commands for this plugin
	getCommand("district").setExecutor(new DistrictCmd(this,players));
	getCommand("dadmin").setExecutor(new AdminCmd(this,players));
	// Register events that this plugin uses
	registerEvents();
	// Load messages
	loadMessages();

	// Get the block donation section
	ConfigurationSection blockgroups = getConfig().getConfigurationSection("districts.blockgroups");
	if (blockgroups != null) {
	    permList.clear();
	    //getLogger().info("DEBUG: Loading blockgroups");
	    //getLogger().info("DEBUG: There are " + blockgroups.getKeys(true).size());
	    for (String perm : blockgroups.getKeys(true)) {
		//getLogger().info("DEBUG: " + perm);
		try {
		    // format is permission: <number of blocks>:<duration>:<max>
		    String settings[] = blockgroups.getString(perm).split(":");
		    if (settings.length == 2) {
			permBlock p = new permBlock();
			p.name = perm;
			p.numberOfBlocks = Integer.valueOf(settings[0]);
			p.max = Integer.valueOf(settings[1]);
			permList.add(p);
			Utils.logger(2,"Loading block permission " + p.name + " " + p.numberOfBlocks + ":" + p.max);
		    }
		} catch (Exception e) {
		    getLogger().severe("Error in config.yml in district.blockgroups section - check it");
		    getLogger().severe("Skipping " + perm);
		}
	    }	    
	} else {
	    getLogger().severe("Error in config.yml in district.blockgroups section - does not exist"); 
	}



	// Kick off a few tasks on the next tick
	getServer().getScheduler().runTask(plugin, new Runnable() {
	    @Override
	    public void run() {
		final PluginManager manager = Bukkit.getServer().getPluginManager();
		if (manager.isPluginEnabled("Vault")) {
		    Utils.logger(1,"Trying to use Vault for permissions...");
		    if (!VaultHelper.setupPermissions()) {
			getLogger().severe("Cannot link with Vault for permissions! Disabling plugin!");
			manager.disablePlugin(Districts.getPlugin());
		    } else {
			Utils.logger(1,"Success!");
		    };
		}
		// Load players and check leases
		loadDistricts();
		// Kick off give blocks
		long dur = Settings.blockTick * 60 * 20; // Minutes
		if (dur > 0) {
		    Utils.logger(1,"Block tick timer started. Will give out blocks every " + Settings.blockTick + " minutes.");
		    getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
			@Override
			public void run() {
			    //Utils.logger(1,"Giving out blocks. Will repeat in " + Settings.blockTick + " mins.");
			    giveBlocks();
			}
		    }, dur, dur);

		} else {
		    getLogger().warning("Blocks will not be given out automatically. Set blocktick to non-zero to change.");
		}


		// Kick off the check leases 
		long duration = Settings.checkLeases * 60 * 60 * 20; // Server ticks
		if (duration > 0) {
		    Utils.logger(1,"Check lease timer started. Will check leases every " + Settings.checkLeases + " hours.");
		    getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
			@Override
			public void run() {
			    Utils.logger(1,"Checking leases. Will check leases again in " + Settings.checkLeases + " hours.");
			    checkLeases();
			}
		    }, 20L, duration);

		} else {
		    checkLeases();
		    getLogger().warning("Leases will not be checked automatically. Make sure your server restarts regularly.");
		}
	    }
	});

    }

    /**
     * Gives out blocks to every registered player depending on their permission level
     */
    public static class permBlock {
	public String name;
	public int numberOfBlocks, max;
    }

    /**
     * Gives blocks to players online up to their maximum allowed balance
     */
    protected void giveBlocks() {
	// Run through all the players and give blocks as appropriate
	for (Player p : getServer().getOnlinePlayers()) {
	    Utils.logger(2,"Trying to give blocks to online player " + p.getName());
	    int maxBlocks = getMaxBlockBalance(p);
	    int blocksOwned = 0;
	    // Count the blocks a player has?
	    if (Settings.maxBlockLimit) {
		blocksOwned = getBlocksInDistricts(p);
	    }
	    int blockBalance = players.getBlockBalance(p.getUniqueId());
	    int bestAdd = 0;
	    if (blocksOwned > maxBlocks) {
		// THis player already has too many blocks. Just set the balance to zero just in case
		Utils.logger(2,"Player too many owned blocks for permission. Set their block balance to zero.");
		blockBalance = 0;
	    } else if ((blocksOwned + blockBalance) > maxBlocks) {
		// Just reduce the blockBalance by the difference
		blockBalance = maxBlocks - blocksOwned;
		Utils.logger(2,"Player has too many owned and balance blocks for permission. New balance = " + blockBalance);
	    } else {
		// Give some blocks to the player
		// Check perms
		for (permBlock pb : permList) {
		    Utils.logger(2,"Checking " + pb.name + " " + pb.numberOfBlocks + ":" + pb.max);
		    if (VaultHelper.checkPerm(p, pb.name)) {
			Utils.logger(2,"Player has perm");
			// Check if they have the max already, if not give them more  
			Utils.logger(2,"Balance = " + blockBalance);
			if ((blockBalance+pb.numberOfBlocks+blocksOwned) <= pb.max && pb.numberOfBlocks > bestAdd) {
			    Utils.logger(2,"Giving blocks!");
			    bestAdd = pb.numberOfBlocks;
			} else if ((pb.max - (blockBalance+blocksOwned)) > bestAdd) {
			    Utils.logger(2,"Maxed out!");
			    bestAdd = pb.max - (blockBalance+blocksOwned);
			}
		    }
		}
	    }
	    Utils.logger(2,"Adding " + bestAdd);
	    players.setBlocks(p.getUniqueId(), bestAdd + blockBalance);
	}
    }


    public int daysToEndOfLease(DistrictRegion d) {
	// Basic checking
	if (d.getLastPayment() == null) {
	    return 0;
	}
	if (d.getRenter() == null) {
	    return 0;
	}
	// Check the lease date
	Calendar lastWeek = Calendar.getInstance();	
	lastWeek.add(Calendar.DAY_OF_MONTH, -7);
	// Only work in days
	lastWeek.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
	lastWeek.set(Calendar.MINUTE, 0);                 // set minute in hour
	lastWeek.set(Calendar.SECOND, 0);                 // set second in minute
	lastWeek.set(Calendar.MILLISECOND, 0);            // set millisecond in second	

	Calendar lease = Calendar.getInstance();
	lease.setTime(d.getLastPayment());
	lease.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
	lease.set(Calendar.MINUTE, 0);                 // set minute in hour
	lease.set(Calendar.SECOND, 0);                 // set second in minute
	lease.set(Calendar.MILLISECOND, 0);            // set millisecond in second

	Utils.logger(2,"DEBUG: Last week = " + lastWeek.getTime().toString());
	Utils.logger(2,"DEBUG: Last payment = " + lease.getTime().toString());
	int daysBetween = 0;
	while (lastWeek.before(lease)) {
	    lastWeek.add(Calendar.DAY_OF_MONTH, 1);
	    daysBetween++;
	}
	Utils.logger(2,"DEBUG: days left on lease = " + daysBetween);
	if (daysBetween < 1) {
	    Utils.logger(2,"Lease expired");
	    return 0;
	}
	return daysBetween;
    }

    protected void checkLeases() {
	if (!VaultHelper.setupEconomy()) {
	    return;
	}
	Utils.logger(2,"Debug: checking leases");
	// Check all the leases
	for (DistrictRegion d:districts) {
	    //Utils.logger(2,"Debug: Checking district owned by " + players.getName(d.getOwner()) + " Days left = " + daysToEndOfLease(d));
	    // Only check rented properties
	    if (d.getLastPayment() != null && d.getRenter() != null) {
		Utils.logger(2,"Debug: Rental property found. Days left = " + daysToEndOfLease(d));
		if (daysToEndOfLease(d) == 0) {
		    Utils.logger(2,"Debug: Check to see if the lease is renewable");
		    // Check to see if the lease is renewable
		    if (d.isForRent()) {
			Utils.logger(2,"Debug: District is still for rent");
			// Try to deduct rent
			Utils.logger(2,"Debug: Withdrawing rent from renters account");
			if (getServer().getOfflinePlayer(d.getRenter()).hasPlayedBefore()) {
			    EconomyResponse r = VaultHelper.econ.withdrawPlayer(getServer().getOfflinePlayer(d.getRenter()), d.getPrice());
			    if (r.transactionSuccess()) {
				Utils.logger(2,"Successfully withdrew rent of " + VaultHelper.econ.format(d.getPrice()) + " from " + getServer().getOfflinePlayer(d.getRenter()).getName() + " account.");
				Calendar currentDate = Calendar.getInstance();
				// Only work in days
				currentDate.set(Calendar.HOUR_OF_DAY, 0);            // set hour to midnight
				currentDate.set(Calendar.MINUTE, 0);                 // set minute in hour
				currentDate.set(Calendar.SECOND, 0);                 // set second in minute
				currentDate.set(Calendar.MILLISECOND, 0);            // set millisecond in second
				d.setLastPayment(currentDate.getTime());

				if (getServer().getPlayer(d.getRenter()) != null) {
				    getServer().getPlayer(d.getRenter()).sendMessage("You paid a rent of " + VaultHelper.econ.format(d.getPrice()) + " to " + getServer().getOfflinePlayer(d.getOwner()).getName() );
				} else {
				    plugin.setMessage(d.getRenter(), "You paid a rent of " + VaultHelper.econ.format(d.getPrice()) + " to " + getServer().getOfflinePlayer(d.getOwner()).getName());
				}
				if (getServer().getPlayer(d.getOwner()) != null) {
				    getServer().getPlayer(d.getOwner()).sendMessage(getServer().getOfflinePlayer(d.getRenter()).getName() + " paid you a rent of " + VaultHelper.econ.format(d.getPrice()));
				} else {
				    plugin.setMessage(d.getOwner(), getServer().getOfflinePlayer(d.getRenter()).getName() + " paid you a rent of " + VaultHelper.econ.format(d.getPrice()));
				}
			    } else {
				// evict!
				Utils.logger(2,"Could not withdraw rent of " + VaultHelper.econ.format(d.getPrice()) + " from " + getServer().getOfflinePlayer(d.getRenter()).getName() + " account.");

				if (getServer().getPlayer(d.getRenter()) != null) {
				    getServer().getPlayer(d.getRenter()).sendMessage("You could not pay a rent of " + VaultHelper.econ.format(d.getPrice()) + " so you were evicted from " + getServer().getOfflinePlayer(d.getOwner()).getName() + "'s district!");
				} else {
				    plugin.setMessage(d.getRenter(),"You could not pay a rent of " + VaultHelper.econ.format(d.getPrice()) + " so you were evicted from " + getServer().getOfflinePlayer(d.getOwner()).getName() + "'s district!");
				}
				if (getServer().getPlayer(d.getOwner()) != null) {
				    getServer().getPlayer(d.getOwner()).sendMessage(getServer().getOfflinePlayer(d.getRenter()).getName() + " could not pay you a rent of " + VaultHelper.econ.format(d.getPrice()) + " so they were evicted from a propery!");
				} else {
				    plugin.setMessage(d.getOwner(), getServer().getOfflinePlayer(d.getRenter()).getName() + " could not pay you a rent of " + VaultHelper.econ.format(d.getPrice()) + " so they were evicted from a propery!");			
				}
				d.setRenter(null);
				d.setRenterTrusted(new ArrayList<UUID>());
				d.setEnterMessage("Entering " + players.getName(d.getOwner()) + "'s district!");
				d.setFarewellMessage("Now leaving " + players.getName(d.getOwner()) + "'s district.");
			    }
			} else {
			    getLogger().severe("Renter does not exist on this server! " + d.getRenter());
			}
		    } else {
			String rentersName = "[No name]";
			if (!getServer().getOfflinePlayer(d.getRenter()).hasPlayedBefore()) {
			    getLogger().severe("Renter does not exist on this server! " + d.getRenter());
			} else {
			    rentersName = getServer().getOfflinePlayer(d.getRenter()).getName();
			}
			// No longer for rent
			Utils.logger(2,"District is no longer for rent - evicting " + rentersName);

			// evict!
			if (getServer().getPlayer(d.getRenter()) != null) {
			    getServer().getPlayer(d.getRenter()).sendMessage("The lease on a district you were renting from " + players.getName(d.getOwner()) + " ended.");
			} else {
			    plugin.setMessage(d.getRenter(),"The lease on a district you were renting from " + players.getName(d.getOwner()) + " ended.");
			}
			if (getServer().getPlayer(d.getOwner()) != null) {
			    getServer().getPlayer(d.getOwner()).sendMessage(rentersName + "'s lease ended.");
			} else {
			    plugin.setMessage(d.getOwner(), rentersName + "'s lease ended.");			
			}
			d.setRenter(null);
			d.setRenterTrusted(new ArrayList<UUID>());
			d.setEnterMessage("Entering " + players.getName(d.getOwner()) + "'s district!");
			d.setFarewellMessage("Now leaving " + players.getName(d.getOwner()) + "'s district.");	
		    }
		}
	    }
	}	
    }


    protected void loadDistricts() {
	// Load all known districts
	districts.clear();
	// Load all the players
	for (final File f : playersFolder.listFiles()) {
	    // Need to remove the .yml suffix
	    String fileName = f.getName();
	    if (fileName.endsWith(".yml")) {
		try {
		    final UUID playerUUID = UUID.fromString(fileName.substring(0, fileName.length() - 4));
		    if (playerUUID == null) {
			getLogger().warning("Player file contains erroneous UUID data.");
			Utils.logger(2,"Looking at " + fileName.substring(0, fileName.length() - 4));
		    }
		    new Players(this, playerUUID);    
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}
	Utils.logger(1,"Loaded " + districts.size() + " districts.");
	// Put all online players in districts
	for (Player p : getServer().getOnlinePlayers()) {
	    for (DistrictRegion d: districts) {
		if (d.intersectsDistrict(p.getLocation())) {
		    players.setInDistrict(p.getUniqueId(), d);
		    break;
		}
	    }
	}

    }


    /**
     * Registers events
     */
    public void registerEvents() {
	final PluginManager manager = getServer().getPluginManager();
	// Nether portal events
	// Island Protection events
	manager.registerEvents(new DistrictGuard(this), this);
	// New V1.8 events
	Class<?> clazz;
	try {
	    clazz = Class.forName("org.bukkit.event.player.PlayerInteractAtEntityEvent");
	} catch (Exception e) {
	    getLogger().info("No PlayerInteractAtEntityEvent found.");
	    clazz = null;
	}
	if (clazz != null) {
	    manager.registerEvents(new DistrictGuardNew(this), this);
	}
	// Events for when a player joins or leaves the server
	manager.registerEvents(new JoinLeaveEvents(this, players), this);
	// WorldGuard PVP
	if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
	    worldGuard = true;
	    manager.registerEvents(new WorldGuardPVPListener(this), this);
	}
    }


    // Localization
    /**
     * Saves the locale.yml file if it does not exist
     */
    public void saveDefaultLocale() {
	if (localeFile == null) {
	    localeFile = new File(getDataFolder(), "locale.yml");
	}
	if (!localeFile.exists()) {            
	    plugin.saveResource("locale.yml", false);
	}
    }

    /**
     * Reloads the locale file
     */
    public void reloadLocale() {
	if (localeFile == null) {
	    localeFile = new File(getDataFolder(), "locale.yml");
	}
	locale = YamlConfiguration.loadConfiguration(localeFile);

	// Look for defaults in the jar
	InputStream defLocaleStream = this.getResource("locale.yml");
	if (defLocaleStream != null) {
	    Reader defLocaleReader = new InputStreamReader(defLocaleStream);
	    YamlConfiguration defLocale = YamlConfiguration.loadConfiguration(defLocaleReader);
	    locale.setDefaults(defLocale);
	}
    }

    /**
     * @return locale FileConfiguration object
     */
    public FileConfiguration getLocale() {
	if (locale == null) {
	    reloadLocale();
	}
	return locale;
    }

    public void saveLocale() {
	if (locale == null || localeFile == null) {
	    return;
	}
	try {
	    getLocale().save(localeFile);
	} catch (IOException ex) {
	    getLogger().severe("Could not save config to " + localeFile);
	}
    }

    /**
     * Sets a message for the player to receive next time they login
     * @param playerUUID
     * @param message
     * @return true if player is offline, false if online
     */
    public boolean setMessage(UUID playerUUID, String message) {
	//Utils.logger(2,"DEBUG: received message - " + message);
	Player player = getServer().getPlayer(playerUUID);
	// Check if player is online
	if (player != null) {
	    if (player.isOnline()) {
		//player.sendMessage(message);
		return false;
	    }
	}
	// Player is offline so store the message

	List<String> playerMessages = messages.get(playerUUID);
	if (playerMessages != null) {
	    playerMessages.add(message);
	} else {
	    playerMessages = new ArrayList<String>(Arrays.asList(message));
	}
	messages.put(playerUUID, playerMessages);
	return true;
    }

    public List<String> getMessages(UUID playerUUID) {
	List<String> playerMessages = messages.get(playerUUID);
	if (playerMessages != null) {
	    // Remove the messages
	    messages.remove(playerUUID);
	} else {
	    // No messages
	    playerMessages = new ArrayList<String>();
	}
	return playerMessages;
    }

    public boolean saveMessages() {
	Utils.logger(2,"Saving offline messages...");
	try {
	    // Convert to a serialized string
	    final HashMap<String,Object> offlineMessages = new HashMap<String,Object>();
	    for (UUID p : messages.keySet()) {
		offlineMessages.put(p.toString(),messages.get(p));
	    }
	    // Convert to YAML
	    messageStore.set("messages", offlineMessages);
	    Utils.saveYamlFile(messageStore, "messages.yml");
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public boolean loadMessages() {
	Utils.logger(1,"Loading offline messages...");
	try {
	    messageStore = Utils.loadYamlFile("messages.yml");
	    if (messageStore.getConfigurationSection("messages") == null) {
		messageStore.createSection("messages"); // This is only used to create
	    }
	    HashMap<String,Object> temp = (HashMap<String, Object>) messageStore.getConfigurationSection("messages").getValues(true);
	    for (String s : temp.keySet()) {
		List<String> messageList = messageStore.getStringList("messages." + s);
		if (!messageList.isEmpty()) {
		    messages.put(UUID.fromString(s), messageList);
		}
	    }
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }


    /**
     * @return the pos1s
     */
    public HashMap<UUID, Location> getPos1s() {
	return pos1s;
    }


    /**
     * @param pos1s the pos1s to set
     */
    public void setPos1s(HashMap<UUID, Location> pos1s) {
	this.pos1s = pos1s;
    }


    /**
     * @return the districts
     */
    public HashSet<DistrictRegion> getDistricts() {
	return districts;
    }


    /**
     * @param districts the districts to set
     */
    public void setDistricts(HashSet<DistrictRegion> districts) {
	this.districts = districts;
    }

	public static WorldGuardPlatform wg;

	private static BlockVector3 makeBlockVector(Location location) {
		return BlockVector3.at(location.getX(), location.getY(), location.getZ());
	}

    /**
     * Checks if a district defined by the corner points pos1 and pos2 overlaps any known districts
     * @param pos1
     * @param pos2
     * @return true if it intersects
     */
    public boolean checkDistrictIntersection(Location pos1, Location pos2) {
	//getLogger().info("DEBUG: checking district intersection " + pos1 + " " + pos2);
	if (worldGuard) {
	    //getLogger().info("DEBUG:worldguard is true");
	    // Check if the district overlaps a worldguard region
		wg = WorldGuard.getInstance().getPlatform();
		RegionContainer container = wg.getRegionContainer();
	    RegionManager regions = container.get(new BukkitWorld(pos1.getWorld()));
	    Location one = new Location(pos1.getWorld(), pos1.getBlockX(), pos1.getWorld().getMinHeight(), pos1.getBlockZ());
	    Location two = new Location(pos1.getWorld(), pos2.getBlockX(), pos2.getWorld().getMaxHeight(), pos2.getBlockZ());
	    BlockVector3 min;
	    BlockVector3 max;
	    min = makeBlockVector(one);
	    max = makeBlockVector(two);
	    ProtectedRegion test = new ProtectedCuboidRegion("dummy", min, max);
	    /*
	    Collection<ProtectedRegion> c = regions.getRegions().values();
	    getLogger().info("C = " + c);
	    List<ProtectedRegion> intersecting = test.getIntersectingRegions(c);*/
	    ApplicableRegionSet intersecting = regions.getApplicableRegions(test);
	    if (!intersecting.getRegions().isEmpty()) {
		// District will overlap a worldguard region
		Utils.logger(2, "District overlaps WG region");
		return true;
	    }
	    //getLogger().info("DEBUG: Regions are empty");
	}
	// Create a 2D rectangle of this
	Rectangle2D.Double rect = new Rectangle2D.Double();
	rect.setFrameFromDiagonal(pos1.getX(), pos1.getZ(), pos2.getX(), pos2.getZ());
	Rectangle2D.Double testRect = new Rectangle2D.Double();
	// Create a set of rectangles of current districts
	for (DistrictRegion d: districts) {
	    if (d.getPos1().getWorld().equals(pos1.getWorld())) {
		testRect.setFrameFromDiagonal(d.getPos1().getX(), d.getPos1().getZ(),d.getPos2().getX(),d.getPos2().getZ());
		if (rect.intersects(testRect)) {
		    return true;
		}
	    }
	}
	return false;
    }

    /**
     * @return the worldGuard
     */
    private WorldGuardPlugin getWorldGuard() {
	Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

	// WorldGuard may not be loaded
	if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	    return null; // Maybe you want throw an exception instead
	}

	return (WorldGuardPlugin) plugin;
    }



    /**
     * Creates a new district
     * @param pos1
     * @param pos2
     * @param owner
     * @return the district region
     */
    public DistrictRegion createNewDistrict(Location pos1, Location pos2, Player owner) {
	DistrictRegion d = new DistrictRegion(plugin, pos1, pos2, owner.getUniqueId());
	d.setEnterMessage("Entering " + owner.getDisplayName() + "'s district!");
	d.setFarewellMessage("Now leaving " + owner.getDisplayName() + "'s district.");
	getDistricts().add(d);
	getPos1s().remove(owner.getUniqueId());
	players.save(owner.getUniqueId());
	// Find everyone who is in this district and visualize them
	for (Player p : getServer().getOnlinePlayers()) {
	    if (d.intersectsDistrict(p.getLocation())) {
		if (!p.equals(owner)) {
		    p.sendMessage("You are now in " + owner.getDisplayName() + "'s district!");
		}
		players.setInDistrict(p.getUniqueId(), d);
		Visualization.visualize(d,p);
	    }
	}
	// Add to grid
	grid.get(pos1.getWorld().getName()).addToGrid(d);
	return d;
    }



    public DistrictRegion getInDistrict(Location location) {
	return grid.get(location.getWorld().getName()).getDistrictRegionAt(location);
    }

    public Location getClosestDistrict(Player player) {
	// Find closest district
	Location closest = null;
	Double distance = 0D;
	for (DistrictRegion d : districts) {
	    UUID owner = d.getOwner();
	    UUID renter = d.getRenter();

	    if ((owner !=null && owner.equals(player.getUniqueId())) || (renter !=null && renter.equals(player.getUniqueId()))) {
		//plugin.Utils.logger(2,owner + "  -  " + renter);
		if (closest == null) {
		    //plugin.Utils.logger(2,owner + "  -  " + renter);
		    Vector mid = d.getPos1().toVector().midpoint(d.getPos2().toVector());
		    closest = mid.toLocation(d.getPos1().getWorld());
		    distance = player.getLocation().distanceSquared(closest);
		    //Utils.logger(1,"DEBUG: first district found at " + d.getPos1().toString() + " distance " + distance);
		} else {
		    // Find out if this location is closer to player
		    Double newDist = player.getLocation().distanceSquared(d.getPos1());
		    if (newDist < distance) {
			Vector mid = d.getPos1().toVector().midpoint(d.getPos2().toVector());
			closest = mid.toLocation(d.getPos1().getWorld());
			distance = player.getLocation().distanceSquared(closest);
			//Utils.logger(1,"DEBUG: closer district found at " + d.getPos1().toString() + " distance " + distance);
		    }
		}
	    }
	}
	//Utils.logger(1,"DEBUG: District " + closest.getBlockX() + "," + closest.getBlockY() + "," + closest.getBlockZ() + " distance " + distance);
	return closest;

    }

    /**
     * Dynamically creates an inventory of challenges for the player
     * @param player
     * @return
     */
    public Inventory controlPanel(Player player) {
	// Create the control panel for the player
	DistrictRegion d = players.getInDistrict(player.getUniqueId());
	//if (d == null) {
	//    player.sendMessage(ChatColor.RED + "You must be in a district to manage it!");
	//    return null;
	//}
	// New panel map
	HashMap<String,Material> icons = new HashMap<String,Material>();
	icons.put("allowShearing",Material.WHITE_WOOL);
	icons.put("allowGateUse", Material.OAK_FENCE_GATE);
	icons.put("allowBucketUse", Material.BUCKET);
	icons.put("allowChestAccess", Material.CHEST);
	icons.put("allowRedStone", Material.REDSTONE);
	icons.put("allowEnderPearls", Material.ENDER_PEARL);
	icons.put("allowFurnaceUse", Material.FURNACE);
	icons.put("allowCrafting", Material.CRAFTING_TABLE);
	icons.put("allowBedUse", Material.RED_BED);
	icons.put("allowBrewing", Material.BREWING_STAND);
	icons.put("allowDoorUse", Material.OAK_TRAPDOOR);
	icons.put("allowMusic", Material.JUKEBOX);
	icons.put("allowPVP", Material.DIAMOND_SWORD);
	icons.put("allowLeverButtonUse", Material.LEVER);
	icons.put("allowMobHarm", Material.LEATHER);
	icons.put("allowPlaceBlocks", Material.COBBLESTONE);
	icons.put("allowBreakBlocks", Material.MOSSY_COBBLESTONE);
	icons.put("allowCropTrample", Material.WHEAT);
	List<CPItem> cp = new ArrayList<CPItem>();
	int slot = 0;
	// Common options
	ItemStack playerSkull = new ItemStack(Material.PLAYER_HEAD);
	SkullMeta meta = (SkullMeta) playerSkull.getItemMeta();
	meta.setOwner(player.getName());
	playerSkull.setItemMeta(meta);
	if (placeholderapi) {
		cp.add(new CPItem(playerSkull, PlaceholderAPI.setPlaceholders(player, Locale.blocksavailable.replace("[number]", String.valueOf(plugin.players.getBlockBalance(player.getUniqueId())))), false, slot++, null, CPItem.Type.INFO, player ));
	} else {
		cp.add(new CPItem(playerSkull, Locale.blocksavailable.replace("[number]", String.valueOf(plugin.players.getBlockBalance(player.getUniqueId()))), false, slot++, null, CPItem.Type.INFO, player ));
	}
	//cp.add(new CPItem(Material.SKULL_ITEM, 3, Locale.blocksavailable.replace("[number]", String.valueOf(plugin.players.getBlockBalance(player.getUniqueId()))), false, slot++, null, CPItem.Type.INFO ));

	// Check if buying blocks is allowed
	if (VaultHelper.setupEconomy() && Settings.blockPrice > 0D && VaultHelper.checkPerm(player,"districts.buyblocks")) {
	    cp.add(new CPItem(Material.GOLD_INGOT, 0, "Buy Blocks", false, slot++, null, CPItem.Type.BUYBLOCKS, player));
	}
	// Visualize toggle
	cp.add(new CPItem(Material.DIAMOND_BLOCK, 0, "District Visualization", plugin.players.getVisualize(player.getUniqueId()), slot++, null, CPItem.Type.VISUALIZE, player));

	// If we are not in a district just offer the claim options
	if (d == null) {
	    cp.add(new CPItem(Material.GOLDEN_HOE, 0, "Claim", plugin.players.getVisualize(player.getUniqueId()), slot++, null, CPItem.Type.CLAIM, player));
	} else {
	    // Put naming in the first few slots
	    // TODO: Fix it so that Ops and Admins can change claims
	    // If this is a rented district, then owner can only look at the name
	    if (d.getOwner().equals(player.getUniqueId()) && d.getRenter() != null) {
		cp.add(new CPItem(Material.WRITABLE_BOOK, 0, "Rented District",false,slot++, Utils.chop(ChatColor.WHITE,d.getEnterMessage(),20), CPItem.Type.INFO, player));
	    } else {
		cp.add(new CPItem(Material.WRITABLE_BOOK, 0, "Name district",false,slot++, null, CPItem.Type.TEXT, player));
	    }
	    // Add other commands here
	    if ((d.getOwner().equals(player.getUniqueId()) && d.getRenter() == null)
		    || player.isOp() || VaultHelper.checkPerm(player, "districts.admin") ){
		cp.add(new CPItem(Material.IRON_DOOR, 0, "Remove District", false, slot++, null, CPItem.Type.REMOVE, player));
	    }
	    if (VaultHelper.checkPerm(player, "districts.advancedplayer") || player.isOp() || VaultHelper.checkPerm(player, "districts.admin")
		    || VaultHelper.checkPerm(player, "districts.trustplayer")) {
		// Owner
		if (d.getOwner().equals(player.getUniqueId()) || player.isOp() || VaultHelper.checkPerm(player, "districts.admin")) {
		    List<String> trusted = d.getOwnerTrusted();
		    if (!trusted.isEmpty()) {
			trusted.add(0, ChatColor.GREEN + "Owner's trusted players:");
			cp.add(new CPItem(Material.SKELETON_SKULL, 3, "Trust players", false, slot++, trusted, CPItem.Type.TRUST, player));
			cp.add(new CPItem(Material.SKELETON_SKULL, 4, "Untrust players", false, slot++, null, CPItem.Type.UNTRUST, player));
		    } else {
			trusted.addAll(Utils.chop(ChatColor.YELLOW,"Trusting allows full access to district",20));
			cp.add(new CPItem(Material.SKELETON_SKULL, 3, "Trust players", false, slot++, trusted, CPItem.Type.TRUST, player));
		    }    
		} else if (d.getRenter() != null && VaultHelper.checkPerm(player, "districts.advancedplayer") || player.isOp() || VaultHelper.checkPerm(player, "districts.admin")) {
		    List<String> trusted = d.getRenterTrusted();
		    if (!trusted.isEmpty()) {
			trusted.add(0, ChatColor.GREEN + "Renter's trusted players:");
			cp.add(new CPItem(Material.SKELETON_SKULL, 3, "Trust players", false, slot++, trusted, CPItem.Type.TRUST, player));
			cp.add(new CPItem(Material.SKELETON_SKULL, 4, "Untrust players", false, slot++, null, CPItem.Type.UNTRUST, player));
		    } else {
			trusted.addAll(Utils.chop(ChatColor.YELLOW,"Trusting allows full access to district",20));
			cp.add(new CPItem(Material.SKELETON_SKULL, 3, "Trust players", false, slot++, trusted, CPItem.Type.TRUST, player));
		    }
		}
		// Only applicable if there is an economy
		if (VaultHelper.setupEconomy() && VaultHelper.checkPerm(player, "districts.advancedplayer") || player.isOp() || VaultHelper.checkPerm(player, "districts.admin")) {
		    // Only allow these if there is no renter and the owner is doing it and they are not already on sale or rent
		    if (!d.isForSale() && !d.isForRent() && d.getOwner().equals(player.getUniqueId()) && d.getRenter() == null) {
			cp.add(new CPItem(Material.MAP, 0, "Sell District", false, slot++, null, CPItem.Type.SELL, player));
			cp.add(new CPItem(Material.IRON_INGOT, 0, "Rent District", false, slot++, null, CPItem.Type.RENT, player));
		    } else {
			// Renter options:
			if (d.getRenter() != null && d.getRenter().equals(player.getUniqueId())) {
			    if (d.isForRent()) {
				cp.add(new CPItem(Material.LAVA, 0, "Cancel your lease", false, slot++, Utils.chop(ChatColor.RED,"Lease will end in " + plugin.daysToEndOfLease(d) + " days", 20), CPItem.Type.CANCEL, player));
			    } else {
				cp.add(new CPItem(Material.LAVA, 0, "Lease canceled!", false, slot++, Utils.chop(ChatColor.RED,"Lease will end in " + plugin.daysToEndOfLease(d) + " days!", 20), CPItem.Type.INFO, player));
			    }
			} else if (d.getOwner().equals(player.getUniqueId())) {
			    // Owner options
			    // If there is a renter, they can cancel the lease
			    if (d.getRenter() != null) {
				if (d.isForRent()) {
				    cp.add(new CPItem(Material.LAVA, 0, "Cancel lease with renter", false, slot++, Utils.chop(ChatColor.WHITE,"Lease will renew in " + plugin.daysToEndOfLease(d) + " days", 20), CPItem.Type.CANCEL, player));
				} else {
				    cp.add(new CPItem(Material.LAVA, 0, "Lease cancelled with renter", false, slot++, Utils.chop(ChatColor.GREEN,"Lease will end in " + plugin.daysToEndOfLease(d) + " days!", 20), CPItem.Type.INFO, player));
				}
			    } else {
				// No renter - remove for rent option
				if (d.isForRent()) {
				    cp.add(new CPItem(Material.LAVA, 0, "Cancel For Rent", false, slot++, null, CPItem.Type.CANCEL, player));
				} else if (d.isForSale()) {
				    // Remove for sale option
				    cp.add(new CPItem(Material.LAVA, 0, "Cancel For Sale", false, slot++, null, CPItem.Type.CANCEL, player));
				}
			    }
			}
		    }
		}
		// Loop through district flags for this player
		for (String flagName : d.getFlags().keySet()) {
		    // Get the icon
		    if (icons.containsKey(flagName)) {
			//Utils.logger(1,"DEBUG:" + flagName + " : " + d.getFlag(flagName) + " slot " + slot);
			if (d.getRenter() != null && d.getRenter().equals(player.getUniqueId())) {
			    cp.add(new CPItem(icons.get(flagName), 0, flagName, d.getFlag(flagName), slot++, null, CPItem.Type.TOGGLEINFO, player));
			} else {
			    cp.add(new CPItem(icons.get(flagName), 0, flagName, d.getFlag(flagName), slot++, null, CPItem.Type.TOGGLE, player));
			}
		    }
		}
	    }
	}
	// Put all the items into the store for this player so when they click on items we know what to do
	controlPanel.put(player.getUniqueId(),cp);

	if (cp.size() > 0) {
	    // Make sure size is a multiple of 9
	    int size = cp.size() +8;
	    size -= (size % 9);
	    Inventory newPanel;
	    if (placeholderapi) {
			newPanel = Bukkit.createInventory(player, size, ChatColor.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, Locale.controlpaneltitle)));
		} else {
			newPanel = Bukkit.createInventory(player, size, ChatColor.translateAlternateColorCodes('&', Locale.controlpaneltitle));
		}

	    // Fill the inventory and return
	    for (CPItem i : cp) {
		newPanel.addItem(i.getItem());
	    }
	    return newPanel;
	}
	return null;
    }


    /**
     * @return the controlPanel
     */
    public List<CPItem> getControlPanel(Player player) {
	return controlPanel.get(player.getUniqueId());
    }
    public List<IPItem> getInfoPanel(Player player){
	return infoPanel.get(player.getUniqueId());
    }

    public Inventory infoPanel(Player player) {
	List<IPItem> ip = new ArrayList<IPItem>();
	// Create in info panel for the district the player is in
	DistrictRegion d = players.getInDistrict(player.getUniqueId());
	if (d == null) {
	    player.sendMessage(ChatColor.RED + "You must be in a district to see info!");
	    return null;
	}
	// New panel map
	HashMap<String,Material> icons = new HashMap<String,Material>();
	icons.put("allowShearing",Material.WHITE_WOOL);
	icons.put("allowGateUse", Material.OAK_FENCE_GATE);
	icons.put("allowBucketUse", Material.BUCKET);
	icons.put("allowChestAccess", Material.CHEST);
	icons.put("allowRedStone", Material.REDSTONE);
	icons.put("allowEnderPearls", Material.ENDER_PEARL);
	icons.put("allowFurnaceUse", Material.FURNACE);
	icons.put("allowCrafting", Material.CRAFTING_TABLE);
	icons.put("allowBedUse", Material.RED_BED);
	icons.put("allowBrewing", Material.BREWING_STAND);
	icons.put("allowDoorUse", Material.OAK_TRAPDOOR);
	icons.put("allowMusic", Material.JUKEBOX);
	icons.put("allowPVP", Material.DIAMOND_SWORD);
	icons.put("allowLeverButtonUse", Material.LEVER);
	icons.put("allowMobHarm", Material.LEATHER);
	icons.put("allowPlaceBlocks", Material.COBBLESTONE);
	icons.put("allowBreakBlocks", Material.MOSSY_COBBLESTONE);
	icons.put("allowCropTrample", Material.WHEAT);
	int slot = 0;
	// Put the owner's name
	UUID o = d.getOwner();
	UUID r = d.getRenter();
	//Utils.logger(1,"Owner ID = " + o.toString());
	//Utils.logger(1,"Renter ID = " + r.toString());

	// Find out if these guys are online
	Player owner = plugin.getServer().getPlayer(o);

	// Get the list of trusted players
	if (o != null) {
	    List<String> trusted = d.getOwnerTrusted();
	    if (!trusted.isEmpty()) {
		trusted.add(0, ChatColor.GREEN + "Trusted players:");
	    }
	    if (owner != null) {

		ip.add(new IPItem(Material.SKELETON_SKULL, 3,  "Owner: " + owner.getDisplayName(), false, slot++, trusted, IPItem.Type.INFO));
	    } else {
		ip.add(new IPItem(Material.SKELETON_SKULL, 3,  "Owner: " + plugin.players.getName(o), false, slot++, trusted, IPItem.Type.INFO));
	    }
	}

	// Check if buying blocks is allowed
	if (VaultHelper.setupEconomy() && Settings.blockPrice > 0D && VaultHelper.checkPerm(player,"districts.buyblocks")) {
	    List<String> description = new ArrayList<String>();
	    description.add(ChatColor.GOLD + "Blocks cost " + VaultHelper.econ.format(Settings.blockPrice) + " each");
	    description.add(ChatColor.GREEN + "Click to enter how many you want to buy");
	    ip.add(new IPItem(Material.GOLD_INGOT, 0, "Buy Blocks", false, slot++, description, IPItem.Type.BUYBLOCKS));
	}


	// For sale
	if (VaultHelper.setupEconomy() && d.isForSale() && VaultHelper.checkPerm(player, "districts.advancedplayer")) {
	    ip.add(new IPItem(Material.MAP, 0,  "District For Sale!", false, slot++,
		    Utils.chop(ChatColor.YELLOW, "Click to buy for " + VaultHelper.econ.format(d.getPrice()), 20), IPItem.Type.BUY));
	}

	// Renting
	if (VaultHelper.setupEconomy() && r != null) {
	    List<String> trusted = d.getRenterTrusted();
	    if (!trusted.isEmpty()) {
		trusted.add(0, ChatColor.GREEN + "Trusted players:");
	    }
	    Player renter = plugin.getServer().getPlayer(r);
	    if (renter != null) {
		ip.add(new IPItem(Material.SKELETON_SKULL, 3,  "Renter: " + renter.getDisplayName(), false, slot++, trusted, IPItem.Type.INFO));
	    } else {
		ip.add(new IPItem(Material.SKELETON_SKULL, 3,  "Renter: " + plugin.players.getName(r), false, slot++, trusted, IPItem.Type.INFO));
	    }
	    if (d.isForRent()) {
		ip.add(new IPItem(Material.GOLD_INGOT, 0,  "Rent: " + VaultHelper.econ.format(d.getPrice()), false, slot++, 
			Utils.chop(ChatColor.YELLOW, "Due in " + plugin.daysToEndOfLease(d) + " days.", 20), IPItem.Type.INFO));
	    } else {
		ip.add(new IPItem(Material.GOLD_INGOT, 0,  "Lease Cancelled!", false, slot++, 
			Utils.chop(ChatColor.RED, "Lease will end in " + plugin.daysToEndOfLease(d) + " days!", 20), IPItem.Type.INFO));
	    }
	} else {
	    if (VaultHelper.setupEconomy() && d.isForRent() && VaultHelper.checkPerm(player, "districts.advancedplayer")) {
		ip.add(new IPItem(Material.GOLD_INGOT, 0,  "District For Rent!", false, slot++, 
			Utils.chop(ChatColor.YELLOW, "Click to rent for " + VaultHelper.econ.format(d.getPrice()), 20), IPItem.Type.RENT));
	    }
	}	
	// Loop through district flags for this player
	for (String flagName : d.getFlags().keySet()) {
	    // Get the icon
	    if (icons.containsKey(flagName)) {
		//Utils.logger(1,"DEBUG:" + flagName + " : " + d.getFlag(flagName) + " slot " + slot);
		ip.add(new IPItem(icons.get(flagName), 0, flagName, d.getFlag(flagName), slot++));
		// Put all the items into the store for this player so when they click on items we know what to do
		infoPanel.put(player.getUniqueId(),ip);
	    }
	}

	if (ip.size() > 0) {
	    // Make sure size is a multiple of 9
	    int size = ip.size() +8;
	    size -= (size % 9);
	    Inventory newPanel;
		if (placeholderapi) {
			newPanel = Bukkit.createInventory(player, size, PlaceholderAPI.setPlaceholders(player, Locale.infoPanelTitle));
		} else {
			newPanel = Bukkit.createInventory(player, size, Locale.infoPanelTitle);
		}
	    // Fill the inventory and return
	    for (IPItem i : ip) {
		newPanel.addItem(i.getItem());
	    }
	    return newPanel;
	}
	return null;
    }

    /**
     * Returns the maximum number of blocks this player can have per their permission
     * @param p
     * @return amount or -1 if no permission applies
     */
    public int getMaxBlockBalance(Player p) {
	// Check perms
	int max = -1;
	for (permBlock pb : permList) {
	    Utils.logger(2,"DEBUG: checking " + pb.name + " " + pb.numberOfBlocks + ":" + pb.max);
	    if (VaultHelper.checkPerm(p, pb.name)) {
		if (pb.max > max) {
		    max = pb.max;
		} 
	    }
	}
	return max;
    }

    /**
     * Returns how many blocks this player has invested in districts
     * @param p
     * @return
     */
    public int getBlocksInDistricts(Player p) {
	int result = 0;
	for (DistrictRegion d : districts) {
	    if (d.getOwner().equals(p.getUniqueId())) {
		result += d.getArea();
	    }
	}
	return result;
    }

    /**
     * Displays the player's balance to them
     * @param player
     */
    public void showBalance(Player player) {
	// Adding zero blocks will check if the player's balance is more than it should be
	players.addBlocks(player.getUniqueId(), 0);
	int balance = players.getBlockBalance(player.getUniqueId());
	int maxBlocks = getMaxBlockBalance(player);
	if (Settings.maxBlockLimit) {
	    int owned = plugin.getBlocksInDistricts(player);
	    player.sendMessage(ChatColor.GREEN + "" + balance + " free blocks, " + ChatColor.AQUA + owned + " owned blocks, " + ChatColor.GOLD + maxBlocks + " max blocks");
	    if (owned >= maxBlocks){
		player.sendMessage(ChatColor.RED + "You have used all your blocks! Remove districts to free up blocks!");
	    }
	} else {
	    // Check perms to see if this player receives blocks over time
	    boolean receivesBlocks = false;
	    if (Settings.blockTick == 0) {
		receivesBlocks = false;
	    } else {
		for (permBlock pb : permList) {
		    if (VaultHelper.checkPerm(player, pb.name)) {
			if (pb.numberOfBlocks> 0) {
			    Utils.logger(2,"Player has a perm that gives blocks over time");
			    receivesBlocks = true;
			}
		    }
		}
	    }
	    if (receivesBlocks) {
		player.sendMessage(ChatColor.GREEN + "" + balance + " free blocks, "+ ChatColor.GOLD + maxBlocks + " max blocks");
	    } else {
		player.sendMessage(ChatColor.GREEN + "" + balance + " free blocks");
	    }
	}
    }




    /**
     * @return the grid
     */
    public GridManager getGrid(String worldName) {
	return grid.get(worldName);
    }

}
