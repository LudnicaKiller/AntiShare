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
package com.turt2live.antishare.permissions;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.turt2live.antishare.AntiShare;

/**
 * Vault permission handler
 * 
 * @author turt2live
 */
public class VaultPerms {

	private Permission perms;

	/**
	 * Creates a new Vault permissions handler
	 */
	public VaultPerms(){
		Plugin plugin = AntiShare.getInstance().getServer().getPluginManager().getPlugin("Vault");
		if(plugin != null){
			RegisteredServiceProvider<Permission> rsp = plugin.getServer().getServicesManager().getRegistration(Permission.class);
			perms = rsp.getProvider();
		}
	}

	/**
	 * Checks if a player has a permission
	 * 
	 * @param player the player
	 * @param node the permission
	 * @param world the world
	 * @return true if they have it
	 */
	public boolean has(Player player, String node, World world){
		if(perms == null){
			return player.hasPermission(node);
		}
		if(perms.hasSuperPermsCompat()){
			return player.hasPermission(node);
		}
		try{
			return perms.playerHas(world, player.getName(), node);
		}catch(UnsupportedOperationException e){
			return player.hasPermission(node);
		}
	}

}