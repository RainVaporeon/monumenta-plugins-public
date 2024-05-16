package com.playmonumenta.plugins.bosses.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class StringReader {
	private final String mStr;
	private int mIdx = 0;

	public StringReader(String str) {
		mStr = str;
	}

	public String getString() {
		return mStr;
	}

	public boolean canRead() {
		return mIdx < mStr.length();
	}

	public String peek() {
		if (!canRead()) {
			return "";
		}
		return mStr.substring(mIdx, mIdx + 1);
	}

	public boolean peek(String desired) {
		if (!canRead()) {
			return false;
		}
		return remaining().startsWith(desired);
	}

	/* Returns the part of the string processed so far */
	public String readSoFar() {
		return mStr.substring(0, mIdx);
	}

	/* Returns the remainder of the string */
	public String remaining() {
		if (mIdx >= mStr.length()) {
			return "";
		}
		return mStr.substring(mIdx);
	}

	/* Reads all characters up to the specified one, not including it
	 * Returns that substring or null if specified character doesn't exist
	 */
	public @Nullable String readUntil(String c) {
		int index = mStr.indexOf(c, mIdx);
		if (index == -1) {
			return null;
		}
		String retval = mStr.substring(mIdx, index);
		mIdx = index;
		return retval;
	}

	public boolean advance() {
		return advance(1);
	}

	public boolean advance(int n) {
		mIdx = mIdx + n;
		if (mIdx > mStr.length()) {
			mIdx = mStr.length();
			return false;
		}
		return true;
	}

	public boolean advance(String next) {
		skipWhitespace();
		String remain = remaining();
		if (remain.startsWith(next)) {
			advance(next.length());
			return true;
		}
		return false;
	}

	public void skipWhitespace() {
		while (mIdx < mStr.length() && mStr.substring(mIdx, mIdx + 1).isBlank()) {
			mIdx += 1;
		}
	}

	public @Nullable String readOneOf(Collection<String> validItems) {
		skipWhitespace();
		String remain = remaining();
		// Make a copy of the input collection and sort it by length,
		// so it works even when keys are entirely contained within other keys
		// For example "damage" and "damagepercent". Need to match the longer one first if possible
		List<String> sortedCopy = new ArrayList<>(validItems);
		Collections.sort(sortedCopy, (a, b) -> Integer.compare(b.length(), a.length()));
		for (String item : sortedCopy) {
			if (remain.startsWith(item)) {
				advance(item.length());
				return item;
			}
		}
		return null;
	}

	public @Nullable Boolean readBoolean() {
		skipWhitespace();
		if (advance("false")) {
			return Boolean.FALSE;
		} else if (advance("true")) {
			return Boolean.TRUE;
		}
		return null;
	}

	public @Nullable Long readLong() {
		skipWhitespace();
		int origIdx = mIdx;
		int tmpIdx = mIdx;
		Long lastParse = null;

		if (peek("-")) {
			tmpIdx++;
		}

		while (tmpIdx < mStr.length()) {
			try {
				lastParse = Long.parseLong(mStr.substring(origIdx, tmpIdx + 1));
				tmpIdx += 1;
			} catch (NumberFormatException nfe) {
				break;
			}
		}
		if (lastParse != null) {
			mIdx = tmpIdx;
		}
		return lastParse;
	}

	public @Nullable Double readDouble() {
		skipWhitespace();
		int origIdx = mIdx;
		int tmpIdx = mIdx;
		Double lastParse = null;

		if (peek("-")) {
			tmpIdx++;
		}

		while (tmpIdx < mStr.length()) {
			try {
				lastParse = Double.parseDouble(mStr.substring(origIdx, tmpIdx + 1));
				tmpIdx += 1;
			} catch (NumberFormatException nfe) {
				break;
			}
		}
		if (lastParse != null) {
			mIdx = tmpIdx;
		}
		return lastParse;
	}

	public @Nullable PotionEffectType readPotionEffectType() {
		skipWhitespace();
		String remain = remaining().toUpperCase(Locale.ROOT);
		for (PotionEffectType type : POTION_EFFECT_TYPES_SORTED) {
			if (remain.startsWith(type.getName())) {
				advance(type.getName().length());
				return type;
			}
		}
		return null;
	}

	public @Nullable Sound readSound() {
		skipWhitespace();
		String remain = remaining().toUpperCase(Locale.ROOT);
		for (Sound type : SOUNDS_SORTED) {
			if (remain.startsWith(type.name())) {
				advance(type.name().length());
				return type;
			}
		}
		return null;
	}

	//we need to check before match with longer particle or we can end up with a misunderstanding
	//aka CRIT_MAGIC can be read with CRIT
	public @Nullable Particle readParticle() {
		skipWhitespace();
		String remain = remaining().toUpperCase(Locale.ROOT);
		for (Particle type : PARTICLES_SORTED) {
			if (remain.startsWith(type.name())) {
				advance(type.name().length());
				return type;
			}
		}
		return null;
	}

	public @Nullable Material readMaterial() {
		skipWhitespace();

		String remain = remaining().toUpperCase(Locale.ROOT);

		for (Material material : MATERIALS_SORTED) {
			if (remain.startsWith(material.name())) {
				advance(material.name().length());
				return material;
			}
		}

		return null;
	}

	public @Nullable <E extends Enum<?>> E readEnum(E[] values) {
		skipWhitespace();
		String remain = remaining();

		for (E value : values) {
			if (remain.startsWith(value.name())) {
				advance(value.name().length());
				return value;
			}
		}

		return null;
	}

	public static final Map<String, Color> COLOR_MAP = new LinkedHashMap<>();
	public static final Map<Color, String> COLOR_NAME_MAP = new LinkedHashMap<>();
	public static final List<Particle> PARTICLES_SORTED = Arrays.asList(Particle.values());
	public static final List<Material> MATERIALS_SORTED = Arrays.asList(Material.values());
	public static final List<Sound> SOUNDS_SORTED = Arrays.asList(Sound.values());
	public static final List<PotionEffectType> POTION_EFFECT_TYPES_SORTED = Arrays.asList(PotionEffectType.values());

	static {
		//this is just because Color don't have the functions values() and getName()...
		COLOR_MAP.put("AQUA", Color.AQUA);
		COLOR_MAP.put("BLACK", Color.BLACK);
		COLOR_MAP.put("BLUE", Color.BLUE);
		COLOR_MAP.put("FUCHSIA", Color.FUCHSIA);
		COLOR_MAP.put("GRAY", Color.GRAY);
		COLOR_MAP.put("GREEN", Color.GREEN);
		COLOR_MAP.put("LIME", Color.LIME);
		COLOR_MAP.put("MAROON", Color.MAROON);
		COLOR_MAP.put("NAVY", Color.NAVY);
		COLOR_MAP.put("OLIVE", Color.OLIVE);
		COLOR_MAP.put("ORANGE", Color.ORANGE);
		COLOR_MAP.put("PURPLE", Color.PURPLE);
		COLOR_MAP.put("RED", Color.RED);
		COLOR_MAP.put("SILVER", Color.SILVER);
		COLOR_MAP.put("TEAL", Color.TEAL);
		COLOR_MAP.put("WHITE", Color.WHITE);
		COLOR_MAP.put("YELLOW", Color.YELLOW);

		for (Map.Entry<String, Color> entry : COLOR_MAP.entrySet()) {
			COLOR_NAME_MAP.put(entry.getValue(), entry.getKey());
		}

		//Sorting Particle
		PARTICLES_SORTED.sort((a, b) -> b.name().length() - a.name().length());

		//Sorting Material
		MATERIALS_SORTED.sort((a, b) -> b.name().length() - a.name().length());

		//sorting Sound
		SOUNDS_SORTED.sort((a, b) -> b.name().length() - a.name().length());

		//sorting Potion Effect Type
		POTION_EFFECT_TYPES_SORTED.sort((a, b) -> b.getName().length() - a.getName().length());
	}

	public @Nullable Color readColor() {
		skipWhitespace();

		String remain = remaining();
		if (remain.startsWith("#")) {
			if (remain.length() >= 7) {
				try {
					Color ret = Color.fromRGB(Integer.parseInt(remain.substring(1, 7), 16));
					advance(7);
					return ret;
				} catch (NumberFormatException nfe) {
					return null;
				}
			}
		} else {
			for (Map.Entry<String, Color> color : COLOR_MAP.entrySet()) {
				if (remain.startsWith(color.getKey())) {
					advance(color.getKey());
					return color.getValue();
				}
			}
		}
		return null;
	}

	private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile("^\"(?:[^\"\\\\]|\\\\.)*\"");
	private static final Pattern NON_QUOTED_STRING_PATTERN = Pattern.compile("^[^\",)][^,)\\]]*");

	public @Nullable String readString() {
		skipWhitespace();
		String remain = remaining();
		Matcher quotedStringMatcher = QUOTED_STRING_PATTERN.matcher(remain);
		if (quotedStringMatcher.find()) {
			String match = quotedStringMatcher.group();
			advance(match);
			// Remove the surrounding quotes and un-escape the contents
			match = StringEscapeUtils.unescapeJava(match.substring(1, match.length() - 1));
			return match;
		}

		Matcher nonQuotedMatcher = NON_QUOTED_STRING_PATTERN.matcher(remain);
		if (nonQuotedMatcher.find()) {
			String match = nonQuotedMatcher.group();
			advance(match);
			return match;
		}

		return null;
	}
}
