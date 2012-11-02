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
package com.turt2live.antishare.compatibility;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import com.turt2live.antishare.AntiShare;
import com.turt2live.antishare.compatibility.type.BlockProtection;
import com.turt2live.antishare.compatibility.type.SignProtection;

public class HookManager {

	private AntiShare plugin = AntiShare.getInstance();
	private List<SignProtection> signs = new ArrayList<SignProtection>();
	private List<BlockProtection> blocks = new ArrayList<BlockProtection>();

	public HookManager(){
		Plugin chestshop = plugin.getServer().getPluginManager().getPlugin("ChestShop");
		if(chestshop != null){
			signs.add(new ChestShop());
			blocks.add(new ChestShop());
		}
		Plugin lwc = plugin.getServer().getPluginManager().getPlugin("lwc");
		if(lwc != null){
			signs.add(new LWC());
			blocks.add(new LWC());
		}
		Plugin lockette = plugin.getServer().getPluginManager().getPlugin("Lockette");
		if(lockette != null){
			signs.add(new Lockette());
			blocks.add(new Lockette());
		}
	}

	public boolean checkForSignProtection(Block block){
		for(SignProtection protection : signs){
			if(protection.isProtected(block)){
				return true;
			}
		}
		return false;
	}

	public boolean checkForBlockProtection(Block block){
		for(BlockProtection protection : blocks){
			if(protection.isProtected(block)){
				return true;
			}
		}
		return false;
	}

}
