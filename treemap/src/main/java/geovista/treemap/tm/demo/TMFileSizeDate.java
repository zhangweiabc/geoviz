/*
 * TMFileSizeDate.java
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

import java.util.Date;

import geovista.treemap.tm.TMComputeSize;
import geovista.treemap.tm.TMExceptionBadTMNodeKind;
import geovista.treemap.tm.TMNode;

/**
 * The TMFileSizeDate class implements an example of a TMComputeSize for a
 * TMFileNode. It use the date of the file as a size.
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
public class TMFileSizeDate implements TMComputeSize {

	private long myDate = 0L; // the instant date

	/**
	 * Constructor.
	 */
	public TMFileSizeDate() {
		Date date = new Date();
		myDate = date.getTime();
	}

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
		if (node instanceof TMFileNode) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the size of the node. The node should be an instance of
	 * TMFileNode. Returns <CODE>0</CODE> for a folder, and the date of the
	 * file, for a file.
	 * 
	 * @param node
	 *            we compute the size of this node; should be an instance of
	 *            TMFileNode
	 * @return the size of the node; <CODE>0</CODE> for a folder; the date of
	 *         the file for a file
	 * @throws TMExceptionBadTMNodeKind
	 *             If the node is not an instance of TMFileNode
	 */
	public float getSize(TMNode node) throws TMExceptionBadTMNodeKind {

		if (node instanceof TMFileNode) {
			TMFileNode fNode = (TMFileNode) node;
			return (myDate - fNode.getDate());
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

}
