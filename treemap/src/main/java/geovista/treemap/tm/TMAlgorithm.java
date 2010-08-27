/*
 * TMAlgorithm.java
 * www.bouthier.net
 *
 * The MIT License :
 * -----------------
 * Copyright (c) 2001 Christophe Bouthier, Vesselin Markovsky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package geovista.treemap.tm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The TMAlgorithm abstract class represent the algorithm of the TreeMap. It
 * should be subclassed by every class that want to implement a particular
 * geovista.matrix.treemap.tm (classic geovista.matrix.treemap.tm, squarified
 * geovista.matrix.treemap.tm, ...).
 * <P>
 * A subclass can also override the drawNode() method to have a customized
 * drawing of a node.
 * <P>
 * And now with cushion geovista.matrix.treemap.tm, thanks to Jarke J. van Wijk
 * :-)
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * @author Vesselin Markovsky [markovsky@semantec.bg]
 * 
 */
public abstract class TMAlgorithm extends Observable {

	private static final ColorModel cModel = ColorModel.getRGBdefault();

	private static final int dimMax = 32;
	private static WritableRaster[][] cachedRasters = new WritableRaster[dimMax + 1][dimMax + 1];
	private static int[][][] cachedBuffers = new int[dimMax + 1][dimMax + 1][];

	private TMCushionPaint painter;

	private final Font titleFont = new Font("Dialog", Font.PLAIN, 10);
	private final Color borderColor = Color.black;

	protected double h = 0.50;
	protected double f = 1;
	protected boolean cushion = false;
	protected boolean border = false;
	protected int IS = 215;

	protected final static double LX = 0.09759;
	protected final static double LY = -0.19518;
	protected final double LZ = 0.9759;

	// Axis of separation in the TreeMap
	protected final static short HORIZONTAL = 0;
	protected final static short VERTICAL = 1;

	protected TMNodeModel root = null; // root of the TMNodeModel tree
	protected TMView view = null; // view using this TMAlgorithm

	protected int borderSize = 4; // size of the border
	protected int borderLimit = 0; // limit to draw nested border

	protected boolean nodesTitles = true; // draw the nodes titles.

	/* --- Initialization --- */

	/**
	 * Initialize the algorithm.
	 * 
	 * @param root
	 *            the root of the TMNodeModel tree
	 * @param view
	 *            the view using this algorithm
	 */
	public void initialize(TMNodeModel root, TMView view) {
		this.root = root;
		this.view = view;
		painter = new TMCushionPaint();
	}

	/* --- Nodes titles --- */

	/**
	 * Draws the nodes titles.
	 */
	public void drawNodesTitles() {
		setDrawingTitles(true);
	}

	/**
	 * Don't draws the nodes titles.
	 */
	public void dontDrawNodesTitles() {
		setDrawingTitles(false);
	}

	public boolean isDrawingTitles() {
		return nodesTitles;
	}

	public void setDrawingTitles(boolean drawing) {
		nodesTitles = drawing;
		view.repaint();
		setChanged();
		notifyObservers();
	}

	/* --- Cushion management --- */

	public void setCushion(boolean cushion) {
		this.cushion = cushion;
		view.repaint();
		setChanged();
		notifyObservers();
	}

	public boolean isCushion() {
		return cushion;
	}

	public void setBorderOnCushion(boolean border) {
		this.border = border;
		view.repaint();
		setChanged();
		notifyObservers();
	}

	public boolean isBorderOnCushion() {
		return border;
	}

	public void setH(double h) {
		this.h = h;
		view.repaint();
		setChanged();
		notifyObservers();
	}

	public double getH() {
		return h;
	}

	public void setF(double f) {
		this.f = f;
		view.repaint();
		setChanged();
		notifyObservers();
	}

	public double getF() {
		return f;
	}

	public void setIS(int IS) {
		this.IS = IS;
		view.repaint();
		setChanged();
		notifyObservers();
	}

	public int getIS() {
		return IS;
	}

	/* --- Nested management --- */

	/**
	 * Sets the border size.
	 * 
	 * @param size
	 *            the border size
	 */
	public void setBorderSize(int size) {
		borderSize = size;
		borderLimit = (borderSize * 2) + 4;
		view.repaint();
		setChanged();
		notifyObservers();
	}

	/**
	 * Returns the border size.
	 * 
	 * @return the border size
	 */
	public int getBorderSize() {
		return borderSize;
	}

