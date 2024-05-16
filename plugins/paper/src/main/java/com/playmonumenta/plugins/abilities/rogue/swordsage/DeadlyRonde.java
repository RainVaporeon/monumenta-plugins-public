package com.playmonumenta.plugins.abilities.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage.DeadlyRondeCS;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class DeadlyRonde extends Ability implements AbilityWithChargesOrStacks {

	private static final int RONDE_1_DAMAGE = 4;
	private static final int RONDE_2_DAMAGE = 6;
	private static final int RONDE_1_MAX_STACKS = 2;
	private static final int RONDE_2_MAX_STACKS = 3;
	private static final double RONDE_SPEED_BONUS = 0.2;
	private static final int RONDE_DECAY_TIMER = 5 * 20;
	private static final double RONDE_RADIUS = 4.5;
	private static final double RONDE_ANGLE = 35;
	private static final float RONDE_KNOCKBACK_SPEED = 0.14f;
	private static final float RONDE_ATTACK_SPEED_SCALING_PORTION = 0.35f;

	public static final String CHARM_DAMAGE = "Deadly Ronde Damage";
	public static final String CHARM_RADIUS = "Deadly Ronde Radius";
	public static final String CHARM_ANGLE = "Deadly Ronde Angle";
	public static final String CHARM_KNOCKBACK = "Deadly Ronde Knockback";
	public static final String CHARM_STACKS = "Deadly Ronde Max Stacks";

	public static final AbilityInfo<DeadlyRonde> INFO =
		new AbilityInfo<>(DeadlyRonde.class, "Deadly Ronde", DeadlyRonde::new)
			.linkedSpell(ClassAbility.DEADLY_RONDE)
			.scoreboardId("DeadlyRonde")
			.shorthandName("DR")
			.descriptions(
				String.format("After casting a skill, gain a stack of Deadly Ronde for %s seconds, stacking up to %s times. " +
					              "While Deadly Ronde is active, you gain %s%% Speed, and your next melee attack consumes a stack to fire a flurry of blades, " +
					              "that fire in a thin cone with a radius of %s blocks and deal %s melee damage to all enemies they hit.",
					RONDE_DECAY_TIMER / 20,
					RONDE_1_MAX_STACKS,
					(int) (RONDE_SPEED_BONUS * 100),
					RONDE_RADIUS,
					RONDE_1_DAMAGE
				),
				String.format("Damage increased to %s, and you can now store up to %s stacks.",
					RONDE_2_DAMAGE,
					RONDE_2_MAX_STACKS))
			.simpleDescription("Damage nearby mobs when striking after casting an ability.")
			.displayItem(Material.BLAZE_ROD);

	private @Nullable BukkitRunnable mActiveRunnable = null;
	private int mRondeStacks = 0;

	private final double mDamage;
	private final float mKnockback;
	private final int mMaxStacks;
	private final DeadlyRondeCS mCosmetic;

	public DeadlyRonde(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? RONDE_1_DAMAGE : RONDE_2_DAMAGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, RONDE_KNOCKBACK_SPEED);
		mMaxStacks = (int) ((isLevelOne() ? RONDE_1_MAX_STACKS : RONDE_2_MAX_STACKS) + CharmManager.getLevel(mPlayer, CHARM_STACKS));
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DeadlyRondeCS());
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		/* Re-up the duration every time an ability is cast */
		if (mActiveRunnable != null) {
			mActiveRunnable.cancel();
		} else {
			cancelOnDeath(new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					mCosmetic.rondeTickEffect(mPlayer, getCharges(), mTicks);
					mPlugin.mEffectManager.addEffect(mPlayer, "DeadlyRonde", new PercentSpeed(5, RONDE_SPEED_BONUS, "DeadlyRondeMod"));
					if (mActiveRunnable == null) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1));
		}
		mActiveRunnable = new BukkitRunnable() {

			@Override
			public void run() {
				mActiveRunnable = null;
				mRondeStacks = 0;
				ClientModHandler.updateAbility(mPlayer, DeadlyRonde.this);
			}

		};
		cancelOnDeath(mActiveRunnable.runTaskLater(mPlugin, RONDE_DECAY_TIMER));

		if (mRondeStacks < mMaxStacks) {
			mCosmetic.rondeGainStackEffect(mPlayer, mPlayer.getLocation());
			mRondeStacks++;
			ClientModHandler.updateAbility(mPlayer, this);
		}

		showChargesMessage();

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mActiveRunnable != null
			    && event.getType() == DamageType.MELEE
			    && InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)
				&& mRondeStacks > 0) {
			float cooldownRatio = mPlayer.getCooledAttackStrength(0);
			float damageRatio = (1 - RONDE_ATTACK_SPEED_SCALING_PORTION) + (RONDE_ATTACK_SPEED_SCALING_PORTION * cooldownRatio);
			double damage = mDamage * damageRatio;

			double angle = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ANGLE, RONDE_ANGLE);
			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RONDE_RADIUS);
			Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), radius, Math.toRadians(angle));

			for (LivingEntity mob : hitbox.getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, damage, mInfo.getLinkedSpell(), true);
				MovementUtils.knockAway(mPlayer, mob, mKnockback, true);
			}

			World world = mPlayer.getWorld();
			mCosmetic.rondeHitEffect(world, mPlayer, radius, RONDE_RADIUS, isLevelTwo());

			mActiveRunnable.cancel();
			mActiveRunnable = null;

			mRondeStacks--;
			showChargesMessage();
			ClientModHandler.updateAbility(mPlayer, this);
			if (mRondeStacks > 0) {
				mActiveRunnable = new BukkitRunnable() {

					@Override
					public void run() {
						mActiveRunnable = null;
						mRondeStacks = 0;
						showChargesMessage();
						ClientModHandler.updateAbility(mPlayer, DeadlyRonde.this);
					}

				};
				cancelOnDeath(mActiveRunnable.runTaskLater(mPlugin, RONDE_DECAY_TIMER));
			}
			return true; // only trigger once per attack
		}
		return false;
	}

	@Override
	public int getCharges() {
		return mRondeStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		TextColor color = INFO.getActionBarColor();
		String name = INFO.getHotbarName();

		int charges = getCharges();
		int maxCharges = getMaxCharges();

		// String output.
		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		output = output.append(Component.text(charges + "/" + maxCharges, (charges == 0 ? NamedTextColor.GRAY : (charges >= maxCharges ? NamedTextColor.GREEN : NamedTextColor.YELLOW))));

		return output;
	}
}
