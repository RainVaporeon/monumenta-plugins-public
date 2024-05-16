package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.MelancholicLamentCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class MelancholicLament extends Ability {

	private static final int DURATION = 20 * 8;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.3;
	private static final int SILENCE_WINDOW = 4 * 20;
	private static final int SILENCE_RADIUS = 3;
	private static final int SILENCE_DURATION = 3 * 20;
	private static final int COOLDOWN = 20 * 16;
	private static final int RADIUS = 8;
	private static final int CLEANSE_REDUCTION = 20 * 10;
	private static final int ENHANCE_RADIUS = 16;
	private static final double ENHANCE_DAMAGE = .025;
	private static final String ENHANCE_EFFECT_NAME = "LamentDamage";
	private static final String ENHANCE_EFFECT_PARTICLE_NAME = "LamentParticle";
	private static final int ENHANCE_EFFECT_DURATION = 20;
	private static final int ENHANCE_MAX_MOBS = 6;
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(DamageEvent.DamageType.MELEE);
	public static final String CHARM_RADIUS = "Melancholic Lament Radius";
	public static final String CHARM_COOLDOWN = "Melancholic Lament Cooldown";
	public static final String CHARM_WEAKNESS = "Melancholic Lament Weakness Amplifier";
	public static final String CHARM_SILENCE_RADIUS = "Melancholic Lament Silence Radius";
	public static final String CHARM_SILENCE_DURATION = "Melancholic Lament Silence Duration";
	public static final String CHARM_RECOVERY = "Melancholic Lament Negative Effect Recovery";
	public static final String CHARM_ENHANCE_DAMAGE = "Melancholic Lament Enhancement Damage Modifier";
	public static final String CHARM_ENHANCE_DURATION = "Melancholic Lament Enhancement Duration";

	public static final AbilityInfo<MelancholicLament> INFO =
		new AbilityInfo<>(MelancholicLament.class, "Melancholic Lament", MelancholicLament::new)
			.linkedSpell(ClassAbility.MELANCHOLIC_LAMENT)
			.scoreboardId("Melancholic")
			.shorthandName("MLa")
			.actionBarColor(TextColor.color(235, 235, 224))
			.descriptions(
				("Press the swap key while sneaking and holding a scythe to recite a haunting song, " +
					"causing all mobs within %s blocks to target the user and afflicting them with %s%% Weaken for %s seconds. Cooldown: %ss.")
					.formatted(RADIUS, StringUtils.multiplierToPercentage(WEAKEN_EFFECT_1), StringUtils.ticksToSeconds(DURATION), StringUtils.ticksToSeconds(COOLDOWN)),
				("Increase the Weaken to %s%% and decrease the duration of all negative potion effects on players in the radius by %ss. " +
					"Your next melee scythe attack within the next %s seconds will silence all mobs in a %s block radius for %ss.")
					.formatted(StringUtils.multiplierToPercentage(WEAKEN_EFFECT_2), StringUtils.ticksToSeconds(CLEANSE_REDUCTION), StringUtils.ticksToSeconds(SILENCE_WINDOW), SILENCE_RADIUS, StringUtils.ticksToSeconds(SILENCE_DURATION)),
				"For %ss after casting this ability, you and your allies in a %s block radius gain +%s%% melee damage for each mob in that same radius targeting you (capped at %s mobs)."
					.formatted(StringUtils.ticksToSeconds(DURATION), ENHANCE_RADIUS, StringUtils.multiplierToPercentage(ENHANCE_DAMAGE), ENHANCE_MAX_MOBS))
			.simpleDescription("Weaken nearby mobs and force them to target you.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MelancholicLament::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(Material.GHAST_TEAR);

	private final double mRadius;
	private final double mWeakenEffect;
	private final double mSilenceRadius;
	private final int mSilenceDuration;

	private @Nullable BukkitRunnable mSilenceRunnable;

	private final MelancholicLamentCS mCosmetic;

	public MelancholicLament(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKNESS) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
		mSilenceRadius = CharmManager.getRadius(player, CHARM_SILENCE_RADIUS, SILENCE_RADIUS);
		mSilenceDuration = CharmManager.getDuration(player, CHARM_SILENCE_DURATION, SILENCE_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new MelancholicLamentCS());

		mSilenceRunnable = null;
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		mCosmetic.onCast(mPlayer, world, loc, mRadius);

		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			EntityUtils.applyWeaken(mPlugin, DURATION, mWeakenEffect, mob);
			EntityUtils.applyTaunt(mob, mPlayer);
			mPlugin.mEffectManager.addEffect(mob, "MelancholicLamentParticles", new Aesthetics(DURATION,
				(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.debuffTick(mob),
				(entity) -> { })
			);

			mCosmetic.onWeakenApply(mPlayer, mob);
		}

		if (isLevelTwo()) {
			if (mSilenceRunnable != null) {
				mSilenceRunnable.cancel();
			}
			mSilenceRunnable = new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					mCosmetic.silenceReadyTick(mPlayer);

					mTicks += 5;
					if (mTicks >= SILENCE_WINDOW) {
						this.cancel();
						mSilenceRunnable = null;
					}
				}
			};
			mSilenceRunnable.runTaskTimer(mPlugin, 0, 5);
		}

		if (isEnhanced()) {

			cancelOnDeath(new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {

					if (!mPlayer.isOnline() || mPlayer.isDead()) {
						this.cancel();
						return;
					}

					Hitbox enhanceHitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), ENHANCE_RADIUS);
					int numTargeting = (int) enhanceHitbox
						.getHitMobs().stream()
						.filter(entity -> entity instanceof Mob mob && mob.getTarget() != null && mob.getTarget().equals(mPlayer))
						.limit(ENHANCE_MAX_MOBS)
						.count();
					for (Player player : enhanceHitbox.getHitPlayers(true)) {
						mPlugin.mEffectManager.addEffect(player, ENHANCE_EFFECT_NAME, new PercentDamageDealt(ENHANCE_EFFECT_DURATION, (ENHANCE_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_DAMAGE)) * numTargeting, AFFECTED_DAMAGE_TYPES));
						mPlugin.mEffectManager.addEffect(player, ENHANCE_EFFECT_PARTICLE_NAME, new Aesthetics(ENHANCE_EFFECT_DURATION,
							(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.enhancementTick(player, mPlayer, fourHertz, twoHertz, oneHertz),
							(entity) -> { })
						);
					}

					mTicks += 1;
					if (mTicks > CharmManager.getDuration(mPlayer, CHARM_ENHANCE_DURATION, DURATION)) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1));

		}

		if (isLevelTwo()) {
			int reductionTime = CharmManager.getDuration(mPlayer, CHARM_RECOVERY, CLEANSE_REDUCTION);
			for (Player player : hitbox.getHitPlayers(true)) {
				mCosmetic.onCleanse(player, mPlayer);
				for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
					PotionEffect effect = player.getPotionEffect(effectType);
					if (effect != null) {
						player.removePotionEffect(effectType);
						if (effect.getDuration() - reductionTime > 0) {
							player.addPotionEffect(new PotionEffect(effectType, effect.getDuration() - reductionTime, effect.getAmplifier()));
						}
					}
				}
				EntityUtils.setWeakenTicks(mPlugin, player, Math.max(0, EntityUtils.getWeakenTicks(mPlugin, player) - reductionTime));
				EntityUtils.setSlowTicks(mPlugin, player, Math.max(0, EntityUtils.getSlowTicks(mPlugin, player) - reductionTime));
			}
		}
		putOnCooldown();
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())
			&& mSilenceRunnable != null && !mSilenceRunnable.isCancelled()) {
			mSilenceRunnable.cancel();
			mSilenceRunnable = null;

			for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), mSilenceRadius)) {
				EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
			}

			mCosmetic.onSilenceHit(mPlayer, enemy, mSilenceRadius);
		}
		return false;
	}


}
