/*
 * TMNodeModelComposite.java
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

package geovista.treemap.tm;

import java.util.Enumeration;
import java.util.Vector;

/**
 * The TMNodeModelComposite implements the Composite design pattern for
 * TMNodeModel. It represent a TMNodeModel which is not a leaf.
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
class TMNodeModelComposite extends TMNodeModel {

	private Vector children = null; // children of this node
	private boolean dirtyBufC = true; // the buffered children is dirty
	private Vector bufChild = null; // children buffer

	/* --- Constructor --- */

	/**
	 * Constructor.
	 * 
	 * @param root
	 *            the root of the TMNode tree
	 * @param modelRoot
	 *            the root of the model
	 */
	TMNodeModelComposite(TMNode root, TMNodeModelRoot modelRoot) {
		this(root, null, modelRoot);
	}

	/**
	 * Constructor.
	 * 
	 * @param node
	 *            the TMNode encapsulated
	 * @param parent
	 *            the parent of this node
	 * @param modelRoot
	 *            the root of the model
	 */
	TMNodeModelComposite(TMNode node, TMNodeModelComposite parent,
			TMNodeModelRoot modelRoot) {
		super(node, parent, modelRoot);
		children = new Vector();

		TMNode childNode = null;
		TMNodeModel child = null;
		for (Enumeration e = node.children(); e.hasMoreElements();) {
			// should test here that e is not null
			// should test here that we really have a TMNode object
			childNode = (TMNode) e.nextElement();
			if (childNode.isLeaf()) {
				child = new TMNodeModel(childNode, this, modelRoot);
			} else {
				child = new TMNodeModelComposite(childNode, this, modelRoot);
			}
			addChild(child);
		}
	}

	/* --- Tree management --- */

	/**
	 * Returns the children of this node, in an Enumeration.
	 * 
	 * @return the children of this node
	 */
	Enumeration children() {
		return bufChild.elements();
	}

	/**
	 * Returns the non-buffered children of this node, in an Enumeration. Could
	 * only be called in a TMThreadQueue thread.
	 * 
	 * @return the non-buffered children of this node
	 */
	private Enumeration trueChildren() {
		return children.elements();
	}

	/**
	 * Adds the TMNodeModel as a children.
	 * 
	 * @param child
	 *            the child
	 */
	private void addChild(TMNodeModel child) {
		children.addElement(child);
		dirtyBufC = true;
	}

	/**
	 * Removes the TMNodeModel as a children.
	 * 
	 * @param child
	 *            the child
	 */
	private void removeChild(TMNodeModel child) {
		children.removeElement(child);
		dirtyBufC = true;
	}

	/**
	 * Returns <CODE>false</CODE> as this node is an instance of
	 * TMNodeModelComposite.
	 * 
	 * @return <CODE>false</CODE>
	 */
	@Override
	boolean isLeaf() {
		return false;
	}

	/* --- Finding node --- */

	/**
	 * Returns the most inner TMNodeModel which contains in its drawing area the
	 * given coordonates.
	 * 
	 * @param x
	 *            the X coordonate
	 * @param y
	 *            the Y coordonate
	 * @return the TMNodeModel containing thoses coordonates; <CODE>null</CODE>
	 *         if there is no such TMNodeModel
	 */
	@Override
	TMNodeModel nodeContaining(int x, int y) {
		if (area.contains(x, y)) {
			for (Enumeration e = children(); e.hasMoreElements();) {
				TMNodeModel neo = ((TMNodeModel) e.nextElement())
						.nodeContaining(x, y);
				if (neo != null) {
					return neo;
				}
			}
			return this;
		}
		return null;
	}

	/**
	 * Returns the most inner TMNodeModel which contains the given TMNode. As
	 * this method works on non-buffered children, it should be called only
	 * within a TMThreadQueue thread.
	 * 
	 * @param node
	 *            the TMNode
	 * @return the TMNodeModel containing this TMNode; <CODE>null</CODE> if
	 *         there is no such TMNodeModel
	 */
	@Override
	TMNodeModel nodeContaining(TMNode node) {
		if (this.node == node) {
			return this;
		}
		for (Enumeration e = trueChildren(); e.hasMoreElements();) {
			TMNodeModel neo = ((TMNodeModel) e.nextElement())
					.nodeContaining(node);
			if (neo != null) {
				return neo;
			}
		}
		return null;
	}

	/* --- Computing --- */

	/**
	 * Compute the size of the node.
	 * 
	 * @return the size of the node
	 */
	@Override
	float computeSize() {
		if (dirtyS) {
			size = 0.0f;
			TMNodeModel child = null;
			for (Enumeration e = trueChildren(); e.hasMoreElements();) {
				child = (TMNodeModel) e.nextElement();
				size += child.computeSize();
			}
			dirtyBufS = true;
			modelRoot.decrementNumberOfDirtySNodes();
			dirtyS = false;
		}
		return size;
	}

	/**
	 * Compute the filling and the tooltip of the node.
	 */
	@Override
	void computeDrawing() {
		super.computeDrawing();
		TMNodeModel child = null;
		for (Enumeration e = trueChildren(); e.hasMoreElements();) {
			child = (TMNodeModel) e.nextElement();
			child.computeDrawing();
		}
	}

	/**
	 * Clear dirty buffers.
	 */
	@Override
	void clearBuffers() {
		if (dirtyBufC) {
			bufChild = (Vector) children.clone();
			dirtyBufC = false;
		}
		super.clearBuffers();
		TMNodeModel child = null;
		for (Enumeration e = trueChildren(); e.hasMoreElements();) {
			child = (TMNodeModel) e.nextElement();
			child.clearBuffers();
		}
	}

	/* --- Updates --- */

	/**
	 * Adds the given TMNodeModel as a child of this node and updates the size
	 * and the drawing of the parents.
	 * 
	 * @param child
	 *            the new child
	 */
	void newChild(TMNodeModel child) {
		addChild(child);
		setMeAndMyParentsAsDirty();
	}

	/**
	 * Removes a child of this node and updates the size and the drawing of the
	 * parents.
	 * 
	 * @param child
	 *            the lost child
	 */
	void lostChild(TMNodeModel child) {
		removeChild(child);
		setMeAndMyParentsAsDirty();
	}

	/**
	 * Flush the dirtyD flag for this node and its children.
	 */
	@Override
	void flushDraw() {
		super.flushDraw();
		TMNodeModel child = null;
		for (Enumeration e = trueChildren(); e.hasMoreElements();) {
			child = (TMNodeModel) e.nextElement();
			child.flushDraw();
		}
	}

	/**
	 * Flush the dirtyS and dirtyD flags for this node and its children.
	 */
	@Override
	void flushAll() {
		super.flushAll();
		TMNodeModel child = null;
		for (Enumeration e = trueChildren(); e.hasMoreElements();) {
			child = (TMNodeModel) e.nextElement();
			child.flushAll();
		}
	}

}
