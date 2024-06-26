package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLaser;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellChangeFloor;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.SpellFireball;
import com.playmonumenta.plugins.bosses.spells.SpellKnockAway;
import com.playmonumenta.plugins.bosses.spells.SpellMinionResist;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public final class Azacor extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_azacor";
	public static final String LASER_SLOWNESS_TAG = "AzacorLaserSlowness";
	public static final String FIRST_MSG = "I took his offer and I remain here. Even assassins cannot make me face death! What makes you think you can fare better?";
	public static final String SECOND_MSG = "I will bask in their screams!";
	public static final String THIRD_MSG = "Foolish mortals! Your efforts mean nothing. You cannot stop me. You will fall, just like the rest.";
	public static final String FOURTH_MSG = "I wield powers beyond your comprehension. I will not be defeated by insects like you!";
	public final int mLaserSlownessDuration = 110;
	public final double mLaserSlownessPotency = -0.75;
	public static final int detectionRange = 50;

	public Azacor(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mBoss.setRemoveWhenFarAway(false);
		mBoss.addScoreboardTag("Boss");

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellChangeFloor(plugin, mBoss, spawnLoc, 24, 3, Material.LAVA, 400),
			new SpellFireball(plugin, boss, detectionRange, 40, 1, 160, 2.0f, true, false,
				// Launch effect
				(Location loc) -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.0f);
					new PartialParticle(Particle.VILLAGER_ANGRY, loc, 10, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(boss);
				}),
			new SpellBaseLaser(plugin, boss, detectionRange, 100, false, false, 160,
				// Tick action per player
				(LivingEntity target, int ticks, boolean blocked) -> {
					if (ticks % 8 == 0) {
						target.getWorld().playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 0.75f, 0.5f + (ticks / 80f) * 1.5f);
					} else if (ticks % 8 == 2) {
						boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 1.0f, 0.5f + (ticks / 80f) * 1.5f);
					} else if (ticks % 8 == 4) {
						target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.75f, 0.5f + (ticks / 100f) * 1.5f);
					} else if (ticks % 8 == 6) {
						boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 1.0f, 0.5f + (ticks / 100f) * 1.5f);
					}
					if (ticks == 0) {
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(boss, LASER_SLOWNESS_TAG,
							new PercentSpeed(mLaserSlownessDuration, mLaserSlownessPotency, LASER_SLOWNESS_TAG));
					}
				},
				// Particles generated by the laser
				(Location loc) -> {
					new PartialParticle(Particle.SMOKE_LARGE, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
					new PartialParticle(Particle.FLAME, loc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(boss);
				},
				// Damage generated at the end of the attack
				(LivingEntity target, Location loc, boolean blocked) -> {
					loc.getWorld().playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1f, 1.5f);
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 30, 0, 0, 0, 0.3).minimumCount(1).spawnAsEntityActive(boss);
					if (!blocked) {
						BossUtils.bossDamagePercent(mBoss, target, 0.75, mBoss.getLocation());
						if (target instanceof Player player && BossUtils.bossDamageBlocked(player, mBoss.getLocation())) {
							BossUtils.bossDamagePercent(mBoss, target, 0.25);
						}
						target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 2));
					} else {
						EntityUtils.summonEntityAt(loc, EntityType.PRIMED_TNT, "{Fuse:0}");
					}
				}),
			new SpellKnockAway(plugin, boss, 5, 20, 1.5f)
		));
		List<Spell> passiveSpells = Arrays.asList(
			new SpellBlockBreak(mBoss),
			// Teleport the boss to spawnLoc if he gets too far away from where he spawned
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> spawnLoc.distance(b.getLocation()) > 80),
			// Teleport the boss to spawnLoc if he is stuck in bedrock
			new SpellConditionalTeleport(mBoss, spawnLoc, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
				                                                   b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
				                                                   b.getLocation().getBlock().getType() == Material.LAVA),
			new SpellMinionResist(mBoss, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 30, 2), detectionRange, 5,
				(entity) -> (entity.getType().equals(EntityType.WITHER_SKELETON) || entity.getType().equals(EntityType.SKELETON)) && entity.getScoreboardTags().contains("azacor_minion"))
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mSpawnLoc, detectionRange);
		events.put(100, (mBoss) -> {
			PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange).sendMessage(Component.text(FIRST_MSG, NamedTextColor.DARK_RED));
			randomMinion();
			if (playerCount >= 3) {
				randomMinion();
			}
		});
		events.put(75, (mBoss) -> {
			PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange).sendMessage(Component.text(SECOND_MSG, NamedTextColor.DARK_RED));
			randomMinion();
			if (playerCount >= 3) {
				randomMinion();
			}
		});
		events.put(50, (mBoss) -> {
			PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange).sendMessage(Component.text(THIRD_MSG, NamedTextColor.DARK_RED));
			randomMinion();
			if (playerCount >= 3) {
				randomMinion();
			}
		});
		events.put(25, (mBoss) -> {
			PlayerUtils.nearbyPlayersAudience(mSpawnLoc, detectionRange).sendMessage(Component.text(FOURTH_MSG, NamedTextColor.DARK_RED));
			randomMinion();
			if (playerCount >= 3) {
				randomMinion();
			}
		});
		BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.RED, BarStyle.SEGMENTED_10, events);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	private void randomMinion() {
		randomMinion(mSpawnLoc, (100.0 + BossUtils.getPlayersInRangeForHealthScaling(mSpawnLoc, detectionRange) * 75.0) * 1.1);
	}

	private static void randomMinion(Location loc, double eliteHealth) {
		int rand = FastUtils.RANDOM.nextInt(4);

		switch (rand) {
			case 0 -> {
				LivingEntity elite = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, "SarintultheUnseen"));
				EntityUtils.setAttributeBase(elite, Attribute.GENERIC_MAX_HEALTH, eliteHealth);
				elite.setHealth(eliteHealth);
			}
			case 1 -> {
				LivingEntity elite = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, "ZirinkelthePrecise"));
				EntityUtils.setAttributeBase(elite, Attribute.GENERIC_MAX_HEALTH, eliteHealth);
				elite.setHealth(eliteHealth);
			}
			case 2 -> {
				LivingEntity elite = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, "KazarthuntheMighty"));
				EntityUtils.setAttributeBase(elite, Attribute.GENERIC_MAX_HEALTH, 1.5 * eliteHealth);
				elite.setHealth(1.5 * eliteHealth);
			}
			default -> {
				LivingEntity elite = Objects.requireNonNull((LivingEntity) LibraryOfSoulsIntegration.summon(loc, "VerkantaltheCunning"));
				EntityUtils.setAttributeBase(elite, Attribute.GENERIC_MAX_HEALTH, 0.75 * eliteHealth);
				elite.setHealth(0.75 * eliteHealth);
			}
		}
	}

	@Override
	public void init() {
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		int hpDelta = 1250;
		double finalHp = hpDelta * BossUtils.healthScalingCoef(playerCount, 0.6, 0.35);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, finalHp);
		mBoss.setHealth(finalHp);

		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, Component.text("Azacor", NamedTextColor.DARK_GRAY), Component.text("The Dark Summoner", NamedTextColor.GRAY));
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, false, true, true));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.7f);
		}
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true);

		BossUtils.endBossFightEffects(players);
		for (Player player : players) {
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 5.0f, 0.8f);
			player.sendMessage(Component.text("No... it's not possible... I was promised...", NamedTextColor.DARK_RED));
		}
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
	}
}
