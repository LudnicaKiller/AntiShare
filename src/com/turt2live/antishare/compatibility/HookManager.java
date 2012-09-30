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
package com.turt2live.antishare.compatibility;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import com.turt2live.antishare.AntiShare;
import com.turt2live.antishare.compatibility.type.SignProtection;

public class HookManager {

	private AntiShare plugin = AntiShare.getInstance();
	private List<SignProtection> signs = new ArrayList<SignProtection>();

	public HookManager(){
		Plugin chestshop = plugin.getServer().getPluginManager().getPlugin("ChestShop");
		if(chestshop != null){
			signs.add(new ChestShop());
		}
		Plugin lwc = plugin.getServer().getPluginManager().getPlugin("lwc");
		if(lwc != null){
			signs.add(new LWC());
		}
		Plugin lockette = plugin.getServer().getPluginManager().getPlugin("Lockette");
		if(lockette != null){
			signs.add(new Lockette());
		}
	}

	public boolean checkSourceBlockForProtection(Block block){
		for(SignProtection protection : signs){
			if(protection.isProtected(block)){
				return true;
			}
		}
		return false;
	}

}
