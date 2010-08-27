/*
 * BasicParallelDisplayUI.java
 *
 * Created on 19. November 2001, 16:06
 *
 * Copyright 2001 Flo Ledermann flo@subnet.at
 *
 * Licensed under GNU General Public License (GPL).
 * See http://www.gnu.org/copyleft/gpl.html
 */

package geovista.geoviz.parvis;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.plaf.ComponentUI;

/**
 * The UI Delegate, responsible for rendering the ParallelDisplay component.
 * 
 * @author Flo Ledermann flo@subnet.at
 * 
 */
public class BasicParallelDisplayUI extends ParallelDisplayUI implements
		MouseListener, MouseMotionListener {

	protected final static Logger logger = Logger
			.getLogger(BasicParallelDisplayUI.class.getName());

	static JPanel observer = new JPanel();

	volatile boolean useSelectionFade = true;

	// GeneralPath[] rPaths;
	int numDimensions;
	int numRecords;

	Color[] colors;

	int[] conditioning;

	int stepx;

	int hoverAxis = -1;
	int hoverRecord = -1;

	float axisScale[];

	int borderH = 20;
	int borderV = 40;

	int width = 0, height = 0;

	String metaText = null;
	int metaX = 0, metaY = 0;

	boolean dragAxis = false;
	int dragX = 0;

	BufferedImage bufferImg = null;
	volatile BufferedImage brushImg = null;

	boolean needsDeepRepaint = true;

	boolean renderQuality = false;

	/** Begin of area that has to be repainted. */
	// int repaintStartAxis = 0;
	/** End of area that has to be repainted. */
	// int repaintStopAxis = 0;
	int brushHoverStart = 0;
	int brushHoverEnd = 0;
	int brushHoverX = 0;
	boolean inBrush = false;

	RenderThread renderThread = null;
	RenderThread brushThread = null;

	Color indicationColor = Color.YELLOW;
	Color secondaryIndicationColor = Color.GREEN;

	// ParallelDisplay comp;

	/**
	 * Default Constructor. Creates a new BasicParallelDisplayUI.
	 */
	public BasicParallelDisplayUI() {
	}

	/**
	 * Swing method. Returns a new instance.
	 */
	public static ComponentUI createUI(JComponent c) {
		return new BasicParallelDisplayUI();
	}

	/**
	 * Installs this instance as UI delegate for the given component.
	 * 
	 * @param c
	 *            The component, a ParallelDisplay in our case.
	 */
	@Override
	public void installUI(JComponent c) {
		ParallelDisplay pd = (ParallelDisplay) c;

		pd.addMouseListener(this);
		pd.addMouseMotionListener(this);

	}

	/**
	 * Uninstalls this instance from its component.
	 * 
	 * @param c
	 *            The component, a ParallelDisplay in our case.
	 */
	@Override
	public void uninstallUI(JComponent c) {
		ParallelDisplay pd = (ParallelDisplay) c;

		pd.removeMouseListener(this);
		pd.removeMouseMotionListener(this);

		numDimensions = 0;
		numRecords = 0;
	}

	/**
	 * Renders the component on the screen.
	 * 
	 * @param g
	 *            The graphics object to draw on.
	 * @param c
	 *            The Component, our ParallelDisplay.
	 */
	@Override
	public void paint(Graphics g, JComponent c) {

		// start our renderThread
		if (renderThread == null) {
			renderThread = new RenderThread(this);
			renderThread.setQuality(false, false);// is a blur

			renderThread.setStyle(new BasicStroke(.5f), new Color(0.0f, 0.0f,
					0.0f, 0.7f));
			renderThread.start();
		}

		if (brushThread == null) {
			brushThread = new RenderThread(this);
			brushThread.setQuality(false, true);
			brushThread.setStyle(new BasicStroke(1.0f), new Color(0.0f, 0.0f,
					0.0f, 0.8f));
			brushThread.setBrushThread(true);
			brushThread.start();
		}

		// set up the environment
		Graphics2D g2 = (Graphics2D) g;
		ParallelDisplay comp = (ParallelDisplay) c;

		RenderingHints qualityHints = new RenderingHints(null);

		qualityHints.put(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		qualityHints.put(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

		g2.setRenderingHints(qualityHints);

		// workaround flag for model change, resize,...
		if (comp.deepRepaint) {
			// throw away buffered image -> complete repaint

			width = c.getWidth() - 2 * borderH;
			height = c.getHeight() - 2 * borderV;

			numDimensions = comp.getNumAxes();
			numRecords = comp.getNumRecords();

			if (conditioning == null) {
				conditioning = new int[numRecords];
			}

			// if (this.rPaths == null || this.rPaths.length != numRecords){
			// this.rPaths = new GeneralPath[numRecords];
			// }
			// this.assembleAllPaths(comp);

			stepx = width / (numDimensions - 1);

			needsDeepRepaint = true;

			bufferImg = new BufferedImage(c.getWidth(), c.getHeight(),
					BufferedImage.TYPE_INT_ARGB);
			// bufferImg = new BufferedImage(c.getWidth(), c.getHeight(),
			// BufferedImage.TYPE_3BYTE_BGR);

			Graphics2D ig = bufferImg.createGraphics();
			ig.setColor(c.getBackground());
			ig.fillRect(0, 0, c.getWidth(), c.getHeight());

			renderThread.reset();
			brushThread.reset();
			renderThread.setCurrentComponent(comp);
			brushThread.setCurrentComponent(comp);

			if (comp.getBrushedCount() == 0) {
				brushImg = null;
			}
			renderAll();

			comp.deepRepaint = false;
		}

		g2.setColor(c.getBackground());
		g2.fillRect(0, 0, comp.getWidth(), comp.getHeight());

		g2.translate(borderH, borderV);

		// save rendered image in new buffer
		if (renderThread.getRenderedImage() != null) {
			// we cant do this becase the renderedImage is only a part of the
			// whole
			// bufferImg = (BufferedImage)renderThread.getRenderedImage();
			Graphics2D ig = bufferImg.createGraphics();
			ig.setColor(comp.getBackground());
			int startAxis = renderThread.getRenderedRegionStart();
			int stopAxis = renderThread.getRenderedRegionStop();

			// delete area that has been rendered
			ig.fillRect(startAxis * stepx, 0, (stopAxis - startAxis) * stepx,
					comp.getHeight());
			// and paint it new
			ig.drawImage(renderThread.getRenderedImage(), 0, 0, comp);
		}

		// if (brushThread.getRenderedImage() != null) {
		// brushThread.useSelectionBlur = false;
		// // we cant do this becase the renderedImage is only a part of the
		// // whole
		// // bufferImg = (BufferedImage)renderThread.getRenderedImage();
		// Graphics2D ig = bufferImg.createGraphics();
		// // ig.setColor(comp.getBackground());
		// int startAxis = brushThread.getRenderedRegionStart();
		// int stopAxis = brushThread.getRenderedRegionStop();
		//		
		// // delete area that has been rendered
		// ig.fillRect(startAxis * stepx, 0, (stopAxis - startAxis) * stepx,
		// comp.getHeight());
		// // and paint it new
		// ig.drawImage(brushThread.getRenderedImage(), 0, 0, comp);
		// }

		if (brushThread.getRenderedImage() != null) {
			brushImg = brushThread.getRenderedImage();
		}

		if (brushImg == null) {
			synchronized (bufferImg) {
				logger.finest("bufferImg null");
				g2.drawImage(bufferImg, 0, 0, comp);

			}
		} else {
			logger.finest("bufferImg not null");

			Composite oldcomp = g2.getComposite();
			if (useSelectionFade) {
				AlphaComposite ac = AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER, 0.5f);// XXX fade

				// previous line changed by FAH 29 july 02 from .2 to .5
				g2.setComposite(ac);
			}
			g2.drawImage(bufferImg, 0, 0, comp);

			g2.setComposite(oldcomp);
			g2.drawImage(brushImg, 0, 0, comp);
		}

		// set up
		g2.setColor(comp.getForeground());
		g2.setStroke(new BasicStroke(1.0f));

		// draw all the dynamic parts on the screen:

		// axis labels
		for (int i = 0; i < numDimensions; i++) {
			float curx = i * stepx;

			// hovering over Axis
			if (i == hoverAxis) {
				g2.setStroke(new BasicStroke(2.5f));
				g2.draw(new Line2D.Float(curx, 0, curx, height));
				g2.setStroke(new BasicStroke(1.0f));
			} else {
				g2.draw(new Line2D.Float(curx, 0, curx, height));
			}

			String label = comp.getAxisLabel(i);
			if (label != null) {
				g2.drawString(label, curx - 10, height + 30);
			}

			g2.drawString("" + comp.getAxisOffset(i), curx + 2,
					borderV / 2 - 22);
			g2.drawString("" + (comp.getAxisOffset(i) + comp.getAxisScale(i)),
					curx + 2, height + borderV / 2 - 5);

			drawArrow(g2, (int) curx, -20, 8, false, (comp.getAxisScale(i) < 0));
		}

		// brush Hover
		if (inBrush) {
			g2.setColor(new Color(0.7f, 0.0f, 0.0f));
			// g2.setColor(Color.blue);
			g2.setStroke(new BasicStroke(2.5f));
			g2.draw(new Line2D.Float(brushHoverX, brushHoverStart, brushHoverX,
					brushHoverEnd));
		}

		// hovering over record
		// added Frank Hardisty 19 July 2002
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("about to paint neighbors, n = "
					+ comp.indicationNeighbors.length);
		}
		for (int obs : comp.indicationNeighbors) {
			GeneralPath rPath = assemblePath(obs, 0, numDimensions - 1, comp);

			g2.setColor(colors[obs]);
			g2.setStroke(new BasicStroke(3.0f));
			g2.draw(rPath);

			g2.setStroke(new BasicStroke(.5f));
			g2.setColor(secondaryIndicationColor);
			g2.draw(rPath);
		}
		boolean paintHoverNative = false;
		boolean paintHoverComp = false;
		int paintHoverRecord = -1;
		if (comp.indication > 0) {
			paintHoverComp = true;
			paintHoverRecord = comp.indication;
		}
		if (comp.indication < 0) {
			paintHoverComp = false;
			paintHoverRecord = -1;
		}

		if ((comp.getBoolPreference("hoverLine")) && (hoverRecord != -1)) {
			paintHoverNative = true;
			paintHoverRecord = hoverRecord;
		}

		if (paintHoverNative || paintHoverComp) {

			Font oldfont = g2.getFont();
			Font newfont = new Font(oldfont.getName(), Font.PLAIN, oldfont
					.getSize() - 2);
			g2.setFont(newfont);

			GeneralPath rPath = assemblePath(paintHoverRecord, 0,
					numDimensions - 1, comp);
			// float yval = getYValue(paintHoverRecord, 0, comp);
			g2.setColor(colors[paintHoverRecord]);
			g2.setStroke(new BasicStroke(3.5f));
			g2.draw(rPath);

			g2.setStroke(new BasicStroke(1.0f));
			g2.setColor(indicationColor);
			g2.draw(rPath);

			g2.setFont(oldfont);
		}

		if ((comp.getBoolPreference("hoverText")) && (paintHoverRecord != -1)) {
			Color col = new Color(1.0f, 1.0f, 0.8f);
			for (int j = 0; j < numDimensions; j++) {
				float yval = getYValue(paintHoverRecord, j, comp);
				drawTooltip(g2, comp.getAxisLabel(j) + "=\n"
						+ comp.getValue(paintHoverRecord, j), stepx * j,
						(int) yval, col);
			}
		}

		// dragging axis
		if (dragAxis) {
			AlphaComposite ac = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.7f);
			g2.setComposite(ac);

			g2.setStroke(new BasicStroke(0.5f));
			g2.drawLine(dragX - borderH, 0, dragX - borderH, height);
		}

		// tooltips
		if ((comp.getBoolPreference("hoverLine")) && (metaText != null)) {
			drawTooltip(g2, metaText, metaX, metaY + 10, new Color(0.7f, 0.7f,
					1.0f));
		}

		g2.translate(-borderH, -borderV);

	}

	void renderRegion(int startAxis, int stopAxis) {
		if (startAxis < 0) {
			startAxis = 0;
		}
		if (stopAxis >= numDimensions) {
			stopAxis = numDimensions - 1;
		}

		renderThread.setRegion(startAxis, stopAxis);
		renderThread.render();

		if (brushImg != null) {
			// if we do this, we have to add another buffer img for the brush in
			// paint()
			// brushThread.setRegion(startAxis, stopAxis);
			brushThread.render();
		}
	}

	void renderAll() {
		renderRegion(0, numDimensions - 1);
	}

	@Override
	public void renderBrush() {
		if (brushThread == null) {
			return;
		}
		brushThread.useSelectionBlur = false;
		brushThread.setRegion(0, numDimensions - 1);
		brushThread.render();
	}

	GeneralPath assemblePath(int num, int startAxis, int stopAxis,
			ParallelDisplay comp) {
		GeneralPath rPath = new GeneralPath();
		float val = getYValue(num, startAxis, comp);
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("PCPUI val = " + val);
		}
		boolean wasNaN = false;
		if (!Float.isNaN(val)) {
			rPath.moveTo(stepx * startAxis, val);
			wasNaN = false;
		} else {
			wasNaN = true;
		}
		for (int j = startAxis + 1; j <= stopAxis; j++) {
			val = getYValue(num, j, comp);
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("PCPUI val = " + val);
			}
			if (Float.isNaN(val)) {// if this one is NaN
				wasNaN = true;
			} else {// if this one is not NaN
				if (wasNaN) {// lastone was NaN, so moveTo
					rPath.moveTo(stepx * j, val);
				} else {// usual case, usual number following on usual number
					// float prevX, midX, midY;
					// prevX = (stepx - 1) * j;
					// midX = (prevX + (stepx * j)) / 2;
					// midY = (val + previousVal) / 2;
					// midY = (val + midY) / 2;

					// rPath.quadTo(midX, midY, stepx * j, val);
					rPath.lineTo(stepx * j, val);
				}
				wasNaN = false;
			}
		}
		return rPath;

	}

	void drawRecord(Graphics2D g2, ParallelDisplay comp, int num,
			int startAxis, int stopAxis, float brushVal) {
		if (numRecords <= 0) {
			return;
		}
		if (conditioning.length != numRecords) {
			conditioning = new int[numRecords];
		}
		if (conditioning[num] < 0) {
			return;
		}
		if (num >= colors.length) {
			return;
		}
		GeneralPath rPath = null;
		// if (startAxis == 0 && stopAxis == this.numDimensions-1){
		// rPath = this.rPaths[num];
		// } else {
		rPath = assemblePath(num, startAxis, stopAxis, comp);
		// }

		if (selectionOutline && brushVal > 0) {
			Stroke currStroke = g2.getStroke();
			g2.setColor(selectionColor);
			g2.setStroke(selectionStroke);
			g2.draw(rPath);
			g2.setStroke(currStroke);
		}

		if (colors != null) {
			g2.setColor(colors[num]);
		}
		if (inBrush) {
			g2.setColor(Color.blue);
		}

		g2.draw(rPath);
	}

	void drawBrushedRecord(Graphics2D g2, ParallelDisplay comp, int num,
			int startAxis, int stopAxis) {
		if (conditioning[num] < 0) {
			return;
		}
		GeneralPath rPath = assemblePath(num, startAxis, stopAxis, comp);
		// if (this.colors != null) {
		g2.setColor(Color.darkGray);
		// }
		g2.draw(rPath);
	}

	public void setColors(Color[] colors) {
		this.colors = colors;
	}

	public void setConditioning(int[] conditioning) {
		this.conditioning = conditioning;
	}

	/**
	 * Helper function to draw a "tooltip" on the given graphics object.
	 * 
	 * @param g2
	 *            The Graphics2D Object to draw on.
	 * @param text
	 *            The (multiline) text of the tooltip.
	 * @param x
	 *            The x coordinate.
	 * @param y
	 *            The y coordinate.
	 * @param col
	 *            The background color.
	 */
	private void drawTooltip(Graphics2D g2, String text, int x, int y, Color col) {
		int i;
		int mheight, mwidth = 0;
		int numLines, lineHeight;

		StringTokenizer tok = new StringTokenizer(text, "\n");
		numLines = tok.countTokens();
		String lines[] = new String[numLines];

		for (i = 0; i < numLines; i++) {
			lines[i] = tok.nextToken();

			int tempwidth = g2.getFontMetrics().stringWidth(lines[i]) + 6;
			if (tempwidth > mwidth) {
				mwidth = tempwidth;
			}
		}

		lineHeight = g2.getFontMetrics().getHeight();
		mheight = numLines * lineHeight + 2;

		x += 10;
		y += 10;
		if (x + mwidth > width) {
			x -= (mwidth + 20);
		}

		AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				0.7f);
		g2.setComposite(ac);

		g2.setStroke(new BasicStroke(0.5f));
		g2.setColor(new Color(0.2f, 0.2f, 0.2f));
		g2.drawRect(x, y, mwidth, mheight);
		g2.setColor(col);
		g2.fillRect(x + 1, y + 1, mwidth - 1, mheight - 1);

		g2.setColor(Color.black);

		ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		g2.setComposite(ac);

		for (i = 0; i < numLines; i++) {
			g2.drawString(lines[i], x + 3, y + (i + 1) * lineHeight - 4);
		}

	}

	/**
	 * Helper function to draw an arrow.
	 * 
	 * @param g2
	 *            The Graphics2D Object to draw on.
	 * @param x
	 *            The x coordinate.
	 * @param y
	 *            The y coordinate.
	 * @param size
	 *            The size in pixels.
	 * @param horizontal
	 *            If true, the arrow is drawn horizontally, if false vertically.
	 * @param topright
	 *            If true, the arrowhead is top/right, if false bottom/left.
	 */
	private void drawArrow(Graphics2D g2, int x, int y, int size,
			boolean horizontal, boolean topright) {

		if (horizontal) {

			g2.drawLine(x - size / 2, y, x + size / 2, y);

			if (topright) {
				g2.drawLine(x + size / 4, y - size / 4, x + size / 2, y);
				g2.drawLine(x + size / 4, y + size / 4, x + size / 2, y);
			} else {
				g2.drawLine(x - size / 4, y - size / 4, x - size / 2, y);
				g2.drawLine(x - size / 4, y + size / 4, x - size / 2, y);
			}
		} else {

			g2.drawLine(x, y - size / 2, x, y + size / 2);

			if (topright) {
				g2.drawLine(x + size / 4, y - size / 4, x, y - size / 2);
				g2.drawLine(x - size / 4, y - size / 4, x, y - size / 2);
			} else {
				g2.drawLine(x + size / 4, y + size / 4, x, y + size / 2);
				g2.drawLine(x - size / 4, y + size / 4, x, y + size / 2);
			}
		}
	}

	/**
	 * Helper function, returns the y value (on screen) for a given record.
	 * Scale factors and translation is applied.
	 * 
	 * @param record
	 *            The recordnumber.
	 * @param axis
	 *            The axis to calculate the y value for.
	 * @param comp
	 *            our "parent" component.
	 */
	private float getYValue(int record, int axis, ParallelDisplay comp) {
		float value = comp.getValue(record, axis);

		value -= comp.getAxisOffset(axis);
		value *= (comp.getHeight() - 2 * borderV) / comp.getAxisScale(axis);

		return value;
	}

	// record old coordinates for dragging
	int oldMouseX, oldMouseY;
	int activeAxis = -1;
	float oldScale, oldOffset;

	// 0.0 - 1.0 value of loacion of click on axis
	float clickValue;

	// actual value of point clicked on axis
	float clickAxisValue;

	int clickModifiers;

	public boolean selectionOutline = false;

	public int selectionWidth = 5;

	public Color selectionColor = Color.blue;

	private BasicStroke selectionStroke = new BasicStroke(selectionWidth);

	/**
	 * Invoked when the mouse exits the component.
	 * 
	 * @param e
	 *            The mouse event.
	 */
	public void mouseExited(MouseEvent e) {
		hoverRecord = -1;
	}

	/**
	 * Invoked when a mouse button has been released on a component. Checks if
	 * something has been dragged and finishes the drag process.
	 * 
	 * @param e
	 *            The mouse event.
	 */
	public void mouseReleased(MouseEvent e) {
		ParallelDisplay comp = (ParallelDisplay) e.getComponent();

		switch (comp.getEditMode()) {
		case ParallelDisplay.REORDER:
			dragAxis = false;
			break;
		case ParallelDisplay.BRUSH:
			inBrush = false;
			break;
		}
	}

	/**
	 * Invoked when a mouse button has been pressed on a component. Checks if
	 * the user starts dragging something.
	 * 
	 * @param e
	 *            The mouse event.
	 */
	public void mousePressed(MouseEvent e) {
		ParallelDisplay comp = (ParallelDisplay) e.getComponent();

		oldMouseX = e.getX();
		oldMouseY = e.getY();

		activeAxis = hoverAxis;

		clickModifiers = e.getModifiers();

		if (activeAxis != -1) {

			oldScale = comp.getAxisScale(activeAxis);
			oldOffset = comp.getAxisOffset(activeAxis);

			clickValue = ((float) oldMouseY - borderV)
					/ (comp.getHeight() - 2 * borderV);
			clickAxisValue = comp.getAxisOffset(activeAxis) + clickValue
					* comp.getAxisScale(activeAxis);
		}

		switch (comp.getEditMode()) {
		case ParallelDisplay.REORDER:
			dragAxis = true;
			break;
		case ParallelDisplay.BRUSH:
			brushHoverStart = oldMouseY - borderV;
			brushHoverEnd = oldMouseY - borderV;
			brushHoverX = oldMouseX - borderH;
			inBrush = true;
			hoverRecord = -1;
			createBrushImage(comp);
		}

	}

	@Override
	public void createBrushImage(ParallelDisplay comp) {
		if (brushImg == null) {
			// brushImg = (BufferedImage)comp.createImage(comp.getWidth(),
			// comp.getHeight());
			brushImg = new BufferedImage(comp.getWidth(), comp.getHeight(),
					BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D ig = brushImg.createGraphics();
			// fill with transparent white
			ig.setColor(new Color(1.0f, 1.0f, 1.0f, 0.0f));
			ig.fillRect(0, 0, comp.getWidth(), comp.getHeight());
		}
	}

	/**
	 * Invoked when a mouse button is pressed on a component and then dragged.
	 * Mouse drag events will continue to be delivered to the component where
	 * the first originated until the mouse button is released (regardless of
	 * whether the mouse position is within the bounds of the component).
	 * 
	 * Depending on the current mode, this method performs scaling, translating
	 * or reordering of axes.
	 * 
	 * @param e
	 *            The mouse event.
	 */
	public void mouseDragged(MouseEvent e) {
		ParallelDisplay comp = (ParallelDisplay) e.getComponent();

		int mouseX = e.getX();
		int mouseY = e.getY();

		setMetaInfo(null, 0, 0);

		switch (comp.getEditMode()) {
		case ParallelDisplay.SCALE:
			if (activeAxis != -1) {
				float way = ((float) (oldMouseY - mouseY))
						/ (comp.getHeight() - 2 * borderV);
				comp.setAxisScale(activeAxis, oldScale + (way * oldScale));
				float newValue = clickValue
						* (comp.getAxisScale(activeAxis) - oldScale);
				comp.setAxisOffset(activeAxis, oldOffset - newValue);

				renderRegion(activeAxis - 1, activeAxis + 1);
			}
			break;
		case ParallelDisplay.TRANSLATE:
			if (activeAxis != -1) {
				float way = ((float) (oldMouseY - mouseY))
						/ (comp.getHeight() - 2 * borderV);
				way *= comp.getAxisScale(activeAxis);
				comp.setAxisOffset(activeAxis, oldOffset + way);

				renderRegion(activeAxis - 1, activeAxis + 1);
			}
			break;
		case ParallelDisplay.REORDER:
			if (activeAxis != -1) {
				int deltaX = mouseX - oldMouseX;
				int num = activeAxis + deltaX / stepx;

				if (num < 0) {
					num = 0;
				}
				if (num >= numDimensions) {
					num = numDimensions - 1;
				}

				dragX = mouseX;

				if (activeAxis != num) {
					comp.swapAxes(activeAxis, num);
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("setting repaint axes: "
								+ (Math.min(num, activeAxis) - 1) + ", "
								+ (Math.max(num, activeAxis) + 1));
					}
					renderRegion(Math.min(num, activeAxis) - 1, Math.max(num,
							activeAxis) + 1);

					activeAxis = num;
					hoverAxis = num;
					oldMouseX = stepx * num + borderH;
				}
				// to display hoverAxis
				comp.repaint();
			}
			break;
		case ParallelDisplay.BRUSH:
			if (activeAxis != -1) {
				brushHoverEnd = mouseY - borderV;
				float releaseValue = ((float) mouseY - borderV)
						/ (comp.getHeight() - 2 * borderV);
				releaseValue = comp.getAxisOffset(activeAxis) + releaseValue
						* comp.getAxisScale(activeAxis);
				float lowerBound = Math.min(clickAxisValue, releaseValue);
				float upperBound = Math.max(clickAxisValue, releaseValue);
				boolean doSoft = comp.getBoolPreference("softBrush");
				float radius = 0.0f;
				int ids[];
				if (doSoft) {
					radius = comp.getFloatPreference("brushRadius")
							* (upperBound - lowerBound);
					if (radius == 0.0f) {
						logger.finest("radius is zero");
						doSoft = false;
					}
					ids = comp.getRecordsByValueRange(activeAxis, lowerBound
							- radius, upperBound + radius);
				} else {
					ids = comp.getRecordsByValueRange(activeAxis, lowerBound,
							upperBound);
				}
				int id = 0;
				for (int i = 0; i < comp.getNumRecords(); i++) {
					if ((ids.length > 0) && (i == ids[id])) {
						// record is inside brush region

						float brushVal = 1.0f;

						if (doSoft) {
							float val = comp.getValue(i, activeAxis);
							if (val < lowerBound) {
								brushVal = 1.0f - (-val + lowerBound) / radius;
							}
							if (val > upperBound) {
								brushVal = 1.0f - (val - upperBound) / radius;
							}
						}

						if (e.isShiftDown() && e.isAltDown()) {
							// shift + alt pressed -> intersect mode
							// we don't have anything to do
						} else if (e.isShiftDown()) {
							// shift pressed -> expand mode
							brushVal = brushVal + comp.getBrushValue(i);
							if (brushVal > 1.0f) {
								brushVal = 1.0f;
							}
							comp.setBrushValue(i, brushVal);
						} else if (e.isAltDown()) {
							// alt pressed -> subtract mode
							comp.setBrushValue(i, 1.0f - brushVal);
						} else {
							// no modifiers -> normal mode
							comp.setBrushValue(i, brushVal);
						}
						if (id < ids.length - 1) {
							id++;
						}
					} else {
						if (e.isShiftDown() && e.isAltDown()) {
							// shift + alt pressed -> intersect mode
							// clear all values outside our brush
							if (comp.getBrushValue(i) > 0) {
								comp.setBrushValue(i, 0.0f);
							}
						} else if (e.isShiftDown()) {
							// shift pressed -> expand mode
							// do nothing
						} else if (e.isAltDown()) {
							// alt pressed -> subtract mode
							// do nothing
						} else {
							// no modifiers -> normal mode
							comp.setBrushValue(i, 0.0f);
						}
					}
				}
			}

			renderBrush();
			// to see brush line in realtime
			comp.repaint();

			comp.fireSelectionChanged();
			break;

		}

	}

	/**
	 * Invoked when the mouse has been clicked on a component.
	 * 
	 * Checks if the click hit an arrow and inverts the corresponding axis.
	 * 
	 * @param e
	 *            The mouse event.
	 */
	public void mouseClicked(MouseEvent e) {
		ParallelDisplay comp = (ParallelDisplay) e.getComponent();

		// arrow clicked or invert mode
		if ((comp.getEditMode() == ParallelDisplay.INVERT)
				|| (e.getY() <= 25 && e.getY() > 12)) {
			if (hoverAxis != -1) {
				comp.setAxisOffset(hoverAxis, comp.getAxisOffset(hoverAxis)
						+ comp.getAxisScale(hoverAxis));
				comp.setAxisScale(hoverAxis, comp.getAxisScale(hoverAxis) * -1);

				renderRegion(activeAxis - 1, activeAxis + 1);

			}
		}

	}

	/**
	 * Invoked when the mouse enters a component.
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * Added by Frank Hardisty 19 July 2002
	 * 
	 * This method is called to create a hover record from outside the component
	 * 
	 */

	public void setHoverRecord(int record, ParallelDisplay comp) {
		if (record != hoverRecord) {
			hoverRecord = record;
			// if (hoverRecord != -1) {
			// setMetaInfo(comp.getRecordLabel(hoverRecord), mousex, mousey);
			// }
			comp.repaint();
		}

	}

	/**
	 * Invoked when the mouse button has been moved on a component (with no
	 * buttons no down).
	 * 
	 * Displays tooltips if mouse is hovering over axes or records.
	 * 
	 * @param e
	 *            The mouse event.
	 */
	public void mouseMoved(MouseEvent e) {

		if (!inBrush) {
			int mousex = e.getX() - borderH;
			int mousey = e.getY() - borderV;

			int oldAxis = hoverAxis;
			int oldRecord = hoverRecord;

			ParallelDisplay comp = (ParallelDisplay) e.getComponent();

			hoverAxis = -1;

			for (int i = 0; i < numDimensions; i++) {
				if ((mousex > (i * stepx - 3)) && (mousex < (i * stepx + 3))) {
					hoverAxis = i;
					break;
				}
			}

			hoverRecord = getRecordByCoordinates(mousex, mousey, comp);

			// added frank Hardisty 19 july 2002
			if (oldRecord != hoverRecord) {
				comp.fireIndicationChanged(hoverRecord);
			}

			if ((oldAxis != hoverAxis) || (oldRecord != hoverRecord)) {
				if (hoverAxis != -1) {
					setMetaInfo(comp.getAxisLabel(hoverAxis), mousex, mousey);

					switch (comp.getEditMode()) {
					case ParallelDisplay.REORDER:
						comp.setCursor(Cursor
								.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
						break;
					case ParallelDisplay.SCALE:
						comp.setCursor(Cursor
								.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
						break;
					case ParallelDisplay.TRANSLATE:
						comp.setCursor(Cursor
								.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
						break;
					case ParallelDisplay.INVERT:
						comp.setCursor(Cursor
								.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
						break;
					case ParallelDisplay.BRUSH:
						comp.setCursor(Cursor
								.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
						break;
					}

				} else {
					setMetaInfo(null, 0, 0);

					comp.resetCursor();
				}

				if (hoverRecord != -1) {
					setMetaInfo(comp.getRecordLabel(hoverRecord), mousex,
							mousey);
				}

				comp.repaint();
			}
		}

	}

	/**
	 * Helper method to display a tooltip on hover.
	 */
	private void setMetaInfo(String text, int x, int y) {
		metaText = text;
		metaX = x;
		metaY = y;
	}

	/**
	 * Returns the record that goes through the screen coordinates x,y. The
	 * first record that is found is returned.
	 * 
	 * @param x
	 *            The x screen coordinate.
	 * @param y
	 *            The y screen coordinate.
	 * @param comp
	 *            The "parent" component.
	 * 
	 * @return The recordnumber of the first record found passing through the
	 *         given point.
	 */
	public int getRecordByCoordinates(int x, int y, ParallelDisplay comp) {
		for (int i = 0; i < numDimensions - 1; i++) {
			if ((x >= i * stepx) && (x < (i + 1) * stepx)) {
				float part = (x - i * stepx) / (float) stepx;

				for (int j = 0; j < numRecords; j++) {
					if (j >= comp.getNumRecords()) {
						return -1;
					}
					float recVal = (1 - part) * getYValue(j, i, comp) + part
							* getYValue(j, i + 1, comp);
					if (logger.isLoggable(Level.FINEST)) {
						logger.finest("getRecordByCoordinates" + recVal);
					}
					if (Math.abs(recVal - y) < 3.0) {
						return j;
					}
				}
				break;
			}
		}

		return -1;
	}

	/**
	 * Returns the record that goes through the screen coordinates x,y. The
	 * first record that is found is returned.
	 * 
	 * @param x
	 *            The x screen coordinate.
	 * @param y
	 *            The y screen coordinate.
	 * @param comp
	 *            The "parent" component.
	 * 
	 * @return The recordnumber of the first record found passing through the
	 *         given point.
	 */
	public int[] getAllRecordsByCoordinates(Rectangle2D hitBox,
			ParallelDisplay comp) {
		int x = (int) hitBox.getX();
		int y = (int) hitBox.getY();
		Vector recs = new Vector();
		for (int i = 0; i < numDimensions - 1; i++) {
			if ((x >= i * stepx) && (x < (i + 1) * stepx)) {// if x part matches
				float part = (x - i * stepx) / (float) stepx;
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("part " + part);
				}
				for (int j = 0; j < numRecords; j++) {
					float recVal = (1 - part) * getYValue(j, i, comp) + part
							* getYValue(j, i + 1, comp);

					if (Math.abs(recVal - y) < 3.0) {
						recs.add(new Integer(j));
					}

				}

			}
		}

		return null;
	}

	@Override
	public RenderThread getRenderThread() {
		return renderThread;
	}

	public RenderThread getBrushThread() {
		return brushThread;
	}

	public void setSelectionWidth(int width2) {
		selectionWidth = width2;
		selectionStroke = new BasicStroke(width2);

	}

}