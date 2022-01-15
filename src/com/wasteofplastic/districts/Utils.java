package com.wasteofplastic.districts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

public class Utils {
    
    private static Districts plugin = Districts.getPlugin();
    // Debug level. 1 = normal operation. 0 = no console output. 2 and above are for debugging.
    private static int debug = 1;
       
    /**
     * @param debug the debug to set
     */
    protected static void setDebug(int debug) {
        Utils.debug = debug;
    }

    /**
     * @return the debug
     */
    protected static int getDebug() {
        return debug;
    }

    /**
     * General logger method
     * @param level
     * @param message
     */
    protected static void logger(int level, String message) {
	if (debug >= level) {
	    if (level > 1) {
		message = "DEBUG["+level+"]:" + message;
	    }
	    plugin.getLogger().info(message);
	}
    }
    
    /**
     * Converts a serialized location to a Location
     * @param s - serialized location in format "world:x:y:z"
     * @return Location
     */
    static public Location getLocationString(final String s) {
	if (s == null || s.trim() == "") {
	    return null;
	}
	final String[] parts = s.split(":");
	if (parts.length == 4) {
	    final World w = Bukkit.getServer().getWorld(parts[0]);
	    final int x = Integer.parseInt(parts[1]);
	    final int y = Integer.parseInt(parts[2]);
	    final int z = Integer.parseInt(parts[3]);
	    return new Location(w, x, y, z);
	}
	return null;
    }

    /**
     * Converts a location to a simple string representation
     * 
     * @param l
     * @return
     */
    static public String getStringLocation(final Location l) {
	if (l == null) {
	    return "";
	}
	return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    /**
     * Saves a YAML file
     * 
     * @param yamlFile
     * @param fileLocation
     */
    public static void saveYamlFile(YamlConfiguration yamlFile, String fileLocation) {
	File dataFolder = plugin.getDataFolder();
	File file = new File(dataFolder, fileLocation);

	try {
	    yamlFile.save(file);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Loads a YAML file
     * 
     * @param file
     * @return
     */
    public static YamlConfiguration loadYamlFile(String file) {
	File dataFolder = plugin.getDataFolder();
	File yamlFile = new File(dataFolder, file);

	YamlConfiguration config = null;
	if (yamlFile.exists()) {
	    try {
		config = new YamlConfiguration();
		config.load(yamlFile);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    // Create the missing file
	    config = new YamlConfiguration();
	    Bukkit.getLogger().info("No " + file + " found. Creating it...");
	    try {
		config.save(yamlFile);
	    } catch (Exception e) {
		Bukkit.getLogger().severe("Could not create the " + file + " file!");
	    }
	}
	return config;
    }
    
    /**
     * Chops up a long string into a list of strings, with a color
     * @param color
     * @param longLine
     * @param length
     * @return
     */
    public static List<String> chop(ChatColor color, String longLine, int length) {
	List<String> result = new ArrayList<String>();
	//int multiples = longLine.length() / length;
	int i = 0;
	for (i = 0; i< longLine.length(); i += length) {
	    //for (int i = 0; i< (multiples*length); i += length) {
	    int endIndex = Math.min(i + length, longLine.length());
	    String line = longLine.substring(i, endIndex);
	    // Do the following only if i+length is not the end of the string
	    if (endIndex < longLine.length()) {
		// Check if last character in this string is not a space
		if (!line.substring(line.length()-1).equals(" ")) {
		    // If it is not a space, check to see if the next character in long line is a space.
		    if (!longLine.substring(endIndex,endIndex+1).equals(" ")) {
			// If it is not, then we are cutting a word in two and need to backtrack to the last space if possible
			int lastSpace = line.lastIndexOf(" ");
			if (lastSpace < line.length()) {
			    line = line.substring(0, lastSpace);
			    i -= (length - lastSpace -1);
			}
		    }
		} 
	    }
	    //}
	    result.add(color + line);
	}
	//result.add(color + longLine.substring(i, longLine.length()));
	return result;
    }

}
