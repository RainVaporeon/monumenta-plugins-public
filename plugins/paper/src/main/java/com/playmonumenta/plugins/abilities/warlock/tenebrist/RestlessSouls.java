package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.abilities.RestlessSoulsBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class RestlessSouls extends Ability {
	private static final int DAMAGE_1 = 8;
	private static final int DAMAGE_2 = 12;
	private static final int SILENCE_DURATION_1 = 2 * 20;
	private static final int SILENCE_DURATION_2 = 3 * 20;
	private static final int VEX_DURATION_1 = 10 * 20;
	private static final int VEX_DURATION_2 = 15 * 20;
	private static final int VEX_CAP_1 = 3;
	private static final int VEX_CAP_2 = 5;
	private static final int DEBUFF_DURATION = 4 * 20;
	public static final String VEX_NAME = "RestlessSoul";
	private static final int TICK_INTERVAL = 5;
	private static final int DETECTION_RANGE = 24;
	private static final int RANGE = 8;

	private final boolean mLevel;
	private final int mDamage;
	private final int mSilenceTime;
	private final int mVexTime;
	private final int mVexCap;
	private @Nullable Vex mVex;
	private List<Vex> mVexList = new ArrayList<Vex>();
	private PartialParticle mParticle1;
	private PartialParticle mParticle2;

	public RestlessSouls(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Restless Souls");
		//TODO add scoreboard "RestlessSouls"
		mInfo.mScoreboardId = "RestlessSouls";
		mInfo.mShorthandName = "RS";
		mInfo.mDescriptions.add("Whenever an enemy dies within " + RANGE + " blocks of you, a glowing invisible invulnerable vex spawns. The vex targets your enemies and possesses them, dealing " + DAMAGE_1 + " damage and silences the target for " + SILENCE_DURATION_1 / 20 + " seconds. Vex count is capped at " + VEX_CAP_1 + " and each lasts for " + VEX_DURATION_1 / 20 + " seconds. Each vex can only possess 1 enemy. Enemies killed by the vex will not spawn additional vexes.");
		mInfo.mDescriptions.add("Damage is increased to " + DAMAGE_2 + " and silence duration increased to " + SILENCE_DURATION_2 + " seconds. Maximum vex count increased to " + VEX_CAP_2 + " and each vex lasts for " + VEX_DURATION_2 + " seconds. Additionally, the possessed mob is inflicted with a level 1 debuff of the corresponding active skill that is on cooldown for " + DEBUFF_DURATION + " seconds. Grasping Claws > 10% Slowness. Level 1 Choleric Flames > Set mobs on Fire. Level 2 Choleric Flames > Hunger. Melancholic Lament > 10% Weaken. Withering Gaze > Wither. Haunting Shades > 5% Vulnerability.");
		mInfo.mLinkedSpell = ClassAbility.RESTLESS_SOULS;
		mDisplayItem = new ItemStack(Material.VEX_SPAWN_EGG, 1);

		boolean isLevelOne = getAbilityScore() == 1;
		mLevel = isLevelOne;
		mDamage = isLevelOne ? DAMAGE_1 : DAMAGE_2;
		mSilenceTime = isLevelOne ? SILENCE_DURATION_1 : SILENCE_DURATION_2;
		mVexTime = isLevelOne ? VEX_DURATION_1 : VEX_DURATION_2;
		mVexCap = isLevelOne ? VEX_CAP_1 : VEX_CAP_2;
	}

	@Override
	public double entityDeathRadius() {
		return RANGE;
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mPlayer == null) {
			return;
		}

		World world = mPlayer.getWorld();
		Location summonLoc = event.getEntity().getLocation();


		if (!summonLoc.isChunkLoaded()) {
			// mob is standing somewhere that's not loaded, abort
			return;
		}

		if (mVexList != null) {
			mVexList.removeIf(e -> !e.isValid() || e.isDead());
		}

		if (event.getEntity().getScoreboardTags().contains("TeneGhost")) {
			return;
		}

		if (mVexList.size() < mVexCap) {
			mVex = (Vex) LibraryOfSoulsIntegration.summon(summonLoc.clone(), VEX_NAME);
			if (mVex == null) {
				return;
			}
			mVexList.add(mVex);

			BossManager bossManager = BossManager.getInstance();
			if (bossManager != null) {
				List<BossAbilityGroup> abilities = BossManager.getInstance().getAbilities(mVex);
				if (abilities != null) {
					for (BossAbilityGroup ability : abilities) {
						if (ability instanceof RestlessSoulsBoss restlessSoulsBoss) {
							FixedMetadataValue playerItemStats = new FixedMetadataValue(mPlugin, new ItemStatManager.PlayerItemStats(mPlugin.mItemStatManager.getPlayerItemStats(mPlayer)));
							restlessSoulsBoss.spawn(mPlayer, mDamage, mSilenceTime, DEBUFF_DURATION, mLevel, playerItemStats);
							break;
						}
					}
				}
			}

			mParticle1 = new PartialParticle(Particle.SOUL, mVex.getLocation().add(0, 0.25, 0), 1, 0.2, 0.2, 0.2, 0.01).spawnAsPlayer(mPlayer);
			mParticle2 = new PartialParticle(Particle.SOUL_FIRE_FLAME, mVex.getLocation().add(0, 0.25, 0), 1, 0.2, 0.2, 0.2, 0.01).spawnAsPlayer(mPlayer);
			new BukkitRunnable() {
				int mTicksElapsed = 0;
				LivingEntity mTarget;
				Vex mBoss = mVex;
				@Override
				public void run() {

					Location loc = mBoss.getLocation().add(0, 0.25, 0);
					mParticle1.location(loc).spawnAsPlayer(mPlayer);
					mParticle2.location(loc).spawnAsPlayer(mPlayer);

					boolean isOutOfTime = mTicksElapsed >= mVexTime;
					if (isOutOfTime || !mBoss.isValid()) {
						if (isOutOfTime && mBoss.isValid()) {
							Location vexLoc = mBoss.getLocation();

							world.playSound(vexLoc, Sound.ENTITY_VEX_DEATH, 1.5f, 1.0f);
							world.spawnParticle(Particle.SOUL, vexLoc, 20, 0.2, 0.2, 0.2);
						}
						if (mTarget != null) {
							mTarget.removePotionEffect(PotionEffectType.GLOWING);
						}
						if (mBoss != null) {
							mBoss.remove();
						}
						this.cancel();
						return;
					}

					if (mTarget != null && !mTarget.isDead() && mTarget.getHealth() > 0) {
						mBoss.setTarget(mTarget);
					}

					LivingEntity target = mBoss.getTarget();
					// re-aggro
					if ((target == null || target.isDead() || target.getHealth() <= 0) && mTicksElapsed >= TICK_INTERVAL * 2) {
						Location pLoc = mPlayer.getLocation();
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(pLoc, DETECTION_RANGE, mBoss);
						if (nearbyMobs != null && nearbyMobs.size() > 0) {
							nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
							Collections.shuffle(nearbyMobs);
							// check mob count again after removal of vexes
							if (nearbyMobs.size() > 0) {
								LivingEntity randomMob = nearbyMobs.get(0);
								if (randomMob != null) {
									mBoss.setTarget(randomMob);
									world.playSound(mBoss.getLocation(), Sound.ENTITY_VEX_AMBIENT, 1.5f, 1.0f);
								}
							}
						}
					}
					mTicksElapsed += TICK_INTERVAL;
				}
			}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);
		}
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (mVexList != null) {
			mVexList.removeIf(e -> !e.isValid() || e.isDead());
			if (mVexList.size() > 0) {
				for (Vex v : mVexList) {
					v.remove();
				}
				mVexList.clear();
			}
		}
	}
}