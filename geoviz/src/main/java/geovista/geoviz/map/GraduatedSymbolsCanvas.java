/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.map;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import geovista.common.data.DataSetForApps;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.DataSetEvent;
import geovista.common.event.IndicationEvent;
import geovista.common.event.SubspaceEvent;
import geovista.geoviz.visclass.VisualClassifier;
import geovista.readers.example.GeoData48States;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.readers.shapefile.ShapeFileToShape;
import geovista.symbolization.glyph.Glyph;
import geovista.symbolization.glyph.GlyphEvent;
import geovista.symbolization.glyph.GlyphListener;

/**
 * Paints an array of GraduatedSymbolsMap. Responds to and broadcasts
 * DataSetChanged, IndicationChanged etc. events.
 * 
 * @author Frank Hardisty
 * 
 */
public class GraduatedSymbolsCanvas extends GeoMap implements GlyphListener {

    NGonGroup ngonLayer;

    VisualClassifier ngonColorer;

    int indication;

    int defaultGlyphSize = 50;

    DataSetForApps data;

    static boolean DEBUG = true;

    boolean settingUp = true;

    // GlyphComboBox glyphCombo;
    // GlyphSizePicker glyphPick;

    public GraduatedSymbolsCanvas() {
	super();
	ngonColorer = new VisualClassifier();
	ngonLayer = new NGonGroup();

	ngonColorer.addColorArrayListener(this);

    }

    public MapCanvas getCanvas() {
	return mapCan;
    }

    private void setNSides(int sides) {
	ngonLayer.setNSides(sides);
	mapCan.setGlyphs(ngonLayer.getNGons());
    }

    public void glyphChanged(GlyphEvent e) {
	// XXX noop
	// this.mapCan.glyphChanged(e);
    }

    @Override
    public void dataSetChanged(DataSetEvent e) {
	settingUp = true;
	super.dataSetChanged(e);
	data = e.getDataSetForApps();
	ngonLayer.dataSetChanged(e);
	ngonColorer.setDataSet(e.getDataSetForApps());
	setLegendIndication(0);
	int nNumericVars = e.getDataSetForApps().getNumberNumericAttributes();
	if (nNumericVars > 3) {
	    nNumericVars = 3;
	}
	int[] selectedVars = new int[nNumericVars];
	for (int i = 0; i < nNumericVars; i++) {
	    selectedVars[i] = i;
	}
	SubspaceEvent subE = new SubspaceEvent(this, selectedVars);
	subspaceChanged(subE);

	Glyph[] sbs = createGlyphs();
	mapCan.setGlyphs(sbs);
	settingUp = false;
    }

    private Glyph[] createGlyphs() {

	Rectangle[] plotLocs = new Rectangle[data.getNumObservations()];

	for (int i = 0; i < plotLocs.length; i++) {
	    plotLocs[i] = new Rectangle(defaultGlyphSize, defaultGlyphSize);
	}

	ngonLayer.setPlotLocations(plotLocs);

	Color[] starColors = ngonColorer.findDataColors();
	ngonLayer.setStarFillColors(starColors);
	return ngonLayer.findGlyphs();
    }

    @Override
    public void subspaceChanged(SubspaceEvent e) {
	ngonLayer.subspaceChanged(e);

	mapCan.setGlyphs(createGlyphs());

	// String[] varNames = ngonLayer.getVarNames();

	// double[] values = ngonLayer.getValues(indication);

	setLegendIndication(indication);
    }

    @Override
    public void colorArrayChanged(ColorArrayEvent e) {
	if (GeoMap.logger.isLoggable(Level.FINEST)) {
	    logger.finest("in GraduatedSymbolsMap colorarraychanged, got colors");
	}
	if (e == null || e.getColors() == null) {
	    return;
	}
	Color[] starColors = e.getColors();
	ngonLayer.setStarFillColors(starColors);
	mapCan.setGlyphs(ngonLayer.findGlyphs());// XXX should not
	// redo glyphs here
    }

    @Override
    public void indicationChanged(IndicationEvent e) {
	if (settingUp) {
	    return;
	}

	int ind = e.getIndication();
	if (ind > ngonLayer.getDataSet().getNumObservations()) {
	    logger.severe("got indication greater than data set size, ind = "
		    + ind);
	    return;
	}
	if (e.getSource() != ngonLayer) {
	    ngonLayer.indicationChanged(e);
	}
	setLegendIndication(ind);
	super.indicationChanged(e);

    }

