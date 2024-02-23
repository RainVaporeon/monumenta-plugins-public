package com.playmonumenta.plugins.classes;


import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.CosmicMoonblade;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import org.jetbrains.annotations.Nullable;

/*
 * Order does not matter here, it is simply alphabetical.
 * Where order matters for what runs first is in AbilityManager.
 * This is just to link existing spells to custom damage events.
 */
public enum ClassAbility {
	// [Mage]
	ARCANE_STRIKE("Arcane Strike"),
	ARCANE_STRIKE_ENHANCED("Arcane Strike"), // special case for aspect enchants
	ELEMENTAL_ARROWS(ElementalArrows.NAME),
	ELEMENTAL_ARROWS_FIRE("Fire Elemental Arrows"),
	ELEMENTAL_ARROWS_ICE("Ice Elemental Arrows"),
	FROST_NOVA(FrostNova.NAME),
	MAGMA_SHIELD(MagmaShield.NAME),
	MANA_LANCE("Mana Lance"),
	PRISMATIC_SHIELD("Prismatic Shield"),
	SPELLSHOCK(Spellshock.NAME),
	THUNDER_STEP(ThunderStep.NAME),

	// Arcanist
	ASTRAL_OMEN(AstralOmen.NAME),
	COSMIC_MOONBLADE(CosmicMoonblade.NAME),

	// Elementalist
	BLIZZARD(Blizzard.NAME),
	ELEMENTAL_SPIRIT_FIRE("Fire Elemental Spirit"),
	ELEMENTAL_SPIRIT_ICE("Ice Elemental Spirit"),
	STARFALL(Starfall.NAME),

	// [Rogue]
	ADVANCING_SHADOWS("Advancing Shadows"),
	BY_MY_BLADE("By My Blade"),
	DAGGER_THROW("Dagger Throw"),
	DODGING("Dodging"),
	SKIRMISHER("Skirmisher"),
	ESCAPE_DEATH("Escape Death"),
	SMOKESCREEN("Smokescreen"),
	VICIOUS_COMBOS("Vicious Combos"), //placeholder for cosmetic

	// Assassin
	BODKIN_BLITZ("Bodkin Blitz"),
	CLOAK_AND_DAGGER("Cloak And Dagger"),
	COUP_DE_GRACE("Coup de Grace"),

	// Swordsage
	BLADE_DANCE("Blade Dance"),
	DEADLY_RONDE("Deadly Ronde"),
	WIND_WALK("Wind Walk"),

	// [Cleric]
	CELESTIAL_BLESSING("Celestial Blessing"),
	CLEANSING_RAIN("Cleansing Rain"),
	DIVINE_JUSTICE(DivineJustice.NAME),
	HAND_OF_LIGHT("Hand of Light"),
	SANCTIFIED_ARMOR("Sanctified Armor"),
	ILLUMINATE("Illuminate"),

	// Hierophant
	ENCHANTED_PRAYER("Enchanted Prayer"),
	HALLOWED_BEAM("Hallowed Beam"),
	THURIBLE_PROCESSION("Thurible Procession"),

	// Paladin
	CHOIR_BELLS("Choir Bells"),
	HOLY_JAVELIN("Holy Javelin"),
	LUMINOUS_INFUSION("Luminous Infusion"),

	// [Scout]
	EAGLE_EYE("Eagle Eye"),
	VOLLEY("Volley"),
	WIND_BOMB("Wind Bomb"),
	HUNTING_COMPANION("Hunting Companion"),
	SWIFT_CUTS("Swift Cuts"), //placeholder for cosmetic
	SWIFTNESS("Swiftness"),

	// Hunter
	PREDATOR_STRIKE("Predator Strike"),
	SPLIT_ARROW("Split Arrow"),
	PINNING_SHOT("Pinning Shot"),

