/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.geoviz.map;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

/**
 * put your documentation comment here
 */
public class LayerPolygon extends LayerShape {

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
	    if (spatialData[i] == null) {
		break;
	    }
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
	Arrays.sort(selectedObservations); // have to do this for the searching
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
	if (spatialData == null) {
	    return Integer.MIN_VALUE;
	}
	for (int i = 0; i < spatialData.length; i++) {
	    if (spatialData[i] == null) {
		break;
	    }
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

}
