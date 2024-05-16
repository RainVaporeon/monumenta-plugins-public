package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.spells.SpellBullet;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BulletHellBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_bullet_hell";

	@BossParam(help = "Bullets")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Amount of damage a bullet will deal")
		public int DAMAGE = 0;
		@BossParam(help = "Amount of true damage a bullet will deal, in %")
		public int DAMAGE_PERCENTAGE = 0;
		@BossParam(help = "The name of the spell.")
		public String SPELL_NAME = "";
		@BossParam(help = "Effects to apply on hit.")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "Cast Duration (30 for Junko, 50 for sanae and 600 for border)")
		public int DURATION = 50;
		@BossParam(help = "Bullet Initial Speed (0 for sanae, 0.1 for Junko, 0.3 for border)")
		public double VELOCITY = 0;
		@BossParam(help = "Detection Range")
		public int DETECTION = 20;
		@BossParam(help = "Windup Delay (20 for sanae and junko, 60 for border)")
		public int DELAY = 20 * 1;
		@BossParam(help = "Ticks per bullet emission (1 for sanae, 5 for Junko and Border")
		public int EMISSION_TICKS = 1;
		@BossParam(help = "Charge sound")
		public Sound CHARGE_SOUND = Sound.BLOCK_NOTE_BLOCK_BIT;
		@BossParam(help = "Cooldown (20 for sanae and junko, 100 for border)")
		public int COOLDOWN = 20 * 1;
		@BossParam(help = "Bullet duration (120 for sanae, 40 for junko and border)")
		public int BULLET_DURATION = 120;
		@BossParam(help = "Bullet size")
		public double HITBOX_RADIUS = 0.3125;
		@BossParam(help = "Bullet material")
		public Material MATERIAL = Material.PURPUR_BLOCK;
		@BossParam(help = "Bullet emission sound")
		public Sound SHOOT_SOUND = Sound.BLOCK_NOTE_BLOCK_SNARE;
		@BossParam(help = "Bullet pattern")
		public String PATTERN = "SANAE";

		@BossParam(help = "Charging volume")
		public float CHARGE_VOLUME = 2;

		@BossParam(help = "Shooting volume")
		public float SHOOT_VOLUME = 0.25f;
		@BossParam(help = "Junko accel")
		// Junko Specific
		public double ACCEL = 0.2;
		@BossParam(help = "Junko begin accel")
		public int ACCEL_START = 10;
		@BossParam(help = "Junko end accel")
		public int ACCEL_END = 15;
		@BossParam(help = "Pass through walls?")
		public boolean PASS_THROUGH = false;
		@BossParam(help = "Y offset")
		public double OFFSET_Y = 0.0;
		@BossParam(help = "Speed of rotation (lower = faster)")
		public double ROTATION_SPEED = 480.0;
	}

	public BulletHellBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		World world = boss.getWorld();
		BulletHellBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new BulletHellBoss.Parameters());

		super.constructBoss(
			new SpellBullet(plugin, boss, new Vector(0, p.OFFSET_Y, 0), p.DURATION, p.DELAY, p.EMISSION_TICKS, p.VELOCITY, p.DETECTION, p.HITBOX_RADIUS, p.COOLDOWN, p.BULLET_DURATION, p.PATTERN,
				p.ACCEL, p.ACCEL_START, p.ACCEL_END, p.PASS_THROUGH, p.ROTATION_SPEED,
				(Entity entity, int tick) -> {
					float t = tick / 10f;
					if (tick % 5 == 0) {
						world.playSound(mBoss.getLocation(), p.CHARGE_SOUND, SoundCategory.HOSTILE, p.CHARGE_VOLUME, t);
					}
				},
				(Entity entity) -> {
					world.playSound(mBoss.getLocation(), p.SHOOT_SOUND, SoundCategory.HOSTILE, p.SHOOT_VOLUME, 0);
				},
				p.MATERIAL,
				(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc) -> {
					if (player != null && !blocked) {
						if (p.DAMAGE > 0) {
							DamageUtils.damage(boss, player, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, null, null, p.SPELL_NAME), p.DAMAGE, false, true, false);
						}
						if (p.DAMAGE_PERCENTAGE > 0) {
							DamageUtils.damage(boss, player, new DamageEvent.Metadata(DamageEvent.DamageType.OTHER, null, null, p.SPELL_NAME), p.DAMAGE_PERCENTAGE * EntityUtils.getMaxHealth(player) / 100.0, false, true, false);
						}
						p.EFFECTS.apply(player, mBoss);
					}
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 5, 0, 0, 0, 0.175).spawnAsEnemy();
				}
			), p.DETECTION, null, 0);

	}

}
