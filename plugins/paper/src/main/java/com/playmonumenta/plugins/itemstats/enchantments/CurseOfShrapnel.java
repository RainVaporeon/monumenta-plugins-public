package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class CurseOfShrapnel implements Enchantment {

	@Override
	public String getName() {
		return "Curse of Shrapnel";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_SHRAPNEL;
	}

	@Override
	public double getPriorityAmount() {
		return 1;
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double level, BlockBreakEvent event) {
		if (ItemUtils.isPickaxe(player.getInventory().getItemInMainHand()) && event.getBlock().getType() == Material.SPAWNER) {
			if (SpawnerUtils.getShields(event.getBlock()) > 0) {
				return;
			}
			new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.1).spawnAsPlayerActive(player);
			Bukkit.getScheduler().runTask(plugin, () -> {
				DamageUtils.damage(null, player, DamageEvent.DamageType.TRUE, level, null, true, false);
			});
		}
	}

}
