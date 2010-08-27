/*
 * TMView.java
 * www.bouthier.net
 *
 * The MIT License :
 * -----------------
 * Copyright (c) 2001 Christophe Bouthier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

/**
 * The TMView class implements a view of the TreeMap.
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
public class TMView extends JPanel implements Observer {

	/* Used to set the display on a classic geovista.matrix.treemap.tm * */
	public static final String CLASSIC = "CLASSIC";

	/* Used to set the display on a squarified geovista.matrix.treemap.tm * */
	public static final String SQUARIFIED = "SQUARIFIED";

	private TMNodeModelRoot root = null; // the root model of the
	// geovista.matrix.treemap.tm
	private TMAlgorithm drawer = null; // the drawing algorithm
	private Hashtable store = null; // the algortihms store
	private TMStatusView statusView = null; // the status view
	private PaintMethod paintMethod = null; // the drawing method

	private JComponent initView = null; // initialization status view
	private JLabel banner = null; // building status banner

	private TMAction action = null; // action manager

	/* --- Constructor --- */

	/**
	 * Constructor.
	 * 
	 * @param updater
	 *            the updater of TMNodes of the TreeMap
	 * @param cSize
	 *            the object computing the size;
	 * @param cDraw
	 *            the object computing the drawing;
	 */
	TMView(TMUpdaterConcrete updater, TMComputeSize cSize, TMComputeDraw cDraw) {
		super(new BorderLayout());
		action = new TMAction(this);

		if (!cSize.isCompatibleWith(updater.getRoot())) {
			throw new TMExceptionBadTMNodeKind(cSize, updater.getRoot());
		}
		if (!cDraw.isCompatibleWith(updater.getRoot())) {
			throw new TMExceptionBadTMNodeKind(cDraw, updater.getRoot());
		}

		setPreferredSize(new Dimension(250, 250));
		setOpaque(true);
		paintMethod = new EmptyPaintMethod();

		statusView = new TMStatusView();
		initView = statusView.getView();
		banner = new JLabel("Building geovista.matrix.treemap.tm view...");
		banner.setHorizontalAlignment(SwingConstants.CENTER);
		banner.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(banner, BorderLayout.NORTH);
		add(initView, BorderLayout.CENTER);

		root = new TMNodeModelRoot(updater.getRoot(), cSize, cDraw, statusView,
				this);

		store = new Hashtable();
		addAlgorithm(new TMAlgorithmClassic(), CLASSIC);
		addAlgorithm(new TMAlgorithmSquarified(), SQUARIFIED);
		setAlgorithm(SQUARIFIED);// fah changed default

		updater.addObserver(this);
	}

	/* --- Initialization --- */

	/**
	 * Sets this view as initialized.
	 */
	void Initialized() {
		ToolTipManager.sharedInstance().registerComponent(this);

		TMAlgorithm algorithm = null;
		for (Enumeration e = store.elements(); e.hasMoreElements();) {
			algorithm = (TMAlgorithm) e.nextElement();
			algorithm.initialize(root.getRoot(), this);
		}

		remove(banner);
		remove(initView);
		banner = null;
		initView = null;
		paintMethod = new FullPaintMethod();

		addMouseListener(action);

		repaint();
	}

	/* --- Status management --- */

	/**
	 * Gets the status view.
	 * 
	 * @return the status view
	 */
	public JComponent getStatusView() {
		return statusView.getView();
	}

	/* --- Changing geovista.matrix.treemap.tm algorithm --- */

	/**
	 * Adds a geovista.matrix.treemap.tm algorithm to this view. The algorithm
	 * is a subclass of TMAlgorithm, and is caracterized by a name. It is this
	 * name that should be passed as parameter to setAlgorithm(). If there were
	 * already an algorithm registered under this name, the old algorithm is
	 * lost.
	 * 
	 * @param algorithm
	 *            the algorithm
	 * @param name
	 *            the name of the algorithm
	 */
	public void addAlgorithm(TMAlgorithm algorithm, String name) {
		if (algorithm == null) {
			throw new TMExceptionNullParameter("Impossible to use a null "
					+ "algorithm.");
		}
		if (name == null) {
			throw new TMExceptionNullParameter("Impossible to register an "
					+ "algorithm under a null name.");
		}
		algorithm.initialize(root.getRoot(), this);
		store.put(name, algorithm);
	}

	/**
	 * Sets the algorithm of the geovista.matrix.treemap.tm. The name given
	 * should be one of the TMView constant or a name already registered with
	 * addAlgorithm(). Does nothing if the parameter is not a reckognized
	 * string.
	 * 
	 * @param algoName
	 *            the name of the algorithm
	 */
	public void setAlgorithm(String algoName) {
		Object o = store.get(algoName);
		if (o instanceof TMAlgorithm) {
			drawer = (TMAlgorithm) o;
			repaint();
		}
	}

	/**
	 * Returns the current algorithm of this view.
	 * 
	 * @return the current algorithm
	 */
	public TMAlgorithm getAlgorithm() {
		return drawer;
	}

	/**
	 * Returns the set of registered names of algorithms.
	 * 
	 * @return an Enumeration of registered names
	 */
	public Enumeration getAlgorithmsNames() {
		return store.keys();
	}

	/* --- Changing TMComputeSize or TMComputeDraw --- */

	/**
	 * Changes the TMComputeSize object used to compute the sizings of TMNodes.
	 * 
	 * @param cSize
	 *            the new TMComputeSize
	 */
	public void changeTMComputeSize(TMComputeSize cSize) {
		if (!cSize.isCompatibleWith(root.getRoot().getNode())) {
			statusView.setStatus(new TMSDSimple("Incompatible type of "
					+ "TMComputeSize."));
			return;
		}
		root.launchChangeSizing(cSize);
	}

	/**
	 * Changes the TMComputeDraw object used to compute the drawings of TMNodes.
	 * 
	 * @param cDraw
	 *            the new TMComputeDraw
	 */
	public void changeTMComputeDraw(TMComputeDraw cDraw) {
		// fah i've never had this come out wrong, and it's causing a problem
		// if (! cDraw.isCompatibleWith(root.getRoot().getNode())) {
		// statusView.setStatus(new TMSDSimple("Incompatible type of "
		// + "TMComputeDraw."));
		// return;
		// }
		root.launchChangeDrawing(cDraw);
	}

	/* --- Titles Nodes --- */

	/**
	 * Sets the drawing or not of the nodes titles.
	 * 
	 * @param bool
	 *            the boolean deciding if yes or no we draws the nodes titles
	 */
	public void DrawTitles(boolean bool) {
		if (bool == true) {
			drawer.drawNodesTitles();
		} else {
			drawer.dontDrawNodesTitles();
		}
		repaint();
	}

	public boolean isDrawingTitle() {
		return drawer.isDrawingTitles();
	}

	/* --- Observer --- */

	/**
	 * Called when a TMUpdater has its data changed.
	 * 
	 * @param o
	 *            the Observable object that has its data changed
	 * @param arg
	 *            the Object describing the change
	 */
	public void update(Observable o, Object arg) {
		if (arg instanceof TMEventUpdate) {
			((TMEventUpdate) arg).execute(root);
		}
	}

	/* --- Node finding --- */

	/**
	 * Returns the node containing the mouse event.
	 * <P>
	 * If using <CODE>TMNode</CODE>, this will be a TMNode. Else, if using
	 * <CODE>TMModelNode</CODE>, this is an Object.
	 * 
	 * @param event
	 *            the mouse event on a node
	 * @return the node containing this event; could be <CODE>null</CODE> if no
	 *         node was found
	 */
	public Object getNodeUnderTheMouse(MouseEvent event) {
		if (root == null) {
			return null;
		}
		TMNodeModel nodeM = root.nodeContaining(event.getX(), event.getY());
		if (nodeM != null) {
			TMNode node = nodeM.getNode();
			if (node instanceof TMNodeEncapsulator) {
				return ((TMNodeEncapsulator) node).getNode();
			}
			return node;
		}
		return null;
	}

	/* --- Zooming --- */

	/**
	 * Zooms the geovista.matrix.treemap.tm of one level in the direction of the
	 * node at the given coordinates.
	 * 
	 * @param x
	 *            the x coord
	 * @param y
	 *            the y coord
	 */
	public void zoom(int x, int y) {
		root.zoom(x, y);
	}

	/**
	 * Unzooms. Does nothing if already at the root.
	 */
	public void unzoom() {
		root.unzoom();
	}

	/* --- Displaying --- */

	/**
	 * Returns the tooltip to be displayed.
	 * 
	 * @param event
	 *            the event triggering the tooltip
	 * @return the String to be displayed
	 */
	@Override
	public String getToolTipText(MouseEvent event) {
		int x = event.getX();
		int y = event.getY();
		TMNodeModel node = root.nodeContaining(x, y);
		if (node != null) {
			root.getLock().lock();
			String tooltip = node.getTooltip();
			root.getLock().unlock();
			return tooltip;
		}
		Insets insets = getInsets();
		if (((x <= insets.left) || (x >= (getWidth() - insets.right - 1)))
				|| ((y <= insets.top) || (y >= (getHeight() - insets.bottom - 1)))) {
			return "This is the border of the geovista.matrix.treemap.tm";
		}
		return "This rectangle is due to imprecision in calculs";
	}

	/**
	 * Paint the component.
	 * 
	 * @param g
	 *            the graphic context
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (g instanceof Graphics2D) {
			paintMethod.paint((Graphics2D) g);
		} else {
			System.err.println("Error : TreeMap 2.0 requires Java 1.2 "
					+ "or superior.");
			System.exit(1);
		}
	}

	/* --- Inners paintMethods --- */

	/**
	 * The PaintMethod abstract class implements a Strategie design pattern for
	 * the paintComponent method.
	 */
	abstract class PaintMethod {

		/**
		 * Paint method.
		 * 
		 * @param g
		 *            the Graphics2D context
		 */
		abstract void paint(Graphics2D g);
	}

	/**
	 * The EmptyPaintMethod implements a empty paint method.
	 */
	class EmptyPaintMethod extends PaintMethod {

		/**
		 * Paint method.
		 * 
		 * @param g
		 *            the Graphics2D context
		 */
		@Override
		final void paint(Graphics2D g) {
			revalidate();
		}
	}

	/**
	 * The FullPaintMethod implements a full paint method.
	 */
	class FullPaintMethod extends PaintMethod {

		/**
		 * Paint method.
		 * 
		 * @param g
		 *            the Graphics2D context
		 */
		@Override
		final void paint(Graphics2D g) {
			Insets insets = getInsets();
			root.getRoot().getArea().setBounds(insets.left, insets.top,
					getWidth() - insets.left - insets.right - 1,
					getHeight() - insets.top - insets.bottom - 1);
			root.getLock().lock();
			drawer.draw(g, root.getRoot());
			root.getLock().unlock();
		}
	}

}
