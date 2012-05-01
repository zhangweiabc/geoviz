/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.map;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import geovista.symbolization.glyph.Glyph;

/**
 * put your documentation comment here
 */
public class LayerPoint extends LayerShape {

    private Point2D[] originalPoints;

    @Override
    public void setGlyphs(Glyph[] glyphs) {
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

}
