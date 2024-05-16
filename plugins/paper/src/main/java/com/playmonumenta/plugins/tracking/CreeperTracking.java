package com.playmonumenta.plugins.tracking;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.Particle;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;

public class CreeperTracking implements EntityTracking {
	private Set<Creeper> mEntities = new HashSet<Creeper>();
	private int mTicks = 0;

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Creeper)entity);
	}

	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}

	@Override
	public void update(int ticks) {
		Iterator<Creeper> creeperIter = mEntities.iterator();
		while (creeperIter.hasNext()) {
			Creeper creeper = creeperIter.next();
			if (creeper != null && creeper.isValid() && creeper.getLocation().isChunkLoaded()) {
				Set<String> tags = creeper.getScoreboardTags();
				if (tags.contains("Snuggles")) {
					new PartialParticle(Particle.HEART, creeper.getLocation().add(0, 1, 0), 1, 0.4, 1, 0.4, 0)
						.spawnAsEntityActive(creeper);
				}

				// Very infrequently check if the creeper is still actually there
				mTicks++;
				if (mTicks > 306) {
					mTicks = 0;
					if (!EntityUtils.isStillLoaded(creeper)) {
						creeperIter.remove();
					}
				}
			} else {
				creeperIter.remove();
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		mEntities.clear();
	}
}
