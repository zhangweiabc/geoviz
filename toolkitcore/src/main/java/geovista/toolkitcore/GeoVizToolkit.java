/*
 Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.

 Original Author: Frank Hardisty

 */

package geovista.toolkitcore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import ncg.statistics.DiscriminantAnalysisGUI;

import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.ext.IndicationConnectUI;

import geovista.animation.IndicationAnimator;
import geovista.animation.SelectionAnimator;
import geovista.cartogram.GeoMapCartogram;
import geovista.collaboration.ComponentProvider;
import geovista.collaboration.GeoJabber;
import geovista.collaboration.GeoJabber.MarshaledComponentListener;
import geovista.common.data.ColumnAppendedBroadcaster;
import geovista.common.data.DataSetBroadcaster;
import geovista.common.data.DataSetForApps;
import geovista.common.data.GeoDataSource;
import geovista.common.data.SpatialWeights;
import geovista.common.event.AnnotationEvent;
import geovista.common.event.AnnotationListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.common.event.TextEvent;
import geovista.common.event.TextListener;
import geovista.common.text.SearchBox;
import geovista.common.ui.NotePad;
import geovista.common.ui.ShapeReporter;
import geovista.common.ui.VariablePicker;
import geovista.coordination.CoordinationManager;
import geovista.coordination.CoordinationUtils;
import geovista.coordination.FiringBean;
import geovista.geoviz.condition.ConditionManager;
import geovista.geoviz.map.GeoMap;
import geovista.geoviz.map.GeoMapUni;
import geovista.geoviz.map.GraduatedSymbolsMap;
import geovista.geoviz.parvis.ParallelPlot;
import geovista.geoviz.radviz.RadViz;
import geovista.geoviz.scatterplot.SingleHistogram;
import geovista.geoviz.scatterplot.SingleScatterPlot;
import geovista.geoviz.spacefill.SpaceFill;
import geovista.geoviz.spreadsheet.TableViewer;
import geovista.geoviz.spreadsheet.VariableTransformer;
import geovista.geoviz.star.StarPlot;
import geovista.geoviz.star.StarPlotMap;
import geovista.largedata.H1N1DataReader;
import geovista.matrix.MapAndScatterplotMatrix;
import geovista.matrix.MapMatrix;
import geovista.matrix.MapScatterplotTreemapMatrix;
import geovista.matrix.MultiplotMatrix;
import geovista.matrix.TreemapAndScatterplotMatrix;
import geovista.matrix.map.LISTAViz;
import geovista.matrix.map.MoranMap;
import geovista.readers.example.GeoData2008Election;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.readers.example.TexasZoonoticDataReader;
import geovista.readers.seerstat.SeerStatReader;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.satscan.Proclude;
import geovista.sound.SonicClassifier;
import geovista.toolkitcore.GvDesktopPane.FrameListener;
import geovista.toolkitcore.data.GeoDataPennaPCA;
import geovista.toolkitcore.data.GeoDataSCarolina;
import geovista.toolkitcore.data.GeoDataSCarolinaCities;
import geovista.toolkitcore.marshal.Marshaler;
import geovista.touchgraph.LinkGraph;
import geovista.touchgraph.PCAViz;
import geovista.touchgraph.SubspaceLinkGraph;

/**
 * Assumptions: 1. One dataset at a time. 2. Maximum coordination as a default.
 * 
 * @author Frank Hardisty
 */

