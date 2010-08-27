package geovista.geoviz.scatterplot;

/**
 * Title: ScatterPlot
 * Description: construct a scatterplot
 * Copyright:    Copyright (c) 2001
 * Company: GeoVISTA Center
 * @author Xiping Dai
 * @author Jin Chen
 * 
 */
import java.awt.Color;
import java.awt.Component;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import geovista.common.jts.EmptyShape;
import geovista.common.ui.ShapeReporter;

/**
 * put your documentation comment here
 */
public class ScatterPlot extends ScatterPlotBasic implements ShapeReporter

{
	public static final Color COLOR_NOSELECTED = new Color(204, 204, 204);
	transient protected ScatterPlot detailSP;
	transient boolean externalColor = false;

	/**
	 * put your documentation comment here
	 */
	public ScatterPlot() {
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
	 * @param boolean plotLine
	 * @param double slope
	 * @param double intercept
	 */
	public ScatterPlot(Object[] dataObject, int[] dataIndices, boolean axisOn,
			Color c) {
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

		this.axisOn = axisOn;

		background = c;
		if (c == Color.black) {
			foreground = Color.white;
		} else {
			foreground = Color.black;
		}
		initialize();
	}

	/*
	 * @Override public void setColorArrayForObs(Color[] colorArray) { //
	 * this.colorArrayForObs = colorArray; if (colorArray != null) { pointColors
	 * = colorArray; externalColor = true; } if (pointColors == null) {
	 * logger.finest("In SP... pointColors null"); } this.repaint(); }
	 */
	public Color[] getColors() {
		return pointColors;
	}

	@Override
	public String getShortDiscription() {
		return "XYP";
	}

	/**
	 * Set up data and axis for drawing the scatter plot.
	 */
	@Override
	public void initialize() {
		super.initialize();
		// added for colors
		if (externalColor == false) {
			makeColors();
		}

	}

	/**
	 * Sets colors for the current data.
	 */
	/*
	 * @Override public void setBivarColorClasser(
	 * BivariateColorSymbolClassification bivarColorClasser, boolean
	 * reverseColor) { this.bivarColorClasser = bivarColorClasser; makeColors();
	 * repaint(); }
	 * 
	 * @Override public BivariateColorSymbolClassification
	 * getBivarColorClasser() { return bivarColorClasser; }
	 */
	public Component renderingComponent() {
		return this;
	}

	public Shape reportShape() {
		if (indiationId >= 0) {
			return new Ellipse2D.Float(exsint[indiationId],
					whyint[indiationId], pointSize, pointSize);
		}
		return EmptyShape.INSTANCE;
	}
	/*
	 * public void setColorArrayForObs(Color[] colors) {
	 * 
	 * }
	 */
}
