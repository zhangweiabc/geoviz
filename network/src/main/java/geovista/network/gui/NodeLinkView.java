/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of

 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */

package geovista.network.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.apache.commons.collections15.functors.MapTransformer;
import org.apache.commons.collections15.map.LazyMap;

import edu.uci.ics.jung.algorithms.cluster.EdgeBetweennessClusterer;
import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.algorithms.scoring.BarycenterScorer;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.algorithms.scoring.DistanceCentralityScorer;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.algorithms.scoring.util.VertexScoreTransformer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.NumberFormattingTransformer;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.util.Animator;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;

/**
 * Demonstrates several of the graph layout algorithms. Allows the user to
 * interactively select one of several graphs, and one of several layouts, and
 * visualizes the combination. The whole panel is divided into three parts:
 * topcontrols, this, and bottomcontrols topcontrols for file and layout
 * selections, this for layout and satellite view, and bottomcontrols for other
 * basic operations.
 * 
 * Satellite view responses only after nodelink view
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain modified by weiluo
 */
@SuppressWarnings("serial")
public class NodeLinkView extends JPanel implements DataSetListener,
	SelectionListener, IndicationListener, MouseListener, ActionListener {
    protected static Graph<? extends Object, ? extends Object>[] g_array;
    protected static int g_index;
    protected static String[] g_names;
    // public static NodeHighlight<String> nh;
    public static VisualizationViewer<Integer, Number> vv;
    public static int nodes = 0;
    DataSetForApps dsa;
    public static ArrayList<Integer> syncFlag = new ArrayList<Integer>();

    // satellite part
    // VisualizationViewer<String,Number> satellite;
    // JInternalFrame dialog;
    // JDesktopPane desktop;

    // Node Part
    // protected JCheckBox v_stroke;
    /*
     * protected JCheckBox v_shape; protected JCheckBox v_size; protected
     * JCheckBox v_aspect; protected JCheckBox v_degree_labels;
     */
    protected JCheckBox v_small;

    // Nodel Filter according to vertex degree
    protected VertexDisplayPredicate<Integer, Number> show_vertex;
    // Node Score Part
    DegreeScorer degreeScorer;
    BarycenterScorer barycenterScorer;
    BetweennessCentrality betweennessCentrality;
    ClosenessCentrality closenessCentrality;
    DistanceCentralityScorer distanceCentralityScorer;
    EigenvectorCentrality eigenvectorCentrality;

    // NodeScore Labels
    Transformer<Integer, String> nonvertexLabel;
    Transformer<Integer, String> vertexLabelDegree;
    Transformer<Integer, String> vertexLabelBarycenter;
    Transformer<Integer, String> vertexLabelBetweenness;
    Transformer<Integer, String> vertexLabelCloseness;
    Transformer<Integer, String> vertexLabelDistanceCentrality;
    Transformer<Integer, String> vertexLabelEigenvector;

    // NodeScore Type Tranformer
    protected VertexShapeSizeAspect<Integer, Number> vssa;
    Transformer<Integer, Double> transformerDegree;
    Transformer<Integer, Double> transformerBarycenter;
    Transformer<Integer, Double> transformerBetweenness;
    Transformer<Integer, Double> transformerCloseness;
    Transformer<Integer, Double> transformerDistanceCentrality;
    Transformer<Integer, Double> transformerEigenvector;

    // Edge
    protected JCheckBox e_labels;

    Transformer<Number, String> edge_label;
    Transformer<Number, String> es_none;

    // Cluster part
    AggregateLayout<Integer, Number> layout;
    Map<Integer, Paint> vertexPaints = LazyMap
	    .<Integer, Paint> decorate(new HashMap<Integer, Paint>(),
		    new ConstantTransformer(Color.white));
    Map<Number, Paint> edgePaints = LazyMap.<Number, Paint> decorate(
	    new HashMap<Number, Paint>(), new ConstantTransformer(Color.blue));

    // Scale part
    Layout constantlayout;

    public final Color[] similarColors = { new Color(216, 134, 134),
	    new Color(135, 137, 211), new Color(134, 206, 189),
	    new Color(206, 176, 134), new Color(194, 204, 134),
	    new Color(145, 214, 134), new Color(133, 178, 209),
	    new Color(103, 148, 255), new Color(60, 220, 220),
	    new Color(30, 250, 100) };

    Graph<? extends Object, ? extends Object> g;

    public NodeLinkView() {
	setPreferredSize(new Dimension(300, 300));
	String[] graph_names = new String[1];
	graph_names[0] = ROAD_CONNECTION;
	// graph_names[1]= COWPEA_CONNECTION;
	// System.out.println(graph_names[0]);
	// graph_array[0]=;

	Graph<? extends Object, ? extends Object>[] graph_array = new Graph<?, ?>[1];
	try {
	    ReadMatrix rm = new ReadMatrix();
	    String fileName = "C:/java_projects/geoviz_gcode/trunk/network/src/main/java/geovista/network/Data/PoliticalBorderNileBasinReorderCorrected.txt";
	    fileName = "C:/Users/Frank/Desktop/BinaryFile_Shapefile/binaries660.txt";
	    graph_array[0] = rm.readMatrixtoDirectedGraph(fileName);

	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}

	init(graph_array, graph_names);
	vv.addMouseListener(this);
	vv.setDoubleBuffered(true);
    }

    public NodeLinkView(String[] file) {
	setPreferredSize(new Dimension(300, 300));
	String[] graph_names = new String[file.length];

	Graph<? extends Object, ? extends Object>[] graph_array = new Graph<?, ?>[file.length];
	try {
	    ReadMatrix rm = new ReadMatrix();

	    for (int i = 0; i < file.length; i++) {
		graph_array[i] = rm.readMatrixtoDirectedGraph(file[i]);
		int index1 = file[i].lastIndexOf("\\");
		int index2 = file[i].lastIndexOf(".");
		graph_names[i] = file[i].substring(index1 + 1, index2);
	    }

	    // graph_array[0] = rm
	    // .readMatrixtoGraph("C:/Users/localadmin/Desktop/Work with Chanda/WaterCooperationNileBasinReorder.txt");
	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}

	init(graph_array, graph_names);
	vv.addMouseListener(this);
    }

    public NodeLinkView(
	    Graph<? extends Object, ? extends Object>[] graph_array,
	    String[] graph_names) {

	init(graph_array, graph_names);
    }

    private void init(Graph<? extends Object, ? extends Object>[] graph_array,
	    String[] graph_names) {
	g_array = graph_array;
	g_names = graph_names;

	doInit();
    }

    public static class GraphChooser implements ActionListener {
	private final JComboBox layout_combo;

	public GraphChooser(JComboBox layout_combo) {
	    this.layout_combo = layout_combo;
	}

	public void actionPerformed(ActionEvent e) {
	    JComboBox cb = (JComboBox) e.getSource();
	    g_index = cb.getSelectedIndex();
	    layout_combo.setSelectedIndex(layout_combo.getSelectedIndex()); // rebuild
									    // the
									    // layout
	}
    }

    /**
     * 
     * @author danyelf
     */

    private static final class LayoutChooser implements ActionListener {
	private final JComboBox jcb;
	private final VisualizationViewer<Integer, Number> vv;

	private LayoutChooser(JComboBox jcb,
		VisualizationViewer<Integer, Number> vv2) {
	    super();
	    this.jcb = jcb;
	    vv = vv2;
	}

	public void actionPerformed(ActionEvent arg0) {
	    Object[] constructorArgs = { g_array[g_index] };

	    Class<? extends Layout<Integer, Number>> layoutC = (Class<? extends Layout<Integer, Number>>) jcb
		    .getSelectedItem();

	    try {
		Constructor<? extends Layout<Integer, Number>> constructor = layoutC
			.getConstructor(new Class[] { Graph.class });
		Object o = constructor.newInstance(constructorArgs);
		Layout<Integer, Number> l = (Layout<Integer, Number>) o;
		l.setInitializer(vv.getGraphLayout());
		l.setSize(vv.getSize());

		LayoutTransition<Integer, Number> lt = new LayoutTransition<Integer, Number>(
			vv, vv.getGraphLayout(), l);
		Animator animator = new Animator(lt);
		animator.start();
		vv.getRenderContext().getMultiLayerTransformer()
			.setToIdentity();
		vv.repaint();

	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    private void doInit() {

	g = g_array[0]; // initial
			// graph
	layout = new AggregateLayout<Integer, Number>(
		new FRLayout<Integer, Number>((Graph<Integer, Number>) g));
	vv = new VisualizationViewer<Integer, Number>(layout);

	// vv = new VisualizationViewer<Integer, Number>(new FRLayout(g));

	// coordination part
	// nh = new NodeHighlight<String>(vv.getPickedVertexState());
	// nh.ID = 1;
	// nh.node_no = g.getVertexCount();
	// nh.node_count = nh.node_no;
	// nh.synchroFlag = syncFlag;
	// nh.synchroFlag=new ArrayList<Integer>();
	// vv.getRenderContext().setVertexFillPaintTransformer(nh);

	// Node Part

	vv.getRenderContext().setVertexFillPaintTransformer(
		new Transformer<Integer, Paint>() {
		    public Paint transform(Integer v) {
			if (vv.getPickedVertexState().isPicked(v)) {
			    return Color.cyan;
			} else {
			    return Color.red;
			}
		    }
		});
	// Filter vertex according to degree
	show_vertex = new VertexDisplayPredicate<Integer, Number>(false);
	vv.getRenderContext().setVertexIncludePredicate(show_vertex);

	// Node Score Label
	// Node Labels
	vv.getRenderer().getVertexLabelRenderer()
		.setPosition(Renderer.VertexLabel.Position.W);

	nonvertexLabel = new ConstantTransformer(null);

	// vertexScores = new VertexScoreTransformer<Integer,
	// Double>(degreeScorer);
	degreeScorer = new DegreeScorer(g);
	transformerDegree = new VertexScoreTransformer<Integer, Double>(
		degreeScorer);
	vertexLabelDegree = new NumberFormattingTransformer<Integer>(
		transformerDegree);
	/*
	 * vertexLabel= new Transformer <Integer, String>(){ public String
	 * transform(Integer s){ return
	 * degreeScorer.getVertexScore(s).toString(); //return
	 * String.valueOf(degreeScorer.getVertexScore(s)); } };
	 */

	barycenterScorer = new BarycenterScorer(g);
	transformerBarycenter = new VertexScoreTransformer<Integer, Double>(
		barycenterScorer);
	vertexLabelBarycenter = new NumberFormattingTransformer<Integer>(
		transformerBarycenter);

	/*
	 * vertexLabelBarycenter= new Transformer <Integer, String>(){ public
	 * String transform(Integer s){ return
	 * barycenterScorer.getVertexScore(s).toString();
	 * 
	 * } };
	 */
	betweennessCentrality = new BetweennessCentrality(g);
	transformerBetweenness = new VertexScoreTransformer<Integer, Double>(
		betweennessCentrality);
	vertexLabelBetweenness = new NumberFormattingTransformer<Integer>(
		transformerBetweenness);
	// final BarycenterScorer vertex_degree_scorer= new BarycenterScorer(g);
	/*
	 * vertexLabelBetweenness= new Transformer <Integer, String>(){ public
	 * String transform(Integer s){ return
	 * betweennessCentrality.getVertexScore(s).toString();
	 * 
	 * } };
	 */
	closenessCentrality = new ClosenessCentrality(g);
	transformerCloseness = new VertexScoreTransformer<Integer, Double>(
		closenessCentrality);
	vertexLabelCloseness = new NumberFormattingTransformer<Integer>(
		transformerCloseness);
	// final BarycenterScorer vertex_degree_scorer= new BarycenterScorer(g);
	/*
	 * vertexLabelCloseness= new Transformer <Integer, String>(){ public
	 * String transform(Integer s){ return
	 * closenessCentrality.getVertexScore(s).toString();
	 * 
	 * } };
	 */
	distanceCentralityScorer = new DistanceCentralityScorer(g, true);
	transformerDistanceCentrality = new VertexScoreTransformer<Integer, Double>(
		distanceCentralityScorer);
	vertexLabelDistanceCentrality = new NumberFormattingTransformer<Integer>(
		transformerDistanceCentrality);
	// final BarycenterScorer vertex_degree_scorer= new BarycenterScorer(g);
	/*
	 * vertexLabelDistanceCentrality= new Transformer <Integer, String>(){
	 * public String transform(Integer s){ return
	 * distanceCentralityScorer.getVertexScore(s).toString();
	 * 
	 * } };
	 */
	eigenvectorCentrality = new EigenvectorCentrality(g);
	transformerEigenvector = new VertexScoreTransformer<Integer, Double>(
		eigenvectorCentrality);
	vertexLabelEigenvector = new NumberFormattingTransformer<Integer>(
		transformerEigenvector);
	// final BarycenterScorer vertex_degree_scorer= new BarycenterScorer(g);
	/*
	 * vertexLabelEigenvector= new Transformer <Integer, String>(){ public
	 * String transform(Integer s){ return
	 * eigenvectorCentrality.getVertexScore(s).toString();
	 * 
	 * } };
	 */

	// Collection<? extends Object> verts = g.getVertices();
	// DegreeScorer vertex_degree_scorer= new DegreeScorer(g);

	// System.out.println(vertex_degree_scorer.getVertexScore(0));
	// for(Integer v:verts){
	// System.out.println(vertex_degree_scorer.getVertexScore(v));

	// }
	// vertex_degree_scorer.getVertexScore(verts);
	// vv.getRenderContext().setVertexLabelTransformer(vertex_degree_scorer.getVertexScore(verts).toString());

	// vv.getRenderContext().setVertexLabelTransformer(new
	// ToStringLabeller());

	// Edge Part
	es_none = new ConstantTransformer(null);
	vv.getRenderContext().setEdgeLabelTransformer(es_none);
	// Set Edge Labels
	vv.getRenderContext().setEdgeLabelRenderer(
		new DefaultEdgeLabelRenderer(Color.cyan));
	edge_label = new Transformer<Number, String>() {
	    public String transform(Number e) {
		return "Edge:" + String.valueOf(e);
	    }
	};

	// Cluster Part

	// vv.getRenderContext().setVertexFillPaintTransformer(MapTransformer.<Integer,Paint>getInstance(vertexPaints));

	/*
	 * vv.getRenderContext().setVertexDrawPaintTransformer(new
	 * Transformer<Integer,Paint>() { public Paint transform(Integer v) {
	 * if(vv.getPickedVertexState().isPicked(v)) { return Color.cyan; } else
	 * { return Color.BLACK; } } });
	 */

	vv.getRenderContext().setEdgeDrawPaintTransformer(
		MapTransformer.<Number, Paint> getInstance(edgePaints));

	vv.getRenderContext().setEdgeStrokeTransformer(
		new Transformer<Number, Stroke>() {
		    protected final Stroke THIN = new BasicStroke(1);
		    protected final Stroke THICK = new BasicStroke(2);

		    public Stroke transform(Number e) {
			Paint c = edgePaints.get(e);
			if (c == Color.LIGHT_GRAY) {
			    return THIN;
			} else {
			    return THICK;
			}
		    }
		});
	// vv.getRenderContext().setEdgeLabelTransformer(stringer);

	// Satellite part
	/*
	 * satellite = new SatelliteVisualizationViewer<String,Number>(vv, new
	 * Dimension(200,200));
	 * satellite.getRenderContext().setEdgeDrawPaintTransformer(new
	 * PickableEdgePaintTransformer<Number>(satellite.getPickedEdgeState(),
	 * Color.black, Color.cyan));
	 * satellite.getRenderContext().setVertexFillPaintTransformer(new
	 * PickableVertexPaintTransformer
	 * <String>(satellite.getPickedVertexState(), Color.red, Color.yellow));
	 * 
	 * ScalingControl satelliteScaler = new CrossoverScalingControl();
	 * satellite.scaleToLayout(satelliteScaler);
	 */

	// Vertex
	// vssa = new VertexShapeSizeAspect<Integer,Number>(g, voltages);
	// PickedState<Integer> picked_state = vv.getPickedVertexState();
	// vsh = new VertexStrokeHighlight<Integer,Number>(g, picked_state);
	// vv.getRenderContext().setVertexStrokeTransformer(vsh);

	// Control part
	addControls(this);
	/*
	 * // Basic button part final ScalingControl scaler = new
	 * CrossoverScalingControl();
	 * 
	 * JButton plus = new JButton("+"); plus.addActionListener(new
	 * ActionListener() { public void actionPerformed(ActionEvent e) {
	 * scaler.scale(vv, 1.1f, vv.getCenter()); } }); JButton minus = new
	 * JButton("-"); minus.addActionListener(new ActionListener() { public
	 * void actionPerformed(ActionEvent e) { scaler.scale(vv, 1 / 1.1f,
	 * vv.getCenter()); } }); JButton reset = new JButton("reset");
	 * reset.addActionListener(new ActionListener() { public void
	 * actionPerformed(ActionEvent e) { Layout<String, Number> layout =
	 * vv.getGraphLayout(); layout.initialize(); Relaxer relaxer =
	 * vv.getModel().getRelaxer(); if (relaxer != null) { relaxer.stop();
	 * relaxer.prerelax(); relaxer.relax(); } } });
	 * 
	 * 
	 * // Tranform and picking part final DefaultModalGraphMouse<Integer,
	 * Number> graphMouse = new DefaultModalGraphMouse<Integer, Number>();
	 * vv.setGraphMouse(graphMouse); JComboBox modeBox =
	 * graphMouse.getModeComboBox();
	 * modeBox.addItemListener(((DefaultModalGraphMouse<Integer, Number>) vv
	 * .getGraphMouse()).getModeListener());
	 * 
	 * //JComboBox modeBox = graphMouse.getModeComboBox();
	 * //modeBox.addItemListener
	 * (((DefaultModalGraphMouse)satellite.getGraphMouse
	 * ()).getModeListener());
	 * 
	 * this.setBackground(Color.WHITE); this.setLayout(new BorderLayout());
	 * this.add(vv, BorderLayout.CENTER); Class[] combos = getCombos();
	 * final JComboBox jcb = new JComboBox(combos); jcb.setRenderer(new
	 * DefaultListCellRenderer() { public Component
	 * getListCellRendererComponent(JList list, Object value, int index,
	 * boolean isSelected, boolean cellHasFocus) { String valueString =
	 * value.toString(); valueString = valueString.substring(valueString
	 * .lastIndexOf('.') + 1); return
	 * super.getListCellRendererComponent(list, valueString, index,
	 * isSelected, cellHasFocus); } });
	 * 
	 * jcb.addActionListener(new LayoutChooser(jcb, vv));
	 * jcb.setSelectedItem(FRLayout.class);
	 * 
	 * JPanel control_panel = new JPanel(new GridLayout(2, 1)); JPanel
	 * topControls = new JPanel(); JPanel bottomControls = new JPanel();
	 * control_panel.add(topControls); control_panel.add(bottomControls);
	 * this.add(control_panel, BorderLayout.NORTH);
	 * 
	 * final JComboBox graph_chooser = new JComboBox(g_names);
	 * 
	 * graph_chooser.addActionListener(new GraphChooser(jcb));
	 * 
	 * topControls.add(jcb); topControls.add(graph_chooser);
	 * bottomControls.add(plus); bottomControls.add(minus);
	 * bottomControls.add(modeBox); bottomControls.add(reset);
	 */

	// Satellite part

	// this.add(satellite,BorderLayout.EAST);
	// Thread t = new Thread(this);
	// t.start();
    }

    protected void addControls(final JPanel jp) {

	// Satellite
	// JComboBox modeBox = graphMouse.getModeComboBox();
	// modeBox.addItemListener(((DefaultModalGraphMouse)satellite.getGraphMouse()).getModeListener());

	// Control Panel
	jp.setBackground(Color.WHITE);
	jp.setLayout(new BorderLayout());
	jp.add(vv, BorderLayout.CENTER);
	JPanel control_panel = new JPanel(new GridLayout(5, 1));
	jp.add(control_panel, BorderLayout.EAST);

	// File_Layout Panel
	Class[] combos = getCombos();
	final JComboBox jcb = new JComboBox(combos);
	jcb.setRenderer(new DefaultListCellRenderer() {
	    @Override
	    public Component getListCellRendererComponent(JList list,
		    Object value, int index, boolean isSelected,
		    boolean cellHasFocus) {
		String valueString = value.toString();
		valueString = valueString.substring(valueString
			.lastIndexOf('.') + 1);
		return super.getListCellRendererComponent(list, valueString,
			index, isSelected, cellHasFocus);
	    }
	});

	jcb.addActionListener(new LayoutChooser(jcb, vv));
	jcb.setSelectedItem(FRLayout.class);
	final Box file_layout_panel = Box.createVerticalBox();
	file_layout_panel.setBorder(BorderFactory
		.createTitledBorder("File_Layout"));
	final JComboBox graph_chooser = new JComboBox(g_names);
	graph_chooser.addActionListener(new GraphChooser(jcb));
	JPanel layoutPanel = new JPanel();
	jcb.setAlignmentX(Component.CENTER_ALIGNMENT);
	layoutPanel.add(jcb);
	graph_chooser.setAlignmentX(Component.CENTER_ALIGNMENT);
	layoutPanel.add(graph_chooser);
	file_layout_panel.add(layoutPanel);

	// Basic Operation Panel

	final ScalingControl scaler = new CrossoverScalingControl();

	JButton plus = new JButton("+");
	plus.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		scaler.scale(vv, 1.1f, vv.getCenter());
	    }
	});
	JButton minus = new JButton("-");
	minus.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		scaler.scale(vv, 1 / 1.1f, vv.getCenter());
	    }
	});
	JButton reset = new JButton("reset");
	reset.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		Layout<Integer, Number> layout = vv.getGraphLayout();
		layout.initialize();
		Relaxer relaxer = vv.getModel().getRelaxer();
		if (relaxer != null) {
		    relaxer.stop();
		    relaxer.prerelax();
		    relaxer.relax();
		}
	    }
	});

	// Tranform and picking part
	final DefaultModalGraphMouse<Integer, Number> graphMouse = new DefaultModalGraphMouse<Integer, Number>();
	vv.setGraphMouse(graphMouse);
	JComboBox modeBox = graphMouse.getModeComboBox();
	modeBox.addItemListener(((DefaultModalGraphMouse<Integer, Number>) vv
		.getGraphMouse()).getModeListener());

	JButton collapse = new JButton("Collapse");
	JButton expand = new JButton("Expand");

	final Box basic_panel = Box.createVerticalBox();
	basic_panel.setBorder(BorderFactory
		.createTitledBorder("Basic_Operation"));
	JPanel zoomPanel = new JPanel();
	// plus.setAlignmentX(Component.CENTER_ALIGNMENT);
	zoomPanel.add(plus);
	// minus.setAlignmentX(Component.CENTER_ALIGNMENT);
	zoomPanel.add(minus);
	// modeBox.setAlignmentX(Component.CENTER_ALIGNMENT);
	zoomPanel.add(modeBox);
	// reset.setAlignmentX(Component.CENTER_ALIGNMENT);
	zoomPanel.add(reset);
	// collapse.setAlignmentY(Component.CENTER_ALIGNMENT);
	zoomPanel.add(collapse);
	// expand.setAlignmentY(Component.CENTER_ALIGNMENT);
	zoomPanel.add(expand);

	basic_panel.add(zoomPanel);

	// Vertex Part
	String[] vertexScoreType = { "VertexScore", "Degree",
		"BarycenterScorer", "BetweennessCentrality",
		"ClosenessCentrality", "DistanceCentralityScorer",
		"EigenvectorCentrality" };
	final JComboBox vertexScoreList = new JComboBox(vertexScoreType);
	vertexScoreList.setSelectedIndex(0);

	vertexScoreList.addItemListener(new ItemListener() {
	    public void itemStateChanged(ItemEvent e) {
		// Renderer.VertexLabel.Position position =
		// (Renderer.VertexLabel.Position)e.getItem();
		// vv.getRenderer().getVertexLabelRenderer().setPosition(position);
		if (vertexScoreList.getSelectedIndex() == 0) {

		    // vertexScores = new VertexScoreTransformer<Integer,
		    // Double>(voltage_scores);
		    // vv.getRenderContext().setVertexShapeTransformer(new
		    // ConstantTransformer(null));
		    // vssa.setScaling(false);
		    vv.getRenderContext().setVertexLabelTransformer(
			    nonvertexLabel);
		    vv.repaint();
		}

		if (vertexScoreList.getSelectedIndex() == 1) {
		    // vertexScores = new VertexScoreTransformer<Integer,
		    // Double>(degreeScorer);
		    /*
		     * vssa = new
		     * VertexShapeSizeAspect<Integer,Number>((Graph<Integer
		     * ,Number>)g, transformerDegree);
		     * vv.getRenderContext().setVertexShapeTransformer(vssa);
		     * vssa.setScaling(true);
		     */

		    vv.getRenderContext().setVertexLabelTransformer(
			    vertexLabelDegree);
		    vv.repaint();
		}
		if (vertexScoreList.getSelectedIndex() == 2) {
		    vssa = new VertexShapeSizeAspect<Integer, Number>(
			    (Graph<Integer, Number>) g, transformerBarycenter);
		    vv.getRenderContext().setVertexShapeTransformer(vssa);
		    vssa.setScaling(true);
		    vv.getRenderContext().setVertexLabelTransformer(
			    vertexLabelBarycenter);
		    vv.repaint();
		}

		if (vertexScoreList.getSelectedIndex() == 3) {

		    // betweennessCentrality= new BetweennessCentrality(g);
		    // voltages = new VertexScoreTransformer<Integer,
		    // Double>(betweennessCentrality);
		    vssa = new VertexShapeSizeAspect<Integer, Number>(
			    (Graph<Integer, Number>) g, transformerBetweenness);
		    vv.getRenderContext().setVertexShapeTransformer(vssa);
		    vssa.setScaling(true);
		    vv.getRenderContext().setVertexLabelTransformer(
			    vertexLabelBetweenness);
		    vv.repaint();
		}
		if (vertexScoreList.getSelectedIndex() == 4) {
		    vssa = new VertexShapeSizeAspect<Integer, Number>(
			    (Graph<Integer, Number>) g, transformerCloseness);
		    vv.getRenderContext().setVertexShapeTransformer(vssa);
		    vssa.setScaling(true);
		    vv.getRenderContext().setVertexLabelTransformer(
			    vertexLabelCloseness);
		    vv.repaint();
		}
		if (vertexScoreList.getSelectedIndex() == 5) {
		    vssa = new VertexShapeSizeAspect<Integer, Number>(
			    (Graph<Integer, Number>) g,
			    transformerDistanceCentrality);
		    vv.getRenderContext().setVertexShapeTransformer(vssa);
		    vssa.setScaling(true);
		    vv.getRenderContext().setVertexLabelTransformer(
			    vertexLabelDistanceCentrality);
		    vv.repaint();
		}
		if (vertexScoreList.getSelectedIndex() == 6) {
		    vssa = new VertexShapeSizeAspect<Integer, Number>(
			    (Graph<Integer, Number>) g, transformerEigenvector);
		    vv.getRenderContext().setVertexShapeTransformer(vssa);
		    vssa.setScaling(true);
		    vv.getRenderContext().setVertexLabelTransformer(
			    vertexLabelEigenvector);
		    vv.repaint();
		}

	    }
	});
	// cb.setSelectedItem(Renderer.VertexLabel.Position.SE);

	/*
	 * v_shape = new JCheckBox("shape by degree");
	 * v_shape.addActionListener(this); v_size = new
	 * JCheckBox("size by vertexScores"); v_size.addActionListener(this);
	 * v_size.setSelected(true); v_aspect = new
	 * JCheckBox("stretch by degree ratio");
	 * v_aspect.addActionListener(this);
	 */
	v_small = new JCheckBox("filter when degree < "
		+ VertexDisplayPredicate.MIN_DEGREE);
	v_small.addActionListener(this);
	e_labels = new JCheckBox("show edge labels");
	e_labels.addActionListener(this);

	// Vertex Panel
	final Box vertex_panel = Box.createVerticalBox();
	vertex_panel.setBorder(BorderFactory.createTitledBorder("Vertices"));
	// vertex_panel.add(v_stroke);
	vertex_panel.add(vertexScoreList);
	// vertex_panel.add(v_degree_labels);
	/*
	 * vertex_panel.add(v_shape); vertex_panel.add(v_size);
	 * vertex_panel.add(v_aspect);
	 */
	vertex_panel.add(v_small);

	// Edge Part
	final Box edge_panel = Box.createVerticalBox();
	edge_panel.setBorder(BorderFactory.createTitledBorder("Edges"));
	edge_panel.add(e_labels);

	final JToggleButton groupVertices = new JToggleButton("Group Clusters");
	// Create slider to adjust the number of edges to remove when clustering
	final JSlider edgeBetweennessSlider = new JSlider(JSlider.HORIZONTAL);
	edgeBetweennessSlider.setBackground(Color.WHITE);
	edgeBetweennessSlider.setPreferredSize(new Dimension(210, 50));
	edgeBetweennessSlider.setPaintTicks(true);
	edgeBetweennessSlider.setMaximum(g.getEdgeCount());
	edgeBetweennessSlider.setMinimum(0);
	edgeBetweennessSlider.setValue(0);
	edgeBetweennessSlider.setMajorTickSpacing(10);
	edgeBetweennessSlider.setPaintLabels(true);
	edgeBetweennessSlider.setPaintTicks(true);

	// Cluster Part
	final Box cluster_panel = Box.createVerticalBox();
	cluster_panel.setBorder(BorderFactory.createTitledBorder("Cluster"));
	cluster_panel.add(edgeBetweennessSlider);

	final String COMMANDSTRING = "Edges removed for clusters: ";
	final String eastSize = COMMANDSTRING
		+ edgeBetweennessSlider.getValue();

	final TitledBorder sliderBorder = BorderFactory
		.createTitledBorder(eastSize);
	cluster_panel.setBorder(sliderBorder);
	cluster_panel.add(Box.createVerticalGlue());
	groupVertices.addItemListener(new ItemListener() {
	    public void itemStateChanged(ItemEvent e) {
		clusterAndRecolor(layout, edgeBetweennessSlider.getValue(),
			similarColors, e.getStateChange() == ItemEvent.SELECTED);
		vv.repaint();
	    }
	});

	clusterAndRecolor(layout, 0, similarColors, groupVertices.isSelected());

	edgeBetweennessSlider.addChangeListener(new ChangeListener() {
	    public void stateChanged(ChangeEvent e) {
		JSlider source = (JSlider) e.getSource();
		if (!source.getValueIsAdjusting()) {
		    int numEdgesToRemove = source.getValue();
		    clusterAndRecolor(layout, numEdgesToRemove, similarColors,
			    groupVertices.isSelected());
		    sliderBorder.setTitle(COMMANDSTRING
			    + edgeBetweennessSlider.getValue());
		    cluster_panel.repaint();
		    vv.validate();
		    vv.repaint();
		}
	    }
	});
	cluster_panel.add(groupVertices);

	control_panel.add(file_layout_panel);
	control_panel.add(vertex_panel);
	control_panel.add(edge_panel);
	control_panel.add(cluster_panel);
	control_panel.add(basic_panel);
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Layout>[] getCombos() {
	List<Class<? extends Layout>> layouts = new ArrayList<Class<? extends Layout>>();
	layouts.add(KKLayout.class);
	layouts.add(FRLayout.class);
	layouts.add(CircleLayout.class);
	layouts.add(SpringLayout.class);
	layouts.add(SpringLayout2.class);
	layouts.add(ISOMLayout.class);
	return layouts.toArray(new Class[0]);
    }

    /*
     * private final static class VertexShapeSizeAspect<V,E> extends
     * AbstractVertexShapeTransformer <V> implements Transformer<V,Shape> {
     * 
     * // protected boolean stretch = false; protected boolean scale = false; //
     * protected boolean funny_shapes = false; protected Transformer<V,Double>
     * voltages; protected Graph<V,E> graph; // protected AffineTransform
     * scaleTransform = new AffineTransform();
     * 
     * public VertexShapeSizeAspect(Graph<? extends Object, ? extends Object> g,
     * Transformer<V,Double> voltagesIn) { this.graph = (Graph<V, E>) g;
     * this.voltages = voltagesIn; setSizeTransformer(new
     * Transformer<V,Integer>() {
     * 
     * public Integer transform(V v) { if (scale){ return new
     * Integer((int)(voltages.transform(v) * 30) + 20); } return 20; } });
     * setAspectRatioTransformer(new Transformer<V,Float>() {
     * 
     * public Float transform(V v) { if (stretch) { return
     * (float)(graph.inDegree(v) + 1) / (graph.outDegree(v) + 1); } else {
     * return 1.0f; } }}); }
     * 
     * public void setStretching(boolean stretch) { this.stretch = stretch; }
     * 
     * public void setScaling(boolean scale) { this.scale = scale; }
     * 
     * public void useFunnyShapes(boolean use) { this.funny_shapes = use; }
     * 
     * public Shape transform(V v) { if (funny_shapes) { if (graph.degree(v) <
     * 5) { int sides = Math.max(graph.degree(v), 3); return
     * factory.getRegularPolygon(v, sides); } else return
     * factory.getRegularStar(v, graph.degree(v)); } else{
     * System.out.println("V value: "+factory.getEllipse(v).toString()); return
     * factory.getEllipse(v);
     * 
     * } }
     */

    @Override
    public void actionPerformed(ActionEvent e) {
	// TODO Auto-generated method stub

	AbstractButton source = (AbstractButton) e.getSource();

	if (source == v_small) {
	    show_vertex.filterSmall(source.isSelected());
	} else if (source == e_labels) {
	    if (source.isSelected()) {
		vv.getRenderContext().setEdgeLabelTransformer(edge_label);
	    } else {
		vv.getRenderContext().setEdgeLabelTransformer(es_none);
	    }
	}
	/*
	 * if (source == v_shape) { vssa.useFunnyShapes(source.isSelected()); }
	 * else
	 */// if (source == v_size)
	{
	    // vssa.setScaling(source.isSelected());
	}
	/*
	 * else if (source == v_aspect) {
	 * vssa.setStretching(source.isSelected()); }
	 */
	vv.repaint();

    }

    public void clusterAndRecolor(AggregateLayout<Integer, Number> layout,
	    int numEdgesToRemove, Color[] colors, boolean groupClusters) {
	// Now cluster the vertices by removing the top 50 edges with highest
	// betweenness
	// if (numEdgesToRemove == 0) {
	// colorCluster( g.getVertices(), colors[0] );
	// } else {

	Graph<Integer, Number> graph = layout.getGraph();
	layout.removeAll();

	EdgeBetweennessClusterer<Integer, Number> clusterer = new EdgeBetweennessClusterer<Integer, Number>(
		numEdgesToRemove);
	Set<Set<Integer>> clusterSet = clusterer.transform(graph);
	List<Number> edges = clusterer.getEdgesRemoved();

	int i = 0;
	// Set the colors of each node so that each cluster's vertices have the
	// same color
	for (Set<Integer> vertices : clusterSet) {

	    Color c = colors[i % colors.length];

	    colorCluster(vertices, c);
	    if (groupClusters == true) {
		groupCluster(layout, vertices);
	    }
	    i++;
	}
	for (Number e : graph.getEdges()) {

	    if (edges.contains(e)) {
		edgePaints.put(e, Color.lightGray);
	    } else {
		edgePaints.put(e, Color.black);
	    }
	}

    }

    private void colorCluster(Set<Integer> vertices, Color c) {
	for (Integer v : vertices) {
	    vertexPaints.put(v, c);
	}
    }

    private void groupCluster(AggregateLayout<Integer, Number> layout,
	    Set<Integer> vertices) {
	if (vertices.size() < layout.getGraph().getVertexCount()) {
	    Point2D center = layout.transform(vertices.iterator().next());
	    Graph<Integer, Number> subGraph = SparseMultigraph
		    .<Integer, Number> getFactory().create();
	    for (Integer v : vertices) {
		subGraph.addVertex(v);
	    }
	    Layout<Integer, Number> subLayout = new CircleLayout<Integer, Number>(
		    subGraph);
	    subLayout.setInitializer(vv.getGraphLayout());
	    subLayout.setSize(new Dimension(40, 40));

	    layout.put(subLayout, center);
	    vv.repaint();
	}
    }

    public static String ROAD_CONNECTION = "RoadConn";
    public static String COWPEA_CONNECTION = "CowpeaConn";

    public void dataSetChanged(DataSetEvent e) {

	dsa = e.getDataSetForApps();
	String[] variableNames = dsa.getAttributeNamesNumeric();
	System.out.println(Arrays.toString(variableNames));
	String[] roadConnections = dsa
		.getStringArrayDataByName(NodeLinkView.ROAD_CONNECTION);
	for (String s : roadConnections) {
	    System.out.println(s);
	}

	for (String roadConnection : roadConnections) {
	    // System.out.println(roadConnections.length);
	    // System.out.println(roadConnections[2].charAt(0));
	}

	/*
	 * ReadShapeFileintoMatrix rsfMatrix = new
	 * ReadShapeFileintoMatrix(roadConnections); try {
	 * rsfMatrix.getMatrixfromShapefile(); } catch (Exception e1) { // TODO
	 * Auto-generated catch block e1.printStackTrace(); }
	 */

	for (int i = 0; i < dsa.getNumberNumericAttributes(); i++) {
	    double[] nthArray = dsa.getNumericDataAsDouble(i);
	    // System.out.println(Arrays.toString(nthArray));
	}

	// XXX needs implementation
	/*
	 * String[] graph_names=new String[1]; graph_names[0]= ROAD_CONNECTION;
	 * //graph_names[1]= COWPEA_CONNECTION;
	 * //System.out.println(graph_names[0]); //graph_array[0]=;
	 * 
	 * Graph<? extends Object, ? extends Object>[] graph_array = (Graph<?
	 * extends Object,? extends Object>[]) new Graph<?,?>[1]; try {
	 * graph_array[0]=ReadMatrix.readMatrixtoGraph(
	 * "C:/Users/localadmin/workspace/Data/nigerRoadConnectivity.txt"); }
	 * catch (IOException e1) { // TODO Auto-generated catch block
	 * e1.printStackTrace(); } this.init(graph_array , graph_names);
	 */
    }

    HashMap<Integer, Integer> GvIDtoNetworkID = new HashMap<Integer, Integer>();

    public void indicationChanged(IndicationEvent e) {
	int obs = e.getIndication();
	if (obs < 0) {
	    return;
	}
	String name = dsa.getObservationName(obs);
	System.out.println(NodeLinkView.class.getName() + " indication = "
		+ obs + " " + name);

	/*
	 * nh.selectedNodes.clear(); nh.selectedNodes.add(String.valueOf(obs));
	 * nh.passive=true; nh.node_count=nh.node_no;
	 */
	// PickedState pve = new MultiPickedState();
	// vv.getRenderContext().setVertexFillPaintTransformer(obs);
	// vv.setPickedVertexState(pve);

    }

    /**
     * adds an IndicationListener
     */
    public void addIndicationListener(IndicationListener l) {
	// listenerList.add(IndicationListener.class, l);
    }

    /**
     * removes an IndicationListener from the component
     */
    public void removeIndicationListener(IndicationListener l) {
	listenerList.remove(IndicationListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    private void fireIndicationChanged(int newIndication) {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	IndicationEvent e = null;

	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == IndicationListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new IndicationEvent(this, newIndication);
		}

		((IndicationListener) listeners[i + 1]).indicationChanged(e);
	    }
	} // next i
    }

    /**
     * adds an SelectionListener
     */
    public void addSelectionListener(SelectionListener l) {

	listenerList.add(SelectionListener.class, l);
    }

    /**
     * removes an SelectionListener from the component
     */
    public void removeSelectionListener(SelectionListener l) {

	listenerList.remove(SelectionListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    static int counter = 0;

    private void fireSelectionChanged(int[] newSelection) {
	// counter++;
	// System.out.println("firing new selection!! " + counter);
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	SelectionEvent e = null;

	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == SelectionListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new SelectionEvent(this, newSelection);
		}

		((SelectionListener) listeners[i + 1]).selectionChanged(e);
	    }
	} // next i
    }

    @Override
    public void selectionChanged(SelectionEvent e) {
	int[] selObs = e.getSelection();
	if (selObs.length == 0) {
	    return;
	}
	// nh.selectedNodes.clear();
	PickedState<Integer> ps = vv.getPickedVertexState();
	ps.clear();
	for (int obs : selObs) {
	    if (obs < 0) {
		return;
	    }
	    ps.pick(new Integer(obs), true);
	    /*
	     * String name = dsa.getObservationName(obs);
	     * System.out.println(NodeLinkView.class.getName() + " selection = "
	     * + obs + " " + name); //nh.selectedNodes.add(String.valueOf(obs));
	     * //nh.passive = true;
	     */
	}
	/*
	 * if (!nh.selectedNodes.isEmpty()) { nh.node_count = nh.node_no;
	 * this.vv.repaint(); }
	 */
	vv.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
	// TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
	// TODO Auto-generated method stub

    }

    @Override
    public void mouseExited(MouseEvent arg0) {
	// TODO Auto-generated method stub

    }

    @Override
    public void mousePressed(MouseEvent arg0) {
	// TODO Auto-generated method stub

    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
	PickedState<Integer> ps = vv.getPickedVertexState();
	Set<Integer> selectedV = ps.getPicked();
	if (selectedV.isEmpty()) {
	    return;
	}
	ArrayList<Integer> selNodes = new ArrayList<Integer>();
	Iterator<Integer> it = selectedV.iterator();
	while (it.hasNext()) {
	    selNodes.add(it.next());
	}
	int[] selObjs = new int[selNodes.size()];
	for (int i = 0; i < selObjs.length; i++) {
	    selObjs[i] = selNodes.get(i).intValue();
	}
	fireSelectionChanged(selObjs);
    }

    /*
     * @Override public void run() { while (true) { synchronized (syncFlag) {
     * while (syncFlag.isEmpty()) { try { syncFlag.wait(); } catch (Exception e)
     * { e.printStackTrace(); } } syncFlag.clear(); if
     * (nh.selectedNodes.isEmpty()) continue; int[] nodes = new
     * int[nh.selectedNodes.size()]; Iterator<String> it =
     * nh.selectedNodes.iterator(); int index = 0; while (it.hasNext()) { int
     * node = Integer.parseInt(it.next()); nodes[index] = node; index++; }
     * fireSelectionChanged(nodes); } } }
     */
}
