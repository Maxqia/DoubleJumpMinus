package net.thegamingcraft.DoubleJump;

import java.util.List;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Main extends JavaPlugin implements Listener {

	HashMap<Player, Boolean> cooldown = new HashMap<Player, Boolean>();

	private boolean gmodeSupport(Player p) {
		switch(p.getGameMode()) {
			case SURVIVAL:
			case ADVENTURE:
				return false;
			default:
				return true;
		}
	}

	@Override
	public void onEnable() {
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		for (Player pl : Bukkit.getOnlinePlayers()) {
			pl.setFlying(false);
			pl.setAllowFlight(false);
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e.getCause() == DamageCause.FALL) {
			if (e.getEntity().getType() == EntityType.PLAYER) {
				Player p = (Player) e.getEntity();

				if (p.hasPermission("DJN.doubleJump") || p.hasPermission("DJN.groundPound")) {
					e.setCancelled(true);
				}

				if (!p.hasPermission("DJN.groundPound")) return;

				if (p.isSneaking() == true) {
					// Play block breaking sound in radius around player
					for (int x = -2; x <= 2; x++) {
						for (int y = -2; y <= 2; y++)
							for (int z = -2; z <= 2; z++)
								for (Player pl : Bukkit.getOnlinePlayers()) {
									Block b = p.getWorld().getBlockAt(p.getLocation().subtract(x, y, z));
									pl.playEffect(b.getLocation(), Effect.STEP_SOUND, b.getTypeId());
								}
					}

					// Damage Entities in radius
					if (p.hasPermission("DJN.damage")) {
						List<Entity> nearby = p.getNearbyEntities(5, 5, 5);
						for (Entity entity : nearby) {
							if (entity instanceof Damageable) {
								((Damageable) entity).damage(1);
								entity.setVelocity(entity.getLocation().subtract(p.getLocation()).toVector().normalize().setY(0.5D));
							}
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();

		if (gmodeSupport(p)) return;
		if (!p.hasPermission("DJN.doubleJump")) return;

		if (cooldown.get(p) != null && cooldown.get(p) == true) {
			p.setAllowFlight(true);
		} else {
			p.setAllowFlight(false);
		}

		if (p.isOnGround()) {
			cooldown.put(p, true);
		}

		if (cooldown.get(p) != null && cooldown.get(p) == false) {
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.playEffect(p.getLocation(), Effect.SMOKE, 2004);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onFly(PlayerToggleFlightEvent e) {
		Player p = e.getPlayer();

		if (gmodeSupport(p)) return;
		if (p.hasPermission("DJN.doubleJump")) {
			if (cooldown.get(p) == true) {
				e.setCancelled(true);
				cooldown.put(p, false);
				p.setVelocity(p.getLocation().getDirection().multiply(1.6D).setY(1.0D));

				for (Player pl : Bukkit.getOnlinePlayers()) {
					pl.playEffect(p.getLocation(), Effect.MOBSPAWNER_FLAMES, 2004);
					pl.playEffect(p.getLocation(), Effect.GHAST_SHOOT, 2004);
				}

				p.setAllowFlight(false);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent e) {
		Player p = e.getPlayer();

		if (gmodeSupport(p)) return;
		if (!p.hasPermission("DJN.groundPound")) return;

		if (p.isOnGround() == false && cooldown.get(p) != null && cooldown.get(p) == false) {
			p.setVelocity(new Vector(0, -5, 0));
		}
	}
}
