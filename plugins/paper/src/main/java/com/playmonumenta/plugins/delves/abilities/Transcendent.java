package com.playmonumenta.plugins.delves.abilities;

import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.bosses.bosses.CommanderBoss;
import com.playmonumenta.plugins.bosses.bosses.DodgeBoss;
import com.playmonumenta.plugins.bosses.bosses.ProjectileBoss;
import com.playmonumenta.plugins.bosses.bosses.WrathBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class Transcendent {

	private static final double ABILITY_CHANCE_PER_LEVEL = 0.1;

	private static final List<List<String>> ABILITY_POOL_MELEE_R1;
	private static final List<List<String>> ABILITY_POOL_MELEE_R2;
	private static final List<List<String>> ABILITY_POOL_MELEE_R3;

	private static final List<List<String>> ABILITY_POOL_R1;
	private static final List<List<String>> ABILITY_POOL_R2;
	private static final List<List<String>> ABILITY_POOL_R3;

	private static final String TRACKING_SPELL_NAME = "Transcendental Missile";
	private static final String WRATH_SPELL_NAME = "Transcendental Wrath";
	public static final ImmutableSet<String> SPELL_NAMES = ImmutableSet.of(TRACKING_SPELL_NAME, WRATH_SPELL_NAME);

	static {
		ABILITY_POOL_MELEE_R1 = new ArrayList<>();
		ABILITY_POOL_MELEE_R2 = new ArrayList<>();
		ABILITY_POOL_MELEE_R3 = new ArrayList<>();

		ABILITY_POOL_R1 = new ArrayList<>();
		ABILITY_POOL_R2 = new ArrayList<>();
		ABILITY_POOL_R3 = new ArrayList<>();

		List<String> seekingProjectileBoss = new ArrayList<>();
		seekingProjectileBoss.add(ProjectileBoss.identityTag);
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[damage=8,distance=32,speed=0.5,delay=30,cooldown=100,turnradius=0.02,charge=3,chargeinterval=10,effects=[(pushforce,0.5)],spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[soundstart=[],soundlaunch=[(ENTITY_ILLUSIONER_CAST_SPELL,1,0.5),(ENTITY_FIREWORK_ROCKET_LAUNCH,1,0.8)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,0.5)]]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[particlelaunch=[(EXPLOSION_LARGE,1)],particleprojectile=[(FIREWORKS_SPARK,3,0,0,0,0.1),(SPELL_WITCH,10,0.2,0.2,0.2,0),(END_ROD,2,0.2,0.2,0.2,0)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.3)]]");
		ABILITY_POOL_MELEE_R1.add(seekingProjectileBoss);
		ABILITY_POOL_R1.add(seekingProjectileBoss);
		seekingProjectileBoss = new ArrayList<>();
		seekingProjectileBoss.add(ProjectileBoss.identityTag);
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[damage=16,distance=32,speed=0.7,delay=30,cooldown=90,turnradius=0.03,charge=3,chargeinterval=10,effects=[(pushforce,0.5)],spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[soundstart=[],soundlaunch=[(ENTITY_ILLUSIONER_CAST_SPELL,1,0.5),(ENTITY_FIREWORK_ROCKET_LAUNCH,1,0.8)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,0.5)]]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[particlelaunch=[(EXPLOSION_LARGE,1)],particleprojectile=[(FIREWORKS_SPARK,3,0,0,0,0.1),(SPELL_WITCH,10,0.2,0.2,0.2,0),(END_ROD,2,0.2,0.2,0.2,0)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.3)]]");
		ABILITY_POOL_MELEE_R2.add(seekingProjectileBoss);
		ABILITY_POOL_R2.add(seekingProjectileBoss);
		seekingProjectileBoss = new ArrayList<>();
		seekingProjectileBoss.add(ProjectileBoss.identityTag);
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[damage=24,distance=32,speed=0.7,delay=30,cooldown=90,turnradius=0.03,charge=3,chargeinterval=10,effects=[(pushforce,0.5)],spellname=\"" + TRACKING_SPELL_NAME + "\"]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[soundstart=[],soundlaunch=[(ENTITY_ILLUSIONER_CAST_SPELL,1,0.5),(ENTITY_FIREWORK_ROCKET_LAUNCH,1,0.8)],soundprojectile=[],soundhit=[(ENTITY_FIREWORK_ROCKET_TWINKLE,0.5,0.5)]]");
		seekingProjectileBoss.add(ProjectileBoss.identityTag + "[particlelaunch=[(EXPLOSION_LARGE,1)],particleprojectile=[(FIREWORKS_SPARK,3,0,0,0,0.1),(SPELL_WITCH,10,0.2,0.2,0.2,0),(END_ROD,2,0.2,0.2,0.2,0)],particlehit=[(FIREWORKS_SPARK,30,0,0,0,0.3)]]");
		ABILITY_POOL_MELEE_R3.add(seekingProjectileBoss);
		ABILITY_POOL_R3.add(seekingProjectileBoss);

		List<String> commanderBoss = new ArrayList<>();
		commanderBoss.add(CommanderBoss.identityTag);
		ABILITY_POOL_MELEE_R1.add(commanderBoss);
		ABILITY_POOL_R1.add(commanderBoss);
		ABILITY_POOL_MELEE_R2.add(commanderBoss);
		ABILITY_POOL_R2.add(commanderBoss);
		ABILITY_POOL_MELEE_R3.add(commanderBoss);
		ABILITY_POOL_R3.add(commanderBoss);

		List<String> wrathBoss = new ArrayList<>();
		wrathBoss.add(WrathBoss.identityTag);
		wrathBoss.add(WrathBoss.identityTag + "[damage=9,spellname=\"" + WRATH_SPELL_NAME + "\"]");
		wrathBoss.add(DodgeBoss.identityTag);
		wrathBoss.add(DodgeBoss.identityTag + "[cooldown=120]");
		ABILITY_POOL_MELEE_R1.add(wrathBoss);
		wrathBoss = new ArrayList<>();
		wrathBoss.add(WrathBoss.identityTag);
		wrathBoss.add(WrathBoss.identityTag + "[damage=18,spellname=\"" + WRATH_SPELL_NAME + "\"]");
		wrathBoss.add(DodgeBoss.identityTag);
		wrathBoss.add(DodgeBoss.identityTag + "[cooldown=100]");
		ABILITY_POOL_MELEE_R2.add(wrathBoss);
		wrathBoss = new ArrayList<>();
		wrathBoss.add(WrathBoss.identityTag);
		wrathBoss.add(WrathBoss.identityTag + "[damage=27,spellname=\"" + WRATH_SPELL_NAME + "\"]");
		wrathBoss.add(DodgeBoss.identityTag);
		wrathBoss.add(DodgeBoss.identityTag + "[cooldown=80]");
		ABILITY_POOL_MELEE_R3.add(wrathBoss);

	}

	public static final String DESCRIPTION = "Elites become greatly empowered.";

	public static String[] rankDescription(int level) {
		return new String[]{
			"Elites have a " + Math.round(ABILITY_CHANCE_PER_LEVEL * level * 100) + "% chance to be Transcendent."
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob) && FastUtils.RANDOM.nextDouble() < ABILITY_CHANCE_PER_LEVEL * level) {
			EntityEquipment equipment = mob.getEquipment();
			ItemStack mainhand = equipment == null ? null : equipment.getItemInMainHand();
			Material material = mainhand == null ? null : mainhand.getType();
			List<List<String>> abilityPool;
			Player nearestPlayer = EntityUtils.getNearestPlayer(mob.getLocation(), 64);
			if (material == Material.BOW || material == Material.CROSSBOW || material == Material.TRIDENT
				|| mob instanceof Evoker) {
				abilityPool = new ArrayList<>(ServerProperties.getClassSpecializationsEnabled(nearestPlayer) ? (ServerProperties.getAbilityEnhancementsEnabled(nearestPlayer) ? ABILITY_POOL_R3 : ABILITY_POOL_R2) : ABILITY_POOL_R1);
			} else {
				abilityPool = new ArrayList<>(ServerProperties.getClassSpecializationsEnabled(nearestPlayer) ? (ServerProperties.getAbilityEnhancementsEnabled(nearestPlayer) ? ABILITY_POOL_MELEE_R3 : ABILITY_POOL_MELEE_R2) : ABILITY_POOL_MELEE_R1);
			}

			abilityPool.removeIf(ability -> mob.getScoreboardTags().contains(ability.get(0)));
			List<String> ability = abilityPool.get(FastUtils.RANDOM.nextInt(abilityPool.size()));
			for (String abilityTag : ability) {
				mob.addScoreboardTag(abilityTag);
			}

			World world = mob.getWorld();
			Location loc = mob.getLocation();

			world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1f, 1f);
			world.playSound(loc, Sound.BLOCK_IRON_DOOR_OPEN, SoundCategory.HOSTILE, 1f, 0.8f);
			world.playSound(loc, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 1f, 0.5f);
			world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 1f, 0.8f);
			world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 0.4f, 0.5f);
			world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 1f, 0.6f);
			new PartialParticle(Particle.FIREWORKS_SPARK, LocationUtils.getEntityCenter(mob), 60, 0.5, 0.5, 0.5, 0.18).spawnAsEnemy();
		}
	}

}
