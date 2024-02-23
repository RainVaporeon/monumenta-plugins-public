package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.Channeling;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.CosmicMoonblade;
import com.playmonumenta.plugins.abilities.mage.arcanist.SagesInsight;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritFire;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

public class Mage extends PlayerClass {

	public static final int CLASS_ID = 1;
	public static final int ARCANIST_SPEC_ID = 1;
	public static final int ELEMENTALIST_SPEC_ID = 2;

	public Mage() {
		mAbilities.add(ArcaneStrike.INFO);
		mAbilities.add(ElementalArrows.INFO);
		mAbilities.add(FrostNova.INFO);
		mAbilities.add(MagmaShield.INFO);
		mAbilities.add(ManaLance.INFO);
		mAbilities.add(Spellshock.INFO);
		mAbilities.add(ThunderStep.INFO);
		mAbilities.add(PrismaticShield.INFO);
		mClass = CLASS_ID;
		mClassName = "Mage";
		mClassColor = TextColor.fromHexString("#A129D3");
		mClassGlassFiller = Material.PURPLE_STAINED_GLASS_PANE;
		mDisplayItem = Material.BLAZE_ROD;
		mClassDescription = "Mages are masters of area control, freezing, wounding, and igniting enemies with their strikes.";
		mClassPassiveDescription = String.format("Your spell damage is increased by your wand's Spell Power stat. After casting a spell, your next melee attack with a wand deals %s%% more damage.", (int) (Channeling.PERCENT_MELEE_INCREASE * 100));
		mClassPassiveName = "Channeling";

		mSpecOne.mAbilities.add(AstralOmen.INFO);
		mSpecOne.mAbilities.add(CosmicMoonblade.INFO);
		mSpecOne.mAbilities.add(SagesInsight.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103i";
		mSpecOne.mSpecialization = ARCANIST_SPEC_ID;
		mSpecOne.mSpecName = "Arcanist";
		mSpecOne.mDisplayItem = Material.DRAGON_BREATH;
		mSpecOne.mDescription = "Arcanists are mages that specialize at controlling their skill cooldowns and getting up close.";

		mSpecTwo.mAbilities.add(Blizzard.INFO);
		mSpecTwo.mAbilities.add(ElementalSpiritFire.INFO);
		mSpecTwo.mAbilities.add(Starfall.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103b";
		mSpecTwo.mSpecialization = ELEMENTALIST_SPEC_ID;
		mSpecTwo.mSpecName = "Elementalist";
		mSpecTwo.mDisplayItem = Material.BLAZE_POWDER;
		mSpecTwo.mDescription = "Elementalists are the undisputed masters of the elements. They excel at zoning and crowd control.";

		mTriggerOrder = ImmutableList.of(
			CosmicMoonblade.INFO,

			Blizzard.INFO,
			Starfall.INFO,

			FrostNova.INFO,
			MagmaShield.INFO, // after blizzard
			ManaLance.INFO,
			ThunderStep.INFO
		);
	}
}
