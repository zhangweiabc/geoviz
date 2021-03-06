/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.matrix.visclass;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import geovista.common.data.DataSetForApps;
import geovista.matrix.UniPlotMatrix;

public class ClassifierMLTest {

	public ClassifierMLTest() {
	}

	public static void main(String[] args) {

		JFrame frame = new JFrame("ML Classifier Test");
		UniPlotMatrix scatterPlotMatrix = new UniPlotMatrix();
		scatterPlotMatrix
				.setElementClassName("geovista.geoviz.scatterplot.ScatterPlot");

		geovista.readers.csv.CSVFileDataReader csvReader = new geovista.readers.csv.CSVFileDataReader();
		String fn = "T:\\GeoVISTA\\data\\kioloa_london.csv";
		// String fn = "T:\\GeoVISTA\\data\\tm24.csv";
		// String fn = "D:\\temp\\datatest\\testdata.csv";
		csvReader.setFileName(fn);
		Object[] trainingDataSet = csvReader.getDataSet();
		// Object[] dataSet = new Object[trainingDataSet.length-1];
		// for (int i = 0; i < dataSet.length; i ++){
		// dataSet[i] = trainingDataSet[i];
		// }

		ClassifierMaximumLikelihood classifierML = new ClassifierMaximumLikelihood();
		// classifierML.setTrainingData(trainingDataSet);
		classifierML.setClassNumber(5);
		DataSetForApps dataSet = new DataSetForApps(trainingDataSet);
		classifierML.setDataSet(dataSet);

		int[] classification;
		classification = classifierML.getClassificaiton();

		Object[] dataObject = new Object[trainingDataSet.length + 1];
		String[] trainingAttributes = (String[]) (trainingDataSet[0]);
		String[] att = new String[trainingAttributes.length + 1];
		for (int i = 0; i < trainingAttributes.length; i++) {
			att[i] = trainingAttributes[i];
		}
		att[trainingAttributes.length] = "class";
		dataObject[0] = att;
		for (int i = 1; i < trainingDataSet.length; i++) {
			dataObject[i] = trainingDataSet[i];
		}
		dataObject[trainingDataSet.length] = classification;

		// ConditionManager cm = new ConditionManager();
		// cm.setDataObject(dataSet);
		// int[] cond = cm.getConditionResults();
		// scatterPlotMatrix.setConditionArray(cond);
		DataSetForApps dataSet2 = new DataSetForApps(dataObject);
		scatterPlotMatrix.setDataSet(dataSet2);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		// scatterPlotMatrix.invokedStandalone = true;
		frame.getContentPane().add(scatterPlotMatrix);
		frame.setLocation(400, 400);
		frame.setSize(600, 600);
		frame.setVisible(true);
		// frame.repaint();
		frame.setVisible(true);
		// scatterPlotMatrix.setDoubleDataArrays(dataValue2);
		frame.pack();
	}

}