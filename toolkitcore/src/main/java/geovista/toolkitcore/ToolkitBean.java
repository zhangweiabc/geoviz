/* -------------------------------------------------------------------
 Java source file for the class ToolkitBean
 Copyright (c), 2005 Frank Hardisty
 $Author: hardisty $
 $Id: ToolkitBean.java,v 1.1 2005/02/13 03:26:27 hardisty Exp $
 $Date: 2005/02/13 03:26:27 $
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

package geovista.toolkitcore;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Shape;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

import geovista.common.jts.EmptyShape;
import geovista.common.ui.ShapeReporter;
import geovista.coordination.CoordinationUtils;

/*
 * This is a set of objects that each bean coordinated in a GeoVizToolkit is
 * expected to have
 * 
 * 
 * 
 */

public class ToolkitBean implements ComponentListener, ShapeReporter {
	final static Logger logger = Logger.getLogger(ToolkitBean.class.getName());
	private Object originalBean;
	private transient JMenuItem removeMenuItem;
	JInternalFrame internalFrame;
	JPanel pinFrame;
	EventListenerList listenerList;
	String uniqueName;
	String objectClass;
	public int zOrder;

	// private static String defaultName = "bean";

	public ToolkitBean() {

	}

	public ToolkitBean(Object originalBean, String uniqueName) {
		init(originalBean, uniqueName);

	}

	public void init(Object originalBean, String uniqueName) {
		logger.fine("in toolkitbean, in init");
		listenerList = new EventListenerList();
		this.setOriginalBean(originalBean, uniqueName);
		objectClass = originalBean.getClass().getName();
		this.uniqueName = uniqueName;
	}

	public JInternalFrame getInternalFrame() {
		return internalFrame;
	}

	public Object getOriginalBean() {
		return originalBean;
	}

	JMenuItem getRemoveMenuItem() {
		return removeMenuItem;
	}

	private void setOriginalBean(Object originalBean, String uniqueName) {
		Image im = CoordinationUtils.findSmallIcon(originalBean);
		Icon ic = new ImageIcon(im);
		this.originalBean = originalBean;
		internalFrame = makeInternalFrame(originalBean, uniqueName, ic);
		removeMenuItem = new JMenuItem(uniqueName, ic);
		this.uniqueName = uniqueName;
	}

	private JInternalFrame makeInternalFrame(Object newInstance,
			String uniqueName, Icon ic) {
		JInternalFrame newFrame = new JInternalFrame(uniqueName, true, true,
				true, true);
		newFrame.addComponentListener(this);

		newFrame.setVisible(true);

		newFrame.setFrameIcon(ic);
		// JRootPane pane = newFrame.getRootPane();
		// pane.setWindowDecorationStyle(JRootPane.NONE);
		Component newComp = null;
		if (newInstance instanceof Component) {
			newComp = (Component) newInstance;
		} else {
			newComp = new JButton(uniqueName);
		}
		Dimension d = newComp.getPreferredSize();
		Dimension newD = new Dimension(d.width + 5, d.height + 10);
		newFrame.setSize(newD);
		newFrame.getContentPane().add(newComp);
		newFrame.revalidate();
		return newFrame;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	// start component event handling
	// note: this class only listens to its JInternalFrame
	// we pass these right on up to any interested listener
	public void componentHidden(ComponentEvent e) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ComponentListener.class) {

				((ComponentListener) listeners[i + 1]).componentHidden(e);
			}
		}

	}

	public void componentMoved(ComponentEvent e) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ComponentListener.class) {

				((ComponentListener) listeners[i + 1]).componentMoved(e);
			}
		}

	}

	public void componentShown(ComponentEvent e) {
		// Supposed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ComponentListener.class) {

				((ComponentListener) listeners[i + 1]).componentShown(e);
			}
		}

	}

	public void componentResized(ComponentEvent e) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ComponentListener.class) {

				((ComponentListener) listeners[i + 1]).componentResized(e);
			}
		}
	}

	void addComponentListener(ComponentListener l) {
		listenerList.add(ComponentListener.class, l);
	}

	void setOriginalBean(Object originalBean) {
		this.originalBean = originalBean;
	}

	public String getObjectClass() {
		return objectClass;
	}

	public Shape reportShape() {
		if (originalBean instanceof ShapeReporter) {
			ShapeReporter sr = (ShapeReporter) originalBean;
			return sr.reportShape();
		}
		return EmptyShape.INSTANCE;
	}

	public Component renderingComponent() {
		if (originalBean instanceof ShapeReporter) {
			ShapeReporter sr = (ShapeReporter) originalBean;
			return sr.renderingComponent();
		}
		return null;
	}

}