public class GeoVizToolkit extends JFrame implements ActionListener,
		ComponentListener, InternalFrameListener, AnnotationListener,
		ComponentProvider, FrameListener, MarshaledComponentListener,
		IndicationListener, TextListener {

	final static Logger logger = Logger
			.getLogger(GeoVizToolkit.class.getName());
	private static String VERSION_NUM = "0.8.9";

	JComponent regularUI;
	IndicationConnectUI<JComponent> indUI;

	// collection of classes to add
	ArrayList toolMenuList;
	HashMap toolClassHash;

	// collection of active beans
	ToolkitBeanSet tBeanSet;

	// Create JDesktopPane to hold the internal frames
	GvDesktopPane desktop = new GvDesktopPane();

	// managing our layouts

	String filePath = "2008 Presidential Election";
	JFileChooser fileChooser;
	ShapeFileDataReader shpRead;
	ShapeFileProjection shpProj;
	DataSetBroadcaster dataCaster;
	CoordinationManager coord;
	DataSetForApps dataSet;
	Vector<DataSetForApps> backgroundDataSets;
	JMenuBar menuBar;
	JMenu menuFile;
	JMenuItem menuItemLoadShp;

	JMenu menuAddTool;
	JMenuItem menuItemLoadStates;
	JMenuItem menuItemLoadCounty;

	JMenuItem menuItemLoadWorld;
	JMenuItem menuItemLoadDataTable;
	JMenuItem menuItemLoadSCBackgroundShape;
	JMenuItem menuItemImportSeerStatData;

	JMenuItem menuItemLoadCsv;
	JMenuItem menuItemExportData;
	JMenuItem menuItemExportSelection;

	JMenuItem menuItemExitProgram;

	JMenu menuRemoveTool;
	JMenuItem menuItemRemoveAllTools;
	JMenu menuAbout;
	JMenu menuHelp;
	JMenuItem menuItemAboutGeoviz;
	JMenuItem menuItemAboutGeoVista;
	JMenuItem menuItemHelp;
	JMenuItem menuItemOpenProject;
	JMenuItem menuItemSaveProject;
	JMenuItem menuItemExit;
	JMenuItem menuItemCopyApplicationToClipboard;
	JMenuItem menuItemCopySelectedWindowToClipboard;
	JMenuItem menuItemSaveWholeImageToFile;
	JMenuItem menuItemSaveSelectedWindowToFile;
	JMenuItem menuItemSaveShapefile;

	JCheckBoxMenuItem menuItemUseProj;

	VizState vizState;
	// how about svg and postscript?

	GeoVizHelp help;

	boolean useProj;

	static boolean DEBUG = false;

	/*
	 * need to call init after the null ctr as an alternative with
	 * initializaiton, call GeoVizToolkit("")
	 */
	public GeoVizToolkit() {
		super();

		vizState = new VizState();
		init("", false);
	}

	public GeoVizToolkit(String fileNameIn) {
		super("GeoViz Toolkit");
		vizState = new VizState();
		init(fileNameIn, useProj);
	}

	private GeoVizToolkit(String fileNameIn, boolean useProj, boolean useAux) {

		super("GeoViz Toolkit");
		vizState = new VizState();
		init(fileNameIn, useProj);
	}

	private void initMembers() {
		// VisualSettingsPopupMenu popMenu = new VisualSettingsPopupMenu(this);
		// MouseAdapter listener = new VisualSettingsPopupAdapter(popMenu);
		// popMenu.addMouseListener(listener);
		// addMouseListener(listener);

		// addMouseMotionListener(desktop);

		// we are replacing with JXLayer for now
		// addImpl(glassPane, null, 0);
		// desktop.add(glassPane, null, 0);

		// addMouseMotionListener(glassPane);
		// desktop.addMouseMotionListener(glassPane);
		// collection of classes to add
		toolMenuList = new ArrayList();
		toolClassHash = new HashMap();

		// Create JDesktopPane to hold the internal frames
		desktop = new GvDesktopPane();
		desktop.setBackground(Color.white);
		desktop.parentKit = this;

		// addBindings();
		// managing our layouts
		filePath = "48States";

		shpRead = new ShapeFileDataReader();
		shpProj = new ShapeFileProjection();
		dataCaster = new DataSetBroadcaster();
		coord = new CoordinationManager();
		coord.addBean(this);// to catch indications

		menuBar = new JMenuBar();
		menuFile = new JMenu();
		menuItemLoadShp = new JMenuItem();

		menuAddTool = new JMenu();
		menuItemLoadStates = new JMenuItem();
		menuItemLoadCounty = new JMenuItem();
		menuItemExitProgram = new JMenuItem();
		menuItemLoadWorld = new JMenuItem();

		menuItemLoadDataTable = new JMenuItem();
		menuItemLoadSCBackgroundShape = new JMenuItem();
		menuItemImportSeerStatData = new JMenuItem();
		menuItemLoadCsv = new JMenuItem();
		menuItemExportData = new JMenuItem();
		menuItemExportSelection = new JMenuItem();

		menuRemoveTool = new JMenu();
		menuItemRemoveAllTools = new JMenuItem();
		menuAbout = new JMenu();
		menuHelp = new JMenu();
		menuItemAboutGeoviz = new JMenuItem();
		menuItemAboutGeoVista = new JMenuItem();
		menuItemHelp = new JMenuItem();
		menuItemOpenProject = new JMenuItem();
		menuItemSaveProject = new JMenuItem();
		menuItemExit = new JMenuItem();
		menuItemCopyApplicationToClipboard = new JMenuItem();
		menuItemCopySelectedWindowToClipboard = new JMenuItem();
		menuItemSaveWholeImageToFile = new JMenuItem();
		menuItemSaveSelectedWindowToFile = new JMenuItem();
		menuItemSaveShapefile = new JMenuItem();

		menuItemUseProj = new JCheckBoxMenuItem();

	}

	void setIndicationUI(boolean indicationUI) {
		if (indicationUI) {
			indUI.paintIndication = true;
		} else {
			indUI.paintIndication = false;
		}
	}

	public void init(String fileNameIn, boolean useProj) {

		initMembers();
		desktop.setBackground(new Color(20, 20, 80));
		setUseProj(useProj);
		JXLayer<JComponent> layer = new JXLayer<JComponent>(desktop);
		indUI = new IndicationConnectUI<JComponent>();
		getContentPane().add(layer, BorderLayout.CENTER);
		layer.setUI(indUI);

		setIndicationUI(false);

		coord.addBean(dataCaster);
		coord.addBean(vizState);
		try {
			this.init();
			initMenuListeners();
		} catch (Exception ex1) {
			ex1.printStackTrace();
		}

		if (fileNameIn == null || fileNameIn.equals("")) {
			fileNameIn = "C:\\data\\grants\\NCI 07\\Files_for_Frank\\Health_Care_2000_Smaller.shp";
			// fileNameIn = "C:\\data\\grants\\NCI
			// 07\\Files_for_Frank\\Health_Care_2000_Smaller.shp";
			fileNameIn = filePath;

		}

		// loadData(fileNameIn);

		URL urlGif = null;

		ImageIcon im = null;
		try {
			Class cl = this.getClass();
			urlGif = cl.getResource("resources/geoviz_toolkit32.gif");
			im = new ImageIcon(urlGif);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		setIconImage(im.getImage());
		pack();
		setVisible(true);

		setExtendedState(Frame.MAXIMIZED_BOTH);

		// if we are coming up from xml format, this might have already been
		// set.
		if (vizState.beanSet == null) {
			VizState state = ToolkitIO.openDefaultLayout();
			setVizState(state);
		}

		this.repaint();
	}

	void openAllComponents() {
		removeAllBeans();
		VizState state = ToolkitIO.openAllComponentsLayout();
		setVizState(state);
		this.repaint();
	}

	public VizState getVizState() {
		updateVizState();
		return vizState;
	}

	private void updateVizState() {
		vizState.setBeanSet(tBeanSet);
		HashSet<ToolkitBean> beans = tBeanSet.getBeanSet();
		for (ToolkitBean tBean : beans) {
			tBean.zOrder = desktop.getComponentZOrder(tBean.getInternalFrame());
		}
		ToolkitBean tBean = tBeanSet.getToolkitBean(desktop.getSelectedFrame());
		if (tBean == null) {
			vizState.setSelectedBean("");
		} else {
			vizState.setSelectedBean(tBean.getUniqueName());
		}

	}

	void saveProgramState() {
		updateVizState();
		ToolkitIO.saveVizStateToFile(vizState);
	}

	public void addExternalBean(Object bean) {
		coord.addBean(bean);

		ColumnAppendedBroadcaster localCaster = (ColumnAppendedBroadcaster) bean;

		localCaster.setDataSet(dataSet);
	}

	/*
	 * deleting all beans
	 */

	private void removeAllBeans() {
		if (tBeanSet == null) {
			return;
		}
		Iterator it = tBeanSet.getBeanSet().iterator();
		while (it.hasNext()) {
			ToolkitBean oldBean = (ToolkitBean) it.next();
			removeBeanFromGui(oldBean);
			oldBean = null;
		}
		tBeanSet.clear();

	}

	/*
	 * deleting named bean
	 */

	private void deleteBean(ToolkitBean oldBean) {
		removeBeanFromGui(oldBean);
		tBeanSet.remove(oldBean);
		oldBean = null;
	}

	/*
	 * cleanup 1. remove item from remove menu 2. remove from map of beans 3.
	 * remove from coordination 4. remove from GUI (should be no more references
	 * at this point, besides in tBeanSet)
	 */
	private void removeBeanFromGui(ToolkitBean oldBean) {
		JMenuItem item = oldBean.getRemoveMenuItem();
		item.removeActionListener(this);
		menuRemoveTool.remove(item);
		coord.removeBean(oldBean.getOriginalBean());

		// find area in layout occupied by internal frame
		JInternalFrame iFrame = oldBean.getInternalFrame();
		int iX = iFrame.getX();
		int iY = iFrame.getY();
		int iWidth = iFrame.getWidth();
		int iHeight = iFrame.getHeight();
		// remove it, then repaint area
		desktop.remove(oldBean.getInternalFrame());
		desktop.repaint(iX, iY, iWidth, iHeight);

	}

	private static Object makeObject(String className) {
		Object obj = null;
		try {

			Class beanClass = Class.forName(className);
			obj = beanClass.newInstance();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return obj;
	}

	/*
	 * one reason this method is called when a bean is instantiated because the
	 * user has clicked on the appropriate menu item. If a bean is instantiated
	 * because we have loaded a layout, then the method in ToolkitIO is used.
	 */
	private ToolkitBean instantiateBean(String className) {

		ToolkitBean newToolkitBean = null;

		Object newBean = GeoVizToolkit.makeObject(className);
		FiringBean newFBean = coord.addBean(newBean);
		String uniqueName = newFBean.getBeanName();

		if (className.equals(GeoJabber.class.getName())) {
			GeoJabber jab = (GeoJabber) newBean;
			jab.setCompProvider(this);
			jab.addMarshaledComponentListener(this);
		}

		newToolkitBean = new ToolkitBean(newBean, uniqueName);

		return newToolkitBean;
	}

	public void setVizState(VizState newState) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		removeAllBeans();
		coord.removeBean(vizState);

		vizState = newState;
		// may be a problem when connections can be cusomized by the user

		vizState.listenerList = new EventListenerList();
		coord.addBean(vizState);

		setUseProj(vizState.useProj);

		loadData(vizState.dataSource);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		addToolkitBeanSet(vizState.getBeanSet());
		HashSet<ToolkitBean> beans = tBeanSet.getBeanSet();
		for (ToolkitBean tBean : beans) {
			desktop.setComponentZOrder(tBean.getInternalFrame(), tBean.zOrder);
		}

		String uniqueName = vizState.getSelectedBean();
		ToolkitBean tBean = tBeanSet.getToolkitBean(uniqueName);
		if (tBean != null) {
			desktop.setSelectedFrame(tBean.getInternalFrame());

		}
		Object newBean = tBean.getOriginalBean();
		fireNewBeanMethods(newBean);
		setCursor(Cursor.getDefaultCursor());
	}

	/*
	 * Adds bean to the main gui area, also to the remove bean menu
	 */
	private void addBeanToGui(ToolkitBean newToolkitBean) {

		desktop.add(newToolkitBean.getInternalFrame(), 0); // add on top
		newToolkitBean.addComponentListener(this);
		newToolkitBean.getInternalFrame().addInternalFrameListener(this);
		JMenuItem item = newToolkitBean.getRemoveMenuItem();
		item.addActionListener(this);

		menuRemoveTool.add(item, 0); // add at the top

		if (newToolkitBean.getOriginalBean().getClass().equals(GeoMap.class)) {
			logger.info("geo geo");

		}

	}

	void pinBean(ToolkitBean beanToPin) {

		Point location = beanToPin.getInternalFrame().getLocation();
		int index = desktop.getIndexOf(beanToPin.getInternalFrame());
		Dimension size = beanToPin.getInternalFrame().getSize();
		desktop.remove(beanToPin.getInternalFrame());
		Component comp = (Component) beanToPin.getOriginalBean();
		desktop.add(comp, index);
		comp.setSize(size);
		comp.setLocation(location);

	}

	void unPinBean(ToolkitBean unPinBean) {
		Component comp = (Component) unPinBean.getOriginalBean();
		Point location = comp.getLocation();
		int index = desktop.getIndexOf(comp);
		Dimension size = comp.getSize();
		desktop.remove(comp);
		JInternalFrame fram = unPinBean.getInternalFrame();
		fram.add(comp);
		desktop.add(fram, index);
		fram.setSize(size);
		fram.setLocation(location);
	}

	private void fireNewBeanMethods(Object newBean) {
		if (dataSet == null || dataSet.getDataObjectOriginal() == null) {
			logger.severe("data set is null");
			return;
		}
		fireDataSetToNewBean(newBean);
		fireOtherNewBeanMethods(newBean);

	}

	private void fireDataSetToNewBean(Object newBean) {
		if (newBean instanceof DataSetListener) {
			DataSetListener dataListener = (DataSetListener) newBean;
			dataListener.dataSetChanged(new DataSetEvent(dataSet, this));

		}
	}

	private void fireOtherNewBeanMethods(Object newBean) {
		if (newBean instanceof SubspaceListener) {
			SubspaceListener selListener = (SubspaceListener) newBean;
			SubspaceEvent e = vizState.getSubspaceEvent();
			if (e.getSubspace() != null) {
				selListener.subspaceChanged(e);
			}
		}

		if (newBean instanceof SelectionListener) {
			SelectionListener selListener = (SelectionListener) newBean;
			SelectionEvent e = vizState.getSelectionEvent();
			if (e.getSelection() != null) {
				selListener.selectionChanged(e);
			}
		}
		if (newBean instanceof IndicationListener) {
			IndicationListener indListener = (IndicationListener) newBean;
			IndicationEvent e = vizState.getIndicationEvent();
			indListener.indicationChanged(e);
		}
	}

	private void addToolkitBeanSet(ToolkitBeanSet beanSet) {
		logger.fine(" addtoolkitbeanset setting tbeanset, size = "
				+ beanSet.getBeanSet().size());
		tBeanSet = beanSet;
		Iterator iter = tBeanSet.iterator();
		while (iter.hasNext()) {
			ToolkitBean tBean = (ToolkitBean) iter.next();
			Object obj = tBean.getOriginalBean();
			coord.addBean(obj);
			addBeanToGui(tBean);
			Object newBean = tBean.getOriginalBean();
			fireNewBeanMethods(newBean);
		}

	}

	// private void dataSetChanged(DataSetEvent e) {
	// dataSet = e.getDataSetForApps();
	// }

	private void showHelp() {
		// lazy initialize
		if (help == null) {
			help = new GeoVizHelp();
			help.pack();
		}
		boolean haveHelp = false;
		Component[] desktopComponents = desktop.getComponents();
		for (Component element : desktopComponents) {
			if (element == help) {
				desktop.remove(element);
				desktop.add(element, 0);
				haveHelp = true;
			}
		}
		if (!haveHelp) {
			logger.finest("GVToolkit, showhelp, adding to desktop");
			desktop.add(help, 0); // add on top
		}
		this.repaint();

	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == menuItemAboutGeoVista) {

			JOptionPane.showMessageDialog(this,
					"Most components in this toolkit "
							+ "developed at the GeoVISTA Center.");

		} else if (e.getSource() == menuItemAboutGeoviz) {

			JOptionPane
					.showMessageDialog(
							this,
							"<html>"
									+ "GeoViz Toolkit, Version "
									+ VERSION_NUM
									+ "<br>This application developed by Frank Hardisty "
									+ "<br>with contributions by many others. "
									+ "<br> Contributor list here: "
									+ "<br> http://code.google.com/p/geoviz/wiki/GeoVizToolkitContributors </html>");
		} else if (e.getSource() == menuItemHelp) {
			showHelp();
		} else if (e.getSource() == menuItemExit) {
			// is this too weak? do we need a system exit?
			setVisible(false);
		} else if (e.getSource() == menuItemLoadCounty) {
			loadData("2008 Presidential Election");
		} else if (e.getSource() == menuItemExitProgram) {
			logger.info("User quit from menu item, bye!");
			System.exit(0);

		} else if (e.getSource() == menuItemLoadShp) {
			openShapefilePicker();
		} else if (e.getSource() == menuItemUseProj) {
			useProj = menuItemUseProj.getState();
		} else if (e.getSource() == menuItemLoadStates) {
			loadData("48States");
		} else if (e.getSource() == menuItemLoadWorld) {
			loadData("GTD");

		} else if (e.getSource() == menuItemLoadDataTable) {
			openExcelFilePicker();
		} else if (e.getSource() == menuItemLoadSCBackgroundShape) {
			loadBackgroundData("SC");
		} else if (e.getSource() == menuItemImportSeerStatData) {
			openSeerStatFilePicker();
		} else if (e.getSource() == menuItemLoadCsv) {
			openCsvFilePicker();

		} else if (e.getSource() == menuItemExportData) {

		} else if (e.getSource() == menuItemExportSelection) {

		}

		else if (e.getSource() == menuItemOpenProject) {
			VizState state = ToolkitIO.getVizState(this);

			if (state == null) {
				return;
			}

			logger.info("about to set VizState");
			setVizState(state);
			logger.info("set VizState");

		} else if (e.getSource() == menuItemSaveProject) {
			Marshaler marsh = Marshaler.INSTANCE;
			String xml = marsh.toXML(getVizState());
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest(xml);
			}
			ToolkitIO.writeLayout(xml, this);
			// ToolkitIO.writeLayout(getFileName(), tBeanSet, this);

		} else if (toolClassHash.containsKey(e.getSource())) {
			createAndAddBean(e);
		}

		else if (e.getSource() == menuItemRemoveAllTools) {
			removeAllBeans();
		} else if (e.getSource() instanceof JMenuItem
				&& tBeanSet.contains((JMenuItem) e.getSource())) {
			JMenuItem item = (JMenuItem) e.getSource();

			ToolkitBean oldTool = tBeanSet.getToolkitBean(item);
			// assert (item != null);
			deleteBean(oldTool); // deleteBean calls removeBeanFromGUI
			oldTool = null; // or should we just let it go out of scope? or do
			// we need to do more?

		} else if (e.getSource() == menuItemCopyApplicationToClipboard) {

			ToolkitIO.copyComponentImageToClipboard(this);
		} else if (e.getSource() == menuItemCopySelectedWindowToClipboard) {
			JInternalFrame frame = desktop.getSelectedFrame();
			if (frame != null) {
				ToolkitIO.copyComponentImageToClipboard(frame);
			}

		} else if (e.getSource() == menuItemSaveWholeImageToFile) {

			ToolkitIO.saveImageToFile(this);
		} else if (e.getSource() == menuItemSaveSelectedWindowToFile) {
			JInternalFrame frame = desktop.getSelectedFrame();
			if (frame != null) {
				ToolkitIO.saveImageToFile(frame);
			}
		} else if (e.getSource() == menuItemSaveShapefile) {
			// BufferedImage buff = new BufferedImage(desktop.getWidth(),
			// desktop
			// .getHeight(), BufferedImage.TYPE_INT_ARGB);
			// Graphics g = buff.getGraphics();
			// desktop.paint(g);

			ToolkitIO.saveShapeFile(this, dataSet);
			// GEXClient.uploadScreenshot(buff, "new test");

		}

	}

	private void createAndAddBean(ActionEvent e) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		String className = (String) toolClassHash.get(e.getSource());
		ToolkitBean tBean = null;
		tBean = instantiateBean(className);
		addBeanToGui(tBean);
		Object newBean = tBean.getOriginalBean();
		fireNewBeanMethods(newBean);
		tBeanSet.add(tBean);
		setCursor(Cursor.getDefaultCursor());

	}

	private void loadAdditionalTable(String name) {
		Object[] newDataSet = null;

		try {
			newDataSet = createData(name);
		} catch (IOException e) {
			JOptionPane
					.showMessageDialog(this, "Sorry, could not load " + name);

			e.printStackTrace();
		}
		DataSetForApps auxData = new DataSetForApps(newDataSet);
		Object viewer = GeoVizToolkit.makeObject(TableViewer.class.getName());
		ToolkitBean newToolkitBean = new ToolkitBean(viewer, "viewer");
		TableViewer tv = (TableViewer) viewer;
		tv.setDataSet(auxData);
		addBeanToGui(newToolkitBean);

	}

	private void loadBackgroundData(String name) {
		Object[] newDataSet = null;

		try {
			newDataSet = createData(name);
		} catch (IOException e) {
			JOptionPane
					.showMessageDialog(this, "Sorry, could not load " + name);

			e.printStackTrace();
		}
		DataSetForApps auxData = new DataSetForApps(newDataSet);
		dataCaster.fireAuxiliaryDataSet(auxData);
	}

	public void setDataSet(DataSetForApps dataSet) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		this.repaint();

		dataCaster.setAndFireDataSet(dataSet);

		setCursor(Cursor.getDefaultCursor());

	}

	private void loadData(String name) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		this.repaint();
		vizState.setDataSource(name);
		filePath = name;
		logger.fine("geovizdemo, loadData, dataSetIsNull =" + (dataSet == null));
		Object[] newDataSet = null;
		// name = "H1N1 Patterns";

		try {
			newDataSet = createData(name);
		} catch (IOException e) {
			JOptionPane
					.showMessageDialog(this, "Sorry, could not load " + name);

			e.printStackTrace();
		}

		logger.fine("geovizdemo, loadData, dataSetIsNull =" + (dataSet == null));
		dataSet = new DataSetForApps(newDataSet);
		File newFile = new File(name);
		if (newFile.canRead()) {
			logger.info(name);
			dataSet.setDataSourceName(name);

		} else {
			dataSet.setDataSourceName("");
		}
		dataCaster.setAndFireDataSet(dataSet);
		logger.fine("geovizdemo, loadData, dataSetIsNull =" + (dataSet == null));
		setCursor(Cursor.getDefaultCursor());

	}

	H1N1DataReader reader = null;

	private Object[] createData(String name) throws IOException {
		setTitle("GeoViz Toolkit -- " + name);

		if (name == null) {
			return null;
		}
		// if (name != null) {
		// reader = new H1N1DataReader();
		//
		// return reader.getDataForApps().getDataObjectOriginal();
		// }

		logger.fine("creating data: " + name);
		Object[] newDataSet = null;
		if (name.equals("Cleveland Crime Patterns")) {

			return ToolkitIO.getClevelandData().getDataObjectOriginal();
		}

		// name = "TX";
		if (name.equals("48States") || name.equals("USCounties")) {

			GeoDataGeneralizedStates statesData = new GeoDataGeneralizedStates();
			// TexasZoonoticDataReader statesData = new
			// TexasZoonoticDataReader();

			// DCCrimeDataReader crimeReader = new DCCrimeDataReader();
			// newDataSet =
			// crimeReader.getDataForApps().getDataObjectOriginal();

			// GoogleFluDataReader statesData = new GoogleFluDataReader();
			ShapeFileProjection proj = new ShapeFileProjection();
			proj.setInputDataSetForApps(statesData.getDataForApps());
			newDataSet = proj.getOutputDataSet();

			// geovista.largedata.GeoDataNiger statesData = new
			// geovista.largedata.GeoDataNiger();

			// newDataSet = statesData.getDataForApps().getDataObjectOriginal();

			// proj.setInputDataSet(countyData.getDataSet());

		} else if (name.equals("TX")) {
			TexasZoonoticDataReader statesData = new TexasZoonoticDataReader();

			ShapeFileProjection proj = new ShapeFileProjection();
			proj.setInputDataSetForApps(statesData.getDataForApps());
			newDataSet = proj.getOutputDataSet();
		}

		else if (name.equals("NZ")) {
			String className = "geovista.largedata.GeoDataNZ";
			newDataSet = geoDataFromName(className);
		}

		else if (name.equals("GTD")) {
			String className = "geovista.largedata.GTDReader";
			newDataSet = geoDataFromName(className);
		} else if (name.equals("Purdue")) {
			String className = "geovista.largedata.PurdueDataReader";
			newDataSet = geoDataFromName(className);
		} else if (name.equals("SC")) {
			GeoDataSCarolina carolinaData = new GeoDataSCarolina();
			newDataSet = carolinaData.getDataSet();

		} else if (name.equals("SCCities")) {
			GeoDataSCarolinaCities carolinaData = new GeoDataSCarolinaCities();
			newDataSet = carolinaData.getDataSet();

		} else if (name.equals("World")) {
			// GeoDataWorld WorldData = new GeoDataWorld();
			// newDataSet = WorldData.getDataSet();

		} else if (name.equals("nctc")) {
			// GeoDataNCTC cartogramData = new GeoDataNCTC();
			// newDataSet = cartogramData.getDataSet();

		} else if (name.equals("USCounties")) {
			TexasZoonoticDataReader statesData = new TexasZoonoticDataReader();
			ShapeFileProjection proj = new ShapeFileProjection();
			proj.setInputDataSetForApps(statesData.getDataForApps());
			newDataSet = proj.getOutputDataSet();

			// GeoData2008Election countyData = new GeoData2008Election();
			// ShapeFileProjection proj = new ShapeFileProjection();
			// proj.setInputDataSet(countyData.getDataSet());

			newDataSet = proj.getOutputDataSet();
		} else if (name.equals("2008 Presidential Election")) {

			GeoData2008Election countyData = new GeoData2008Election();
			ShapeFileProjection proj = new ShapeFileProjection();
			proj.setInputDataSet(countyData.getDataSet());

			SpatialWeights sw = proj.getOutputDataSetForApps()
					.getSpatialWeights();

			newDataSet = proj.getOutputDataSet();

		}

		else if (name.equals("Niger")) {
			String className = "geovista.largedata.GeoDataNiger";

			newDataSet = geoDataFromName(className);
		}

		else if (name.equals("PennaPCA")) {
			// vestigial, please remove when appropriate
			GeoDataPennaPCA pennaPCAData = new GeoDataPennaPCA();
			// GeoDataGeneralizedStates pennaPCAData = new
			// GeoDataGeneralizedStates();
			newDataSet = pennaPCAData.getDataSet();

		} else {
			shpRead.setFileName(name);
			Object[] newDataArray = shpRead.getDataSet();
			if (newDataArray == null) {
				logger.severe("could not read:" + name);
				return null;
			}
			if (useProj && shpRead.getDataForApps().getShapeData() != null) {
				shpProj.setInputDataSet(shpRead.getDataSet());
				newDataSet = shpProj.getOutputDataSet();
			} else {

				newDataSet = shpRead.getDataSet();

			}
		}

		return newDataSet;

	}

	private Object[] geoDataFromName(String className) {
		Object[] newDataSet;
		Class nigerClass;
		Object nigerInstance = null;
		try {
			nigerClass = Class.forName(className);
			nigerInstance = nigerClass.newInstance();
		} catch (Exception e) {
			logger.severe("could not find geoData from class " + className);
			e.printStackTrace();
		}
		GeoDataSource dataSource = (GeoDataSource) nigerInstance;

		newDataSet = dataSource.getDataForApps().getDataObjectOriginal();
		return newDataSet;
	}

	private void openShapefilePicker() {

		String fileName = ToolkitIO.getFileName(this, ToolkitIO.Action.OPEN,
				ToolkitIO.FileType.SHAPEFILE);
		if (fileName == null) {
			return;// user hit cancel
		}
		loadData(fileName);
	}

	private void openExcelFilePicker() {
		String fileName = ToolkitIO.getFileName(this, ToolkitIO.Action.OPEN,
				ToolkitIO.FileType.SHAPEFILE);

		// loadBackgroundData(fileName);
		loadAdditionalTable(fileName);
		logger.finest("Background name = " + fileName);

	}

	private void openSeerStatFilePicker() {
		String fileName = ToolkitIO.getFileName(this, ToolkitIO.Action.OPEN,
				ToolkitIO.FileType.SEERSTAT);
		if (fileName == null || fileName.equals("")) {
			return;
		}
		SeerStatReader reader = new SeerStatReader();
		reader.setDicFileLocation(fileName);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		this.repaint();
		dataSet = reader.readFiles();
		dataCaster.setAndFireDataSet(dataSet);

		setCursor(Cursor.getDefaultCursor());
	}

	private void openCsvFilePicker() {
		String fileName = ToolkitIO.getFileName(this, ToolkitIO.Action.OPEN,
				ToolkitIO.FileType.CSV);
		joinCsvData(fileName);

	}

	private void joinCsvData(String fileName) {
		logger.finest(fileName);
	}

	/*
	 * flow of tool added to menu: add JMenuItem with appropriate name and icon
	 * to menu add app as a listener to JMenuItem add entry in ListArray of
	 * added menu tools
	 */
	private void addToolToMenu(Class tool) {

		String className = tool.getName();
		int place = className.lastIndexOf(".") + 1;
		int len = className.length();
		String shortName = className.substring(place, len);
		Image im = CoordinationUtils.findSmallIcon(tool);
		Icon ic = new ImageIcon(im);
		JMenuItem menuItem = new JMenuItem(shortName, ic);
		menuAddTool.add(menuItem);
		menuItem.addActionListener(this);
		toolClassHash.put(menuItem, className);
		toolMenuList.add(menuItem);

	}

	/*
	 * There is no corresponding setter, because it would duplicate
	 * loadData(String)
	 */
	public String getFileName() {
		return filePath;
	}

	// start component event handling
	public void componentHidden(ComponentEvent e) {
		if (e.getSource() instanceof JInternalFrame
				&& tBeanSet.contains((JInternalFrame) e.getSource())) {
			deleteBean(tBeanSet.getToolkitBean((JInternalFrame) e.getSource()));

		}

	}

	public void componentMoved(ComponentEvent e) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("moved");
		}
	}

	public void componentShown(ComponentEvent e) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("shown");
		}

	}

	public void componentResized(ComponentEvent e) {

		if (logger.isLoggable(Level.FINEST)) {
			if (e.getSource() instanceof JInternalFrame
					&& tBeanSet.contains((JInternalFrame) e.getSource())) {
				JInternalFrame iFrame = (JInternalFrame) e.getSource();
				int width = iFrame.getWidth();
				int height = iFrame.getHeight();
				logger.finest("iFrame.width = " + width + ",iFrame.height"
						+ height);

			}

		}
		logger.info("Width = " + getWidth() + ", Height = " + getHeight());

	}

	// end component event handling
	private void initMenuListeners() {

		menuItemAboutGeoVista.addActionListener(this);
		menuItemAboutGeoviz.addActionListener(this);
		menuItemExit.addActionListener(this);
		menuItemLoadCounty.addActionListener(this);
		menuItemExitProgram.addActionListener(this);
		menuItemLoadShp.addActionListener(this);
		menuItemLoadWorld.addActionListener(this);

		menuItemLoadDataTable.addActionListener(this);
		menuItemLoadSCBackgroundShape.addActionListener(this);
		menuItemImportSeerStatData.addActionListener(this);
		menuItemLoadCsv.addActionListener(this);
		menuItemExportData.addActionListener(this);
		menuItemExportSelection.addActionListener(this);

		menuItemLoadStates.addActionListener(this);
		menuItemOpenProject.addActionListener(this);
		menuItemSaveProject.addActionListener(this);
		menuItemRemoveAllTools.addActionListener(this);
		menuItemHelp.addActionListener(this);
		menuItemSaveWholeImageToFile.addActionListener(this);
		menuItemSaveSelectedWindowToFile.addActionListener(this);
		menuItemSaveShapefile.addActionListener(this);
		menuItemCopyApplicationToClipboard.addActionListener(this);
		menuItemCopySelectedWindowToClipboard.addActionListener(this);

		menuItemUseProj.addActionListener(this);

		// components are organized by dimension of analysis
		// then by commonality of usage

		menuAddTool.add(new JLabel(" ~~~~~ Data Handling ~~~~~ "));
		addToolToMenu(VariablePicker.class);
		addToolToMenu(TableViewer.class);
		addToolToMenu(VariableTransformer.class);
		menuAddTool.addSeparator();

		menuAddTool.add(new JLabel(" ~~~~~ Univariate Visualization ~~~~~ "));
		addToolToMenu(SingleHistogram.class);
		addToolToMenu(GeoMapUni.class);
		addToolToMenu(SpaceFill.class);
		addToolToMenu(SonicClassifier.class);
		addToolToMenu(GeoMapCartogram.class);
		menuAddTool.addSeparator();

		menuAddTool.add(new JLabel(" ~~~~~ Bivariate Visualization ~~~~~ "));
		addToolToMenu(SingleScatterPlot.class);
		addToolToMenu(GeoMap.class);
		menuAddTool.addSeparator();

		menuAddTool.add(new JLabel(" ~~~~~ Multivariate Visualization ~~~~~ "));
		addToolToMenu(LinkGraph.class);
		addToolToMenu(ParallelPlot.class);
		addToolToMenu(StarPlot.class);
		addToolToMenu(StarPlotMap.class);
		addToolToMenu(GraduatedSymbolsMap.class);
		addToolToMenu(RadViz.class);
		// addToolToMenu(TreeMap.class);
		menuAddTool.addSeparator();

		menuAddTool.add(new JLabel(
				" ~~~~~ Multiform Matrix Visualization ~~~~~ "));

		addToolToMenu(MapAndScatterplotMatrix.class);
		addToolToMenu(TreemapAndScatterplotMatrix.class);
		addToolToMenu(MapScatterplotTreemapMatrix.class);
		addToolToMenu(MapMatrix.class);
		addToolToMenu(MultiplotMatrix.class);
		// addToolToMenu(CartogramAndScatterplotMatrix.class);
		// addToolToMenu(CartogramMatrix.class);
		menuAddTool.addSeparator();

		menuAddTool.add(new JLabel(" ~~~~~ Dimensional Reduction ~~~~~ "));
		addToolToMenu(PCAViz.class);
		addToolToMenu(SubspaceLinkGraph.class);
		menuAddTool.addSeparator();

		menuAddTool.add(new JLabel(" ~~~~~ Animation and Conditioning ~~~~~ "));
		addToolToMenu(SelectionAnimator.class);
		addToolToMenu(IndicationAnimator.class);
		addToolToMenu(ConditionManager.class);
		menuAddTool.addSeparator();

		menuAddTool.add(new JLabel(
				" ~~~~~ Spatial Analysis and Clustering ~~~~~ "));
		addToolToMenu(LISTAViz.class);
		addToolToMenu(MoranMap.class);
		addToolToMenu(Proclude.class);
		addToolToMenu(DiscriminantAnalysisGUI.class);
		menuAddTool.addSeparator();

		menuAddTool.add(new JLabel(" ~~~~~ Collaboration and Text ~~~~~ "));
		addToolToMenu(GeoJabber.class);
		addToolToMenu(NotePad.class);
		addToolToMenu(SearchBox.class);

	}

	private void init() throws Exception {
		desktop.fListener = this;
		setJMenuBar(menuBar);
		menuFile.setText("File");
		menuItemLoadShp.setText("Load Shapefile");
		menuAddTool.setText("Add Tool");
		menuItemLoadStates.setText("Load DC Crime Data");
		menuItemLoadCounty.setText("Load U.S. County Data");
		menuItemLoadWorld.setText("Load GTD Data");

		menuRemoveTool.setText("Remove Tool");
		menuAbout.setText("About");
		menuHelp.setText("Help");
		menuItemAboutGeoviz.setText("About GeoViz Toolkit");
		menuItemAboutGeoVista.setText("About GeoVISTA Studio");
		menuItemHelp.setText("Tutorial");
		menuItemOpenProject.setText("Open Project");
		menuItemSaveProject.setText("Save Project");
		menuItemLoadDataTable.setText("Load Data Table");
		menuItemLoadSCBackgroundShape.setText("Load South Carolina Background");
		menuItemImportSeerStatData.setText("Change Default Data");
		menuItemLoadCsv.setText("Load Default Data: " + filePath);
		menuItemExportData.setText("Export Data Set");
		menuItemExportSelection.setText("Export Selected Rows and Columns");

		menuItemExitProgram.setText("Exit GeoViz Toolkit");

		menuItemRemoveAllTools.setText("Remove All Tools");
		menuItemCopyApplicationToClipboard
				.setText("Copy image of main window to clipboard");
		menuItemCopySelectedWindowToClipboard
				.setText("Copy image of currently selected window to clipboard");
		menuItemSaveWholeImageToFile
				.setText("Export image of main window to file");
		menuItemSaveSelectedWindowToFile
				.setText("Export image of currently selected window to file");

		menuItemSaveShapefile.setText("Export data to shapefile");

		menuItemUseProj.setText("Use U.S. Projection");
		menuItemUseProj.setState(true);

		menuBar.add(menuFile);
		menuBar.add(menuAddTool);
		menuBar.add(menuRemoveTool);
		menuBar.add(menuAbout);
		menuBar.add(menuHelp);

		menuFile.add(menuItemLoadShp);
		menuFile.add(menuItemUseProj);
		menuFile.addSeparator();

		menuFile.add(menuItemLoadDataTable);

		menuFile.addSeparator();
		menuFile.add(menuItemLoadStates);
		menuFile.add(menuItemLoadCounty);

		menuFile.add(menuItemLoadWorld);

		// menuFile.addSeparator();
		// menuFile.add(menuItemLoadCsv);
		// menuFile.add(menuItemImportSeerStatData);

		// menuFile.addSeparator();
		// menuFile.add(menuItemExportData);
		// menuFile.add(menuItemExportSelection);
		menuFile.addSeparator();
		menuFile.add(menuItemCopyApplicationToClipboard);
		menuFile.add(menuItemCopySelectedWindowToClipboard);
		menuFile.add(menuItemSaveWholeImageToFile);
		menuFile.add(menuItemSaveSelectedWindowToFile);
		menuFile.addSeparator();
		menuFile.add(menuItemSaveShapefile);
		menuFile.addSeparator();
		menuFile.add(menuItemOpenProject);
		menuFile.add(menuItemSaveProject);
		menuFile.addSeparator();

		menuFile.add(menuItemExitProgram);

		menuAbout.add(menuItemAboutGeoviz);
		menuAbout.add(menuItemAboutGeoVista);
		menuHelp.add(menuItemHelp);
		menuRemoveTool.addSeparator();
		menuRemoveTool.add(menuItemRemoveAllTools);
	}

	public ToolkitBeanSet getTBeanSet() {
		return tBeanSet;
	}

	public void setTBeanSet(ToolkitBeanSet beanSet) {
		logger.fine("settbeanset setting tbeanset, size = "
				+ beanSet.getBeanSet().size());
		tBeanSet = beanSet;
	}

	public void internalFrameActivated(InternalFrameEvent arg0) {
		// no-op

	}

	public void internalFrameClosed(InternalFrameEvent arg0) {
		// no-op
	}

	public void internalFrameClosing(InternalFrameEvent arg0) {
		JInternalFrame iFrame = arg0.getInternalFrame();
		ToolkitBean tBean = tBeanSet.getToolkitBean(iFrame);

		deleteBean(tBean);

	}

	public void internalFrameDeactivated(InternalFrameEvent arg0) {
		// no-op

	}

	public void internalFrameDeiconified(InternalFrameEvent arg0) {
		// no-op

	}

	public void internalFrameIconified(InternalFrameEvent arg0) {
		// no-op

	}

	public void internalFrameOpened(InternalFrameEvent arg0) {
		// no-op

	}

	public boolean isUseProj() {
		return useProj;
	}

	public void setUseProj(boolean useProj) {
		this.useProj = useProj;
		menuItemUseProj.setState(useProj);
	}

	public void annotationChanged(AnnotationEvent e) {

	}

	public static void main(String[] args) {

		Logger logger = Logger.getLogger("geovista");
		// Logger mapLogger = Logger.getLogger("geovista.geoviz.map.MapCanvas");
		logger.setLevel(Level.INFO);
		// mapLogger.setLevel(Level.FINEST);
		// LogManager mng = LogManager.getLogManager();
		// mng.addLogger(logger);
		// mng.addLogger(mapLogger);

		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.INFO);
		// logger.addHandler(handler);
		logger.info(System.getProperty("java.version"));
		try {
			// Create a file handler that write log record to a file called
			// my.log
			FileHandler fHandler = new FileHandler("gv_toolkit.log");
			fHandler.setFormatter(new SimpleFormatter());
			logger.addHandler(fHandler);
		} catch (IOException e) {
			System.err.println("could not create log file");
			e.printStackTrace();
		}

		boolean useProj = false;
		boolean useAux = true;
		double tolerance = 50;
		tolerance = 0.0;
		ShapeFileDataReader.tolerance = tolerance;
		System.setProperty("swing.aatext", "true");

		try {
			// UIManager
			// .setLookAndFeel(new
			// org.jvnet.substance.skin.BusinessBlackSteelSkin());

			UIManager
					.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");

			// UIManager
			// .setLookAndFeel(
			// "org.jvnet.substance.skin.SubstanceTwilightLookAndFeel");

		} catch (Exception e) {
			logger.info("couldn't find Nimbus look and feel, continue");
			e.printStackTrace();
		}
		/*
		 * try { Class beanClass =
		 * Class.forName("geovista.collaboration.GeoJabber"); Object
		 * beanInstance = beanClass.newInstance(); logger.finest("yahhh");
		 * logger.finest("found a " + beanClass.getClass().getCanonicalName());
		 * logger.finest("found a " +
		 * beanInstance.getClass().getCanonicalName()); } catch (Exception e) {
		 * logger.finest("waaaa"); e.printStackTrace(); }
		 */
		logger.finest("starting.....");
		logger.finest("java.version = " + System.getProperty("java.version"));

		String fileName = null;

		if (args.length == 0) {
			// fileName = "GTD";
			// fileName = "c:\\temp\\shapefiles\\ca_cities.shp";
			// fileName =
			// "C:\\data\\geovista_data\\Historical-Demographic\\census\\census80_90_00.shp"
			// ;
		} else {
			fileName = args[0];
		}
		// fileName = "D:\\publications\\atpm\\Export_Output4.shp";
		// fileName = "D:\\courses\\geog
		// 741\\charlie\\richlex_zone63_diss2.shp";
		// fileName = "C:\\temp\\states48.shp";
		// fileName = "C:\\temp\\county\\county.shp";
		if (args.length >= 2) {
			String arg2 = args[1];

			if (arg2.compareToIgnoreCase("false") == 0) {
				useProj = false;
			}
		}

		GeoVizToolkit app = new GeoVizToolkit(fileName, useProj, useAux);

		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		app.setPreferredSize(new Dimension(800, 600));
		app.setMinimumSize(new Dimension(480, 320));

	}

	public static void main2(@SuppressWarnings("unused") String[] args) {
		JFrame app = new JFrame();
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		URL url = null;
		JEditorPane ep = null;
		try {
			url = new URL("http://en.wikipedia.org/wiki/Philippines");
			ep = new JEditorPane(url);
		} catch (Exception e) {

			e.printStackTrace();
		}
		app.add(ep);
		app.setVisible(true);
		// String html =

	}

	public String marshalCurrentComponent() {
		JInternalFrame intFrame = desktop.lastFrame;
		ToolkitBean tBean = null;
		tBean = tBeanSet.getToolkitBean(intFrame);
		if (tBean == null) {
			logger.info("selected frame is null, returning first one");
			tBean = tBeanSet.getBeanSet().iterator().next();
		}

		logger.info("selected toolkitBean = " + tBean.getUniqueName());
		Marshaler marsh = Marshaler.INSTANCE;
		String xml = marsh.toXML(tBean);
		logger.info(xml);
		return xml;
	}

	public boolean componentProviderExists() {
		return true;
	}

	public void selectedFrameChanged(JInternalFrame f) {
		ToolkitBean tBean = tBeanSet.getToolkitBean(f);
		if (tBean != null) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.info("selected internal frame of :"
						+ tBean.getUniqueName());
			}
		}
		if (tBean != null
				&& (tBean.getObjectClass().equals(GeoJabber.class.getName()) == false)) {
			desktop.lastFrame = f;
		}

	}

	public void marshaledComponent(String xml) {
		// XML of some component arrived here, we need to unmarshal and add
		logger.info("got marshaled xml:" + xml);
		Marshaler marsh = Marshaler.INSTANCE;
		xml = "<ToolkitBean><objectClass>geovista.geoviz.map.GeoMap</objectClass><originalBean><selection><int>40</int></selection><backgroundColor><red>238</red><green>238</green><blue>238</blue><alpha>255</alpha></backgroundColor></originalBean><uniqueName>GeoMap</uniqueName><internalFrame><location><x>996</x><y>4</y></location><size><width>282</width><height>395</height></size></internalFrame><zOrder>4</zOrder></ToolkitBean>";
		ToolkitBean tBean = (ToolkitBean) marsh.fromXML(xml);
		addBeanToGui(tBean);
		fireNewBeanMethods(tBean.getOriginalBean());
		coord.addBean(tBean.getOriginalBean());
		SelectionListener selListener = (SelectionListener) tBean
				.getOriginalBean();
		int[] sel = { 40 };
		selListener.selectionChanged(new SelectionEvent(this, sel));
	}

	public void indicationChanged(IndicationEvent e) {
		if (indUI.paintIndication == false) {
			return;
		}
		indUI.clear();
		if (e.getIndication() < 0) {
			return;
		}
		Object src = e.getSource();
		logger.info("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
		logger.finest(src.getClass().getName());

		HashSet<ToolkitBean> beans = tBeanSet.getBeanSet();
		for (ToolkitBean tBean : beans) {
			Object obj = tBean.getOriginalBean();

			if (obj instanceof ShapeReporter) {
				ShapeReporter shpR = (ShapeReporter) obj;
				// Component srcComp = ((ShapeReporter)
				// obj).renderingComponent();
				// Shape reportedShape = shpR.reportShape();
				Rectangle rect = shpR.reportShape().getBounds();
				Point pt = SwingUtilities.convertPoint((Component) obj, rect.x,
						rect.y, desktop);
				logger.info(obj.getClass().getName());
				Component comp = desktop.getComponentAt(pt);

				if (comp == null) {
					continue;
				}
				logger.info("comp at pt = " + comp.getClass().getName());

				if (comp instanceof JInternalFrame) {
					JInternalFrame iFrame = (JInternalFrame) comp;
					ToolkitBean tBeanA = tBeanSet.getToolkitBean(iFrame);
					Object originalBean = tBeanA.getOriginalBean();
					logger.info("originalBean = "
							+ originalBean.getClass().getName());
					if (obj == originalBean) {
						indUI.addShape(shpR.reportShape(),
								shpR.renderingComponent());
					}
				}

				logger.info("pt x = " + pt.x);
				logger.info("pt y = " + pt.y);
				logger.info(">>>>>");

				// if (desktop.getComponentAt(pt) == obj) {

				logger.finest("adding shape ");
				logger.finest("x " + rect.x);
				logger.finest("y " + rect.y);
				logger.finest("w " + rect.width);
				logger.finest("h " + rect.height);

			}
		}

	}

	@Override
	public void textChanged(TextEvent e) {
		String s = e.getText();
		DataSetForApps dsa = reader.makeDataSetForApps(s);
		dataCaster.setAndFireDataSet(dsa);

	}

}
