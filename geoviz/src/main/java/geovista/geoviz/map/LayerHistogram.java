/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.map;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import geovista.common.data.DataSetForApps;
import geovista.geoviz.scatterplot.Histogram;
import geovista.symbolization.glyph.Glyph;

/**
 * put your documentation comment here
 */
public class LayerHistogram extends LayerShape {

    private Point2D[] originalPoints;

    ArrayList<Histogram> histos;
    HashMap<Integer, Histogram> obsToHistos;
    DataSetForApps dataSet;

    public LayerHistogram() {
	super();
	// phistos = new ArrayList<Histogram>();
	obsToHistos = new HashMap<Integer, Histogram>();
    }

    @Override
    public void setGlyphs(Glyph[] glyphs) {
    }

    private void makeHistos() {
	histos = new ArrayList<Histogram>();
	HashMap<Shape, Histogram> shapeToHisto;
	shapeToHisto = new HashMap<Shape, Histogram>();
	for (int i = 0; i < dataSet.getNumObservations(); i++) {
	    Shape shp = dataSet.getShapeData()[i];
	    if (shp == null) {
		break;
	    }
	    if (shapeToHisto.containsKey(shp) == false) {
		Histogram histo = new Histogram();
		logger.info("made histo for obs " + i);
		histo.setVariableName(dataSet.getObservationName(i));
		shapeToHisto.put(shp, histo);
		histo.setLocation(shp.getBounds().x, shp.getBounds().y);
		obsToHistos.put(i, histo);
		histos.add(histo);

	    } else {
		obsToHistos.put(i, shapeToHisto.get(shp));
	    }
	}

	HashMap<Histogram, ArrayList<Double>> histosToNums;
	histosToNums = new HashMap<Histogram, ArrayList<Double>>();

	for (Histogram histo : histos) {
	    histosToNums.put(histo, new ArrayList<Double>());
	}

	int numericArrayIndex = 1;
	// now add the data to the histograms
	for (Integer obs : obsToHistos.keySet()) {
	    Double value = dataSet.getNumericValueAsDouble(numericArrayIndex,
		    obs);
	    ArrayList<Double> vals = histosToNums.get(obsToHistos.get(obs));

	    vals.add(value);
	    histosToNums.put(obsToHistos.get(obs), vals);

	}

	for (Histogram histo : histos) {
	    ArrayList<Double> vals = histosToNums.get(histo);
	    if (vals.size() == 0) {
		logger.info("removing histo");
		histos.remove(histo);
		break;
	    }
	    double[] valVals = new double[vals.size()];
	    for (int i = 0; i < vals.size(); i++) {
		valVals[i] = vals.get(i);
	    }
	    logger.info("valVals: " + Arrays.toString(valVals));

	    histo.setData(valVals);
	    // histo.dataSetChanged(new DataSetEvent(dataSet, this));
	}

    }

    /*
     * SelectionX1 is expected to be less than selectionX2, same with Y1 and y2.
     * Selected observations should be rendered with the color "colorSelection".
     */
    @Override
    public void findSelection(int selectionX1, int selectionX2,
	    int selectionY1, int selectionY2) {
	Rectangle selBox = new Rectangle(selectionX1, selectionY1, selectionX2
		- selectionX1, selectionY2 - selectionY1);

	Vector selObs = new Vector();
	for (int i = 0; i < spatialData.length; i++) {
	    Rectangle shpBox = spatialData[i].getBounds();
	    if (selBox.intersects(shpBox)) {
		if (spatialData[i].contains(selBox)
			|| spatialData[i].intersects(selBox)) {
		    selObs.add(new Integer(i));
		} // end if really intersects
	    } // end if rough intersects
	} // next
	selectedObservations = new int[selObs.size()];
	int j = 0;
	for (Enumeration e = selObs.elements(); e.hasMoreElements();) {
	    Integer anInt = (Integer) e.nextElement();
	    selectedObservations[j] = anInt.intValue();
	    j++;
	}
    }

