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
import geovista.readers.shapefile.ShapeFileDataReader;

/**
 * @author pfoley
 * 
 */
@SuppressWarnings("serial")

public class DiscriminantAnalysisGUI extends JPanel
	implements ActionListener ,DataSetListener, SubspaceListener {
	 

	// backend components
	transient DiscriminantAnalysis da = null;
	transient DataSetForApps dataSet = null;

	// gui components
	transient JButton goButton = null;
	transient JComboBox categoryCombo = null;
	transient VariablePicker indVarPicker = null;
	transient JPanel outputInfo = null;
	
	// array of indices of independent variables from the indVarPicker object
	transient int [] indVarIndices = new int[0];
	
	// index of dependent variable from the categoryCombo object
	transient int categoryIndex = -1;
	
	// number of distinct classifications for the current bean
	transient int numClassifications = 0;
	
	// categoryIndexMap maps the indices of items in the categoryCombo Box to indices of attributes in dataSet
	transient Map<Integer,Integer> categoryIndexMap = null;
	
	// logger
	protected final static Logger logger = Logger.getLogger(DiscriminantAnalysisGUI.class.getPackage().getName());
	
	// constructor
	public DiscriminantAnalysisGUI() {

		super(new BorderLayout());
		setPreferredSize(new Dimension(500, 400));
		
		// create the class variables
		da = new DiscriminantAnalysis();
		goButton = new JButton("Classify");
		categoryCombo = new JComboBox();
		indVarPicker = new VariablePicker(DataSetForApps.TYPE_DOUBLE);
		outputInfo = new JPanel();
		
		// set class variable specific properties
		outputInfo.setLayout(new BoxLayout(outputInfo, BoxLayout.Y_AXIS));
		indVarPicker.setPreferredSize(new Dimension(180, 400));
		indVarPicker.setBorder(BorderFactory.createTitledBorder("Independent Variables"));
		
		// add this bean a listener for the following events types:
		goButton.addActionListener(this);
		categoryCombo.addActionListener(this);
		indVarPicker.addSubspaceListener(this);
		
		// make the outputInfo JPanel scrollable
		JScrollPane outputInfoSPane = new JScrollPane(outputInfo);
		
		// create and add items to the bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(categoryCombo);
		bottomPanel.add(new JLabel("Category"));
		bottomPanel.add(goButton);

		// add all gui items to the current window
		this.add(outputInfoSPane, BorderLayout.CENTER);
		this.add(indVarPicker, BorderLayout.WEST);
		this.add(bottomPanel, BorderLayout.SOUTH);

	}

	/**
	 * main method is used exclusively for testing and debugging purposes only
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// create & open output log file
		String logFile = DiscriminantAnalysisGUI.class.getSimpleName() + ".log";
		logger.setLevel(Level.ALL);
		logger.info(System.getProperty("java.version"));
		FileHandler logHandler = null;

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
		
		// open the sample data file
		URL testFileName = daGui.getClass().getResource("resources/iris_poly.shp");
		ShapeFileDataReader shpRead = new ShapeFileDataReader();
		shpRead.setFileName(testFileName.getFile());
		
		// now broadcast the sample data we have just read in to the this bean 
		// see the dataSetChanged method
		Object[] testDataArray = shpRead.getDataSet();

		if (testDataArray != null) {
			DataSetForApps data = new DataSetForApps(testDataArray);
			DataSetBroadcaster dataCaster = new DataSetBroadcaster();
			dataCaster.addDataSetListener(daGui);
			dataCaster.setAndFireDataSet(data);
			logger.info("Sample data loaded from " + testFileName.getFile());
		} else {
			logger.severe("Unable to read Sample data from file " + testFileName.getFile());
		}

	}
	
	/*
	 * Called when the user presses the classifier button or when a new category is chosen
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		try {
			if (e.getSource() == categoryCombo) {
				setCategoryIndex(categoryCombo.getSelectedIndex());
			} else if (e.getSource() == goButton) {
				
				// classify the data 
				classify();	
				// write the classification output to the screen (diagnostics)
				getDiagnostics();
				// send the classification output to all other listening beans
				fireNewDataSet();
			}
		} catch (DiscriminantAnalysisGUIException de) {
			String message = "unable to classify " + de.getMessage();
			JOptionPane.showMessageDialog(this, message, "WARNING", JOptionPane.WARNING_MESSAGE);
		}
	}
	
	/*
	 * Called whenever a new DataSetEvent object is broadcast
	 * @see geovista.common.event.DataSetListener#dataSetChanged(geovista.common.event.DataSetEvent)
	 */
	public void dataSetChanged(DataSetEvent e) {

		dataSet = e.getDataSetForApps();
		
		// map indices of categories in combo box to indices of attributes in the dataSet object
		categoryIndexMap = new HashMap<Integer, Integer>();
				
		// only include variables of type integer (categorical variables) in the category combo box
		// as this represents the dependent variable in the classification
		int numVars = dataSet.getColumnCount();
		int[] dTypes = dataSet.getDataTypeArray();
		categoryCombo.removeAllItems();
		
		for (int i = 0;  i < numVars; i++){
						
			if (dTypes[i] == DataSetForApps.TYPE_INTEGER) {
				categoryCombo.addItem(dataSet.getColumnName(i));
				categoryIndexMap.put(Integer.valueOf(categoryIndexMap.size()),Integer.valueOf(i));
			} 
		}
		
		// send the dataset to the independenet variable picker
		indVarPicker.dataSetChanged(e);
	}

	/*
	 * Called whenever a SubspaceEvent is broadcast from a firing bean
	 * (in this case the local variable picker)
	 * 
	 * @see geovista.common.event.SubspaceListener#subspaceChanged(geovista.common.event.SubspaceEvent)
	 */
	public void subspaceChanged(SubspaceEvent e) {

		if (e.getSource() == indVarPicker) {
			setIndVarIndices(e.getSubspace());
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
		
	
	/*
	 * Perform the classification 
	 */
	private void classify() throws DiscriminantAnalysisGUIException {
				
		// need a valid categoryIndex and an indVarIndices array to proceed	
		if (categoryIndex < 0 || indVarIndices.length == 0) {
			
			String message = ((categoryIndex < 0) ? "\nCategory Index is not set" : "");
			message += ((indVarIndices.length == 0) ? "\nIndependent Variables are not set" : "");
			
			throw new DiscriminantAnalysisGUIException(message);
		}
		
		
		// get the data for the independent variables
		double[][] data = new double[indVarIndices.length][0];
		for (int i = 0; i < indVarIndices.length; i++) {
			data[i] = dataSet.getNumericDataAsDouble(indVarIndices[i]);
		}
			
		// get the data for the dependent variable
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
							
		} catch (DiscriminantAnalysisException e ){
			throw new DiscriminantAnalysisGUIException(e.getMessage(), e.getCause());
		}		
	}
	
	/*
	 * fire a new DataSetForApps object to all listening beans
	 * this new object will contain the classification output
	 */
	
	private void fireNewDataSet() throws DiscriminantAnalysisGUIException {
		
		try {
			int numClasses = da.getUniqueClasses().length;					
			
			String[] initFieldNames = dataSet.getAttributeNamesOriginal();
			Object[] initData = dataSet.getDataSetNumeric();
			int numAttributes = dataSet.getColumnCount();
	
			
			Object[] newData = new Object[numAttributes + (1 + (numClasses*2))];
			String[] newFieldNames = new String[numAttributes + (1 + (numClasses*2))];
			
			for ( int i = 0; i < numAttributes; i++) {
				newData[i] = initData[i];
				newFieldNames[i] = initFieldNames[i];
			}
			
			newFieldNames[numAttributes] = "Classified";
			newData[numAttributes] = da.getClassified();
			
			for ( int i = 0; i < numClasses; i++) {
				newFieldNames[i+(numAttributes+1)] = "MhDist2_" + String.valueOf(i);
				newData[i+(numAttributes+1)] = da.getMahalanobisDistance2(i);
			}
			for ( int i = 0; i < numClasses; i++) {
				newFieldNames[i+(numAttributes+numClasses+1)] = "PostProb_" + String.valueOf(i);
				newData[i+(numAttributes+numClasses+1)] = da.getPosteriorProbabilities(i);
			}
			
			// create a new DataSetForApps object and send it to the other listening beans
			DataSetForApps newDataSet = new DataSetForApps(newFieldNames, newData, dataSet.getShapeData());
			fireDataSetChanged(newDataSet);
			
		} catch (DiscriminantAnalysisException e ){
			throw new DiscriminantAnalysisGUIException(e.getMessage(), e.getCause());
		}
	}
	
	/*
	 * write diagnostics on current classification to the outputInfo JPanel
	 */
	
	private void getDiagnostics() throws DiscriminantAnalysisGUIException {
		
		try {
			
			int[][] confMatrix = da.confusionMatrix();
			int[] classFreq = da.getClassFrequencies();
			double[][] params = da.getParameters();
			int numFields = da.getNumAttributes();
			int numObs = da.getNumObservations();
			int[] uniqueClasses = da.getUniqueClasses();
			int numClasses = uniqueClasses.length;
		
			String categoryName = dataSet.getNumericArrayName(categoryIndex);
			
			// basic information on classification just performed
			JTextArea info = new JTextArea("\nClassification " + Integer.toString(++numClassifications));
			info.setEditable(false);
			
			info.append("\n\nClassification Category : " + categoryName);
			info.append("\nIndependent Variables (" + Integer.toString(indVarIndices.length) + ") : ");
			for (int i=0; i < indVarIndices.length; i++) {
				info.append("\n" + dataSet.getNumericArrayName(indVarIndices[i]) + " (Variable " + 
							+ Integer.valueOf(indVarIndices[i]) + ")");
			}
					
			// confusion matrix and percentages correctly classified
			JTable cMatrixTable = new JTable(numClasses + 2, numClasses + 2);
			cMatrixTable.setBorder(BorderFactory.createLineBorder(Color.black));
			
			// classification accuracy (percentage correctly classified)
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

			
		} catch (DiscriminantAnalysisException e ){
			throw new DiscriminantAnalysisGUIException(e.getMessage(), e.getCause());
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
	
}
