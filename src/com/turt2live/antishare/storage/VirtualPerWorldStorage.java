package com.turt2live.antishare.storage;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.feildmaster.lib.configuration.EnhancedConfiguration;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.turt2live.antishare.AntiShare;
import com.turt2live.antishare.SQL.SQLManager;
import com.turt2live.antishare.enums.BlockedType;
import com.turt2live.antishare.regions.ASRegion;

public class VirtualPerWorldStorage {

	private AntiShare plugin;
	private World world;
	private Vector<Integer> blocked_break = new Vector<Integer>();
	private Vector<Integer> blocked_place = new Vector<Integer>();
	private Vector<Integer> blocked_drop = new Vector<Integer>();
	private Vector<Integer> blocked_death = new Vector<Integer>();
	private Vector<Integer> blocked_interact = new Vector<Integer>();
	private Vector<String> blocked_commands = new Vector<String>();
	private Vector<Material> tracked_creative_blocks = new Vector<Material>();
	private Vector<ASRegion> gamemode_regions = new Vector<ASRegion>();
	private boolean blocked_bedrock = false;
	private HashMap<Player, ASVirtualInventory> inventories = new HashMap<Player, ASVirtualInventory>();
	public boolean blockDrops;

	public VirtualPerWorldStorage(World world, AntiShare plugin){
		this.plugin = plugin;
		this.world = world;
		build();
	}

	public void build(){
		blocked_break.clear();
		blocked_place.clear();
		blocked_drop.clear();
		blocked_death.clear();
		blocked_interact.clear();
		blocked_commands.clear();
		blocked_bedrock = false;
		inventories.clear();
		gamemode_regions.clear();
		load();
	}

	public boolean command(String command, BlockedType type){
		return blocked_commands.contains(command);
	}

	public void freePlayer(Player player){
		if(inventories.containsKey(player)){
			inventories.get(player).saveInventoryToDisk();
		}
		inventories.remove(player);
	}

	public ASVirtualInventory getInventoryManager(Player player){
		if(!inventories.containsKey(player)){
			inventories.put(player, new ASVirtualInventory(player, world, plugin));
		}
		return inventories.get(player);
	}

	public boolean isBlocked(Material material, BlockedType type){
		switch (type){
		case BEDROCK:
			return blocked_bedrock;
		case BLOCK_PLACE:
			return blocked_place.contains(material.getId());
		case BLOCK_BREAK:
			return blocked_break.contains(material.getId());
		case INTERACT:
			return blocked_interact.contains(material.getId());
		case DEATH:
			return blocked_death.contains(material.getId());
		case DROP_ITEM:
			return blocked_drop.contains(material.getId());
		}
		return false;
	}

	public boolean isCreativeBlock(Block material, BlockedType type){
		switch (type){
		case CREATIVE_BLOCK_PLACE:
			return false;
		case CREATIVE_BLOCK_BREAK:
			if(material.hasMetadata("ASCreative")){
				return material.getMetadata("ASCreative").get(0).asBoolean();
			}
		}
		return false;
	}

