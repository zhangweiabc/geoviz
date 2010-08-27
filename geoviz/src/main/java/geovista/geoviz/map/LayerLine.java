/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.geoviz.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import geovista.symbolization.glyph.Glyph;

/**
 * put your documentation comment here
 */
public class LayerLine extends LayerShape {

	@Override
	public void setGlyphs(Glyph[] glyphs) {
		this.glyphs = glyphs;
	}

	/**
	 * we need to override superclass because we should not fill
	 * 
	 * @param g2
	 */
	@Override
	public void renderSelectedObservations(Graphics2D g2) {
		if (objectColors == null) {
			logger.finest("LayerLine, render called on null objectColors");
			return;
		}
		if (g2 == null) {
			throw new IllegalArgumentException(toString()
					+ " Null graphics passed in to render(Graphics2D).");
		}

		if (isAuxiliary) {
			logger.finest("rendering auxiliary layer....line. ");

			super.renderAux(g2);

			return;
		}

		for (int path = 0; path < spatialData.length; path++) {
			renderObservationNoIndication(path, g2);
		}
	} // end method

	/**
	 * we need to override superclass because we should not fill
	 * 
	 * @param g2
	 */
	@Override
	public void renderObservation(int obs, Graphics2D g2) {
		if (obs < 0) {
			return;
		}
		if (objectColors.length <= obs) {
			return;
		}

		Shape shp = spatialData[obs];
		if (fisheyes != null) {
			shp = fisheyes.transform(shp);
		}

		g2.setStroke(defaultStroke);
		Color color = objectColors[obs];
		if (indication == obs) {
			color = Color.red;
		}
		g2.setColor(color);
		g2.draw(shp);

		if (defaultStrokeWidth >= 0.1f) {
			g2.setColor(colorLine);
			g2.draw(shp);
		}

	}

	/**
	 * we need to override superclass because we should not fill
	 * 
	 * @param g2
	 */
	public void renderObservationNoIndication(int obs, Graphics2D g2) {
		if (obs < 0) {
			return;
		}
		if (objectColors.length <= obs) {
			return;
		}
		Shape shp = spatialData[obs];
		if (fisheyes != null) {
			shp = fisheyes.transform(shp);
		}

		if (conditionArray[obs] > -1) {
			g2.setStroke(defaultStroke);
			if (selectedObservationsFullIndex[obs] == STATUS_SELECTED
					|| !selectionExists) {
				Color color = objectColors[obs];
				g2.setColor(color);
				g2.draw(shp);
			}

			if (defaultStrokeWidth >= 0.1f) {
				g2.setColor(colorLine);
				g2.draw(shp);
			}

			if (selectedObservationsFullIndex[obs] == STATUS_NOT_SELECTED
					&& selectionExists) {
				g2.setPaint(defaultTexture);
				g2.draw(shp);
			}

		}

	}

	/**
	 * SelectionX1 is expected to be less than selectionX2, same with Y1 and y2.
	 * Selected observations should be rendered with the color "colorSelection".
	 * 
	 * selectionX1 is expected to be less than selectionX2, same with Y1 and y2
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
}
