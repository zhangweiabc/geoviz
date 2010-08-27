package geovista.geoviz.scatterplot;

/**
 * Title: ScatterPlot
 * Description: construct a scatterplot
 * Copyright:    Copyright (c) 2001
 * Company: GeoVISTA Center
 * @author Xiping Dai
 * 
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JDialog;
import javax.swing.JFrame;

import geovista.common.classification.BoundaryClassifier;
import geovista.symbolization.BivariateColorSymbolClassification;

/**
 * put your documentation comment here
 */
public class ScatterPlotWithBackground extends ScatterPlotBasic {
	transient protected BoundaryClassifier xClasser = null;
	transient protected BoundaryClassifier yClasser = null;
	transient protected double[] xBoundaries;
	transient protected double[] yBoundaries;
	transient protected int[] xBoundariesInt;
	transient protected int[] yBoundariesInt;
	transient protected Color[][] classColors;
	transient protected ScatterPlotWithBackground detailSP;
	transient protected JFrame dummyFrame;
	transient protected JDialog dlgSP;

	/**
	 * put your documentation comment here
	 */
	public ScatterPlotWithBackground() {
		super();
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param String
	 *            attributeXName
	 * @param String
	 *            attributeYName
	 * @param double[] dataX
	 * @param double[] dataY
	 * @param boolean axisOn
	 */
	public ScatterPlotWithBackground(Object[] dataObject, int[] dataIndices,
			boolean axisOn, Color c) {
		super();
		this.dataObject = dataObject;
		attributeArrayNames = (String[]) dataObject[0];
		int len = attributeArrayNames.length;
		if (dataObject[len + 1] == null) {
			observNames = null;
		} else {
			observNames = (String[]) dataObject[len + 1];
		}
		this.dataIndices = dataIndices;
		// convert Object array to double arrays.
		axisDataSetup();
		// initialize();
		this.axisOn = axisOn;
		background = c;
		if (c == Color.black) {
			foreground = Color.white;
		} else {
			foreground = Color.black;
		}
		initialize();
	}

	@Override
	public String getShortDiscription() {
		return "XYP";
	}

	/**
	 * Set up data and axis for drawing the scatter plot.
	 */
	@Override
	protected void initialize() {
		super.initialize();
		// Set up class boundaries, which will be plotted at the background.
		makeBoundaries();
	}

	/**
	 * Draw the scatter plot.
	 * 
	 * @param g
	 */
	@Override
	public void paintComponent(Graphics g) {

		if (dataIndices == null) {
			return;
		}
		g.setColor(background);
		g.fillRect(0, 0, getSize().width, getSize().height);
		g.setColor(foreground);
		paintBorder(g);

		if (axisOn) {
			drawAxis(g);
		}
		drawPlot(g);
		Graphics2D g2 = (Graphics2D) g;
		if (exLabels != null && axisOn == true) {
			setToolTipText("");
			exLabels.paint(g2);
		}
		// add by Jin Chen for indication
		if (indiationId >= 0) {
			drawIndication(g2, indiationId);
		}

	}

	/**
	 * Draw pot (points) on the screen.
	 * 
	 * @param g
	 */
	@Override
	protected void drawPlot(Graphics g) {

		drawClassBackground(g);
		super.drawPlot(g);
		if (multipleSelectionColors != null) {
			for (int i = 0; i < dataX.length; i++) {
				if (multipleSelectionColors[i] != null) {
					// g.setColor(multipleSelectionColors[i]);
					g.setColor(foreground);
					g.drawOval(exsint[i] - 1, whyint[i] - 1, pointSize,
							pointSize);
					// g.fillOval(exsint[i] - 1, whyint[i] -1 , pointSize,
					// pointSize);
				}
			}
		}

	}

	protected void drawClassBackground(Graphics g) {
		// draw background in colors.
		if (xClasser != null) {
			logger.finest("classer is not null");
			for (int i = 0; i < classColors.length; i++) {
				for (int j = 0; j < classColors[0].length; j++) {
					g.setColor(classColors[i][j]);
					g.fillRect(xBoundariesInt[i], yBoundariesInt[j + 1],
							xBoundariesInt[i + 1] - xBoundariesInt[i],
							yBoundariesInt[j] - yBoundariesInt[j + 1]);
				}
			}
		}

	}

	@Override
	protected void drawSelections(Graphics g, int len) {
		if (pointSelected == false) { // only draw original points.
			for (int i = 0; i < len; i++) {
				if ((exsint[i] <= plotEndX) && (exsint[i] >= plotOriginX)
						&& (whyint[i] <= plotOriginY)
						&& (whyint[i] >= plotEndY) && (conditionArray[i] > -1)) {
					g.setColor(foreground);
					// g.setColor(colorNonSelected[i]);
					// g.drawOval(exsint[i] - 1, whyint[i] - 1, pointSize,
					// pointSize);
					g.fillOval(exsint[i] - 2, whyint[i] - 2, pointSize,
							pointSize);
				}
			}
		} else { // draw original points and selected points.
			// according to the color mode, draw selected points and
			// non-selected points.
			if (selOriginalColorMode == false) {
				for (int i = 0; i < len; i++) {
					if ((exsint[i] <= plotEndX) && (exsint[i] >= plotOriginX)
							&& (whyint[i] <= plotOriginY)
							&& (whyint[i] >= plotEndY)
							&& (conditionArray[i] > -1)) {
						g.setColor(foreground);
						// g.setColor(colorNonSelected[i]);
						// g.drawOval(exsint[i] - 1, whyint[i] - 1, pointSize,
						// pointSize);
						g.fillOval(exsint[i] - 2, whyint[i] - 2, pointSize,
								pointSize);
					}
				}
				for (int i = 0; i < len; i++) {
					g.setColor(selectionColor);
					if ((exsint[i] <= plotEndX) && (exsint[i] >= plotOriginX)
							&& (whyint[i] <= plotOriginY)
							&& (whyint[i] >= plotEndY)
							&& (conditionArray[i] > -1)) {

						if (selections[i] == 1) {
							// g.drawOval(exsint[i] - 1, whyint[i] - 1,
							// pointSize, pointSize);
							g.fillOval(exsint[i] - 2, whyint[i] - 2, pointSize,
									pointSize);
						}
					}
				}
			} else {
				for (int i = 0; i < len; i++) {
					if ((exsint[i] <= plotEndX) && (exsint[i] >= plotOriginX)
							&& (whyint[i] <= plotOriginY)
							&& (whyint[i] >= plotEndY)
							&& (conditionArray[i] > -1)) {
						g.setColor(foreground);
						// g.setColor(colorNonSelected[i]);
						g.drawOval(exsint[i] - 2, whyint[i] - 2, pointSize - 2,
								pointSize - 2);
					}
				}
				for (int i = 0; i < len; i++) {
					if ((exsint[i] <= plotEndX) && (exsint[i] >= plotOriginX)
							&& (whyint[i] <= plotOriginY)
							&& (whyint[i] >= plotEndY)
							&& (conditionArray[i] > -1)) {
						// g.setColor(colorNonSelected[i]);
						g.setColor(foreground);
						if (selections[i] == 1) {
							// g2.drawOval(exsint[i] - 1, whyint[i] - 1,
							// pointSize, pointSize);
							g.fillOval(exsint[i] - 2, whyint[i] - 2, pointSize,
									pointSize);
							// g2.fillRect(exsint[i] - 1, whyint[i] - 1,
							// pointSize, pointSize);
							// g.drawOval(exsint[i] - 1, whyint[i] - 1,
							// pointSize+1, pointSize+1);
						}
					}
				}
			}
		}
	}

	@Override
	protected void setupDataforDisplay() {

		logger.finest("In setup data for display ..." + xAxisExtents[0]);
		setVisibleAxis(axisOn);
		if (dataArrayX == null) {
			return;
		}
		int len = dataArrayX.length();
		if (len != dataArrayY.length()) {
			return;
		}
		// exsint = new int[len];
		// whyint = new int[len];
		// get positions on screen
		double xScale;
		double yScale;
		xScale = getScale(plotOriginX, plotEndX, xAxisExtents[0],
				xAxisExtents[1]);
		exsint = getValueScreen(dataX, xScale, plotOriginX, xAxisExtents[0]);
		yScale = getScale(plotOriginY, plotEndY, yAxisExtents[0],
				yAxisExtents[1]);
		whyint = getValueScreen(dataY, yScale, plotOriginY, yAxisExtents[0]);
		// get class boundaries' positions on screen
		if (xBoundaries != null && yBoundaries != null) {
			logger.finest("x and y boundaries are not null.");
			xBoundariesInt = new int[xBoundaries.length];
			yBoundariesInt = new int[yBoundaries.length];
			xBoundariesInt = getValueScreen(xBoundaries, xScale, plotOriginX,
					xAxisExtents[0]);
			yBoundariesInt = getValueScreen(yBoundaries, yScale, plotOriginY,
					yAxisExtents[0]);
		}
	}

	/**
	 * Sets colors for the current data.
	 */
	@Override
	public void setBivarColorClasser(
			BivariateColorSymbolClassification bivarColorClasser,
			boolean reverseColor) {
		this.bivarColorClasser = bivarColorClasser;
		makeColors();
		// this.classColors = this.bivarColorClasser.getClassColors();
		makeBoundaries();
		repaint();
	}

	@Override
	public BivariateColorSymbolClassification getBivarColorClasser() {
		return bivarColorClasser;
	}

	protected void makeBoundaries() {
		int numClasses;
		try {
			if (dataX != null) {
				xClasser = (BoundaryClassifier) bivarColorClasser.getClasserX();
				numClasses = bivarColorClasser.getXColorSymbolizer()
						.getNumClasses();
				logger.finest("num classes" + numClasses);
				xBoundaries = xClasser.getEqualBoundaries(dataX, numClasses);
				yClasser = (BoundaryClassifier) bivarColorClasser.getClasserY();
				numClasses = bivarColorClasser.getYColorSymbolizer()
						.getNumClasses();
				yBoundaries = yClasser.getEqualBoundaries(dataY, numClasses);
				setupDataforDisplay();
			}
		} catch (ClassCastException ex) {
		}
	}

	@Override
	public void makeColors() {
		classColors = bivarColorClasser.getClassColors();
		if (dataX != null && dataY != null) {
			pointColors = bivarColorClasser.symbolize(dataX, dataY);
		}
	}

}
