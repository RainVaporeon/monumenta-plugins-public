package com.playmonumenta.plugins.delves.abilities;

import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.bosses.bosses.ChargerBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileBoss;
import com.playmonumenta.plugins.bosses.bosses.RejuvenationBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Arcanic {

	private static final double ABILITY_CHANCE_PER_LEVEL = 0.06;

	private static final List<List<String>> ABILITY_POOL_R1;
	private static final List<List<String>> ABILITY_POOL_R2;
	private static final List<List<String>> ABILITY_POOL_R3;

	private static final String TRACKING_SPELL_NAME = "Arcanic Missile";
	private static final String MAGIC_ARROW_SPELL_NAME = "Arcanic Arrow";
	private static final String CHARGE_SPELL_NAME = "Arcanic Charge";
	public static final ImmutableSet<String> SPELL_NAMES = ImmutableSet.of(TRACKING_SPELL_NAME, MAGIC_ARROW_SPELL_NAME, CHARGE_SPELL_NAME);

	static {
		ABILITY_POOL_R1 = new ArrayList<>();
		ABILITY_POOL_R2 = new ArrayList<>();
		ABILITY_POOL_R3 = new ArrayList<>();

		//RejuvenationBoss
		List<String> rejuvenation = new ArrayList<>();
		rejuvenation.add(RejuvenationBoss.identityTag + "[heal=10]");
		ABILITY_POOL_R1.add(rejuvenation);
		rejuvenation = new ArrayList<>();
		rejuvenation.add(RejuvenationBoss.identityTag + "[heal=25]");
		ABILITY_POOL_R2.add(rejuvenation);
		rejuvenation = new ArrayList<>();
		rejuvenation.add(RejuvenationBoss.identityTag + "[heal=35]");
		ABILITY_POOL_R3.add(rejuvenation);

		//ProjectileBoss - tracking
		List<String> trackingProjectile = new ArrayList<>();
		trackingProjectile.add(ProjectileBoss.identityTag);
		trackingProjectile.add(ProjectileBoss.identityTag + "[damage=12,speed=0.2,delay=20,cooldown=320,turnradius=3.141],spellname=\"" + TRACKING_SPELL_NAME + "\"");
		trackingProjectile.add(ProjectileBoss.identityTag + "[soundstart=[(BLOCK_BEACON_POWER_SELECT,1,0.5)],soundlaunch=[(ENTITY_EVOKER_CAST_SPELL,1,0.5)],soundprojectile=[(BLOCK_BEACON_POWER_SELECT,0.4,0.5)],soundhit=[(BLOCK_BEACON_DEACTIVATE,1,0.5)]]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[particlelaunch=[(SPELL_WITCH,40,0,0,0,0.3)],particleprojectile=[(SPELL_WITCH,6,0,0,0,0.3),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],particlehit=[(SPELL_WITCH,50,0,0,0,0.3)]]");
		ABILITY_POOL_R1.add(trackingProjectile);
		trackingProjectile = new ArrayList<>();
		trackingProjectile.add(ProjectileBoss.identityTag);
		trackingProjectile.add(ProjectileBoss.identityTag + "[damage=30,speed=0.2,delay=20,cooldown=320,turnradius=3.141,spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[soundstart=[(BLOCK_BEACON_POWER_SELECT,1,0.5)],soundlaunch=[(ENTITY_EVOKER_CAST_SPELL,1,0.5)],soundprojectile=[(BLOCK_BEACON_POWER_SELECT,0.4,0.5)],soundhit=[(BLOCK_BEACON_DEACTIVATE,1,0.5)]]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[particlelaunch=[(SPELL_WITCH,40,0,0,0,0.3)],particleprojectile=[(SPELL_WITCH,6,0,0,0,0.3),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],particlehit=[(SPELL_WITCH,50,0,0,0,0.3)]]");
		ABILITY_POOL_R2.add(trackingProjectile);
		trackingProjectile = new ArrayList<>();
		trackingProjectile.add(ProjectileBoss.identityTag);
		trackingProjectile.add(ProjectileBoss.identityTag + "[damage=40,speed=0.2,delay=20,cooldown=320,turnradius=3.141,spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[soundstart=[(BLOCK_BEACON_POWER_SELECT,1,0.5)],soundlaunch=[(ENTITY_EVOKER_CAST_SPELL,1,0.5)],soundprojectile=[(BLOCK_BEACON_POWER_SELECT,0.4,0.5)],soundhit=[(BLOCK_BEACON_DEACTIVATE,1,0.5)]]");
		trackingProjectile.add(ProjectileBoss.identityTag + "[particlelaunch=[(SPELL_WITCH,40,0,0,0,0.3)],particleprojectile=[(SPELL_WITCH,6,0,0,0,0.3),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],particlehit=[(SPELL_WITCH,50,0,0,0,0.3)]]");
		ABILITY_POOL_R3.add(trackingProjectile);

		//ChargerStrongBoss
		List<String> charger = new ArrayList<>();
		charger.add(ChargerBoss.identityTag);
		charger.add(ChargerBoss.identityTag + "[damage=15,spellname=\"" + CHARGE_SPELL_NAME + "\"]");
		ABILITY_POOL_R1.add(charger);
		charger = new ArrayList<>();
		charger.add(ChargerBoss.identityTag);
		charger.add(ChargerBoss.identityTag + "[damage=30,spellname=\"" + CHARGE_SPELL_NAME + "\"]");
		ABILITY_POOL_R2.add(charger);
		charger = new ArrayList<>();
		charger.add(ChargerBoss.identityTag);
		charger.add(ChargerBoss.identityTag + "[damage=40,spellname=\"" + CHARGE_SPELL_NAME + "\"]");
		ABILITY_POOL_R3.add(charger);

		//ProjectileBoss - magic arrow
		List<String> magicArrow = new ArrayList<>();
		magicArrow.add(ProjectileBoss.identityTag);
		magicArrow.add(ProjectileBoss.identityTag + "[damage=12,distance=32,speed=0.8,delay=20,cooldown=160,turnradius=0,spellname=\"" + MAGIC_ARROW_SPELL_NAME + "\"]");
		magicArrow.add(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1)],soundlaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]");
		magicArrow.add(ProjectileBoss.identityTag + "[particlelaunch=[],particleprojectile=[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.25)]]");
		ABILITY_POOL_R1.add(magicArrow);
		magicArrow = new ArrayList<>();
		magicArrow.add(ProjectileBoss.identityTag);
		magicArrow.add(ProjectileBoss.identityTag + "[damage=30,distance=32,speed=0.8,delay=20,cooldown=160,turnradius=0,spellname=\"" + MAGIC_ARROW_SPELL_NAME + "\"]");
		magicArrow.add(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1)],soundlaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]");
		magicArrow.add(ProjectileBoss.identityTag + "[particlelaunch=[],particleprojectile=[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.25)]]");
		ABILITY_POOL_R2.add(magicArrow);
		magicArrow = new ArrayList<>();
		magicArrow.add(ProjectileBoss.identityTag);
		magicArrow.add(ProjectileBoss.identityTag + "[damage=40,distance=32,speed=0.8,delay=20,cooldown=160,turnradius=0,spellname=\"" + MAGIC_ARROW_SPELL_NAME + "\"]");
		magicArrow.add(ProjectileBoss.identityTag + "[soundstart=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1)],soundlaunch=[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,1.5)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,1.5)]]");
		magicArrow.add(ProjectileBoss.identityTag + "[particlelaunch=[],particleprojectile=[(FIREWORKS_SPARK,5,0.1,0.1,0.1,0.05),(CRIT_MAGIC,20,0.2,0.2,0.2,0.1)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.25)]]");
		ABILITY_POOL_R3.add(magicArrow);
	}

	public static final String DESCRIPTION = "Enemies gain magical abilities.";

	public static String[] rankDescription(int level) {
		return new String[]{"Enemies have a " + Math.round(100 * ABILITY_CHANCE_PER_LEVEL * level) + "% chance to be Arcanic."};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		Player nearestPlayer = EntityUtils.getNearestPlayer(mob.getLocation(), 64);
		if (FastUtils.RANDOM.nextDouble() < ABILITY_CHANCE_PER_LEVEL * level && !DelvesUtils.isDelveMob(mob)) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			List<List<String>> abilityPool = new ArrayList<>(ServerProperties.getClassSpecializationsEnabled(nearestPlayer) ? (ServerProperties.getAbilityEnhancementsEnabled(nearestPlayer) ? ABILITY_POOL_R3 : ABILITY_POOL_R2) : ABILITY_POOL_R1);
			abilityPool.removeIf(ability -> mob.getScoreboardTags().contains(ability.get(0)));
			List<String> ability = abilityPool.get(FastUtils.RANDOM.nextInt(abilityPool.size()));
			for (String abilityTag : ability) {
				mob.addScoreboardTag(abilityTag);
			}
		}
	}
}
