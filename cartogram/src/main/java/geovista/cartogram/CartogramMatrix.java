/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */
package geovista.cartogram;

import java.awt.Dimension;

import javax.swing.JFrame;

import geovista.matrix.BiPlotMatrix;
import geovista.readers.example.GeoDataGeneralizedStates;

/**
 * 
 * @author Frank Hardisty
 * 
 */
public class CartogramMatrix extends BiPlotMatrix

{

	public CartogramMatrix() {
		super();
		this.setElementClass1(CartogramMatrixElement.class);
		this.setElementClass2(CartogramMatrixElement.class);

	}

	public static void main(String[] args) {

		GeoDataGeneralizedStates stateData = new GeoDataGeneralizedStates();

		CartogramMatrix gui = new CartogramMatrix();
		gui.setPreferredSize(new Dimension(500, 500));
		JFrame app = new JFrame("Cartogram Matrix");
		app.getContentPane().add(gui);
		gui.setDataSet(stateData.getDataForApps());
		app.pack();
		app.setVisible(true);

		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
