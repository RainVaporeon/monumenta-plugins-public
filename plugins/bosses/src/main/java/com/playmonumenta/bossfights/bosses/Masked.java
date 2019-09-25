package com.playmonumenta.bossfights.bosses;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.bossfights.BossBarManager;
import com.playmonumenta.bossfights.BossBarManager.BossHealthAction;
import com.playmonumenta.bossfights.SpellManager;
import com.playmonumenta.bossfights.spells.Spell;
import com.playmonumenta.bossfights.spells.SpellBaseLaser;
import com.playmonumenta.bossfights.spells.SpellBlockBreak;
import com.playmonumenta.bossfights.spells.SpellConditionalTeleport;
import com.playmonumenta.bossfights.spells.SpellPushPlayersAway;
import com.playmonumenta.bossfights.spells.spells_masked.SpellFrostNova;
import com.playmonumenta.bossfights.spells.spells_masked.SpellShadowGlade;
import com.playmonumenta.bossfights.spells.spells_masked.SpellSummonBlazes;
import com.playmonumenta.bossfights.utils.DamageUtils;
import com.playmonumenta.bossfights.utils.SerializationUtils;
import com.playmonumenta.bossfights.utils.Utils;

public class Masked extends BossAbilityGroup {

	public static final String identityTag = "boss_masked";
	public static final int DETECTION_RANGE = 50;

	private static final double MOVEMENT_SPEED = 0.25;
	private static final int MAXIMUM_BASE_HEALTH = 1024;
	private static final int TIMER_INCREMENT = 20 * 1;
	private static final int TIME_SPAWN = 0;
	private static final int TIME_TITLE = 20 * 2;
	private static final int TIME_BEGIN = 20 * 8;

	private static final String SPAWN_DIALOG_COMMAND = "tellraw @s [\"\",{\"text\":\"[Masked Man]\",\"color\":\"gold\"},{\"text\":\" Beautiful, isn't it. A Black Shard, shorn from the Black Wool itself. I don't know how you survived me once, but I am impressed.\"}]";
	private static final String BEGIN_DIALOG_COMMAND = "tellraw @s [\"\",{\"text\":\"[Masked Man]\",\"color\":\"gold\"},{\"text\":\" However, you have interfered with our plans. Nothing will stop us! Die!\"}]";
	private static final String PHASE_CHANGE_DIALOG_COMMAND = "tellraw @s [\"\",{\"text\":\"[Masked Man]\",\"color\":\"gold\"},{\"text\":\" Know that even with my death our plans will not stop. The Masked are unstoppable!\"}]";
	private static final String DEATH_DIALOG_COMMAND = "tellraw @s [\"\",{\"text\":\"[Masked Man]\",\"color\":\"gold\"},{\"text\":\" Hah... My death won't stop the shard... We will not fail... We will not fail Lord Calder...\"}]";
	private static final String TITLE_TIME_COMMAND = "title @s times 15 100 15";
	private static final String TITLE_SUBTITLE_COMMAND = "title @s subtitle {\"text\":\"Harbinger of Shadow\",\"color\":\"light_purple\"}";
	private static final String TITLE_TITLE_COMMAND = "title @s title {\"text\":\"The Masked Man\",\"bold\":true,\"color\":\"light_purple\"}";

