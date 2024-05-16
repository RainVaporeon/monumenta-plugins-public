package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/*
Salient of Decay - A straight piercing line appears from the lich at ⅓  targets, after 1 second
anyone within that line takes 3 ailment damage every second for 5 seconds. Players are
also unable to heal for the duration.
 */
public class SpellSalientOfDecay extends Spell {
	private static final String SPELL_NAME = "Salient of Decay";
	private static final int TELL = 40;
	private static final int CAP = 10;
	private static final Particle.DustOptions SALIENT_OF_DECAY_COLOR = new Particle.DustOptions(Color.fromRGB(3, 163, 116), 1f);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final ChargeUpManager mChargeUp;
	private final PartialParticle mDust;
	private final PartialParticle mSmoke;
	private final PartialParticle mWitch;

	public SpellSalientOfDecay(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mChargeUp = new ChargeUpManager(mBoss, TELL, Component.text("Channeling " + SPELL_NAME + "...", NamedTextColor.YELLOW), BossBar.Color.RED, BossBar.Overlay.PROGRESS, 50);
		mDust = new PartialParticle(Particle.REDSTONE, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0, SALIENT_OF_DECAY_COLOR);
		mSmoke = new PartialParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 1, 0.25, 0.25, 0.25, 0.25);
		mWitch = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 2, 0.25, 0.25, 0.25, 1);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 5, 0.4f);
		List<Player> players = Lich.playersInRange(mBoss.getLocation(), 50, true);
		if (SpellDimensionDoor.getShadowed() != null && SpellDimensionDoor.getShadowed().size() > 0) {
			players.removeAll(SpellDimensionDoor.getShadowed());
		}
		List<Player> targets;
		if (players.size() <= 2) {
			targets = players;
		} else {
			Collections.shuffle(players);
			targets = players.subList(0, (int) Math.min(players.size(), Math.max(2, Math.min(CAP, (players.size() + 2) / 3))));
		}

		BukkitRunnable runA = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mChargeUp.nextTick();
				if (Lich.phase3over()) {
					this.cancel();
					mChargeUp.reset();
				} else if (mT >= TELL) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 5, 0.4f);
					this.cancel();
					mChargeUp.reset();
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);

		for (Player player : targets) {
			BukkitRunnable runB = new BukkitRunnable() {
				Vector mDir = LocationUtils.getDirectionTo(player.getLocation().add(0, 1.25, 0), mBoss.getLocation().add(0, 1.25, 0));
				int mT = 0;

				@Override
				public void run() {
					mT += 2;
					Location loc = mBoss.getLocation().add(0, 1.25, 0);
					for (int i = 0; i < 40; i++) {
						loc.add(mDir.clone().multiply(0.75));
						mDust.location(loc).spawnAsBoss();
					}

					if (Lich.phase3over()) {
						this.cancel();
					} else if (mT >= TELL) {
						BoundingBox box = BoundingBox.of(mBoss.getLocation().add(0, 1.25, 0), 0.75, 0.75, 0.75);
						for (int i = 0; i < 40; i++) {
							box.shift(mDir.clone().multiply(0.75));
							Location bLoc = box.getCenter().toLocation(world);
							mWitch.location(bLoc).spawnAsBoss();
							mSmoke.location(bLoc).spawnAsBoss();
							Iterator<Player> it = players.iterator();
							while (it.hasNext()) {
								Player p = it.next();
								if (p.getBoundingBox().overlaps(box)) {
									AbilityUtils.increaseHealingPlayer(p, 20 * 5, -1.0, "Lich");
									p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 5, 0));
									BukkitRunnable runC = new BukkitRunnable() {
										int mT = 0;

										@Override
										public void run() {
											if (SpellDimensionDoor.getShadowed().contains(p)) {
												p.removePotionEffect(PotionEffectType.WITHER);
												this.cancel();
											}
											mT++;
											int ndt = p.getNoDamageTicks();
											p.setNoDamageTicks(0);
											Vector velocity = p.getVelocity();
											//3 dmg every sec, 5 seconds
											DamageUtils.damage(mBoss, p, DamageType.AILMENT, 3, null, false, true, SPELL_NAME);
											p.setVelocity(velocity);
											p.setNoDamageTicks(ndt);
											if (mT >= 5 || p.isDead()) {
												this.cancel();
											}
										}
									};
									runC.runTaskTimer(mPlugin, 0, 20);
									mActiveRunnables.add(runC);
									it.remove();
								}
							}
						}
						this.cancel();
					}
				}

			};
			runB.runTaskTimer(mPlugin, 0, 2);
			mActiveRunnables.add(runB);
		}
	}

	@Override
	public int cooldownTicks() {
		return 20 * 6;
	}

}
