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
package geovista.touchgraph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.common.classification.ClassifierPicker;
import geovista.common.cluster.MSTEdge;
import geovista.common.cluster.NDimensionalMST;
import geovista.common.data.DataSetForApps;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.ColorArrayListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.PaletteEvent;
import geovista.common.event.PaletteListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.geoviz.visclass.VisualClassifier;
import geovista.readers.example.GeoData48States;
import geovista.symbolization.ColorInterpolator;
import geovista.touchgraph.interaction.GLEditUI;
import geovista.touchgraph.interaction.GLNavigateUI;
import geovista.touchgraph.interaction.HVScroll;
import geovista.touchgraph.interaction.LocalityScroll;
import geovista.touchgraph.interaction.RotateScroll;
import geovista.touchgraph.interaction.TGUIManager;
import geovista.touchgraph.interaction.ZoomScroll;

/**
 * GLPanel contains code for adding scrollbars and interfaces to the TGPanel The
 * "GL" prefix indicates that this class is GraphLayout specific, and will
 * probably need to be rewritten for other applications.
 * 
 * @author Alexander Shapiro
 * 
 */
public class LinkGraph extends JPanel implements DataSetListener,
		ListSelectionListener, IndicationListener, SelectionListener,
		ColorArrayListener, NavigableLinkGraph, SubspaceListener,
		PaletteListener, TableModelListener {
	protected final static Logger logger = Logger.getLogger(LinkGraph.class
			.getName());
	public String zoomLabel = "Zoom"; // label for zoom menu item
	public String rotateLabel = "Rotate"; // label for rotate menu item
	public String localityLabel = "Locality"; // label for locality menu item
	transient public HVScroll hvScroll;
	transient public ZoomScroll zoomScroll;

	// public HyperScroll hyperScroll;
	transient public RotateScroll rotateScroll;
	transient public LocalityScroll localityScroll;
	public transient JPopupMenu glPopup;
	public Hashtable scrollBarHash; // = new Hashtable();
	protected TGPanel tgPanel;
	protected TGLensSet tgLensSet;
	protected transient TGUIManager tgUIManager;
	private Color defaultColor = Color.lightGray;

	// these next are for interactively setting the variables to plot
	protected transient JList attList;
	protected transient int[] plottedAttributes;
	private final JFrame dummyFrame = new JFrame();
	private transient JDialog dialog = null;
	private transient JScrollPane dialogPane = null;

	protected transient Node[] gvNodes;
	protected transient Edge[] gvEdges;
	protected transient DataSetForApps data;
	protected transient String[] attributesDisplay;
	transient private VisualClassifier vc;
	protected transient JPanel modeSelectPanel;
	private SelectionEvent savedSelectionEvent;

	// ............

	/**
	 * Default constructor.
	 */
	public LinkGraph() {
		scrollBarHash = new Hashtable();
		tgLensSet = new TGLensSet();
		tgPanel = new TGPanel();
		hvScroll = new HVScroll(tgPanel, tgLensSet);
		zoomScroll = new ZoomScroll(tgPanel);

		// hyperScroll = new HyperScroll(tgPanel);
		rotateScroll = new RotateScroll(tgPanel);
		localityScroll = new LocalityScroll(tgPanel);
		initialize();
		tgPanel.setBackColor(Color.black);
	}

	/**
	 * Constructor with a Color to be used for UI background.
	 */
	public LinkGraph(Color color) {
		defaultColor = color;
		setBackground(color);
		scrollBarHash = new Hashtable();
		tgLensSet = new TGLensSet();
		tgPanel = new TGPanel();
		tgPanel.setBackground(color);
		hvScroll = new HVScroll(tgPanel, tgLensSet);

		// hvScroll.getHorizontalSB().setBackground(Color.orange);
		// hvScroll.getVerticalSB().setBackground(Color.cyan);
		zoomScroll = new ZoomScroll(tgPanel);

		// zoomScroll.getZoomSB().setBackground(Color.green);
		// hyperScroll = new HyperScroll(tgPanel);
		rotateScroll = new RotateScroll(tgPanel);

		// rotateScroll.getRotateSB().setBackground(Color.blue);
		localityScroll = new LocalityScroll(tgPanel);

		// localityScroll.getLocalitySB().setBackground(Color.red);
		initialize();
	}

	/**
	 * Initialize panel, lens, and establish a random graph as a demonstration.
	 */
	public void initialize() {
		tgPanel.setFireIndications(true);
		tgPanel.setFireSelections(true);
		buildPanel();
		buildLens();
		tgPanel.setLensSet(tgLensSet);
		addUIs();
		vc = new VisualClassifier();
		vc.setPreferredSize(new Dimension(400, 20));
		this.add(vc, BorderLayout.SOUTH);
		vc.addColorArrayListener(this);
		vc
				.setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
		// tgPanel.addNode(); //Add a starting node.

		/*
		 * try { randomGraph(); } catch ( TGException tge ) {
		 * System.err.println(tge.getMessage());
		 * tge.printStackTrace(System.err); }
		 * tgPanel.setSelect(tgPanel.getGES().getFirstNode()); //Select first
		 * node, so hiding works
		 */
		setVisible(true);
	}

	public TGPanel getTGPanel() {
		return tgPanel;
	}

	public LocalityScroll getLocalityScroll() {
		return localityScroll;
	}

	public RotateScroll getRotateScroll() {
		return rotateScroll;
	}

	public HVScroll getHVScroll() {
		return hvScroll;
	}

	public JPopupMenu getGlPopup() {
		return glPopup;
	}

	public void buildLens() {
		tgLensSet.addLens(hvScroll.getLens());
		tgLensSet.addLens(zoomScroll.getLens());

		// tgLensSet.addLens(hyperScroll.getLens());
		tgLensSet.addLens(rotateScroll.getLens());
		tgLensSet.addLens(tgPanel.getAdjustOriginLens());
	}

	public void buildPanel() {
		final JScrollBar horizontalSB = hvScroll.getHorizontalSB();
		final JScrollBar verticalSB = hvScroll.getVerticalSB();
		final JScrollBar zoomSB = zoomScroll.getZoomSB();
		final JScrollBar rotateSB = rotateScroll.getRotateSB();
		final JScrollBar localitySB = localityScroll.getLocalitySB();

		setLayout(new BorderLayout());

		JPanel scrollPanel = new JPanel();
		scrollPanel.setBackground(defaultColor);
		scrollPanel.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		modeSelectPanel = new JPanel();
		modeSelectPanel.setBackground(defaultColor);
		modeSelectPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

		AbstractAction navigateAction = new AbstractAction("Navigate") {
			public void actionPerformed(ActionEvent e) {
				tgUIManager.activate("Navigate");
			}
		};

		AbstractAction editAction = new AbstractAction("Edit") {
			public void actionPerformed(ActionEvent e) {
				tgUIManager.activate("Edit");
			}
		};

		JButton configButton = new JButton("Variables");
		configButton.addActionListener(new java.awt.event.ActionListener() {
			/**
			 * Set up the attributes for plotted in SPM.
			 * 
			 * @param e
			 */
			public void actionPerformed(ActionEvent e) {
				try {
					configButton_actionPerformed(e);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		modeSelectPanel.add(configButton);

		JRadioButton rbNavigate = new JRadioButton(navigateAction);
		rbNavigate.setBackground(defaultColor);
		rbNavigate.setSelected(true);

		JRadioButton rbEdit = new JRadioButton(editAction);
		rbEdit.setBackground(defaultColor);

		ButtonGroup bg = new ButtonGroup();
		bg.add(rbNavigate);
		bg.add(rbEdit);

		// modeSelectPanel.add(rbNavigate);
		// modeSelectPanel.add(rbEdit);

		final JPanel topPanel = new JPanel();
		topPanel.setBackground(defaultColor);
		topPanel.setLayout(new GridBagLayout());
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;

		/*
		 * c.gridx=0;c.weightx=0; topPanel.add(new Label("Zoom",Label.RIGHT),
		 * c); c.gridx=1;c.weightx=0.5; topPanel.add(zoomSB,c);
		 * c.gridx=2;c.weightx=0; topPanel.add(new
		 * Label("Locality",Label.RIGHT), c); c.gridx=3;c.weightx=0.5;
		 * topPanel.add(localitySB,c);
		 */
		c.gridx = 0;
		c.weightx = 0;
		c.insets = new Insets(0, 10, 0, 10);
		topPanel.add(modeSelectPanel, c);
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 1;
		c.weightx = 1;

		scrollBarHash.put(zoomLabel, zoomSB);
		scrollBarHash.put(rotateLabel, rotateSB);
		scrollBarHash.put(localityLabel, localitySB);

		JPanel scrollselect = scrollSelectPanel(new String[] { zoomLabel,
				rotateLabel, localityLabel });
		scrollselect.setBackground(defaultColor);
		topPanel.add(scrollselect, c);

		add(topPanel, BorderLayout.NORTH);

		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		scrollPanel.add(tgPanel, c);

		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0;
		c.weighty = 0;
		scrollPanel.add(verticalSB, c);

		c.gridx = 0;
		c.gridy = 2;
		scrollPanel.add(horizontalSB, c);

		add(scrollPanel, BorderLayout.CENTER);

		glPopup = new JPopupMenu();
		glPopup.setBackground(defaultColor);

		JMenuItem menuItem = new JMenuItem("Toggle Controls");
		ActionListener toggleControlsAction = new ActionListener() {
			boolean controlsVisible = true;

			public void actionPerformed(ActionEvent e) {
				controlsVisible = !controlsVisible;
				horizontalSB.setVisible(controlsVisible);
				verticalSB.setVisible(controlsVisible);
				topPanel.setVisible(controlsVisible);
			}
		};

		menuItem.addActionListener(toggleControlsAction);
		glPopup.add(menuItem);
	}

	protected JPanel scrollSelectPanel(String[] scrollBarNames) {
		final JComboBox scrollCombo = new JComboBox(scrollBarNames);
		scrollCombo.setBackground(defaultColor);
		scrollCombo.setPreferredSize(new Dimension(80, 20));
		scrollCombo.setSelectedIndex(0);

		final JScrollBar initialSB = (JScrollBar) scrollBarHash
				.get(scrollBarNames[0]);
		scrollCombo.addActionListener(new ActionListener() {
			JScrollBar currentSB = initialSB;

			public void actionPerformed(ActionEvent e) {
				JScrollBar selectedSB = (JScrollBar) scrollBarHash
						.get(scrollCombo.getSelectedItem());

				if (currentSB != null) {
					currentSB.setVisible(false);
				}

				if (selectedSB != null) {
					selectedSB.setVisible(true);
				}

				currentSB = selectedSB;
			}
		});

		final JPanel sbp = new JPanel(new GridBagLayout());
		sbp.setBackground(defaultColor);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		sbp.add(scrollCombo, c);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.insets = new Insets(0, 10, 0, 17);
		c.fill = GridBagConstraints.HORIZONTAL;

		for (int i = 0; i < scrollBarNames.length; i++) {
			JScrollBar sb = (JScrollBar) scrollBarHash.get(scrollBarNames[i]);

			if (sb == null) {
				continue;
			}

			if (i != 0) {
				sb.setVisible(false);
			}

			// sb.setMinimumSize(new Dimension(200,17));
			sbp.add(sb, c);
		}

		return sbp;
	}

	/**
	 * Inform that value or selection in the JList (variable selection dialog)
	 * has been changed.
	 * 
	 * @param e
	 */
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			return;
		}

		JList theList = (JList) e.getSource();

		if (theList.isSelectionEmpty()) {
			return;
		}
		plottedAttributes = theList.getSelectedIndices();
	}

	/**
	 * Configure attributes for plotted in matrix.
	 * 
	 * @param e
	 */
	private void configButton_actionPerformed(ActionEvent e) {
		attSelectDialog(400, 400);
	}

	/**
	 * pop up variable selection dialog
	 * 
	 * @param x
	 * @param y
	 */
	private void attSelectDialog(int x, int y) {
		attList.setSelectedIndices(plottedAttributes);

		if (dialog == null) {
			dialog = new JDialog(dummyFrame, "Attributes for Plot", true);

			JButton selectButton;
			JButton closeButton;
			dialog.setSize(150, 300);
			dialog.getContentPane().setLayout(new BorderLayout());
			attList
					.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

			// attList.addListSelectionListener(this);
			// JScrollPane scrollPane = new JScrollPane(attList);
			dialogPane = new JScrollPane(attList);
			selectButton = new JButton("Select");
			selectButton.addActionListener(new java.awt.event.ActionListener() {
				/**
				 * put your documentation comment here
				 * 
				 * @param e
				 */
				public void actionPerformed(ActionEvent e) {
					logger.finest("about to press button Select");
					selectButton_actionPerformed(e);

					logger.finest("after pressed button Select");
				}
			});
			closeButton = new JButton("Close");
			closeButton.addActionListener(new java.awt.event.ActionListener() {
				/**
				 * put your documentation comment here
				 * 
				 * @param e
				 */
				public void actionPerformed(ActionEvent e) {
					closeButton_actionPerformed(e);
				}
			});

			JPanel buttonPanel = new JPanel(new BorderLayout());
			buttonPanel.add(selectButton, BorderLayout.WEST);
			buttonPanel.add(closeButton, BorderLayout.EAST);
			dialog.getContentPane().add(new JLabel("Attribute Names:"),
					BorderLayout.NORTH);
			dialog.getContentPane().add(dialogPane, BorderLayout.CENTER);
			dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		} else {
			dialogPane.setViewportView(attList);
		}

		attList.addListSelectionListener(this);
		plottedAttributes = attList.getSelectedIndices();
		dialog.setLocation(x, y);
		dialog.setVisible(true);
	}

	/**
	 * Selection button in variable selection dialog. Click on it, the variables
	 * selected in the JList will be forwarded to matrix.
	 * 
	 * @param e
	 */
	private void selectButton_actionPerformed(ActionEvent e) {
		logger.finest("select button pressed, in action performed");
		int plotNumber = plottedAttributes.length;
		logger.finest("select button pressed, plot number = " + plotNumber);
		// init();
		processSelectedVariables();
	}

	/**
	 * Close the variable selection dialog.
	 * 
	 * @param e
	 */
	private void closeButton_actionPerformed(ActionEvent e) {
		dialog.setVisible(false);
	}

	// I think this is for the subspace one only
	// public void processAllVariables() {
	// Node nl = tgPanel.getSelect();
	// int selection = 0;
	//
	// if (nl != null) { //if the selection is not null, remember it
	//
	// String selectedID = nl.getID();
	// Integer selINT = new Integer(selectedID);
	// selection = selINT.intValue();
	// }
	//
	// for (int i = 0; i < gvEdges.length; i++) {
	// tgPanel.deleteEdge(gvEdges[i]);
	// }
	//
	// NDimensionalMST nDMST = new NDimensionalMST();
	//
	// nDMST.addData(data);
	//
	// //SpatialEdge[] mstEdges = nDMST.getMST();
	// //gvEdges = new Edge[data.getNumObservations() - 1]; // has to be, for
	// MST
	//
	// SpatialEdge[] mstEdges= nDMST.getMST();
	// gvEdges = new Edge[mstEdges.length];
	//
	// for (int i = 0; i < mstEdges.length; i++) {
	// Node from = gvNodes[mstEdges[i].getOrigin().getId()];
	// Node to = gvNodes[mstEdges[i].getDestination().getId()];
	// double length = mstEdges[i].getDistance();
	//
	// int dist = (int) (length * 100000d);
	//
	// if (dist < 10) {
	// dist = 10; //otherwise component might blow up.
	// }

	// Edge e = new Edge(from, to, dist);
	//
	//
	// //Edge e = new Edge(from,to);
	// gvEdges[i] = e;
	// tgPanel.addEdgeToEltSet(e);
	//
	// //i = i + 10;
	// }
	//
	// Node selNode = gvNodes[1];
	// tgPanel.setLocale(selNode, localityScroll.getRadius());
	// tgPanel.setSelect(selNode);
	// }
	public void processSelectedVariables(int[] subspace) {
		if (data == null) {
			return;
		}
		plottedAttributes = subspace;
		Node nl = tgPanel.getSelect();
		int selection = 0;

		if (nl != null) { // if the selection is not null, remember it

			String selectedID = nl.getID();
			Integer selINT = new Integer(selectedID);
			selection = selINT.intValue();
		}

		for (Edge element : gvEdges) {
			tgPanel.deleteEdge(element);
		}

		NDimensionalMST nDMST = new NDimensionalMST();
		if (plottedAttributes == null) {
			int[] nums = { 1, 2, 3 };
			plottedAttributes = nums;
		}
		nDMST.addData(data, plottedAttributes);

		// SpatialEdge[] mstEdges = nDMST.getMST();
		// gvEdges = new Edge[data.getNumObservations() - 1]; // has to be, for
		// MST

		MSTEdge[] mstEdges = nDMST.getMST();

		gvEdges = new Edge[mstEdges.length];
		logger.finest("num edges = " + gvEdges.length);

		for (int i = 0; i < mstEdges.length; i++) {
			Node from = gvNodes[mstEdges[i].getStart()];
			Node to = gvNodes[mstEdges[i].getEnd()];
			double length = mstEdges[i].getWeight();

			int dist = (int) (length * 10000d);

			if (dist < 10) {
				dist = 10; // otherwise component might blow up.
			}

			Edge e = new Edge(from, to, dist);

			// Edge e = new Edge(from,to);
			gvEdges[i] = e;
			tgPanel.addEdgeToEltSet(e);
			// tgPanel.addEdge(e);

			// i = i + 10;
		}

		Node selNode = gvNodes[selection];
		tgPanel.setLocale(selNode, localityScroll.getRadius());
		forceShowEdges(selNode);

	}

	private void forceShowEdges(Node selNode) {
		for (int i = 0; i <= localityScroll.getRadius(); i++) {
			tgPanel.setLocale(selNode, i);
		}
	}

	public void processSelectedVariables() {
		this.processSelectedVariables(plottedAttributes);

	}

	public void addUIs() {
		tgUIManager = new TGUIManager();

		GLEditUI editUI = new GLEditUI(this);
		GLNavigateUI navigateUI = new GLNavigateUI(this);
		tgUIManager.addUI(editUI, "Edit");
		tgUIManager.addUI(navigateUI, "Navigate");
		tgUIManager.activate("Navigate");
	}

	public void indicationChanged(IndicationEvent e) {
		int indication = e.getIndication();

		if ((indication < 0) || (indication > tgPanel.getNodeCount())) {
			return;
		}

		String id = String.valueOf(indication);
		Node nl = tgPanel.findNode(id);
		tgPanel.setSelect(nl);
		tgPanel.repaint();
	}

	public void selectionChanged(SelectionEvent e) {
		int[] selection = e.getSelection();

		if (selection.length == 0) {
			return;
		}

		int indication = selection[0];
		setSelectedNode(indication);
		savedSelectionEvent = e;
	}

	public SelectionEvent getSelectionEvent() {
		SelectionEvent e = new SelectionEvent(this, savedSelectionEvent
				.getSelection());
		return e;
	}

	protected void setSelectedNode(int selNode) {
		if ((selNode >= 0) || (selNode < tgPanel.getNodeCount())) {
			String id = String.valueOf(selNode);
			Node nl = tgPanel.findNode(id);
			tgPanel.setLocale(nl, localityScroll.getRadius());
			tgPanel.setSelect(nl);
		}
	}

	public void subspaceChanged(SubspaceEvent e) {

		if (data == null) {
			return;
		}
		int[] vars = e.getSubspace();
		for (int element : vars) {
			logger.finest(element + "is the value");
		}
		this.processSelectedVariables(vars);

	}

	public void colorArrayChanged(ColorArrayEvent e) {
		Color[] colors = e.getColors();
		if (colors == null) {
			return;
		}
		if (colors.length == 0) {
			return;
		}
		setColors(colors);
		logger.finest("glpanel, got colors!");
	}

	// let's set colors
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

	public void paletteChanged(PaletteEvent e) {
		vc.paletteChanged(e);
	}

	public void dataSetChanged(DataSetEvent e) {
		DataSetForApps dataSet = e.getDataSetForApps();
		// the vm can't handle a big data set
		// so we bail if data set is big, big = 800 //XXX fix by sampling
		// if (dataSet.getNumObservations() > 800){
		// dataSet = null;
		// return;
		// }

		setDataSet(dataSet);
		vc.setDataSet(dataSet);
		setColors(vc.getColorForObservations());
	}

	public void setDataSet(DataSetForApps data) {
		// vc.setData(data.getDataObjectOriginal());
		// this.setColors(vc.getColorForObservations());
		// first blast old graph

		tgPanel.clearAllFull();

		this.data = data;

		String[] obsNames = data.getObservationNames();
		if (obsNames == null) {
			obsNames = new String[data.getNumObservations()];
			for (int i = 0; i < obsNames.length; i++) {
				obsNames[i] = String.valueOf(i);
			}
		}
		gvNodes = new Node[data.getNumObservations()];

		for (int i = 0; i < data.getNumObservations(); i++) {
			String id = String.valueOf(i);

			// Node n = new
			// Node(id,Node.TYPE_RECTANGLE,Color.white,obsNames[i]);
			Node n = new Node(id, obsNames[i]);
			gvNodes[i] = n;
		}

		try {
			for (int i = 0; i < data.getNumObservations(); i++) {
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

		attributesDisplay = data.getAttributeNamesNumeric();
		attList = new JList(attributesDisplay);
		gvEdges = new Edge[0];
		processSelectedVariables();

		// int[][] mst = nDMST.getMSTIndexes();
		// tgPanel.setLocale(tgPanel.getGES().getFirstNode(),localityScroll.getRadius());
		// tgPanel.setSelect(tgPanel.getGES().getFirstNode()); //Select first
		// node, so hiding works
	}

	public void randomGraph() {
		try {
			/*
			 * Node larry = new Node("0","Larry"); Node curly = new
			 * Node("1","Curly"); Node moe = new Node("2","Moe");
			 * 
			 * Edge lc = new Edge(larry,curly,1000); Edge lm = new
			 * Edge(larry,moe,1000); Edge cl = new Edge(curly,larry,1000);
			 * 
			 * tgPanel.addNode(larry); tgPanel.addNode(curly);
			 * tgPanel.addNode(moe);
			 * 
			 * tgPanel.addEdge(lc); tgPanel.addEdge(cl); tgPanel.addEdge(lm);
			 */
			Node n1 = tgPanel.addNode();
			n1.setType(0);

			for (int i = 0; i < 2490; i++) {
				Node r = tgPanel.getGES().getRandomNode();
				Node n = tgPanel.addNode();
				n.setType(0);

				if (tgPanel.findEdge(r, n) == null) {
					tgPanel.addEdge(r, n, Edge.DEFAULT_LENGTH);
				}

				if ((i % 2) == 0) {
					r = tgPanel.getGES().getRandomNode();

					if (tgPanel.findEdge(r, n) == null) {
						tgPanel.addEdge(r, n, Edge.DEFAULT_LENGTH);
					}
				}
			}

			tgPanel.setLocale(n1, 2);
			tgPanel.setSelect(tgPanel.getGES().getFirstNode()); // Select first
			// node, so
			// hiding works
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * adds an IndicationListener
	 */
	public void addIndicationListener(IndicationListener l) {
		tgPanel.addIndicationListener(l);
	}

	/**
	 * removes an IndicationListener from the component
	 */
	public void removeIndicationListener(IndicationListener l) {
		tgPanel.removeIndicationListener(l);
	}

	/**
	 * adds an SelectionListener
	 */
	public void addSelectionListener(SelectionListener l) {
		tgPanel.addSelectionListener(l);
	}

	/**
	 * removes an SelectionListener from the component
	 */
	public void removeSelectionListener(SelectionListener l) {
		tgPanel.removeSelectionListener(l);
	}

	public void hideVC() {

		this.remove(vc);
	}

	public JPanel getModeSelectPanel() {
		return modeSelectPanel;
	}

	// todo: move main method to seperate class
	public static void main(String[] args) {
		JFrame frame;
		frame = new JFrame("Graph Layout");

		LinkGraph glPanel = new LinkGraph();

		boolean useInternalFrame = true;

		JInternalFrame internalFrame = new JInternalFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		if (useInternalFrame) {

			internalFrame.add(glPanel);
			internalFrame.setSize(500, 500);
			internalFrame.setVisible(true);
			frame.getContentPane().add("Center", internalFrame);
		} else {
			frame.getContentPane().add("Center", glPanel);

		}

		frame.setSize(500, 500);
		frame.pack();
		frame.setVisible(true);

		GeoData48States data = new GeoData48States();
		DataSetEvent e = new DataSetEvent(data.getDataForApps(), frame);
		glPanel.dataSetChanged(e);
	}

	public void tableChanged(TableModelEvent e) {
		vc.setDataSet(data);

	}
} // end com.touchgraph.graphlayout.GLPanel