	// Ranger
	TACTICAL_MANEUVER("Tactical Maneuver"),
	QUICKDRAW("Quickdraw"),
	WHIRLING_BLADE("Whirling Blade"),

	// [Warlock]
	AMPLIFYING("Amplifying Hex"),
	CHOLERIC_FLAMES("Choleric Flames"),
	CURSED_WOUND("Cursed Wound"),
	GRASPING_CLAWS("Grasping Claws"),
	MELANCHOLIC_LAMENT("Melancholic Lament"),
	SANGUINE_HARVEST("Sanguine Harvest"),
	SOUL_REND("Soul Rend"),

	// Reaper
	DARK_PACT("Dark Pact"),
	JUDGEMENT_CHAIN("Judgement Chain"),
	VOODOO_BONDS("Voodoo Bonds"),

	// Tenebrist
	HAUNTING_SHADES("Haunting Shades"),
	RESTLESS_SOULS("Restless Souls"),
	WITHERING_GAZE("Withering Gaze"),

	// [Warrior]
	BRUTE_FORCE("Brute Force"),
	BRUTE_FORCE_AOE("Brute Force"), // special case for glorious battle
	COUNTER_STRIKE("Counter Strike"),
	COUNTER_STRIKE_AOE("Counter Strike"), // special case for glorious battle
	DEFENSIVE_LINE("Defensive Line"),
	RIPOSTE("Riposte"),
	SHIELD_BASH("Shield Bash"),
	SHIELD_BASH_AOE("Shield Bash"), // special case for glorious battle

	// Berserker
	METEOR_SLAM(MeteorSlam.NAME),
	RAMPAGE("Rampage"),
	GLORIOUS_BATTLE("Glorious Battle"),

	// Guardian
	BODYGUARD("Bodyguard"),
	CHALLENGE("Challenge"),
	SHIELD_WALL("Shield Wall"),

	// [Alchemist]
	ALCHEMIST_POTION("Alchemist Potion"),
	ALCHEMICAL_ARTILLERY("Alchemical Artillery"),
	BEZOAR("Bezoar"),
	BRUTAL_ALCHEMY("Brutal Alchemy"),
	ENERGIZING_ELIXIR("Energizing Elixir"),
	GRUESOME_ALCHEMY("Gruesome Alchemy"), //placeholder for cosmetic
	IRON_TINCTURE("Iron Tincture"),
	UNSTABLE_AMALGAM("Unstable Amalgam"),
	EMPOWERING_ODOR("Empowering Odor"),

	// Apothecary
	PANACEA("Panacea"),
	TRANSMUTATION_RING("Transmutation Ring"),
	WARDING_REMEDY("Warding Remedy"),

	// Harbinger
	ESOTERIC_ENHANCEMENTS("Esoteric Enhancements"),
	TABOO("Taboo"),
	SCORCHED_EARTH("Scorched Earth"),

	// [Shaman]
	CLEANSING_TOTEM("Cleansing Totem"),
	EARTHEN_TREMOR("Earthen Tremor"),
	FLAME_SPIRIT("Flame Spirit"),
	FLAME_TOTEM("Flame Totem"),
	INTERCONNECTED_HAVOC("Interconnected Havoc"),
	CHAIN_LIGHTNING("Chain Lightning"),
	LIGHTNING_TOTEM("Lightning Totem"),
	CRYSTALLINE_COMBOS("Crystalline Combos"),
	TOTEMIC_PROJECTION("Totemic Projection"),

	// Soothsayer
	CHAIN_HEALING_WAVE("Chain Healing Wave"),
	SANCTUARY("Sanctuary"),
	WHIRLWIND_TOTEM("Whirlwind Totem"),

	// Hexbreaker
	DECAYED_TOTEM("Decayed Totem"),
	DESECRATING_SHOT("Desecrating Shot"),
	DEVASTATION("Devastation"),

