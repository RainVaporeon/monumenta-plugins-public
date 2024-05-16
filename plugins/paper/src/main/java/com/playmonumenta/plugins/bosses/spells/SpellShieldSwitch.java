package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellShieldSwitch extends Spell {
	private final Mob mLauncher;
	private final Plugin mPlugin;

	public SpellShieldSwitch(Mob launcher, Plugin plugin) {
		mLauncher = launcher;
		mPlugin = plugin;
	}

	@Override
	public void run() {
		LivingEntity target = mLauncher.getTarget();

		ItemStack curItem = mLauncher.getEquipment().getItemInMainHand();
		ItemStack offItem = mLauncher.getEquipment().getItemInOffHand();

		if (curItem == null || offItem == null) {
			return;
		}

		if (target == null || mLauncher.getLocation().distance(target.getLocation()) > 8) {
			// Switch to shield if not already equipped

			if (!curItem.getType().equals(Material.SHIELD) && offItem.getType().equals(Material.SHIELD)) {
				// Need to switch hands - offhand is shield
				mLauncher.getEquipment().setItemInMainHand(offItem);
				mLauncher.getEquipment().setItemInOffHand(curItem);
			}
		} else {
			// Switch to non-shield weapon if not already equipped

			if (curItem.getType().equals(Material.SHIELD)) {
				// Need to switch - using a shield currently
				if (!offItem.getType().equals(Material.SHIELD)) {
					// Need to switch hands - offhand is not a shield and mainhand is
					mLauncher.getEquipment().setItemInMainHand(offItem);
					mLauncher.getEquipment().setItemInOffHand(curItem);
				}
			}
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (damager instanceof Projectile) {
			ItemStack curItem = mLauncher.getEquipment().getItemInMainHand();

			if (curItem != null && curItem.getType().equals(Material.SHIELD)) {
				event.setDamage(0.01);
				mLauncher.getWorld().playSound(mLauncher.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1.0f, 1.0f);
				if (source instanceof Player player) {
					player.playSound(mLauncher.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 0.5f, 1.0f);
				}

				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						mLauncher.setVelocity(new Vector(0, 0, 0));
						mTicks += 1;
						if (mTicks > 2) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 10;
	}
}
