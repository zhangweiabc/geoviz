/*
 * TMComputeDrawAdapter.java
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

import java.awt.Paint;

/**
 * The TMComputeDrawAdapter class implements a adapter for the TMComputeDraw
 * interface for users of the TMModelNode interface.
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
public abstract class TMComputeDrawAdapter implements TMComputeDraw {

	/**
	 * DO NOT OVERLOAD.
	 */
	public final boolean isCompatibleWith(TMNode node) {
		if (node instanceof TMNodeEncapsulator) {
			TMNodeEncapsulator n = (TMNodeEncapsulator) node;
			return isCompatibleWithObject(n.getNode());
		}
		return false;
	}

	/**
	 * DO NOT OVERLOAD.
	 */
	public final Paint getFilling(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();

		if (isCompatibleWith(node)) {
			TMNodeEncapsulator n = (TMNodeEncapsulator) node;
			return getFillingOfObject(n.getNode());
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

	/**
	 * DO NOT OVERLOAD.
	 */
	public final String getTooltip(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (isCompatibleWith(node)) {
			TMNodeEncapsulator n = (TMNodeEncapsulator) node;
			return getTooltipOfObject(n.getNode());
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

	/**
	 * DO NOT OVERLOAD.
	 */
	public final String getTitle(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (isCompatibleWith(node)) {
			TMNodeEncapsulator n = (TMNodeEncapsulator) node;
			return getTitleOfObject(n.getNode());
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

	/**
	 * DO NOT OVERLOAD.
	 */
	public final Paint getTitleColor(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (isCompatibleWith(node)) {
			TMNodeEncapsulator n = (TMNodeEncapsulator) node;
			return getColorTitleOfObject(n.getNode());
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

	/**
	 * TO BE IMPLEMENTED.
	 */
	public abstract boolean isCompatibleWithObject(Object node);

	/**
	 * TO BE IMPLEMENTED.
	 */
	public abstract Paint getFillingOfObject(Object node);

	/**
	 * TO BE IMPLEMENTED.
	 */
	public abstract String getTooltipOfObject(Object node);

	/**
	 * TO BE IMPLEMENTED.
	 */
	public abstract String getTitleOfObject(Object node);

	/**
	 * TO BE IMPLEMENTED.
	 */
	public abstract Paint getColorTitleOfObject(Object node);
}
