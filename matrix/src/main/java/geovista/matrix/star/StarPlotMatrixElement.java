/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.matrix.star;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JToolBar;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.IndicationListener;
import geovista.geoviz.star.StarPlotCanvas;
import geovista.matrix.MatrixElement;
import geovista.symbolization.BivariateColorSymbolClassification;

/**
 * Suitable for inclusion into multiform matrixes
 * 
 * @author Frank Hardisty
 * 
 */
public class StarPlotMatrixElement extends StarPlotCanvas implements
		MatrixElement {
	DataSetForApps data;

	int[] elementPosition;

	/**
	 * @param data
	 * 
	 *            This method is deprecated becuase it wants to create its very
	 *            own pet DataSetForApps. This is no longer allowed, to allow
	 *            for a mutable, common data set. Use of this method may lead to
	 *            unexpected program behavoir. Please use setDataSet instead.
	 */
	@Deprecated
	public void setData(Object[] data) {
		setDataSet(new DataSetForApps(data));

	}

	public void setDataSet(DataSetForApps data) {
		DataSetEvent e = new DataSetEvent(data, this);
		super.dataSetChanged(e);

	}

	public void setDataIndices(int[] dataIndices) {
		elementPosition = dataIndices;
		// xxx update
	}

	public int[] getElementPosition() {
		return elementPosition;
	}

	// For axes of scatter plot.
	// a noop for this class
	public void setAxisOn(boolean axisOn) {
	}

	public void setSelectionColor(Color c) {
		// xxx implement me
	}

	public void setSelOriginalColorMode(boolean selOriginalColorMode) {
		// xxx implement me?
	}

	public void setMultipleSelectionColors(Color[] c) {
		// xxx implement me
	}

	public Vector getSelectedObservations() {
		// xxx implement me
		return null;
	}

	@Override
	public void setIndication(int indication) {
		// xxx implement me
	}

	@Override
	public void addIndicationListener(IndicationListener ind) {
		// xxx implement me
	}

	@Override
	public void removeIndicationListener(IndicationListener ind) {
		// xxx implement me
	}

	public void setSelectedObservations(Vector selectedObservations) {
		// xxx implement me
	}

	public void setSelections(int[] selections) {
		// xxx implement me
	}

	public int[] getSelections() {
		// xxx implement me
		return null;
	}

	public void setConditionArray(int[] conditionArray) {
		// xxx implement me
	}

	public void addActionListener(ActionListener l) {
		// xxx implement me
	}

	// Set min and max for axes. xAxisExtents[0] = min, [1] = max.
	public void setXAxisExtents(double[] xAxisExtents) {
		// xxx implement me
	}

	public void setYAxisExtents(double[] yAxisExtents) {
		// xxx implement me
	}

	public double[] getXAxisExtents() {
		// noop
		return null;
	}

	public double[] getYAxisExtents() {
		// noop
		return null;
	}

	public JToolBar getTools() {
		return null;

	}

	public void setBivarColorClasser(
			BivariateColorSymbolClassification bivarColorClasser,
			boolean reverseColor) {
		// super isn't really a bivariate component, so let's handle this here
		double[] data = this.data.getNumericDataAsDouble(elementPosition[0]);
		Color[] obsColors = bivarColorClasser.symbolize(data, data);
		super.setStarFillColors(obsColors);

	}

	public BivariateColorSymbolClassification getBivarColorClasser() {
		// we don't really use this.
		return null;
	}

	public String getShortDiscription() {
		return "STARPLOT";
	}

}
