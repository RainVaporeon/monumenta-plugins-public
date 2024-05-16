package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.DodgeBoss;
import com.playmonumenta.plugins.bosses.bosses.TpBehindBoss;
import com.playmonumenta.plugins.bosses.bosses.UnseenBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

public class Assassins {
	private static List<List<String>> ABILITY_POOL;

	public static final String DESCRIPTION = "Enemies become stealthy assassins.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Mobs deal 40% extra damage when not in the player's"),
			Component.text("field of view, and have a 30% chance to become"),
			Component.text("\"stealthed,\" gaining new abilities.")
		};
	}

	static {
		ABILITY_POOL = new ArrayList<>();

		//TpBehindTargetedBoss
		List<String> tpBehind = new ArrayList<>();
		tpBehind.add(TpBehindBoss.identityTag);
		tpBehind.add(TpBehindBoss.identityTag + "[range=50,random=false]");
		ABILITY_POOL.add(tpBehind);

		List<String> dodge = new ArrayList<>();
		dodge.add(DodgeBoss.identityTag);
		ABILITY_POOL.add(dodge);
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(UnseenBoss.identityTag);
			mob.addScoreboardTag(UnseenBoss.identityTag + "[damageincrease=1.4]");
			if (FastUtils.RANDOM.nextDouble() < .3) {
				List<List<String>> abilityPool = new ArrayList<>(ABILITY_POOL);
				abilityPool.removeIf(ability -> mob.getScoreboardTags().contains(ability.get(0)));
				List<String> ability = abilityPool.get(FastUtils.RANDOM.nextInt(abilityPool.size()));
				for (String abilityTag : ability) {
					mob.addScoreboardTag(abilityTag);
				}
			}
		}
	}
}
