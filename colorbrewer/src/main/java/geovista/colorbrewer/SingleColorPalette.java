package geovista.colorbrewer;

import java.awt.Color;

/**
 * This class is intended to be immutable, and therefore thread-safe.
 * 
 */
public final class SingleColorPalette implements Palette1D {

	Color col;

	public SingleColorPalette(Color col) {
		this.col = col;
	}

	public String getName() {
		return "SingleColor";
	}

	public SequenceType getType() {
		return Palette1D.SequenceType.QUALITATIVE;
	}

	public Color[] getColors(int length) {
		Color[] colors = new Color[length];
		for (int i = 0; i < length; i++) {
			colors[i] = col;
		}
		return colors;
	}

}
