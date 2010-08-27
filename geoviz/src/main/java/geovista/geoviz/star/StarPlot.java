/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.star;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.common.classification.ClassifierPicker;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.ColorArrayListener;
import geovista.common.event.ColumnAppendedEvent;
import geovista.common.event.ColumnAppendedListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.geoviz.visclass.VisualClassifier;

/**
 * Paints an array of StarPlot. Responds to and broadcasts DataSetChanged,
 * IndicationChanged etc. events.
 * 
 * @author Frank Hardisty
 * 
 */
public class StarPlot extends JPanel implements DataSetListener,
		ColumnAppendedListener, IndicationListener, SubspaceListener,
		ColorArrayListener, TableModelListener, ActionListener {

	StarPlotCanvas starCan;
	StarPlotLegend starLeg;
	VisualClassifier vc;
	int indication;
	final static Logger logger = Logger.getLogger(StarPlot.class.getName());

	JComboBox methodCombo;

	@SuppressWarnings("deprecation")
	public StarPlot() {
		vc = new VisualClassifier();
		starCan = new StarPlotCanvas();
		starLeg = new StarPlotLegend();
		vc.getClassPick().addActionListener(this);
		setLayout(new BorderLayout());
		methodCombo = new JComboBox(StarPlotLayer.ScaleMethod.values());
		// this.add(methodCombo, BorderLayout.WEST);
		methodCombo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (methodCombo.getSelectedItem().equals(
						StarPlotLayer.ScaleMethod.Linear)) {
					starCan.getPlotLayer().setMethod(
							StarPlotLayer.ScaleMethod.Linear);
				}
				if (methodCombo.getSelectedItem().equals(
						StarPlotLayer.ScaleMethod.Rank_Order)) {
					starCan.getPlotLayer().setMethod(
							StarPlotLayer.ScaleMethod.Rank_Order);
				}
				if (methodCombo.getSelectedItem().equals(
						StarPlotLayer.ScaleMethod.Normalized)) {
					starCan.getPlotLayer().setMethod(
							StarPlotLayer.ScaleMethod.Normalized);
				}
				if (methodCombo.getSelectedItem().equals(
						StarPlotLayer.ScaleMethod.Log)) {
					starCan.getPlotLayer().setMethod(
							StarPlotLayer.ScaleMethod.Log);
				}

				StarPlot.this.repaint();
			}
		});

		this.add(starCan, BorderLayout.CENTER);
		this.add(starLeg, BorderLayout.SOUTH);
		this.add(vc, BorderLayout.NORTH);
		starCan.addIndicationListener(this);
		vc.addColorArrayListener(this);

		String[] names = { "toy", "data" };
		double[] toy = { 1, 2, 3 };
		double[] set = { 4, 5, 6 };
		Object[] toySet = { names, toy, set };
		dataSetChanged(new DataSetEvent(this, toySet));
		starLeg.isInitializing = false;
	}

	public void dataSetChanged(DataSetEvent e) {
		starLeg.isInitializing = true;

		starCan.dataSetChanged(e);
		vc.setDataSet(e.getDataSetForApps());

		Color[] starColors = vc.getColorForObservations();
		starCan.setStarFillColors(starColors);
		int nNumericVars = starCan.getDataSet().getNumberNumericAttributes();
		if (nNumericVars > 6) {
			nNumericVars = 6;
		}
		int[] selectedVars = new int[nNumericVars];
		for (int i = 0; i < nNumericVars; i++) {
			selectedVars[i] = i;
		}
		SubspaceEvent subE = new SubspaceEvent(this, selectedVars);
		subspaceChanged(subE);
		// this.setLegendIndication(0);
		starLeg.isInitializing = false;
	}

	public void dataSetModified(ColumnAppendedEvent e) {

	}

	public void subspaceChanged(SubspaceEvent e) {
		starCan.subspaceChanged(e);
		setLegendIndication(indication);
	}

	public void colorArrayChanged(ColorArrayEvent e) {
		Color[] starColors = e.getColors();
		starCan.setStarFillColors(starColors);
	}

	public void indicationChanged(IndicationEvent e) {
		int ind = e.getIndication();
		if (e.getSource() != starCan) {
			starCan.indicationChanged(e);
		}
		setLegendIndication(ind);

	}

	private void setLegendIndication(int ind) {

		if (ind > starCan.getDataSet().getNumObservations()) {
			logger.severe("got indication greater than data set size, ind = "
					+ ind);
			return;
		}
		if (ind >= 0) {
			starLeg.setObsName(starCan.getObservationName(ind));
			String[] varNames = starCan.getVarNames();
			double[] values = starCan.getValues(ind);
			if (values == null) {
				return;
			}
			int[] spikeLengths = starCan.getSpikeLengths(ind);
			starLeg.setValues(values);
			starLeg.setVariableNames(varNames);
			if (spikeLengths == null) {
				return;
			}
			starLeg.setSpikeLengths(spikeLengths);
			Color starColor = starCan.getStarFillColor(ind);
			if (starColor == null) {
				return;
			}
			starLeg.setStarFillColor(starColor);

		}

	}

	/**
	 * adds an IndicationListener to the component
	 */
	public void addIndicationListener(IndicationListener l) {
		starCan.addIndicationListener(l);
	}

	/**
	 * removes an IndicationListener from the component
	 */
	public void removeIndicationListener(IndicationListener l) {
		starCan.removeIndicationListener(l);
	}

	public void tableChanged(TableModelEvent e) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Starplot, got a data set, id = "
					+ starCan.getDataSet().hashCode());
		}
		vc.setDataSet(starCan.getDataSet());

	}

	public StarPlotCanvas getStarCan() {
		return starCan;
	}

	public void actionPerformed(ActionEvent arg0) {
		String actionCommand = arg0.getActionCommand();
		if (actionCommand
				.equals(ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED)) {
			int varIndex = vc.getCurrVariableIndex();
			starCan.setCurrentVar(varIndex);
		}

	}

}
