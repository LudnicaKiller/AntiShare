package com.turt2live.antishare.tekkitcompat;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.painting.PaintingBreakByEntityEvent;
import org.bukkit.event.painting.PaintingBreakEvent;
import org.bukkit.event.painting.PaintingBreakEvent.RemoveCause;
import org.bukkit.event.painting.PaintingPlaceEvent;
import org.bukkit.inventory.ItemStack;

import com.turt2live.antishare.AntiShare;
import com.turt2live.antishare.money.Tender.TenderType;
import com.turt2live.antishare.notification.Alert.AlertTrigger;
import com.turt2live.antishare.notification.Alert.AlertType;
import com.turt2live.antishare.notification.MessageFactory;
import com.turt2live.antishare.permissions.PermissionNodes;
import com.turt2live.antishare.storage.PerWorldConfig.ListType;

@SuppressWarnings ("deprecation")
public class PaintingListener implements Listener {

	private AntiShare plugin = AntiShare.getInstance();

	@EventHandler (priority = EventPriority.LOW)
	public void onPaintingBreak(PaintingBreakEvent event){
		if(event.isCancelled() || !plugin.getConfig().getBoolean("enabled-features.no-drops-when-block-break.paintings-are-attached")){
			return;
		}
		if(event.getCause() == RemoveCause.PHYSICS){
			// Removed by something
			Painting hanging = event.getPainting();
			Location block = hanging.getLocation().getBlock().getRelative(hanging.getAttachedFace()).getLocation();
			GameMode gamemode = plugin.getBlockManager().getRecentBreak(block);
			if(gamemode != null && gamemode == GameMode.CREATIVE){
				event.setCancelled(true);
				hanging.remove();
				plugin.getBlockManager().removeEntity(hanging);
			}
		}
	}

	@EventHandler (priority = EventPriority.LOW)
	public void onPaintingBreak(PaintingPlaceEvent event){
		if(event.isCancelled()){
			return;
		}
		Player player = event.getPlayer();
		Painting hanging = event.getPainting();
		AlertType type = AlertType.ILLEGAL;
		Material item = Material.PAINTING;
		boolean region = false;

		// Check if they should be blocked
		if(!plugin.isBlocked(player, PermissionNodes.ALLOW_BLOCK_PLACE, PermissionNodes.DENY_BLOCK_PLACE, hanging.getWorld(), item)){
			type = AlertType.LEGAL;
		}
		// TODO: Regions
		//		ASRegion asregion = plugin.getRegionManager().getRegion(hanging.getLocation());
		//		if(asregion != null){
		//			if(!asregion.getConfig().isBlocked(item, ListType.BLOCK_PLACE)){
		//				type = AlertType.LEGAL;
		//			}
		//		}else{
		if(!plugin.getListener().getConfig(hanging.getWorld()).isBlocked(item, ListType.BLOCK_PLACE)){
			type = AlertType.LEGAL;
		}
		//		}

		// TODO: Regions
		//		if(!plugin.getPermissions().has(player, PermissionNodes.REGION_PLACE)){
		//			ASRegion playerRegion = plugin.getRegionManager().getRegion(player.getLocation());
		//			ASRegion blockRegion = plugin.getRegionManager().getRegion(hanging.getLocation());
		//			if(playerRegion != blockRegion){
		//				type = AlertType.ILLEGAL;
		//				region = true;
		//			}
		//		}

		// Handle event
		if(type == AlertType.ILLEGAL){
			event.setCancelled(plugin.shouldCancel(player, true));
		}else{
			// Handle block place for tracker
			if(!plugin.getPermissions().has(player, PermissionNodes.FREE_PLACE)){
				plugin.getBlockManager().addEntity(player.getGameMode(), hanging);
			}
		}

		// Alert
		if(region){
			if(type == AlertType.ILLEGAL){
				String message = ChatColor.YELLOW + player.getName() + ChatColor.WHITE + (type == AlertType.ILLEGAL ? " tried to place " : " placed ") + (type == AlertType.ILLEGAL ? ChatColor.RED : ChatColor.GREEN) + item.name().replace("_", " ") + ChatColor.WHITE + " in a region.";
				String playerMessage = ChatColor.RED + "You cannot place blocks in another region!";
				plugin.getAlerts().alert(message, player, playerMessage, type, AlertTrigger.BLOCK_PLACE);
			}
		}else{
			String message = ChatColor.YELLOW + player.getName() + ChatColor.WHITE + (type == AlertType.ILLEGAL ? " tried to place " : " placed ") + (type == AlertType.ILLEGAL ? ChatColor.RED : ChatColor.GREEN) + item.name().replace("_", " ");
			String playerMessage = plugin.getMessage("blocked-action.place-block");
			MessageFactory factory = new MessageFactory(playerMessage);
			factory.insert(item, player, hanging.getWorld(), TenderType.BLOCK_PLACE);
			playerMessage = factory.toString();
			plugin.getAlerts().alert(message, player, playerMessage, type, AlertTrigger.BLOCK_PLACE);
		}
	}

