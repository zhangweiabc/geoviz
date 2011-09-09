/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.matrix;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JToolBar;

import geovista.common.data.DataSetForApps;
import geovista.common.event.IndicationListener;
import geovista.symbolization.BivariateColorClassifier;

public interface MatrixElement {

	static final String COMMAND_POINT_SELECTED = "cmdSel";
	static final String COMMAND_DATARANGE_SET = "cmdset";
	static final String COMMAND_COLOR_CLASSFICIATION = "colorClass";

	public void setDataSet(DataSetForApps data);

	public void setDataIndices(int[] dataIndices);

	public int[] getElementPosition();

	// For axes of scatter plot.
	public void setAxisOn(boolean axisOn);

	public void setBackground(Color c);

	public void setSelectionColor(Color c);

	public void setSelOriginalColorMode(boolean selOriginalColorMode);

	public void setMultipleSelectionColors(Color[] c);

	public Vector getSelectedObservations();

	public void setIndication(int indication);

	public void addIndicationListener(IndicationListener ind);

	public void removeIndicationListener(IndicationListener ind);

	public void setSelectedObservations(Vector selectedObservations);

	public void setSelections(int[] selections);

	public int[] getSelections();

	public void setConditionArray(int[] conditionArray);

	public void addActionListener(ActionListener l);

	// Set min and max for axes. xAxisExtents[0] = min, [1] = max.
	public void setXAxisExtents(double[] xAxisExtents);

	public void setYAxisExtents(double[] yAxisExtents);

	public double[] getXAxisExtents();

	public double[] getYAxisExtents();

	public void setBivarColorClasser(
			BivariateColorClassifier bivarColorClasser,
			boolean reverseColor);

	public BivariateColorClassifier getBivarColorClasser();

	public String getShortDiscription();

	public JToolBar getTools();
}
