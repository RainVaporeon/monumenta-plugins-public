package com.playmonumenta.plugins.itemstats.gui;

import com.playmonumenta.plugins.itemstats.enchantments.Cloaked;
import com.playmonumenta.plugins.itemstats.enchantments.Ethereal;
import com.playmonumenta.plugins.itemstats.enchantments.Evasion;
import com.playmonumenta.plugins.itemstats.enchantments.Guard;
import com.playmonumenta.plugins.itemstats.enchantments.Poise;
import com.playmonumenta.plugins.itemstats.enchantments.Reflexes;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.itemstats.enchantments.Steadfast;
import com.playmonumenta.plugins.itemstats.enchantments.Tempo;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

enum PSGUISecondaryStat {
	POISE(1, Material.LILY_OF_THE_VALLEY, EnchantmentType.POISE, true, """
		Gain (Level*20%%) effective Armor
		when above %s%% Max Health
		or (Level*10%%) effective Armor
		when above %s%% Max Health""".formatted(StringUtils.multiplierToPercentage(Poise.MIN_HEALTH_PERCENT), StringUtils.multiplierToPercentage(Poise.HALF_BONUS_PERCENT))),
	INURE(2, Material.NETHERITE_SCRAP, EnchantmentType.INURE, true, """
		Gain (Level*20%) effective Armor
		when taking the same type of mob damage consecutively
		(Melee, Projectile, Blast, or Magic). It will remain
		being half as effective in the case of alternating damage types
		after activation, until a third type of damage is taken."""),
	STEADFAST(3, Material.LEAD, EnchantmentType.STEADFAST, true, """
		Gain (Level*%s%%) armor for every
		1%% health lost, up to (Level*20%% armor).
		Also calculates bonus from Second Wind when enabled.""".formatted(Steadfast.BONUS_SCALING_RATE)),
	ETHEREAL(5, Material.PHANTOM_MEMBRANE, EnchantmentType.ETHEREAL, false, """
		Gain (Level*20%%) effective Agility
		on hits taken within %s seconds of any previous hit.""".formatted(StringUtils.ticksToSeconds(Ethereal.PAST_HIT_DURATION_TIME))),
	REFLEXES(6, Material.ENDER_EYE, EnchantmentType.REFLEXES, false, """
		Gain (Level*20%%) effective Agility
		when there are %s or more enemies within %s blocks.""".formatted(Reflexes.MOB_CAP, Reflexes.RADIUS)),
	EVASION(7, Material.ELYTRA, EnchantmentType.EVASION, false, """
		Gain (Level*20%%) effective Agility
		when taking damage from a source further
		than %s blocks from the player.""".formatted(Evasion.DISTANCE)),
	SHIELDING(10, Material.NAUTILUS_SHELL, EnchantmentType.SHIELDING, true, """
		Gain (Level*20%%) effective Armor
		when taking damage from an enemy within %s blocks.
		Taking damage that would stun a shield
		halves Shielding reduction for %s seconds.""".formatted(Shielding.DISTANCE, StringUtils.ticksToSeconds(Shielding.DISABLE_DURATION))),
	GUARD(11, Material.SHULKER_SHELL, EnchantmentType.GUARD, true, """
		Gain (Level*20%%) effective Armor
		after blocking an attack with a shield.
		The duration lasts for %ss if blocked
		from offhand, and %ss from mainhand.""".formatted(
		StringUtils.ticksToSeconds(Guard.PAST_HIT_DURATION_TIME_OFFHAND), StringUtils.ticksToSeconds(Guard.PAST_HIT_DURATION_TIME_MAINHAND))),
	TEMPO(15, Material.CLOCK, EnchantmentType.TEMPO, false, """
		Gain (Level*20%%) effective Agility
		on the first hit taken after
		%s seconds of taking no damage.
		Half of the bonus is granted after
		%s seconds of taking no damage.""".formatted(
		StringUtils.ticksToSeconds(Tempo.PAST_HIT_DURATION_TIME),
		StringUtils.ticksToSeconds(Tempo.PAST_HIT_DURATION_TIME_HALF)
	)),
	CLOAKED(16, Material.BLACK_DYE, EnchantmentType.CLOAKED, false, """
		Gain (Level*20%%) effective Agility
		when there are %s or less enemies within %s blocks.""".formatted(Cloaked.MOB_CAP, Cloaked.RADIUS));

	private final int mSlot;
	private final Material mIcon;
	private final String mName;
	private final boolean mIsArmorModifier;
	private final EnchantmentType mEnchantmentType;
	private final String mDescription;

	PSGUISecondaryStat(int slot, Material icon, EnchantmentType enchantmentType, boolean isArmorModifier, String description) {
		mSlot = slot;
		mIcon = icon;
		mName = enchantmentType.getName();
		mEnchantmentType = enchantmentType;
		mIsArmorModifier = isArmorModifier;
		mDescription = description;
	}

	public int getSlot() {
		return mSlot;
	}

	public Material getIcon() {
		return mIcon;
	}

	public String getName() {
		return mName;
	}

	public EnchantmentType getEnchantmentType() {
		return mEnchantmentType;
	}

	public boolean isArmorModifier() {
		return mIsArmorModifier;
	}

	public Component getDisplay(boolean enabled) {
		return Component.text(String.format("Calculate Bonus from %s%s", mName, enabled ? " - Enabled" : " - Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
	}

	public List<Component> getDisplayLore() {
		return Arrays.stream(mDescription.split("\n")).map(line -> (Component) Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList();
	}
}
