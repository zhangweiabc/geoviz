/*
 * TouchGraph LLC. Apache-Style Software License
 *
 *
 * Copyright (c) 2002 Alexander Shapiro. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        TouchGraph LLC (http://www.touchgraph.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "TouchGraph" or "TouchGraph LLC" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission.  For written permission, please contact
 *    alex@touchgraph.com
 *
 * 5. Products derived from this software may not be called "TouchGraph",
 *    nor may "TouchGraph" appear in their name, without prior written
 *    permission of alex@touchgraph.com.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL TOUCHGRAPH OR ITS CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 */

package geovista.touchgraph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Date;
import java.util.Vector;

/**
 * Node.
 * 
 * @author Alexander Shapiro
 * @author Murray Altheim (2001-11-06; added support for round rects and
 *         alternate Node colors)
 * 
 */
public class Node {

	/** This Node's type is a Rectangle. */
	public final static int TYPE_RECTANGLE = 1;

	/** This Node's type is a Round Rectangle. */
	public final static int TYPE_ROUNDRECT = 2;

	/** This Node's type is an Ellipse. */
	public final static int TYPE_ELLIPSE = 3;

	public static final String AUTO_ID_STRING = "AutoID ";
	public static final Font SMALL_TAG_FONT = new Font("Courier", Font.PLAIN, 9);

	// Variables that store default values for colors + fonts + node type
	public Color BACK_FIXED_COLOR = Color.red;
	public static Color BACK_SELECT_COLOR = new Color(255, 224, 0);
	public static Color BACK_DEFAULT_COLOR = new Color(208, 96, 0);
	public static Color BACK_HILIGHT_COLOR = Color.decode("#ffb200"); // altheim:
	// new

	public static Color BORDER_DRAG_COLOR = Color.black;
	public static Color BORDER_MOUSE_OVER_COLOR = new Color(160, 160, 160);
	public static Color BORDER_INACTIVE_COLOR = Color.white;

	public static Color TEXT_COLOR = Color.black;// modified fah

	public static Font TEXT_FONT = new Font("Courier", Font.PLAIN, 12);

	public static int DEFAULT_TYPE = 1;

	/**
	 * an int indicating the Node type.
	 * 
	 * @see TYPE_RECTANGLE
	 * @see TYPE_ROUNDRECT
	 * @see TYPE_ELLIPSE
	 */
	transient protected int typ = TYPE_RECTANGLE;
	transient private String id;

	transient public double drawx;
	transient public double drawy;

	transient protected FontMetrics fontMetrics;
	transient protected Font font;

	transient protected String lbl;
	transient protected Color backColor = BACK_DEFAULT_COLOR;
	transient protected Color textColor = TEXT_COLOR;

	transient protected boolean visible;

	transient public double x;
	transient public double y;

	transient protected double dx; // Used by layout
	transient protected double dy; // Used by layout

	transient protected boolean fixed;
	transient protected int repulsion; // Used by layout

	transient public boolean justMadeLocal = false;
	transient public boolean markedForRemoval = false;
	transient public int localEdgeCount;

	transient private Vector edges;

	// ............

	/**
	 * Minimal constructor which will generate an ID value from Java's Date
	 * class. Defaults will be used for type and color. The label will be taken
	 * from the ID value.
	 */
	public Node() {
		initialize(null);
		lbl = id;
	}

	/**
	 * Constructor with the required ID <tt>id</tt>, using defaults for type
	 * (rectangle), color (a static variable from TGPanel). The Node's label
	 * will be taken from the ID value.
	 */
	public Node(String id) {
		initialize(id);
		lbl = id;
	}

	/**
	 * Constructor with Strings for ID <tt>id</tt> and <tt>label</tt>, using
	 * defaults for type (rectangle) and color (a static variable from TGPanel).
	 * If the label is null, it will be taken from the ID value.
	 */
	public Node(String id, String label) {
		initialize(id);
		if (label == null) {
			lbl = id;
		} else {
			lbl = label;
		}
	}

	/**
	 * Constructor with a String ID <tt>id</tt>, an int <tt>type</tt>,
	 * Background Color <tt>bgColor</tt>, and a String <tt>label</tt>. If the
	 * label is null, it will be taken from the ID value.
	 * 
	 * @see TYPE_RECTANGLE
	 * @see TYPE_ROUNDRECT
	 */
	public Node(String id, int type, Color color, String label) {
		initialize(id);
		typ = type;
		backColor = color;
		if (label == null) {
			lbl = id;
		} else {
			lbl = label;
		}
	}

	private void initialize(String identifier) {
		if (identifier != null) {
			id = identifier;
		} else {
			id = AUTO_ID_STRING + new Date().getTime();
		}

		edges = new Vector();
		x = Math.random() * 2 - 1; // If multiple nodes are added without
		// repositioning,
		y = Math.random() * 2 - 1; // randomizing starting location causes them
		// to spread out nicely.
		repulsion = 100;
		font = TEXT_FONT;
		fixed = false;
		typ = DEFAULT_TYPE;
		localEdgeCount = 0;
	}

