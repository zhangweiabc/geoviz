package geovista.toolkitcore.marshal;

import java.awt.Color;

import javax.swing.JComponent;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SpatialExtentEvent;
import geovista.common.event.SpatialExtentListener;

public abstract class VizBeanConverter implements Converter {

    private static String SELECTION = "selection";
    private static String BACKGROUND_COLOR = "backgroundColor";
    private static String SPATIAL_EXTENT = "spatialExtent";

    /* subclassses need to override, always returns false */
    public boolean canConvert(Class clazz) {
	return false;
    }

    public void marshal(Object value, HierarchicalStreamWriter writer,
	    MarshallingContext context) {

	if (value instanceof SelectionListener) {
	    SelectionListener selList = (SelectionListener) value;
	    // commented out the following line because we are not using
	    // it currently.
	    // int[] selection = selList.getSelectionEvent().getSelection();
	    int[] selection = null;
	    if (selection != null) {
		writer.startNode(VizBeanConverter.SELECTION);
		context.convertAnother(selection);
		writer.endNode();

	    }
	}

	if (value instanceof JComponent) {
	    JComponent jcomp = (JComponent) value;
	    writer.startNode(VizBeanConverter.BACKGROUND_COLOR);
	    Color background = jcomp.getBackground();
	    Color newColor = new Color(background.getRGB());
	    context.convertAnother(newColor);
	    writer.endNode();
	}
	if (value instanceof SpatialExtentListener) {
	    SpatialExtentListener selList = (SpatialExtentListener) value;
	    SpatialExtentEvent e = selList.getSpatialExtentEvent();
	    if (e != null) {
		writer.startNode(VizBeanConverter.SPATIAL_EXTENT);
		context.convertAnother(e);
		writer.endNode();

	    }
	}

    }

    public Object unmarshal(HierarchicalStreamReader reader,
	    UnmarshallingContext context) {
	return new Object();

    }

    public Object unmarshal(HierarchicalStreamReader reader,
	    UnmarshallingContext context, Object instantiatedObject) {

	while (reader.hasMoreChildren()) {
	    reader.moveDown();
	    if (VizBeanConverter.SELECTION.equals(reader.getNodeName())) {
		SelectionListener selList = (SelectionListener) instantiatedObject;
		int[] selection = (int[]) context.convertAnother(
			instantiatedObject, int[].class);
		SelectionEvent e = new SelectionEvent(this, selection);
		selList.selectionChanged(e);
	    } else if (VizBeanConverter.BACKGROUND_COLOR.equals(reader
		    .getNodeName())) {
		JComponent comp = (JComponent) instantiatedObject;
		// Color bColor = (ColorUIResource) context.convertAnother(
		// instantiatedObject, ColorUIResource.class);
		Color bColor = (Color) context.convertAnother(
			instantiatedObject, Color.class);
		comp.setBackground(bColor);
	    } else if (VizBeanConverter.SPATIAL_EXTENT.equals(reader
		    .getNodeName())) {
		SpatialExtentListener comp = (SpatialExtentListener) instantiatedObject;
		SpatialExtentEvent e = (SpatialExtentEvent) context
			.convertAnother(instantiatedObject,
				SpatialExtentEvent.class);

		comp.spatialExtentChanged(e);
	    }
	    reader.moveUp();
	}

	return instantiatedObject;
    }

}
