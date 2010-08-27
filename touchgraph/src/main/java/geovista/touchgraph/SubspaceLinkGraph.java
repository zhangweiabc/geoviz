/*
 * Frank Hardisty
 */
package geovista.touchgraph;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

import geovista.common.cluster.MSTEdge;
import geovista.common.cluster.NDimensionalMST;
import geovista.common.data.DataSetForApps;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.DataSetEvent;
import geovista.common.event.IndicationEvent;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.readers.example.GeoData48States;
import geovista.symbolization.ColorInterpolator;
import geovista.touchgraph.graphelements.VisibleLocality;

/**
 * GLPanel contains code for adding scrollbars and interfaces to the TGPanel The
 * "GL" prefix indicates that this class is GraphLayout specific, and will
 * probably need to be rewritten for other applications.
 * 
 * @author Alexander Shapiro
 * 
 */
public class SubspaceLinkGraph extends LinkGraph implements ActionListener {
	JButton sendButton = new JButton("Send Selection");

	/**
	 * Default constructor.
	 */
	public SubspaceLinkGraph() {
		super();
		hideVC();

		JPanel modePanel = getModeSelectPanel();
		modePanel.add(sendButton);
		sendButton.addActionListener(this);
		tgPanel.setFireIndications(false);
		tgPanel.setFireSelections(false);
	}

	public void actionPerformed(ActionEvent e) {
		// button was pressed

		// let's be clever and reorder this by distance from the currently
		// selected variable
		VisibleLocality loc = (VisibleLocality) tgPanel.getGES();
		Edge[] eds = loc.getAllEdges();
		Arrays.sort(eds);
		Vector nodes = findNodeOrder(eds, tgPanel.getSelect());
		int[] sel = new int[nodes.size()];
		for (int i = 0; i < nodes.size(); i++) {
			Node no = (Node) nodes.get(i);
			String id = no.getID();
			Integer valInt = new Integer(id);
			int nodeNum = valInt.intValue();
			sel[i] = nodeNum;
		}
		fireSubspaceChanged(sel);
	}

	private Vector findNodeOrder(Edge[] eds, Node selNode) {
		Vector returnVec = new Vector();

		Vector sortedEds = new Vector(eds.length);

		for (Edge element : eds) {
			sortedEds.add(element);
		}

		returnVec.add(selNode);

		Vector nodesToFind = new Vector();
		nodesToFind.add(selNode);

		Node currNode = selNode;

		while (returnVec.size() < eds.length + 1) {// mst always has one more
			// node than edge

			Edge ed = findAndRemoveFirstEdge(sortedEds, currNode);

			if (ed == null) {
				nodesToFind.remove(currNode);
				currNode = (Node) nodesToFind.get(nodesToFind.size() - 1); // last
				// element
			} else { // found an edge containing that node

				Node nextNode = null;

				if (ed.getTo() == currNode) {
					nextNode = ed.getFrom();
				} else {
					nextNode = ed.getTo();
				}
				nodesToFind.add(nextNode);
				returnVec.add(nextNode);
				currNode = (Node) nodesToFind.get(nodesToFind.size() - 1); // last
				// element
			}
		}// loop

		return returnVec;
	}

	private Edge findAndRemoveFirstEdge(Vector v, Node n) {
		for (Enumeration e = v.elements(); e.hasMoreElements();) {
			Edge ed = (Edge) e.nextElement();
			Node to = ed.getTo();
			Node from = ed.getFrom();

			if ((n == to) || (n == from)) {
				v.remove(ed);

				return ed;
			}
		}

		return null;
	}

	public void processAllVariables() {

		for (Edge element : gvEdges) {
			tgPanel.deleteEdge(element);
		}

		NDimensionalMST nDMST = new NDimensionalMST();

		nDMST.addSubspaceData(data);

		// SpatialEdge[] mstEdges = nDMST.getMST();
		// gvEdges = new Edge[data.getNumObservations() - 1]; // has to be, for
		// MST
		MSTEdge[] mstEdges = nDMST.getMST();
		gvEdges = new Edge[mstEdges.length];

		for (int i = 0; i < mstEdges.length; i++) {
			Node from = gvNodes[mstEdges[i].getStart()];
			Node to = gvNodes[mstEdges[i].getEnd()];
			double length = mstEdges[i].getWeight();

			int dist = (int) (length * 1000d);

			if (dist < 10) {
				dist = 10; // otherwise component might blow up.
			}

			Edge e = new Edge(from, to, dist);
			e.setColor(Color.cyan);

			// Edge e = new Edge(from,to);
			gvEdges[i] = e;
			tgPanel.addEdgeToEltSet(e);

			// i = i + 10;
		}

		Node selNode = gvNodes[1];
		tgPanel.setLocale(selNode, localityScroll.getRadius());
		tgPanel.setSelect(selNode);
	}

