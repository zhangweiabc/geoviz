/*
 * TMFileDraw.java
 * www.bouthier.net
 *
 * The MIT License :
 * -----------------
 * Copyright (c) 2001 Christophe Bouthier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package geovista.treemap.tm.demo;

import java.awt.Color;
import java.awt.Paint;
import java.text.DateFormat;
import java.util.Date;

import geovista.treemap.tm.TMComputeDraw;
import geovista.treemap.tm.TMExceptionBadTMNodeKind;
import geovista.treemap.tm.TMNode;
import geovista.treemap.tm.TMNodeAdapter;

/**
 * The TMFileDraw class implements an example of a TMComputeDraw for a
 * TMFileNode. It use the date of last modification as color, and the name of
 * the file as tooltip.
 * <P>
 * The color legend is :
 * <UL>
 * <IL> white for files less than a hour old <IL> green for files less than a
 * day old <IL> yellow for files less than a week old <IL> orange for files less
 * than a month old <IL> red for files less than a year old <IL> blue for files
 * more than a year old
 * </UL>
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
public class TMFileDraw implements TMComputeDraw {

	/**
	 * Test if this TMComputeDraw could be used with the kind of TMNode passed
	 * in parameter.
	 * 
	 * @param node
	 *            the TMNode to test the compatibility with
	 * @return <CODE>true</CODE> if this kind of node is compatible;
	 *         <CODE>false</CODE> otherwise
	 */
	public boolean isCompatibleWith(TMNode node) {
		if (node instanceof TMFileNode) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the filling of the node. The nodeAdapter should return an
	 * instance of TMFileNode.
	 * 
	 * @param nodeAdapter
	 *            we compute the filling of this node; should return an instance
	 *            of TMFileNode
	 * @return the filling of the node
	 * @throws TMExceptionBadTMNodeKind
	 *             If the node does not return an instance of TMFileNode
	 */
	public Paint getFilling(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (node instanceof TMFileNode) {
			TMFileNode fNode = (TMFileNode) node;
			long time = fNode.getDate();
			long diff = (new Date()).getTime() - time;
			if (diff <= 3600000L) { // less than an hour
				nodeAdapter.setUserData("Less than an hour");
				return Color.white;
			} else if (diff <= 86400000L) { // less than a day
				nodeAdapter.setUserData("Less than a day");
				return Color.green;
			} else if (diff <= 604800000L) { // less than a week
				nodeAdapter.setUserData("Less than a week");
				return Color.yellow;
			} else if (diff <= 2592000000L) { // less than a month
				nodeAdapter.setUserData("Less than a month");
				return Color.orange;
			} else if (diff <= 31536000000L) { // less than a year
				nodeAdapter.setUserData("Less than a year");
				return Color.red;
			} else { // more than a year
				nodeAdapter.setUserData("More than a year");
				return Color.blue;
			}
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

	/**
	 * Returns the tooltip of the node. The nodeAdapter should return an
	 * instance of TMFileNode.
	 * 
	 * @param nodeAdapter
	 *            we compute the tooltip of this node; should return an instance
	 *            of TMFileNode
	 * @return the tooltip of the node
	 * @throws TMExceptionBadTMNodeKind
	 *             If the node does not return an instance of TMFileNode
	 */
	public String getTooltip(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (node instanceof TMFileNode) {
			TMFileNode fNode = (TMFileNode) node;

			String name = fNode.getName();

			float size = nodeAdapter.getSize();
			String state = (String) nodeAdapter.getUserData();

			long modTime = fNode.getDate();
			DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
			DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
			String date = df.format(new Date(modTime));
			String time = tf.format(new Date(modTime));

			String tooltip = "<html>" + name + "<p>" + date + " : " + time
					+ "<p>" + state + "<p>" + size + " octets";
			return tooltip;
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

	/**
	 * Returns the color of the title of the node.
	 * 
	 * @param nodeAdapter
	 *            the node for which we want the title
	 * @return the title of the node
	 * @throws TMExceptionBadTMNodeKind
	 *             if the kind of TMNode returned is incompatible with this
	 *             TMComputeDraw.
	 */
	public Paint getTitleColor(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (node instanceof TMFileNode) {
			return Color.black;
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

	/**
	 * Returns the title of the node.
	 * 
	 * @param nodeAdapter
	 *            the node for which we want the title
	 * @return the title of the node
	 * @throws TMExceptionBadTMNodeKind
	 *             if the kind of TMNode returned is incompatible with this
	 *             TMComputeDraw.
	 */
	public String getTitle(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (node instanceof TMFileNode) {
			TMFileNode fNode = (TMFileNode) node;

			return fNode.getName();
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

}