	/**
	 * Returns a view for configuring this algorithm.
	 * 
	 * @return the configuring view
	 */
	public JPanel getConfiguringView() {
		return new TMBorderConf();
	}

	/* --- Drawing --- */

	/**
	 * Starts the process of drawing the geovista.matrix.treemap.tm.
	 * 
	 * @param g
	 *            the graphic context
	 * @param root
	 *            the root
	 */
	void draw(Graphics2D g, TMNodeModel root) {
		this.root = root;
		drawNodes(g, root, HORIZONTAL, 1);
	}

	/**
	 * Draws the node and recurses the drawing on its children.
	 * 
	 * @param g
	 *            the graphic context
	 * @param node
	 *            the node to draw
	 * @param axis
	 *            the axis of separation
	 * @param level
	 *            the level of deep
	 */
	protected void drawNodes(Graphics2D g, TMNodeModel node, short axis,
			int level) {
		Rectangle oldClip = g.getClipBounds();
		Rectangle area = node.getArea();
		TMCushionData data = computeCushionData(node.getParent(), area, axis);
		node.setCushionData(data);
		g.clipRect(area.x, area.y, area.width + 1, area.height + 1);
		if (cushion) {
			fillCushionNode(g, node, level, data);
		} else {
			fillNode(g, node, level);
		}
		if (!node.isLeaf()) {
			drawChildren(g, (TMNodeModelComposite) node, axis, level);
		} else {
		}
		g.setClip(oldClip.x, oldClip.y, oldClip.width, oldClip.height);
	}

	/* --- SubClass utility --- */

	/**
	 * Switch the axis and return the new axis.
	 * 
	 * @param axis
	 *            the axis to switch
	 * @return the new axis
	 */
	protected short switchAxis(short axis) {
		// Axis : Bold as love
		if (axis == HORIZONTAL) {
			return VERTICAL;
		}
		return HORIZONTAL;
	}

	/**
	 * Compute node's cushion data.
	 */
	protected TMCushionData computeCushionData(TMNodeModel father,
			Rectangle area, short axis) {
		TMCushionData data = null;
		if (father == null) {
			data = new TMCushionData();
			data.h = h;
		} else {
			data = new TMCushionData(father.getCushionData());
			data.h *= f;
		}
		if (axis == VERTICAL) {
			data.x2 -= (4 * data.h) / area.width;
			data.x += (4 * data.h * (area.x + area.x + area.width))
					/ area.width;
		} else {
			data.y2 -= (4 * data.h) / area.height;
			data.y += (4 * data.h * (area.y + area.y + area.height))
					/ area.height;
		}
		return data;
	}

	protected void fillCushionNode(Graphics2D g, TMNodeModel node, int level,
			TMCushionData data) {
		Rectangle area = node.getArea();

		// WARNING !!!
		// Don't use g.fill(Shape s) or g.draw(Shape s),
		// they are really too slow !!!
		Paint filling = node.getFilling();
		if (!(filling instanceof Color)) {
			filling = Color.WHITE;
		}
		g.setPaint(painter.init((Color) filling, data));
		g.fillRect(area.x, area.y, area.width, area.height);
		if (border) {
			g.setPaint(borderColor);
			g.drawRect(area.x, area.y, area.width, area.height);
		}
		if (nodesTitles) {
			g.setPaint(node.getColorTitle());
			g.setFont(titleFont);
			g.drawString(node.getTitle(), area.x + 1, area.y + 10);
		}
	}

	/* --- COULD BE OVERRIDED IN SUBCLASS --- */

	/**
	 * Fills the node.
	 * 
	 * @param g
	 *            the graphic context
	 * @param node
	 *            the TMNodeModel to draw
	 * @param level
	 *            the level of deep
	 */
	protected void fillNode(Graphics2D g, TMNodeModel node, int level) {
		Rectangle area = node.getArea();

		// WARNING !!!
		// Don't use g.fill(Shape s) or g.draw(Shape s),
		// they are really too slow !!!
		g.setPaint(node.getFilling());
		g.fillRect(area.x, area.y, area.width, area.height);
		g.setPaint(borderColor);
		g.drawRect(area.x, area.y, area.width, area.height);
		if (nodesTitles) {
			g.setPaint(node.getColorTitle());
			g.setFont(titleFont);
			g.drawString(node.getTitle(), area.x + 1, area.y + 10);
		}
	}

