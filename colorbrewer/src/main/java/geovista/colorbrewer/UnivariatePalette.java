package geovista.colorbrewer;

import java.awt.Color;
import java.util.HashMap;

/**
 * This class is intended to be immutable, and therefore thread-safe.
 * 
 */
public final class UnivariatePalette implements Palette1D {

	String name;
	SequenceType type;
	HashMap<Integer, Color[]> colorMap;
	static int minLength = 3;
	int maxLength;

	UnivariatePalette(String name, SequenceType type,
			HashMap<Integer, Color[]> colorMap, int maxLength) {
		this.name = name;
		this.type = type;
		this.colorMap = colorMap;
		this.maxLength = maxLength;
	}

	UnivariatePalette combine(UnivariatePalette otherPalette) {
		int max = maxLength;
		if (otherPalette.maxLength > maxLength) {
			max = otherPalette.maxLength;
		}
		HashMap<Integer, Color[]> newColorMap = (HashMap<Integer, Color[]>) colorMap
				.clone();
		for (Integer key : otherPalette.colorMap.keySet()) {
			newColorMap.put(key, otherPalette.colorMap.get(key));
		}
		return new UnivariatePalette(name, type, newColorMap, max);
	}

	public Color[] getColors(int length) {

		if (length < 0) {
			throw new IllegalArgumentException(
					"can't ask for a negative number of colors ");
		}
		Color[] colors = null;
		if (length == 0) {
			colors = new Color[0];
		} else if (length == 1) {
			colors = new Color[1];
			colors[0] = colorMap.get(minLength)[0];
		} else if (length == 2) {
			colors = new Color[2];
			colors[0] = colorMap.get(minLength)[0];
			colors[1] = colorMap.get(minLength)[1];
		} else if (length <= maxLength) {
			colors = colorMap.get(length);
		} else {
			colors = new Color[length];
			Color[] brewerColors = colorMap.get(maxLength);
			for (int i = 0; i < colors.length; i++) {
				if (i < brewerColors.length) {
					colors[i] = brewerColors[i];
				} else {
					colors[i] = Color.black;
				}
			}

		}
		assert (colors.length == length);
		return colors;
	}

	public SequenceType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

}