	private void load(){
		boolean flatfile = true;
		if(plugin.getSQLManager() != null){
			if(plugin.getSQLManager().isConnected()){
				flatfile = false;
				SQLManager sql = plugin.getSQLManager();
				ResultSet results = sql.getQuery("SELECT DISTINCT username FROM AntiShare_Inventory");
				if(results != null){
					try{
						while (results.next()){
							String username = results.getString("username");
							if(Bukkit.getPlayer(username) != null){
								Player player = Bukkit.getPlayer(username);
								if(player.isOnline()){
									inventories.put(player, new ASVirtualInventory(player, world, plugin));
								}
							}
						}
					}catch(SQLException e){
						e.printStackTrace();
					}
				}
			}
		}
		blocked_bedrock = !plugin.config().getBoolean("other.allow_bedrock", world);
		blockDrops = plugin.config().getBoolean("other.blockDrops", world);
		if(flatfile){
			File listing[] = new File(plugin.getDataFolder(), "inventories").listFiles();
			if(listing != null){
				for(File file : listing){
					String username = file.getName().split("_")[0];
					if(Bukkit.getPlayer(username) != null){
						Player player = Bukkit.getPlayer(username);
						if(player.isOnline()){
							inventories.put(player, new ASVirtualInventory(player, world, plugin));
						}
					}
				}
			}
		}
		String trackedBlocks[] = plugin.getConfig().getString("other.tracked-blocks").split(" ");
		boolean notNoneOrAll = false;
		if(trackedBlocks.length == 1){
			if(trackedBlocks[0].equalsIgnoreCase("*")){
				for(Material m : Material.values()){
					tracked_creative_blocks.add(m);
				}
				notNoneOrAll = false;
			}else if(trackedBlocks[0].equalsIgnoreCase("none")){
				notNoneOrAll = false;
			}
		}
		if(notNoneOrAll){
			for(String tracked : trackedBlocks){
				int id = Integer.valueOf(tracked);
				tracked_creative_blocks.add(Material.getMaterial(id));
			}
		}
		String blockedBreak[] = plugin.config().getString("events.block_break", world).split(" ");
		String blockedPlace[] = plugin.config().getString("events.block_place", world).split(" ");
		String blockedDrop[] = plugin.config().getString("events.drop_item", world).split(" ");
		String blockedDeath[] = plugin.config().getString("events.death", world).split(" ");
		String blockedInteract[] = plugin.config().getString("events.interact", world).split(" ");
		String blockedCommands[] = plugin.config().getString("events.commands", world).split(" ");
		boolean skip = false;
		/*## Block Break ##*/
		if(blockedBreak.length == 1){
			if(blockedBreak[0].equalsIgnoreCase("*")){
				for(Material m : Material.values()){
					blocked_break.add(m.getId());
				}
				skip = true;
			}else if(blockedBreak[0].equalsIgnoreCase("none")){
				skip = true;
			}
		}
		if(!skip){
			for(String blocked : blockedBreak){
				blocked_break.add(Integer.valueOf(blocked));
			}
		}
		skip = false;
		/*## Block Place ##*/
		if(blockedPlace.length == 1){
			if(blockedPlace[0].equalsIgnoreCase("*")){
				for(Material m : Material.values()){
					blocked_place.add(m.getId());
				}
				skip = true;
			}else if(blockedPlace[0].equalsIgnoreCase("none")){
				skip = true;
			}
		}
		if(!skip){
			for(String blocked : blockedPlace){
				blocked_place.add(Integer.valueOf(blocked));
			}
		}
		skip = false;
		/*## Block Drop ##*/
		if(blockedDrop.length == 1){
			if(blockedDrop[0].equalsIgnoreCase("*")){
				for(Material m : Material.values()){
					blocked_drop.add(m.getId());
				}
				skip = true;
			}else if(blockedDrop[0].equalsIgnoreCase("none")){
				skip = true;
			}
		}
		if(!skip){
			for(String blocked : blockedDrop){
				blocked_drop.add(Integer.valueOf(blocked));
			}
		}
		skip = false;
		/*## Death ##*/
		if(blockedDeath.length == 1){
			if(blockedDeath[0].equalsIgnoreCase("*")){
				for(Material m : Material.values()){
					blocked_death.add(m.getId());
				}
				skip = true;
			}else if(blockedDeath[0].equalsIgnoreCase("none")){
				skip = true;
			}
		}
		if(!skip){
			for(String blocked : blockedDeath){
				blocked_death.add(Integer.valueOf(blocked));
			}
		}
		skip = false;
		/*## Interact ##*/
		if(blockedInteract.length == 1){
			if(blockedInteract[0].equalsIgnoreCase("*")){
				for(Material m : Material.values()){
					blocked_interact.add(m.getId());
				}
				skip = true;
			}else if(blockedInteract[0].equalsIgnoreCase("none")){
				skip = true;
			}
		}
		if(!skip){
			for(String blocked : blockedInteract){
				blocked_interact.add(Integer.valueOf(blocked));
			}
		}
		skip = false;
		/*## Commands ##*/
		for(String blocked : blockedCommands){
			blocked_commands.add(blocked);
		}
		/*## Inventories ##*/
		for(Player player : inventories.keySet()){
			inventories.get(player).reload();
		}
		/*## GameMode Regions ##*/
		flatfile = true;
		if(plugin.getConfig().getBoolean("SQL.use") && plugin.getSQLManager() != null){
			if(plugin.getSQLManager().isConnected()){
				SQLManager sql = plugin.getSQLManager();
				ResultSet results = sql.getQuery("SELECT * FROM AntiShare_Regions");
				if(results != null){
					flatfile = false;
					try{
						while (results.next()){
							World world = plugin.getServer().getWorld(results.getString("world"));
							if(!this.world.equals(world)){
								continue;
							}
							Location minimum = new Location(world,
									results.getDouble("mix"),
									results.getDouble("miy"),
									results.getDouble("miz"));
							Location maximum = new Location(world,
									results.getDouble("max"),
									results.getDouble("may"),
									results.getDouble("maz"));
							String setBy = results.getString("creator");
							GameMode gamemode = GameMode.valueOf(results.getString("gamemode"));
							String name = results.getString("regionName");
							boolean enterMessage = results.getInt("showEnter") == 1;
							boolean exitMessage = results.getInt("showExit") == 1;
							ASRegion region = new ASRegion(new CuboidSelection(world, minimum, maximum), setBy, gamemode);
							region.setUniqueID(results.getString("unqiueID"));
							region.setName(name);
							region.setMessageOptions(enterMessage, exitMessage);
							gamemode_regions.add(region);
						}
					}catch(SQLException e){
						e.printStackTrace();
					}
				}
			}
		}
		if(flatfile){
			File[] listing = new File(plugin.getDataFolder(), "regions").listFiles();
			if(listing != null){
				for(File regionFile : listing){
					EnhancedConfiguration regionYAML = new EnhancedConfiguration(regionFile, plugin);
					regionYAML.load();
					World world = plugin.getServer().getWorld(regionYAML.getString("worldName"));
					if(!this.world.equals(world)){
						continue;
					}
					Location minimum = new Location(world,
							regionYAML.getDouble("mi-x"),
							regionYAML.getDouble("mi-y"),
							regionYAML.getDouble("mi-z"));
					Location maximum = new Location(world,
							regionYAML.getDouble("ma-x"),
							regionYAML.getDouble("ma-y"),
							regionYAML.getDouble("ma-z"));
					String setBy = regionYAML.getString("set-by");
					GameMode gamemode = GameMode.valueOf(regionYAML.getString("gamemode"));
					String name = regionYAML.getString("name");
					boolean enterMessage = regionYAML.getBoolean("showEnter");
					boolean exitMessage = regionYAML.getBoolean("showExit");
					ASRegion region = new ASRegion(new CuboidSelection(world, minimum, maximum), setBy, gamemode);
					region.setUniqueID(regionFile.getName().replace(".yml", ""));
					region.setName(name);
					region.setMessageOptions(enterMessage, exitMessage);
					File saveFile = new File(plugin.getDataFolder() + "/region_inventories", region.getUniqueID() + ".yml");
					region.setInventory(ASVirtualInventory.getInventoryFromDisk(saveFile, plugin));
					gamemode_regions.add(region);
				}
			}
		}
	}

