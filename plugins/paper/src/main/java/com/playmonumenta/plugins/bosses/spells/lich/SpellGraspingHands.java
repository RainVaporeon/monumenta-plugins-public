package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
Grasping Hands - Pools of "hands" appear under ½  players in a 5 block radius. Players inside
it after 1 second(s) take 10% max health damage every 0.5s second(s) and are given slowness 3
and negative jump boost for 4 seconds. “Minions” in the pools are healed for 5% max hp every 0.5 seconds
while in a pool.

-maybe change to spoopy's house of jump scare boss hand attack
 */
public class SpellGraspingHands extends Spell {
	private static final String SPELL_NAME = "Grasping Hands";
	private static final String SLOWNESS_SRC = "GraspingHandsSlowness";
	private static final int CAP = 7;
	private static final double DURATION = 20 * 7;
	private static final int HEAL_CAP = 75;
	private static final Particle.DustOptions GRASPING_HANDS_COLOR = new Particle.DustOptions(Color.fromRGB(0, 135, 96), 1.65f);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final ThreadLocalRandom mRand = ThreadLocalRandom.current();
	private final ChargeUpManager mChargeUp;
	private boolean mCanRun = true;

	public SpellGraspingHands(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mChargeUp = Lich.defaultChargeUp(mBoss, 110, "Channeling " + SPELL_NAME + "...");
	}

	@Override
	public void run() {
		mCanRun = false;
		World world = mBoss.getWorld();
		List<Player> players = Lich.playersInRange(mBoss.getLocation(), 50, true);
		players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p));
		List<Player> targets = new ArrayList<>();
		if (players.size() <= 2) {
			targets = players;
		} else {
			int cap = (int) Math.min(CAP, Math.ceil(1.0 * players.size() / 3));
			for (int i = 0; i < cap; i++) {
				Player player = players.get(mRand.nextInt(players.size()));
				if (targets.contains(player)) {
					cap++;
				} else {
					targets.add(player);
				}
			}
		}
		//boss bar
		BukkitRunnable runA = new BukkitRunnable() {

			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.setTitle(Component.text("Casting " + SPELL_NAME + "...", NamedTextColor.YELLOW));
					mChargeUp.setColor(BossBar.Color.RED);
					BukkitRunnable runB = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							mT++;
							double progress = 1.0d - mT / DURATION;
							if (progress >= 0) {
								mChargeUp.setProgress(progress);
							}
							if (progress < 0 || Lich.phase3over()) {
								this.cancel();
								mChargeUp.reset();
								mChargeUp.setTitle(Component.text("Charging " + SPELL_NAME + "...", NamedTextColor.YELLOW));
							}
						}

					};
					runB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runB);
				}
			}

		};
		runA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runA);

		for (Player player : targets) {
			player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, SoundCategory.HOSTILE, 1, 0.75f);
			player.sendMessage(Component.text("A pool forms under you.", NamedTextColor.AQUA));
			world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1, 0.5f);
			world.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1, 0.75f);

			PPCircle indicator = new PPCircle(Particle.REDSTONE, player.getLocation(), 0).count(8).delta(0.25, 0.1, 0.25).data(GRASPING_HANDS_COLOR);
			PPCircle indicator2 = new PPCircle(Particle.SMOKE_NORMAL, player.getLocation(), 0).count(6).delta(0.2, 0, 0.2);
			PPCircle indicator3 = new PPCircle(Particle.DRAGON_BREATH, player.getLocation(), 0).count(4).delta(0.25, 0.1, 0.25).extraRange(0.01, 0.05);
			BukkitRunnable runC = new BukkitRunnable() {
				int mT = 0;
				Location mLoc = player.getLocation();
				final double mRadius = 5;

				@Override
				public void run() {
					mT++;
					if (mT <= 20 * 4 && player.getLocation().getY() > 27) {
						mLoc = player.getLocation();
					}

					indicator.radius(mRadius).location(mLoc).spawnAsBoss();
					indicator2.radius(mRadius).location(mLoc).spawnAsBoss();

					if (mT >= 20 * 5.5) {
						world.playSound(player.getLocation(), Sound.ENTITY_SPIDER_STEP, SoundCategory.HOSTILE, 1, 0.75f);
						world.playSound(player.getLocation(), Sound.ENTITY_SPIDER_STEP, SoundCategory.HOSTILE, 1, 0.5f);
						world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1, 0.75f);
						BukkitRunnable runD = new BukkitRunnable() {
							int mT = 0;

							@Override
							public void run() {
								mT++;
								if (mT % 10 == 0) {
									//damage the player
									for (Player p : Lich.playersInRange(mLoc, 5.0, true)) {
										Vector v = p.getVelocity();
										BossUtils.bossDamagePercent(mBoss, p, 0.15, SPELL_NAME);
										p.setVelocity(v);
										com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p,
											SLOWNESS_SRC, new PercentSpeed(20 * 4, -0.5, SLOWNESS_SRC));
										p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 20 * 4, -4));
									}
									//heal mobs except boss
									List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mLoc, 5.0, 3.0, 5.0);
									for (LivingEntity e : mobs) {
										if (!(e.getType() == EntityType.PLAYER) && !e.isDead()) {
											double maxHealth = EntityUtils.getMaxHealth(e);
											double restore = maxHealth * 0.005 + 3;
											if (restore >= HEAL_CAP) {
												restore = HEAL_CAP;
											}
											double newHealth = e.getHealth() + restore;
											e.setHealth(Math.min(newHealth, maxHealth));
										}
									}
								}

								indicator.radius(mRadius).location(mLoc).spawnAsBoss();
								indicator2.radius(mRadius).location(mLoc).spawnAsBoss();

								if (mT % 4 == 0) {
									for (double r = 1; r < mRadius; r++) {
										indicator3.radius(r).location(mLoc).spawnAsBoss();
									}
								}
								if (mT >= DURATION) {
									this.cancel();
									mCanRun = true;
								}
							}

						};
						runD.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runD);
						this.cancel();
					}
				}

			};
			runC.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runC);
		}
	}


	@Override
	public boolean canRun() {
		return mCanRun;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 8;
	}

}
