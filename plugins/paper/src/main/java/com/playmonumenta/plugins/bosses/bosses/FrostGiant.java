package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.SpellRemoveLevitation;
import com.playmonumenta.plugins.bosses.spells.frostgiant.ArmorOfFrost;
import com.playmonumenta.plugins.bosses.spells.frostgiant.GiantStomp;
import com.playmonumenta.plugins.bosses.spells.frostgiant.RingOfFrost;
import com.playmonumenta.plugins.bosses.spells.frostgiant.Shatter;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellAirGolemStrike;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostGiantBlockBreak;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostRift;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostbite;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellFrostedIceBreak;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellGlacialPrison;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellGreatswordSlam;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellHailstorm;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellSpinDown;
import com.playmonumenta.plugins.bosses.spells.frostgiant.SpellTitanicRupture;
import com.playmonumenta.plugins.bosses.spells.frostgiant.UltimateSeismicRuin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/* WARNING: Basically all the spell info in the comments is outdated.
 * Please use the Frost Giant Formal Write-up for up to date spell descriptions.
 */

/*
 *Frost Giant:
Block Break
Blizzard/Hailstorm - Creates a snowstorm in a circle that is 18 blocks
and beyond that passively deals 5% max health damage every half second
to players are in it and giving them slowness 3 for 2 seconds.

Heavy Blows - Frost Giant deals massive knockback to anyone he hits with
melee attacks and anyone within 4 blocks of that target, also deals 50%
of the damage done to the target to nearby enemies. The frost giant then
swaps targets.

Frostbite - Going above 4 blocks deals 5% max health damage to the player
and giving them slowness 3 for 2 seconds every 0.5s second(s)

Phase 1 Skills:
Shatter - All players within a 70 degree cone in front of the giant after
a 1 second charge up take 24 damage and are knocked back X blocks. If they
collide with a wall they take 10 additional damage and are stunned (Slowness 7,
Negative Jump Boost, weakness 10, maybe putting bows on cooldown, you get the
idea) for 2 seconds.

Glacial Prison - Traps ⅓ players in ice for 3 seconds, after those 3 seconds
the prison explodes dealing 20 damage and giving mining fatigue 3 for 10 seconds
and weakness 2 for 10 seconds.

Whirlwind - The Frost giant gains slowness 2 for 6 seconds while dealing 18
damage and knocking back players slightly if they are within 8 blocks for
those 6 seconds every half second

Phase 2 skills:
Shatter - All players within a 70 degree cone in front of the giant after a
1 second charge up take 24 damage and are knocked back X blocks. If they
collide with a wall they take 10 additional damage and are stunned (Slowness
7, Negative Jump Boost, weakness 10, maybe putting bows on cooldown, you get
the idea) for 2 seconds.

Rush Down - The giant pauses for 1 second, then rushes towards target player
dealing 30 damage to any player it passes through and knocking them up greatly,
once it reaches its destination it deals 12 damage in an 5 block radius.

Shield of Frost - The frost giant gains a shield that absorbs the next
(Same scaling as health scaling function with base 100) damage and applies
slowness 3 and deals knockback to the attacker for 5 seconds. If the shield
expires naturally it explodes dealing 28 damage in an 8 block radius. Expires
after 15 seconds.

Phase 3 Skills:
Shatter - All players within a 70 degree cone in front of the giant after a 1
second charge up take 24 damage and are knocked back X blocks. If they collide
with a wall they take 10 additional damage and are stunned (Slowness 7, Negative
Jump Boost, weakness 10, maybe putting bows on cooldown, you get the idea) for 2 seconds.

Summon the Moon Riders - Summons (Scaling based on players) horseback Archers
with swords in their offhand, if they are dismounted they swap to their swords.

Frost Rift: Targets ⅓  players. Afterwards, breaks the ground towards them,
creating a large rift of ice that deals 20 damage and applies Slowness 2,
Weakness 2, and Wither 3, for 8 seconds. This rift stays in place for 10 seconds.
If this rift collides with a target while rippling through the terrain, they
are knocked back and take 30 damage. This rift continues until it reaches the
edge of the Blizzard/Hailstorm.

Eclipse - The Giant stops moving, becomes invulnerable and causes 5 pulses of
energy to come towards it in a circle that extends 16 blocks out (think earth’s
wrath from Kaul but in reverse) these pulses deal 12 damage. give slowness 5
for 3 seconds and knock the player hit towards the Frost Giant, after the 5 pulses
the Frost Giant explodes dealing 60 damage in an 8 block radius. High CD, High DMG

 */

