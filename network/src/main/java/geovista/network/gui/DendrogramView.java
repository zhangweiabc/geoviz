package geovista.network.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import cern.colt.matrix.impl.SparseDoubleMatrix2D;

import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.algorithms.matrix.GraphMatrixOperations;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.picking.PickedInfo;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformerDecorator;
import edu.uci.ics.jung.visualization.util.Animator;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.coordination.CoordinationManager;
import geovista.geoviz.map.GeoMap;
import geovista.network.algorithm.MultiLevelConcor;
import geovista.readers.example.GeoData48States;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.readers.shapefile.ShapeFileToShape;

/*1: How to convert cern.colt.matrix.impl.SparseDoubleMatrix2D into double array, 
 because when we convert graph into matrix with the class provided by jung, the output is SparseDoubleMatrix2D.
 */
/**
 * @modified Aug. 19 add a transformer that can highlight the children nodes of
 *           a selected node
 * @modified Aug. 23 modify the transformer so that all descendant nodes are
 *           highlighted.
 */
public class DendrogramView extends JPanel implements ChangeListener,
	DataSetListener, SelectionListener, IndicationListener, MouseListener {

    double[][][] Matrix;
    final static Logger logger = Logger.getLogger(DendrogramView.class
	    .getName());
    int current_level = 1;
    MultiLevelConcor mlc;
    NodeFillColor<String> nfc;
    /**
     * the graph
     */
    Forest<String, Number> graph;
    Graph<? extends Object, ? extends Object>[] g_array;
    public static ArrayList<Integer> syncFlag = new ArrayList<Integer>();

    Factory<DirectedGraph<String, Integer>> graphFactory = new Factory<DirectedGraph<String, Integer>>() {

	public DirectedGraph<String, Integer> create() {
	    return new DirectedSparseMultigraph<String, Integer>();
	}
    };

    Factory<Tree<String, Integer>> treeFactory = new Factory<Tree<String, Integer>>() {

	public Tree<String, Integer> create() {
	    return new DelegateTree<String, Integer>(graphFactory);
	}
    };

    Factory<Integer> edgeFactory = new Factory<Integer>() {
	int i = 0;

	public Integer create() {
	    return i++;
	}
    };

    Factory<String> vertexFactory = new Factory<String>() {
	int i = 0;

	public String create() {
	    return "V" + i++;
	}
    };

    /**
     * the visual component and renderer for the graph
     */
    public VisualizationViewer<String, Number> vv;
    // public NodeHighlight<String> nh;
    Container content;

    VisualizationServer.Paintable rings;

    String root;

    TreeLayout<String, Number> treeLayout;

    BalloonLayout<String, Number> radialLayout;

    JSlider levelControl;
    JToggleButton radial;
    public static int nodes = 0;
    DataSetForApps dsa;

    public DendrogramView() {

	g_array = new Graph<?, ?>[1];
	try {
	    ReadMatrix rm = new ReadMatrix();
	    /*
	     * graph_array[0] = rm .readMatrixtoGraph(
	     * "C:/Users/localadmin/workspace/Data/nigerRoadConnectivity.txt");
	     */
	    // C:\Users\localadmin\workspace\Data\nigerRoadConnectivity.txt
	    // Data/Niger/Niger_Cowpea_Reorder.txt"
	    // graph_array[0] = rm
	    // .readMatrixtoGraph("C:/Users/localadmin/Desktop/Work with Chanda/PoliticalBorderNileBasinReorderCorrected.txt");
	    // matrix=rm.ReadData("C:/Users/localadmin/Desktop/Work with Chanda/PoliticalBorderNileBasinReorderCorrected.txt");
	    String fileName = "C:\\Users\\weiluo\\Documents\\PSU\\GeoVista\\Social-spatial\\Data\\COW_Trade_2.01\\network_2005_reorder.txt";
	    fileName = "C:/Users/Frank/Desktop/BinaryFile_Shapefile/binaries660.txt";
	    g_array[0] = rm.readMatrixtoDirectedGraph(fileName);
	    String[] files = { fileName };
	    Matrix = rm.ReadData(files);

	    // graph_array[0] = rm
	    // .readMatrixtoGraph("C:/Users/localadmin/Desktop/Work with Chanda/WaterCooperationNileBasinReorder.txt");
	    // matrix=rm.ReadData("C:/Users/localadmin/Desktop/Work with Chanda/WaterCooperationNileBasinReorder.txt");

	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}

	init(g_array, Matrix);
    }

    public DendrogramView(String[] file) {

	g_array = new Graph<?, ?>[file.length];
	try {
	    ReadMatrix rm = new ReadMatrix();
	    /*
	     * graph_array[0] = rm .readMatrixtoGraph(
	     * "C:/Users/localadmin/workspace/Data/nigerRoadConnectivity.txt");
	     */
	    // C:\Users\localadmin\workspace\Data\nigerRoadConnectivity.txt
	    // Data/Niger/Niger_Cowpea_Reorder.txt"
	    // graph_array[0] = rm
	    // .readMatrixtoGraph("C:/Users/localadmin/Desktop/Work with Chanda/PoliticalBorderNileBasinReorderCorrected.txt");
	    // matrix=rm.ReadData("C:/Users/localadmin/Desktop/Work with Chanda/PoliticalBorderNileBasinReorderCorrected.txt");

	    for (int i = 0; i < file.length; i++) {
		g_array[i] = rm.readMatrixtoDirectedGraph(file[i]);
	    }

	    Matrix = rm.ReadData(file);

	    // graph_array[0] = rm
	    // .readMatrixtoGraph("C:/Users/localadmin/Desktop/Work with Chanda/WaterCooperationNileBasinReorder.txt");
	    // matrix=rm.ReadData("C:/Users/localadmin/Desktop/Work with Chanda/WaterCooperationNileBasinReorder.txt");

	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}

	init(g_array, Matrix);

    }

    private void init(Graph<? extends Object, ? extends Object>[] graph_array,
	    double[][][] matrix) {

	// super("DendrogramView View");
	Graph<String, Number> g = (Graph<String, Number>) graph_array[0];

	SparseDoubleMatrix2D matrixArray = GraphMatrixOperations
		.graphToSparseMatrix((Graph<String, Number>) graph_array[0]);

	/*
	 * matrix=new double[g.getVertexCount()][g.getVertexCount()]; for(int
	 * i=0; i<matrix.length; i++){ matrix[i][i]=0; for(int j=i+1;
	 * j<matrix[i].length; j++) if(g.isNeighbor(String.valueOf(i),
	 * String.valueOf(j))){ matrix[i][j]=1; matrix[j][i]=1; } else{
	 * matrix[i][j]=0; matrix[j][i]=0; } }
	 */

	graph = new DelegateForest<String, Number>();
	Collection<String> c = g.getVertices();

	// create a simple graph for the demo
	// graph = new DelegateForest<String,Integer>();
	String labels[] = new String[matrix[0].length];
	for (int i = 0; i < labels.length; i++) {
	    labels[i] = String.valueOf(i);
	}

	mlc = new MultiLevelConcor(labels, matrix);
	initTree();

	treeLayout = new TreeLayout<String, Number>(graph);

	radialLayout = new BalloonLayout<String, Number>(graph);
	radialLayout.setSize(new Dimension(600, 600));
	vv = new VisualizationViewer<String, Number>(treeLayout, new Dimension(
		600, 600));
	vv.setBackground(Color.white);
	nfc = new NodeFillColor<String>(vv.getPickedVertexState());
	vv.getRenderContext().setVertexFillPaintTransformer(nfc);
	/*
	 * nh = new NodeHighlight<String>(vv.getPickedVertexState()); nh.ID = 2;
	 * nh.node_no = graph.getVertexCount(); nh.node_count = nh.node_no;
	 * nh.synchroFlag = syncFlag;
	 * vv.getRenderContext().setVertexFillPaintTransformer(nh);
	 */
	vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
	vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
	// add a listener for ToolTips
	vv.setVertexToolTipTransformer(new ToStringLabeller());
	vv.getRenderContext().setArrowFillPaintTransformer(
		new ConstantTransformer(Color.lightGray));
	rings = new Rings(radialLayout);

	// Container content = getContentPane();
	// final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
	// content.add(panel);

	final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();

	vv.setGraphMouse(graphMouse);

	JComboBox modeBox = graphMouse.getModeComboBox();
	modeBox.addItemListener(graphMouse.getModeListener());
	graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);

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

	radial = new JToggleButton("Balloon");
	radial.addItemListener(new ItemListener() {

	    public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {

		    LayoutTransition<String, Number> lt = new LayoutTransition<String, Number>(
			    vv, treeLayout, radialLayout);
		    Animator animator = new Animator(lt);
		    animator.start();
		    vv.getRenderContext().getMultiLayerTransformer()
			    .setToIdentity();
		    vv.addPreRenderPaintable(rings);
		} else {
		    LayoutTransition<String, Number> lt = new LayoutTransition<String, Number>(
			    vv, radialLayout, treeLayout);
		    Animator animator = new Animator(lt);
		    animator.start();
		    vv.getRenderContext().getMultiLayerTransformer()
			    .setToIdentity();
		    vv.removePreRenderPaintable(rings);
		}
		vv.repaint();
	    }
	});

	JPanel scaleGrid = new JPanel(new GridLayout(1, 0));
	scaleGrid.setBorder(BorderFactory.createTitledBorder("Zoom"));

	JPanel controls = new JPanel();
	scaleGrid.add(plus);
	scaleGrid.add(minus);
	controls.add(radial);
	controls.add(scaleGrid);
	controls.add(modeBox);

	int max = (int) Math.round(Math.log(matrix[0].length) / Math.log(2));
	levelControl = new JSlider(JSlider.HORIZONTAL, 1, max, 1);
	levelControl.setMajorTickSpacing(1);
	levelControl.setMinorTickSpacing(1);
	levelControl.setPaintTicks(true);
	levelControl.addChangeListener(this);
	levelControl.setPaintLabels(true);
	Hashtable<Integer, JLabel> ht = new Hashtable<Integer, JLabel>();
	for (int i = 1; i <= max; i++) {
	    ht.put(new Integer(i), new JLabel(String.valueOf(i)));
	}
	levelControl.setLabelTable(ht);
	controls.add(levelControl);

	setBackground(Color.WHITE);
	setLayout(new BorderLayout());
	this.add(vv, BorderLayout.CENTER);
	this.add(controls, BorderLayout.NORTH);

	// content.add(controls, BorderLayout.SOUTH);
	// Dimension preferredSize = new Dimension(600,600);
	// setSize(preferredSize);
	// setLocation(600,0);
	// setVisible(true);
	// Thread t = new Thread(this);
	// t.start();
	vv.addMouseListener(this);
    }

    class Rings implements VisualizationServer.Paintable {

	BalloonLayout<String, Number> layout;

	public Rings(BalloonLayout<String, Number> layout) {
	    this.layout = layout;
	}

	public void paint(Graphics g) {
	    g.setColor(Color.gray);

	    Graphics2D g2d = (Graphics2D) g;

	    Ellipse2D ellipse = new Ellipse2D.Double();
	    for (String v : layout.getGraph().getVertices()) {
		Double radius = layout.getRadii().get(v);
		if (radius == null) {
		    continue;
		}
		Point2D p = layout.transform(v);
		ellipse.setFrame(-radius, -radius, 2 * radius, 2 * radius);
		AffineTransform at = AffineTransform.getTranslateInstance(
			p.getX(), p.getY());
		Shape shape = at.createTransformedShape(ellipse);

		MutableTransformer viewTransformer = vv.getRenderContext()
			.getMultiLayerTransformer().getTransformer(Layer.VIEW);

		if (viewTransformer instanceof MutableTransformerDecorator) {
		    shape = vv.getRenderContext().getMultiLayerTransformer()
			    .transform(shape);
		} else {
		    shape = vv.getRenderContext().getMultiLayerTransformer()
			    .transform(Layer.LAYOUT, shape);
		}

		g2d.draw(shape);
	    }

	}

	public boolean useTransform() {
	    return true;
	}
    }

    /**
     * 
     */

    protected void initTree() {
	int level = 1;
	graph.addVertex("Root");
	ArrayList<String> queue = new ArrayList<String>();
	queue.add("Root");
	createParent(1, level, queue);
	createLeaf(1, queue);
	// nh.node_no=graph.getVertexCount();
	// nh.node_count=nh.node_no;
    }

    protected void createTree(int level) {
	clearLeaf();
	ArrayList<String> queue = null;
	if (level > current_level) { // more nodes needs to be drawn
	    queue = Enqueue(current_level);
	    createParent(current_level + 1, level, queue);
	} else { // clear some nodes
	    clearParent(level + 1, current_level);
	    queue = Enqueue(level);
	}
	createLeaf(level, queue);
	/*
	 * nh.node_no = graph.getVertexCount(); nh.node_count = nh.node_no;
	 */
    }

    protected ArrayList<String> Enqueue(int level) {
	int size = (int) Math.pow(2, level);
	ArrayList<String> queue = new ArrayList<String>();
	for (int i = 1; i <= size; i++) {
	    queue.add("V" + level + i);
	}
	return queue;
    }

    protected void clearParent(int start, int level) {
	for (int i = start; i <= level; i++) {
	    int size = (int) Math.pow(2, i);
	    for (int j = 1; j <= size; j++) {
		graph.removeVertex("V" + i + j);
	    }
	}
    }

    protected void createParent(int start, int level, ArrayList<String> queue) {
	for (int i = start; i <= level; i++) {
	    int size = (int) Math.pow(2, i);
	    for (int j = 1; j <= size; j += 2) {
		String parent = queue.remove(0);
		graph.addEdge(edgeFactory.create(), parent, "V" + i + j);
		queue.add("V" + i + j);
		graph.addEdge(edgeFactory.create(), parent, "V" + i + (j + 1));
		queue.add("V" + i + (j + 1));
	    }
	}
    }

    protected void clearLeaf() {
	for (int i = 0; i < Matrix[0].length; i++) {
	    graph.removeVertex(mlc.label(i));
	}
    }

    protected void createLeaf(int level, ArrayList<String> queue) {
	mlc.concor(level, 50);
	int sig;
	for (int i = 0; i < Matrix[0].length; i++) {
	    sig = mlc.signature(i);
	    if (sig == -1) {
		System.err.println("illegal label " + i);
		continue;
	    }
	    sig = sig >> (mlc.maxEff - level);
	    graph.addEdge(edgeFactory.create(), queue.get(sig), mlc.label(i));
	}
    }

    @Override
    public void stateChanged(ChangeEvent e) {
	int level = levelControl.getValue();
	if (level != current_level) {
	    createTree(level);
	    current_level = level;
	    if (!radial.isSelected()) {
		TreeLayout<String, Number> tl2 = treeLayout;
		radialLayout = new BalloonLayout<String, Number>(graph);
		radialLayout.setSize(new Dimension(1200, 1200));
		rings = new Rings(radialLayout);
		treeLayout = new TreeLayout<String, Number>(graph);
		LayoutTransition<String, Number> lt = new LayoutTransition<String, Number>(
			vv, tl2, treeLayout);
		Animator animator = new Animator(lt);
		animator.start();
	    } else {
		BalloonLayout<String, Number> r2 = radialLayout;
		radialLayout = new BalloonLayout<String, Number>(graph);
		radialLayout.setSize(new Dimension(1200, 1200));
		treeLayout = new TreeLayout<String, Number>(graph);
		vv.getRenderContext().getMultiLayerTransformer()
			.setToIdentity();
		vv.removePreRenderPaintable(rings);
		rings = new Rings(radialLayout);
		LayoutTransition<String, Number> lt = new LayoutTransition<String, Number>(
			vv, r2, radialLayout);
		Animator animator = new Animator(lt);
		animator.start();
		vv.getRenderContext().getMultiLayerTransformer()
			.setToIdentity();
		vv.addPreRenderPaintable(rings);
	    }
	    // vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
	}
    }

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

    @Override
    public void indicationChanged(IndicationEvent e) {
	// TODO Auto-generated method stub
	int obs = e.getIndication();
	if (obs < 0) {
	    return;
	}
	String name = dsa.getObservationName(obs);
	System.out.println(DendrogramView.class.getName() + " indication = "
		+ obs + " " + name);

    }

    @Override
    /**
     * @modified by peifeng yin
     * @date Jan 13, 2011
     */
    public void selectionChanged(SelectionEvent e) {
	// TODO Auto-generated method stub

	int[] selObs = e.getSelection();
	// nh.selectedNodes.clear();
	if (selObs.length == 0) {
	    return;
	}
	// else if(selObs.length==1)//include the selected node's children
	// selObs=includeChildren(selObs[0]);
	PickedState<String> ps = vv.getPickedVertexState();
	ps.clear();
	nfc.kids.clear();
	for (int obs : selObs) {
	    if (obs < 0) {
		return;
	    }
	    ps.pick(String.valueOf(obs), true);
	    /*
	     * String name = dsa.getObservationName(obs);
	     * System.out.println(DendrogramView.class.getName() +
	     * " selection = " + obs + " " + name);
	     * nh.selectedNodes.add(String.valueOf(obs));
	     */
	}
	/*
	 * if (!nh.selectedNodes.isEmpty()) { nh.passive = true; nh.node_count =
	 * nh.node_no; this.vv.repaint(); }
	 */
	vv.repaint();
    }

    public static String ROAD_CONNECTION = "RoadConn";

    @Override
    public void dataSetChanged(DataSetEvent e) {
	// TODO Auto-generated method stub

	dsa = e.getDataSetForApps();
	String[] variableNames = dsa.getAttributeNamesNumeric();
	System.out.println(Arrays.toString(variableNames));
	String[] roadConnections = dsa
		.getStringArrayDataByName(DendrogramView.ROAD_CONNECTION);
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

    }

    public static void main2(String[] args) {
	JFrame app = new JFrame("test frame");
	app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	app.setLayout(new FlowLayout());
	// NodeLinkView nlView = new NodeLinkView();
	// nlView.setBorder(BorderFactory.createLineBorder(Color.green, 3));
	// getGraphPanel();

	String fileName = "C:\\Users\\localadmin\\Documents\\GeoVista\\Gate's project\\Data\\Niger\\data\\data3\\NigerNewWGS84.shp";
	ShapeFileDataReader shpRead = new ShapeFileDataReader();
	shpRead.setFileName(fileName);
	DendrogramView dv = new DendrogramView();

	CoordinationManager coord = new CoordinationManager();

	ShapeFileToShape shpToShape = new ShapeFileToShape();
	ShapeFileProjection shpProj = new ShapeFileProjection();
	GeoData48States stateData = new GeoData48States();

	dv.setBorder(BorderFactory.createLineBorder(Color.green, 3));

	GeoMap map = new GeoMap();

	app.add(map);
	app.add(dv);

	coord.addBean(shpToShape);
	coord.addBean(dv);
	coord.addBean(map);
	shpProj.setInputDataSet(shpRead.getDataSet());
	Object[] dataSet = null;
	dataSet = shpProj.getOutputDataSet();
	shpToShape.setInputDataSet(dataSet);

	app.pack();
	app.setVisible(true);

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

    public void addIndicationListener(IndicationListener l) {
	listenerList.add(IndicationListener.class, l);
    }

    /**
     * removes an IndicationListener from the component
     */
    public void removeIndicationListener(IndicationListener l) {
	listenerList.remove(IndicationListener.class, l);
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
	PickedState<String> ps = vv.getPickedVertexState();
	Set<String> ss = ps.getPicked();
	if (ss.size() == 1) {
	    for (String s : ss) {
		nfc.kids.clear();
		ArrayList<String> kids = new ArrayList<String>();
		kids.add(s);
		for (int i = 0; i < kids.size(); i++) {
		    includeKidNodes(kids.get(i), kids);
		}
		nfc.kids.addAll(kids);
		for (int i = kids.size() - 1; i >= 0; i--) {
		    String id = kids.get(i);
		    if (id.equals("Root") || id.startsWith("V")) {
			kids.remove(i);
		    }
		}
		int[] selObj = new int[kids.size()];
		for (int i = 0; i < selObj.length; i++) {
		    selObj[i] = Integer.parseInt(kids.get(i));
		}
		fireSelectionChanged(selObj);
		logger.info(Arrays.toString(selObj));
	    }
	} else {
	    nfc.kids.clear();
	}
    }

    /**
     * add kid nodes into the given set
     * 
     * @param id
     * @param kids
     */
    protected void includeKidNodes(String id, ArrayList<String> kids) {
	if (id.equals("Root")) {
	    kids.add("V11");
	    kids.add("V12");
	    return;
	} else if (!id.startsWith("V")) {
	    return;
	}
	int level = Integer.parseInt(id.substring(1, 2));
	int num = Integer.parseInt(id.substring(2, id.length()));
	if (level < current_level) { // not lowest parent node
	    level++;
	    kids.add("V" + level + (num * 2 - 1));
	    kids.add("V" + level + (num * 2));
	} else { // lowest parent node, need to add leaf nodes
	    for (int i = 0; i < Matrix[0].length; i++) {
		int sig = mlc.signature(i);
		if (sig == -1) {
		    System.err.println("illegal label " + i);
		    continue;
		}
		sig = sig >> (mlc.maxEff - level);
		if (sig == num - 1) {
		    kids.add(mlc.label(i));
		}
	    }
	}
    }

    /**
     * add kid nodes into the given set.
     * 
     * @param id
     * @param kids
     */
    protected void includeKidNodes(String id, HashSet<String> kids) {
	if (id.equals("Root")) {
	    kids.add("V11");
	    kids.add("V12");
	    return;
	} else if (!id.startsWith("V")) {
	    return;
	}
	int level = Integer.parseInt(id.substring(1, 2));
	int num = Integer.parseInt(id.substring(2, id.length()));
	if (level < current_level) { // not lowest parent node
	    level++;
	    kids.add("V" + level + (num * 2 - 1));
	    kids.add("V" + level + (num * 2));
	} else { // lowest parent node, need to add leaf nodes
	    for (int i = 0; i < Matrix[0].length; i++) {
		int sig = mlc.signature(i);
		if (sig == -1) {
		    System.err.println("illegal label " + i);
		    continue;
		}
		sig = sig >> (mlc.maxEff - level);
		if (sig == num - 1) {
		    kids.add(mlc.label(i));
		}
	    }
	}
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
	PickedState<String> ps = vv.getPickedVertexState();
	Set<String> selectedV = ps.getPicked();
	if (selectedV.isEmpty()) {
	    return;
	}
	ArrayList<Integer> selNodes = new ArrayList<Integer>();
	Iterator<String> it = selectedV.iterator();
	while (it.hasNext()) {
	    try {
		Integer I = Integer.parseInt(it.next());
		selNodes.add(I);
	    } catch (Exception e) {
		continue;
	    }
	}
	int[] selObjs = new int[selNodes.size()];
	for (int i = 0; i < selObjs.length; i++) {
	    selObjs[i] = selNodes.get(i).intValue();
	}
	fireSelectionChanged(selObjs);
	logger.info(Arrays.toString(selObjs));
    }

    private final class NodeFillColor<V> implements Transformer<V, Paint> {

	protected PickedInfo<V> pi;
	public HashSet<V> kids;

	public NodeFillColor(PickedInfo<V> pi) {
	    this.pi = pi;
	    kids = new HashSet<V>();
	}

	@Override
	public Paint transform(V arg0) {
	    if (pi.isPicked(arg0)) {
		return Color.yellow;
	    } else if (kids.contains(arg0)) {
		return Color.green;
	    } else {
		return Color.red;
	    }
	}

    }

    public static void main(String[] args) {
	JFrame app = new JFrame("DendrogramView");
	DendrogramView dv = new DendrogramView();
	ReadMatrix rm = new ReadMatrix();
	// dv.srm.ReadData("C:/Users/Frank/Desktop/BinaryFile_Shapefile/binaries660.txt");
	app.add(dv);
	app.pack();
	app.setVisible(true);

    }

}
