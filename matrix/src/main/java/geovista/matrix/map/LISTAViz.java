/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.matrix.map;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.common.data.DataSetBroadcaster;
import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.data.GeoDataSource;
import geovista.common.data.SpatialStatistics;
import geovista.common.data.SpatialWeights;
import geovista.common.data.TemporalWeights;
import geovista.common.data.WeightedNeighbor;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.PaletteEvent;
import geovista.common.event.PaletteListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SpatialExtentEvent;
import geovista.common.event.SpatialExtentListener;
import geovista.common.ui.VariablePicker;
import geovista.coordination.CoordinationManager;
import geovista.readers.example.GeoData2008Election;
import geovista.readers.example.GoogleFluDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.symbolization.event.ColorClassifierEvent;
import geovista.symbolization.event.ColorClassifierListener;

/**
 * A Moran Map has a choropleth varSigMap and a scatterplot tied together
 */
public class LISTAViz extends JPanel implements IndicationListener,
		DataSetListener, ColorClassifierListener, SpatialExtentListener,
		PaletteListener, TableModelListener {
	protected final static Logger logger = Logger.getLogger(LISTAViz.class
			.getName());

	ArrayList<MapHistoPair> mastos;

	DataSetForApps dataSetOriginal;
	DataSetForApps dataSetZ;
	DataSetForApps dataSetMoran;
	SpatialWeights spatialWeights;

	int monteCarloIterations = 1000;
	DataSetBroadcaster dataCaster;

	VariablePicker varPicker;

	CoordinationManager coord;
	JPanel mainPanel;
	JScrollPane scrollPane;

	public LISTAViz() {
		super();

		mastos = new ArrayList<MapHistoPair>();

		dataCaster = new DataSetBroadcaster();

		coord = new CoordinationManager();

		mainPanel = new JPanel();
		scrollPane = new JScrollPane(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(mainPanel);
		mainPanel.setPreferredSize(new Dimension(1000, 400));
		// BoxLayout box = new BoxLayout(this, BoxLayout.X_AXIS);
		// varSigMap = new GeoMap();
		// sp = new SingleScatterPlot();
		// varSigMap.addSelectionListener(sp);
		// sp.addSelectionListener(varSigMap);
		// varSigMap.addIndicationListener(sp);
		// sp.addIndicationListener(varSigMap);
		// setLayout(box);

		// LineBorder border = (LineBorder) BorderFactory
		// .createLineBorder(Color.black);
		// varSigMap.setBorder(border);
		// sp.setBorder(border);
		setLayout(new BorderLayout());
		this.add(scrollPane);
		// this.add(sp);

	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, new int[0]);
	}

	public void indicationChanged(IndicationEvent e) {
		for (MapHistoPair masto : mastos) {
			masto.indicationChanged(e);
		}

	}

	public void dataSetChanged(DataSetEvent e) {
		dataSetOriginal = e.getDataSetForApps();
		String[] fullVarNames = dataSetOriginal.getAttributeNamesNumeric();
		logger.info("n vars = " + fullVarNames.length);
		String[] varNames = new String[100];
		int startNum = 230;
		for (int i = 0; i < varNames.length; i++) {
			varNames[i] = fullVarNames[i + startNum];
		}
		spatialWeights = dataSetOriginal.getSpatialWeights();
		mainPanel.setLayout(new GridLayout(1, varNames.length));
		Dimension prefSize = new Dimension(120, 240);
		for (String varName : varNames) {
			MapHistoPair masto = new MapHistoPair();
			mastos.add(masto);
			mainPanel.add(masto);
			masto.setMaximumSize(prefSize);
			masto.setMinimumSize(prefSize);
			masto.setPreferredSize(prefSize);

		}

		mainPanel
				.setPreferredSize(new Dimension(
						(int) (prefSize.getWidth() * varNames.length),
						prefSize.height));
		mainPanel.revalidate();
		for (MapHistoPair masto : mastos) {
			masto.dataSetChanged(e);
		}
		for (int i = 0; i < varNames.length; i++) {
			// logger.info("calculating space time... " + i);
			MapHistoPair masto = mastos.get(i);
			masto.setSelectedVar(i + startNum);
			long startTime = System.currentTimeMillis();
			int[] sel = calcSpaceTime(i + startNum);
			long endTime = System.currentTimeMillis();
			System.out.println("" + (endTime - startTime));
			masto.selectionChanged(new SelectionEvent(this, sel));
		}

	}

	public int[] calcSpaceTime(int var) {

		double[] newData = dataSetOriginal.getNumericDataAsDouble(var);

		double[] zData = DescriptiveStatistics.calculateZScores(newData);

		int nVars = 3;
		TemporalWeights tw = new TemporalWeights(nVars);

		List<double[]> doubleData = new ArrayList<double[]>();

		for (int i = 0; i < nVars; i++) {
			if ((var - i) >= 0) {
				doubleData.add(dataSetOriginal.getNumericDataAsDouble(var - i));
			} else {
				doubleData.add(new double[0]);
			}
		}

		// double[] moranData = SpatialStatistics.calculateSpaceTimeMoranScores(
		// doubleData, dataSetOriginal.getSpatialWeights(), tw);

		// double[] moranData = SpatialStatistics.calculateMoranScores(zData,
		// spatialWeights);

		// double[] monteCarloData = SpatialStatistics.findPValues(zData,
		// monteCarloIterations, spatialWeights);

		double[] monteCarloData = SpatialStatistics.findSpaceTimePValues(
				doubleData, monteCarloIterations, spatialWeights, tw, true);

		ArrayList<Integer> sel = new ArrayList<Integer>();
		for (int i = 0; i < monteCarloData.length; i++) {
			if (monteCarloData[i] < .1 && zData[i] > 0) {
				sel.add(i);
			}
		}
		int[] selArray = new int[sel.size()];
		for (int i = 0; i < selArray.length; i++) {
			selArray[i] = sel.get(i);

		}
		return selArray;

	}

	public void colorClassifierChanged(ColorClassifierEvent e) {
		// TODO Auto-generated method stub

	}

	SpatialExtentEvent savedEvent;

	public SpatialExtentEvent getSpatialExtentEvent() {
		return savedEvent;
	}

	public void spatialExtentChanged(SpatialExtentEvent e) {
		savedEvent = e;
		// TODO Auto-generated method stub

	}

	public void paletteChanged(PaletteEvent e) {
		// TODO Auto-generated method stub

	}

	public void tableChanged(TableModelEvent e) {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {

		LISTAViz varSigMap = new LISTAViz();
		double[] vals = { 0, 1, 2, 3 };
		double along = DescriptiveStatistics.percentAbove(vals, 2.9);
		logger.info("" + along);

		JFrame frame = new JFrame("LISTA-Viz");
		frame.add(varSigMap);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// GeoDataGeneralizedStates geodata = new GeoDataGeneralizedStates();
		GeoDataSource geoData = null;
		try {
			geoData = new GoogleFluDataReader();
			geoData = new GeoData2008Election();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ShapeFileProjection proj = new ShapeFileProjection();
		proj.setInputDataSetForApps(geoData.getDataForApps());
		// newDataSet = statesData.getDataForApps().getDataObjectOriginal();

		// proj.setInputDataSet(countyData.getDataSet());

		// newDataSet = proj.getOutputDataSet();
		SpatialWeights sw = proj.getOutputDataSetForApps().getSpatialWeights();

		Collection<ArrayList<WeightedNeighbor>> results = sw
				.findUniqueTopologies();

		for (ArrayList<WeightedNeighbor> bors : results) {
			logger.info("n = " + bors.size());
			// double[] results = SpatialStatistics.
		}

		// System.exit(0);

		DataSetEvent e = new DataSetEvent(proj.getOutputDataSetForApps(),
				geoData);

		varSigMap.dataSetChanged(e);
		System.out.println(geoData.getDataForApps().getNumObservations());

	}
}
