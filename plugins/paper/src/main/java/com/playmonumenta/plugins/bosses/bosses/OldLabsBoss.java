package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBombToss;
import com.playmonumenta.plugins.bosses.spells.oldslabsbos.SpellBash;
import com.playmonumenta.plugins.bosses.spells.oldslabsbos.SpellWhirlwind;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class OldLabsBoss extends SerializedLocationBossAbilityGroup {
	public static final int detectionRange = 32;
	public static final String identityTag = "boss_oldlabs";

	private static final String SUMMON_SLOWNESS_SRC = "SummonOldLabsBossSlowness";
	private static final int SUMMON_SLOWNESS_DURATION = 20 * 2;
	private static final double SUMMON_SLOWNESS_POTENCY = -0.75;
	private static final String[] mDialog = new String[] {
		"Well, this is very peculiar...",
		"The rats causing a ruckus down here are mere commoners? How feeble are those bandits?",
		"The aristocratic code of ethics demands I take pity on you, but why shouldn't I prune you here where no one will know?",
		"Your heroics end here! Have at ye, commoners!"
	};

	public OldLabsBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		this(plugin, boss, spawnLoc, endLoc, true);
	}

	public OldLabsBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc, boolean newBoss) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		if (!newBoss) {
			resumeBossFight(plugin, boss);
		} else {
			mBoss.setGravity(false);
			mBoss.setInvulnerable(true);
			mBoss.setAI(false);
			Location tempLoc = mSpawnLoc.clone();
			tempLoc.setY(300);
			mBoss.teleport(tempLoc);

			for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
				player.playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, SoundCategory.HOSTILE, 5.0f, 0.6f);
			}
			new BukkitRunnable() {
				int mIdx = 0;

				@Override
				public void run() {
					String line = mDialog[mIdx];

					if (mIdx < 3) {
						PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange)
							.sendMessage(Component.text("", NamedTextColor.WHITE)
								.append(Component.text("[???] ", NamedTextColor.GOLD))
								.append(Component.text(line)));
						mIdx += 1;
					} else {
						this.cancel();
						Location loc = mSpawnLoc.clone().add(0, 1, 0);
						PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange)
							.sendMessage(Component.text("", NamedTextColor.WHITE)
								.append(Component.text("[Elcard the Ignoble] ", NamedTextColor.GOLD))
								.append(Component.text(line)));

						mBoss.teleport(mSpawnLoc);
						mBoss.setAI(true);
						mBoss.setInvulnerable(false);
						mBoss.setGravity(true);

						new PartialParticle(Particle.CLOUD, loc, 10, 0.2, 0.45, 0.2, 0.125).spawnAsEntityActive(boss);
						new PartialParticle(Particle.SMOKE_NORMAL, loc, 75, 0.2, 0.45, 0.2, 0.2).spawnAsEntityActive(boss);
						new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 35, 0.2, 0.45, 0.2, 0.15).spawnAsEntityActive(boss);
						for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
							MessagingUtils.sendBoldTitle(player, Component.text("Elcard", NamedTextColor.GOLD), Component.text("The Ignoble", NamedTextColor.RED));
							player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 10, 1.65f);
							player.playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, SoundCategory.HOSTILE, 10, 0.6f);
						}

						resumeBossFight(plugin, boss);
					}
				}

			}.runTaskTimer(plugin, 0, 20 * 5);
		}
	}

	/* This is called either when the boss chunk loads OR when he is first created */
	private void resumeBossFight(Plugin plugin, LivingEntity boss) {
		SpellManager phase1Spells = new SpellManager(Arrays.asList(
			new SpellBombToss(plugin, mBoss, 20, 2, 1, 100, false, true),
			new SpellBash(plugin, mBoss)
		));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
			new SpellWhirlwind(plugin, mBoss),
			new SpellBash(plugin, mBoss)
		));

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(75, mBoss -> PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange)
			.sendMessage(Component.text("", NamedTextColor.WHITE)
				.append(Component.text("[Elcard the Ignoble] ", NamedTextColor.GOLD))
				.append(Component.text("Do not interfere with my affairs! I will see that crown-head fall and assert myself as King!"))));
		events.put(50, mBoss -> {
			changePhase(phase2Spells, Collections.emptyList(), null);
			PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange)
				.sendMessage(Component.text("", NamedTextColor.WHITE)
					.append(Component.text("[Elcard the Ignoble] ", NamedTextColor.GOLD))
					.append(Component.text("Agh! You think you're so strong? Let me show you true swordsmanship!")));
		});

		events.put(35, mBoss -> PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange)
			.sendMessage(Component.text("", NamedTextColor.WHITE)
				.append(Component.text("[Elcard the Ignoble] ", NamedTextColor.GOLD))
				.append(Component.text("Even if you stop this, city leeches like you will never step foot outside of Sierhaven! Where you do you think you'll go? Back to the slums!?"))));

		events.put(20, mBoss -> {
			PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange)
				.sendMessage(Component.text("", NamedTextColor.WHITE)
					.append(Component.text("[Elcard the Ignoble] ", NamedTextColor.GOLD))
					.append(Component.text("Useless sluggards! Kill these commoners where they stand or we all face the gallows!")));
			Location spawnLoc = mSpawnLoc.clone().add(-1, -1, 13);
			try {
				new PartialParticle(Particle.SMOKE_LARGE, spawnLoc, 15, 0.2, 0.45, 0.2, 0.2).spawnAsEntityActive(boss);
				Entity mob = LibraryOfSoulsIntegration.summon(spawnLoc, "RebelSoldier");
				if (mob instanceof LivingEntity) {
					Plugin.getInstance().mEffectManager.addEffect(mob, SUMMON_SLOWNESS_SRC,
						new PercentSpeed(SUMMON_SLOWNESS_DURATION, SUMMON_SLOWNESS_POTENCY, SUMMON_SLOWNESS_SRC));
				}

				spawnLoc = spawnLoc.add(2, 0, 0);
				new PartialParticle(Particle.SMOKE_LARGE, spawnLoc, 15, 0.2, 0.45, 0.2, 0.2).spawnAsEntityActive(boss);
				mob = LibraryOfSoulsIntegration.summon(spawnLoc, "RebelSlinger");
				if (mob instanceof LivingEntity) {
					Plugin.getInstance().mEffectManager.addEffect(mob, SUMMON_SLOWNESS_SRC,
						new PercentSpeed(SUMMON_SLOWNESS_DURATION, SUMMON_SLOWNESS_POTENCY, SUMMON_SLOWNESS_SRC));
				}
			} catch (Exception ex) {
				MMLog.warning("Failed to spawn labs boss summons");
				ex.printStackTrace();
			}
		});

		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_10, events);
		constructBoss(phase1Spells, Collections.emptyList(), detectionRange, bossBar);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 100.0f, 0.8f);
			player.sendMessage(Component.text("", NamedTextColor.WHITE)
				.append(Component.text("[Elcard The Ignoble] ", NamedTextColor.GOLD))
				.append(Component.text("You are no commoner... Who... are you...?")));
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mSpawnLoc, detectionRange);
		int hpDelta = 160;
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2;
			playerCount--;
		}
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		mBoss.setHealth(bossTargetHp);
	}
}
