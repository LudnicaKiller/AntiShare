package com.turt2live.antishare.blocks;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.GameMode;

import com.turt2live.antishare.AntiShare;
import com.turt2live.antishare.feildmaster.lib.configuration.EnhancedConfiguration;

class ObjectSaver implements Runnable {

	private final Map<String, List<String>> list = new HashMap<String, List<String>>();
	private final AntiShare plugin = AntiShare.getInstance();
	private final BlockManager blockman = plugin.getBlockManager();
	private final File dir;
	private final String gamemode;
	private final String identity;
	private boolean clear = false, load = false;
	private double completed = 0;
	private final int listSize;
	private boolean done = false;

	public ObjectSaver(CopyOnWriteArrayList<String> list, GameMode gm, File saveDir, String identity, boolean isBlock){
		if(this instanceof NullObjectSaver){
			dir = null;
			gamemode = null;
			this.identity = null;
			listSize = 0;
			return;
		}
		this.dir = saveDir;
		this.gamemode = gm.name();
		this.identity = identity;
		for(String item : list){
			/*
			 * 0 = chunkX
			 * 1 = chunkZ
			 * 2 = world name
			 * 3 = block x
			 * 4 = block y
			 * 5 = block z
			 * 6 = (if provided) entity type as string
			 */
			String[] parts = item.split(";");
			if(parts.length < (isBlock ? 6 : 7) || parts.length > (isBlock ? 6 : 7)){
				plugin.getLogger().warning("INVALID " + (isBlock ? "BLOCK" : "ENTITY") + ": " + item + " (GM=" + gm.name() + "). Report this to Turt2Live.");
				continue;
			}
			String fname = parts[0] + "." + parts[1] + "." + parts[2] + ".yml";
			List<String> items = new ArrayList<String>();
			if(this.list.containsKey(fname)){
				items = this.list.get(fname);
			}
			items.add(parts[3] + ";" + parts[4] + ";" + parts[5] + ";" + parts[2] + (isBlock ? "" : ";" + parts[6]));
			this.list.put(fname, items);
		}
		this.listSize = this.list.keySet().size();
	}

	@Override
	public void run(){
		done = false;
		for(String chunk : list.keySet()){
			List<String> list = this.list.get(chunk);
			for(String item : list){
				save(dir, chunk, item);
				completed++;
			}
			blockman.getFile(dir, chunk).save();
		}
		blockman.markSaveAsDone(identity, this);
		done = true;
	}

	void save(File dir, String fname, String key){
		EnhancedConfiguration file = blockman.getFile(dir, fname);
		file.set(key, gamemode);
	}

	boolean getClear(){
		return clear;
	}

	boolean getLoad(){
		return load;
	}

	void setClear(boolean clear){
		this.clear = clear;
	}

	void setLoad(boolean load){
		this.load = load;
	}

	double getPercent(){
		if(done){
			return 100; // Redundant
		}
		return listSize == 0 ? 100 : ((completed / listSize) * 100);
	}

}
