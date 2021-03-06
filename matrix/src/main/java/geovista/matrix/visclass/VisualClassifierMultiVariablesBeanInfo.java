/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.matrix.visclass;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

import geovista.geoviz.visclass.VisualClassifier;

public class VisualClassifierMultiVariablesBeanInfo extends SimpleBeanInfo {
	static Class beanClass = VisualClassifier.class;
	static String iconColor16x16Filename = "resources/visualClassifier16.gif";
	static String iconColor32x32Filename = "resources/visualClassifier32.gif";
	static String iconMono16x16Filename;
	static String iconMono32x32Filename;

	public VisualClassifierMultiVariablesBeanInfo() {
	}

	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		try {
			return (new PropertyDescriptor[] {
					new PropertyDescriptor("symbolizationPanel", beanClass),
					new PropertyDescriptor("variableChooserMode", beanClass),
					new PropertyDescriptor("colors", beanClass) // ,
			// new PropertyDescriptor("labelTable", beanClass),
			});
		} catch (Exception e) {
			e.printStackTrace();
			return new PropertyDescriptor[] {};
		}
	}

	@Override
	public java.awt.Image getIcon(int iconKind) {
		switch (iconKind) {
		case BeanInfo.ICON_COLOR_16x16:
			return iconColor16x16Filename != null
					? loadImage(iconColor16x16Filename) : null;
		case BeanInfo.ICON_COLOR_32x32:
			return iconColor32x32Filename != null
					? loadImage(iconColor32x32Filename) : null;
		case BeanInfo.ICON_MONO_16x16:
			return iconMono16x16Filename != null
					? loadImage(iconMono16x16Filename) : null;
		case BeanInfo.ICON_MONO_32x32:
			return iconMono32x32Filename != null
					? loadImage(iconMono32x32Filename) : null;
		}
		return null;
	}

}