	/* --- TO BE IMPLEMENTED IN SUBCLASS --- */

	/**
	 * Draws the children of a node, by setting their drawing area first,
	 * dependant of the algorithm used.
	 * 
	 * @param g
	 *            the graphic context
	 * @param node
	 *            the node whose children should be drawn
	 * @param axis
	 *            the axis of separation
	 * @param level
	 *            the level of deep
	 */
	protected abstract void drawChildren(Graphics2D g,
			TMNodeModelComposite node, short axis, int level);

	/* --- Inner view --- */

	/**
	 * The TMBorderConf class implements a configuration view for the
	 * geovista.matrix.treemap.tm algorithms. It keeps also the cushion
	 * parameters. It display a JSlider to configure the border size, thus
	 * putting the algorithm into nested.
	 */
	class TMBorderConf extends JPanel implements Observer {

		JSlider bSize = null; // the JSlider
		JSlider fSize = null;
		JSlider hSize = null;
		JSlider iSize = null;
		JCheckBox cush = null;
		JCheckBox bord = null;

		/**
		 * Constructor.
		 */
		TMBorderConf() {
			super(new BorderLayout());

			addObserver(this);

			bSize = new JSlider(SwingConstants.HORIZONTAL, 0, 50,
					getBorderSize());
			bSize.setMajorTickSpacing(10);
			bSize.setMinorTickSpacing(1);
			bSize.setPaintTicks(true);
			bSize.setPaintLabels(true);
			bSize.addChangeListener(new SliderListener());
			this.add(bSize, BorderLayout.CENTER);
			this.add(new JLabel("Border Size"), BorderLayout.NORTH);

			JPanel cushionPanel = new JPanel(new BorderLayout());
			cushionPanel.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createEtchedBorder(), "Cushion"));
			this.add(cushionPanel, BorderLayout.SOUTH);

			JPanel checkPanel = new JPanel(new GridLayout(1, 2));
			cush = new JCheckBox("Cushion", cushion);
			cush.addActionListener(new CushListener());
			bord = new JCheckBox("Border", border);
			bord.addActionListener(new BordListener());
			checkPanel.add(cush);
			checkPanel.add(bord);
			cushionPanel.add(checkPanel, BorderLayout.NORTH);

			JPanel slidersPanel = new JPanel(new GridLayout(3, 1));
			cushionPanel.add(slidersPanel, BorderLayout.CENTER);

			JPanel fPanel = new JPanel(new BorderLayout());
			fSize = new JSlider(SwingConstants.HORIZONTAL, 0, 20, 10);
			fSize.setMajorTickSpacing(5);
			fSize.setMinorTickSpacing(1);
			fSize.setPaintTicks(true);
			fSize.setPaintLabels(true);
			fSize.addChangeListener(new fSliderListener());
			fPanel.add(fSize, BorderLayout.CENTER);
			fPanel.add(new JLabel("Propagation Factor"), BorderLayout.NORTH);
			slidersPanel.add(fPanel);

			JPanel hPanel = new JPanel(new BorderLayout());
			hSize = new JSlider(SwingConstants.HORIZONTAL, -10, 10, 5);
			hSize.setMajorTickSpacing(5);
			hSize.setMinorTickSpacing(1);
			hSize.setPaintTicks(true);
			hSize.setPaintLabels(true);
			hSize.addChangeListener(new hSliderListener());
			hPanel.add(hSize, BorderLayout.CENTER);
			hPanel.add(new JLabel("Hight"), BorderLayout.NORTH);
			slidersPanel.add(hPanel);

