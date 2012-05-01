package geovista.geoviz.map;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import geovista.geoviz.map.MapCanvas.MapMode;

public class MapPopup extends JPopupMenu implements ActionListener {

    private JMenuItem panItem;
    private JMenuItem selectItem;
    private JMenuItem zoomInItem;
    private JMenuItem zoomOutItem;
    private JMenuItem resetItem;

    MapModeListener listener;

    public MapPopup(MapModeListener listener) {
	this.listener = listener;

	makeItems();
    }

    public interface MapModeListener {
	void setMapMode(MapMode mode);
    }

    private void makeItems() {
	panItem = new JMenuItem("Pan Mode");
	this.add(panItem);
	panItem.addActionListener(this);

	selectItem = new JMenuItem("Select Mode");
	this.add(selectItem);
	selectItem.addActionListener(this);

	zoomInItem = new JMenuItem("Zoom In Mode");
	this.add(zoomInItem);
	zoomInItem.addActionListener(this);

	zoomOutItem = new JMenuItem("Zoom Out Mode");
	this.add(zoomOutItem);
	zoomOutItem.addActionListener(this);

	resetItem = new JMenuItem("Reset Map");
	this.add(resetItem);
	resetItem.addActionListener(this);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == panItem) {
	    listener.setMapMode(MapCanvas.MapMode.Pan);
	} else if (e.getSource() == zoomInItem) {
	    listener.setMapMode(MapCanvas.MapMode.ZoomIn);
	} else if (e.getSource() == selectItem) {
	    listener.setMapMode(MapCanvas.MapMode.Select);
	} else if (e.getSource() == zoomOutItem) {
	    listener.setMapMode(MapCanvas.MapMode.ZoomOut);
	} else if (e.getSource() == resetItem) {
	    listener.setMapMode(MapCanvas.MapMode.Reset);
	}

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
	// TODO Auto-generated method stub

    }

}