    /*
     * selectionX1 is expected to be less than selectionX2, same with Y1 and y2
     */
    @Override
    public void findSelectionShift(int selectionX1, int selectionX2,
	    int selectionY1, int selectionY2) {
	Rectangle selBox = new Rectangle(selectionX1, selectionY1, selectionX2
		- selectionX1, selectionY2 - selectionY1);

	Vector selObs = new Vector();
	Arrays.sort(selectedObservations); // have to do this for the
	// searching
	for (int i = 0; i < spatialData.length; i++) {
	    Rectangle shpBox = spatialData[i].getBounds();
	    if (selBox.intersects(shpBox)) {
		if (Arrays.binarySearch(selectedObservations, i) < 0) {
		    selObs.add(new Integer(i));
		}
	    }
	}
	int[] selectedObserCp = new int[selectedObservations.length];
	selectedObserCp = (selectedObservations.clone());
	selectedObservations = new int[selectedObserCp.length + selObs.size()];
	int j = 0;
	for (j = 0; j < selectedObserCp.length; j++) {
	    selectedObservations[j] = selectedObserCp[j];
	}
	for (Enumeration e = selObs.elements(); e.hasMoreElements();) {
	    Integer anInt = (Integer) e.nextElement();
	    selectedObservations[j] = anInt.intValue();
	    j++;
	}

    }

    @Override
    public int findIndication(int x, int y) {
	for (int i = 0; i < spatialData.length; i++) {
	    Rectangle shpBox = spatialData[i].getBounds();
	    if (shpBox.contains(x, y)) {
		if (spatialData[i].contains(x, y)) {
		    return i;

		} // end if really intersects
	    } // end if rough intersects
	} // next
	  // couldn't find anything, so
	return Integer.MIN_VALUE;
    }

    /**
     * sets the points that the layer will use to draw from these are assumed to
     * be in user space
     * 
     * @param originalPoints
     */
    public void setOriginalPoints(Point2D[] originalPoints) {
	this.originalPoints = originalPoints;
    }

    @Override
    public void renderSelectedObservations(Graphics2D g2) {
	logger.info("painting histos");
	for (Histogram histo : histos) {
	    logger.info("location = " + histo.getLocation());
	    logger.info(Arrays.toString(histo.getData()));
	    histo.paintComponent(g2);
	}

    }

    /**
     * retrieves the points
     * 
     * @param originalPoints
     */
    public Point2D[] getOriginalPoints() {
	return originalPoints;
    }

    public Point2D[] shapesToPoints(Shape[] shapes) {
	Point2D[] points = new Point2D.Double[shapes.length];
	for (int i = 0; i < shapes.length; i++) {
	    Point2D pnt = new Point2D.Double();
	    Shape shp = shapes[i];
	    PathIterator path = shp.getPathIterator(null);
	    double[] seg = new double[6];
	    int segType = path.currentSegment(seg);
	    if (segType != PathIterator.SEG_MOVETO) {
		throw new IllegalArgumentException(
			"LayerPointExtension.shapesToPoints expects only PathIterator.SEG_MOVETO segments");
	    }
	    pnt.setLocation(seg[0], seg[1]);
	    points[i] = pnt;
	}

	return points;
    }

    public Shape[] pointsToShapes(Point2D[] points) {
	Shape[] shapes = new Shape[points.length];
	for (int i = 0; i < shapes.length; i++) {

	    GeneralPath shp = new GeneralPath();
	    Point2D pnt = points[i];
	    shp.moveTo((float) pnt.getX(), (float) pnt.getY());
	    Shape circle = new Ellipse2D.Float((float) pnt.getX(),
		    (float) pnt.getY(), 10, 10);

	    shapes[i] = shp;
	    shapes[i] = circle;
	}

	return shapes;
    }

    public Shape[] findShapesForPoints(Point2D[] points) {
	Shape[] shapes = new Shape[points.length];
	float shapeSize = 8f;
	float half = shapeSize / 2f;
	for (int i = 0; i < shapes.length; i++) {
	    Point2D pt = points[i];

	    Ellipse2D eli = new Ellipse2D.Float((float) pt.getX() - half,
		    (float) pt.getY() - half, shapeSize, shapeSize);
	    GeneralPath path = new GeneralPath(eli);
	    shapes[i] = path;
	}

	return shapes;
    }

    public void setDataSet(DataSetForApps dataSet2) {
	dataSet = dataSet2;
	makeHistos();

    }

}
