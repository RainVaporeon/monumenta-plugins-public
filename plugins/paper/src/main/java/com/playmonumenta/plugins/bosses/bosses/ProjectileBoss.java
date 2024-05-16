package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.Limit.LIMITSENUM;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets.TARGETS;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_projectile";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int DAMAGE = 0;

		@BossParam(help = "How long the projectile can travel at most.")
		public int DISTANCE = 64;

		@BossParam(help = "Projectile speed")
		public double SPEED = 0.4;

		@BossParam(help = "not written")
		public int DETECTION = 24;

		@BossParam(help = "Delay of the first spell, then cooldown is used to determinate when this spell will cast again")
		public int DELAY = 20 * 5;

		@BossParam(help = "Time period between the start of the last charge and next start.")
		public int COOLDOWN = 20 * 10;

		@BossParam(help = "not written")
		public boolean LINGERS = true;

		@BossParam(help = "not written")
		public double HITBOX_LENGTH = 0.5;

		@BossParam(help = "not written", deprecated = true)
		public boolean SINGLE_TARGET = true;

		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Boolean on if the projectile can damage entities or not")
		public boolean DAMAGE_PLAYER_ONLY = false;

		@BossParam(help = "Track target when launching if true.")
		public boolean LAUNCH_TRACKING = true;

		@BossParam(help = "Angular velocity (in radian) of projectile when tracking target. Set to 0 for linear projectile.")
		public double TURN_RADIUS = Math.PI / 30;

		@BossParam(help = "not written")
		public boolean COLLIDES_WITH_OTHERS = false;

		@BossParam(help = "not written")
		public boolean COLLIDES_WITH_BLOCKS = true;

		@BossParam(help = "Percentage Speed of projectile when moving in liquids")
		public double SPEED_LIQUID = 0.5;

		@BossParam(help = "Percentage Speed of projectile when moving in blocks")
		public double SPEED_BLOCKS = 0.125;

		@BossParam(help = "Delay on each single cast between sound_start and the actual cast of the projectile")
		public int SPELL_DELAY = Integer.MAX_VALUE;

		@BossParam(help = "The glowing color")
		public String COLOR = "red";

		@BossParam(help = "Let you choose the targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;
		//note: this object is only used to show the default value while using /bosstag add boss_projectile[targets=[...]]

		@BossParam(help = "Effects applied to the player when he got hit")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "How many times to be cast after one cooldown")
		public int CHARGE = 1;

		@BossParam(help = "Interval between casting with charges")
		public int CHARGE_INTERVAL = 40;

		@BossParam(help = "Left offset from mob's eye to projectile start point")
		public double OFFSET_LEFT = 0;

		@BossParam(help = "Up offset from mob's eye to projectile start point")
		public double OFFSET_UP = 0;

		@BossParam(help = "Front offset from mob's eye to projectile start point")
		public double OFFSET_FRONT = 0;

		@BossParam(help = "How many projectiles mob will launch in a sector plane")
		public int SPLIT = 1;

		@BossParam(help = "Interval angles between splitting projectiles in degree")
		public double SPLIT_ANGLE = 30;

		@BossParam(help = "Dupe launch in mirror position. 0=None, 1=L-R, 2=F-B, 3=Both")
		public int MIRROR = 0;

		@BossParam(help = "Force launch at a yaw degree offset from boss' sight. [-180, 180] is valid.")
		public double FIX_YAW = 200.0;

		@BossParam(help = "Force launch at a fixed pitch degree. [-90, 90] is valid.")
		public double FIX_PITCH = 100.0;

		@BossParam(help = "overheal y/n")
		public boolean OVERHEAL = false;

		@BossParam(help = "amount healed")
		public double HEAL_AMOUNT = 0;

		//particle & sound used!
		@BossParam(help = "Sound played at the start")
		public SoundsList SOUND_START = SoundsList.fromString("[(ENTITY_BLAZE_AMBIENT,1.5,1)]");

		@BossParam(help = "Particle used when launching the projectile")
		public ParticlesList PARTICLE_LAUNCH = ParticlesList.fromString("[(EXPLOSION_LARGE,1)]");

		@BossParam(help = "Sound used when launching the projectile")
		public SoundsList SOUND_LAUNCH = SoundsList.fromString("[(ENTITY_BLAZE_SHOOT,0.5,0.5)]");

		@BossParam(help = "Particle used for the projectile")
		public ParticlesList PARTICLE_PROJECTILE = ParticlesList.fromString("[(FLAME, 4, 0.05, 0.05, 0.05, 0.1),(SMOKE_LARGE, 3, 0.25, 0.25, 0.25)]");

		@BossParam(help = "Sound summoned every 2 sec on the projectile location")
		public SoundsList SOUND_PROJECTILE = SoundsList.fromString("[(ENTITY_BLAZE_BURN,0.5,0.2)]");

		@BossParam(help = "Particle used when the projectile hit something")
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(CLOUD,50,0,0,0,0.25)]");

		@BossParam(help = "Sound used when the projectile hit something")
		public SoundsList SOUND_HIT = SoundsList.fromString("[(ENTITY_GENERIC_DEATH,0.5,0.5)]");

	}

	public ProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		int lifetimeTicks = (int) (p.DISTANCE / p.SPEED);

		if (p.TARGETS == EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT) {
			//same object
			//probably an older mob version?
			//build a new target from others config
			p.TARGETS = new EntityTargets(TARGETS.PLAYER, p.DETECTION, false, p.SINGLE_TARGET ? new Limit(1) : new Limit(LIMITSENUM.ALL), List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));
			//by default ProjectileBoss doesn't take player in stealth and need line of sight.
		}

		if (p.SPELL_DELAY == Integer.MAX_VALUE) {
			p.SPELL_DELAY = p.DELAY;
		}

		if (p.MIRROR > 3 || p.MIRROR < 0) {
			p.MIRROR = 0;
		}

		Spell spell = new SpellBaseSeekingProjectile(plugin, boss, p.LAUNCH_TRACKING, p.CHARGE, p.CHARGE_INTERVAL, p.COOLDOWN, p.SPELL_DELAY,
			p.OFFSET_LEFT, p.OFFSET_UP, p.OFFSET_FRONT, p.MIRROR, p.FIX_YAW, p.FIX_PITCH, p.SPLIT, p.SPLIT_ANGLE,
			p.SPEED, p.TURN_RADIUS, lifetimeTicks, p.HITBOX_LENGTH, p.LINGERS, p.COLLIDES_WITH_BLOCKS, p.SPEED_LIQUID, p.SPEED_BLOCKS, p.COLLIDES_WITH_OTHERS, 0,
			//spell targets
			() -> {
				return p.TARGETS.getTargetsList(mBoss);
			},
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				if (p.SPELL_DELAY > 0) {
					PotionUtils.applyColoredGlowing(identityTag, boss, NamedTextColor.NAMES.valueOr(p.COLOR, NamedTextColor.RED), p.SPELL_DELAY);
				}
				p.SOUND_START.play(loc);
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				p.PARTICLE_LAUNCH.spawn(boss, loc);
				p.SOUND_LAUNCH.play(loc);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				p.PARTICLE_PROJECTILE.spawn(boss, loc, 0.1, 0.1, 0.1, 0.1);
				if (ticks % 40 == 0) {
					p.SOUND_PROJECTILE.play(loc);
				}
			},
			// Hit Action
			(World world, @Nullable LivingEntity target, Location loc, @Nullable Location prevLoc) -> {
				if (!p.DAMAGE_PLAYER_ONLY || target instanceof Player) {
					p.SOUND_HIT.play(loc, 0.5f, 0.5f);
					p.PARTICLE_HIT.spawn(boss, loc, 0d, 0d, 0d, 0.25d);

					if (target != null) {
						if (p.DAMAGE > 0) {
							BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE, p.SPELL_NAME, prevLoc);
						}

						if (p.DAMAGE_PERCENTAGE > 0.0) {
							BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, prevLoc, p.SPELL_NAME);
						}

						if (p.HEAL_AMOUNT > 0) {
							double healed = EntityUtils.healMob(target, p.HEAL_AMOUNT);
							if (p.OVERHEAL && healed < p.HEAL_AMOUNT) {
								double missing = p.HEAL_AMOUNT - healed;
								AbsorptionUtils.addAbsorption(target, missing, p.HEAL_AMOUNT, -1);
							}
						}
						p.EFFECTS.apply(target, boss);
					}
				}
			});

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);

	}
}
