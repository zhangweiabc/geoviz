/*
 * TMNodeModel.java
 * www.bouthier.net
 *
 * The MIT License :
 * -----------------
 * Copyright (c) 2001 Christophe Bouthier, Vesselin Markovsky
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

import java.awt.Paint;
import java.awt.Rectangle;

/**
 * The TMNodeModel class implements encapsulation of a TMNode for the TMView.
 * Its responsability is to keep the size and the drawing area and filling of a
 * TMNode. It implements the Composite design pattern.
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * @author Vesselin Markovsky [markovsky@semantec.bg]
 * 
 */
class TMNodeModel extends TMNodeAdapter {

	protected TMNode node = null; // encapsulated node
	protected TMNodeModelRoot modelRoot = null; // root of the model

	protected TMNodeModelComposite parent = null; // parent

	protected Rectangle area = null; // drawing area

	protected boolean dirtyS = true; // size should be computed
	protected float size = 0L; // size of the node
	protected boolean dirtyBufS = true; // size buffer is dirty
	protected float bufSize = 0L; // size buffer

	private boolean dirtyD = true; // fill should be computed
	private Paint filling = null; // filling of this node
	private String tooltip = null; // tooltip of this node
	private boolean dirtyBufF = true; // filling buffer is dirty
	private Paint bufFill = null; // filling buffer

	private boolean dirtyBufT = true; // tooltip buffer is dirty
	private String bufTip = null; // tooltip buffer

	private String title = null; // title of the node
	private boolean dirtyBufTitle = true; // title buff dirty
	private String bufTitle = null; // title buffer

	private boolean dirtyBufCT = true; // color buff dirty
	private Paint colorTitle = null; // color of the title
	private Paint bufColorTitle = null; // color title buffer

	/* --- Constructor --- */

