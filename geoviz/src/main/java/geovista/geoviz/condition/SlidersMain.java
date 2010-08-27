/*
 * put your module comment here
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 */


package  geovista.geoviz.condition;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import geovista.common.data.DataSetForApps;

/**
 * put your documentation comment here
 */
public class SlidersMain {

	/**
	 * put your documentation comment here
	 */
	public SlidersMain () {
	}

	/**
	 * put your documentation comment here
	 * @param args
	 */
	public static void main (String[] args) {
		Object[] dataObject = new Object[5];
		double[][] dataValue =  {
			{
				235, 234, 553, 234, 342, 232, 343, 354, 212, 435
			},  {
				0.034,0.067, 0.023, 0.056, 0.0035, 0.034, 0.012,0.085, 0.095, 0.009
			},  {
				3456, 1234, 234, 2345, 577, 2455, 3566, 7644, 467, 366
			}
		};

		String[] attributes = new String[] {
			"Population", "Age", "Income"
		};

		dataObject[0] = attributes;
		dataObject[1] = dataValue[0];
		dataObject[2] = dataValue[1];
		dataObject[3] = dataValue[2];

		JFrame frame = new JFrame("Scatter Plot Matrix");

		Sliders someSliders = new Sliders();
		ConditionManager cm = new ConditionManager();
		Dimension size = new Dimension(400,200);
		someSliders.setPreferredSize(size);
		cm.setPreferredSize(size);


		//UniPlotMatrix someSliders = new UniPlotMatrix();
		//someSliders.setElementClassName("geovista.geoviz.scatterplot.ScatterPlot");
		geovista.readers.shapefile.ShapeFileDataReader shpReader = new geovista.readers.shapefile.ShapeFileDataReader();
               //String fn = "C:\\ESRI\\ESRIDATA\\USA\\STATES48.shp";
			   //String fn = "V:\\Historical-Demographic\\census\\dc_tracts\\dc_tracts.shp";
		String fn = "D:\\xiping\\dgo2003demo\\dataset\\48states.shp";
		shpReader.setFileName(fn);
		DataSetForApps dataSet = shpReader.getDataForApps();

        //ConditionManager cm = new ConditionManager();
        //cm.setDataObject(dataSet);
        //int[] cond = cm.getConditionResults();
        //someSliders.setConditionArray(cond);

		cm.addChangeListener(someSliders);
		someSliders.addChangeListener(cm);
		
		cm.setDataSet(dataSet);
		Vector title = cm.getRangesTitleVector();
		someSliders.setTitleRangesVector(title);


		//someSliders.setDataObject(dataObject);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0) ;
            }
        });
        //someSliders.invokedStandalone = true;
		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));
		frame.getContentPane().add(someSliders);
		frame.getContentPane().add(cm);
		frame.setLocation(400, 400);
		frame.setSize(600, 600);
		frame.setVisible(true);
        //frame.repaint();
		frame.setVisible(true);
		//someSliders.setDoubleDataArrays(dataValue2);
		frame.pack();
	}
}



