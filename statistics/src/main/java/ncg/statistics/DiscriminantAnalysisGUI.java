/**
 *
 */
package ncg.statistics;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

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
	private transient DataSetForApps dataSet = null;
	private transient boolean newDataSetFired = false;

	// gui components
	private transient JButton goButton = null;
	private transient JButton resetButton = null;
	private transient JComboBox categoryCombo = null;
	private transient VariablePicker indVarPicker = null;
	private transient JTextArea outputInfo = null;
	
	// array of indices of independent variables from the indVarPicker object
	private transient int [] indVarIndices = new int[0];
	
	// index of dependent variable from the categoryCombo object
	private transient int categoryIndex = -1;
	
	// number of distinct classifications for the current bean
	private transient int numClassifications = 0;
	
	// categoryIndexMap maps the indices of items in the categoryCombo Box to indices of numeric attributes 
	// in dataSet (dataSetForApps object)
	private transient Map<Integer,Integer> categoryIndexMap = null;
	
	// indVarIndexMap maps the indices of items in the independent variable picker to indices of numeric attributes
	// in dataSet (dataSetForApps object)
	private transient Map<Integer,Integer> indVarIndexMap = null;
			
	// logger
	protected final static Logger logger = Logger.getLogger(DiscriminantAnalysisGUI.class.getPackage().getName());
	
	// constructor
	public DiscriminantAnalysisGUI() {

		super(new BorderLayout());
		setPreferredSize(new Dimension(500, 400));
		
		// create the class variables
		goButton = new JButton("Classify");
		resetButton = new JButton("Reset");
		categoryCombo = new JComboBox();
		indVarPicker = new VariablePicker(DataSetForApps.TYPE_DOUBLE);
		outputInfo = new JTextArea();
		outputInfo.setEditable(false);
		outputInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
		
		// set class variable specific properties
		outputInfo.setLayout(new BoxLayout(outputInfo, BoxLayout.Y_AXIS));
		indVarPicker.setPreferredSize(new Dimension(180, 400));
		indVarPicker.setBorder(BorderFactory.createTitledBorder("Independent Variables"));
		
		// add this bean a listener for the following events types:
		goButton.addActionListener(this);
		resetButton.addActionListener(this);
		categoryCombo.addActionListener(this);
		indVarPicker.addSubspaceListener(this);
		
		// make the outputInfo JPanel scrollable
		JScrollPane outputInfoSPane = new JScrollPane(outputInfo);
		
		// create and add items to the bottom panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(new JLabel("Category : "));
		bottomPanel.add(categoryCombo);
		bottomPanel.add(goButton);
		bottomPanel.add(resetButton);

		// add all gui items to the current window
		this.add(outputInfoSPane, BorderLayout.CENTER);
		this.add(indVarPicker, BorderLayout.LINE_START);
		this.add(bottomPanel, BorderLayout.PAGE_END);

	}
		
	/*
	 * ClassifierThread is an subclass of the SwingWorker class which is designed to allow
	 * a) the classification
	 * b) sending of diagnostics to the gui 
	 * c) create a new DataSetForApps object ready for broadcast to the other beans
	 * as a separate worker thread
	 * Note that b should work as we are using JTextArea.append to send to gui which is designated
	 * as thread safe by the java documentation
	 * Note also that the java Logger object is thread safe
	 */
	
	private class ClassifierThread extends SwingWorker<DataSetForApps,Void> {
		
		private DiscriminantAnalysis daTask = null;
		private DataSetForApps newDataSet = null;
		
		public ClassifierThread() {
			super();
			daTask = new DiscriminantAnalysis();
		}
		
		@Override
		public DataSetForApps doInBackground() {
						
			DataSetForApps newDataSetForApps = null;
			
			try{
				classify();
				getDiagnostics();
				newDataSetForApps = getNewDataSet();
			} catch (final DiscriminantAnalysisGUIException e ) {
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						final String message = "unable to classify : " + e.getMessage();
						logger.warning(message);
						JOptionPane.showMessageDialog(DiscriminantAnalysisGUI.this, message, "WARNING", JOptionPane.WARNING_MESSAGE);
					}
				});
				
			} catch ( final Exception e) {
				logger.severe("unhandled exception during classification : " + e.getMessage());
			}
			return newDataSetForApps;
		}
		
		@Override
		public void done() {
			try {
				
				newDataSet = get();
				
				if ( newDataSet != null ) {
					fireDataSetChanged(newDataSet);
					newDataSetFired = true;
					
					daTask = null;					
					// run the java garbage collector
					Runtime.getRuntime().gc();
				}
				
			} catch (InterruptedException e) {
				logger.severe(e.getMessage());
			} catch (ExecutionException e) {
				logger.severe(e.getMessage());
			} catch (Exception e) {
				logger.severe("unhandled exception encountered during classification : " + e.getMessage());
			}
		}
		
		/*
		 * classify the dataset
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
				Object x = dataSet.getColumnValues(indVarIndices[i]);
				if (x instanceof double[]) {
					data[i] = (double[])x;
				} else {
					logger.severe("type of column " + dataSet.getColumnName(indVarIndices[i]) + " is not double[] as expected");
					throw new DiscriminantAnalysisGUIException();
				}
				
			}
								
			//get the data for the dependent variables
			int[] categories = null;	
			Object x = dataSet.getColumnValues(categoryIndex);	
			if (x instanceof int[]) {
				categories = (int[])x;
			} else {
				logger.severe("type of column " + dataSet.getColumnName(categoryIndex) + " is not int[] as expected");
				throw new DiscriminantAnalysisGUIException();
			}
						
			try {
								
				// set the independent variables
				daTask.setPredictorVariables(data,false);
					
				// set the dependent variable (category)
				daTask.setClassification(categories);

				// set the prior probabilities to the default (equal)
				daTask.setPriorProbabilities();;
					
				// classify the data
				daTask.classify();
								
			} catch (DiscriminantAnalysisException e ){
				throw new DiscriminantAnalysisGUIException(e.getMessage(), e.getCause());
			}
		}
		
		/*
		 * Update the GUI with various diagnostics
		 * Note here that we can update the gui using the append method of the JTextArea object
		 * outputInfo as this method is considered thread safe.
		 */
		
		private void getDiagnostics() throws DiscriminantAnalysisGUIException {
			
			try {
				
				int[][] confMatrix = daTask.confusionMatrix();
				int[] classFreq = daTask.getClassFrequencies();
				double[][] params = daTask.getParameters();
				int numFields = daTask.getNumAttributes();
				int[] uniqueClasses = daTask.getUniqueClasses();
				double classAccuracy = daTask.getClassificationAccuracy();
				double randomClassAccuracy = daTask.getRandomClassificationAccuracy();
				int numClasses = uniqueClasses.length;

			
				String categoryName = dataSet.getColumnName(categoryIndex);
				
				outputInfo.append("\nClassification " + Integer.toString(++numClassifications));
				outputInfo.append("\n\nClassification Category : " + categoryName);
				outputInfo.append("\nIndependent Variables (" + Integer.toString(indVarIndices.length) + ") : ");
				for (int i=0; i < indVarIndices.length; i++) {
					outputInfo.append("\n" + dataSet.getColumnName(indVarIndices[i]));
				}
				
				// confusion matrix and percentages correctly classified		
				String confMatrixStr = "\n\nConfusion Matrix\n\n";
				confMatrixStr += String.format("%8s"," ");
				for ( int i = 0; i < numClasses; i++) {		
					confMatrixStr += " | " + String.format("%-8s","Class " + String.valueOf(uniqueClasses[i]));			
				}
				confMatrixStr += " | " + String.format("%-8s","Total") + "\n";
				for ( int i = 0; i < numClasses; i++) {	
					confMatrixStr += String.format("%-8s","Class " + String.valueOf(uniqueClasses[i]));
					for ( int j = 0; j < numClasses; j++) {	
						confMatrixStr += " | " + String.format("%8d", confMatrix[i][j]);
					}
					confMatrixStr += " | " + String.format("%8d", classFreq[i]) + "\n";
					
				}
				
				outputInfo.append(confMatrixStr);

				// classification accuracy and error rate
				outputInfo.append("\n\nClassification Accuracy                     : " + 
						String.format("%5.2f", classAccuracy * 100.0) + " %");
				outputInfo.append("\nClassification Error Rate                   : " + 
						String.format("%5.2f", (1.0 - classAccuracy) * 100.0 )  + " %");
				outputInfo.append("\nClassification Accuracy (Random Assignment) : " 
						+ String.format("%5.2f", randomClassAccuracy  * 100.0) + " %");
						
				// write out the classification function coefficients
				String classFuncParams = "\n\nClassification Function Parameters\n";
				classFuncParams += String.format("%-12s"," ");
				for (int j = 0; j < numClasses; j++) {
					
					classFuncParams += " | " + String.format("%-12s","Class " + String.valueOf(j));
				}
				classFuncParams += "\n";
				
				for (int i = 0; i < (numFields+1); i++) {
					
					if (i == 0) {
						classFuncParams += String.format("%-12s", "Intercept");
					} else {
						classFuncParams += String.format("%-12s", dataSet.getColumnName(indVarIndices[i-1]));
					}
					
					
					for (int j = 0; j < numClasses; j++) {
						classFuncParams += " | " + String.format("%12.4f",params[i][j]);
					}
					classFuncParams += "\n";
				}
				outputInfo.append(classFuncParams);
				
			} catch (DiscriminantAnalysisException e ){
				throw new DiscriminantAnalysisGUIException(e.getMessage(), e.getCause());
			}
		}
		
		/*
		 * create a new DataSetForApps ready for broadcast
		 */
		
		private DataSetForApps getNewDataSet() throws DiscriminantAnalysisException {
						
			int numClasses = daTask.getUniqueClasses().length;					
			
			int numAttributes = dataSet.getColumnCount();
						
			Object[] newData = new Object[numAttributes + (1 + (numClasses*2))];
			String[] newFieldNames = new String[numAttributes + (1 + (numClasses*2))];
			
			for ( int i = 0; i < numAttributes; i++) {
				newData[i] = dataSet.getColumnValues(i);
				newFieldNames[i] = dataSet.getColumnName(i);
			}
			
			newFieldNames[numAttributes] = "Classified";
			newData[numAttributes] = daTask.getClassified();
			
			for ( int i = 0; i < numClasses; i++) {
				newFieldNames[i+(numAttributes+1)] = "MhDist2_" + String.valueOf(i);
				newData[i+(numAttributes+1)] = daTask.getMahalanobisDistance2(i);
			}
			for ( int i = 0; i < numClasses; i++) {
				newFieldNames[i+(numAttributes+numClasses+1)] = "PostProb_" + String.valueOf(i);
				newData[i+(numAttributes+numClasses+1)] = daTask.getPosteriorProbabilities(i);
			}
			
			DataSetForApps newDataSetForApps = new DataSetForApps(newFieldNames, newData, dataSet.getShapeData());
			
			return newDataSetForApps;
		}
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
		final DiscriminantAnalysisGUI daGui = new DiscriminantAnalysisGUI();
		app.add(daGui);
		app.pack();
		app.setVisible(true);
		
		// read in the test data set and create the new dataSetForApps object in a 
		// separate worker thread
		// note that the Logger class is thread safe according to the java documentation
		(new SwingWorker<DataSetForApps,Void>() {
			
			DataSetForApps data = null;
			
			@Override
			public DataSetForApps doInBackground() {
				
				// open the sample data file
				URL testFileName = daGui.getClass().getResource("resources/iris_poly.shp");
				ShapeFileDataReader shpRead = new ShapeFileDataReader();
				shpRead.setFileName(testFileName.getFile());
				
				Object[] testDataArray = shpRead.getDataSet();
				
				DataSetForApps dataTest = null;
				
				if (testDataArray != null) {
					dataTest = new DataSetForApps(testDataArray);
					logger.info("test data loaded from " + testFileName.getFile());
				} else {
					logger.severe("unable to read test data from file " + testFileName.getFile());
				}
				
				return dataTest;
			}
			
			@Override
			public void done() {
				try {
					// get the new DataSetForApps object and broadcast it
					data = get();				
					DataSetBroadcaster dataCaster = new DataSetBroadcaster();
					dataCaster.addDataSetListener(daGui);
					dataCaster.setAndFireDataSet(data);
					
				} catch (ExecutionException e) {
					logger.severe("unable to broadcast test data : " + e.getMessage());
				}
				catch (InterruptedException e) {
					logger.severe("unable to broadcast test data : " + e.getMessage());
				}	
			}
		}).execute();
				
	}
	
	/*
	 * Called when the user presses the classifier button or when a new category is chosen
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {


		if (e.getSource() == categoryCombo) {
			setCategoryIndex(categoryCombo.getSelectedIndex());
		} else if (e.getSource() == goButton) {
			
			// perform the classification, update the gui with diagnostics and create a new DataSetForApps object
			// ready for broadcast in a separate  thread
			(new ClassifierThread()).execute();
			
		} else if (e.getSource() == resetButton) {
			
			// reset the java bean to it's initial status
			// need to rebroadcast the DataSetForApps object if it has been modified by a previous 
			// classification
			if (newDataSetFired == true) {
				fireDataSetChanged(dataSet);
				newDataSetFired = false;
				outputInfo.setText("");	
				indVarIndices = new int[0];
				categoryIndex = -1;
				numClassifications = 0;
			}
		}
	}
	
	/*
	 * Called whenever a new DataSetEvent object is broadcast
	 * @see geovista.common.event.DataSetListener#dataSetChanged(geovista.common.event.DataSetEvent)
	 */
	public void dataSetChanged(DataSetEvent e) {

		dataSet = e.getDataSetForApps();
		
		// map indices of categories in combo box to indices of numeric attributes in the dataSet object
		categoryIndexMap = new HashMap<Integer, Integer>();
		
		// map indices of variables in the variable picker to attribute indices in the dataSet object
		indVarIndexMap = new HashMap<Integer,Integer>();
				
		int numVars = dataSet.getColumnCount();
				
		// remove all existing items in the category combo box
		categoryCombo.removeAllItems();
		
		// remove all existing items in the outputInfo text area
		outputInfo.setText("");
		
		// only include variables of type integer (categorical variables) in the category combo box
		// as this represents the dependent variable in the classification
		// include only variable of type double in the independent variable picker
		
		for (int i = 0;  i < numVars; i++){
						
			if (dataSet.getColumnType(i) == DataSetForApps.TYPE_INTEGER) {
				categoryCombo.addItem(dataSet.getColumnName(i));
				categoryIndexMap.put(Integer.valueOf(categoryIndexMap.size()),Integer.valueOf(i));
			} else if ( dataSet.getColumnType(i) == DataSetForApps.TYPE_DOUBLE ) {
				indVarIndexMap.put(Integer.valueOf(indVarIndexMap.size()), Integer.valueOf(i));
			}
		}
		
		// send the dataset to the independent variable picker
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
	 * Set the independent variable indices
	 */
	private void setIndVarIndices(int[] indVarIndices) {
		
		this.indVarIndices = new int[indVarIndices.length];
		
		for ( int i = 0; i < indVarIndices.length; i++) {
			if ( indVarIndexMap.containsKey(Integer.valueOf(indVarIndices[i])) ) {
				this.indVarIndices[i] = indVarIndexMap.get(Integer.valueOf(indVarIndices[i]));
			} else {
				logger.severe("Independent Variable Index Map does not contain key " + String.valueOf(indVarIndices[i]));
			}
		}
	}
	
	/*
	 * Set the category index 
	 */
	private void setCategoryIndex(int categoryIndex) {

		if (!categoryIndexMap.isEmpty()) {
			if (categoryIndexMap.containsKey(Integer.valueOf(categoryIndex))) {
				this.categoryIndex = categoryIndexMap.get(Integer.valueOf(categoryIndex));
			} else {
				logger.severe("Category Index Map does not contain key " + String.valueOf(categoryIndex));
			}
		}
	}	
}