	// setters and getters ...............

	public void setNodeBackFixedColor(Color color) {
		BACK_FIXED_COLOR = color;
	}

	public static void setNodeBackSelectColor(Color color) {
		BACK_SELECT_COLOR = color;
	}

	public static void setNodeBackDefaultColor(Color color) {
		BACK_DEFAULT_COLOR = color;
	}

	public static void setNodeBackHilightColor(Color color) {
		BACK_HILIGHT_COLOR = color;
	}

	public static void setNodeBorderDragColor(Color color) {
		BORDER_DRAG_COLOR = color;
	}

	public static void setNodeBorderMouseOverColor(Color color) {
		BORDER_MOUSE_OVER_COLOR = color;
	}

	public static void setNodeBorderInactiveColor(Color color) {
		BORDER_INACTIVE_COLOR = color;
	}

	public static void setNodeTextColor(Color color) {
		TEXT_COLOR = color;
	}

	public static void setNodeTextFont(Font font) {
		TEXT_FONT = font;
	}

	public static void setNodeType(int type) {
		DEFAULT_TYPE = type;
	}

	/**
	 * Set the ID of this Node to the String <tt>id</tt>.
	 */
	public void setID(String id) {
		this.id = id;
	}

	/**
	 * Return the ID of this Node as a String.
	 */
	public String getID() {
		return id;
	}

	/**
	 * Set the location of this Node provided the Point <tt>p</tt>.
	 */
	public void setLocation(Point p) {
		x = p.x;
		y = p.y;
	}

	/**
	 * Return the location of this Node as a Point.
	 */
	public Point getLocation() {
		return new Point((int) x, (int) y);
	}

	/**
	 * Set the visibility of this Node to the boolean <tt>v</tt>.
	 */
	public void setVisible(boolean v) {
		visible = v;
	}

	/**
	 * Return the visibility of this Node as a boolean.
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Set the type of this Node to the int <tt>type</tt>.
	 * 
	 * @see TYPE_RECTANGLE
	 * @see TYPE_ROUNDRECT
	 * @see TYPE_ELLIPSE
	 */

	public void setType(int type) {
		typ = type;
	}

	/**
	 * Return the type of this Node as an int.
	 * 
	 * @see TYPE_RECTANGLE
	 * @see TYPE_ROUNDRECT
	 * @see TYPE_ELLIPSE
	 */
	public int getType() {
		return typ;
	}

	/** Set the font of this Node to the Font <tt>font</tt>. */
	public void setFont(Font font) {
		this.font = font;
	}

	/** Returns the font of this Node as a Font */
	public Font getFont() {
		return font;
	}

	/** Set the background color of this Node to the Color <tt>bgColor</tt>. */
	public void setBackColor(Color bgColor) {
		backColor = bgColor;
	}

	/**
	 * Return the background color of this Node as a Color.
	 */
	public Color getBackColor() {
		return backColor;
	}

	/** Set the text color of this Node to the Color <tt>txtColor</tt>. */
	public void setTextColor(Color txtColor) {
		textColor = txtColor;
	}

	/**
	 * Return the text color of this Node as a Color.
	 */
	public Color getTextColor() {
		return textColor;
	}

	/** Set the label of this Node to the String <tt>label</tt>. */
	public void setLabel(String label) {
		lbl = label;
	}

	/**
	 * Return the label of this Node as a String.
	 */
	public String getLabel() {
		return lbl;
	}