	/**
	 * Constructor.
	 * 
	 * @param root
	 *            the root of the TMNode tree
	 * @param modelRoot
	 *            the root of the model
	 */
	TMNodeModel(TMNode root, TMNodeModelRoot modelRoot) {
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
	TMNodeModel(TMNode node, TMNodeModelComposite parent,
			TMNodeModelRoot modelRoot) {
		this.node = node;
		this.modelRoot = modelRoot;

		this.parent = parent;

		area = new Rectangle();
		modelRoot.incrementNumberOfNodes();
		modelRoot.incrementNumberOfDirtySNodes();
		modelRoot.incrementNumberOfDirtyDNodes();
	}

	/* --- Tree management --- */

	/**
	 * Returns the parent of this node.
	 * 
	 * @return the parent of this node
	 */
	TMNodeModelComposite getParent() {
		return parent;
	}

	/**
	 * Returns <CODE>true</CODE> if this node is not an instance of
	 * TMNodeModelComposite.
	 * 
	 * @return <CODE>true</CODE>
	 */
	boolean isLeaf() {
		return true;
	}

	/* --- Accessor --- */

	/**
	 * Returns the TMNode encapsulated.
	 * 
	 * @return the TMNode encapsulated
	 */
	@Override
	public TMNode getNode() {
		return node;
	}

	/**
	 * Returns the size of this node.
	 * 
	 * @return the size of this node
	 */
	@Override
	public float getSize() {
		return bufSize;
	}

	/**
	 * Returns the area representing this node in the view.
	 * 
	 * @return the Rectangle representing the area
	 */
	Rectangle getArea() {
		return area;
	}

	/**
	 * Returns the filling of this node.
	 * 
	 * @return the filling of this node
	 */
	Paint getFilling() {
		return bufFill;
	}

	/**
	 * Returns the tooltip of this node.
	 * 
	 * @return the tooltip of this node
	 */
	String getTooltip() {
		return bufTip;
	}

	/**
	 * Returns the title of the node.
	 * 
	 * @return the title of the node
	 */
	public String getTitle() {
		return bufTitle;
	}

	/**
	 * Returns the color of the title of this node.
	 * 
	 * @return the color of the title of this node
	 */
	public Paint getColorTitle() {
		return bufColorTitle;
	}

	/* --- Cushion Data --- */

	public void setCushionData(TMCushionData data) {
		cushionData = data;
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
	TMNodeModel nodeContaining(int x, int y) {
		if (area.contains(x, y)) {
			return this;
		}
		return null;
	}

	/**
	 * Returns the most inner TMNodeModel which contains the given TMNode.
	 * 
	 * @param node
	 *            the TMNode
	 * @return the TMNodeModel containing this TMNode; <CODE>null</CODE> if
	 *         there is no such TMNodeModel
	 */
	TMNodeModel nodeContaining(TMNode node) {
		if (this.node == node) {
			return this;
		}
		return null;
	}

	/* --- Computing --- */

	/**
	 * Compute the size of the node.
	 * 
	 * @return the size of the node
	 */
	float computeSize() {
		if (dirtyS) {
			size = modelRoot.getCSize().getSize(node);
			dirtyBufS = true;
			modelRoot.decrementNumberOfDirtySNodes();
			dirtyS = false;
		}
		return size;
	}

	/**
	 * Compute the filling and the tooltip of the node.
	 */
	void computeDrawing() {
		if (dirtyD) {
			filling = modelRoot.getCDraw().getFilling(this);
			dirtyBufF = true;
			tooltip = modelRoot.getCDraw().getTooltip(this);
			dirtyBufT = true;
			title = modelRoot.getCDraw().getTitle(this);
			dirtyBufTitle = true;
			colorTitle = modelRoot.getCDraw().getTitleColor(this);
			dirtyBufCT = true;
			modelRoot.decrementNumberOfDirtyDNodes();
			dirtyD = false;
		}
	}

	/**
	 * Clear dirty buffers.
	 */
	void clearBuffers() {
		if (dirtyBufS) {
			bufSize = size;
			dirtyBufS = false;
		}
		if (dirtyBufF) {
			bufFill = filling;
			dirtyBufF = false;
		}
		if (dirtyBufT) {
			bufTip = tooltip;
			dirtyBufT = false;
		}
		if (dirtyBufTitle) {
			bufTitle = title;
			dirtyBufTitle = false;
		}
		if (dirtyBufCT) {
			bufColorTitle = colorTitle;
			dirtyBufCT = false;
		}
	}

	/* --- Updates --- */

	/**
	 * Updates the size of this node. As the size of the parents depends of the
	 * size of the child, marks this node and its parents as dirty size nodes.
	 * As the drawing of a node could be dependent of its size, marks this node
	 * and its parents as dirty drawing nodes.
	 */
	void updateSize() {
		setMeAndMyParentsAsDirty();
	}

	/**
	 * Updates the drawing of this node.
	 */
	void updateDrawing() {
		filling = modelRoot.getCDraw().getFilling(this);
		dirtyBufF = true;
		tooltip = modelRoot.getCDraw().getTooltip(this);
		dirtyBufT = true;
		title = modelRoot.getCDraw().getTitle(this);
		dirtyBufTitle = true;
		colorTitle = modelRoot.getCDraw().getTitleColor(this);
		dirtyBufCT = true;
	}

	/**
	 * Sets the dirty size and dirty drawing flags on this TMNodeModel and on
	 * its parents.
	 */
	protected void setMeAndMyParentsAsDirty() {
		dirtyS = true;
		modelRoot.incrementNumberOfDirtySNodes();
		dirtyD = true;
		modelRoot.incrementNumberOfDirtyDNodes();
		if (parent != null) {
			parent.setMeAndMyParentsAsDirty();
		}
	}

	/**
	 * Flush the dirtyD flag for this node.
	 */
	void flushDraw() {
		dirtyD = true;
		modelRoot.incrementNumberOfDirtyDNodes();
	}

	/**
	 * Flush the dirtyS and dirtyD flags for this node.
	 */
	void flushAll() {
		dirtyS = true;
		dirtyD = true;
		modelRoot.incrementNumberOfDirtySNodes();
		modelRoot.incrementNumberOfDirtyDNodes();
	}

}