public class FrostGiant extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_frostgiant";
	public static final int detectionRange = 80;
	public static final int hailstormRadius = 16;
	public static final int frostedIceDuration = 30;
	//Range of those who are actively in the fight from the center of the arena
	public static final int fighterRange = 36;
	public static @Nullable FrostGiant mInstance = null;
	public static Boolean castStomp = true;
	//If the immune armor is active
	public boolean mFrostArmorActive = true;
	//If true, Frost Giant will stop trying to force target someone
	public boolean mPreventTargetting = false;

	private static final int MAX_HEALTH = 5012;
	private static final double SCALING_X = 0.6;
	private static final double SCALING_Y = 0.35;
	private static final String START_TAG = "FrostGiantStart";
	//Directions for Seismic Ruin skill
	private static final String NORTH = "FrostGiantNorth";
	private static final String EAST = "FrostGiantEast";
	private static final String SOUTH = "FrostGiantSouth";
	private static final String WEST = "FrostGiantWest";
	private static final String GOLEM_FREEZE_EFFECT_NAME = "FrostGiantGolemPercentSpeedEffect";
	private static final String PLAYER_ANTICHEESE_SLOWNESS_SRC = "FrostGiantAnticheeseSlowness";
	private static final Component[] fightDialogue = new Component[] {
		Component.text("WHAT CHILDREN OF ALRIC DARE ENTER THIS PLACE...", NamedTextColor.DARK_AQUA),
		Component.text("YOU... SHOULD HAVE NOT COME HERE... PERISH...", NamedTextColor.DARK_AQUA),
		Component.text("An armor forms around the boss that blocks all damage.", NamedTextColor.AQUA),
		Component.text("THE FROST WILL CONSUME YOU...", NamedTextColor.DARK_AQUA),
		Component.text("The permafrost shield reforms around the giant, blocking damage dealt once more.", NamedTextColor.AQUA),
		Component.text("THE SONG WILL PREVAIL... ALL WILL SUCCUMB TO THE BITTER COLD...", NamedTextColor.DARK_AQUA),
		Component.text("The permafrost shield reforms again.", NamedTextColor.AQUA),
		Component.text("I... WILL NOT... BE THE END... OF THE SONG!", NamedTextColor.DARK_AQUA),
		Component.text("THIS EARTH... WAS OURS ONCE... WE SHAPED IT...", NamedTextColor.DARK_AQUA),
		Component.text("DO NOT LET IT... PERISH WITH ME... THE SONG MUST NOT GO... UNSUNG...", NamedTextColor.DARK_AQUA)
	};
	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(66, 185, 245), 1.0f);
	private static final Particle.DustOptions LIGHT_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 255, 247), 1.0f);
	private final UltimateSeismicRuin mRuin;
	private final Location mStartLoc;
	private boolean mCutsceneDone = false;
	private @Nullable LivingEntity mNorthStand;
	private @Nullable LivingEntity mEastStand;
	private @Nullable LivingEntity mSouthStand;
	private @Nullable LivingEntity mWestStand;
	private @Nullable LivingEntity mTargeted;
	private @Nullable Location mStuckLoc;
	private int mPlayerCount;
	private double mDefenseScaling;
	public @Nullable ItemStack[] mArmor = null;
	private @Nullable ItemStack mMainhand = null;
	private @Nullable ItemStack mOffhand = null;

	public FrostGiant(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		World world = boss.getWorld();
		mBoss.addScoreboardTag("Boss");
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 9999, 0));
		//Giants hitboxes are huge as hell. We need a custom melee method
		LivingEntity start = null;
		mInstance = this;

		//Gets starting position from an armor stand with START_TAG
		//And all icicle armor stands
		//And all directional armor stands
		for (LivingEntity e : EntityUtils.getNearbyMobs(mSpawnLoc.clone().subtract(0, 44, 0), 75, EnumSet.of(EntityType.ARMOR_STAND))) {
			Set<String> tags = e.getScoreboardTags();
			for (String tag : tags) {
				switch (tag) {
					default -> {
					}
					case START_TAG -> start = e;
					case NORTH -> mNorthStand = e;
					case EAST -> mEastStand = e;
					case SOUTH -> mSouthStand = e;
					case WEST -> mWestStand = e;
				}
			}
		}

		if (mNorthStand == null || mEastStand == null || mSouthStand == null || mWestStand == null) {
			MMLog.warning("[Eldrask] Failed to find at least one directional armor stand when spawning!");
		}

		mStartLoc = start != null ? start.getLocation() : boss.getLocation();
		mPlayerCount = BossUtils.getPlayersInRangeForHealthScaling(mStartLoc, fighterRange);
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);

		//Adds directions of the arena that seismic ruin destroys
		List<Character> directions = new ArrayList<>();
		directions.add('n');
		directions.add('e');
		directions.add('s');
		directions.add('w');

		mRuin = new UltimateSeismicRuin(mPlugin, mBoss, directions, mNorthStand, mEastStand, mSouthStand, mWestStand);

		//Prevents the boss from getting set on fire (does not prevent it from getting damaged by inferno)
		new BukkitRunnable() {

			@Override
			public void run() {
				mBoss.setFireTicks(0);
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (!mCutsceneDone || mPreventTargetting) {
					return;
				}

				//Teleports boss back if too far in terms of x, y, or z
				if (mBoss.getLocation().getY() - mStartLoc.getY() < -6 || mStartLoc.distance(mBoss.getLocation()) > 36) {
					teleport(mStartLoc);
					mT = 0;
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				if (mStuckLoc == null || mBoss.getLocation().distance(mStuckLoc) > 0.5) {
					mStuckLoc = mBoss.getLocation().clone();
					mT = 0;
				} else if (mT >= 20 * 6 && mBoss.getLocation().distance(mStuckLoc) < 0.5 && ((Creature) mBoss).getTarget() != null) {
					teleport(mStartLoc);
					mT = 0;
				}
				mT += 5;
			}

		}.runTaskTimer(mPlugin, 20 * 5, 5);

		Creature c = (Creature) mBoss;

		//Targetting system
		//Forcefully targets a nearby player if no target
		//After targetting the same player for 30 seconds, play a sound and change targets after 1/2 a second
		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (!mCutsceneDone || mPreventTargetting) {
					return;
				}

				if (c.getTarget() == null || !c.getTarget().equals(mTargeted)) {
					mT = 0;
					mTargeted = c.getTarget();

					if (mTargeted instanceof Player) {
						((Player) mTargeted).playSound(mTargeted.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 5, 0);
					}
				} else if (mT >= 20 * 30 && c.getTarget().equals(mTargeted)) {
					new BukkitRunnable() {
						int mTicks = 0;

						@Override
						public void run() {
							if (mTicks >= 10) {
								mT = 0;
								this.cancel();

								//List is farthest players in the beginning, and nearest players at the end
								List<Player> players = EntityUtils.getNearestPlayers(mStartLoc, detectionRange);
								players.removeIf(p -> p.getGameMode() == GameMode.CREATIVE || p.getScoreboardTags().contains("disable_class"));
								for (Player player : players) {
									if (mTargeted == null || !player.getUniqueId().equals(mTargeted.getUniqueId())) {
										c.setTarget(player);
										mTargeted = player;
										break;
									}
								}
							} else {
								mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 5, 0);
							}
							mTicks += 5;
						}

					}.runTaskTimer(mPlugin, 0, 5);
				}
				mT += 5;

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

			}

		}.runTaskTimer(mPlugin, 0, 5);

		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : PlayerUtils.playersInRange(mStartLoc, detectionRange, true)) {
					if (player.isSleeping() && player.getGameMode() != GameMode.ADVENTURE) {
						DamageUtils.damage(mBoss, player, DamageType.OTHER, 22);
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, PLAYER_ANTICHEESE_SLOWNESS_SRC,
							new PercentSpeed(20 * 15, -0.3, PLAYER_ANTICHEESE_SLOWNESS_SRC));
						player.sendMessage(Component.text("YOU DARE MOCK OUR BATTLE?", NamedTextColor.DARK_AQUA));
						player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.HOSTILE, 1, 0.85f);
					}
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 5);

		SpellManager phase1Spells = new SpellManager(Arrays.asList(
			new SpellAirGolemStrike(mPlugin, mBoss, mStartLoc),
			new Shatter(mPlugin, mBoss, 3f, mStartLoc),
			new SpellGlacialPrison(mPlugin, mBoss, fighterRange, mStartLoc),
			new RingOfFrost(mPlugin, mBoss, 12, mStartLoc)
		));

		SpellManager phase2Spells = new SpellManager(Arrays.asList(
			new Shatter(mPlugin, mBoss, 3f, mStartLoc),
			new SpellAirGolemStrike(mPlugin, mBoss, mStartLoc),
			new SpellGreatswordSlam(mPlugin, mBoss, frostedIceDuration - 5, 90, mStartLoc),
			new SpellGreatswordSlam(mPlugin, mBoss, frostedIceDuration - 5, 90, mStartLoc),
			new SpellSpinDown(mPlugin, mBoss, mStartLoc),
			new SpellSpinDown(mPlugin, mBoss, mStartLoc)
		));

		SpellManager phase3Spells = new SpellManager(Arrays.asList(
			new Shatter(mPlugin, mBoss, 3f, mStartLoc),
			new SpellTitanicRupture(mPlugin, mBoss, mStartLoc),
			new SpellFrostRift(mPlugin, mBoss, mStartLoc),
			new SpellGreatswordSlam(mPlugin, mBoss, 20, 90, mStartLoc)
		));

		SpellManager phase4Spells = new SpellManager(Arrays.asList(
			new Shatter(mPlugin, mBoss, 3f, mStartLoc),
			new SpellTitanicRupture(mPlugin, mBoss, mStartLoc),
			new SpellFrostRift(mPlugin, mBoss, mStartLoc),
			new SpellGreatswordSlam(mPlugin, mBoss, 20, 60, mStartLoc)
		));

		List<Spell> phase1PassiveSpells = Arrays.asList(
			new ArmorOfFrost(mPlugin, mBoss, this, 2),
			new SpellPurgeNegatives(mBoss, 20 * 4),
			new SpellFrostGiantBlockBreak(mBoss, 5, 15, 5, mStartLoc),
			new SpellHailstorm(mPlugin, mBoss, hailstormRadius, mStartLoc),
			new SpellFrostbite(mPlugin, mBoss, mStartLoc),
			new GiantStomp(mPlugin, mBoss),
			new SpellRemoveLevitation(mBoss)
		);

		List<Spell> phase2PassiveSpells = Arrays.asList(
			new ArmorOfFrost(mPlugin, mBoss, this, 2),
			new SpellPurgeNegatives(mBoss, 20 * 3),
			new SpellFrostGiantBlockBreak(mBoss, 5, 15, 5, mStartLoc),
			new SpellHailstorm(mPlugin, mBoss, hailstormRadius, mStartLoc),
			new SpellFrostbite(mPlugin, mBoss, mStartLoc),
			new SpellFrostedIceBreak(mBoss),
			new GiantStomp(mPlugin, mBoss),
			new SpellRemoveLevitation(mBoss)
		);
		List<Spell> phase3PassiveSpells = Arrays.asList(
			new ArmorOfFrost(mPlugin, mBoss, this, 1),
			new SpellPurgeNegatives(mBoss, 20 * 2),
			new SpellFrostGiantBlockBreak(mBoss, 5, 15, 5, mStartLoc),
			new SpellHailstorm(mPlugin, mBoss, hailstormRadius, mStartLoc),
			new SpellFrostbite(mPlugin, mBoss, mStartLoc),
			new SpellFrostedIceBreak(mBoss),
			new GiantStomp(mPlugin, mBoss),
			new SpellRemoveLevitation(mBoss)
		);

		List<Spell> phase4PassiveSpells = Arrays.asList(
			new ArmorOfFrost(mPlugin, mBoss, this, 1, false),
			new SpellPurgeNegatives(mBoss, 20 * 2),
			new SpellFrostGiantBlockBreak(mBoss, 5, 15, 5, mStartLoc),
			new SpellHailstorm(mPlugin, mBoss, hailstormRadius, mStartLoc),
			new SpellFrostbite(mPlugin, mBoss, mStartLoc),
			new SpellFrostedIceBreak(mBoss),
			new GiantStomp(mPlugin, mBoss),
			new SpellRemoveLevitation(mBoss)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();
		events.put(100, mBoss -> {
			mPreventTargetting = false;
			sendDialogue(1);
			sendDialogue(2);

			//Changes held weapon to bone wand
			ItemStack wand = new ItemStack(Material.BONE);
			if (mBoss.getEquipment().getItemInMainHand().getType() != Material.AIR) {
				wand = mBoss.getEquipment().getItemInMainHand();
			} else {
				wand = modifyItemName(wand, "Frost Giant's Staff", NamedTextColor.AQUA, true);
			}
			ItemMeta im = wand.getItemMeta();
			im.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("generic.attack_damage", -100, AttributeModifier.Operation.ADD_NUMBER));
			wand.setItemMeta(im);
			mBoss.getEquipment().setItemInMainHand(wand);
		});

		events.put(66, mBoss -> {
			sendDialogue(3);
			//Manually cancel Armor of Frost's cooldown mechanic
			for (Spell sp : phase1PassiveSpells) {
				if (sp instanceof ArmorOfFrost) {
					((ArmorOfFrost) sp).stopSkill();
					break;
				}
			}
			changePhase(phase2Spells, phase2PassiveSpells, null);
			mFrostArmorActive = true;
			changeArmorPhase(mBoss.getEquipment(), false);
			mPreventTargetting = false;
			mBoss.setAI(true);
			mRuin.run();
			teleport(mStartLoc);
			sendDialogue(4);

			//Changes held weapon to iron sword
			ItemStack sword = modifyItemName(new ItemStack(Material.IRON_SWORD), "Frost Giant's Greatsword", NamedTextColor.AQUA, true);
			ItemMeta im = sword.getItemMeta();
			im.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("generic.attack_damage", -100, AttributeModifier.Operation.ADD_NUMBER));
			sword.setItemMeta(im);
			mBoss.getEquipment().setItemInMainHand(sword);
		});

		//Phase 3
		events.put(33, mBoss -> {
			sendDialogue(5);
			//Manually cancel Armor of Frost's cooldown mechanic
			for (Spell sp : phase2PassiveSpells) {
				if (sp instanceof ArmorOfFrost) {
					((ArmorOfFrost) sp).stopSkill();
					break;
				}
			}
			changePhase(phase3Spells, phase3PassiveSpells, null);
			mFrostArmorActive = true;
			changeArmorPhase(mBoss.getEquipment(), false);
			mPreventTargetting = false;
			mBoss.setAI(true);
			mRuin.run();
			teleport(mStartLoc);
			sendDialogue(6);

			//Changes held weapon to iron axe
			ItemStack axe = modifyItemName(new ItemStack(Material.IRON_AXE), "Frost Giant's Crusher", NamedTextColor.AQUA, true);
			ItemMeta im = axe.getItemMeta();
			im.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("generic.attack_damage", -100, AttributeModifier.Operation.ADD_NUMBER));
			axe.setItemMeta(im);
			mBoss.getEquipment().setItemInMainHand(axe);
		});

		//Third and fourth seismic ruin
		events.put(15, mBoss -> {
			sendDialogue(7);
			mFrostArmorActive = true;
			changeArmorPhase(mBoss.getEquipment(), false);
			changePhase(phase4Spells, phase4PassiveSpells, null);
			mBoss.setAI(true);
			for (Spell sp : phase3PassiveSpells) {
				if (sp instanceof ArmorOfFrost) {
					((ArmorOfFrost) sp).stopSkill();
					break;
				}
			}
			mRuin.run();
			mRuin.run();
			teleport(mStartLoc);

			//Changes held weapon to iron scythe
			ItemStack scythe = modifyItemName(new ItemStack(Material.IRON_HOE), "Frost Giant's Crescent", NamedTextColor.AQUA, true);
			ItemMeta im = scythe.getItemMeta();
			im.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier("generic.attack_damage", -100, AttributeModifier.Operation.ADD_NUMBER));
			scythe.setItemMeta(im);
			mBoss.getEquipment().setItemInMainHand(scythe);
		});

		//Show hailstorm before fight starts
		new BukkitRunnable() {
			final Creature mC = (Creature) mBoss;

			@Override
			public void run() {
				Location loc = mStartLoc.clone();
				for (double degree = 0; degree < 360; degree += 8) {
					double radian = Math.toRadians(degree);
					double cos = FastUtils.cos(radian);
					double sin = FastUtils.sin(radian);
					loc.add(cos * (hailstormRadius + 5), 2.5, sin * (hailstormRadius + 5));
					new PartialParticle(Particle.CLOUD, loc, 3, 3, 1, 4, 0.075).spawnAsEntityActive(boss);
					new PartialParticle(Particle.CLOUD, loc, 3, 3, 4, 4, 0.075).spawnAsEntityActive(boss);
					new PartialParticle(Particle.REDSTONE, loc, 3, 3, 4, 4, 0.075, BLUE_COLOR).spawnAsEntityActive(boss);
					new PartialParticle(Particle.REDSTONE, loc, 3, 3, 1, 4, 0.075, BLUE_COLOR).spawnAsEntityActive(boss);
					loc.subtract(cos * (hailstormRadius + 5), 2.5, sin * (hailstormRadius + 5));
				}

				for (double degree = 0; degree < 360; degree++) {
					if (FastUtils.RANDOM.nextDouble() < 0.3) {
						double radian = Math.toRadians(degree);
						double cos = FastUtils.cos(radian);
						double sin = FastUtils.sin(radian);
						loc.add(cos * hailstormRadius, 0.5, sin * hailstormRadius);
						new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, LIGHT_BLUE_COLOR).spawnAsEntityActive(boss);
						loc.subtract(cos * hailstormRadius, 0.5, sin * hailstormRadius);
					}
				}

				mC.setTarget(null);
				if (mCutsceneDone) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 10);

		mBoss.setGravity(false);
		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		world.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 6, 0.5f);
		world.playSound(mStartLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 6, 0.5f);
		sendDialogue(0);
		mBoss.setInvisible(true);

		new BukkitRunnable() {
			final Location mLoc = mStartLoc.clone();
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 1.5;
				for (double degree = 0; degree < 360; degree += 5) {
					double radian = Math.toRadians(degree);
					mLoc.add(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
					new PartialParticle(Particle.CLOUD, mLoc, 4, 1, 1, 1, 0.35).spawnAsEntityActive(boss);
					mLoc.subtract(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
				}
				if (mRadius >= 40) {
					this.cancel();

					//Grow the FG statue using growables
					try {
						GrowableAPI.grow("FrostGiantStatue", mStartLoc, 1, 2, false);
					} catch (Exception e) {
						e.printStackTrace();
					}

					new BukkitRunnable() {
						int mTicks = 0;
						int mRotation = 0;

						@Override
						public void run() {
							mTicks += 2;
							if (mTicks >= 20 * 8) {
								this.cancel();
							}

							for (int y = 0; y <= 12; y += 3) {
								double rad1 = Math.toRadians(mRotation);
								Location loc1 = mStartLoc.clone().add(FastUtils.cos(rad1) * 3, y, FastUtils.sin(rad1));
								double rad2 = Math.toRadians(mRotation + 180);
								Location loc2 = mStartLoc.clone().add(FastUtils.cos(rad2) * 3, y, FastUtils.sin(rad2));

								new PartialParticle(Particle.SPELL_INSTANT, loc1, 5, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(boss);
								new PartialParticle(Particle.SPELL_INSTANT, loc2, 5, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(boss);
							}
							mRotation += 10;
							if (mRotation >= 360) {
								mRotation = 0;
							}

							if (mTicks % 20 == 0) {
								world.playSound(mStartLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 3, 0.25f);
							}

							if (mTicks % 10 == 0) {
								new PartialParticle(Particle.BLOCK_DUST, mStartLoc, 40, 2, 0.35, 2, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(boss);
								new PartialParticle(Particle.BLOCK_DUST, mStartLoc, 75, 5, 0.35, 5, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(boss);
								new PartialParticle(Particle.EXPLOSION_NORMAL, mStartLoc, 15, 5, 0.35, 5, 0.15).spawnAsEntityActive(boss);
								new PartialParticle(Particle.CLOUD, mStartLoc, 20, 5, 0.35, 5, 0.15).spawnAsEntityActive(boss);
							}
						}
					}.runTaskTimer(mPlugin, 0, 2);

					new BukkitRunnable() {
						@Override
						public void run() {
							world.playSound(mStartLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);


							new BukkitRunnable() {
								final Location mLoc = mStartLoc.clone();
								double mR = 0;

								@Override
								public void run() {
									if (mR == 0) {
										mBoss.getEquipment().setArmorContents(mArmor);
										mBoss.getEquipment().setItemInMainHand(mMainhand);
										mBoss.getEquipment().setItemInOffHand(mOffhand);
										Location startLoc = mStartLoc;
										Location l = startLoc.clone();
										for (int y = 15; y >= 0; y--) {
											for (int x = -5; x <= 5; x++) {
												for (int z = -5; z <= 5; z++) {
													l.set(startLoc.getX() + x, startLoc.getY() + y, startLoc.getZ() + z);
													l.getBlock().setType(Material.AIR);
												}
											}
										}

										mBoss.removePotionEffect(PotionEffectType.INVISIBILITY);
										mBoss.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
										mCutsceneDone = true;
										world.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 6, 0.5f);
										world.playSound(mStartLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 6, 0.5f);

										for (Player player : PlayerUtils.playersInRange(mStartLoc, detectionRange, true)) {
											MessagingUtils.sendBoldTitle(player, Component.text("Eldrask", NamedTextColor.AQUA), Component.text("The Waking Giant", NamedTextColor.BLUE));
											player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.75f);
										}

										BossBarManager bossBar = new BossBarManager(plugin, boss, detectionRange, BarColor.BLUE, BarStyle.SEGMENTED_10, events, false);
										constructBoss(phase1Spells, phase1PassiveSpells, detectionRange, bossBar, 20 * 10);

										mBoss.setGravity(true);
										mBoss.setAI(true);
										mBoss.setInvulnerable(false);
										mBoss.setInvisible(false);

										mBoss.teleport(mStartLoc);
									}
									mR++;
									for (double degree = 0; degree < 360; degree += 5) {
										double radian = Math.toRadians(degree);
										mLoc.add(FastUtils.cos(radian) * mR, 1, FastUtils.sin(radian) * mR);
										new PartialParticle(Particle.CLOUD, mLoc, 4, 1, 1, 1, 0.35).spawnAsEntityActive(boss);
										mLoc.subtract(FastUtils.cos(radian) * mR, 1, FastUtils.sin(radian) * mR);
									}
									if (mR >= 40) {
										this.cancel();

									}
								}
							}.runTaskTimer(plugin, 20 * 2, 1);
						}
					}.runTaskLater(mPlugin, 20 * 8);
				}
			}
		}.runTaskTimer(mPlugin, 1, 1);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);
		if (players.size() == 0) {
			return;
		}
		sendDialogue(8);

		List<Spell> passives = getPassives();
		if (passives != null) {
			for (Spell sp : passives) {
				if (sp instanceof ArmorOfFrost) {
					((ArmorOfFrost) sp).stopSkill();
				}
			}
		}

		BossUtils.endBossFightEffects(mBoss, players, 20 * 40, true, false);
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		teleport(mStartLoc);

		if (event != null) {
			event.setCancelled(true);
			event.setReviveHealth(100);
		}

		Location loc = mBoss.getLocation().clone();
		for (double degree = 0; degree < 360; degree += 5) {
			double radian = Math.toRadians(degree);
			loc.add(FastUtils.cos(radian), 1, FastUtils.sin(radian));
			new PartialParticle(Particle.CLOUD, loc, 4, 1, 1, 1, 0.35).spawnAsEntityActive(mBoss);
			loc.subtract(FastUtils.cos(radian), 1, FastUtils.sin(radian));
		}

		World world = mBoss.getWorld();
		new BukkitRunnable() {
			@Override
			public void run() {
				mBoss.remove();

				//Instantly spawn the FG statue
				try {
					GrowableAPI.grow("FrostGiantStatue", mStartLoc, 1, 300, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
				sendDialogue(9);
				world.playSound(mStartLoc, Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 3, 0);

				//Initiate the growable "melt" which converts the blocks of the giant into barriers
				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							GrowableAPI.grow("FrostGiantStatueBarrier2", mStartLoc.clone().add(0, 13, 1), 1, 2, false);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.runTaskLater(mPlugin, 20);
			}
		}.runTaskLater(mPlugin, 20 * 3);

		new BukkitRunnable() {
			final Location mLoc = mStartLoc.clone();
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 1.5;
				for (double degree = 0; degree < 360; degree += 5) {
					double radian = Math.toRadians(degree);
					mLoc.add(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
					new PartialParticle(Particle.CLOUD, mLoc, 4, 1, 1, 1, 0.35).spawnAsEntityActive(mBoss);
					mLoc.subtract(FastUtils.cos(radian) * mRadius, 1, FastUtils.sin(radian) * mRadius);
				}
				if (mRadius >= 40) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 2f;
			int mRotation = 360;
			//The end totem green particle sequence
			boolean mEndingParticles = false;

			@Override
			public void run() {

				if (mTicks <= 20 * 2) {

					if (mTicks % 10 == 0) {
						world.playSound(mStartLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
					}

					new PartialParticle(Particle.EXPLOSION_LARGE, mStartLoc.clone().add(0, 5, 0), 1, 1, 5, 1).minimumCount(1).spawnAsEntityActive(mBoss);
				}

				if (mTicks >= 20 * 4 && mTicks <= 20 * 10 && mTicks % 2 == 0) {
					world.playSound(mStartLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 3, mPitch);

					for (int y = 0; y <= 12; y += 3) {
						double rad1 = Math.toRadians(mRotation);
						Location loc1 = mBoss.getLocation().clone().add(FastUtils.cos(rad1) * 3, y, FastUtils.sin(rad1));
						double rad2 = Math.toRadians(mRotation + 180);
						Location loc2 = mBoss.getLocation().clone().add(FastUtils.cos(rad2) * 3, y, FastUtils.sin(rad2));

						new PartialParticle(Particle.SPELL_WITCH, loc1, 5, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.SPELL_WITCH, loc2, 5, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
					}
					mRotation -= 20;
					if (mRotation <= 0) {
						mRotation = 360;
					}
				}
				mPitch -= 0.025f;

				Location startLoc = mStartLoc;

				if (mTicks >= 20 * 4 && mTicks <= 20 * 10 && mTicks % 10 == 0) {
					world.playSound(mStartLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2, FastUtils.RANDOM.nextFloat());
					world.playSound(mStartLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 2, mPitch);
					new PartialParticle(Particle.BLOCK_DUST, startLoc, 100, 2, 0.35, 2, 0.25, Material.BLUE_ICE.createBlockData()).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.BLOCK_DUST, startLoc, 100, 2, 0.35, 2, 0.25, Material.IRON_TRAPDOOR.createBlockData()).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.DRAGON_BREATH, startLoc.clone().add(0, 1, 0), 25, 1, 1, 1, 0.25).spawnAsEntityActive(mBoss);
				}

				if (!mEndingParticles && mTicks >= 20 * 10 /*&& (startLoc.getBlock().getType() == Material.BARRIER || startLoc.getBlock().getType() == Material.AIR) */) {
					new PartialParticle(Particle.VILLAGER_HAPPY, startLoc.clone().add(0, 5, 0), 300, 1, 5, 1, 0.25).spawnAsEntityActive(mBoss);
					mEndingParticles = true;
				}

				if (mTicks >= 20 * 14) {
					//Delete barriers after cutscene melt
					Location l = startLoc.clone();
					for (int y = 15; y >= 0; y--) {
						for (int x = -5; x <= 5; x++) {
							for (int z = -5; z <= 5; z++) {
								l.set(startLoc.getX() + x, startLoc.getY() + y, startLoc.getZ() + z);
								l.getBlock().setType(Material.AIR);
							}
						}
					}

					this.cancel();

					for (Player player : PlayerUtils.playersInRange(mSpawnLoc, fighterRange, true)) {
						MessagingUtils.sendBoldTitle(player, Component.text("VICTORY", NamedTextColor.AQUA), Component.text("Eldrask, The Waking Giant", NamedTextColor.DARK_AQUA));
						player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 100, 0.8f);
					}
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

				}
				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 1, 1);
	}

	public static boolean testHitByIcicle(BoundingBox icicleBoundingBox) {
		if (mInstance != null && mInstance.mBoss.isValid() && !mInstance.mBoss.isDead()) {
			if (icicleBoundingBox.overlaps(mInstance.mBoss.getBoundingBox())) {
				List<Spell> passives = mInstance.getPassives();
				if (passives != null) {
					for (Spell sp : passives) {
						if (sp instanceof ArmorOfFrost) {
							((ArmorOfFrost) sp).hitByIcicle();
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	//If cracked = true, convert armor to cracked variant
	//If cracked = false, convert armor to uncracked variant
	public static void changeArmorPhase(EntityEquipment equip, boolean cracked) {
		if (cracked) {
			equip.setHelmet(modifyItemName(equip.getHelmet(), "Cracked Giant's Crown", NamedTextColor.AQUA, false));
			equip.setChestplate(modifyItemName(equip.getChestplate(), "Cracked Giant's Courage", NamedTextColor.AQUA, false));
			equip.setLeggings(modifyItemName(equip.getLeggings(), "Cracked Giant's Leggings", NamedTextColor.AQUA, false));
			equip.setBoots(modifyItemName(equip.getBoots(), "Cracked Giant's Boots", NamedTextColor.AQUA, false));
		} else {
			equip.setHelmet(modifyItemName(equip.getHelmet(), "Frost Giant's Crown", NamedTextColor.AQUA, false));
			equip.setChestplate(modifyItemName(equip.getChestplate(), "Frost Giant's Courage", NamedTextColor.AQUA, false));
			equip.setLeggings(modifyItemName(equip.getLeggings(), "Frost Giant's Leggings", NamedTextColor.AQUA, false));
			equip.setBoots(modifyItemName(equip.getBoots(), "Frost Giant's Boots", NamedTextColor.AQUA, false));
		}
	}

	private static ItemStack modifyItemName(ItemStack item, String newName, TextColor color, boolean isUnderlined) {
		ItemMeta im = item.getItemMeta();
		im.displayName(Component.text(newName, color, TextDecoration.BOLD)
			               .decoration(TextDecoration.ITALIC, false)
			               .decoration(TextDecoration.UNDERLINED, isUnderlined));
		item.setItemMeta(im);
		ItemUtils.setPlainName(item, newName);

		return item;
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		if (!mCutsceneDone) {
			event.setCancelled(true);
			event.setTarget(null);
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		//The "default" Giant attacks need to be cancelled so it does not trigger evasion
		if (event.getDamage() <= 0) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		event.setDamage(event.getDamage() / mDefenseScaling);
		LivingEntity source = event.getSource();
		if (!mFrostArmorActive) {
			if (source instanceof Player player) {
				player.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 5, 0.75f);
			}
		}
		//Punch resist
		if (event.getDamager() instanceof Projectile proj) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mBoss.setVelocity(new Vector(0, 0, 0));
					mTicks += 1;
					if (mTicks > 2) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);

			//Check if arrow shot came from arena
			if (proj.getShooter() instanceof Player player && player.getLocation().distance(mStartLoc) > FrostGiant.fighterRange) {
				event.setCancelled(true);
			}
		}
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		mPlayerCount = BossUtils.getPlayersInRangeForHealthScaling(mStartLoc, detectionRange);
		mDefenseScaling = BossUtils.healthScalingCoef(mPlayerCount, SCALING_X, SCALING_Y);
	}

	@Override
	public void nearbyBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.FROSTED_ICE) {
			Location loc = event.getBlock().getLocation();
			Location tempLoc = loc.clone();
			for (int z = -1; z <= 1; z++) {
				for (int y = -1; y <= 1; y++) {
					for (int x = -1; x <= 1; x++) {
						tempLoc.set(loc.getX() + x, loc.getY() + y, loc.getZ() + z);

						if (tempLoc.getBlock().getType() == Material.FROSTED_ICE) {
							tempLoc.getBlock().setType(Material.CRACKED_STONE_BRICKS);
						}
					}
				}
			}

			event.setCancelled(true);
		}
	}

	@Override
	public boolean hasNearbyBlockBreakTrigger() {
		return true;
	}

	//Teleport with special effects
	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);

		//Both abilities delayed by 1.5s
		//Delays damage for melee
		delayHailstormDamage();
	}

	public static void delayHailstormDamage() {
		//Delays damage for hailstorm
		if (mInstance != null) {
			List<Spell> passives = mInstance.getPassives();
			if (passives != null) {
				for (Spell sp : passives) {
					if (sp instanceof SpellHailstorm) {
						((SpellHailstorm) sp).delayDamage();
					}
				}
			}
		}
	}

	@Override
	public void init() {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true);

		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(MAX_HEALTH);
		mBoss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(detectionRange);
		mBoss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
		mBoss.setHealth(MAX_HEALTH);
		EntityEquipment equips = mBoss.getEquipment();
		mArmor = equips.getArmorContents();
		mMainhand = equips.getItemInMainHand();
		mOffhand = equips.getItemInOffHand();
		mBoss.getEquipment().clear();
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 9999, 0));
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 9999, 10));

		mBoss.setPersistent(true);

		for (Player player : players) {
			if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
				player.removePotionEffect(PotionEffectType.GLOWING);
			}
		}
	}

	//Punch Resist
	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		mBoss.setVelocity(new Vector(0, 0, 0));
	}

	//Golem Stun on certain ability casts from boss.
	public static void freezeGolems(LivingEntity mBoss) {
		castStomp = false;
		mBoss.addScoreboardTag("GolemFreeze");
		Location loc = mBoss.getLocation();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, FrostGiant.detectionRange)) {
			if (mob.getType() == EntityType.IRON_GOLEM) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mob, GOLEM_FREEZE_EFFECT_NAME,
					new PercentSpeed(20 * 20, -1, GOLEM_FREEZE_EFFECT_NAME));
				mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 10));
			}
		}
	}

	public static void unfreezeGolems(LivingEntity mBoss) {
		castStomp = true;
		if (mBoss.getScoreboardTags().contains("GolemFreeze")) {
			Location loc = mBoss.getLocation();
			mBoss.removeScoreboardTag("GolemFreeze");
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, FrostGiant.detectionRange)) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mob, GOLEM_FREEZE_EFFECT_NAME);
				mob.removePotionEffect(PotionEffectType.GLOWING);
			}
		}
	}

	private void sendDialogue(int dialogueIndex) {
		for (Player player : PlayerUtils.playersInRange(mStartLoc, detectionRange, true)) {
			player.sendMessage(fightDialogue[dialogueIndex]);
		}
	}
}
