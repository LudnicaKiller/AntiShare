/*******************************************************************************
 * Copyright (c) 2012 turt2live (Travis Ralston).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 * turt2live (Travis Ralston) - initial API and implementation
 ******************************************************************************/
package com.turt2live.antishare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.feildmaster.lib.configuration.PluginWrapper;
import com.turt2live.antishare.compatibility.HookManager;
import com.turt2live.antishare.inventory.InventoryManager;
import com.turt2live.antishare.metrics.Metrics;
import com.turt2live.antishare.metrics.TrackerList;
import com.turt2live.antishare.metrics.TrackerList.TrackerType;
import com.turt2live.antishare.money.MoneyManager;
import com.turt2live.antishare.notification.Alert;
import com.turt2live.antishare.notification.Messages;
import com.turt2live.antishare.permissions.PermissionNodes;
import com.turt2live.antishare.permissions.Permissions;
import com.turt2live.antishare.regions.ASRegion;
import com.turt2live.antishare.regions.RegionFactory;
import com.turt2live.antishare.regions.RegionManager;
import com.turt2live.antishare.signs.SignManager;
import com.turt2live.antishare.storage.BlockManager;
import com.turt2live.antishare.storage.ItemMap;
import com.turt2live.antishare.storage.PerWorldConfig;
import com.turt2live.antishare.storage.SQL;
import com.turt2live.antishare.xmail.XMailListener;

/**
 * AntiShare
 * 
 * @author turt2live
 */
public class AntiShare extends PluginWrapper {

	/**
	 * AntiShare tool material
	 */
	public static final Material ANTISHARE_TOOL = Material.BLAZE_ROD;
	private static AntiShare instance;
	private boolean useSQL = false;
	private boolean sqlRetry = false;
	private Permissions permissions;
	private ItemMap itemMap;
	private ASListener listener;
	private Alert alerts;
	private Messages messages;
	private RegionManager regions;
	private RegionFactory factory;
	private BlockManager blocks;
	private InventoryManager inventories;
	private SQL sql;
	private Metrics metrics;
	private TrackerList trackers;
	private SignManager signs;
	private MoneyManager tender;
	private XMailListener xmail;
	private List<String> disabledSNPlayers = new ArrayList<String>();
	private HookManager hooks;
	private ASTimer timer;
	private String timerUID;

	/**
	 * Gets the active AntiShare instance
	 * 
	 * @return the instance
	 */
	public static AntiShare getInstance(){
		return instance;
	}

