/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.geoviz.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Vector;

import javax.swing.JFrame;

import geovista.symbolization.glyph.Glyph;

/**
 * put your documentation comment here
 */
public class LayerText extends LayerShape {
	transient Vector strings;
	transient Vector points;
	transient Vector fonts;
	transient Vector colors;
	transient Color defaultColor;
	transient Font defaultFont;
	// GeoReference gvGeoReference;
	private transient boolean scalable;

	@Override
	public int findIndication(int x, int y) {
		return Integer.MIN_VALUE;
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param scalable
	 */
	public void setScaleable(boolean scalable) {
		this.scalable = scalable;
	}

	@Override
	public void setGlyphs(Glyph[] glyphs) {
		// XXX noop
	}

	/**
	 * put your documentation comment here
	 * 
	 * @return
	 */
	public boolean getScaleable() {
		return scalable;
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param s
	 * @param p
	 * @param f
	 * @param c
	 */
	public void addString(String s, Point p, Font f, Color c) {
		strings.add(s);
		points.add(p);
		fonts.add(f);
		colors.add(c);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param s
	 * @param p
	 * @param fontSize
	 * @param c
	 */
	public void addString(String s, Point p, float fontSize, Color c) {
		strings.add(s);
		points.add(p);
		fonts.add(defaultFont.deriveFont(fontSize));
		colors.add(c);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param s
	 * @param p
	 */
	public void addString(String s, Point p) {
		strings.add(s);
		points.add(p);
		fonts.add(defaultFont);
		colors.add(defaultColor);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @return
	 */
	public Vector getStrings() {
		return strings;
	}

	/**
	 * put your documentation comment here
	 * 
	 * @return
	 */
	public Vector getPoints() {
		return points;
	}

	/**
	 * put your documentation comment here
	 * 
	 * @return
	 */
	public Vector getFonts() {
		return fonts;
	}

	/*
	 * selectionX1 is expected to be less than selectionX2, same with Y1 and y2
	 */
	@Override
	public void findSelection(int selectionX1, int selectionX2,
			int selectionY1, int selectionY2) {

	}

	/*
	 * selectionX1 is expected to be less than selectionX2, same with Y1 and y2
	 */
	@Override
	public void findSelectionShift(int selectionX1, int selectionX2,
			int selectionY1, int selectionY2) {

	}

	/**
	 * put your documentation comment here
	 * 
	 * @param g2
	 */
	@Override
	public void renderSelectedObservations(Graphics2D g2) {

		int numStrings = strings.size();
		for (int stringNum = 0; stringNum < numStrings; stringNum++) {
			Point p = (Point) getPoints().get(stringNum);
			String drawS = (String) getStrings().get(stringNum);
			Font f = (Font) getFonts().get(stringNum);
			g2.setFont(f);
			// Color c = (Color)getColors().get(stringNum);
			// g2.setColor(c);
			g2.drawString(drawS, p.x, p.y);
		}
	}

	/**
	 * dummy method to fit superclass
	 * 
	 * @param polygonColors
	 */
	public void setColorIndex(int[] polygonColors) {
		// this.colorIndex = polygonColors;
	}

	/**
	 * dummy method to fit superclass
	 * 
	 * @param polygonColors
	 */
	public int[] getColorIndex() {
		// return this.colorIndex;
		return null;
	}

	/**
	 * Main method for testing.
	 */
	public static void main(String[] args) {
		JFrame jFrame = new JFrame();
		jFrame.setSize(200, 200);
		jFrame.setVisible(true);
		Graphics2D g2 = (Graphics2D) jFrame.getGraphics();
		String s = "Hello World!";
		Point point = new Point(50, 100);
		LayerText gvTextLayer = new LayerText();
		gvTextLayer.addString(s, point);
		gvTextLayer.renderSelectedObservations(g2);
	}
}
