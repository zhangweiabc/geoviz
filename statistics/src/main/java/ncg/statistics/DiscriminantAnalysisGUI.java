/**
 *
 */
package ncg.statistics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import geovista.common.data.DataSetBroadcaster;
import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.common.ui.VariablePicker;
import geovista.geoviz.map.GeoMap;
import geovista.readers.shapefile.ShapeFileDataReader;

/**
 * @author pfoley
 * 
 */
@SuppressWarnings("serial")
public class DiscriminantAnalysisGUI extends JPanel implements ActionListener,
		DataSetListener, SubspaceListener {

	// backend components
	DiscriminantAnalysis da = null;
	DataSetForApps dataSet = null;

	// gui components
	JButton goButton = null;
	JLabel category = null;
	JComboBox categoryCombo = null;
	VariablePicker indVarPicker = null;
	JPanel outputInfo = null;
	GeoMap map = null;

	// arrays to hold indices for independent and dependent variables
	// in the dataSet object
	int[] indVarIndices = new int[0];
	int categoryIndex = -1;

	// categoryIndexMap maps the indices of variables
	// in the categoryCombo Box to indices of variables
	// in dataSet
	Map<Integer, Integer> categoryIndexMap = null;

	// logger object
	protected final static Logger logger = Logger
			.getLogger(DiscriminantAnalysisGUI.class.getPackage().getName());

	public DiscriminantAnalysisGUI() {

		super(new BorderLayout());

		setPreferredSize(new Dimension(800, 600));

		// create the class variables
		da = new DiscriminantAnalysis();
		goButton = new JButton("Classify");
		category = new JLabel("Category");
		categoryCombo = new JComboBox();
		indVarPicker = new VariablePicker();
		outputInfo = new JPanel();
		outputInfo.setLayout(new BoxLayout(outputInfo, BoxLayout.Y_AXIS));
		map = new GeoMap();

		// set class variable properties
		indVarPicker.setPreferredSize(new Dimension(180, 400));
		indVarPicker.setBorder(BorderFactory
				.createTitledBorder("Independent Variables"));
		// textArea.setEditable(false);

		// add items to the tabbedPane
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("GeoMap", map);
		tabbedPane.addTab("Diagnostics", outputInfo);
		// tabbedPane.addTab("Info", textArea);

		// add items to the bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(categoryCombo);
		bottomPanel.add(category);
		bottomPanel.add(goButton);

		// add all gui items to the current window
		this.add(tabbedPane, BorderLayout.CENTER);
		this.add(indVarPicker, BorderLayout.WEST);
		this.add(bottomPanel, BorderLayout.SOUTH);

		goButton.addActionListener(this);
		categoryCombo.addActionListener(this);
		indVarPicker.addSubspaceListener(this);

	}

	/**
	 * main method is used exclusively for testing and debugging purposes only
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// First, create an output log file:
		String logFile = DiscriminantAnalysisGUI.class.getSimpleName() + ".log";
		// set the logger level
		logger.setLevel(Level.ALL);
		logger.info(System.getProperty("java.version"));

		// output log file handler
		FileHandler logHandler = null;

		// open the log file
		try {
			logHandler = new FileHandler(logFile);
			logHandler.setFormatter(new SimpleFormatter());
			logHandler.setLevel(Level.ALL);
			logger.addHandler(logHandler);
		} catch (IOException e) {
			System.out.println("Unable to create log file : " + e.getMessage());
			e.printStackTrace();
		}

		// create the GUI
		JFrame app = new JFrame();
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		DiscriminantAnalysisGUI daGui = new DiscriminantAnalysisGUI();
		app.add(daGui);
		app.pack();
		app.setVisible(true);

		// read the data in the test file
		// URL testFileName =
		// daGui.getClass().getResource("resources/electoral_divisions_cu_cso_2006.shp");
		URL testFileName = daGui.getClass().getResource(
				"resources/iris_poly.shp");
		ShapeFileDataReader shpRead = new ShapeFileDataReader();
		shpRead.setFileName(testFileName.getFile());
		Object[] testDataArray = shpRead.getDataSet();

		if (testDataArray != null) {
			DataSetForApps data = new DataSetForApps(testDataArray);
			DataSetBroadcaster dataCaster = new DataSetBroadcaster();
			dataCaster.addDataSetListener(daGui);
			dataCaster.setAndFireDataSet(data);
			logger.info("Test data loaded from [" + testFileName.getFile()
					+ "]");
		} else {
			logger.severe("Unable to read test data from file ["
					+ testFileName.getFile() + "]");
		}

	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == categoryCombo) {
			setCategoryIndex(categoryCombo.getSelectedIndex());
		} else if (e.getSource() == goButton) {
			classify();
		}

	}

	public void dataSetChanged(DataSetEvent e) {

		// get the dataset
		dataSet = e.getDataSetForApps();

		// identify the variables of type 'int'
		// and add them to the 'category' combo box
		// int numNumericVars = dataSet.getNumberNumericAttributes();
		int numVars = dataSet.getNamedArrays().length;
		int[] dTypes = dataSet.getDataTypeArray();

		// categoryIndexMap maps the indices of variables
		// in the categoryCombo Box to indices of variables
		// in dataSet
		categoryIndexMap = new HashMap<Integer, Integer>();

		// use numericIndex to distinguish between indices of numeric
		// variables (double, boolean and int) as displayed in the
		// variable picker and the full set of attributes
		int numericIndex = -1;

		// use categoryCombo index for indices of items added
		// to categoryCombo Box
		int categoryComboIndex = -1;

		for (int i = 0; i < numVars; i++) {

			if (dTypes[i] == DataSetForApps.TYPE_INTEGER) {

				categoryCombo.addItem(dataSet.getAttributeNamesOriginal()[i]);
				categoryIndexMap.put(Integer.valueOf(++categoryComboIndex),
						Integer.valueOf(++numericIndex));
			} else if (dTypes[i] == DataSetForApps.TYPE_BOOLEAN
					|| dTypes[i] == DataSetForApps.TYPE_DOUBLE) {
				numericIndex++;
			}
		}

		// the variable picker will only include variables of types
		// int, double or boolean
		indVarPicker.dataSetChanged(e);
	}

	public void subspaceChanged(SubspaceEvent e) {

		// only include subspace events from the local
		// variable picker
		if (e.getSource() == indVarPicker) {
			setIndVarIndices(e.getSubspace());
		}
	}

	private void classify() {

		if (categoryIndex >= 0 && indVarIndices.length > 0) {

			logger.info("Classification Category : ["
					+ dataSet.getNumericArrayName(categoryIndex)
					+ "], numeric index : [" + categoryIndex + "]");
			logger.info("Independent Variables:");
			for (int indVarIndice : indVarIndices) {
				logger.info("[" + dataSet.getNumericArrayName(indVarIndice)
						+ "], numeric index : [" + indVarIndice + "]");
			}

			logger.info("Data set contains ["
					+ String.valueOf(dataSet.getNumObservations())
					+ "] observations");
			logger.info("Dataset contains ["
					+ String.valueOf(dataSet.getNumObservations()) + "] fields");

			// the data array consists of arrays of columns
			double[][] data = new double[indVarIndices.length][0];
			for (int i = 0; i < indVarIndices.length; i++) {
				data[i] = dataSet.getNumericDataAsDouble(indVarIndices[i]);
			}

			String categoryName = dataSet.getNumericArrayName(categoryIndex);
			int[] categories = dataSet.getIntArrayDataByName(categoryName);
			da.setPredictorVariables(data, false);
			da.setClassification(categories);
			da.classify();

			// **********************************************************
			// get various diagnostics
			// **********************************************************

			int[] uniqueClasses = da.getUniqueClasses();
			int[][] confMatrix = da.confusionMatrix();
			int[] classFreq = da.getClassFrequencies();
			double[][] params = da.getParameters();
			int numFields = da.getNumAttributes();
			int numObs = da.getNumObservations();

			int numClasses = uniqueClasses.length;

			// write out the confusion matrix
			JTable cMatrixTable = new JTable(numClasses + 2, numClasses + 2);
			cMatrixTable.setBorder(BorderFactory.createLineBorder(Color.black));

			for (int i = 0; i < numClasses; i++) {

				cMatrixTable.setValueAt(
						"Class " + String.valueOf(uniqueClasses[i]), i + 1, 0);
				cMatrixTable.setValueAt(
						"Class " + String.valueOf(uniqueClasses[i]), 0, i + 1);

				int totalClassified = 0;

				for (int j = 0; j < numClasses; j++) {
					cMatrixTable.setValueAt(confMatrix[i][j], i + 1, j + 1);
					totalClassified += confMatrix[j][i];
				}
				cMatrixTable.setValueAt(classFreq[i], i + 1, numClasses + 1);
				cMatrixTable.setValueAt(totalClassified, numClasses + 1, i + 1);
			}
			cMatrixTable.setValueAt("Total by Class", 0, numClasses + 1);
			cMatrixTable.setValueAt("Total Assigned", numClasses + 1, 0);
			cMatrixTable.setValueAt(numObs, numClasses + 1, numClasses + 1);

			outputInfo.add(new JLabel("Confusion Matrix"));
			outputInfo.add(cMatrixTable);

			// write out the classification function coefficients
			JTable pMatrixTable = new JTable(numFields + 2, numClasses + 1);
			pMatrixTable.setBorder(BorderFactory.createLineBorder(Color.black));

			for (int i = 0; i < numFields + 1; i++) {

				pMatrixTable.setValueAt("Paramater " + String.valueOf(i),
						i + 1, 0);

				for (int j = 0; j < numClasses; j++) {

					if (i == 0) {
						pMatrixTable.setValueAt("Class " + String.valueOf(j),
								0, j + 1);
					}
					pMatrixTable.setValueAt(
							String.format("%.4f", params[i][j]), i + 1, j + 1);
				}
			}

			outputInfo.add(new JLabel("Classification Function Paramaters"));
			outputInfo.add(pMatrixTable);

			DataSetBroadcaster dataCaster = new DataSetBroadcaster();

			String[] fNames = new String[2 + (2 * numClasses)];
			Object[] dataSetChanged = new Object[4 + (2 * numClasses)];
			fNames[0] = "Class";
			fNames[1] = "Classified";
			dataSetChanged[1] = da.getClassification();
			dataSetChanged[2] = da.getClassified();
			for (int i = 0; i < numClasses; i++) {
				fNames[i + 2] = "MhDist2_" + String.valueOf(i);
				dataSetChanged[i + 3] = da.getMahalanobisDistance2(i);
			}
			for (int i = 0; i < numClasses; i++) {
				fNames[i + (2 + numClasses)] = "PostProb_" + String.valueOf(i);
				dataSetChanged[i + (3 + numClasses)] = da
						.getPosteriorProbabilities(i);
			}
			dataSetChanged[0] = fNames;
			dataSetChanged[3 + (2 * numClasses)] = dataSet.getShapeData();

			// String[] fNames = {"Class","Classified","Mh"};
			// Object[] dataSetChanged = {fNames, da.getClassification(),
			// da.getClassified(), da.getMahalanobisDistance2(0),
			// dataSet.getShapeData()};

			// DataSetForApps dataSetChanged = new
			// DataSetForApps(dataSet.getDataSetFull());
			// dataSetChanged.addColumn("Classified", da.getClassified());
			// for (int i = 0; i < numClasses; i++) {
			// dataSetChanged.addColumn("MhDist2_" + String.valueOf(i),
			// da.getMahalanobisDistance2(i));
			// dataSetChanged.addColumn("PostProb_" + String.valueOf(i),
			// da.getPosteriorProbabilities(i));
			// }

			dataCaster.addDataSetListener(map);
			dataCaster.setAndFireDataSet(dataSetChanged);
			map.repaint();

		} else {
			logger.info("Unable to run classification");
			logger.info("Category Index is ["
					+ ((categoryIndex < 0) ? "not set" : "set") + "]");
			logger.info("Independent Variables are ["
					+ ((indVarIndices.length == 0) ? "not set" : "set") + "]");
		}
	}

	private void setIndVarIndices(int[] indVarIndices) {

		this.indVarIndices = indVarIndices;
		checkIndVarIndices();
	}

	private void setCategoryIndex(int categoryIndex) {

		if (categoryIndexMap.containsKey(Integer.valueOf(categoryIndex))) {
			this.categoryIndex = categoryIndexMap.get(Integer
					.valueOf(categoryIndex));
			checkIndVarIndices();
		}
	}

	/*
	 * Check the independent Variable choices to ensure that they are
	 * Consistent. Also check to see if the categoryIndex is contained in the
	 * indVarIndices array. If so, remove it.
	 */
	private void checkIndVarIndices() {

		// only do check if we have a categoryIndex value and
		// an indVarIndices array to search
		if (categoryIndex >= 0 && indVarIndices.length > 0) {

			// array must be sorted for binary search
			Arrays.sort(indVarIndices);
			int found = Arrays.binarySearch(indVarIndices, categoryIndex);

			if (found >= 0) {

				logger.info("Unable use ["
						+ dataSet.getNumericArrayName(indVarIndices[found])
						+ "] as an independent and dependent variable");
				logger.info("Excluding ["
						+ dataSet.getNumericArrayName(indVarIndices[found])
						+ "] as an independent variable");

				int[] indVarIndicesNew = new int[indVarIndices.length - 1];
				for (int i = 0; i < indVarIndices.length; i++) {
					if (i < found) {
						indVarIndicesNew[i] = indVarIndices[i];
					} else if (i > found) {
						indVarIndicesNew[i - 1] = indVarIndices[i];
					}
				}

				indVarIndices = indVarIndicesNew;
			}
		}
	}

}
