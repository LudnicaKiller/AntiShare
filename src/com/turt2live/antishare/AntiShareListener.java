package com.turt2live.antishare;

import java.util.HashMap;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class AntiShareListener implements Listener {

	private AntiShare plugin;
	private HashMap<Player, Long> blockDropTextWarnings = new HashMap<Player, Long>();

	public AntiShareListener(AntiShare p){
		plugin = p;
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent event){
		if(event.isCancelled()){
			return;
		}
		Player player = event.getPlayer();
		if(player == null){
			return;
		}
		if(plugin.storage.bedrockBlocked(player.getWorld())
				&& !player.hasPermission("AntiShare.bedrock")
				&& event.getBlock().getType().equals(Material.BEDROCK)){
			if(plugin.getConfig().getBoolean("other.only_if_creative")){
				if(player.getGameMode() == GameMode.CREATIVE){
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.bedrock", player.getWorld()));
					ASNotification.sendNotification(NotificationType.ILLEGAL_BEDROCK, plugin, player, "BEDROCK");
					event.setCancelled(true);
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_BEDROCK, plugin, player, "BEDROCK");
				}
			}else{
				ASUtils.sendToPlayer(player, plugin.config().getString("messages.bedrock", player.getWorld()));
				ASNotification.sendNotification(NotificationType.ILLEGAL_BEDROCK, plugin, player, "BEDROCK");
				event.setCancelled(true);
			}
		}else{
			ASNotification.sendNotification(NotificationType.LEGAL_BEDROCK, plugin, player, "BEDROCK");
		}
		if(event.isCancelled()){
			return;
		}
		if(plugin.storage.isBlocked(event.getBlock().getType(), BlockedType.BLOCK_BREAK, player.getWorld())){
			System.out.println("BLOCK BREAK: ILLEGAL");
			if(plugin.config().getBoolean("other.only_if_creative", player.getWorld())){
				if(player.getGameMode().equals(GameMode.CREATIVE)
						&& !player.hasPermission("AntiShare.allow.break")){
					event.setCancelled(true);
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.block_break", player.getWorld()));
					ASNotification.sendNotification(NotificationType.ILLEGAL_BLOCK_BREAK, plugin, player, event.getBlock().getType().name());
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_BLOCK_BREAK, plugin, player, event.getBlock().getType().name());
				}
			}else{
				if(!player.hasPermission("AntiShare.allow.break")){
					event.setCancelled(true);
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.block_break", player.getWorld()));
					ASNotification.sendNotification(NotificationType.ILLEGAL_BLOCK_BREAK, plugin, player, event.getBlock().getType().name());
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_BLOCK_BREAK, plugin, player, event.getBlock().getType().name());
				}
			}
		}else{
			ASNotification.sendNotification(NotificationType.LEGAL_BLOCK_BREAK, plugin, player, event.getBlock().getType().name());
		}
		if(event.isCancelled()){
			return;
		}
		if(plugin.config().getBoolean("other.track_blocks", player.getWorld())
				&& !player.hasPermission("AntiShare.blockBypass")){
			if(player.getGameMode().equals(GameMode.SURVIVAL)){
				boolean isBlocked = ASBlockRegistry.isBlockCreative(event.getBlock());
				if(isBlocked){
					if(!plugin.getConfig().getBoolean("other.blockDrops")){
						ASUtils.sendToPlayer(player, plugin.config().getString("messages.creativeModeBlock", player.getWorld()));
						event.setCancelled(true);
					}else{
						ASUtils.sendToPlayer(player, plugin.config().getString("messages.creativeModeBlock", player.getWorld()));
						ASBlockRegistry.unregisterCreativeBlock(event.getBlock());
						Block block = event.getBlock();
						event.setCancelled(true);
						block.setTypeId(0); // Fakes a break
					}
					ASNotification.sendNotification(NotificationType.ILLEGAL_CREATIVE_BLOCK_BREAK, plugin, player, event.getBlock().getType().name());
				}
			}else{
				ASBlockRegistry.unregisterCreativeBlock(event.getBlock());
			}
		}else{
			ASNotification.sendNotification(NotificationType.LEGAL_CREATIVE_BLOCK_BREAK, plugin, player, event.getBlock().getType().name());
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onBlockDamage(BlockDamageEvent event){
		if(event.isCancelled() || event.getPlayer() == null){
			return;
		}
		Player player = event.getPlayer();
		if(!event.isCancelled()){
			if(plugin.getConfig().getBoolean("other.blockDrops")
					&& !player.hasPermission("AntiShare.blockBypass")
					&& ASBlockRegistry.isBlockCreative(event.getBlock())){
				long systemTime = System.currentTimeMillis();
				if(blockDropTextWarnings.containsKey(player)){
					if((systemTime - blockDropTextWarnings.get(player)) > 1000){
						ASUtils.sendToPlayer(player, plugin.getConfig().getString("messages.noBlockDrop"));
						blockDropTextWarnings.remove(player);
						blockDropTextWarnings.put(player, systemTime);
					}
				}else{
					ASUtils.sendToPlayer(player, plugin.getConfig().getString("messages.noBlockDrop"));
					blockDropTextWarnings.put(player, systemTime);
				}
			}
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event){
		if(event.isCancelled() || event.getPlayer() == null){
			return;
		}
		Player player = event.getPlayer();
		if(plugin.storage.isBlocked(event.getBlock().getType(), BlockedType.BLOCK_PLACE, player.getWorld())
				&& !player.hasPermission("AntiShare.allow.place")){
			if(plugin.config().getBoolean("other.only_if_creative", player.getWorld())){
				if(player.getGameMode().equals(GameMode.CREATIVE)){
					event.setCancelled(true);
					ASNotification.sendNotification(NotificationType.ILLEGAL_BLOCK_PLACE, plugin, player, event.getBlock().getType().name());
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.block_place", player.getWorld()));
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_BLOCK_PLACE, plugin, player, event.getBlock().getType().name());
				}
			}else{
				event.setCancelled(true);
				ASNotification.sendNotification(NotificationType.ILLEGAL_BLOCK_PLACE, plugin, player, event.getBlock().getType().name());
				ASUtils.sendToPlayer(player, plugin.config().getString("messages.block_place", player.getWorld()));
			}
		}else{
			ASNotification.sendNotification(NotificationType.LEGAL_BLOCK_PLACE, plugin, player, event.getBlock().getType().name());
		}
		if(event.isCancelled()){
			return;
		}
		//Bedrock check
		if(plugin.storage.bedrockBlocked(player.getWorld())
				&& !player.hasPermission("AntiShare.bedrock")
				&& event.getBlock().getType().equals(Material.BEDROCK)){
			if(plugin.config().getBoolean("other.only_if_creative", player.getWorld())){
				if(player.getGameMode().equals(GameMode.CREATIVE)){
					event.setCancelled(true);
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.bedrock", player.getWorld()));
					ASNotification.sendNotification(NotificationType.ILLEGAL_BEDROCK, plugin, player, "BEDROCK");
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_BEDROCK, plugin, player, "BEDROCK");
				}
			}else{
				event.setCancelled(true);
				ASUtils.sendToPlayer(player, plugin.config().getString("messages.bedrock", player.getWorld()));
				ASNotification.sendNotification(NotificationType.ILLEGAL_BEDROCK, plugin, player, "BEDROCK");
			}
		}else{
			ASNotification.sendNotification(NotificationType.LEGAL_BEDROCK, plugin, player, "BEDROCK");
		}
		if(event.isCancelled()){
			return;
		}
		//Creative Mode Placing
		if(plugin.config().getBoolean("other.track_blocks", player.getWorld())
				&& player.getGameMode() == GameMode.CREATIVE
				&& !player.hasPermission("AntiShare.freePlace")){
			ASBlockRegistry.saveCreativeBlock(event.getBlock());
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onEntityDamage(EntityDamageEvent event){
		if(!(event instanceof EntityDamageByEntityEvent)
				|| event.isCancelled()){
			return;
		}
		Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
		if(damager instanceof Player){
			Player dealer = (Player) damager;
			if(!dealer.getGameMode().equals(GameMode.CREATIVE) && !plugin.getConfig().getBoolean("other.only_if_creative")){
				return;
			}else if(!dealer.getGameMode().equals(GameMode.CREATIVE)){
				return;
			}
			//System.out.println("GM: " + dealer.getGameMode().toString());
			if(event.getEntity() instanceof Player){
				if(plugin.getConfig().getBoolean("other.pvp")){
					ASNotification.sendNotification(NotificationType.LEGAL_PLAYER_PVP, plugin, dealer, ((Player) event.getEntity()).getName());
					return;
				}
				if(!dealer.hasPermission("AntiShare.pvp")){
					ASUtils.sendToPlayer(dealer, plugin.config().getString("messages.pvp", event.getEntity().getWorld()));
					ASNotification.sendNotification(NotificationType.ILLEGAL_PLAYER_PVP, plugin, dealer, ((Player) event.getEntity()).getName());
					event.setCancelled(true);
				}
			}else{
				if(plugin.getConfig().getBoolean("other.pvp-mob")){
					ASNotification.sendNotification(NotificationType.LEGAL_MOB_PVP, plugin, dealer, event.getEntity().getClass().getName().replace("Craft", "").replace("org.bukkit.craftbukkit.entity.", ""));
					return;
				}
				if(!dealer.hasPermission("AntiShare.mobpvp")){
					ASNotification.sendNotification(NotificationType.ILLEGAL_MOB_PVP, plugin, dealer, event.getEntity().getClass().getName().replace("Craft", "").replace("org.bukkit.craftbukkit.entity.", ""));
					ASUtils.sendToPlayer(dealer, plugin.config().getString("messages.mobpvp", event.getEntity().getWorld()));
					event.setCancelled(true);
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_MOB_PVP, plugin, dealer, event.getEntity().getClass().getName().replace("Craft", "").replace("org.bukkit.craftbukkit.entity.", ""));
				}
			}
		}else if(damager instanceof Projectile){
			LivingEntity shooter = ((Projectile) damager).getShooter();
			if(shooter instanceof Player){
				Player dealer = ((Player) shooter);
				if(!dealer.getGameMode().equals(GameMode.CREATIVE) && !plugin.getConfig().getBoolean("other.only_if_creative")){
					ASNotification.sendNotification(NotificationType.LEGAL_PLAYER_PVP, plugin, dealer, ((Player) event.getEntity()).getName());
					return;
				}else if(!dealer.getGameMode().equals(GameMode.CREATIVE)){
					ASNotification.sendNotification(NotificationType.LEGAL_PLAYER_PVP, plugin, dealer, ((Player) event.getEntity()).getName());
					return;
				}
				if(!dealer.hasPermission("AntiShare.pvp")){
					ASUtils.sendToPlayer(dealer, plugin.config().getString("messages.pvp", dealer.getWorld()));
					ASNotification.sendNotification(NotificationType.ILLEGAL_PLAYER_PVP, plugin, dealer, ((Player) event.getEntity()).getName());
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onEntityDeath(EntityDeathEvent event){
		// System.out.println("onDeath | " + event.getEntity());
		if(event.getEntity() instanceof Player){
			Player player = (Player) event.getEntity();
			if(player == null){
				return;
			}
			boolean illegal = false;
			if(!player.hasPermission("AntiShare.allow.death")){
				boolean doCheck = false;
				if(plugin.config().getBoolean("other.only_if_creative", player.getWorld())){
					if(player.getGameMode().equals(GameMode.CREATIVE)){
						doCheck = true;
					}
				}else{
					doCheck = true;
				}
				if(doCheck){
					for(ItemStack item : event.getDrops()){
						if(plugin.storage.isBlocked(item, BlockedType.DEATH, player.getWorld())){
							illegal = true;
							item.setAmount(0);
						}
					}
				}
			}
			if(illegal){
				ASUtils.sendToPlayer(player, plugin.config().getString("messages.death", player.getWorld()));
				ASNotification.sendNotification(NotificationType.ILLEGAL_DEATH, plugin, player, player.getGameMode().toString());
			}else{
				ASNotification.sendNotification(NotificationType.LEGAL_DEATH, plugin, player, player.getGameMode().toString());
			}
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onEntityTarget(EntityTargetEvent event){
		if(event.isCancelled())
			return;

		Entity targetEntity = event.getTarget();
		if(event.getEntity() instanceof Monster
				&& targetEntity != null
				&& targetEntity instanceof Player){
			Player player = (Player) targetEntity;
			if(plugin.getConfig().getBoolean("other.only_if_creative")){
				if(player.getGameMode().equals(GameMode.CREATIVE)){
					if(!player.hasPermission("AntiShare.mobpvp")
							&& !plugin.getConfig().getBoolean("other.mobpvp")){
						event.setCancelled(true);
					}
				}
			}else{
				if(!player.hasPermission("AntiShare.mobpvp")
						&& !plugin.getConfig().getBoolean("other.mobpvp")){
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onPlayerCommand(PlayerCommandPreprocessEvent event){
		String commandSent = event.getMessage();
		if(event.isCancelled()){
			return;
		}
		Player sender = event.getPlayer();
		if(sender.hasPermission("AntiShare.allow.commands")){
			ASNotification.sendNotification(NotificationType.LEGAL_COMMAND, plugin, sender, commandSent);
			return;
		}
		if(plugin.getConfig().getBoolean("other.only_if_creative")){
			if(sender.getGameMode().equals(GameMode.SURVIVAL)){
				ASNotification.sendNotification(NotificationType.LEGAL_COMMAND, plugin, sender, commandSent);
				return;
			}
		}
		if(plugin.storage.commandBlocked(commandSent, sender.getWorld())){
			ASUtils.sendToPlayer(sender, plugin.getConfig().getString("messages.illegalCommand"));
			ASNotification.sendNotification(NotificationType.ILLEGAL_COMMAND, plugin, sender, commandSent);
			event.setCancelled(true);
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event){
		if(event.isCancelled() || event.getPlayer() == null){
			return;
		}
		Player player = event.getPlayer();
		if(player.hasPermission("AntiShare.allow.drop")){
			ASNotification.sendNotification(NotificationType.LEGAL_DROP_ITEM, plugin, player, event.getItemDrop().getItemStack().getType().name());
			return;
		}
		if(plugin.storage.isBlocked(event.getItemDrop().getItemStack(), BlockedType.DROP_ITEM, player.getWorld())){
			if(plugin.config().getBoolean("other.only_if_creative", player.getWorld())){
				if(player.getGameMode().equals(GameMode.CREATIVE)){
					event.setCancelled(true);
					ASNotification.sendNotification(NotificationType.ILLEGAL_DROP_ITEM, plugin, player, event.getItemDrop().getItemStack().getType().name());
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.drop_item", player.getWorld()));
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_DROP_ITEM, plugin, player, event.getItemDrop().getItemStack().getType().name());
				}
			}else{
				event.setCancelled(true);
				ASNotification.sendNotification(NotificationType.ILLEGAL_DROP_ITEM, plugin, player, event.getItemDrop().getItemStack().getType().name());
				ASUtils.sendToPlayer(player, plugin.config().getString("messages.drop_item", player.getWorld()));
			}
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event){
		Player player = event.getPlayer();
		if(plugin.config().getBoolean("other.inventory_swap", event.getPlayer().getWorld())){
			if(player != null){
				if(!player.hasPermission("AntiShare.noswap")){
					ASInventory.save(player, player.getGameMode(), player.getWorld());
					ASInventory.load(player, event.getNewGameMode(), player.getWorld());
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.inventory_swap", player.getWorld()));
				}
			}
		}
		ASNotification.sendNotification(NotificationType.GAMEMODE_CHANGE, plugin, player, event.getNewGameMode().toString());
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event){
		Player player = event.getPlayer();
		if(event.isCancelled() || player == null || event.getClickedBlock() == null){
			return;
		}
		if(!player.hasPermission("AntiShare.allow.interact")
				&& plugin.storage.isBlocked(event.getClickedBlock().getType(), BlockedType.INTERACT, player.getWorld())){
			if(plugin.config().getBoolean("other.only_if_creative", player.getWorld())){
				if(player.getGameMode().equals(GameMode.CREATIVE)){
					event.setCancelled(true);
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.interact", player.getWorld()));
					ASNotification.sendNotification(NotificationType.ILLEGAL_INTERACTION, plugin, player, event.getClickedBlock().getType().name());
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_INTERACTION, plugin, player, event.getClickedBlock().getType().name());
				}
			}else{
				event.setCancelled(true);
				ASUtils.sendToPlayer(player, plugin.config().getString("messages.interact", player.getWorld()));
				ASNotification.sendNotification(NotificationType.ILLEGAL_INTERACTION, plugin, player, event.getClickedBlock().getType().name());
			}
		}else{
			ASNotification.sendNotification(NotificationType.LEGAL_INTERACTION, plugin, player, event.getClickedBlock().getType().name());
		}
		if(event.isCancelled()){
			return;
		}
		//Egg check
		if(plugin.config().getBoolean("other.allow_eggs", player.getWorld()) == false){
			ItemStack possibleEgg = event.getItem();
			if(possibleEgg != null){
				if(possibleEgg.getTypeId() != 383){
					return;
				}
			}else{
				return;
			}
			if(player.hasPermission("AntiShare.allow.eggs")){
				ASNotification.sendNotification(NotificationType.LEGAL_EGG, plugin, player, "MONSTER EGG");
				return;
			}
			// At this point the player is not allowed to use eggs, and we are dealing with an egg
			if(plugin.config().getBoolean("other.only_if_creative", player.getWorld())){
				if(player.getGameMode().equals(GameMode.CREATIVE)){
					event.setCancelled(true);
					ASNotification.sendNotification(NotificationType.ILLEGAL_EGG, plugin, player, "MONSTER EGG");
					ASUtils.sendToPlayer(player, plugin.config().getString("messages.eggs", player.getWorld()));
				}else{
					ASNotification.sendNotification(NotificationType.LEGAL_EGG, plugin, player, "MONSTER EGG");
				}
			}else{
				event.setCancelled(true);
				ASNotification.sendNotification(NotificationType.ILLEGAL_EGG, plugin, player, "MONSTER EGG");
				ASUtils.sendToPlayer(player, plugin.config().getString("messages.eggs", player.getWorld()));
			}
		}
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onPlayerPortal(PlayerPortalEvent event){
		onPlayerTeleport(event);
	}

	@EventHandler (priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event){
		if(event.isCancelled()){
			return;
		}
		Player player = event.getPlayer();
		if(!event.getFrom().getWorld().equals(event.getTo().getWorld())){
			boolean cancel = !ASMultiWorld.worldSwap(plugin, player, event.getFrom(), event.getTo());
			if(cancel){
				ASUtils.sendToPlayer(player, plugin.config().getString("messages.worldSwap", event.getTo().getWorld()));
				ASNotification.sendNotification(NotificationType.ILLEGAL_WORLD_CHANGE, plugin, player, event.getTo().getWorld().getName());
				event.setCancelled(true);
			}else{
				scheduleInventoryChange(player, event);
			}
		}
	}

	public void scheduleInventoryChange(final Player player, final PlayerTeleportEvent event){
		new Thread(new Runnable(){
			@Override
			public void run(){
				long time = System.currentTimeMillis();
				while (player.getLocation().getWorld() != event.getTo().getWorld()){
					if((System.currentTimeMillis() - time) >= 5000){
						AntiShare.log.severe("[" + plugin.getDescription().getFullName() + "] ERROR: World transfer inventory change took longer than 5 seconds!");
						AntiShare.log.severe("[" + plugin.getDescription().getFullName() + "] Please report this to turt2live! http://mc.turt2live.com/plugins/bug.php?simple&plugin=AntiShare");
						return;
					}
				}
				if(!player.hasPermission("AntiShare.worlds")){
					ASInventory.save(player, player.getGameMode(), event.getFrom().getWorld());
					ASInventory.load(player, player.getGameMode(), event.getTo().getWorld());
				}
				ASNotification.sendNotification(NotificationType.LEGAL_WORLD_CHANGE, plugin, player, event.getTo().getWorld().getName());
			}
		}).start();
	}
}
