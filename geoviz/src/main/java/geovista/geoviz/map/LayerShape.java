/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.geoviz.map;

// import geovista.common.data.DataSetForApps;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import geovista.common.data.DescriptiveStatistics;
import geovista.common.ui.Fisheyes;
import geovista.symbolization.ColorInterpolator;
import geovista.symbolization.glyph.Glyph;

/**
 * Layer and its subclasses are responsible for rendering spatial data, using
 * classifications and symbolizations set by the user.
 * 
 * The spatial data to be rendered is expected to be in user device space.
 */
public abstract class LayerShape {

	public static final String COMMAND_SELECTION = "cmdSel";
	protected static final int STATUS_NOT_SELECTED = 0; // default
	protected static final int STATUS_SELECTED = 1;

	public enum LayerType {
		POINT, LINE, POLYGON, RASTER, SYMBOL
	}

	public static final int FILL_ORDER_MAX = 3;
	protected transient Shape[] spatialData; // in user space
	protected transient Shape[] originalSpatialData; // originalCoordinates
	protected transient Rectangle[] boundingBoxes;
	protected transient int indication;
	protected transient int[] classification;
	protected transient int[] focus;
	protected transient int[] selectedObservations;
	protected transient int[] selectedObservationsFullIndex;
	protected transient int[] selectedObservationsOld;
	protected transient int[] selectedObservationsOldFullIndex;
	protected transient Color[] objectColors;
	protected transient String[] variableNames;
	protected transient BufferedImage buff;
	protected transient int currOrderColumn;
	protected transient int currColorColumn;
	protected transient int[] conditionArray;
	protected transient TexturePaint[] textures;
	protected transient Glyph[] glyphs;

	// Colors
	// protected Color colorSelection = new
	// Color(Color.blue.getRed(),Color.blue.getGreen(),Color.blue.getBlue(),128);
	protected transient Color colorSelection = Color.blue;
	protected transient Color colorIndication = new Color(Color.red.getRed(),
			Color.red.getGreen(), Color.red.getBlue(), 150);

	// protected Color colorIndication = Color.green;
	protected transient Color colorNull = Color.darkGray;
	protected transient Color colorOutOfFocus = Color.black;
	protected transient Color colorNotInStudyArea = Color.black;
	protected transient Color colorLine = Color.gray;
	// protected transient Color colorAuxLine = new Color(232, 232, 191); //
	// kind
	protected transient Color colorAuxLine = Color.red; // kind
	protected transient Color colorBlur = new Color(128, 128, 128, 0); // of
	// tan
	protected transient Color colorBackground = Color.white;
	protected transient float defaultStrokeWidth;
	protected transient Stroke defaultStroke;
	protected transient Stroke selectionStroke;
	protected transient Stroke deselectionStroke;
	protected transient TexturePaint defaultTexture;
	protected transient TexturePaint indicationTexture;
	protected boolean isAuxiliary = false;
	protected transient float[] spatialDataArea; // area of spatial data in
	// pixels
	transient Fisheyes fisheyes;
	transient boolean colorsRecieved = false;
	transient boolean selectionExists = false;
	// selections show colors, unselected are blank, unless none are selected,
	// in which case show all
	transient boolean fillAux = false;
	final static Logger logger = Logger.getLogger(LayerShape.class.getName());

	public LayerShape() {
		indication = Integer.MIN_VALUE;
		selectedObservations = new int[0];

		defaultStroke = new BasicStroke(1f);
		selectionStroke = new BasicStroke(2f);
		deselectionStroke = new BasicStroke(2f);
		selectedObservationsOld = new int[0];
		makeTextures();
	}

