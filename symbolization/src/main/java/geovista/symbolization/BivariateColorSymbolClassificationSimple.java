/* -------------------------------------------------------------------
 Java source file for the interface BivariateColorSymbolClassificationSimple
 Original Author: Frank Hardisty
 $Author: hardistf $
 $Id: ComparableShapes.java,v 1.1 2005/12/05 20:17:05 hardistf Exp $
 $Date: 2005/12/05 20:17:05 $
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 -------------------------------------------------------------------   */

package geovista.symbolization;

import java.awt.Color;

import geovista.colorbrewer.ColorBrewer;
import geovista.colorbrewer.UnivariatePalette;
import geovista.common.classification.Classifier;
import geovista.common.classification.ClassifierQuantiles;

public class BivariateColorSymbolClassificationSimple implements
		BivariateColorSymbolClassification {

	private ColorSymbolizer colorerX;
	private Classifier classerX;
	private ColorSymbolizer colorerY;
	private Classifier classerY;
	// XXX need to merge ColorSymbolizer and UnivariatePalette
	UnivariatePalette xPal;
	UnivariatePalette yPal;
	private transient int numClassesX;
	private transient int numClassesY;
	transient int[] classesX;
	transient int[] classesY;

	public static final int DEFAULT_NUM_CLASSES = 3;

	public ColorSymbolizer getXColorSymbolizer() {
		return colorerX;
	}

	public ColorSymbolizer getYColorSymbolizer() {
		return colorerY;
	}

	public Color[][] getClassColors() {
		Color[][] currColors = new Color[numClassesX][numClassesY];
		Color[] xColors = colorerX.getColors(numClassesX);
		Color[] yColors = colorerY.getColors(numClassesY);

		xColors = xPal.getColors(numClassesX);
		yColors = yPal.getColors(numClassesY);
		for (int x = 0; x < currColors.length; x++) {
			for (int y = 0; y < currColors[0].length; y++) {
				Color colorX = xColors[x];
				Color colorY = yColors[y];

				currColors[x][y] = ColorInterpolator
						.mixColorsRGBHighSaturation(colorX, colorY);
			}
		}

		return currColors;

	}

	public BivariateColorSymbolClassificationSimple() {
		// defaults
		xPal = ColorBrewer
				.getPalette(ColorBrewer.BrewerNames.Blues);
		yPal = ColorBrewer
				.getPalette(ColorBrewer.BrewerNames.Greens);

		ColorSymbolizerLinear colX = new ColorSymbolizerLinear();
		colX.setLowColor(ColorRampPicker.DEFAULT_LOW_COLOR);
		colX.setHighColor(ColorRampPicker.DEFAULT_HIGH_COLOR_PURPLE);
		colorerX = colX;

		classerX = new ClassifierQuantiles();

		ColorSymbolizerLinear colY = new ColorSymbolizerLinear();
		colY.setLowColor(ColorRampPicker.DEFAULT_LOW_COLOR);
		colY.setHighColor(ColorRampPicker.DEFAULT_HIGH_COLOR_GREEN);
		colorerY = colY;

		classerY = new ClassifierQuantiles();
		numClassesX = BivariateColorSymbolClassificationSimple.DEFAULT_NUM_CLASSES;
		numClassesY = BivariateColorSymbolClassificationSimple.DEFAULT_NUM_CLASSES;

	}

	public Color[] symbolize(double[] dataX, double[] dataY) {

		if (dataX == null || dataY == null) {
			return null;
		}

		if (dataX.length != dataY.length) {
			// throw new
			// IllegalArgumentException("BivariateColorSymbolClassificationSimple.symbolize"
			// +
			// " recieved input arrays of different length.");
			System.err
					.println("BivariateColorSymbolClassificationSimple.symbolize recieved input arrays of different length");
			return null;
		}

		if (dataX == dataY) { // if they are the same object
			return symbolizeUnivariate(dataX);
		}

		Color[] colorsX = colorerX.getColors(numClassesX);
		classesX = classerX.classify(dataX, numClassesX);
		int myClassX = 0;
		Color colorX = null;

		Color[] colorsY = colorerY.getColors(numClassesY);
		classesY = classerY.classify(dataY, numClassesY);
		int myClassY = 0;
		Color colorY = null;

		Color[] returnColors = new Color[dataX.length];

		for (int i = 0; i < classesX.length; i++) {
			myClassX = classesX[i];
			myClassY = classesY[i];

			if (myClassX == Classifier.NULL_CLASS
					|| myClassY == Classifier.NULL_CLASS
					|| myClassX > colorsX.length || myClassY > colorsY.length) {
				returnColors[i] = ColorSymbolizer.DEFAULT_NULL_COLOR;
			} else {
				colorX = colorsX[myClassX];
				colorY = colorsY[myClassY];
				returnColors[i] = ColorInterpolator.mixColorsRGBHighSaturation(
						colorX, colorY);
			}
		}
		return returnColors;
	}

	private Color[] symbolizeUnivariate(double[] dataX) {
		Color[] colorsX = colorerX.getColors(numClassesX);
		classesX = classerX.classify(dataX, numClassesX);
		int myClassX = 0;
		Color colorX = null;

		Color[] returnColors = new Color[dataX.length];

		for (int i = 0; i < classesX.length; i++) {
			myClassX = classesX[i];
			if (myClassX == Classifier.NULL_CLASS
					|| classesX[i] > colorsX.length) {
				returnColors[i] = ColorSymbolizer.DEFAULT_NULL_COLOR;
			} else {
				colorX = colorsX[classesX[i]];
				// returnColors[i] =
				// ColorInterpolator.mixColorsRGB(colorX,colorY);
				returnColors[i] = colorX;
			}
		}

		// for the benefit of getClassY
		classesY = classesX;

		return returnColors;
	}

	public void setColorerY(ColorSymbolizer colorerY) {
		this.colorerY = colorerY;
		numClassesY = colorerY.getNumClasses();
	}

	public ColorSymbolizer getColorerY() {
		return colorerY;
	}

	public void setClasserY(Classifier classerY) {
		this.classerY = classerY;
	}

	public Classifier getClasserY() {
		return classerY;
	}

	public void setColorerX(ColorSymbolizer colorerX) {
		this.colorerX = colorerX;
		numClassesX = colorerX.getNumClasses();
	}

	public ColorSymbolizer getColorerX() {
		return colorerX;
	}

	public void setClasserX(Classifier classerX) {
		this.classerX = classerX;
	}

	public Classifier getClasserX() {
		return classerX;
	}

	public int getClassX(int observation) {
		if (classesX == null) {
			return -1;
		}
		return classesX[observation];
	}

	public int getClassY(int observation) {
		if (classesY == null) {
			return -1;
		}
		return classesY[observation];
	}

	public int getNumClassesX() {
		return numClassesX;
	}

	public int getNumClassesY() {
		return numClassesY;
	}

	public void setNumClassesX(int numClassesX) {
		this.numClassesX = numClassesX;
	}

	public void setNumClassesY(int numClassesY) {
		this.numClassesY = numClassesY;
	}

}
