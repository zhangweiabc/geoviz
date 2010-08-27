/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.colorbrewer;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import geovista.colorbrewer.Palette1D.SequenceType;

public class ColorBrewerDataReader {

	final static Logger logger = Logger.getLogger(ColorBrewerDataReader.class
			.getName());

	public ColorBrewerDataReader() throws IOException {
		readContents();

	}

	private static SequenceType findSequenceType(String typeString) {
		SequenceType type = null;
		if (typeString.equals("qual")) {
			type = SequenceType.QUALITATIVE;
		} else if (typeString.equals("seq")) {
			type = SequenceType.SEQUENTIAL;
		} else if (typeString.equals("div")) {
			type = SequenceType.DIVERGING;
		} else {
			logger.severe("unknown string:" + typeString);
		}
		return type;
	}

	private static UnivariatePalette readPalette(Scanner scan) {
		String line = scan.nextLine();
		Scanner lineScan = new Scanner(line);
		lineScan.useDelimiter(",");

		String name = lineScan.next();
		int maxLength = lineScan.nextInt();
		SequenceType type = findSequenceType(lineScan.next());
		HashMap<Integer, Color[]> colorMap = new HashMap<Integer, Color[]>();
		Color[] colors = new Color[maxLength];
		for (int i = 0; i < maxLength; i++) {
			if (i > 0) {
				line = scan.nextLine();
				lineScan = new Scanner(line);
				lineScan.useDelimiter(",");
				lineScan.next();
				lineScan.next();
			}
			String indexString = lineScan.next();
			int index = Integer.valueOf(indexString);
			assert (i == index - 1);
			lineScan.next(); // skip letter
			int r = lineScan.nextInt();
			int g = lineScan.nextInt();
			String bString = lineScan.next();
			// logger.info("bString = " + bString);
			int b = Integer.valueOf(bString.trim());
			Color col = new Color(r, g, b);
			colors[i] = col;

		}
		colorMap.put(maxLength, colors);

		UnivariatePalette pal = new UnivariatePalette(name, type, colorMap,
				maxLength);
		return pal;
	}

	private static boolean checkPalette(UnivariatePalette pal) {
		for (int i = pal.minLength; i < pal.maxLength; i++) {
			Color[] cols = pal.getColors(i);
			assert (cols != null);
		}

		return true;
	}

	public static HashMap<String, UnivariatePalette> readContents() {
		HashMap<String, UnivariatePalette> palettes = new HashMap<String, UnivariatePalette>();
		InputStream colorBrewerIs = ColorBrewerDataReader.class
				.getResourceAsStream("resources/brewer_values.csv");
		Scanner scan = new Scanner(colorBrewerIs);
		Pattern pat = Pattern.compile("[,\r]");
		scan.useDelimiter(pat);
		scan.nextLine();// skip header
		UnivariatePalette currentPalette = readPalette(scan);
		while (scan.hasNext()) {
			UnivariatePalette pal = readPalette(scan);
			if (currentPalette.name.equals(pal.name)) {
				currentPalette = currentPalette.combine(pal);
			} else {
				assert (checkPalette(currentPalette));
				palettes.put(currentPalette.name, currentPalette);
				currentPalette = pal;
			}
		}

		return palettes;

	}

	public static void main(String[] args) {

		ColorBrewerDataReader reader = null;
		double duration = 0;
		try {
			long start = System.currentTimeMillis();
			reader = new ColorBrewerDataReader();
			long end = System.currentTimeMillis();
			duration = end - start;
			duration = duration / 1000;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.info("All done! took this long:" + duration);

	}
}