	@EventHandler (priority = EventPriority.LOW)
	public void onPaintingBreak(PaintingBreakByEntityEvent event){
		if(event.isCancelled()){
			return;
		}
		Painting hanging = event.getPainting();
		GameMode blockGamemode = plugin.getBlockManager().getType(hanging);
		if(blockGamemode == null){
			return;
		}
		Material item = Material.PAINTING;
		Entity remover = event.getRemover();
		if(remover instanceof Player){
			Player player = (Player) remover;
			AlertType type = AlertType.ILLEGAL;
			boolean special = false;
			boolean region = false;
			Boolean drops = null;
			boolean deny = false;
			AlertType specialType = AlertType.LEGAL;
			String blockGM = "Unknown";

			// Check if they should be blocked
			if(!plugin.isBlocked(player, PermissionNodes.ALLOW_BLOCK_BREAK, PermissionNodes.DENY_BLOCK_BREAK, hanging.getWorld(), item)){
				type = AlertType.LEGAL;
			}
			// TODO: Regions
			//			ASRegion asregion = plugin.getRegionManager().getRegion(hanging.getLocation());
			//			if(asregion != null){
			//				if(!asregion.getConfig().isBlocked(item, ListType.BLOCK_BREAK)){
			//					type = AlertType.LEGAL;
			//				}
			//			}else{
			if(!plugin.getListener().getConfig(hanging.getWorld()).isBlocked(item, ListType.BLOCK_BREAK)){
				type = AlertType.LEGAL;
			}
			//			}

			// Check creative/survival blocks
			if(!plugin.getPermissions().has(player, PermissionNodes.FREE_PLACE)){
				if(blockGamemode != null){
					blockGM = blockGamemode.name().toLowerCase();
					String oGM = player.getGameMode().name().toLowerCase();
					if(player.getGameMode() != blockGamemode){
						special = true;
						deny = plugin.getConfig().getBoolean("settings." + oGM + "-breaking-" + blockGM + "-blocks.deny");
						drops = plugin.getConfig().getBoolean("settings." + oGM + "-breaking-" + blockGM + "-blocks.block-drops");
						if(deny){
							specialType = AlertType.ILLEGAL;
						}
					}
				}
			}

			// Check regions
			// TODO: Regions
			//			if(!plugin.getPermissions().has(player, PermissionNodes.REGION_BREAK)){
			//				ASRegion playerRegion = plugin.getRegionManager().getRegion(player.getLocation());
			//				ASRegion blockRegion = plugin.getRegionManager().getRegion(hanging.getLocation());
			//				if(playerRegion != blockRegion){
			//					special = true;
			//					region = true;
			//					specialType = AlertType.ILLEGAL;
			//				}
			//			}

			// Handle event
			if(type == AlertType.ILLEGAL || specialType == AlertType.ILLEGAL){
				event.setCancelled(plugin.shouldCancel(player, false));
			}else{
				plugin.getBlockManager().removeEntity(hanging);
			}

			// Alert
			if(special){
				if(region){
					if(specialType == AlertType.ILLEGAL){
						String specialMessage = ChatColor.YELLOW + player.getName() + ChatColor.WHITE + (specialType == AlertType.ILLEGAL ? " tried to break " : " broke  ") + (specialType == AlertType.ILLEGAL ? ChatColor.RED : ChatColor.GREEN) + item.name().replace("_", " ") + ChatColor.WHITE + " in a region.";
						String specialPlayerMessage = ChatColor.RED + "You cannot break blocks that are not in your region";
						plugin.getAlerts().alert(specialMessage, player, specialPlayerMessage, specialType, AlertTrigger.BLOCK_BREAK);
					}
				}else{
					String specialMessage = ChatColor.YELLOW + player.getName() + ChatColor.WHITE + (specialType == AlertType.ILLEGAL ? " tried to break the " + blockGM + " block " : " broke the " + blockGM + " block ") + (specialType == AlertType.ILLEGAL ? ChatColor.RED : ChatColor.GREEN) + item.name().replace("_", " ");
					String specialPlayerMessage = plugin.getMessage("blocked-action." + blockGM + "-block-break");
					MessageFactory factory = new MessageFactory(specialPlayerMessage);
					factory.insert(item, player, hanging.getWorld(), blockGM.equalsIgnoreCase("creative") ? TenderType.CREATIVE_BLOCK : (blockGM.equalsIgnoreCase("survival") ? TenderType.SURVIVAL_BLOCK : TenderType.ADVENTURE_BLOCK));
					specialPlayerMessage = factory.toString();
					plugin.getAlerts().alert(specialMessage, player, specialPlayerMessage, specialType, (blockGM.equalsIgnoreCase("creative") ? AlertTrigger.CREATIVE_BLOCK : (blockGM.equalsIgnoreCase("survival") ? AlertTrigger.SURVIVAL_BLOCK : AlertTrigger.ADVENTURE_BLOCK)));
				}
			}else{
				String message = ChatColor.YELLOW + player.getName() + ChatColor.WHITE + (type == AlertType.ILLEGAL ? " tried to break " : " broke ") + (type == AlertType.ILLEGAL ? ChatColor.RED : ChatColor.GREEN) + item.name().replace("_", " ");
				String playerMessage = plugin.getMessage("blocked-action.break-block");
				MessageFactory factory = new MessageFactory(playerMessage);
				factory.insert(item, player, hanging.getWorld(), TenderType.BLOCK_BREAK);
				playerMessage = factory.toString();
				plugin.getAlerts().alert(message, player, playerMessage, type, AlertTrigger.BLOCK_BREAK);
			}

			// Handle drops
			if(drops != null && !deny && special){
				if(drops){
					plugin.getBlockManager().removeEntity(hanging);
					hanging.getWorld().dropItemNaturally(hanging.getLocation(), new ItemStack(item));
					hanging.remove();
				}else{
					plugin.getBlockManager().removeEntity(hanging);
					hanging.remove();
				}
			}
		}else{
			if(blockGamemode == GameMode.CREATIVE && plugin.getConfig().getBoolean("enabled-features.no-drops-when-block-break.paintings-are-attached")){
				event.setCancelled(true);
				hanging.remove();
				plugin.getBlockManager().removeEntity(hanging);
			}
		}
	}

}
