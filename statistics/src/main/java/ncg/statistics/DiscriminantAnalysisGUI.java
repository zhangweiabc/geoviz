/**
 *
 */
package ncg.statistics;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
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
	private transient JCheckBox doPCA = null;
	private transient JComboBox numPCAVars = null;
	private transient JCheckBox standardize = null;
	private transient JCheckBox doGWDA = null;
	private transient JCheckBox useCrossValidation = null;
	private transient JComboBox crossValidationMethod = null;
	private transient JComboBox kernelFunctionType = null;
	private transient JComboBox numNearestNeighbours = null;
	
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
		//setPreferredSize(new Dimension(500, 400));
		
		// create the class variables
		goButton = new JButton("Classify");
		resetButton = new JButton("Reset");
		categoryCombo = new JComboBox();
		doPCA = new JCheckBox("Use Principal Components Analysis");
		numPCAVars = new JComboBox();
		standardize = new JCheckBox("Standarize Independent Variables");
		doGWDA = new JCheckBox("Use Geographical Weighting");
		useCrossValidation = new JCheckBox("Use Cross Validation");
		crossValidationMethod = new JComboBox();
		numNearestNeighbours = new JComboBox();
		kernelFunctionType = new JComboBox();
		indVarPicker = new VariablePicker(DataSetForApps.TYPE_DOUBLE);
		outputInfo = new JTextArea();
				
		/*
		 * output info pane specific properties 
		 */
		outputInfo.setEditable(false);
		outputInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
		outputInfo.setLayout(new BoxLayout(outputInfo, BoxLayout.Y_AXIS));
		
		/*
		 * independent variable picker specific properties
		 */
		indVarPicker.setPreferredSize(new Dimension(180, 400));
		indVarPicker.setBorder(BorderFactory.createTitledBorder("Independent Variables"));
		
		/*
		 * principal components analysis specific properties
		 */
		doPCA.setSelected(false);     // pca is unchecked by default
		numPCAVars.setEnabled(false); // numPCAVarsis disabled when pca is unchecked
		
		/*
		 * standardization specific properties
		 */
		standardize.setSelected(false); // standardization is unchecked by default
		
		/*
		 * Geographically weighed discriminant analysis specific properties
		 */
		doGWDA.setSelected(false);                                    // default action is not to use gwda
		crossValidationMethod.addItem("Cross Validation Likelihood"); // index 0 - matches GWDiscriminantAnalysis.CROSS_VALIDATION_LIKELIHOOD
		crossValidationMethod.addItem("Cross Validation Score");      // index 1 - matches GWDiscriminantAnalysis.CROSS_VALIDATION_SCORE
		kernelFunctionType.addItem("Bisquare Kernel");                // index 0 - matches GWDiscriminantAnalysis.BISQARE_KERNEL
		kernelFunctionType.addItem("Moving Window");                  // index 1 - matches GWDiscriminantAnalysis.MOVING_WINDOW
		useCrossValidation.setSelected(false);                        // cross validation is turned off by default
		useCrossValidation.setEnabled(false);                         // cross validation is not enabled when gwda is unchecked
		numNearestNeighbours.setEnabled(false);                       //number of nearest neighbours is not enabled when gwda is unchecked
		crossValidationMethod.setEnabled(false);                      // cross validation method is not enabled when gwda is unchecked
		kernelFunctionType.setEnabled(false);                         // kernel function type is not enabled when gwda is unchecked
		
		
		
		// add this bean a listener for ActionEvents from the following components:
		goButton.addActionListener(this);
		resetButton.addActionListener(this);
		categoryCombo.addActionListener(this);
		indVarPicker.addSubspaceListener(this);
		doPCA.addActionListener(this);
		doGWDA.addActionListener(this);
		useCrossValidation.addActionListener(this);
		
		/*
		 * GridBagConstraints Constructor :
		 * gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill, insets, padx, pady
		 */
		
		// create the classification area
		JPanel classArea = new JPanel(new GridBagLayout());
		classArea.setBorder(BorderFactory.createTitledBorder("Classification"));
		classArea.add(new JLabel("Category : "),
				new GridBagConstraints(0,0,1,1,0.0,0.0,GridBagConstraints.FIRST_LINE_START,
						GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		classArea.add(categoryCombo,
				new GridBagConstraints(1,0,1,1,0.0,0.0,GridBagConstraints.FIRST_LINE_END,
						GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		classArea.add(goButton,
				new GridBagConstraints(0,1,1,1,0.0,0.0,GridBagConstraints.LAST_LINE_START,
						GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		classArea.add(resetButton,
				new GridBagConstraints(1,1,1,1,0.0,0.0,GridBagConstraints.LAST_LINE_END,
						GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		
		// create PCA area
		JPanel pcaArea = new JPanel(new GridBagLayout());
		pcaArea.setBorder(BorderFactory.createTitledBorder("Principal Components Analysis"));
		pcaArea.add(doPCA,
				new GridBagConstraints(0,0,2,1,0.0,0.0,GridBagConstraints.FIRST_LINE_START,
						GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		pcaArea.add(new JLabel("Number of Principal Components"),
				new GridBagConstraints(0,1,1,1,0.0,0.0,GridBagConstraints.LINE_START,
						GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		pcaArea.add(numPCAVars,
				new GridBagConstraints(1,1,1,1,0.0,0.0,GridBagConstraints.LINE_END,
						GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		
		// create standardization area
		JPanel stdArea = new JPanel();
		stdArea.setBorder(BorderFactory.createTitledBorder("Standardization"));
		stdArea.add(standardize, BorderLayout.WEST);
		
		// create GWDA area
		JPanel gwdaArea = new JPanel(new GridBagLayout());
		gwdaArea.setBorder(BorderFactory.createTitledBorder("Geographical Weighting"));
		gwdaArea.add(doGWDA, new GridBagConstraints(0,0,2,1,0.0,0.0,
				GridBagConstraints.FIRST_LINE_START,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		
		gwdaArea.add(new JLabel("Kernel Function"),new GridBagConstraints(0,1,1,1,0.0,0.0,
				GridBagConstraints.LINE_START,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));	
		gwdaArea.add(kernelFunctionType, new GridBagConstraints(1,1,1,1,0.0,0.0,
				GridBagConstraints.LINE_START,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		
		gwdaArea.add(new JLabel("Number of Neighbours"), new GridBagConstraints(0,2,1,1,0.0,0.0,
				GridBagConstraints.LINE_START,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		gwdaArea.add(numNearestNeighbours, new GridBagConstraints(1,2,1,1,0.0,0.0,
				GridBagConstraints.LINE_START,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		
		gwdaArea.add(useCrossValidation, new GridBagConstraints(0,3,2,1,0.0,0.0,
				GridBagConstraints.LINE_START,GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		
		gwdaArea.add(new JLabel("CV Method"),new GridBagConstraints(0,4,1,1,0.0,0.0,
				GridBagConstraints.LINE_START,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		gwdaArea.add(crossValidationMethod, new GridBagConstraints(1,4,1,1,0.0,0.0,
				GridBagConstraints.LINE_START,GridBagConstraints.NONE,new Insets(1,1,1,1),0,0));
		
		//gwdaArea.add(doGWDA, BorderLayout.WEST);
		
		// create and add items to the menu pane
		JPanel menuArea = new JPanel(new GridBagLayout());
		menuArea.add(classArea,
				new GridBagConstraints(0,0,1,1,1.0,0.0,GridBagConstraints.FIRST_LINE_START,
						GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		menuArea.add(pcaArea,
				new GridBagConstraints(0,1,1,1,1.0,0.0,GridBagConstraints.LINE_START,
						GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		menuArea.add(stdArea,
				new GridBagConstraints(0,2,1,1,1.0,0.0,GridBagConstraints.LINE_START,
						GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		menuArea.add(gwdaArea,
				new GridBagConstraints(0,3,1,1,1.0,0.0,GridBagConstraints.LAST_LINE_START,
						GridBagConstraints.HORIZONTAL,new Insets(1,1,1,1),0,0));
		
		// make the outputInfo JTextArea scrollable
		JScrollPane outputInfoSPane = new JScrollPane(outputInfo);
		
		// create a tabbed pane and add the JTextArea and 
		// the menu pane to it
		JTabbedPane tabs = new JTabbedPane();		
		tabs.add("Menu",menuArea);
		tabs.add("Output",outputInfoSPane);
			
		// add all gui items to the current window
		this.add(indVarPicker, BorderLayout.LINE_START);
		this.add(tabs,BorderLayout.CENTER);

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
		private boolean stdIndVars = false; // set to true if we are standardizing the independent variables
		private boolean pcaIndVars = false; // set to true if we will use pca on the  independent variables
		private int numPCs = -1;            // this is set to the number of principal components to use
		private boolean gwda = false;       // set to true if we are using gwda instead of lda
		private int kernel = -1;            // specify the kernel function type (moving window or bisquare kernel)
		private int numNN= -1;              // specify the number of nearest neighbours to use for adaptive bandwidth
		private boolean cv = false;         // set to true if cross validation is used to select the optimum number of nearest neighbours
		private int cvMethod = -1;          // specify the cross validation method (cross validation likelihood or cross validation score)
		
		
		public ClassifierThread() {
			super();
			daTask = new DiscriminantAnalysis();
			
			// is standardization required?
			stdIndVars = standardize.isSelected();
			
			// is pca required?
			if ( (pcaIndVars = doPCA.isSelected()) == true ) {
				numPCs = (numPCAVars.getSelectedIndex() + 1);
			}
			
			// is gwda required ?
			if ( (gwda = doGWDA.isSelected()) == true) {
				
				// get the kernel function type (moving window or bisquare kernel)
				kernel = kernelFunctionType.getSelectedIndex();
				
				// is cross-validation required to selecte the optimum
				// number of nearest neighbours
				if ( (cv = useCrossValidation.isSelected()) == true) {
					cvMethod = crossValidationMethod.getSelectedIndex();
				} else {
					
					// set the number of nearest neighbours
					numNN = (numNearestNeighbours.getSelectedIndex()  + 1);
				}
			}
			
			System.out.println("gwda is set to " + ((gwda == true) ? "TRUE" : "FALSE") );
			System.out.println("number nearest neighboubours is set to  " + numNN);
			System.out.println("cv is set to " + ((cv == true) ? "TRUE" : "FALSE") );
			System.out.println("kernel function is set to " + kernel );
			System.out.println("cross validation method is set to " + cvMethod );
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
				
				 if ( pcaIndVars == true ) {
					
					// do a PCA transformation on the independent
					// variables prior to the discriminant analysis
					PCA pcaTask = new PCA();
					pcaTask.setObservations(data, false, true);
					pcaTask.transform();
									
					// return the first numPCs principal components
					data = pcaTask.getPrincipalComponents(numPCs);
					
				}
				
				// set the independent variables and standardize if required
				daTask.setPredictorVariables(data,false,stdIndVars);
					
				// set the dependent variable (category)
				daTask.setClassification(categories);

				// set the prior probabilities to the default (equal)
				daTask.setPriorProbabilities();
					
				// classify the data
				daTask.classify();
								
			} catch (DiscriminantAnalysisException e ){
				throw new DiscriminantAnalysisGUIException(e.getMessage(), e.getCause());
			} catch (PCAException e) {
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
				
				if ( stdIndVars == true ) {
					outputInfo.append("\n\nIndpendent variables are standardized prior to classification\n");
				}
				
				if (pcaIndVars == true) {
					outputInfo.append("\n\nClassification uses the first " + numPCs + " Prinicpal Components\n");
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
						
						if ( pcaIndVars == false ) {
							classFuncParams += String.format("%-12s", dataSet.getColumnName(indVarIndices[i-1]));
						} else {
							classFuncParams += String.format("%-8s %3d", "PC", i);
						}
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
			
			// a class has been chosen - set the category index
			setCategoryIndex(categoryCombo.getSelectedIndex());
			
			// populate the number of neighbours combo box - but only
			// if the categoryIndex has been set
			if (categoryIndex > -1 ) {
				
				// compute the category class frequencies in a worker thread
				// these are used to populate the number of nearest neighbours combo box
				(new SwingWorker<Integer,Void>() {
										
					@Override
					public Integer doInBackground() {
												
						// get an array containing the categories
						int[] categories = (int[])dataSet.getColumnValues(categoryIndex);
						
						// compute the minimum category frequency
						int[] categoryFrequencies = NCGStatUtils.getFrequencies(categories);
						int categoryMinIndex = NCGStatUtils.getMin(categoryFrequencies);
						int categoryMinFrequency = categoryFrequencies[categoryMinIndex];
						
						return Integer.valueOf(categoryMinFrequency);
					}
					
					@Override
					public void done() {
						try {
							// get the category minimum class frequency
							int categoryMinFrequency = get();
							
							// populate the contents of the numNearestNeighbours combo box
							numNearestNeighbours.removeAllItems();
							for (int i = 0;i <= categoryMinFrequency; i++) {
								numNearestNeighbours.addItem(Integer.valueOf(i));
							}
							
						} catch (ExecutionException e) {
							logger.severe("unable to population nearest neighbours combo box : " + e.getMessage());
						}
						catch (InterruptedException e) {
							logger.severe("unable to population nearest neighbours combo box : " + e.getMessage());
						}	
					}
				}).execute();									
			}
	
		} else if (e.getSource() == goButton) {
			
			// perform the classification, update the gui with diagnostics and create a new DataSetForApps object
			// ready for broadcast in a separate  thread
			(new ClassifierThread()).execute();
			
		} else if (e.getSource() == doPCA ) {
			
			// when the PCA button is clicked standardization
			// is performed automatically prior to the 
			// transformation so disable the standarize button
			if (standardize.isSelected() == true) {
				standardize.setSelected(false);
			}		
			standardize.setEnabled(!doPCA.isSelected());
			
			// enable the number of pca variables
			numPCAVars.setEnabled(doPCA.isSelected());
			
			
		} else if (e.getSource() == doGWDA ) {
			
			// gwd checkbox is checked / unchecked
			
			// reset all the gwda options to the default
			kernelFunctionType.setSelectedIndex(0);    // GWDiscriminantAnalysis.BISQUARE_KERNEL
			//numNearestNeighbours.setSelectedIndex(0); 
			useCrossValidation.setSelected(false);
			crossValidationMethod.setSelectedIndex(0); // GWDiscriminantAnalysis.CROSS_VALIDATION_LIKELIHOOD
								
			// when the gwda button is clicked enable/disable gwda options
			kernelFunctionType.setEnabled(doGWDA.isSelected());
			numNearestNeighbours.setEnabled(doGWDA.isSelected());
			useCrossValidation.setEnabled(doGWDA.isSelected());
			crossValidationMethod.setEnabled(false);
										
		} else if (e.getSource() == useCrossValidation ) {
			
			// useCrossValidation checkbox is checked / unchecked 
			// this can only ever be checked when doGWDA checkbox is selected
			crossValidationMethod.setEnabled(useCrossValidation.isSelected());
			numNearestNeighbours.setEnabled(!useCrossValidation.isSelected());
			
		} else if (e.getSource() == resetButton ) {
			
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
				doPCA.setSelected(false);
				standardize.setSelected(false);
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
		
		// remove all existing items in the pca variables box
		numPCAVars.removeAllItems();
		
		// remove all existing items in the outputInfo text area
		outputInfo.setText("");
		
		// only include variables of type integer (categorical variables) in the category combo box
		// as this represents the dependent variable in the classification
		// include only variable of type double in the independent variable picker
		
		for (int i = 0;  i < numVars; i++){
						
			if (dataSet.getColumnType(i) == DataSetForApps.TYPE_INTEGER) {
				categoryCombo.addItem(dataSet.getColumnName(i));
				categoryIndexMap.put(Integer.valueOf(categoryIndexMap.size()),Integer.valueOf(i));
				numPCAVars.addItem(Integer.valueOf(categoryIndexMap.size()));
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
