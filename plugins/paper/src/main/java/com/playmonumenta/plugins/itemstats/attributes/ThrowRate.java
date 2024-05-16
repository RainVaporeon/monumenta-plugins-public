package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.depths.abilities.steelsage.DepthsVolley;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enchantments.TwoHanded;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ThrowRate implements Attribute {

	@Override
	public String getName() {
		return "Throw Rate";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.THROW_RATE;
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile proj) {
		Quickdraw quickdraw = AbilityManager.getManager().getPlayerAbility(player, Quickdraw.class);
		boolean isQuickdraw = quickdraw != null && quickdraw.isQuickDraw(proj);

		if (isQuickdraw) {
			return;
		}

		if (proj instanceof Trident trident) {
			ItemStack item = trident.getItemStack();

			//Check for Two Handed Curse.
			if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TWO_HANDED) > 0) {
				if (TwoHanded.checkForOffhand(plugin, player)) {
					return;
				}
			}

			// Only run Throw Rate if the Infinity enchantment is not on the trident
			if (item.getEnchantmentLevel(Enchantment.ARROW_INFINITE) <= 0 && value > 0) {
				event.setCancelled(true);
				// If a trident made from the volley skill, don't run sound/unbreaking
				Volley volley = AbilityManager.getManager().getPlayerAbility(player, Volley.class);
				DepthsVolley depthsVolley = AbilityManager.getManager().getPlayerAbility(player, DepthsVolley.class);
				boolean isVolley = (volley != null && volley.mVolley.contains(trident))
					|| (depthsVolley != null && depthsVolley.mDepthsVolley.contains(trident));

				if (isVolley) {
					return;
				}

				// Make trident unpickupable, set cooldown, damage trident based on Unbreaking enchant

				player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 1);
				player.setCooldown(item.getType(), (int) (20 / value));
				new BukkitRunnable() {
					@Override
					public void run() {
						player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 2.0f, 1);
					}
				}.runTaskLater(plugin, (int) (20 / value));

				// Duplicate the entity, then cancel the throw event so the trident doesn't leave inventory
				Trident newProj = NmsUtils.getVersionAdapter().duplicateEntity(trident);

				// Set a bunch of stuff that isn't caught by the entity duplication
				newProj.setShooter(player);
				DamageListener.addProjectileItemStats(newProj, player);

				newProj.setPickupStatus(PickupStatus.CREATIVE_ONLY);
				trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);

				if (player.getGameMode() != GameMode.CREATIVE && !isVolley) {
					ItemUtils.damageItemWithUnbreaking(plugin, player, player.getInventory().getItemInMainHand(), 1, true);
				}
				AbilityManager.getManager().playerShotProjectileEvent(player, newProj);
			}
		} else if (proj instanceof Snowball oldSnowball) {
			if (value > 0) {
				// If a trident made from the volley skill, don't run sound/unbreaking
				Volley volley = AbilityManager.getManager().getPlayerAbility(player, Volley.class);
				DepthsVolley depthsVolley = AbilityManager.getManager().getPlayerAbility(player, DepthsVolley.class);
				boolean isVolley = (volley != null && volley.mVolley.contains(proj))
					|| (depthsVolley != null && depthsVolley.mDepthsVolley.contains(proj));

				if (isVolley) {
					return;
				}

				Snowball snowball = (Snowball) player.getWorld().spawnEntity(proj.getLocation(), EntityType.SNOWBALL);
				snowball.setShooter(player);
				snowball.setVelocity(proj.getVelocity());
				DamageListener.addProjectileItemStats(snowball, player);
				ItemUtils.setSnowballItem(snowball, oldSnowball.getItem());
				player.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.5f, 0.5f);
				AbilityManager.getManager().playerShotProjectileEvent(player, snowball);

				player.setCooldown(Material.SNOWBALL, (int) (20 / value));
				event.setCancelled(true);
			}
		}
	}
}
