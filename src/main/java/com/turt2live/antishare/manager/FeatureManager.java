package com.turt2live.antishare.manager;

import java.io.File;

import com.turt2live.antishare.feildmaster.lib.configuration.EnhancedConfiguration;

public class FeatureManager extends AntiShareManager {

	public static enum Feature{
		INVENTORIES, REGIONS, BLOCKS, MONEY, SELF, ALWAYS_ON;
	}

	private EnhancedConfiguration config;

	@Override
	public boolean load(){
		config = new EnhancedConfiguration(new File(plugin.getDataFolder(), "features.yml"), plugin);
		config.load();
		config.loadDefaults(plugin.getResource("resources/features.yml"));
		return true;
	}

	@Override
	public boolean save(){
		return true;
	}

	public boolean isEnabled(Feature feature){
		if(feature == Feature.SELF || feature == Feature.ALWAYS_ON){
			return true;
		}
		return config.getBoolean(feature.name().toLowerCase().trim());
	}

}
