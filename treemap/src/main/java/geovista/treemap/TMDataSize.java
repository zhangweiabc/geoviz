/*
 * TMDataSize.java
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

import geovista.treemap.tm.TMComputeSize;
import geovista.treemap.tm.TMExceptionBadTMNodeKind;
import geovista.treemap.tm.TMNode;

/**
 * The TMDataSize class implements an example of a TMComputeSize for a
 * TMDataNode.
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
public class TMDataSize implements TMComputeSize {

	/**
	 * Test if this TMComputeSize could be used with the kind of TMNode passed
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
	 * Returns the size of the node. The node should be an instance of
	 * TMDataNode. Returns <CODE>0</CODE> for a folder, and the size of the
	 * file, in byte, for a file.
	 * 
	 * @param node
	 *            we compute the size of this node; should be an instance of
	 *            TMDataNode
	 * @return the size of the node; <CODE>0</CODE> for a folder; the size of
	 *         the file in byte for a file
	 * @throws TMExceptionBadTMNodeKind
	 *             If the node is not an instance of TMDataNode
	 */
	public float getSize(TMNode node) throws TMExceptionBadTMNodeKind {

		if (node instanceof TMDataNode) {
			TMDataNode fNode = (TMDataNode) node;
			return (float) fNode.getValue();
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

}
