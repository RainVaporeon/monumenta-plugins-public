package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class Persistence implements Enchantment {

	@Override
	public String getName() {
		return "Persistence";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PERSISTENCE;
	}

}