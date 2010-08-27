package geovista.geoviz.mosaic;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Vector;
import java.util.logging.Logger;

public class MyRect extends Rectangle {

	final static Logger logger = Logger.getLogger(MyRect.class.getName());
	private final int x, y;
	final int w, h;
	private int plusX = 0;
	private int plusY = 1;
	private final String info;
	private final String mode;
	private final double alpha = 1;
	private final boolean alphaSet = false;
	private final boolean censored = false;
	private final char dir;
	private double obs = 1;
	double hilite = 0;
	private final double exp;
	private final double scale;
	private double max;
	private final float p;
	private final Color drawColor = Color.black;
	private final Vector tileIds;
	private Table tablep;

	private double[] Colors;

	// public MyRect(boolean full, char dir, String mode, int x, int y, int w,
	// int h, double obs, double exp, double scale, double p, String info,
	// Vector tileIds, Table tablep) {
	public MyRect(char dir, String mode, int x, int y, int w, int h,
			double obs, double exp, double scale, double p, String info,
			Vector tileIds) {

		super(x, y, w, h);
		this.dir = dir;
		this.exp = exp;
		this.scale = scale;
		this.mode = mode;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.obs = obs;
		this.p = (float) p;
		this.info = info;
		this.tileIds = tileIds;

	}

	private void colorBreakdown() {

		double[] Selection;
		int sels = 0;
		Selection = tablep.data.getSelection();
		Colors = new double[tablep.data.getNumColors() + 1]; // we need one
		// more slot
		// (the highest)
		// for the
		// hilite color
		for (int j = 0; j < tileIds.size(); j++) {
			int id = ((Integer) (tileIds.elementAt(j))).intValue();
			for (int l = 0; l < (tablep.Ids[id]).length; l++) {
				if (Selection[tablep.Ids[id][l]] == 0) {
					if (tablep.count == -1) {
						Colors[tablep.data.colorArray[tablep.Ids[id][l]]]++;
					} else {
						Colors[tablep.data.colorArray[tablep.Ids[id][l]]] += (tablep.data
								.getRawNumbers(tablep.count))[tablep.Ids[id][l]];
					}
				} else if (tablep.count == -1) {
					sels++;
				} else {
					sels += (tablep.data.getRawNumbers(tablep.count))[tablep.Ids[id][l]];
				}
			}
		}
		Colors[0] = sels; // number of selected cases
		Colors[Colors.length - 1] = (int) obs; // number of cases WITHOUT any
		// color
		for (int i = 0; i < Colors.length - 1; i++) {
			Colors[Colors.length - 1] -= Colors[i];
			// for( int i=0; i<Colors.length; i++)
			// logger.info("i: "+i+" Count: "+Colors[i]);
		}
	}

