package pe.project.items;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class FarmlandOverride extends OverrideItem {
	@Override
	public boolean physicsInteraction(Plugin plugin, Player player, Block block) {
		return false;
	}
}
