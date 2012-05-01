/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SubspaceEvent;
import geovista.symbolization.glyph.Glyph;
import geovista.symbolization.glyph.NGon;

/**
 * 
 * Forms NGon[] out of data given to it. Paints NGon[] onto a Graphics2D object
 * passed in.
 * 
 * 
 * @author Frank Hardisty
 * 
 */
public class NGonGroup implements DataSetListener, IndicationListener {

    private transient NGon[] nGons;
    private transient DataSetForApps dataSet;
    private transient int indication = -1;
    private transient float penWidth;
    private transient int[] varList;
    private transient int[] obsList; // in case we want to subset for some
    // reason, i.e. selection or
    // conditioning
    private transient Color[] starColors;
    private transient final HashMap obsHashMap;
    // private static boolean DEBUG = false;

    public static final int MAX_N_VARS = 6;

    public NGonGroup() {

	obsHashMap = new HashMap();

    }

    public void dataSetChanged(DataSetEvent e) {
	indication = -1;
	dataSet = e.getDataSetForApps();
	initStarPlots(dataSet.getNumObservations());

	int nVars = dataSet.getNumberNumericAttributes();
	int maxVars = NGonGroup.MAX_N_VARS;

	if (maxVars < nVars) {
	    nVars = maxVars;
	}

	varList = new int[nVars];
	for (int i = 0; i < nVars; i++) {
	    varList[i] = i;
	}

	makeNGons();
    }

    public void subspaceChanged(SubspaceEvent e) {
	varList = e.getSubspace();
	findSpikeLengths(varList);

    }

    private void initStarPlots(int numPlots) {
	obsList = new int[dataSet.getNumObservations()];
	nGons = new NGon[obsList.length];

	nGons = new NGon[obsList.length];
	for (int i = 0; i < nGons.length; i++) {
	    obsList[i] = i;
	    nGons[i] = new NGon(6);
	}
	obsHashMap.clear();
	for (int i = 0; i < nGons.length; i++) {
	    obsHashMap.put(new Integer(i), nGons[i]);

	}

    }

    private void makeNGons() {

	applyStarFillColors(starColors);
	findSpikeLengths(varList);
	indication = -1; // clear indication

    }

    public void findSpikeLengths(int[] vars) {
	// int nVars = vars.length;
	double[] varData = dataSet.getNumericDataAsDouble(vars[0]);
	double minVal = DescriptiveStatistics.min(varData);
	// double maxVals = DescriptiveStatistics.max(varData);
	double ranges = DescriptiveStatistics.range(varData);

	double range = 0d;
	double prop = 0d;
	for (int row = 0; row < obsList.length; row++) {
	    int index = obsList[row];
	    double val = varData[index];

	    // make range zero-based
	    range = range - minVal;
	    val = val - minVal;
	    prop = val / ranges;
	    int size = (int) (prop * 100d);

	    nGons[row].setSize(size);

	}

    }

    public void renderNGons(Graphics2D g2) {

	if (nGons == null || nGons[0] == null) {
	    return;
	}

	g2.setStroke(new BasicStroke(penWidth));

	for (NGon element : nGons) {
	    element.draw(g2);

	}

	NGon sp = (NGon) obsHashMap.get(new Integer(indication));
	if (sp != null) {
	    // XXX g2.setColor(NGon.defaultIndicationColor);
	    sp.draw(g2);
	}

    }

    public void renderNGon(Graphics2D g2, int plotNumber) {

	// xxx need to handle negative width
	if (plotNumber < 0 || plotNumber >= nGons.length) {
	    return;
	}
	g2.setStroke(new BasicStroke(penWidth));
	nGons[plotNumber].draw(g2);
    }

    public String[] getVarNames() {
	if (varList == null) {
	    return null;
	}

	String[] varNames = new String[varList.length];
	for (int i = 0; i < varList.length; i++) {
	    varNames[i] = dataSet.getNumericArrayName(varList[i]);
	}

	return varNames;
    }

    public double[] getValues(int obs) {
	if (varList == null) {
	    return null;
	}
	double[] vals = new double[varList.length];

	for (int i = 0; i < varList.length; i++) {
	    vals[i] = dataSet.getNumericValueAsDouble(varList[i], obs);
	}
	return vals;
    }

    public void indicationChanged(IndicationEvent e) {

	setIndication(e.getIndication());

    }

    public Glyph[] findGlyphs() {
	NGon[] newPlots = new NGon[nGons.length];
	for (int i = 0; i < newPlots.length; i++) {
	    newPlots[i] = nGons[i].copy();
	}
	return newPlots;
    }

    public int getIndication() {
	return indication;
    }

    public void setIndication(int newIndication) {
	// the incoming indication is an index into the overall data set
	if (indication == newIndication) {
	    return; // no need to do anything
	}

	NGon spOld = (NGon) obsHashMap.get(new Integer(indication));
	if (indication >= 0 && spOld != null) { // clear old indication, if we
	    // have one
	    spOld.setFillColor(starColors[indication]);
	}
	NGon spNew = (NGon) obsHashMap.get(new Integer(newIndication));

	if (spNew != null) { // paint new indication

	    spNew.setFillColor(NGon.DEFAULT_INDICATION_COLOR);

	    indication = newIndication;
	}

    }

    public String getObservationName(int index) {
	// int orderIndex = this.plotsOrder[index];
	if (dataSet == null) {
	    return "";
	}
	if (dataSet.getObservationNames() == null) {
	    return String.valueOf(index);
	}
	return dataSet.getObservationNames()[index];
    }

    public void setStarFillColors(Color[] starColors) {
	applyStarFillColors(starColors);
    }

    void applyStarFillColors(Color[] starColors) {
	this.starColors = starColors;
	if (nGons == null || starColors != null
		&& nGons.length != starColors.length) {
	    return;
	}
	if (this.starColors == null) {
	    this.starColors = new Color[nGons.length];
	    for (int i = 0; i < nGons.length; i++) {
		this.starColors[i] = Color.DARK_GRAY;
		nGons[i].setFillColor(this.starColors[i]);
	    }
	} else {
	    for (int i = 0; i < nGons.length; i++) {
		nGons[i].setFillColor(this.starColors[i]);
	    }
	}

    }

    public Color getStarFillColor(int ind) {
	return starColors[ind];

    }

    public void setPlotLocations(Rectangle[] plotLocs) {
	for (int i = 0; i < nGons.length; i++) {
	    nGons[i].setTargetArea(plotLocs[i]);
	}
    }

    /*
     * Sets the visible set of starplots. the array of ints is an array of which
     * starplots are to be visible. Null arrays or arrays of length zero are
     * ignored.
     */
    public void setObsList(int[] obsList) { // for selections etc.
	if (obsList == null || obsList.length < 1) {
	    return;
	}
	this.obsList = obsList;
	// no no no, we should set the target area of the relevant nGons
	// this.plotsOrder = obsList;
	// this.plotLocations = new Rectangle[plotsOrder.length];
	// this.makeStarPlots();
	// and update our obsHashMap
	obsHashMap.clear();
	for (int element : obsList) {
	    obsHashMap.put(new Integer(element), nGons[element]);

	}
	indication = -1; // clear indication

    }

    public int[] getObsList() {
	return obsList;
    }

    public DataSetForApps getDataSet() {
	return dataSet;
    }

    public void setNSides(int sides) {
	for (NGon element : nGons) {
	    element.setNSides(sides);
	}

    }

    public NGon[] getNGons() {
	return nGons;
    }

    public void setNGons(NGon[] gons) {
	nGons = gons;
    }
}
