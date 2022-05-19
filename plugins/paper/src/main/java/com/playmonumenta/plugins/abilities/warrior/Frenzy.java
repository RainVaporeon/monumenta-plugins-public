package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;



public class Frenzy extends Ability {

	private static final String PERCENT_ATTACK_SPEED_EFFECT_NAME = "FrenzyPercentAttackSpeedEffect";
	private static final int DURATION = 5 * 20;
	private static final double PERCENT_ATTACK_SPEED_EFFECT_1 = 0.3;
	private static final double PERCENT_ATTACK_SPEED_EFFECT_2 = 0.4;
	private static final String PERCENT_SPEED_EFFECT_NAME = "FrenzyPercentSpeedEffect";
	private static final double PERCENT_SPEED = 0.2;
	private static final double DAMAGE_BONUS = 0.2;

	public static final String CHARM_DURATION = "Frenzy Duration";
	public static final String CHARM_ATTACK_SPEED = "Frenzy Attack Speed";
	public static final String CHARM_SPEED = "Frenzy Speed";
	public static final String CHARM_BONUS_DAMAGE = "Frenzy Bonus Damage";

	private final double mPercentAttackSpeedEffect;
	private int mLastKillTick;
	private int mDuration;

	public Frenzy(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Frenzy");
		mInfo.mScoreboardId = "Frenzy";
		mInfo.mShorthandName = "Fnz";
		mInfo.mDescriptions.add("Gain +30% Attack Speed for 5 seconds after killing a mob.");
		mInfo.mDescriptions.add("Gain +40% Attack Speed and +20% Speed for 5 seconds after killing a mob.");
		mInfo.mDescriptions.add("Additionally, your next melee damage after getting a kill, within 5 seconds, deals 20% extra damage.");
		mDisplayItem = new ItemStack(Material.FEATHER, 1);

		mPercentAttackSpeedEffect = (isLevelOne() ? PERCENT_ATTACK_SPEED_EFFECT_1 : PERCENT_ATTACK_SPEED_EFFECT_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ATTACK_SPEED);
		mDuration = DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);
		mLastKillTick = -1;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (mPlayer == null) {
			return;
		}

		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_ATTACK_SPEED_EFFECT_NAME,
				new PercentAttackSpeed(mDuration, mPercentAttackSpeedEffect, PERCENT_ATTACK_SPEED_EFFECT_NAME));

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME,
					new PercentSpeed(mDuration, PERCENT_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED), PERCENT_SPEED_EFFECT_NAME));
		}

		if (isEnhanced()) {
			mLastKillTick = mPlayer.getTicksLived();
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if (mLastKillTick > 0 && mLastKillTick + mDuration > mPlayer.getTicksLived() && (type == DamageEvent.DamageType.MELEE || type == DamageEvent.DamageType.MELEE_SKILL)) {
			mLastKillTick = -1;
			event.setDamage(event.getDamage() * (1 + DAMAGE_BONUS + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS_DAMAGE)));
		}
		return true;
	}
}
