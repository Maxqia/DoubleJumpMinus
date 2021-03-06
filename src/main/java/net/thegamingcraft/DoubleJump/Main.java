package net.thegamingcraft.DoubleJump;

import java.util.List;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Main extends JavaPlugin implements Listener {

	HashMap<Player, Boolean> cooldown = new HashMap<Player, Boolean>();
	HashMap<Player, Boolean> flying = new HashMap<Player, Boolean>();

	private boolean shouldFly (Player p) {
	    if (flying.get(p) != null && flying.get(p))
	        return true;
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
		saveDefaultConfig();
	}

	@Override
	public void onDisable() {
		for (Player pl : Bukkit.getOnlinePlayers()) {
			pl.setFlying(false);
			pl.setAllowFlight(false);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("fly")) {
			Player p = (Player) sender;
			Boolean fly = flying.get(p);
			if (shouldFly(p)) fly = p.getAllowFlight();
			if (fly == null) fly = false;
			fly = !fly;
			p.setAllowFlight(fly);
			p.sendMessage("§6Set fly mode§c " + (fly ? "enabled" : "disabled") + " §6for " + p.getDisplayName() + "§6." );
			flying.put(p, fly);
			return true;
		}
		return false;
	}

	@EventHandler
	public void onGameModeSwitch(PlayerGameModeChangeEvent event) {
		Player p = event.getPlayer();
		flying.put(p, null);
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
					// Play land effect in radius around player
					for (int x = -2; x <= 2; x++) {
						for (int y = -2; y <= 2; y++)
							for (int z = -2; z <= 2; z++)
								for (Player pl : Bukkit.getOnlinePlayers()) {
									Block b = p.getWorld().getBlockAt(p.getLocation().subtract(x, y, z));
									String effect = getConfig().getString("groundpound.landeffect");
									if (!effect.equalsIgnoreCase("disabled"))
										pl.playEffect(b.getLocation(), Effect.valueOf(effect), b.getTypeId());
								}
						}

					// Damage Entities in radius
					if (p.hasPermission("DJN.damage")) {
						List<Entity> nearby = p.getNearbyEntities(5, 5, 5);
						for (Entity entity : nearby) {
							if (entity instanceof Damageable) {
								((Damageable) entity).damage(getConfig().getInt("groundpound.damage")); // Damage Entity
								Vector vector = entity.getLocation().subtract(p.getLocation()).toVector().normalize(); // Get relative direction
								entity.setVelocity(vector.setY(getConfig().getDouble("groundpound.knockback"))); // Set upward force
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

		if (shouldFly(p)) return;
		if (!p.hasPermission("DJN.doubleJump")) return;

		if (cooldown.get(p) != null && cooldown.get(p) == true) {
			p.setAllowFlight(true);
		} else {
			p.setAllowFlight(false);
		}

		if (p.isOnGround()) {
			cooldown.put(p, true);
		}

		if ((!getConfig().getString("doublejump.traileffect").equalsIgnoreCase("disabled")) && cooldown.get(p) != null && cooldown.get(p) == false) {
			for (Player pl : Bukkit.getOnlinePlayers()) {
				pl.playEffect(p.getLocation(), Effect.valueOf(getConfig().getString("doublejump.traileffect")), 2004); // Effect Trail
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onFly(PlayerToggleFlightEvent e) {
		Player p = e.getPlayer();

		if (shouldFly(p)) return;
		if (p.hasPermission("DJN.doubleJump")) {
			if (cooldown.get(p) == true) {
				e.setCancelled(true);
				cooldown.put(p, false);

				Double multiply = getConfig().getDouble("doublejump.speed");
				Double setY = getConfig().getDouble("doublejump.height");
				p.setVelocity(p.getLocation().getDirection().multiply(multiply).setY(setY));

				for (Player pl : Bukkit.getOnlinePlayers()) {
					if (!getConfig().getString("doublejump.launcheffect").equalsIgnoreCase("disabled"))
							pl.playEffect(p.getLocation(), Effect.valueOf(getConfig().getString("doublejump.launcheffect")), 2004);
					if (!getConfig().getString("doublejump.launchsound").equalsIgnoreCase("disabled"))
						pl.playEffect(p.getLocation(), Effect.valueOf(getConfig().getString("doublejump.launchsound")), 2004);
				}

				p.setAllowFlight(false);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onSneak(PlayerToggleSneakEvent e) {
		Player p = e.getPlayer();

		if (shouldFly(p)) return;
		if (!p.hasPermission("DJN.groundPound")) return;

		if (p.isOnGround() == false && cooldown.get(p) != null && cooldown.get(p) == false) {
			p.setVelocity(new Vector(0, getConfig().getInt("groundpound.velocity"), 0)); // Shoot player towards ground
		}
	}
}
