package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.ManaLanceCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ManaLance extends MultipleChargeAbility {

	private static final float DAMAGE_1 = 6.0f;
	private static final float DAMAGE_2 = 7.0f;
	private static final int COOLDOWN_1 = 5 * 20;
	private static final int COOLDOWN_2 = 3 * 20;
	private static final int RANGE = 8;
	private static final float KNOCKBACK = 0.25f;

	public static final String CHARM_DAMAGE = "Mana Lance Damage";
	public static final String CHARM_COOLDOWN = "Mana Lance Cooldown";
	public static final String CHARM_RANGE = "Mana Lance Range";
	public static final String CHARM_CHARGES = "Mana Lance Charge";

	public static final AbilityInfo<ManaLance> INFO =
		new AbilityInfo<>(ManaLance.class, "Mana Lance", ManaLance::new)
			.linkedSpell(ClassAbility.MANA_LANCE)
			.scoreboardId("ManaLance")
			.shorthandName("ML")
			.descriptions(
				String.format("Right clicking with a wand fires forth a piercing beam of Mana going %s blocks, dealing %s arcane magic damage to enemies in the path of the beam. " +
					              "This beam will not go through solid blocks. Cooldown: %ss.",
					RANGE,
					(int) DAMAGE_1,
					COOLDOWN_1 / 20
				),
				String.format("The beam instead deals %s damage. Cooldown: %ss.",
					(int) DAMAGE_2,
					COOLDOWN_2 / 20),
				"Mana Lance now has two charges.")
			.simpleDescription("Shoot a beam that damages mobs in its path.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ManaLance::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(false),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.TRIDENT);

	private final float mDamage;
	private int mLastCastTicks = 0;

	private final ManaLanceCS mCosmetic;

	public ManaLance(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mMaxCharges = (isEnhanced() ? 2 : 1) + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mCharges = getTrackedCharges();
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ManaLanceCS());
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return false;
		}
		mLastCastTicks = ticks;

		float damage = mDamage;
		damage = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
		damage = SpellPower.getSpellDamage(mPlugin, mPlayer, damage);

		Location startLoc = mPlayer.getEyeLocation();
		double range = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RANGE);
		World world = mPlayer.getWorld();

		Location endLoc = LocationUtils.rayTraceToBlock(mPlayer, range, loc -> mCosmetic.lanceHitBlock(mPlayer, loc, world));

		boolean hit = false;
		for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, 0.7, true).accuracy(0.5).getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true);

			MovementUtils.knockAway(mPlayer.getLocation(), mob, KNOCKBACK, KNOCKBACK, true);

			if (!hit) {
				mCosmetic.lanceHit(LocationUtils.getHalfHeightLocation(mob), mPlayer);
				hit = true;
			}
		}

		mCosmetic.lanceParticle(mPlayer, startLoc, endLoc);
		mCosmetic.lanceSound(world, mPlayer, mPlayer.getLocation());

		return true;
	}

}
