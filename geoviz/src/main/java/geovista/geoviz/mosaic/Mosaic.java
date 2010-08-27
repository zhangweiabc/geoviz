/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.mosaic;

import java.util.Vector;

/**
 * Paints a Mosaic plot.
 * 
 * @author Frank Hardisty
 * 
 */
public class Mosaic {

	private Table tablep;
	public String displayMode = "Observed";
	private double residSum;
	private final Vector rects = new Vector(256, 256); // Store the tiles.
	private int k; // number of variables
	private int[] levels; // number of levels for each variable
	private int[] plevels; // reverse cummulative product of levels
	private int aGap[];
	private int Gaps[];
	private char Dirs[];

	private String[] names; // variable names
	private String[][] lnames; // names of levels

	private int maxLevel; // t How many variables should be drawn

	public void createMosaic(int start, int levelid, double[] Mtable, int x1,
			int y1, int x2, int y2, String infop) {

		double[] counts = new double[levels[levelid] + 1];
		double[] oCounts = new double[levels[levelid] + 1];
		double[] exps = new double[levels[levelid]];
		double[] obs = new double[levels[levelid]];
		double total = 0;

		String info;
		MyRect tile;
		int index;
		Vector[] tileIds = new Vector[levels[levelid]];
		for (int j = 0; j < levels[levelid]; j++) {
			tileIds[j] = new Vector(8, 8);
		}

		// Calculate the absolute counts for each level first
		if (levelid < k - 1) { // if we did not reach the lowest level

			for (int j = 0; j < levels[levelid]; j++) {
				for (int i = 0; i < plevels[levelid]; i++) {
					index = start + j * plevels[levelid] + i;
					total += Mtable[index];
					counts[j + 1] += Mtable[index];
					oCounts[j + 1] += tablep.table[index];
					exps[j] += tablep.exp[index];
					obs[j] += tablep.table[index];
					if (levelid == maxLevel - 1) {
						tileIds[j].addElement(new Integer(index));
					}
				}
				counts[j + 1] += counts[j];
				oCounts[j + 1] += oCounts[j];
			}
		} else {
			for (int j = 0; j < levels[levelid]; j++) {
				total += Mtable[start + j];
				counts[j + 1] += Mtable[start + j];
				counts[j + 1] += counts[j];
				oCounts[j + 1] += tablep.table[start + j];
				oCounts[j + 1] += oCounts[j];
				exps[j] += tablep.exp[start + j];
				obs[j] += tablep.table[start + j];
				tileIds[j].addElement(new Integer(start + j));
			}
		}

		// int thisGap = 0;
		// if( !displayMode.equals("Fluctuation") )
		int thisGap = aGap[levelid];

		int emptyBin = 0;
		int emptyWidth = 0;
		if (levelid > 0) {
			if (levelid == maxLevel - 1) {
				emptyBin = 0;
			} else if (levelid == maxLevel - 2) {
				emptyBin = 1;
			} else {
				emptyBin = aGap[levelid] - Gaps[levelid];
			}
			emptyWidth = aGap[levelid - 1] - Gaps[levelid - 1];
		}

		int sizeX = x2 - x1;
		int sizeY = y2 - y1;

		if (total > 0) {
			for (int j = 0; j < levels[levelid]; j++) { // for each level in
				// this variable

				info = infop.toString() + names[levelid] + ": "
						+ lnames[levelid][j] + '\n';// Add the popup information

				boolean empty = false;
				boolean stop = false;
				int addGapX = 0;
				int addGapY = 0;

				if (counts[j + 1] - counts[j] == 0) {
					empty = true;
				}
				if (displayMode.equals("Same Bin Size")
						&& oCounts[j + 1] - oCounts[j] == 0
						|| levelid == maxLevel - 1) {
					stop = true;
					for (int i = levelid + 1; i < maxLevel; i++) {
						if (Dirs[i] == 'x') {
							addGapX += aGap[i];
						} else {
							addGapY += aGap[i];
						}
					}
				}

				if (stop || empty) { // Now the rectangles are generated
					if (Dirs[levelid] == 'x') {
						if (empty) {
							tile = new MyRect('y', displayMode, x1
									+ (int) (counts[j] / total * sizeX) + j
									* thisGap, y1, emptyBin,
									sizeY + emptyWidth, 0, exps[j],
									1 / residSum, tablep.p, info, tileIds[j]);
						} else {
							tile = new MyRect('y', displayMode, x1
									+ (int) (counts[j] / total * sizeX) + j
									* thisGap, y1, Math
									.max(1, (int) ((counts[j + 1] - counts[j])
											/ total * sizeX))
									+ addGapX, y2 - y1 + addGapY, obs[j],
									exps[j], 1 / residSum, tablep.p, info,
									tileIds[j]);
						}
					} else {
						if (empty) {
							tile = new MyRect('x', displayMode, x1, y1
									+ (int) (counts[j] / total * sizeY) + j
									* thisGap, sizeX + emptyWidth, emptyBin, 0,
									exps[j], 1 / residSum, tablep.p, info,
									tileIds[j]);
						} else {
							tile = new MyRect('x', displayMode, x1, y1
									+ (int) (counts[j] / total * sizeY) + j
									* thisGap, x2 - x1 + addGapX, Math
									.max(1, (int) ((counts[j + 1] - counts[j])
											/ total * sizeY))
									+ addGapY, obs[j], exps[j], 1 / residSum,
									tablep.p, info, tileIds[j]);
						}
					}
					rects.addElement(tile);
				} else { // Still to go in the recursion
					if (Dirs[levelid] == 'x') {
						createMosaic(
								start + j * plevels[levelid],
								levelid + 1,
								Mtable,
								x1 + j * thisGap
										+ (int) (counts[j] / total * sizeX),
								y1,
								x1
										+ j
										* thisGap
										+ Math
												.max(
														(int) (counts[j]
																/ total * sizeX + 1),
														(int) (counts[j + 1]
																/ total * sizeX)),
								y2, info);
					} else {
						createMosaic(
								start + j * plevels[levelid],
								levelid + 1,
								Mtable,
								x1,
								y1 + j * thisGap
										+ (int) (counts[j] / total * sizeY),
								x2,
								y1
										+ j
										* thisGap
										+ Math
												.max(
														(int) (counts[j]
																/ total * sizeY + 1),
														(int) (counts[j + 1]
																/ total * sizeY)),
								info);
					}
				}
			}
		}
	}
}
