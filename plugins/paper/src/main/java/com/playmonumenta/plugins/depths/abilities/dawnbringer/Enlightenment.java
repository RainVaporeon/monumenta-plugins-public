package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Enlightenment extends DepthsAbility {

	//Technical implementation of this ability is handled in the depths listener, so that any member of the party can benefit from it

	public static final String ABILITY_NAME = "Enlightenment";
	public static final double[] XP_MULTIPLIER = {1.3, 1.35, 1.4, 1.45, 1.5, 2.0};
	public static final int[] RARITY_INCREASE = {3, 4, 5, 6, 7, 30};

	public static final DepthsAbilityInfo<Enlightenment> INFO =
		new DepthsAbilityInfo<>(Enlightenment.class, ABILITY_NAME, Enlightenment::new, DepthsTree.SUNLIGHT, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.EXPERIENCE_BOTTLE))
			.descriptions(Enlightenment::getDescription, MAX_RARITY);


	public Enlightenment(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static String getDescription(int rarity) {
		return "All players in your party gain " + DepthsUtils.getRarityColor(rarity) + XP_MULTIPLIER[rarity - 1] + "x" + ChatColor.WHITE + " experience. " +
			       "Does not stack if multiple players in the party have the skill. " +
			       "Additionally, your chances of finding higher rarity abilities are increased by " + DepthsUtils.getRarityColor(rarity) + RARITY_INCREASE[rarity - 1] + "%" + ChatColor.WHITE + ".";
	}

}

