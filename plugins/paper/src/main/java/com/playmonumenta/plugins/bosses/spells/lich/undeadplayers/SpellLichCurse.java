package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellLichCurse extends Spell {
	private final LivingEntity mBoss;
	private final PartialParticle mParticle1;
	private final PartialParticle mParticle2;

	private int mT = 0;

	public SpellLichCurse(LivingEntity boss) {
		mBoss = boss;
		mParticle1 = new PartialParticle(Particle.SOUL, mBoss.getLocation().add(0, 0.25, 0), 2, 0.2, 0.2, 0.2, 0.01).spawnAsEnemy();
		mParticle2 = new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation().add(0, 0.25, 0), 2, 0.2, 0.2, 0.2, 0.01).spawnAsEnemy();
	}

	@Override
	public void run() {
		mT += 5;
		Location loc = mBoss.getLocation().add(0, 0.25, 0);
		mParticle1.location(loc).spawnAsEnemy();
		mParticle2.location(loc).spawnAsEnemy();
		if (mT >= 20 * 20) {
			mBoss.remove();
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
