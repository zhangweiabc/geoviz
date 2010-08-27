/*
 * TMDataDraw.java
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

package geovista.treemap;

import java.awt.Color;
import java.awt.Paint;

import geovista.treemap.tm.TMComputeDraw;
import geovista.treemap.tm.TMExceptionBadTMNodeKind;
import geovista.treemap.tm.TMNode;
import geovista.treemap.tm.TMNodeAdapter;

/**
 * The TMDataDraw class implements an example of a TMComputeDraw for a
 * TMDataNode. It uses the color property of each node as its color.
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
public class TMDataDraw implements TMComputeDraw {

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
		if (node instanceof TMDataNode) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the filling of the node. The nodeAdapter should return an
	 * instance of TMDataNode.
	 * 
	 * @param nodeAdapter
	 *            we compute the filling of this node; should return an instance
	 *            of TMDataNode
	 * @return the filling of the node
	 * @throws TMExceptionBadTMNodeKind
	 *             If the node does not return an instance of TMDataNode
	 */
	public Paint getFilling(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (node instanceof TMDataNode) {
			TMDataNode fNode = (TMDataNode) node;
			return fNode.getColor();
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

	/**
	 * Returns the tooltip of the node. The nodeAdapter should return an
	 * instance of TMDataNode.
	 * 
	 * @param nodeAdapter
	 *            we compute the tooltip of this node; should return an instance
	 *            of TMDataNode
	 * @return the tooltip of the node
	 * @throws TMExceptionBadTMNodeKind
	 *             If the node does not return an instance of TMDataNode
	 */
	public String getTooltip(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (node instanceof TMDataNode) {
			TMDataNode fNode = (TMDataNode) node;

			String name = fNode.getName();

			double value = fNode.getValue();

			String tooltip = "<html>" + name + "<p>" + value;
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
		if (node instanceof TMDataNode) {
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
		if (node instanceof TMDataNode) {
			TMDataNode fNode = (TMDataNode) node;

			return fNode.getName();
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

}
