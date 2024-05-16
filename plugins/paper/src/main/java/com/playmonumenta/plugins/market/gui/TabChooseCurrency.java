package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public class TabChooseCurrency implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Select Currency");
	static final int TAB_SIZE = 5 * 9;

	static final String[][] CURRENCY_LOOTTABLE = new String[][]{
		{"epic:r1/items/currency/experience", "epic:r1/items/currency/concentrated_experience", "epic:r1/items/currency/hyper_experience"},
		{"epic:r2/items/currency/crystalline_shard", "epic:r2/items/currency/compressed_crystalline_shard", "epic:r2/items/currency/hyper_crystalline_shard"},
		{"epic:r3/items/currency/archos_ring", "epic:r3/items/currency/hyperchromatic_archos_ring"},
	};

	public TabChooseCurrency(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {
		for (int region = 0; region < CURRENCY_LOOTTABLE.length; region++) {
			if ((region == 1 && ScoreboardUtils.getScoreboardValue(mGui.mPlayer, "Quest101").orElse(0) < 13)
			|| (region == 2 && ScoreboardUtils.getScoreboardValue(mGui.mPlayer, "R3Access").orElse(0) < 1)) {
				continue;
			}
			for (int compression = 0; compression < CURRENCY_LOOTTABLE[region].length; compression++) {
				ItemStack currency = InventoryUtils.getItemFromLootTable(mGui.mPlayer, NamespacedKeyUtils.fromString(CURRENCY_LOOTTABLE[region][compression]));
				if (currency == null) {
					continue;
				}

				mGui.setItem(1 + region, 3 + compression, MarketGuiIcons.buildChooseCurrencyIcon(currency))
					.onLeftClick(() -> {
						mGui.mCurrencyItem = currency;
						mGui.switchToTab(mGui.TAB_ADD_LISTING);
					});
			}
		}
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
	}

	@Override
	public void onLeave() {

	}
}
