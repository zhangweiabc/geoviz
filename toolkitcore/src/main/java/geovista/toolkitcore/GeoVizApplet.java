/* 
 Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 
 Original Author: Frank Hardisty
 
 */

package geovista.toolkitcore;

import javax.swing.JApplet;

/**
 * Creates an instance of GeoVizToolkit wrapped as an applet
 * 
 * @author Frank Hardisty
 */

public class GeoVizApplet extends JApplet {
	@Override
	public void start() {
		GeoVizToolkit gvt = new GeoVizToolkit();
		gvt.setVisible(true);
	}

}
