/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty 
 */


package geovista.geoviz.descriptive;

import java.awt.BorderLayout;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.text.NumberFormatter;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.readers.example.GeoData48States;

/**
 * Displays descriptive statistics about a current variable, including
 * statistics for the currently selected set.
 * 
 * @author Frank Hardisty Wei Luo 
 * 
 */
public class DescriptiveStats extends JPanel

implements SelectionListener, DataSetListener {
	JTable table;
	DataSetForApps dataSet;
	NumberFormatter decMatter;
	JFormattedTextField meanFTF;
	JFormattedTextField stdDevFTF;
	JFormattedTextField skewnessFTF;
	JFormattedTextField kurtosisFTF;
	JFormattedTextField meanSelFTF;
	JFormattedTextField stdDevSelFTF;
	JFormattedTextField skewnessSelFTF;
	JFormattedTextField kurtosisSelFTF;
	JFormattedTextField numRecordsFTF;
	JFormattedTextField numRecordsSelFTF;
	
	
	int[] savedSelection;
	int selectedColumn = 0; //compute statistics for this column in dataSetForApps

	/**
	 * 
	 */

	public DescriptiveStats() {
		DecimalFormat decimalFormat = new DecimalFormat();
		decMatter = new NumberFormatter(decimalFormat);
		decMatter.setOverwriteMode(true);
		decMatter.setAllowsInvalid(false);

		JPanel currVarPanel = new JPanel();
		currVarPanel.setLayout(new BoxLayout(currVarPanel, BoxLayout.Y_AXIS));
		currVarPanel.setBorder(BorderFactory
				.createTitledBorder("Column Stats:"));
		currVarPanel.add(makeNumRecordsPanel());
		currVarPanel.add(makeMeanPanel());
		currVarPanel.add(makeStdDevPanel());
		currVarPanel.add(makeSkewnessPanel());
		currVarPanel.add(makeKurtosisPanel());
		this.add(currVarPanel);

		JPanel selObsPanel = new JPanel();
		selObsPanel.setLayout(new BoxLayout(selObsPanel, BoxLayout.Y_AXIS));
		selObsPanel.setBorder(BorderFactory
				.createTitledBorder("Selection Stats:"));
		selObsPanel.add(makeNumRecordsSelPanel());
		selObsPanel.add(makeSelMeanPanel());
		selObsPanel.add(makeSelStdDevPanel());
		selObsPanel.add(makeSelSkewnessPanel());
		selObsPanel.add(makeSelKurtosisPanel());
		this.add(selObsPanel);

		JPanel bottomPanel = new JPanel();
		bottomPanel.add(currVarPanel);
		bottomPanel.add(selObsPanel);

		setLayout(new BorderLayout());
		this.add(bottomPanel, BorderLayout.SOUTH);
		JTable table = new JTable();
		JScrollPane sPane = new JScrollPane();
		sPane.add(table);
		this.add(sPane);

	}

	private JPanel makeNumRecordsPanel() {
		JPanel NumRecordsPan = new JPanel();
		JLabel label = new JLabel("Count:");
		numRecordsFTF = new JFormattedTextField(decMatter);
		numRecordsFTF.setEditable(false);
		double num = 0;
		numRecordsFTF.setValue(num);
		NumRecordsPan.add(label);
		NumRecordsPan.add(numRecordsFTF);
		return NumRecordsPan;
	}
	
	
	
	private JPanel makeMeanPanel() {
		JPanel meanPan = new JPanel();
		JLabel label = new JLabel("Mean:");
		meanFTF = new JFormattedTextField(decMatter);
		meanFTF.setEditable(false);
		double num = 0;
		meanFTF.setValue(num);
		meanPan.add(label);
		meanPan.add(meanFTF);
		return meanPan;
	}

	private JPanel makeStdDevPanel() {
		JPanel stdDevPan = new JPanel();
		JLabel label = new JLabel("StdDev:");
		stdDevFTF = new JFormattedTextField(decMatter);
		stdDevFTF.setEditable(false);
		double num = 0;
		stdDevFTF.setValue(num);
		stdDevPan.add(label);
		stdDevPan.add(stdDevFTF);
		return stdDevPan;
	}

	private JPanel makeSkewnessPanel() {
		JPanel skewnessPan = new JPanel();
		JLabel label = new JLabel("Skewness:");
		skewnessFTF = new JFormattedTextField(decMatter);
		skewnessFTF.setEditable(false);
		double num = 0;
		skewnessFTF.setValue(num);
		skewnessPan.add(label);
		skewnessPan.add(skewnessFTF);
		return skewnessPan;
	}

	private JPanel makeKurtosisPanel() {
		JPanel kurtosisPan = new JPanel();
		JLabel label = new JLabel("Kurtosis:");
		kurtosisFTF = new JFormattedTextField(decMatter);
		kurtosisFTF.setEditable(false);
		double num = 0;
		kurtosisFTF.setValue(num);
		kurtosisPan.add(label);
		kurtosisPan.add(kurtosisFTF);
		return kurtosisPan;
	}
	
	private JPanel makeNumRecordsSelPanel() {
		JPanel NumRecordsSelPan = new JPanel();
		JLabel label = new JLabel("Count:");
		numRecordsSelFTF = new JFormattedTextField(decMatter);
		numRecordsSelFTF.setEditable(false);
		double num = 0;
		numRecordsSelFTF.setValue(num);
		NumRecordsSelPan.add(label);
		NumRecordsSelPan.add(numRecordsSelFTF);
		return NumRecordsSelPan;
	}

	private JPanel makeSelMeanPanel() {
		JPanel meanPan = new JPanel();
		JLabel label = new JLabel("Mean:");
		meanSelFTF = new JFormattedTextField(decMatter);
		meanSelFTF.setEditable(false);
		double num = 0;
		meanSelFTF.setValue(num);
		meanPan.add(label);
		meanPan.add(meanSelFTF);
		return meanPan;
	}

	private JPanel makeSelStdDevPanel() {
		JPanel stdDevPan = new JPanel();
		JLabel label = new JLabel("StdDev:");
		stdDevSelFTF = new JFormattedTextField(decMatter);
		stdDevSelFTF.setEditable(false);
		double num = 0;
		stdDevSelFTF.setValue(num);
		stdDevPan.add(label);
		stdDevPan.add(stdDevSelFTF);
		return stdDevPan;
	}

	private JPanel makeSelSkewnessPanel() {
		JPanel skewnessPan = new JPanel();
		JLabel label = new JLabel("Skewness:");
		skewnessSelFTF = new JFormattedTextField(decMatter);
		skewnessSelFTF.setEditable(false);
		double num = 0;
		skewnessSelFTF.setValue(num);
		skewnessPan.add(label);
		skewnessPan.add(skewnessSelFTF);
		return skewnessPan;
	}

	private JPanel makeSelKurtosisPanel() {
		JPanel kurtosisPan = new JPanel();
		JLabel label = new JLabel("Kurtosis:");
		kurtosisSelFTF = new JFormattedTextField(decMatter);
		kurtosisSelFTF.setEditable(false);
		double num = 0;
		kurtosisSelFTF.setValue(num);
		kurtosisPan.add(label);
		kurtosisPan.add(kurtosisSelFTF);
		return kurtosisPan;
	}

	public void selectionChanged(SelectionEvent e) {
		int[] selection = e.getSelection();
		calculateSelStats(selection);
		savedSelection = selection;

	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, savedSelection);
	}

	public void dataSetChanged(DataSetEvent e) {
		dataSet = e.getDataSetForApps();
		calculateStats();

	}
	
	public void recaculatedStats(DataSetForApps newDataSet){
	    	dataSet = newDataSet;
	    	calculateStats();

	}
	
	/*
	 * select the selectedColumn variable and recalculate the statistics
	 */
	
	public void setSelectedColumn(int column) {
		selectedColumn = column;
		calculateStats();
		calculateSelStats(new int[0]);
	}

	private void calculateStats() {
	    
	        //Object[] data1 = dataSet.get
	        
	        //ColumnValues.toString();
	        int Selectedtype = dataSet.getColumnType(selectedColumn);
	        
	        if(Selectedtype == 1 || Selectedtype==2){
	        
	        //ColumnValues get the the original column numbers from table
		double[] data = dataSet.getNumericDataAsDoubleFromOriginalData(selectedColumn);
		double[] datawithoutNaN = DescriptiveStatistics.removeNaNfromDoubleArray(data);
		//if(datawithoutNaN.length)
		//int n = datawithoutNaN.length;
		
		numRecordsFTF.setValue(datawithoutNaN.length);
		
		
		double meanValue = DescriptiveStatistics.mean(datawithoutNaN);
		meanFTF.setValue(meanValue);
		if (meanFTF.getText().equals("-0")) {
			meanFTF.setText("0");
		}
                
		double stdDev = DescriptiveStatistics.stdDev(datawithoutNaN, false);
		stdDevFTF.setValue(stdDev);
		double skewness = DescriptiveStatistics.skewness(datawithoutNaN, false);
		skewnessFTF.setValue(skewness);
		double kurtosis = DescriptiveStatistics.kurtosis(datawithoutNaN, false);
		kurtosisFTF.setValue(kurtosis);
		revalidate();
		}
	        else {
	            meanFTF.setText("0");
	            stdDevFTF.setText("0");
	            skewnessFTF.setText("0");
	            kurtosisFTF.setText("0");
	            revalidate();
	        }
	}

	private void calculateSelStats(int[] selObs) {
		
		if (dataSet == null) {
			return;
		}
		
		int Selectedtype = dataSet.getColumnType(selectedColumn);
		if(Selectedtype == 1 || Selectedtype==2){
		
		double[] originalData = dataSet.getNumericDataAsDoubleFromOriginalData(selectedColumn);
		double[] data = null;
		if (selObs.length > 0) {
			//data = DescriptiveStatistics.removeNaNfromDoubleArray(originalData);
			data = new double[selObs.length];
		    int counter = 0;
			for (int i : selObs) {
				data[counter++] = originalData[i];    
			}
		} else {
			// strangely, if we don't do this next, the figures
			// don't agree. XXX investigate why....
			meanSelFTF.setText(meanFTF.getText());
			stdDevSelFTF.setText(stdDevFTF.getText());
			skewnessSelFTF.setText(skewnessFTF.getText());
			kurtosisSelFTF.setText(kurtosisFTF.getText());
			return;
		}
		
		double[] datawithoutNaN = DescriptiveStatistics.removeNaNfromDoubleArray(data);
		numRecordsSelFTF.setValue(datawithoutNaN.length);
		double meanValue = DescriptiveStatistics.mean(datawithoutNaN);
		meanSelFTF.setValue(meanValue);
		double stdDev = DescriptiveStatistics.stdDev(datawithoutNaN, true);
		stdDevSelFTF.setValue(stdDev);
		double skewness = DescriptiveStatistics.skewness(datawithoutNaN, true);
		skewnessSelFTF.setValue(skewness);
		double kurtosis = DescriptiveStatistics.kurtosis(datawithoutNaN, true);
		kurtosisSelFTF.setValue(kurtosis);
		revalidate();
		}
		else{
		    meanSelFTF.setText("0");
		    stdDevSelFTF.setText("0");
		    skewnessSelFTF.setText("0");
		    kurtosisSelFTF.setText("0");
	            revalidate();
		}
	}

	public static void main(String[] args) {
		JFrame app = new JFrame("stats");
		DescriptiveStats stats = new DescriptiveStats();
		app.add(stats);
		app.pack();
		app.setVisible(true);
		GeoData48States stateData = new GeoData48States();
		DataSetEvent e = new DataSetEvent(stateData.getDataForApps(), app);
		stats.dataSetChanged(e);
	}

}
