package com.playmonumenta.plugins.abilities.mage.arcanist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SagesInsight extends Ability implements AbilityWithChargesOrStacks {
	private static final int DECAY_TIMER = 20 * 4;
	private static final int MAX_STACKS = 8;
	private static final int SPEED_DURATION = 5 * 20;
	private static final int ABILITIES_COUNT_1 = 2;
	private static final int ABILITIES_COUNT_2 = 3;
	private static final String ATTR_NAME = "SagesExtraSpeedAttr";

	private static final float[] PITCHES = {1.6f, 1.8f, 1.6f, 1.8f, 2f};
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(222, 219, 36), 1.0f);

	private final int mResetSize;
	private final int mMaxStacks;
	private final List<ClassAbility> mResets = new ArrayList<>();
	private final double mSpeed;
	public static final String CHARM_STACKS = "Sage's Insight Stack Trigger Threshold";
	public static final String CHARM_DECAY = "Sage's Insight Decay Duration";
	public static final String CHARM_SPEED = "Sage's Insight Speed Amplifier";
	public static final String CHARM_ABILITY = "Sage's Insight Ability Count";

	public static final AbilityInfo<SagesInsight> INFO =
		new AbilityInfo<>(SagesInsight.class, "Sage's Insight", SagesInsight::new)
			.scoreboardId("SagesInsight")
			.shorthandName("SgI")
			.actionBarColor(TextColor.color(222, 219, 36))
			.descriptions(
				String.format("If an active spell hits an enemy, you gain an Arcane Insight. Insights stack up to %s, " +
						"but decay every %ss of not gaining one. Once %s Insights are revealed, %s " +
						"cast is refreshed. This sets your Insights back to 0.",
					MAX_STACKS,
					DECAY_TIMER / 20,
					MAX_STACKS,
					String.format("the cooldowns of the previous %s spells", ABILITIES_COUNT_1)
				),
				String.format("Sage's Insight now refreshes the cooldowns of your previous %s spells, upon activating.",
					ABILITIES_COUNT_2
				))
			.simpleDescription("Refresh a spell's cooldown after multiple spells damage enemies in succession.")
			.displayItem(Material.ENDER_EYE);


	private final HashMap<ClassAbility, Boolean> mStacksMap;

	public SagesInsight(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mResetSize = (isLevelOne() ? ABILITIES_COUNT_1 : ABILITIES_COUNT_2) + (int) CharmManager.getLevel(player, CHARM_ABILITY);
		mMaxStacks = (int) CharmManager.getLevel(player, CHARM_STACKS) + MAX_STACKS;
		mSpeed = CharmManager.getLevelPercentDecimal(player, CHARM_SPEED);
		mResets.clear();
		mStacksMap = new HashMap<>();
	}

	private int mStacks = 0;
	private int mTicksToStackDecay = 0;

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mStacks > 0) {
			mTicksToStackDecay -= 5;

			if (mTicksToStackDecay <= 0) {
				mTicksToStackDecay = CharmManager.getDuration(mPlayer, CHARM_DECAY, DECAY_TIMER);
				mStacks--;
				showChargesMessage();
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		ClassAbility ability = event.getAbility();
		if (ability == null || ability.isFake()) {
			return false;
		}
		mTicksToStackDecay = CharmManager.getDuration(mPlayer, CHARM_DECAY, DECAY_TIMER);
		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		Location locD = event.getDamagee().getLocation().add(0, 1, 0);

		Boolean bool = mStacksMap.get(ability);
		if (bool != null && bool) {
			mStacks++;
			mStacksMap.put(ability, false);
			if (mStacks >= mMaxStacks) {
				if (mSpeed > 0) {
					mPlugin.mEffectManager.addEffect(mPlayer, "SagesExtraSpeed", new PercentSpeed(SPEED_DURATION, mSpeed, ATTR_NAME));
				}
				new PartialParticle(Particle.REDSTONE, loc, 20, 1.4, 1.4, 1.4, COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 2.1, 0), 20, 0.5, 0.1, 0.5, 0.1).spawnAsPlayerActive(mPlayer);
				for (int i = 0; i < PITCHES.length; i++) {
					float pitch = PITCHES[i];
					new BukkitRunnable() {
						@Override
						public void run() {
							world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1, pitch);
						}
					}.runTaskLater(mPlugin, i);
				}

				mStacks = 0;
				for (ClassAbility s : mResets) {
					if (s == ClassAbility.MANA_LANCE) {
						// Special Treatment for Mana Lance because of charged abilities.
						Objects.requireNonNull(mPlugin.mAbilityManager.getPlayerAbility(mPlayer, ManaLance.class)).incrementCharge();
					} else {
						mPlugin.mTimers.removeCooldown(mPlayer, s);
					}
				}
				mResets.clear();
			} else {
				new PartialParticle(Particle.REDSTONE, locD, 15, 0.4, 0.4, 0.4, COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.EXPLOSION_NORMAL, locD, 15, 0, 0, 0, 0.2).spawnAsPlayerActive(mPlayer);
				showChargesMessage();
			}
			ClientModHandler.updateAbility(mPlayer, this);
		}
		return false; // only used to check that an ability dealt damage, and does not cause more damage instances.
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		ClassAbility cast = event.getSpell();
		mStacksMap.put(cast, true);

		mResets.add(cast);
		if (mResets.size() > mResetSize) {
			mResets.remove(0);
		}
		return true;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mMaxStacks - 1; // -1 as "max stacks" is never reached - it immediately resets back to 0
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