	public void reload(){
		saveToDisk();
		build();
	}

	public void removeCreativeBlock(Block block){
		block.setMetadata("ASCreative", new FixedMetadataValue(plugin, false));
	}

	public void saveCreativeBlock(Block block){
		if(!trackBlock(block)){
			return;
		}
		block.setMetadata("ASCreative", new FixedMetadataValue(plugin, true));
	}

	public void saveRegion(ASRegion region){
		gamemode_regions.add(region);
	}

	public void removeRegion(ASRegion region){
		gamemode_regions.remove(region);
	}

	public ASRegion getRegion(Location location){
		for(ASRegion region : gamemode_regions){
			if(region.has(location)){
				return region;
			}
		}
		return null;
	}

	public ASRegion getRegionByID(String id){
		for(ASRegion region : gamemode_regions){
			if(region.getUniqueID().equals(id)){
				return region;
			}
		}
		return null;
	}

	public ASRegion getRegionByName(String name){
		for(ASRegion region : gamemode_regions){
			if(region.getName().equalsIgnoreCase(name)){
				return region;
			}
		}
		return null;
	}

	public Vector<ASRegion> getRegionsNearby(Location location, int distance){
		Vector<ASRegion> regions = new Vector<ASRegion>();
		for(ASRegion region : gamemode_regions){
			// Top (Y)
			if(Math.abs(region.getSelection().getMaximumPoint().getBlockY() - location.getBlockY()) <= distance){
				regions.add(region);
				continue;
			}
			// Side (X)
			if(Math.abs(region.getSelection().getMaximumPoint().getBlockX() - location.getBlockX()) <= distance){
				regions.add(region);
				continue;
			}
			if(Math.abs(region.getSelection().getMinimumPoint().getBlockX() - location.getBlockX()) <= distance){
				regions.add(region);
				continue;
			}
			// Face  (Z)
			if(Math.abs(region.getSelection().getMaximumPoint().getBlockZ() - location.getBlockZ()) <= distance){
				regions.add(region);
				continue;
			}
			if(Math.abs(region.getSelection().getMinimumPoint().getBlockZ() - location.getBlockZ()) <= distance){
				regions.add(region);
				continue;
			}
		}
		if(regions.size() == 0){
			return null;
		}
		return regions;
	}