	private void makeTextures() {
		int texSize = 4;

		Rectangle2D.Float indRect = new Rectangle2D.Float(0, 0, texSize,
				texSize);
		BufferedImage indBuff = new BufferedImage(texSize, texSize,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D indG2 = indBuff.createGraphics();
		Color clearBlack = new Color(0, 0, 0, 0);
		indG2.setColor(clearBlack);
		indG2.fill(indRect);
		indG2.setColor(colorIndication);
		indG2.drawLine(0, texSize, texSize, 0);
		indicationTexture = new TexturePaint(indBuff, indRect);

	}

	private void initColors() {
		objectColors = new Color[spatialData.length];

		for (int i = 0; i < objectColors.length; i++) {
			objectColors[i] = Color.yellow;
		}

		conditionArray = new int[spatialData.length];
	}

	public Point findCentroid(int obs) {
		if (spatialData == null || spatialData[obs] == null) {
			return null;
		}
		Shape s = spatialData[obs];

		// here's an inaccurate but fast algorithm. Todo: replace with something
		// more accurate but also fast.
		Rectangle rect = s.getBounds();
		int x = (int) (rect.getX() + rect.getWidth() / 2d);
		int y = (int) (rect.getY() + rect.getHeight() / 2d);
		Point p = new Point(x, y);

		return p;
	}

	// begin accessors
	// public void setExtent(Rectangle extent) {
	// this.extent = extent;
	// }

	// MapCanvas calls this on resize and data being set
	public void setSpatialData(Shape[] spatialData) {
		/*
		 * //special stuff for Chaomei if (this.isAuxiliary) { //String[]
		 * observationNames = dataSet.getObservationNames(); int[] numPoints =
		 * new int[25]; int x = 0; int y = 0; int prevX = 0; int prevY = 0; int
		 * whichShape = 0; int numSkipped = 0;
		 * logger.finest(spatialData.length); for (int i = 0; i <
		 * spatialData.length; i++) { logger.finest(i); //special thing: if
		 * points are identical to previous points, we skip 'em PathIterator pi
		 * = spatialData[i].getPathIterator(new AffineTransform()); int numPolys
		 * = 0; while (!pi.isDone()) { float[] coords = new float[6]; int
		 * segType = pi.currentSegment(coords); x = (int)coords[0]; y =
		 * (int)coords[1]; if (segType == PathIterator.SEG_MOVETO) { x =
		 * Integer.MAX_VALUE; y = Integer.MAX_VALUE; numPolys++;
		 * numPoints[numPolys] = 0; } if (x == prevX && y == prevY) {
		 * numSkipped++; } else { numPoints[numPolys]++; prevX = x; prevY = y; }
		 * pi.next(); }//wend pi = spatialData[i].getPathIterator(new
		 * AffineTransform()); logger.finest(observationNames[i]);
		 * logger.finest(numPolys); numPolys = 0; while (!pi.isDone()) { float[]
		 * coords = new float[6]; int segType = pi.currentSegment(coords); x =
		 * (int)coords[0]; y = (int)coords[1]; if (segType ==
		 * PathIterator.SEG_MOVETO) { x = Integer.MAX_VALUE; y =
		 * Integer.MAX_VALUE; numPolys++; logger.finest(numPoints[numPolys]); }
		 * if (x == prevX && y == prevY) { numSkipped++; } else {
		 * logger.finest(coords[0] +","+ coords[1]); prevX = x; prevY = y; }
		 * pi.next(); }//wend } //next spatialData logger.finest("num skipped "
		 * + numSkipped); } //end if aux //end special stuff for chaomei
		 */
		this.spatialData = spatialData;

		// we need to check for null if this is the first time through
		// we need to check for length in case the spatial data
		// has changed.
		// however the spatial data might well change with no other changes
		// needed, in case of panning or zooming, for instance.
		int numObs = spatialData.length;

		if ((classification == null) || (classification.length != numObs)) {
			classification = new int[spatialData.length];
		}

		if ((objectColors == null) || (objectColors.length != numObs)) {
			initColors();
		}

		if ((selectedObservationsFullIndex == null)
				|| (selectedObservationsFullIndex.length != numObs)) {
			selectedObservationsFullIndex = new int[spatialData.length];
		}

		if ((selectedObservationsOldFullIndex == null)
				|| (selectedObservationsOldFullIndex.length != numObs)) {
			selectedObservationsOldFullIndex = new int[spatialData.length];
		}

		if ((conditionArray == null) || (conditionArray.length != numObs)) {
			conditionArray = new int[numObs];
		}

		if ((spatialDataArea == null) || (spatialDataArea.length != numObs)) {
			spatialDataArea = new float[numObs];
		}
	}

	public Shape[] getSpatialData() {
		return spatialData;
	}

	public Color[] getColors() {
		return objectColors;
	}

	public void setIndication(int indication) {
		this.indication = indication;
	}

	public void setGlyphs(Glyph[] glyphs) {
		this.glyphs = glyphs;
		locateGlyphs();
	}

	private void locateGlyphs() {
		if (glyphs == null) {
			return;
		}
		for (int i = 0; i < glyphs.length; i++) {
			Glyph gly = glyphs[i];
			gly.setLocation(findCentroid(i));
		}
	}

	public void setTextures(TexturePaint[] textures) {
		this.textures = textures;

	}

	// just sets data
	public void setSelectedObservations(int[] selectedObservations) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest(Arrays.toString(selectedObservations));
		}
		// check for error condition
		int maxVal = DescriptiveStatistics.max(selectedObservations);
		if (maxVal >= spatialData.length) {
			logger.severe("selection index too long, max value = " + maxVal);
			return;
		}
		// copy old full index values
		for (int i = 0; i < selectedObservationsFullIndex.length; i++) {
			selectedObservationsOldFullIndex[i] = selectedObservationsFullIndex[i];
		}

