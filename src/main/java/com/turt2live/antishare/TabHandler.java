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
package com.turt2live.antishare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.turt2live.antishare.util.ASUtils;
import com.turt2live.antishare.util.ErrorStringList;
import com.turt2live.antishare.util.StringList;

public class TabHandler implements TabCompleter {

	protected enum Tab{
		REGION("The last argument is a region name.", new StringList("region"), new StringList("creative", "survival", "adventure")),
		RELOAD("No more arguments", new StringList("reload", "rl")),
		RMREGION("You can enter a name, or remove the region you are standing in.", new StringList("rmregion")),
		EDITREGION("Please enter a value", new StringList("editregion"), new ErrorStringList("Enter a region name"), new StringList("name", "ShowEnterMessage", "ShowExitMessage", "EnterMessage", "ExitMessage", "inventory", "gamemode", "area")),
		LISTREGIONS("Enter a page number (optional)", new StringList("listregions")),
		MIRROR("No more arguments", new StringList("mirror")),
		TOOL("No more arguments", new StringList("tool")),
		MONEY("No more arguments", new StringList("money"), new StringList("on", "off", "status")),
		SIMPLENOTICE("No more arguments", new StringList("simplenotice")),
		CHECK("No more arguments", new StringList("check"), new StringList("creative", "survival", "adventure", "all")),
		HELP("No more arguments", new StringList("help", "?"));

		private StringList[] arguments;
		private StringList valid;
		private String errMsg;

		private Tab(String errMsg, StringList valid, StringList... arguments){
			this.arguments = arguments;
			this.valid = valid;
			this.errMsg = errMsg;
		}

		private Tab(StringList valid, StringList... arguments){
			this.arguments = arguments;
			this.valid = valid;
			this.errMsg = null;
		}

		public StringList getArguments(int argument){
			if(argument >= arguments.length || argument < 0){
				return null;
			}
			return arguments[argument];
		}

		public boolean isThis(String argument){
			String[] array = valid.get();
			for(String a : array){
				if(a.equalsIgnoreCase(argument)){
					return true;
				}
			}
			return false;
		}

		public boolean isPartial(String argument){
			String[] array = valid.get();
			argument = argument.toLowerCase();
			for(String a : array){
				if(a.equalsIgnoreCase(argument) || a.toLowerCase().startsWith(argument)){
					return true;
				}
			}
			return false;
		}

		public void error(CommandSender sender){
			if(errMsg != null && errMsg.trim().length() > 0){
				ASUtils.sendToPlayer(sender, ChatColor.RED + errMsg, true);
			}
		}

		public void error(int argument, CommandSender sender){
			if(argument >= arguments.length || argument < 0){
				// Do nothing
			}else{
				StringList e = arguments[argument];
				if(e.isError()){
					e.print(sender);
				}
			}
		}

	}

	private List<String> preliminary = new ArrayList<String>();

	public TabHandler(){
		for(Tab tab : Tab.values()){
			preliminary.add(tab.name().toLowerCase());
		}
		Collections.sort(preliminary, String.CASE_INSENSITIVE_ORDER);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args){
		if(command.getName().equalsIgnoreCase("antishare")){
			if(args.length > 0){
				int arg = args.length - 2;
				List<String> values = new ArrayList<String>();
				if(args.length == 1){
					for(Tab tab : Tab.values()){
						if(tab.isPartial(args[0])){
							values.add(tab.name().toLowerCase());
						}
					}
				}else{
					for(Tab tab : Tab.values()){
						if(tab.isThis(args[0])){
							StringList list = tab.getArguments(arg);
							if(list != null && !list.isError()){
								if(args.length < arg + 2 && args[arg + 2] != null){
									List<String> l2 = list.getList();
									for(String s : l2){
										if(s.toLowerCase().startsWith(args[1].toLowerCase())){
											values.add(s);
										}
									}
								}else{
									values.addAll(list.getList());
								}
							}else if(list != null && list.isError()){
								tab.error(arg, sender);
							}else{
								tab.error(sender);
							}
						}
					}
				}
				Collections.sort(values, String.CASE_INSENSITIVE_ORDER);
				return values;
			}else{
				return preliminary;
			}
		}
		return null; // Return player list
	}

}
