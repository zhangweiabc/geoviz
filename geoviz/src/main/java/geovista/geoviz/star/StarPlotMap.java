/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.star;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JPanel;

import geovista.common.data.DataSetForApps;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.DataSetEvent;
import geovista.common.event.IndicationEvent;
import geovista.common.event.SubspaceEvent;
import geovista.geoviz.map.GeoMap;
import geovista.geoviz.visclass.VisualClassifier;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.symbolization.glyph.Glyph;
import geovista.symbolization.glyph.GlyphEvent;
import geovista.symbolization.glyph.GlyphListener;

/**
 * Paints an array of StarPlotMap. Responds to and broadcasts DataSetChanged,
 * IndicationChanged etc. events.
 * 
 * @author Frank Hardisty
 * 
 */
public class StarPlotMap extends GeoMap implements GlyphListener {

	StarPlotLegend starLeg;

	StarPlotLayer starLayer;

	VisualClassifier starColorer;

	int indication;

	int defaultGlyphSize = 5;

	DataSetForApps data;

	static boolean DEBUG = true;

	boolean settingUp = true;

	public StarPlotMap() {
		super();
		starColorer = new VisualClassifier();
		starLayer = new StarPlotLayer();
		starLeg = new StarPlotLegend();

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(starLeg, BorderLayout.CENTER);
		southPanel.add(starColorer, BorderLayout.NORTH);
		this.add(southPanel, BorderLayout.SOUTH);
		starColorer.addColorArrayListener(this);

	}

	public void glyphChanged(GlyphEvent e) {
		// this.mapCan.glyphChanged(e);
	}

	@Override
	public void dataSetChanged(DataSetEvent e) {
		settingUp = true;
		super.dataSetChanged(e);
		data = e.getDataSetForApps();
		starLayer.dataSetChanged(e);
		starColorer.setDataSet(e.getDataSetForApps());
		setLegendIndication(0);
		int nNumericVars = e.getDataSetForApps().getNumberNumericAttributes();
		if (nNumericVars > 6) {
			nNumericVars = 6;
		}
		int[] selectedVars = new int[nNumericVars];
		for (int i = 0; i < nNumericVars; i++) {
			selectedVars[i] = i;
		}
		SubspaceEvent subE = new SubspaceEvent(this, selectedVars);
		subspaceChanged(subE);
		mapCan.setGlyphs(createGlyphs());
		settingUp = false;
	}

	private Glyph[] createGlyphs() {

		Rectangle[] plotLocs = new Rectangle[data.getNumObservations()];

		for (int i = 0; i < plotLocs.length; i++) {
			plotLocs[i] = new Rectangle(defaultGlyphSize, defaultGlyphSize);
		}

		starLayer.setPlotLocations(plotLocs);

		Color[] starColors = starColorer.findDataColors();
		starLayer.setStarFillColors(starColors);
		return starLayer.findGlyphs();
	}

	@Override
	public void subspaceChanged(SubspaceEvent e) {
		starLayer.subspaceChanged(e);

		mapCan.setGlyphs(createGlyphs());

		String[] varNames = starLayer.getVarNames();
		starLeg.setVariableNames(varNames);
		double[] values = starLayer.getValues(indication);
		starLeg.setValues(values);

		setLegendIndication(indication);
	}

	@Override
	public void colorArrayChanged(ColorArrayEvent e) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("in starplotmap colorarraychanged, got colors");
		}
		if (e == null || e.getColors() == null) {
			return;
		}
		Color[] starColors = e.getColors();
		starLayer.setStarFillColors(starColors);
		mapCan.setGlyphs(starLayer.findGlyphs());// XXX should not
		// redo glyphs here
	}

	@Override
	public void indicationChanged(IndicationEvent e) {
		if (settingUp) {
			return;
		}

		int ind = e.getIndication();
		if (ind > starLayer.getDataSet().getNumObservations()) {
			logger.severe("got indication greater than data set size, ind = "
					+ ind);
			return;
		}
		if (e.getSource() != starLayer) {
			starLayer.indicationChanged(e);
		}
		setLegendIndication(ind);
		super.indicationChanged(e);

	}

	private void setLegendIndication(int ind) {
		if (ind >= 0) {
			starLeg.setObsName(starLayer.getObservationName(ind));
			String[] varNames = starLayer.getVarNames();
			double[] values = starLayer.getValues(ind);
			int[] spikeLengths = starLayer.getSpikeLengths(ind);
			// we need this check, or we start blowing null pointer exceptions
			// if the we get an indication during iniditalization.
			if (spikeLengths == null) {
				return;
			}
			starLeg.setValues(values);
			starLeg.setVariableNames(varNames);

			starLeg.setSpikeLengths(spikeLengths);
			Color starColor = starLayer.getStarFillColor(ind);
			starLeg.setStarFillColor(starColor);

		}

	}

	public static void main(String[] args) {
		StarPlotMap map = new StarPlotMap();
		JFrame frame = new JFrame("StarPlot Map");
		frame.add(map);
		frame.pack();
		frame.setVisible(true);
		GeoDataGeneralizedStates geodata = new GeoDataGeneralizedStates();
		DataSetEvent e = new DataSetEvent(geodata.getDataForApps(), geodata);
		map.dataSetChanged(e);

	}

}