		// reset full index array
		for (int i = 0; i < selectedObservationsFullIndex.length; i++) {
			selectedObservationsFullIndex[i] = STATUS_NOT_SELECTED;
		}

		// set correct selection values in full index
		for (int obs : selectedObservations) {
			selectedObservationsFullIndex[obs] = STATUS_SELECTED;
		}
		// set selectionExists for rendering
		if (selectedObservations.length > 0) {
			selectionExists = true;
		} else {
			selectionExists = false;
		}

		this.selectedObservations = selectedObservations; // this happens
		// anyway

		// copy current selection values to the old one.
		if (selectedObservationsOld.length != selectedObservations.length) {
			selectedObservationsOld = new int[selectedObservations.length];
		}

		for (int i = 0; i < selectedObservations.length; i++) {
			selectedObservationsOld[i] = selectedObservations[i];

		}
	}

	public int[] getSelectedObservations() {
		return selectedObservations;
	}

	public void setIsAuxiliary(boolean isAuxiliary) {
		this.isAuxiliary = isAuxiliary;

		int red = Color.darkGray.getRed();
		int green = Color.darkGray.getGreen();
		int blue = Color.darkGray.getBlue();
		int alpha = 200;
		Color transGray = new Color(red, green, blue, alpha);
		colorLine = transGray;
		defaultStroke = new BasicStroke(2f);
	}

	public void setParentSize(int height, int width) {
		findStrokeSize(height, width);

		locateGlyphs();
	}

	private void findStrokeSize(int height, int width) {
		if (spatialData == null) {
			return;
		}
		int shapeCount = 0;
		Rectangle currBox = new Rectangle(0, 0, width, height);

		for (Shape element : spatialData) {
			if (currBox.intersects(element.getBounds())) {
				shapeCount++;
			}
		}

		if (shapeCount <= 0) {
			return;
		}

		spatialDataArea = new float[spatialData.length];

		int counter = 0;

		for (Shape element : spatialData) {
			if (currBox.intersects(element.getBounds2D())) {
				float area = (float) element.getBounds().getWidth()
						* (float) element.getBounds().getHeight();
				spatialDataArea[counter] = area;
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("area = " + area);
				}
				counter++;
			}
		}

		Arrays.sort(spatialDataArea);

		int firstForth = spatialDataArea.length / 2;
		float shapeArea = spatialDataArea[firstForth];
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("LayerShape.setParent, shapeCount = " + shapeCount);
			logger.finest(",shapeArea = " + shapeArea);
		}
		float strokeWidth = 0f;

		if (shapeArea < 40) {
			strokeWidth = 0f;
		} else if (shapeArea < 2500) {
			strokeWidth = 0.1f;
		} else if (shapeArea < 5000) {
			strokeWidth = 1f;
		} else if (shapeArea < 500000) {
			strokeWidth = 2f;
		} else {
			strokeWidth = 3f;
		}
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("strokeWidth = " + strokeWidth);

		}
		defaultStroke = new BasicStroke(strokeWidth);

		defaultStrokeWidth = strokeWidth;
		if (strokeWidth < 1) { // start with at least one for finding
			// selectionStroke
			strokeWidth = 1;
		}
		selectionStroke = new BasicStroke(strokeWidth * 1.5f,
				BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}

	public void setOriginalSpatialData(Shape[] spatialData) {
		originalSpatialData = spatialData;
	}

	public Shape[] getOriginalSpatialData() {
		return originalSpatialData;
	}

	public boolean getIsAuxiliary() {
		return isAuxiliary;
	}

	public void setObjectColors(Color[] objectColors) {
		this.objectColors = objectColors;
		colorsRecieved = true;
	}

	public void setColorSelection(Color colorSelection) {
		this.colorSelection = colorSelection;
		makeTextures();
	}

	void setSelectionLineWidth(int lineWidth) {
		selectionStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND);
	}

	public void setColorIndication(Color colorIndication) {
		this.colorIndication = colorIndication;
		makeTextures();
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param conditionArray
	 */
	public void setConditionArray(int[] conditionArray) {
		this.conditionArray = conditionArray;
	}

	// end accessors
	// Abstract methods that must be implemented by a subclass
	public abstract void findSelection(int x1, int x2, int y1, int y2);

	public abstract void findSelectionShift(int x1, int x2, int y1, int y2);

	public abstract int findIndication(int x, int y);

	/*
	 * SelectionX1 is expected to be less than selectionX2, same with Y1 and y2.
	 * Selected observations should be rendered with the color "colorSelection".
	 */
	public int[] findSelection(Rectangle2D selBox) {

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
		int[] selObsInt = new int[selObs.size()];
		int j = 0;
		for (Enumeration e = selObs.elements(); e.hasMoreElements();) {
			Integer anInt = (Integer) e.nextElement();
			selObsInt[j] = anInt.intValue();
			j++;
		}
		return selObsInt;
	}

	@Override
	public String toString() {
		String s = this.getClass().toString();

		if (spatialData == null) {
			s = s + ", spatialData == null.";
		} else {
			s = s + ", spatialData.length = " + spatialData.length + ".";
		}

		return s;
	}

	void renderBackground(Graphics2D g2) {
		if (objectColors == null) {
			return;
		}
		for (int path = 0; path < spatialData.length; path++) {
			renderBackgroundObservation(path, g2);
		}
	}

	private void renderBackgroundObservation(int obs, Graphics2D g2) {
		if (obs < 0) {
			return;
		}
		if (objectColors == null || objectColors.length <= obs) {
			return;
		}
		Shape shp = spatialData[obs];

		Color color = objectColors[obs];
		if (color == null) {
			g2.setColor(colorBlur);
		} else if (colorBlur.getAlpha() > 0) {
			Color newColor = ColorInterpolator.mixColorsRGB(colorBlur, color);
			g2.setColor(newColor);
		} else {
			g2.setColor(color);
		}
		g2.fill(shp);
		if (defaultStrokeWidth >= 0.1f) {
			g2.setColor(colorLine);
			g2.draw(shp);
		}

	}

	private void renderSelectedObservationOutline(int obs, Graphics2D g2) {
		if (obs < 0) {
			return;
		}
		if (objectColors == null || objectColors.length <= obs) {
			return;
		}
		Shape shp = spatialData[obs];

		if (fisheyes != null) {
			shp = fisheyes.transform(shp);

		}
		Color color = objectColors[obs];
		if (obs == indication) {
			renderIndication(g2, shp, color);

		}
		if (conditionArray[obs] > -1) {
			g2.setStroke(selectionStroke);
			if (selectedObservationsFullIndex[obs] == STATUS_SELECTED
					|| !selectionExists) {

				g2.setColor(colorSelection);

				g2.draw(shp);

			}

		} // end if condition
	}

	private void renderSelectedObservationFill(int obs, Graphics2D g2) {
		if (obs < 0) {
			return;
		}
		if (objectColors == null || objectColors.length <= obs) {
			return;
		}
		Shape shp = spatialData[obs];

		if (fisheyes != null) {
			shp = fisheyes.transform(shp);

		}
		Color color = objectColors[obs];
		if (obs == indication) {
			renderIndication(g2, shp, color);

		}
		if (conditionArray[obs] > -1) {
			g2.setStroke(defaultStroke);
			if (selectedObservationsFullIndex[obs] == STATUS_SELECTED
					|| !selectionExists) {

				g2.setColor(color);
				if (textures != null && textures[obs] != null) {
					g2.setPaint(textures[obs]);
				}
				g2.fill(shp);

			}

			if (defaultStrokeWidth >= 0.1f) {
				g2.setColor(colorLine);
				g2.draw(shp);
			}
		} // end if condition

	}

	public void renderSelectedObservationGlyph(int obs, Graphics2D g2) {
		if (obs < 0) {
			return;
		}
		if (conditionArray[obs] > -1) {
			if (selectedObservationsFullIndex[obs] == STATUS_SELECTED
					|| !selectionExists) {

				renderGlyph(obs, g2);
			}

		} // end if condition

	}

	public void renderObservation(int obs, Graphics2D g2) {
		if (obs < 0) {
			return;
		}
		if (objectColors == null || objectColors.length <= obs) {
			return;
		}
		Shape shp = spatialData[obs];

		if (fisheyes != null) {
			shp = fisheyes.transform(shp);

		}
		Color color = objectColors[obs];
		if (obs == indication) {
			renderIndication(g2, shp, color);

		}
		if (conditionArray[obs] > -1) {
			g2.setStroke(defaultStroke);
			if (defaultStrokeWidth >= 0.1f) {
				g2.setColor(colorLine);
				g2.draw(shp);
			}
			if (selectedObservationsFullIndex[obs] == STATUS_SELECTED
					|| !selectionExists) {

				g2.setColor(color);
				if (textures != null && textures[obs] != null) {
					g2.setPaint(textures[obs]);
				}
				g2.fill(shp);
				renderGlyph(obs, g2);
			}

		} // end if condition
		// glyphs go on top
		if (obs == indication) {
			renderGlyph(obs, g2);
		}

	}

	void renderSecondaryIndication(Graphics2D g2, int obs) {
		if (obs < 0) {
			return;
		}
		if (objectColors == null || objectColors.length <= obs) {
			return;
		}
		Shape shp = spatialData[obs];
		Color color = objectColors[obs];

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		Stroke tempStroke = g2.getStroke();
		BasicStroke secondStroke = new BasicStroke(6f, BasicStroke.CAP_ROUND,
				BasicStroke.CAP_ROUND);
		g2.setStroke(secondStroke);

		g2.setColor(new Color(255, 255, 0, 128));
		g2.draw(shp);

		g2.setColor(color);
		g2.fill(shp);

		g2.setStroke(tempStroke);
	}

	private final boolean simpleHighlighting = false;
	private boolean selOutline;

	private void renderIndication(Graphics2D g2, Shape shp, Color color) {
		Stroke tempStroke = g2.getStroke();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		BasicStroke secondStroke = new BasicStroke(6f, BasicStroke.CAP_ROUND,
				BasicStroke.CAP_ROUND);

		BasicStroke underStroke = new BasicStroke(50f, BasicStroke.CAP_ROUND,
				BasicStroke.CAP_ROUND);

		if (simpleHighlighting) {
			g2.setStroke(secondStroke);
			g2.setColor(colorSelection);
			g2.draw(shp);
			return;
		}

		g2.setColor(new Color(128, 128, 128, 128));
		g2.setStroke(underStroke);
		g2.draw(shp);

		g2.setStroke(secondStroke);
		g2.setColor(Color.black);
		g2.draw(shp);

		g2.setColor(color);
		g2.fill(shp);
		g2.setStroke(tempStroke);

	}

	public void renderSelectedObservations(Graphics2D g2) {

		if (objectColors == null) {
			logger.finest("LayerShape, render called on null objectColors");
			return;
		}

		if (g2 == null) {
			throw new IllegalArgumentException(toString()
					+ " Null graphics passed in to render(Graphics2D).");
		}

		if (isAuxiliary) {
			// XXX this happens too often, that is, more than once upon load.
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("rendering auxiliary layer....shape. ");
			}
			try {
				renderAux(g2);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return;
		}
		// skip indication
		int tempInd = indication;
		indication = -1;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		if (selOutline) {
			logger.info("render outlines");
			for (int path = 0; path < spatialData.length; path++) {

				renderSelectedObservationOutline(path, g2);

			}
		} else {

			for (int path = 0; path < spatialData.length; path++) {
				renderSelectedObservationFill(path, g2);

			}
		}
		if (glyphs != null) {
			for (int path = 0; path < spatialData.length; path++) {
				renderSelectedObservationGlyph(path, g2);

			}
		}
		// this.renderGlyphs(g2);
		indication = tempInd;

	} // end method

	private void renderGlyph(int obs, Graphics2D g2) {
		if (spatialData == null) {
			return;
		}
		Glyph glyph = null;
		if (glyphs != null) {
			if (glyphs[obs] != null) {
				glyph = glyphs[obs];
				Color col = glyph.getFillColor();
				// glyph.setFillColor(this.colorIndication);
				glyph.draw(g2);
				glyph.setFillColor(col);

			}
		}
	}

	@SuppressWarnings("unused")
	private void renderGlyphs(Graphics2D g2) {
		if (spatialData == null) {
			return;
		}
		Glyph glyph = null;
		if (glyphs != null) {
			for (Glyph element : glyphs) {
				if (element != null) {
					glyph = element;
					glyph.draw(g2);
				}
			}
		}

	}

	protected void renderAux(Graphics2D g2) {
		// draw all shapes

		RenderingHints qualityHints = new RenderingHints(null);

		qualityHints.put(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		qualityHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		g2.setRenderingHints(qualityHints);
		// if (this.defaultStrokeWidth >= 0.1f) {
		g2.setColor(colorAuxLine);
		g2.setStroke(selectionStroke);

		if (spatialData != null) {
			int numPaths = spatialData.length;

			for (int path = 0; path < numPaths; path++) {
				Shape s = spatialData[path];
				if (fisheyes != null) {
					s = fisheyes.transform(s);
				}

				if (fillAux) {
					g2.fill(s);
				} else {
					g2.draw(s);
				}
			} // end for path

		} // end if null
	}

	public Color getColorBackground() {
		return colorBackground;
	}

	public void setColorBackground(Color colorBackground) {
		this.colorBackground = colorBackground;
		int rgb = colorBackground.getRed() + colorBackground.getGreen()
				+ colorBackground.getBlue();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("rgb " + rgb);
		}
		if (rgb > 300) {
			// its close to white, so
			colorLine = Color.lightGray;
		} else {
			colorLine = Color.darkGray;
		}
		makeTextures();
	}

	public void setFillAux(boolean fillAux) {
		this.fillAux = fillAux;
	}

	public void useSelectionOutline(boolean selOutline) {
		this.selOutline = selOutline;

	}

}