			JPanel iPanel = new JPanel(new BorderLayout());
			iSize = new JSlider(SwingConstants.HORIZONTAL, 0, 255, 215);
			iSize.setMajorTickSpacing(50);
			iSize.setMinorTickSpacing(25);
			iSize.setPaintTicks(true);
			iSize.setPaintLabels(true);
			iSize.addChangeListener(new iSliderListener());
			iPanel.add(iSize, BorderLayout.CENTER);
			iPanel.add(new JLabel("Intensity"), BorderLayout.NORTH);
			slidersPanel.add(iPanel);
		}

		/**
		 * Called by the algorithm when parameters have changed.
		 * 
		 * @param o
		 *            the TMAlgorithm
		 * @param arg
		 *            not used here
		 */
		public void update(Observable o, Object arg) {
			bSize.setValue(getBorderSize());
		}

		/**
		 * Inner's inner listener.
		 */
		class SliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {
				setBorderSize(bSize.getValue());
			}
		}

		class fSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {
				f = (fSize.getValue()) / 10.0;
				view.repaint();
				setChanged();
				notifyObservers();
			}
		}

		class hSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {
				h = (hSize.getValue()) / 10.0;
				view.repaint();
				setChanged();
				notifyObservers();
			}
		}

		class iSliderListener implements ChangeListener {

			public void stateChanged(ChangeEvent e) {
				setIS(iSize.getValue());
				view.repaint();
				setChanged();
				notifyObservers();
			}
		}

		class CushListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				if (cush.isSelected()) {
					cushion = true;
				} else {
					cushion = false;
				}
				view.repaint();
				setChanged();
				notifyObservers();
			}
		}

		class BordListener implements ActionListener {

			public void actionPerformed(ActionEvent e) {
				if (bord.isSelected()) {
					border = true;
				} else {
					border = false;
				}
				view.repaint();
				setChanged();
				notifyObservers();
			}
		}
	}

	/**
	 * The TMCushionPaint implements a customized java.awt.Paint for the cushion
	 * visualization
	 * 
	 * @author Christophe Bouthier [bouthier@loria.fr]
	 * 
	 */

	class TMCushionPaint implements Paint, PaintContext {

		private Color c = null;
		private TMCushionData cData = null;

		private WritableRaster raster = null;

		double nx;
		double ny;
		double delta;
		double intensity;

		int i;
		int j;
		int base;
		int[] data;

		int tX;
		int tY;

		/* --- Constructor --- */

		/**
		 * Constructor.
		 */
		public TMCushionPaint init(Color c, TMCushionData cData) {
			this.c = c;
			this.cData = cData;
			return this;
		}

		/* --- Paint --- */

		public PaintContext createContext(ColorModel cm,
				Rectangle deviceBounds, Rectangle2D userBounds,
				AffineTransform xform, RenderingHints hints) {
			// coordinate may be tranlated when repainting a part
			// of the widget.
			tX = (int) xform.getTranslateX();
			tY = (int) xform.getTranslateY();
			return this;
		}

		/* --- Transparency, from Paint --- */

		public int getTransparency() {
			return c.getTransparency();
		}

		/* --- Paint context --- */

		public void dispose() {
		}

		public ColorModel getColorModel() {
			return cModel;
		}

		public Raster getRaster(int x, int y, int w, int h) {
			raster = createRaster(w, h);

			// taking in account the possible translation
			x -= tX;
			y -= tY;

			int[] data = createBuffer(w, h);
			for (j = 0; j < h; j++) {
				ny = ((-2) * cData.y2 * (y + j)) - cData.y;
				for (i = 0; i < w; i++) {
					nx = ((-2) * cData.x2 * (x + i)) - cData.x;
					delta = ((nx * LX) + (ny * LY) + LZ)
							/ Math.sqrt((nx * nx) + (ny * ny) + 1);

					if (delta < 0) {
						delta = 0;
					}

					intensity = 1 + getIS() * ((delta - 1) / 255);

					base = (j * w + i) * 4;
					data[base + 0] = (int) (c.getRed() * intensity);
					data[base + 1] = (int) (c.getGreen() * intensity);
					data[base + 2] = (int) (c.getBlue() * intensity);
					data[base + 3] = (c.getAlpha());
				}
			}

			raster.setPixels(0, 0, w, h, data);
			return raster;
		}

		/* --- Raster management --- */

		private int[] createBuffer(int w, int h) {
			int[] buffer;
			if ((w <= dimMax) && (h <= dimMax)) {
				buffer = cachedBuffers[w][h];
				if (buffer == null) {
					buffer = new int[w * h * 4];
					cachedBuffers[w][h] = buffer;
				}
			} else {
				buffer = new int[w * h * 4];
			}
			return buffer;
		}

		private WritableRaster createRaster(int w, int h) {
			WritableRaster raster;

			if ((w <= dimMax) && (h <= dimMax)) {
				raster = cachedRasters[w][h];
				if (raster == null) {
					raster = getColorModel().createCompatibleWritableRaster(w,
							h);
					cachedRasters[w][h] = raster;
				}
			} else {
				raster = getColorModel().createCompatibleWritableRaster(w, h);
			}
			return raster;
		}

	}

}
