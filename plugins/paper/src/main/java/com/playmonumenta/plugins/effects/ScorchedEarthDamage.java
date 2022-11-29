package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ScorchedEarthDamage extends Effect {
	public static final String effectID = "ScorchedEarthDamage";

	private final double mDamage;
	private final Player mAlchemist;
	private final PlayerItemStats mStats;
	private final int mFireTickDuration;

	public ScorchedEarthDamage(int duration, double damage, Player player, PlayerItemStats stats, int fireDuration) {
		super(duration, effectID);
		mDamage = damage;
		mAlchemist = player;
		mStats = stats;
		mFireTickDuration = fireDuration;
	}

	@Override
	public double getMagnitude() {
		return mDamage;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		DamageType type = event.getType();
		if (type != DamageType.AILMENT && type != DamageType.FIRE && type != DamageType.OTHER && event.getAbility() != ClassAbility.SCORCHED_EARTH
			    && (type != DamageType.MELEE || !(event.getDamager() instanceof Player player) || player.getCooledAttackStrength(0) > 0.5f)) {
			DamageUtils.damage(mAlchemist, entity, new DamageEvent.Metadata(DamageType.MAGIC, ClassAbility.SCORCHED_EARTH, mStats), mDamage, true, false, false);
			EntityUtils.applyFire(Plugin.getInstance(), mFireTickDuration, entity, mAlchemist);
			World world = entity.getWorld();
			Location loc = entity.getLocation().clone().add(0, 1, 0);
			world.spawnParticle(Particle.FLAME, loc, 5, 0.25, 0.5, 0.25, 0.05);
			world.spawnParticle(Particle.REDSTONE, loc, 15, 0.35, 0.5, 0.35, new Particle.DustOptions(ScorchedEarth.SCORCHED_EARTH_COLOR_DARK, 1.0f));
			world.spawnParticle(Particle.LAVA, loc, 3, 0.25, 0.5, 0.25, 0);
		}
	}

	@Override
	public String toString() {
		return String.format("ScorchedEarthDamage duration=%d", this.getDuration());
	}
}