	/** Set the fixed status of this Node to the boolean <tt>fixed</tt>. */
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}

	/**
	 * Returns true if this Node is fixed (in place).
	 */
	public boolean getFixed() {
		return fixed;
	}

	// ....

	/** Returns the number of Edges attached to this Node. */
	public int edgeNum() {
		return edges.size();
	}

	/** Returns the local Edge count. */
	public int localEdgeNum() {
		return localEdgeCount;
	}

	/** Return the Edge at int <tt>index</tt>. */
	public Edge edgeAt(int index) {
		return (Edge) edges.elementAt(index);
	}

	/** Add the Edge <tt>edge</tt> to the graph. */
	public void addEdge(Edge edge) {
		if (edge == null) {
			return;
		}
		edges.addElement(edge);
	}

	/** Remove the Edge <tt>edge</tt> from the graph. */
	public void removeEdge(Edge edge) {
		edges.removeElement(edge);
	}

	/** Return the width of this Node. */
	public int getWidth() {
		if (fontMetrics != null && lbl != null) {
			return fontMetrics.stringWidth(lbl) + 12;
		}
		return 10;
	}

	/** Return the height of this Node. */
	public int getHeight() {
		if (fontMetrics != null) {
			return fontMetrics.getHeight() + 6;
		}
		return 6;
	}

	/** Returns true if this Node intersects Dimension <tt>d</tt>. */
	public boolean intersects(Dimension d) {
		return (drawx > 0 && drawx < d.width && drawy > 0 && drawy < d.height);
	}

	/** Returns true if this Node contains the Point <tt>px,py</tt>. */
	public boolean containsPoint(double px, double py) {
		return ((px > drawx - getWidth() / 2) && (px < drawx + getWidth() / 2)
				&& (py > drawy - getHeight() / 2) && (py < drawy + getHeight()
				/ 2));
	}

	/** Returns true if this Node contains the Point <tt>p</tt>. */
	public boolean containsPoint(Point p) {
		return ((p.x > drawx - getWidth() / 2)
				&& (p.x < drawx + getWidth() / 2)
				&& (p.y > drawy - getHeight() / 2) && (p.y < drawy
				+ getHeight() / 2));
	}

	/** Paints the Node. */
	public void paint(Graphics g, TGPanel tgPanel) {
		if (!intersects(tgPanel.getSize())) {
			return;
		}
		paintNodeBody(g, tgPanel);

		if (localEdgeNum() < edgeNum()) {
			int ix = (int) drawx;
			int iy = (int) drawy;
			int h = getHeight();
			int w = getWidth();
			int tagX = ix + (w - 7) / 2 - 2 + w % 2;
			int tagY = iy - h / 2 - 2;
			char character;
			int hiddenEdgeNum = edgeNum() - localEdgeNum();
			character = (hiddenEdgeNum < 9) ? (char) ('0' + hiddenEdgeNum)
					: '*';
			paintSmallTag(g, tgPanel, tagX, tagY, Color.red, Color.white,
					character);
		}
	}

	public Color getPaintBorderColor(TGPanel tgPanel) {
		if (this == tgPanel.getDragNode()) {
			return BORDER_DRAG_COLOR;
		} else if (this == tgPanel.getMouseOverN()) {
			return BORDER_MOUSE_OVER_COLOR;
		} else {
			return BORDER_INACTIVE_COLOR;
		}
	}

	public Color getPaintBackColor(TGPanel tgPanel) {
		if (this == tgPanel.getSelect()) {
			return BACK_SELECT_COLOR;
		}
		if (fixed) {
			return BACK_FIXED_COLOR;
		}
		if (markedForRemoval) {
			return new Color(100, 60, 40);
		}
		if (justMadeLocal) {
			return new Color(255, 220, 200);
		}
		return backColor;
	}

	public Color getPaintTextColor(TGPanel tgPanel) {
		return textColor;
	}

	/** Paints the background of the node, along with its label */
	public void paintNodeBody(Graphics g, TGPanel tgPanel) {
		g.setFont(font);
		fontMetrics = g.getFontMetrics();

		int ix = (int) drawx;
		int iy = (int) drawy;
		int h = getHeight();
		int w = getWidth();
		int r = h / 2 + 1; // arc radius

		Color borderCol = getPaintBorderColor(tgPanel);
		g.setColor(borderCol);

		if (typ == TYPE_ROUNDRECT) {
			g.fillRoundRect(ix - w / 2, iy - h / 2, w, h, r, r);
		} else if (typ == TYPE_ELLIPSE) {
			g.fillOval(ix - w / 2, iy - h / 2, w, h);
		} else { // TYPE_RECTANGLE
			g.fillRect(ix - w / 2, iy - h / 2, w, h);
		}

		Color backCol = getPaintBackColor(tgPanel);
		g.setColor(backCol);

		if (typ == TYPE_ROUNDRECT) {
			g.fillRoundRect(ix - w / 2 + 2, iy - h / 2 + 2, w - 4, h - 4, r, r);
		} else if (typ == TYPE_ELLIPSE) {
			g.fillOval(ix - w / 2 + 2, iy - h / 2 + 2, w - 4, h - 4);
		} else { // TYPE_RECTANGLE
			g.fillRect(ix - w / 2 + 2, iy - h / 2 + 2, w - 4, h - 4);
		}

		Color textCol = getPaintTextColor(tgPanel);
		g.setColor(textCol);
		g.drawString(lbl, ix - fontMetrics.stringWidth(lbl) / 2, iy
				+ fontMetrics.getDescent() + 1);
	}

	/** Paints a tag with containing a character in a small font. */
	public void paintSmallTag(Graphics g, TGPanel tgPanel, int tagX, int tagY,
			Color backCol, Color textCol, char character) {
		g.setColor(backCol);
		g.fillRect(tagX, tagY, 8, 8);
		g.setColor(textCol);
		g.setFont(SMALL_TAG_FONT);
		g.drawString("" + character, tagX + 2, tagY + 7);
	}

} // end com.touchgraph.graphlayout.Node
