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
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;

import geovista.common.data.DataSetBroadcaster;
import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.common.ui.VariablePicker;
import geovista.geoviz.map.GeoMap;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;

/**
 * @author pfoley
 * 
 */
@SuppressWarnings("serial")

public class DiscriminantAnalysisGUI extends JPanel
	implements ActionListener ,DataSetListener, SubspaceListener, SelectionListener, IndicationListener {
	 

	// backend components
	transient DiscriminantAnalysis da = null;
	transient DataSetForApps dataSet = null;

	// gui components
	transient JButton goButton = null;
	transient JLabel category = null;
	transient JComboBox categoryCombo = null;
	transient VariablePicker indVarPicker = null;
	transient JPanel outputInfo = null;
	transient GeoMap map = null;

	// arrays to hold indices for independent and dependent variables
	// in the dataSet object
	transient int [] indVarIndices = new int[0];
	transient int categoryIndex = -1;
	transient int numClassifications = 0;
	
	// holds the number of numeric attributes in the initial dataset
	// need this so we don't keep adding new attributes with each classification
	transient int numOrgNumAttributes = 0;
	
	// categoryIndexMap maps the indices of variables
	// in the categoryCombo Box to indices of variables
	// in dataSet
	transient Map<Integer,Integer> categoryIndexMap = null;
	
	// logger object
	protected final static Logger logger = Logger
			.getLogger(DiscriminantAnalysisGUI.class.getPackage().getName());


	public DiscriminantAnalysisGUI() {

		super(new BorderLayout());

		setPreferredSize(new Dimension(600, 400));

		// create the class variables
		da = new DiscriminantAnalysis();
		goButton = new JButton("Classify");
		category = new JLabel("Category");
		categoryCombo = new JComboBox();
		indVarPicker = new VariablePicker(DataSetForApps.TYPE_DOUBLE);
		outputInfo = new JPanel();
		outputInfo.setLayout(new BoxLayout(outputInfo, BoxLayout.Y_AXIS));
		JScrollPane outputInfoPane = new JScrollPane(outputInfo);
		map = new GeoMap();

		// set class variable properties
		indVarPicker.setPreferredSize(new Dimension(180, 400));
		indVarPicker.setBorder(BorderFactory
				.createTitledBorder("Independent Variables"));

		// add items to the tabbedPane
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("GeoMap", map);
		tabbedPane.addTab("Diagnostics", outputInfoPane);

		// add items to the bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(categoryCombo);
		bottomPanel.add(category);
		bottomPanel.add(goButton);

		// add all gui items to the current window
		this.add(tabbedPane, BorderLayout.CENTER);
		this.add(indVarPicker, BorderLayout.WEST);
		this.add(bottomPanel, BorderLayout.SOUTH);

		// add this bean as a listener for the following events types:
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
		
		// test file name
		URL testFileName = daGui.getClass().getResource(
				"resources/iris_poly.shp");

		ShapeFileDataReader shpRead = new ShapeFileDataReader();
		shpRead.setFileName(testFileName.getFile());
		
		// now broadcast the dataset we have just read in - this bean is listening for it (dataSetChanged method)
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
	
	/*
	 * Called when the user presses the classifier button or when a new category is chosen
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == categoryCombo) {
			setCategoryIndex(categoryCombo.getSelectedIndex());
		} else if (e.getSource() == goButton) {
			classify();
		}

	}
	
	/*
	 * Called whenever a new DataSetEvent object is broadcast
	 * @see geovista.common.event.DataSetListener#dataSetChanged(geovista.common.event.DataSetEvent)
	 */
	public void dataSetChanged(DataSetEvent e) {

		// get the dataset
		dataSet = e.getDataSetForApps();
		
		// save the number of numeric attributes in the original dataset
		numOrgNumAttributes  = dataSet.getNumberNumericAttributes();

		// identify the variables of type 'int'
		// and add them to the 'category' combo box
		// int numNumericVars = dataSet.getNumberNumericAttributes();
		int numVars = dataSet.getColumnCount();
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
		
		// ensure that the categoryCombo doesn't have any existing items
		categoryCombo.removeAllItems();
		
		// only include variables of type integer in the category combo box
		for (int i = 0;  i < numVars; i++){
						
			if (dTypes[i] == DataSetForApps.TYPE_INTEGER) {

				categoryCombo.addItem(dataSet.getColumnName(i));
				categoryIndexMap.put(Integer.valueOf(++categoryComboIndex),
						Integer.valueOf(++numericIndex));
			} else if (dTypes[i] == DataSetForApps.TYPE_BOOLEAN
					|| dTypes[i] == DataSetForApps.TYPE_DOUBLE) {
				numericIndex++;
			}
		}

		// the variable picker will only include variables of types
		// int, double or boolean by default. Want it to only include variables
		// of type double so need to create a new DataSetEvent with variables of this type
		indVarPicker.dataSetChanged(e);
	}

	/*
	 * Called whenever a SubspaceEvent is broadcast from a firing bean
	 * @see geovista.common.event.SubspaceListener#subspaceChanged(geovista.common.event.SubspaceEvent)
	 */
	public void subspaceChanged(SubspaceEvent e) {

		// only include subspace events from the local
		// variable picker
		if (e.getSource() == indVarPicker) {
			setIndVarIndices(e.getSubspace());
		}
	}
	
	/*
	 * Methods dealing with sending and receiving selection events
	 * Sending such events : addSelectionListener and removeSelectionListener
	 * Receiving such events : selectionChanged and getSelectionEvent (SelectionListener interface)
	 */

	public void selectionChanged(SelectionEvent e) {
		map.selectionChanged(e);
	}
	
	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, map.getSelectedObservations());
	}
	
	public void addSelectionListener(SelectionListener l) {
		map.addSelectionListener(l);
	}

	public void removeSelectionListener(SelectionListener l) {
		map.removeSelectionListener(l);
	}
	
	
	/*
	 * Methods dealing with sending and receiving IndicationEvents :
	 * Sending such events : addIndicationListener & removeIndicationListener
	 * Receiving such events : indicationChanged (IndicationListener Interface)
	 */
	
	public void indicationChanged(IndicationEvent e) {
		map.indicationChanged(e);
	}
	
	public void addIndicationListener(IndicationListener l) {
		map.addIndicationListener(l);
	}

	public void removeIndicationListener(IndicationListener l) {
		map.removeIndicationListener(l);
	}
	
	
	/*
	 * Perform the classification and write subsequent diagnostics to the screen
	 */
	private void classify() {
				
		// First, make sure that a category index has been found and that there
		// is at least one independent variable		
		if ( categoryIndex >= 0 && indVarIndices.length > 0 ) {

			// the data array consists of arrays of columns
			double[][] data = new double[indVarIndices.length][0];
			for (int i = 0; i < indVarIndices.length; i++) {
				data[i] = dataSet.getNumericDataAsDouble(indVarIndices[i]);
			}
			
			// get the category data
			String categoryName = dataSet.getNumericArrayName(categoryIndex);
			int[] categories = dataSet.getIntArrayDataByName(categoryName);
				
			try {
				
				// set the independent variables
				da.setPredictorVariables(data,false);
				
				// set the dependent variable (category)
				da.setClassification(categories);

				// set the prior probabilities to the default (equal)
				da.setPriorProbabilities();
				
				// classify the data
				da.classify();
					
				// get output data				
				int[] uniqueClasses = da.getUniqueClasses();			
				int[][] confMatrix = da.confusionMatrix();
				int[] classFreq = da.getClassFrequencies();
				double[][] params = da.getParameters();
				int numFields = da.getNumAttributes();
				int numObs = da.getNumObservations();				
				int numClasses = uniqueClasses.length;
				
				// write some basic details about the classification to the screen 
				//( name of dependent and independent variables etc)
				JTextArea info = new JTextArea("\nClassification " + Integer.toString(++numClassifications));
				info.append("\n\nClassification Category : " + categoryName);
				info.append("\nIndependent Variables (" + Integer.toString(indVarIndices.length) + ") : ");
				for (int i=0; i < indVarIndices.length; i++) {
					info.append("\n" + dataSet.getNumericArrayName(indVarIndices[i]) + " (Variable " + 
								+ Integer.valueOf(indVarIndices[i]) + ")");
				}
				info.setEditable(false);
							
				// write out the confusion matrix & percentages correctly classified
				JTable cMatrixTable = new JTable(numClasses + 2, numClasses + 2);
				cMatrixTable.setBorder(BorderFactory.createLineBorder(Color.black));
				
				// compute classification accuracy (percentage correctly classified)
				double classAccuracy = 0.0;
				
				// compute classification accuracy rate if classification were done randomly on the basis of the 
				// observed class frequencies which are used in lieu of prior probabilities
				double randomClassAccuracy = 0.0;
							
				for ( int i = 0; i < numClasses; i++) {
					cMatrixTable.setValueAt("Class " + String.valueOf(uniqueClasses[i]), i+1, 0);
					cMatrixTable.setValueAt("Class " + String.valueOf(uniqueClasses[i]), 0, i+1);
									
					// count the number of correctly classified observations
					classAccuracy += confMatrix[i][i];
					
					// compute classification accuracy rate if classification were done randomly
					// on the basis of observed classification frequencies
					randomClassAccuracy += Math.pow(( (double)classFreq[i] / (double)numObs),2);
													
					int totalClassified = 0;
					
					for (int j = 0; j < numClasses; j++) {
						
						// compute percentage correctly classified for the current cell in the 
						// confusion matrix
						double pcc = ( (double)confMatrix[i][j] / (double)classFreq[i] ) * 100.0;
						
						cMatrixTable.setValueAt(String.format("%2d (%-3.1f %%)",confMatrix[i][j],pcc), i+1, j+1);
						totalClassified += confMatrix[j][i];
						
					}
					cMatrixTable.setValueAt(classFreq[i],i+1,numClasses+1);
					cMatrixTable.setValueAt(totalClassified, numClasses+1,i+1);
				}
				cMatrixTable.setValueAt("Total by Class",0,numClasses+1);
				cMatrixTable.setValueAt("Total Assigned",numClasses+1,0);
				cMatrixTable.setValueAt(numObs,numClasses+1,numClasses+1);
				
				
				// compute the classification accuracy and error rate & write to the diagnostics screen
				classAccuracy = ( classAccuracy / numObs ) * 100;
				info.append("\n\nClassification Accuracy                      :  " + 
						String.format("%.2f %%", classAccuracy));
				info.append("\nClassification Error Rate                    : " + 
						String.format("%.2f %%", 100.0 - classAccuracy));
				info.append("\nClassification Accuracy (Random Assignment)  :  " 
						+ String.format("%.2f %%", randomClassAccuracy * 100));
						
				outputInfo.add(info);
				outputInfo.add(new JLabel("Confusion Matrix - Percentage Correctly Classified"));
				outputInfo.add(cMatrixTable);
				
				// write out the classification function coefficients
				JTable pMatrixTable = new JTable(numFields + 2, numClasses + 1);
				pMatrixTable.setBorder(BorderFactory.createLineBorder(Color.black));
						 			
				for (int i = 0; i < numFields+1; i++) {
			
					pMatrixTable.setValueAt("Paramater " + String.valueOf(i),i+1,0);
				
					for (int j = 0; j < numClasses; j++) {
						
						if (i == 0) {
							pMatrixTable.setValueAt("Class " + String.valueOf(j),0,j+1);
						}
						pMatrixTable.setValueAt(String.format("%.4f",params[i][j]),i+1,j+1);
					}
				}
				
				outputInfo.add(new JLabel("Classification Function Paramaters"));
				outputInfo.add(pMatrixTable);
				
				// now send the new data to the geomap in the current bean
										
				DataSetBroadcaster dataCaster = new DataSetBroadcaster();
				
				String[] fNames = new String[2 + (2*numClasses)];
				Object[] dataSetChanged = new Object[4 + (2*numClasses)];
				fNames[0] = "Class";
				fNames[1] = "Classified";
				dataSetChanged[1] = da.getClassification();
				dataSetChanged[2] = da.getClassified();
				for ( int i = 0; i < numClasses; i++) {
					fNames[i+2] = "MhDist2_" + String.valueOf(i);
					dataSetChanged[i+3] = da.getMahalanobisDistance2(i);
				}
				for ( int i = 0; i < numClasses; i++) {
					fNames[i+(2 + numClasses)] = "PostProb_" + String.valueOf(i);
					dataSetChanged[i+(3+numClasses)] = da.getPosteriorProbabilities(i);
				}
				dataSetChanged[0] = fNames;
				dataSetChanged[3 + (2*numClasses)] = dataSet.getShapeData();
				
				
				dataCaster.addDataSetListener(map);
				dataCaster.setAndFireDataSet(dataSetChanged);
				map.repaint();
				
				///////////////////////////////////////////////////
				// now send the data to all other listening beans
				//////////////////////////////////////////////////
				
				String[] orgFieldNames = dataSet.getAttributeNamesOriginal();
				Object[] orgData = dataSet.getDataSetNumeric();
				
				Object[] newData = new Object[numOrgNumAttributes + (1 + (numClasses*2))];
				String[] newFieldNames = new String[numOrgNumAttributes + (1 + (numClasses*2))];
				
				for ( int i = 0; i < numOrgNumAttributes; i++) {
					newData[i] = orgData[i];
					newFieldNames[i] = orgFieldNames[i];
				}
				
				newFieldNames[numOrgNumAttributes] = "Classified";
				newData[numOrgNumAttributes] = da.getClassified();
				
				for ( int i = 0; i < numClasses; i++) {
					newFieldNames[i+(numOrgNumAttributes+1)] = "MhDist2_" + String.valueOf(i);
					newData[i+(numOrgNumAttributes+1)] = da.getMahalanobisDistance2(i);
				}
				for ( int i = 0; i < numClasses; i++) {
					newFieldNames[i+(numOrgNumAttributes+numClasses+1)] = "PostProb_" + String.valueOf(i);
					newData[i+(numOrgNumAttributes+numClasses+1)] = da.getPosteriorProbabilities(i);
				}
				
				DataSetForApps newDataSet = new DataSetForApps(newFieldNames, newData, dataSet.getShapeData());
				fireDataSetChanged(newDataSet);
				
			} catch (DiscriminantAnalysisException e ){
				String message = "Unable to classify : " + e.getMessage();
				JOptionPane.showMessageDialog(this, message, "WARNING", JOptionPane.WARNING_MESSAGE);
			}
												
		} else {
			
			String message = "Unable to classify : ";
			if ( categoryIndex < 0 ) {
				message += "\nCategory Index is " + ((categoryIndex < 0) ? "not set" : "set");
			}
			
			if (indVarIndices.length == 0) {
				message += "\nIndependent Variables are " + ((indVarIndices.length == 0) ? "not set" : "set");
			}
			JOptionPane.showMessageDialog(this, message, "WARNING", JOptionPane.WARNING_MESSAGE);
		}
	}

	/*
	 * Set the independent variable indices and check to make sure that there is no overlap between 
	 * them and the dependent variable
	 */
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
	
	/**
	 * adds a DataSetListener
	 */
	public void addDataSetListener(DataSetListener l) {
		listenerList.add(DataSetListener.class, l);
	}

	/**
	 * removes a DataSetListener
	 */
	public void removeDataSetListener(DataSetListener l) {
		listenerList.remove(DataSetListener.class, l);
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 *
	 * @see EventListenerList
	 */
	protected void fireDataSetChanged(DataSetForApps data) {
		logger.finest("ShpToShp.fireDataSetChanged, Hi!!");
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		DataSetEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == DataSetListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new DataSetEvent(data, this);

				}
				((DataSetListener) listeners[i + 1]).dataSetChanged(e);
			}
		}
	}

}