	private final LivingEntity mBoss;
	private final World mWorld;
	private final Location mSpawnLoc;
	private final Location mEndLoc;
	private final ItemStack mMeleeWeapon;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return SerializationUtils.statefulBossDeserializer(boss, identityTag, (spawnLoc, endLoc) -> {
			return new Masked(plugin, boss, spawnLoc, endLoc);
		});
	}

	@Override
	public String serialize() {
		return SerializationUtils.statefulBossSerializer(mSpawnLoc, mEndLoc);
	}

	public Masked(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		mPlugin = plugin;
		mWorld = boss.getWorld();
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mEndLoc = endLoc;

		// Store the Arcane Gladius to a variable for phase 2
		mMeleeWeapon = mBoss.getEquipment().getItemInMainHand();

		mBoss.setRemoveWhenFarAway(false);

		mBoss.setGravity(false);
		mBoss.setInvulnerable(true);
		mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);

		mBoss.addScoreboardTag("Boss");

		new BukkitRunnable() {
			int t = 0;

			@Override
			public void run() {
				if (t == TIME_SPAWN) {
					Utils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, SPAWN_DIALOG_COMMAND);
					mWorld.spawnParticle(Particle.DRAGON_BREATH, mSpawnLoc, 50, 0.5, 0.5, 0.5, 0.02);
					mWorld.playSound(mSpawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 2f, 1f);
				} else if (t == TIME_TITLE) {
					Utils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, TITLE_TIME_COMMAND);
					Utils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, TITLE_SUBTITLE_COMMAND);
					Utils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, TITLE_TITLE_COMMAND);
				} else if (t == TIME_BEGIN) {
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);
					// Swap weapon to bow for phase 1
					ItemStack item = new ItemStack(Material.BOW, 1);
					item.addEnchantment(Enchantment.ARROW_DAMAGE, 3);
					mBoss.getEquipment().setItemInMainHand(item);
					Utils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, BEGIN_DIALOG_COMMAND);
					resumeBossFight();
					this.cancel();
				}

				t += TIMER_INCREMENT;
			}
		}.runTaskTimer(mPlugin, 0, TIMER_INCREMENT);
	}

	@Override
	public void init() {
		int playerCount = Utils.playersInRange(mBoss.getLocation(), DETECTION_RANGE).size();
		int armor = (int)(Math.sqrt(playerCount * 2) - 1);
		int health = (int)((1 - Math.pow(0.5, playerCount)) * MAXIMUM_BASE_HEALTH);

		mBoss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
		mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
		mBoss.setHealth(health);
	}

	@Override
	public void death() {
		mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);

		Utils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, DEATH_DIALOG_COMMAND);
	}

	private void resumeBossFight() {
		SpellManager activeSpells1 = new SpellManager(Arrays.asList(
			new SpellBaseLaser(mPlugin, mBoss, 40, 120, true, false, 160,
			                   // Tick action per player
			                   (Player player, int ticks, boolean blocked) -> {
			                       player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (ticks / 80f) * 1.5f);
			                       mBoss.getLocation().getWorld().playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, 2, 0.5f + (ticks / 80f) * 1.5f);
			                       if (!blocked && ticks > 0 && ticks % 20 == 0) {
			                           DamageUtils.damage(mBoss, player, 5);
			                       }
			                   },
			                   // Particles generated by the laser
			                   (Location loc) -> {
			                       loc.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.02, 0.02, 0.02, 0);
			                       loc.getWorld().spawnParticle(Particle.SPELL_MOB, loc, 1, 0.02, 0.02, 0.02, 1);
			                   },
			                   null),
			new SpellShadowGlade(mPlugin, mBoss.getLocation(), 2),
			new SpellSummonBlazes(mPlugin, mBoss)
		));

		SpellManager activeSpells2 = new SpellManager(Arrays.asList(
			new SpellFrostNova(mPlugin, mBoss, 9, 70),
			new SpellShadowGlade(mPlugin, mSpawnLoc, 2),
			new SpellSummonBlazes(mPlugin, mBoss)
		));

		List<Spell> passiveSpells1 = Arrays.asList(
			new SpellBlockBreak(mBoss),
			new SpellPushPlayersAway(mBoss, 7, 15),
			// Teleport the boss to spawnLoc whenever "true" (always)
			new SpellConditionalTeleport(mBoss, mSpawnLoc, b -> true)
		);

		List<Spell> passiveSpells2 = Arrays.asList(
			new SpellBlockBreak(mBoss),
			// Teleport the boss to spawnLoc whenever condition is true
			new SpellConditionalTeleport(mBoss, mSpawnLoc, b -> b.getLocation().getY() < 157)
		);

		Map<Integer, BossHealthAction> events = new HashMap<Integer, BossHealthAction>();

		events.put(50, mBoss -> {
			changePhase(activeSpells2, passiveSpells2, null);
			mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED);
			// Put sword back in mainhand
			mBoss.getEquipment().setItemInMainHand(mMeleeWeapon);
			Utils.executeCommandOnNearbyPlayers(mSpawnLoc, DETECTION_RANGE, PHASE_CHANGE_DIALOG_COMMAND);
		});

		BossBarManager bossBar = new BossBarManager(mPlugin, mBoss, DETECTION_RANGE, BarColor.WHITE, BarStyle.SEGMENTED_10, events);
		super.constructBoss(mPlugin, identityTag, mBoss, activeSpells1, passiveSpells1, DETECTION_RANGE, bossBar);
	}
}
