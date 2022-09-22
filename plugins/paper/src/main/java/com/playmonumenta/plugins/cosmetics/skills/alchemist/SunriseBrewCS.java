package com.playmonumenta.plugins.cosmetics.skills.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SunriseBrewCS extends BezoarCS {
	// Change bezoar item into sun drop style. Depth set: dawnbringer

	public static final String NAME = "Sunrise Brew";
	private static final Particle.DustOptions SUNDROP_COLOR =
		new Particle.DustOptions(Color.fromRGB(252, 255, 196), 0.8f);
	private static final Particle.DustOptions SUNDROP_DRIP_COLOR =
		new Particle.DustOptions(Color.fromRGB(252, 255, 196), 1.25f);
	private static final Particle.DustOptions SUNDROP_RING_COLOR =
		new Particle.DustOptions(Color.fromRGB(252, 255, 196), 0.65f);
	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"The purest drops of sunlight.",
			"Radiant, shining, replenishing.");
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.BEZOAR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.HONEYCOMB_BLOCK;
	}

	@Override
	public ItemStack bezoarItem() {
		ItemStack itemBezoar = new ItemStack(Material.HONEYCOMB_BLOCK);
		ItemUtils.setPlainName(itemBezoar, "Sundrop");
		ItemMeta sundropMeta = itemBezoar.getItemMeta();
		sundropMeta.displayName(Component.text("Sundrop", NamedTextColor.WHITE)
			.decoration(TextDecoration.ITALIC, false));
		itemBezoar.setItemMeta(sundropMeta);
		return itemBezoar;
	}

	@Override
	public void bezoarTick(Player mPlayer, Location loc, int tick) {
		for (int i = 0; i < 2; i++) {
			double radian = FastMath.toRadians((tick * 6) + (i * 180));
			Vector vec = new Vector(FastMath.cos(radian) * 0.65, 0.125, FastMath.sin(radian) * 0.65);
			Location l = loc.clone().add(vec);
			new PartialParticle(Particle.SPELL, l, 1, 0, 0, 0, 0).spawnAsPlayerActive(mPlayer);
		}

		for (int i = 0; i < 2; i++) {
			double radian = FastMath.toRadians((tick * 6) + (i * 180) + 90);
			Vector vec = new Vector(FastMath.cos(radian) * 0.65, 0.125, FastMath.sin(radian) * 0.65);
			Location l = loc.clone().add(vec);
			new PartialParticle(Particle.REDSTONE, l, 2, 0.1, 0.1, 0.1, 0, SUNDROP_COLOR).spawnAsPlayerActive(mPlayer);
		}
	}

	@Override
	public void bezoarPickup(Player mPlayer, Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 1, 1.15f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 1, 1.65f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1, 0.85f);
		world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1, 2f);

		loc.setPitch(0);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 60, 0, 0, 0, 0.75F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.END_ROD, loc, 45, 0, 0, 0, 0.2F).spawnAsPlayerActive(mPlayer);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc.clone().add(0, 0.15, 0), 0, 1, 0, 0, 50, 0.3f,
			true, 0, Particle.END_ROD);
		new BukkitRunnable() {

			double mRadius = 0;
			final Location mL = loc.clone();
			final double RADIUS = 4;
			@Override
			public void run() {

				for (int i = 0; i < 2; i++) {
					mRadius += 0.25;
					for (int degree = 0; degree < 360; degree += 6) {
						double radian = Math.toRadians(degree);
						Vector vec = new Vector(FastMath.cos(radian) * mRadius, 0.75 * FastMath.sin(Math.PI * (mRadius / RADIUS)),
							FastMath.sin(radian) * mRadius);
						Location loc = mL.clone().add(vec);
						new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, SUNDROP_RING_COLOR).spawnAsPlayerActive(mPlayer);
					}
				}

				if (mRadius >= RADIUS) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		createDrip(new Vector(0, 1, 0), loc, mPlayer);
		for (int i = 0; i < 4; i++) {
			createDrip(new Vector(FastUtils.randomDoubleInRange(-1, 1), 1,
				FastUtils.randomDoubleInRange(-1, 1)), loc, mPlayer);
		}
	}

	private void createDrip(Vector dir, Location loc, Player mPlayer) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;
			double mArcCurve = 0;
			Vector mD = dir.clone();

			@Override
			public void run() {
				mT++;

				Location to = mPlayer.getLocation().add(0, 1, 0);

				for (int i = 0; i < 2; i++) {
					if (mT <= 5) {
						mD = dir.clone();
					} else {
						mArcCurve += 0.075;
						mD = dir.clone().add(LocationUtils.getDirectionTo(to, mL).multiply(mArcCurve));
					}

					if (mD.length() > 0.25) {
						mD.normalize().multiply(0.25);
					}

					mL.add(mD);

					new PartialParticle(Particle.REDSTONE, mL, 1, 0, 0, 0, 0, SUNDROP_DRIP_COLOR)
						.spawnAsPlayerActive(mPlayer);

					if (mT > 5 && mL.distance(to) < 0.35) {
						world.playSound(mL, Sound.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundCategory.PLAYERS, 1, 0.75f);
						world.playSound(mL, Sound.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundCategory.PLAYERS, 1, 0.5f);
						new PartialParticle(Particle.CRIT_MAGIC, mL, 15, 0, 0, 0, 0.6F).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.END_ROD, mL, 3, 0, 0, 0, 0.125F).spawnAsPlayerActive(mPlayer);
						this.cancel();
						return;
					}
				}

				if (mT >= 100) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}