	public boolean regionExists(ASRegion region){
		return gamemode_regions.contains(region);
	}

	public void saveToDisk(){
		// Clear SQL
		if(plugin.getConfig().getBoolean("SQL.use") && plugin.getSQLManager() != null){
			if(plugin.getSQLManager().isConnected()){
				SQLManager sql = plugin.getSQLManager();
				// Inventory wipe handled in wipe() in ASVirtualInventory
				sql.deleteQuery("DELETE FROM AntiShare_Regions WHERE world='" + world.getName() + "'");
			}
		}
		// Save
		for(Player player : inventories.keySet()){
			inventories.get(player).saveInventoryToDisk();
		}
		for(ASRegion region : gamemode_regions){
			region.saveToDisk();
		}
	}

	public boolean trackBlock(Block block){
		return tracked_creative_blocks.contains(block.getType());
	}

	public int convertCreativeBlocks(){
		int total = 0;
		boolean flatfile = true;
		if(plugin.getSQLManager() != null){
			if(plugin.getSQLManager().isConnected()){
				flatfile = false;
				SQLManager sql = plugin.getSQLManager();
				ResultSet results = sql.getQuery("SELECT * FROM AntiShare_Blocks");
				if(results != null){
					try{
						while (results.next()){
							Location location = new Location(Bukkit.getWorld(results.getString("world")), results.getInt("blockX"), results.getInt("blockY"), results.getInt("blockZ"));
							Block block = Bukkit.getWorld(results.getString("world")).getBlockAt(location);
							if(!block.hasMetadata("ASCreative")){
								saveCreativeBlock(block);
								total++;
							}
						}
						sql.deleteQuery("DELETE FROM AntiShare_Blocks"); // Free up space
					}catch(SQLException e){
						e.printStackTrace();
					}
				}
			}
		}
		if(flatfile){
			File listing[] = new File(plugin.getDataFolder(), "blocks").listFiles();
			if(listing != null){
				for(File file : listing){
					if(file.getName().startsWith(world.getName())){
						EnhancedConfiguration blockRegistryData = new EnhancedConfiguration(file, plugin);
						blockRegistryData.load();
						Set<String> keys = blockRegistryData.getConfigurationSection("").getKeys(false);
						for(String x : keys){
							Set<String> keys2 = blockRegistryData.getConfigurationSection(x).getKeys(false);
							for(String y : keys2){
								Set<String> keys3 = blockRegistryData.getConfigurationSection(y).getKeys(false);
								for(String z : keys3){
									Block block = world.getBlockAt(new Location(world, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z)));
									if(!block.hasMetadata("ASCreative")){
										saveCreativeBlock(block);
										total++;
									}
								}
							}
						}
					}
					file.delete(); // Free up space
				}
			}
		}
		return total;
	}
}
