/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.matrix.spacefill;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JToolBar;

import geovista.common.data.DataSetForApps;
import geovista.geoviz.spacefill.SpaceFillCanvas;
import geovista.matrix.MatrixElement;

public class SpaceFillMatrixElement extends SpaceFillCanvas implements
		MatrixElement {

	// the following are required for returning to matrix
	private int[] elementPosition;
	private double[] xAxisExtents;
	private double[] yAxisExtents;
	private DataSetForApps dataSet;
	private Color selectionColor;

	public SpaceFillMatrixElement() {
		super();
		super.setUseDrawingShapes(false);
		setBorder(BorderFactory.createLineBorder(Color.darkGray, 1));
	}

	/**
	 * @param data
	 * 
	 *            This method is deprecated becuase it wants to create its very
	 *            own pet DataSetForApps. This is no longer allowed, to allow
	 *            for a mutable, common data set. Use of this method may lead to
	 *            unexpected program behavoir. Please use setDataSet instead.
	 */
	@Deprecated
	public void setDataObject(Object[] data) {
		setDataSet(new DataSetForApps(data));

	}

	@Override
	public void setDataSet(DataSetForApps data) {
		dataSet = data;
		super.setDataSet(data);
	}

	public void setDataIndices(int[] dataIndices) {
		elementPosition = dataIndices.clone();
		super.setCurrOrderColumn(elementPosition[0]);// order = x
		super.setCurrColorColumn(elementPosition[1]);// color = y

	}

	public int[] getElementPosition() {
		return elementPosition;
	}

	// For axes of scatter plot.
	// a noop for this class
	public void setAxisOn(boolean axisOn) {
	}

	// Set min and max for axes. xAxisExtents[0] = min, [1] = max.
	public void setXAxisExtents(double[] xAxisExtents) {

	}

	public void setYAxisExtents(double[] yAxisExtents) {
	}

	public double[] getXAxisExtents() {
		return xAxisExtents;
	}

	public double[] getYAxisExtents() {
		return yAxisExtents;
	}

	public String getShortDiscription() {
		return "SFP";
	}

	// public void setBivarColorClasser (BivariateColorSymbolClassification
	// bivarColorClasser) {
	// this.bivarColorClasser = bivarColorClasser;
	// this.sendColorsToLayers(this.dataColorX.length);
	// }

	public void setSelectionColor(Color c) {
		selectionColor = c;
		super.setColorSelection(c);
	}

	public Color getSelectionColor() {
		return selectionColor;
	}

	public void setMultipleSelectionColors(Color[] c) {
	}

	public JToolBar getTools() {
		return null;
	}

	/**
	 * This method only paints the current contents of the drawingBuff.
	 * 
	 * @param g
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (elementPosition == null) {
			return;
		}

		if (elementPosition[0] == elementPosition[1]) {
			Graphics2D g2 = (Graphics2D) g;
			Color half = new Color(255, 255, 255, 150);
			g2.setColor(half);
			Font font = new Font("Serif", Font.PLAIN, 24);
			g2.setFont(font);
			FontRenderContext frc = g2.getFontRenderContext();

			String[] varNames = dataSet.getAttributeNamesNumeric();// XXX
			// right
			// method
			// call?
			String varName = varNames[elementPosition[0] - 1];// -1 to
			// skip
			// string
			// array

			int midX = 10;
			int midY = getHeight() / 2;

			Rectangle2D textBounds = g2.getFont().getStringBounds(varName, frc);
			Rectangle rect = textBounds.getBounds();
			rect.setSize(rect.width, (int) (rect.height * 1.5));
			rect.setLocation(midX, midY - (int) (rect.getHeight() / 1.5));
			// Rectangle rect = new Rectangle();
			rect.setLocation(0, 0);
			rect.setSize(getWidth(), getHeight());
			g2.fill(rect);
			// g2.fillRect(0,0,this.getWidth(),this.getHeight());
			g2.setColor(Color.black);

			// g2.drawString(varName,midX,midY);
		}
	}

}
