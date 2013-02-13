/*******************************************************************************
 * Copyright (c) 2012 turt2live (Travis Ralston).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 * turt2live (Travis Ralston) - initial API and implementation
 ******************************************************************************/
package com.turt2live.antishare.util.generic;

import java.io.File;
import java.io.IOException;

import org.bukkit.GameMode;

import com.feildmaster.lib.configuration.EnhancedConfiguration;
import com.turt2live.antishare.AntiShare;

public class MoneySaver {

	private static EnhancedConfiguration getFile(){
		AntiShare plugin = AntiShare.getInstance();
		File file = new File(plugin.getDataFolder(), "data" + File.separator + "balance.yml");
		if(!file.exists()){
			try{
				file.createNewFile();
			}catch(IOException e){
				e.printStackTrace();
			}
		}
		EnhancedConfiguration efile = new EnhancedConfiguration(file, plugin);
		efile.load();
		return efile;
	}

	/**
	 * Gets the balance for a player
	 * 
	 * @param player the player
	 * @param gamemode the gamemode to get the balance for
	 * @return a double (balance). If not found this will return 0
	 */
	public static double getLevel(String player, GameMode gamemode){
		EnhancedConfiguration file = getFile();
		double balance = file.getDouble(player + "." + gamemode.name(), 0.0);
		return balance;
	}

	/**
	 * Saves a balance for a player
	 * 
	 * @param player the player
	 * @param gamemode the gamemode to save as
	 * @param balance the balance to save
	 */
	public static void saveLevel(String player, GameMode gamemode, double balance){
		if(balance <= 0){
			return;
		}
		EnhancedConfiguration file = getFile();
		file.set(player + "." + gamemode.name(), balance);
		file.save();
	}

}