	// [DEPTHS ABILITIES]
	// FLAMECALLER
	APOCALYPSE("Apocalypse"),
	FIREBALL("Fireball"),
	IGNEOUS_RUNE("Igneous Rune"),
	FLAMESTRIKE("Flamestrike"),
	VOLCANIC_METEOR("Volcanic Meteor"),
	PYROBLAST("Pyroblast"),

	// STEELSAGE
	METALMANCY("Metalmancy"),
	FIREWORK_BLAST("Firework Blast"),
	PRECISION_STRIKE("Precision Strike"),
	RAPIDFIRE("Rapid Fire"),
	SCRAPSHOT("Scrapshot"),
	SIDEARM("Sidearm"),
	STEEL_STALLION("Steel Stallion"),
	VOLLEY_DEPTHS("Volley"),

	// WINDWALKER
	GUARDING_BOLT("Guarding Bolt"),
	LAST_BREATH("Last Breath"),
	SKYHOOK("Skyhook"),
	AEROBLAST("Aeroblast"),
	WHIRLWIND("Whirlwind"),
	HOWLINGWINDS("Howling Winds"),
	WIND_WALK_DEPTHS("Wind Walk"),

	// SHADOW
	ADVANCING_SHADOWS_DEPTHS("Advancing Shadows"),
	BLADE_FLURRY("Blade Flurry"),
	CLOAK_OF_SHADOWS("Cloak of Shadows"),
	CHAOS_DAGGER("Chaos Dagger"),
	DUMMY_DECOY("Dummy Decoy"),
	ESCAPE_ARTIST("Escape Artist"),

	// SUNLIGHT
	BOTTLED_SUNLIGHT("Bottled Sunlight"),
	LIGHTNING_BOTTLE("Lightning Bottle"),
	RADIANT_BLESSING("Radiant Blessing"),
	TOTEM_OF_SALVATION("Totem of Salvation"),
	WARD_OF_LIGHT("Ward of Light"),
	DIVINE_BEAM("Divine Beam"),
	ETERNAL_SAVIOR("Eternal Savior"),

	// FROSTBORN
	ICE_LANCE("Ice Lance"),
	ICE_BARRIER("Ice Barrier"),
	FROST_NOVA_DEPTHS("Frost Nova"),
	AVALANCHE("Avalanche"),
	PIERCING_COLD("Piercing Cold"),
	CRYOBOX("Cryobox"),

	// EARTHBORN
	BRAMBLE_SHELL("Bramble Shell"),
	BULWARK("Bulwark"),
	CRUSHING_EARTH("Crushing Earth"),
	EARTHEN_WRATH("Earthen Wrath"),
	EARTHQUAKE("Earthquake"),
	STONE_SKIN("Stone Skin"),
	TAUNT("Taunt"),

	//PRISMATIC
	CHROMA_BLADE("Chroma Blade"),
	COLOR_SPLASH("Color Splash"),
	CONVERGENCE("Convergence"),
	DISCO_BALL("Disco Ball"),
	ENCORE("Encore"),
	REFRACTION("Refraction"),
	RESURRECTION("Resurrection"),
	SOLAR_RAY("Solar Ray"),
	REBIRTH("Rebirth"),

	// Fake class abilities for some enchantments
	// Used to trigger other events
	ERUPTION("Eruption", true),
	QUAKE("Quake", true),
	EXPLOSIVE("Explosive", true),
	INFERNO("Inferno", true),
	REVERB("Reverb", true);

	private final String mName;
	private final boolean mFake;

	ClassAbility(String name) {
		this(name, false);
	}

	ClassAbility(String name, boolean fake) {
		mName = name;
		mFake = fake;
	}

	public String getName() {
		return mName;
	}

	public boolean isFake() {
		return mFake;
	}

	public static @Nullable ClassAbility getAbility(String name) {
		for (ClassAbility ability : ClassAbility.values()) {
			if (ability.getName().equals(name)) {
				return ability;
			}
		}
		return null;
	}
}