	@Override
	public void processSelectedVariables() {
		// I can't think of any reason for subspace linkgraph to do this, so i'm
		// making this a no-op for now fah 17 may 03

		// what we should do here:
		// make the first variable selected
		// fire the set as a subspace
		int[] attList = plottedAttributes;

		super.setSelectedNode(attList[0]);
		fireSubspaceChanged(attList);
		/*
		 * Node nl = tgPanel.getSelect(); int selection = 0;
		 * 
		 * if (nl != null) { //if the selection is not null, remember it
		 * 
		 * String selectedID = nl.getID(); Integer selINT = new
		 * Integer(selectedID); selection = selINT.intValue(); }
		 * 
		 * for (int i = 0; i < gvEdges.length; i++) {
		 * tgPanel.deleteEdge(gvEdges[i]); }
		 * 
		 * NDimensionalMST nDMST = new NDimensionalMST();
		 * 
		 * nDMST.addData(data, plottedAttributes);
		 * 
		 * //SpatialEdge[] mstEdges = nDMST.getMST(); //gvEdges = new
		 * Edge[data.getNumObservations() - 1]; // has to be, for MST
		 * SpatialEdge[] mstEdges = nDMST.getMST(); gvEdges = new
		 * Edge[mstEdges.length];
		 * 
		 * for (int i = 0; i < mstEdges.length; i++) { Node from =
		 * gvNodes[mstEdges[i].getOrigin().getId()]; Node to =
		 * gvNodes[mstEdges[i].getDestination().getId()]; double length =
		 * mstEdges[i].getDistance();
		 * 
		 * int dist = (int) (length * 1000d);
		 * 
		 * if (dist < 10) { dist = 10; //otherwise component might blow up. }
		 * 
		 * Edge e = new Edge(from, to, dist);
		 * 
		 * //Edge e = new Edge(from,to); gvEdges[i] = e;
		 * tgPanel.addEdgeToEltSet(e);
		 * 
		 * //i = i + 10; }
		 * 
		 * Node selNode = gvNodes[selection]; tgPanel.setLocale(selNode,
		 * localityScroll.getRadius()); tgPanel.setSelect(selNode);
		 */
	}

	@Override
	public void colorArrayChanged(ColorArrayEvent e) {
		Color[] colors = e.getColors();
		if (colors == null) {
			return;
		}
		if (colors.length == 0) {
			return;
		}

		// this.setColors(colors);

	}

	// let's set colors
	@Override
	public void setColors(Color[] colors) {
		if (data == null) {
			return;
		}

		for (int i = 0; i < data.getNumObservations(); i++) {
			gvNodes[i].setBackColor(colors[i]);
			Node.setNodeBackDefaultColor(colors[i]);
			gvNodes[i].setNodeBackFixedColor(colors[i]);
		}

		// calling all edges
		Edge[] allEdges = tgPanel.getAllEdges();

		for (Edge e : allEdges) {
			Color colOne = e.getFrom().getBackColor();
			Color colTwo = e.getTo().getBackColor();

			e.setColor(ColorInterpolator.mixColorsRGB(colOne, colTwo));
		}

		this.repaint();
	}

	@Override
	public void dataSetChanged(DataSetEvent e) {

		setDataSet(e.getDataSetForApps());
	}

	// this method for graphing attributes
	@Override
	public void setDataSet(DataSetForApps data) {
		// first blast old graph
		tgPanel.clearAllFull();

		this.data = data;
		attributesDisplay = data.getAttributeNamesNumeric();
		attList = new JList(attributesDisplay);

		gvNodes = new Node[data.getNumberNumericAttributes()];

		for (int i = 0; i < data.getNumberNumericAttributes(); i++) {
			String id = String.valueOf(i);

			// Node n = new
			// Node(id,Node.TYPE_RECTANGLE,Color.white,obsNames[i]);
			Node n = new Node(id, attributesDisplay[i]);
			n.setBackColor(Color.cyan);
			gvNodes[i] = n;
		}

		try {
			for (int i = 0; i < data.getNumberNumericAttributes(); i++) {
				Node n = gvNodes[i];
				tgPanel.addNodeToEltSet(n);
				gvNodes[i] = n;
			}
		} catch (TGException tge) {
			System.err.println(tge.getMessage());
			tge.printStackTrace(System.err);
		}

		int numAtts = data.getNumberNumericAttributes();

		if (numAtts >= 3) {
			int[] plottedAtt = { 1, 2 };
			plottedAttributes = plottedAtt;
		} else if (numAtts == 2) {
			int[] plottedAtt = { 1, 2 };
			plottedAttributes = plottedAtt;
		} else if (numAtts == 1) {
			int[] plottedAtt = { 1 };
			plottedAttributes = plottedAtt;
		} else if (numAtts == 1) {
			plottedAttributes = new int[0];
		}

		gvEdges = new Edge[0];
		processAllVariables();

		// int[][] mst = nDMST.getMSTIndexes();
		// tgPanel.setLocale(tgPanel.getGES().getFirstNode(),localityScroll.getRadius());
		// tgPanel.setSelect(tgPanel.getGES().getFirstNode()); //Select first
		// node, so hiding works
	}

	@Override
	public void selectionChanged(SelectionEvent e) {
		// int[] sel = e.getSelection();
	}

	@Override
	public void indicationChanged(IndicationEvent e) {
		// noop
	}

	/**
	 * adds an SubspaceListener
	 */
	public void addSubspaceListener(SubspaceListener l) {
		listenerList.add(SubspaceListener.class, l);

	}

	/**
	 * removes an SubspaceListener from the component
	 */
	public void removeSubspaceListener(SubspaceListener l) {
		listenerList.remove(SubspaceListener.class, l);

	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireSubspaceChanged(int[] selection) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		SubspaceEvent e = null;

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == SubspaceListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new SubspaceEvent(this, selection);
				}

				((SubspaceListener) listeners[i + 1]).subspaceChanged(e);
			}
		}
		// next i
	}

	public static void main(String[] args) {
		JFrame app;
		app = new JFrame("Graph Layout");

		SubspaceLinkGraph glPanel = new SubspaceLinkGraph();
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		app.getContentPane().add("Center", glPanel);
		app.setSize(500, 500);
		app.setVisible(true);

		GeoData48States data = new GeoData48States();
		glPanel.setDataSet(data.getDataForApps());
	}
}