    private void setLegendIndication(int ind) {
	if (ind >= 0) {
	    // starLeg.setObsName(ngonLayer.getObservationName(ind));
	    // String[] varNames = ngonLayer.getVarNames();
	    // double[] values = ngonLayer.getValues(ind);
	    // int[] spikeLengths = ngonLayer.getSpikeLengths(ind);
	    // // we need this check, or we start blowing null pointer
	    // exceptions
	    // // if the we get an indication during iniditalization.
	    // if (spikeLengths == null) {
	    // return;
	    // }
	    // starLeg.setValues(values);
	    // starLeg.setVariableNames(varNames);
	    //
	    // starLeg.setSpikeLengths(spikeLengths);
	    // Color starColor = ngonLayer.getStarFillColor(ind);
	    // starLeg.setStarFillColor(starColor);

	}

    }

    public static void main2(String[] args) {
	GraduatedSymbolsCanvas map = new GraduatedSymbolsCanvas();
	JFrame frame = new JFrame("Graduated Symbols Canvas");
	frame.add(map);
	frame.pack();
	frame.setVisible(true);
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	GeoDataGeneralizedStates geodata = new GeoDataGeneralizedStates();
	DataSetEvent e = new DataSetEvent(geodata.getDataForApps(), geodata);
	map.dataSetChanged(e);

    }

    public static void main(String[] args) {
	boolean useProj = true;

	// GeoMap map = new GeoMap();
	JFrame app = new JFrame("proportion map Main Class");
	app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	String fileName = "C:\\Users\\Frank\\Documents\\ArcGIS\\mid_east.shp";
	ShapeFileDataReader shpRead = new ShapeFileDataReader();
	shpRead.setFileName(fileName);
	// CoordinationManager coord = new CoordinationManager();
	ShapeFileToShape shpToShape = new ShapeFileToShape();
	ShapeFileProjection shpProj = new ShapeFileProjection();
	GeoData48States stateData = new GeoData48States();
	// coord.addBean(map);
	// coord.addBean(shpToShape);
	shpProj.setInputDataSetForApps(shpRead.getDataForApps());

	// geoDsa = shpProj.getOutputDataSetForApps();

	// shpToShape.setInputDataSetForApps(geoDsa);

	// map.setDataSet(geoDsa);

	String citiesFileName = "D:\\temp\\shapefiles\\capitals_1_million.shp";
	citiesFileName = "D:/temp/shapefiles/capitals_1_million.shp";
	// citiesFileName = "D:/temp/shapefiles/yeman_roads.shp";
	// citiesFileName = "D:\\temp\\shapefiles\\yeman_roads";
	ShapeFileDataReader shpRead2 = new ShapeFileDataReader();
	shpRead2.setFileName(citiesFileName);
	// ShapeFileProjection shpProj2 = new ShapeFileProjection();

	// shpProj2.setInputAuxiliaryData(shpRead2.getDataForApps());
	// DataSetForApps citiesDSA = shpProj2
	// .getOutputAuxiliarySpatialDataForApps();
	// shpToShape.setInputDataSetForApps(citiesDSA);
	// map.setDataSet(shpRead2.getDataForApps());
	// map2.setAuxiliarySpatialData(shpRead2.getDataForApps());
	// map.setCurrColorColumnX(1);
	// map.setCurrColorColumnY(2);

	shpRead.setFileName(fileName);
	// CoordinationManager coord = new CoordinationManager();
	// ShapeFileToShape shpToShape = new ShapeFileToShape();
	// ShapeFileProjection shpProj = new ShapeFileProjection();

	// coord.addBean(map2);
	// coord.addBean(shpToShape);

	if (useProj) {
	    // stateData.addActionListener(shpProj);
	    shpProj.setInputDataSet(shpRead.getDataSet());
	}

	Object[] dataSet = null;
	if (useProj) {
	    dataSet = shpProj.getOutputDataSet();
	} else {
	    dataSet = shpRead.getDataSet();
	}

	shpToShape.setInputDataSet(dataSet);

	shpProj.setInputAuxiliaryData(shpRead2.getDataSet());

	shpToShape.setInputDataSet(shpProj.getOutputAuxiliarySpatialData());
	GraduatedSymbolsCanvas map2 = new GraduatedSymbolsCanvas();
	// map2.setUseHistogram(true);
	app.getContentPane().add(map2);
	map2.setDataSet(shpToShape.getOutputDataSetForApps());
	map2.setAuxiliarySpatialData(shpProj.getOutputDataSetForApps());

	app.getContentPane().setLayout(
		new BoxLayout(app.getContentPane(), BoxLayout.X_AXIS));
	// app.getContentPane().add(map);
	app.pack();
	app.setVisible(true);

    }

}
