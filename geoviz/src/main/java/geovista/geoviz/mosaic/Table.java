package geovista.geoviz.mosaic;

public class Table implements Cloneable {
	public double[] table;
	public double[] exp;

	public double p;
	public int[][] Ids;
	public dataSet data;
	public final int count; // == -1 if the tables needs to be "breaked

	// private Table(String name, double[] table, int k, int[] levels,
	// String[] names, String[][] lnames, int[] initialVars, int[][] Ids,
	// dataSet data, int count) {
	private Table(double[] table, int k, String[][] lnames, int[][] Ids,
			int count) {
		this.table = table;
		this.Ids = Ids;
		this.count = count;
		exp = new double[table.length];

		// Clean Numerical Labels 1.0 -> 1
		for (int j = 0; j < k; j++) {
			boolean allDotNull = true;
			for (int i = 0; i < lnames[j].length; i++) {
				if (!lnames[j][i].endsWith(".0")) {
					allDotNull = false;
				}
			}
			if (allDotNull) {
				for (int i = 0; i < lnames[j].length; i++) {
					lnames[j][i] = lnames[j][i].substring(0, lnames[j][i]
							.length() - 2);
				}
			}
		}
	}

} // end Table