	public void draw(Graphics g) {

		float currAlpha = ((AlphaComposite) ((Graphics2D) g).getComposite())
				.getAlpha();

		if (tablep != null && tablep.data.colorBrush) {
			colorBreakdown();
		}

		// logger.info(residual);
		if (obs > 0) {
			if (dir != 'f') {
				if (info.indexOf("¥") == -1
						&& info.indexOf(": NA\n") == -1
						&& !(info.length() > 2 ? (info.substring(0, 3))
								.equals("NA\n") : false)) {
					// g.setColor(MFrame.objectColor);
				} else {
					g.setColor(Color.white);
				}
				if (tablep != null && tablep.data.colorBrush) {
					if (dir == 'x') {
						int[] ws = roundProportions(Colors, obs, Math.min(w,
								width));
						int altp = x;
						for (int i = 0; i < Colors.length; i++) {
							if (i == Colors.length - 1) {
								// g.setColor(MFrame.objectColor);
							} else if (i == 0) {
								// g.setColor(DragBox.hiliteColor);
							} else {
								g.setColor(tablep.data.getColorByID(i));
							}
							g.fillRect(altp, y + Math.max(0, h - height),
									ws[i], Math.min(h, height) + 1);
							altp += ws[i];
						}
					} else if (dir == 'y') {
						int[] hs = roundProportions(Colors, obs, Math.min(h,
								height));
						int altp = 0;
						for (int i = 0; i < Colors.length; i++) {
							if (i == Colors.length - 1) {
								// g.setColor(MFrame.objectColor);
							} else if (i == 0) {
								// g.setColor(DragBox.hiliteColor);
							} else {
								g.setColor(tablep.data.getColorByID(i));
							}
							g.fillRect(x, y + Math.max(0, h - height)
									+ Math.min(h, height) - altp - hs[i], Math
									.min(w, width), hs[i]);
							altp += hs[i];
						}
					}
				} else {
					if (alphaSet) {
						((Graphics2D) g).setComposite(AlphaComposite
								.getInstance(AlphaComposite.SRC_OVER,
										((float) alpha)));
					}
					g.fillRect(x, y + Math.max(0, h - height), Math.min(w,
							width), Math.min(h, height) + 1);
				}
			} else {
				g.setColor(drawColor);
				if (alphaSet) {
					((Graphics2D) g).setComposite(AlphaComposite.getInstance(
							AlphaComposite.SRC_OVER, (float) alpha));
				}
				g.fillRect(x, y, Math.max(1, w), h);
			}
		}
		if (mode.equals("Expected")) {
			int high = (int) (192 + 63 * (0.15 + Stat.pnorm((1 - p - 0.9) * 10)));
			int low = (int) (192 * (0.85 - Stat.pnorm((1 - p - 0.9) * 10)));
			// logger.info(Stat.pnorm((1-p-0.9)*15));
			if (obs - exp > 0.00001) {
				g.setColor(new Color(low, low, high));
			} else if (obs - exp < -0.00001) {
				g.setColor(new Color(high, low, low));
			} else {
				g.setColor(Color.lightGray);
			}
			double resid = Math.abs((obs - exp) / Math.sqrt(exp));
			// logger.info("Cell: "+getLabel()+" resid: "+resid+" max
			// "+max+ " scale "+scale);
			if (dir == 'x') {
				g.fillRect(x, y, (int) (w * resid / max), h);
			} else if (dir == 'y') {
				g.fillRect(x, y + h - (int) (h * resid / max), w, (int) (h
						* resid / max));
			}
		}

		if (hilite > 0 && (tablep == null || !tablep.data.colorBrush)) { // draw
			// hilite
			// on
			// none
			// color-brushing
			// ...
			Color c = g.getColor();
			// g.setColor(DragBox.hiliteColor);
			// logger.info("w: "+w+" hilite:"+hilite+"wh:
			// "+(int)((double)w*hilite));
			if (Math.min(w, width) > 2 && Math.min(h, height) > 2) { // Mit
				// Rahmen
				plusX = 1;
				plusY = 0;
			}
			if (dir == 'x') {
				int dw = (((int) (w * hilite) == 0) ? 1
						: (((int) (w * hilite) == w - 1) && hilite < 1 && w > 2
								? w - 2 : (int) Math.min(width, (w * hilite))));
				int dh = 1 + Math.min(h, height);

				g.fillRect(x + plusX, y + Math.max(0, h - height), dw, dh);
			} else if (dir == 'y') {
				g.fillRect(x, y
						+ Math.max(0, h - height)
						+ Math.min(h, height)
						- (((int) ((h + plusY) * hilite) == 0) ? (1 - plusY)
								: (int) Math.min(height, (h * hilite))), Math
						.min(w, width), (((int) ((h + plusY) * hilite) == 0)
						? 1 : (int) Math.min(height + plusY,
								((h + plusY) * hilite))));
			} else {
				g.setColor(new Color(255, 0, 0, (int) (255 * hilite)));
				g.fillRect(x, y, w, h);
			}
			g.setColor(c);
		}

		((Graphics2D) g).setComposite(AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, currAlpha));

		if (obs == 0 || censored) {
			g.setColor(Color.red);
		} else {
			// g.setColor(MFrame.lineColor);
		}
		if (dir != 'f' && Math.min(w, width) > 2 && Math.min(h, height) > 2
				|| obs == 0 || censored) {
			g.drawRect(x, y + Math.max(0, h - height), Math.min(w, width), Math
					.min(h, height));
		}
	}

	@SuppressWarnings("unused")
	private String getLabel() {
		String pinfo = info.toString();
		if (obs > 0) {
			pinfo += "\n" + "Count\t " + obs;
		} else {
			pinfo += "\n" + "Empty Bin ";
		}
		if (hilite > 0) {
			pinfo += "\n" + "Hilited\t " + round(hilite * obs, 0) + " ("
					+ round(100 * hilite, 2) + "%)";
		}
		if (mode.equals("Expected")) {
			pinfo += "\n" + "Expected\t " + round(exp, 2);
			pinfo += "\n" + "Residual\t " + round(obs - exp, 3);
			pinfo += "\n"
					+ "Scaled Res.\t"
					+ round(Math
							.abs((obs - exp) / Math.sqrt(exp) * scale * 100), 1)
					+ "%";
		}

		return pinfo;
	}

	private static double round(double x, int n) {
		return Math.round(x * Math.pow(10, n)) / Math.pow(10, n);
	}

	public static int[] roundProportions(double[] votes, double total, int pie) {

		int[] rounds = new int[votes.length];

		int start = -1;
		int stop = votes.length;
		while (votes[++start] == 0) {
		}
		while (votes[--stop] == 0) {
		}
		// logger.info("Start: "+start+" Stop: "+stop);
		int k = 1;
		double eps = 0;
		int sum = 0;
		int converge = 24;
		while (sum != pie && k < 64) {
			k++;
			sum = 0;
			for (int i = start; i <= stop; i++) {
				if (k >= converge) {
					eps = Math.random() - 0.5;
				}
				if (votes[i] < 0.0000000001) {
					rounds[i] = 0;
				} else {
					rounds[i] = (int) Math
							.round((votes[i]) / total * pie + eps);
				}
				sum += rounds[i];
			}
			// logger.info("k: "+k+" eps: "+eps+" sum: "+sum+" pie:
			// "+pie);
			if (sum > pie) {
				eps -= 1 / Math.pow(2, k);
			} else if (sum < pie) {
				eps += 1 / Math.pow(2, k);
			}
		}
		if (sum != pie) {
			logger.info(" Rounding Failed !!!");
		}

		return rounds;
	}
}
