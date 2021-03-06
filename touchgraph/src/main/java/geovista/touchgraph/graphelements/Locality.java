/*
 * TouchGraph LLC. Apache-Style Software License
 *
 *
 * Copyright (c) 2002 Alexander Shapiro. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        TouchGraph LLC (http://www.touchgraph.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "TouchGraph" or "TouchGraph LLC" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission.  For written permission, please contact
 *    alex@touchgraph.com
 *
 * 5. Products derived from this software may not be called "TouchGraph",
 *    nor may "TouchGraph" appear in their name, without prior written
 *    permission of alex@touchgraph.com.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL TOUCHGRAPH OR ITS CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 */

package geovista.touchgraph.graphelements;

import java.util.Vector;

import geovista.touchgraph.Edge;
import geovista.touchgraph.Node;
import geovista.touchgraph.TGException;

/**
 * Locality: A way of representing a subset of a larger set of nodes. Allows for
 * both manipulation of the subset, and manipulation of the larger set. For
 * instance, one can call removeNode to delete it from the subset, or deleteNode
 * to remove it from the larger set.
 * 
 * Locality is used in conjunction with LocalityUtils, which handle locality
 * shift animations.
 * 
 * More synchronization will almost definitely be required.
 * 
 * @author Alexander Shapiro
 * 
 */
public class Locality extends GraphEltSet {

	protected transient GraphEltSet completeEltSet;

	// ............

	/**
	 * Constructor with GraphEltSet <tt>ges</tt>.
	 */
	public Locality(GraphEltSet ges) {
		super();
		completeEltSet = ges;
	}

	public GraphEltSet getCompleteEltSet() {
		return completeEltSet;
	}

	@Override
	public void addNode(Node n) throws TGException {
		super.addNode(n);
		// If a new node is added to locality, add it to the complete elt set as
		// well
		if (!completeEltSet.contains(n)) {
			completeEltSet.addNode(n);
		}
	}

	@Override
	public void addEdge(Edge e) {
		if (!contains(e)) {
			edges.addElement(e);
			e.from.localEdgeCount++;
			e.to.localEdgeCount++;
		}
		// If a new node edge is added to locality, add it to the complete elt
		// set as well
		if (!completeEltSet.contains(e)) {
			completeEltSet.addEdge(e);
		}
	}

	public void addNodeWithEdges(Node n) throws TGException {
		addNode(n);
		for (int i = 0; i < n.edgeNum(); i++) {
			Edge e = n.edgeAt(i);
			if (contains(e.getOtherEndpt(n))) {
				addEdge(e);
			}
		}

	}

	@Override
	public Edge findEdge(Node from, Node to) {
		Edge foundEdge = super.findEdge(from, to);
		if (foundEdge != null && edges.contains(foundEdge)) {
			return foundEdge;
		}
		return null;
	}

	@Override
	public boolean deleteEdge(Edge e) {
		if (e == null) {
			return false;
		}
		removeEdge(e);
		return completeEltSet.deleteEdge(e);
	}

	@Override
	public synchronized void deleteEdges(Vector edgesToDelete) {
		removeEdges(edgesToDelete);
		completeEltSet.deleteEdges(edgesToDelete);
	}

	public boolean removeEdge(Edge e) {
		if (e == null) {
			return false;
		}
		if (edges.removeElement(e)) {
			e.from.localEdgeCount--;
			e.to.localEdgeCount--;
			return true;
		}
		return false;
	}

	public synchronized void removeEdges(Vector edgesToRemove) {
		for (int i = 0; i < edgesToRemove.size(); i++) {
			removeEdge((Edge) edgesToRemove.elementAt(i));
		}
	}

	@Override
	public boolean deleteNode(Node node) {
		if (node == null) {
			return false;
		}
		removeNode(node);
		return completeEltSet.deleteNode(node);
	}

	@Override
	public synchronized void deleteNodes(Vector nodesToDelete) {
		removeNodes(nodesToDelete);
		completeEltSet.deleteNodes(nodesToDelete);
	}

	public boolean removeNode(Node node) {
		if (node == null) {
			return false;
		}
		if (!nodes.removeElement(node)) {
			return false;
		}

		String id = node.getID();
		if (id != null) {
			nodeIDRegistry.remove(id); // remove from registry
		}

		for (int i = 0; i < node.edgeNum(); i++) {
			removeEdge(node.edgeAt(i));
		}

		return true;
	}

	public synchronized void removeNodes(Vector nodesToRemove) {
		for (int i = 0; i < nodesToRemove.size(); i++) {
			removeNode((Node) nodesToRemove.elementAt(i));
		}
	}

	public synchronized void removeAll() {
		super.clearAll();
	}

	@Override
	public synchronized void clearAll() {
		removeAll();
		completeEltSet.clearAll();
	}

} // end com.touchgraph.graphlayout.graphelements.Locality
