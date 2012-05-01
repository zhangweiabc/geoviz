/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.geoviz.map;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MapPopupAdapter extends MouseAdapter {
    MapPopup popMenu;

    public MapPopupAdapter(MapPopup popMenu) {
	super();
	this.popMenu = popMenu;
    }

    @Override
    public void mousePressed(MouseEvent e) {
	maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
	maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
	if (e.isPopupTrigger()) {
	    popMenu.show(e.getComponent(), e.getX(), e.getY());
	}
    }

    // XXX add in methods to handle
}
