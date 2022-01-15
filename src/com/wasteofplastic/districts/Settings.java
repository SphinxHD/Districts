package com.wasteofplastic.districts;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;


/**
 * @author ben
 * Where all the settings are
 */
public class Settings {
    // DistrictGuard settings
    public static boolean allowPvP;
    public static boolean allowBreakBlocks;
    public static boolean allowPlaceBlocks;
    public static boolean allowBedUse;
    public static boolean allowBucketUse;
    public static boolean allowShearing;
    public static boolean allowEnderPearls;
    public static boolean allowDoorUse;
    public static boolean allowLeverButtonUse;
    public static boolean allowCropTrample;
    public static boolean allowChestAccess;
    public static boolean allowFurnaceUse;
    public static boolean allowRedStone;
    public static boolean allowMusic;
    public static boolean allowCrafting;
    public static boolean allowBrewing;
    public static boolean allowGateUse;
    public static boolean allowMobHarm;
    public static boolean allowFlowIn;
    public static boolean allowFlowOut;
    // General
    public static List<String> worldName = new ArrayList<String>();
    public static int beginningBlocks;
    public static int checkLeases;
    public static Material visualization;
    public static int blockTick;
    public static double blockPrice;
    public static boolean maxBlockLimit;
    public static int vizRange;
    public static boolean actionbarProtection;
    public static boolean actionbarDistmessage;

   
    
}