package geovista.geoviz.mosaic;

import java.awt.Color;
import java.util.Vector;

public class dataSet {

	private final Vector data = new Vector(256, 256);
	private double[] selectionArray;
	public byte[] colorArray;
	private Color[] brushColors;
	public boolean colorBrush = false;

	private int[][] RGBs;

	private dataSet(String setName) {
		defineColors();
	}

	private void defineColors() {
		RGBs = new int[13][];
		RGBs[1] = new int[] { 50, 106, 157 };
		RGBs[2] = new int[] { 199, 106, 149 };
		RGBs[3] = new int[] { 102, 154, 103 };
		RGBs[4] = new int[] { 255, 122, 0 };
		RGBs[5] = new int[] { 159, 64, 255 };
		RGBs[6] = new int[] { 255, 0, 255 };
		RGBs[7] = new int[] { 159, 255, 64 };
		RGBs[8] = new int[] { 255, 210, 0 };
		RGBs[9] = new int[] { 0, 255, 255 };
		RGBs[10] = new int[] { 210, 0, 0 };
		RGBs[11] = new int[] { 0, 255, 0 };
		RGBs[12] = new int[] { 0, 0, 210 };
	}

	public double[] getRawNumbers(int i) {
		Variable v = (Variable) data.elementAt(i);
		return v.data;
	}

	public Color getColorByID(int id) {
		return brushColors[id];
	}

	public int getNumColors() {
		return brushColors.length;
	}

	public double[] getSelection() {
		return selectionArray;
	}

	class Variable {

		private double[] data;

		Variable(String name) {
			if (name.substring(0, 2).equals("/P")) {
			}
		}

		Variable(int n, String name) {
			if (name.length() > 1) {
				if (name.substring(0, 2).equals("/P")) {
				}
			}
			data = new double[n];
		}

	}
}