	@Override
	public void onEnable(){
		instance = this;
		timer = new ASTimer();
		timerUID = timer.addListener(new Timer());
		long timerId = timer.start(this.getClass(), "STARTUP");

		// File check
		if(!getDataFolder().exists()){
			getDataFolder().mkdirs();
		}

		// Get all disabled SimpleNotice users
		try{
			File snFile = new File(getDataFolder(), "disabled-simplenotice-users.txt");
			if(snFile.exists()){
				BufferedReader in = new BufferedReader(new FileReader(snFile));
				String line;
				while ((line = in.readLine()) != null){
					disabledSNPlayers.add(line);
				}
				in.close();
			}else{
				snFile.createNewFile();
			}
		}catch(IOException e){
			e.printStackTrace();
		}

		// We need to initiate an SQL connection now
		sql = new SQL();
		if(getConfig().getBoolean("enabled-features.sql")){
			// Setup properties
			String hostname = getConfig().getString("settings.sql.host");
			String username = getConfig().getString("settings.sql.username");
			String password = getConfig().getString("settings.sql.password");
			String database = getConfig().getString("settings.sql.database");

			// Try connection
			boolean connected = sql.connect(hostname, username, password, database);
			if(connected){
				sql.setup();
				useSQL = true;
			}
		}

		// Check configuration
		getConfig().loadDefaults(getResource("resources/config.yml"));
		if(!getConfig().fileExists() || !getConfig().checkDefaults()){
			getConfig().saveDefaults();
		}
		getConfig().load();

		// Check for online mode
		if(!getServer().getOnlineMode()){
			if(!getConfig().getBoolean("other.quiet-offline-mode-warning")){
				getLogger().severe("**********************");
				getLogger().severe("Your server is in Offline Mode. AntiShare does not support offline mode servers.");
				getLogger().severe("AntiShare will still run, but you will not get help from turt2live!!");
				getLogger().severe("You can turn this message off in the configuration.");
				getLogger().severe("**********************");
			}
		}

		// Setup (order is important!)
		try{
			metrics = new Metrics(this);
		}catch(IOException e1){
			getLogger().severe("AntiShare encountered and error. Please report this to turt2live.");
			e1.printStackTrace();
		}

		// Register SimpleNotice channel to AntiShare
		getServer().getMessenger().registerOutgoingPluginChannel(this, "SimpleNotice");

		hooks = new HookManager();
		signs = new SignManager();
		trackers = new TrackerList();
		tender = new MoneyManager();
		permissions = new Permissions();
		itemMap = new ItemMap();
		listener = new ASListener();
		alerts = new Alert();
		messages = new Messages();
		regions = new RegionManager();
		factory = new RegionFactory();
		blocks = new BlockManager();
		inventories = new InventoryManager();

		// Migrate world configurations
		PerWorldConfig.migrate();

		// Migrate region players (3.8.0-3.9.0)
		migratePlayerData();

		// xMail integration
		if(getConfig().getBoolean("xMail.hook-into-xMail")){
			trackers.getTracker(TrackerType.FEATURE_XMAIL).increment(1);
			Plugin xmail = getServer().getPluginManager().getPlugin("xMail");
			if(xmail != null){
				this.xmail = new XMailListener();
				getServer().getPluginManager().registerEvents(this.xmail, this);
			}
		}

		// Statistics
		UpdateChecker.start();
		// mcstats.org
		trackers.addTo(metrics);
		metrics.start(); // Handles it's own opt-out

		// Start listeners
		getServer().getPluginManager().registerEvents(permissions, this);
		getServer().getPluginManager().registerEvents(listener, this);

		// Command handlers
		getCommand("antishare").setExecutor(new CommandHandler());

		// Check players
		for(Player player : Bukkit.getOnlinePlayers()){
			ASRegion playerRegion = regions.getRegion(player.getLocation());
			if(playerRegion != null){
				playerRegion.alertSilentEntry(player);
			}
		}

		// Enabled
		getLogger().info("Enabled!");

		// Scan for players
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
			@Override
			public void run(){
				for(Player player : Bukkit.getOnlinePlayers()){
					inventories.loadPlayer(player);
				}
			}
		});

		// Conflict messages
		getServer().getScheduler().scheduleSyncDelayedTask(this, new ConflictThread());
		timer.stop(this.getClass(), "STARTUP", timerId);
	}

	@Override
	public void onDisable(){
		long timerId = timer.start(this.getClass(), "SHUTDOWN");
		// Save
		if(regions != null)
			regions.save();
		if(blocks != null)
			blocks.save();
		if(inventories != null)
			inventories.save();
		if(tender != null)
			tender.save();
		if(metrics != null)
			metrics.flush();
		if(sql != null)
			sql.disconnect();

		// Disable
		getServer().getScheduler().cancelTasks(this);
		getLogger().info("Disabled!");

		// Prepare as though it's a reload
		permissions = null;
		itemMap = null;
		listener = null;
		alerts = null;
		messages = null;
		regions = null;
		factory = null;
		blocks = null;
		inventories = null;
		sql = null;
		metrics = null;
		trackers = null;
		signs = null;
		tender = null;
		xmail = null;
		hooks = null;

		// Save disabled SimpleNotice users
		try{
			File snFile = new File(getDataFolder(), "disabled-simplenotice-users.txt");
			BufferedWriter out = new BufferedWriter(new FileWriter(snFile, false));
			for(String user : disabledSNPlayers){
				out.write(user + "\r\n");
			}
			out.close();
		}catch(IOException e){
			e.printStackTrace();
		}
		timer.stop(this.getClass(), "SHUTDOWN", timerId);
		timer.removeListener(timerUID);
	}

	/**
	 * Reloads AntiShare
	 */
	public void reload(){
		long timerId = timer.start(this.getClass(), "RELOAD");
		reloadConfig();
		// Permissions has no reload
		itemMap.reload();
		signs.reload();
		listener.reload();
		alerts.reload();
		messages.reload();
		tender.reload();
		regions.reload();
		// Region Factory has no reload
		blocks.reload();
		inventories.reload();
		// SQL has no reload
		// Metrics has no reload
		// Tracker List has no reload
		// Simple Notice has no reload
		// xMail has no reload
		timer.stop(this.getClass(), "RELOAD", timerId);
	}

	/**
	 * Determines if a player decided to turn off SimpleNotice support
	 * 
	 * @param name the player name
	 * @return true if enabled (gets messages through SimpleNotice)
	 */
	public boolean isSimpleNoticeEnabled(String name){
		return !disabledSNPlayers.contains(name);
	}

	/**
	 * Enables SimpleNotice support for a user
	 * 
	 * @param name the user
	 */
	public void enableSimpleNotice(String name){
		disabledSNPlayers.remove(name);
	}

	/**
	 * Disables SimpleNotice support for a user
	 * 
	 * @param name the user
	 */
	public void disableSimpleNotice(String name){
		disabledSNPlayers.add(name);
	}

	/**
	 * Determines if a player is blocked from doing something
	 * 
	 * @param player the player
	 * @param allowPermission the "allow" permission
	 * @param world the world
	 * @return true if blocked
	 */
	public boolean isBlocked(Player player, String allowPermission, World world){
		if(permissions.has(player, allowPermission, world)){
			return false;
		}
		if(permissions.has(player, PermissionNodes.AFFECT_CREATIVE, world) && player.getGameMode() == GameMode.CREATIVE){
			return true;
		}
		if(permissions.has(player, PermissionNodes.AFFECT_SURVIVAL, world) && player.getGameMode() == GameMode.SURVIVAL){
			return true;
		}
		if(permissions.has(player, PermissionNodes.AFFECT_ADVENTURE, world) && player.getGameMode() == GameMode.ADVENTURE){
			return true;
		}
		return false;
	}

	/**
	 * Determines if a player is blocked from doing something
	 * 
	 * @param player the player
	 * @param allowPermission the "allow" permission
	 * @param world the world
	 * @param inverse invert the player's Game Mode when checking
	 * @return true if blocked
	 */
	public boolean isBlocked(Player player, String allowPermission, World world, boolean inverse){
		if(permissions.has(player, allowPermission, world)){
			return false;
		}
		if(permissions.has(player, PermissionNodes.AFFECT_CREATIVE, world) && player.getGameMode() == (inverse ? GameMode.SURVIVAL : GameMode.CREATIVE)){
			return true;
		}
		if(permissions.has(player, PermissionNodes.AFFECT_SURVIVAL, world) && player.getGameMode() == (!inverse ? GameMode.SURVIVAL : GameMode.CREATIVE)){
			return true;
		}
		return false;
	}

	/**
	 * Gets a message
	 * 
	 * @param path the path to the message
	 * @return the message
	 */
	public String getMessage(String path){
		return messages.getMessage(path);
	}

	/**
	 * Gets the messages handler in AntiShare
	 * 
	 * @return the messages handler
	 */
	public Messages getMessages(){
		return messages;
	}

	/**
	 * Gets the permissions handler for AntiShare
	 * 
	 * @return the permissions
	 */
	public Permissions getPermissions(){
		return permissions;
	}

	/**
	 * Gets the Item Map for AntiShare
	 * 
	 * @return the item map
	 */
	public ItemMap getItemMap(){
		return itemMap;
	}

	/**
	 * Gets the Alert instance for AntiShare
	 * 
	 * @return the alerts system
	 */
	public Alert getAlerts(){
		return alerts;
	}

	/**
	 * Gets the listener being used by AntiShare
	 * 
	 * @return the listener
	 */
	public ASListener getListener(){
		return listener;
	}

	/**
	 * Gets the region manager being used by AntiShare
	 * 
	 * @return the region manager
	 */
	public RegionManager getRegionManager(){
		return regions;
	}

	/**
	 * Gets the region factory being used by AntiShare
	 * 
	 * @return the region factory
	 */
	public RegionFactory getRegionFactory(){
		return factory;
	}

	/**
	 * Gets the block manager being used by AntiShare
	 * 
	 * @return the block manager
	 */
	public BlockManager getBlockManager(){
		return blocks;
	}

	/**
	 * Gets the inventory manager being used by AntiShare
	 * 
	 * @return the inventory manager
	 */
	public InventoryManager getInventoryManager(){
		return inventories;
	}

	/**
	 * Gets the metrics being used by AntiShare
	 * 
	 * @return the metrics
	 */
	public Metrics getMetrics(){
		return metrics;
	}

	/**
	 * Gets the tracker list being used by AntiShare
	 * 
	 * @return the trackers
	 */
	public TrackerList getTrackers(){
		return trackers;
	}

	/**
	 * Gets the sign manager being used by AntiShare
	 * 
	 * @return the sign manager
	 */
	public SignManager getSignManager(){
		return signs;
	}

	/**
	 * Gets the money manager (rewards/fines) being used by AntiShare
	 * 
	 * @return the money manager
	 */
	public MoneyManager getMoneyManager(){
		return tender;
	}

	/**
	 * Gets the SQL manager for AntiShare
	 * 
	 * @return the SQL manager
	 */
	public SQL getSQL(){
		return sql;
	}

	/**
	 * Determines if AntiShare should use SQL or not
	 * 
	 * @return true if SQL should be used
	 */
	public boolean useSQL(){
		if(getConfig().getBoolean("enabled-features.sql") && !useSQL && !sqlRetry){
			startSQL();
			sqlRetry = true;
		}
		return useSQL && getConfig().getBoolean("enabled-features.sql") && sql.isConnected();
	}

	/**
	 * Force starts the SQL connection
	 * 
	 * @return true if connected
	 */
	public boolean startSQL(){
		sql = new SQL();

		// Setup properties
		String hostname = getConfig().getString("settings.sql.host");
		String username = getConfig().getString("settings.sql.username");
		String password = getConfig().getString("settings.sql.password");
		String database = getConfig().getString("settings.sql.database");

		// Try connection
		boolean connected = sql.connect(hostname, username, password, database);
		if(connected){
			sql.setup();
			useSQL = true;
			return true;
		}

		// Failed connection
		return false;
	}

	/**
	 * Gets the Hook Manager in use by AntiShare
	 * 
	 * @return the hook manager
	 */
	public HookManager getHookManager(){
		return hooks;
	}

	/**
	 * Gets the AntiShare debug timer
	 * 
	 * @return the AntiShare debug timer
	 */
	public ASTimer getTimer(){
		return timer;
	}

	/**
	 * Log a message
	 * 
	 * @param string the message
	 * @param level the level
	 */
	public void log(String string, Level level){
		getLogger().log(level, string);
	}

	/**
	 * Migrates player data from region_players to data/region_players
	 */
	public static void migratePlayerData(){
		AntiShare plugin = AntiShare.getInstance();
		File newSaveFolder = new File(plugin.getDataFolder(), "data" + File.separator + "region_players");
		File oldSaveFolder = new File(plugin.getDataFolder(), "region_players");
		newSaveFolder.mkdirs();
		if(oldSaveFolder.exists()){
			File[] files = oldSaveFolder.listFiles();
			if(files != null && files.length > 0){
				for(File file : files){
					file.renameTo(new File(newSaveFolder, file.getName()));
				}
				plugin.getLogger().info("Region Player Files Migrated: " + files.length);
			}
			oldSaveFolder.delete();
		}
	}

}
