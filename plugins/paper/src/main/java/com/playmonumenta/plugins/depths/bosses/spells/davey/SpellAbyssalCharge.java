package com.playmonumenta.plugins.depths.bosses.spells.davey;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellAbyssalCharge extends Spell {
	private final int mCooldownTicks;

	private final LivingEntity mBoss;
	private static final double DAMAGE_MULTIPLIER = 1.5;
	private static final int DURATION = 6 * 20;

	public boolean mEmpoweredAttack;

	public SpellAbyssalCharge(LivingEntity boss, int cooldown) {
		mBoss = boss;
		mCooldownTicks = cooldown;
		mEmpoweredAttack = false;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		LivingEntity e = ((Mob) mBoss).getTarget();
		if (!(e instanceof Player)) {
			e = EntityUtils.getNearestPlayer(loc, 20.0);
			((Mob) mBoss).setTarget(e);
		}
		if (e == null) {
			return;
		}
		//Jump back
		MovementUtils.knockAway(e, mBoss, 2.0f, false);
		Plugin.getInstance().mEffectManager.addEffect(mBoss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
			new BaseMovementSpeedModifyEffect(DURATION, 0.4));
		loc.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 5, 1.25f);
		loc.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_AMBIENT, SoundCategory.HOSTILE, 5, 0.5f);
		mEmpoweredAttack = true;

		//Disabled empowered attack later
		new BukkitRunnable() {

			@Override
			public void run() {
				mEmpoweredAttack = false;
				loc.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 5, 1.25f);
			}

		}.runTaskLater(Plugin.getInstance(), DURATION);
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		//Extra damage
		if (mEmpoweredAttack && damagee instanceof Player) {
			event.setDamage(event.getDamage() * DAMAGE_MULTIPLIER);
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 5, 1.25f);
			mEmpoweredAttack = false;
		}
	}
}
