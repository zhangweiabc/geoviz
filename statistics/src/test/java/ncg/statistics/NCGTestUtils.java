package ncg.statistics;

/*
 * Various testing function extensions for use with NCG testing code
 * 
 * Author : Peter Foley,  03.11.2010
 * 
 */
import static org.junit.Assert.*;
import geovista.common.data.DataSetForApps;
import geovista.readers.shapefile.ShapeFileDataReader;

import java.util.logging.Logger;

public class NCGTestUtils {
	
	// logger object
	protected final static Logger logger = Logger.getLogger(NCGTestUtils.class.getPackage().getName());

	/*
	 * Compare an array of doubles
	 */
	public static void assertArrayEquals(double[] init, double[] comparison, double delta) {
		
		assertTrue(init.length == comparison.length);
		
		for ( int i = 0; i < init.length; i++) {
			assertTrue((init[i] - comparison[i]) < delta);
		}	
	}
	
	/*
	 * Compare a 2d array of doubles
	 */
	public static void assertArrayEquals(double[][] init, double[][] comparison, double delta) {

		assertTrue(init.length == comparison.length);
		assertTrue(init[0].length == comparison[0].length);
		
		for ( int i = 0; i < init.length; i++ ) {
			for ( int j = 0; j < init[0].length; j++ ) {
				assertTrue((init[i][j] - comparison[i][j]) < delta);
			}
		}	
	}
	
	/*
	 * Name    : loadShapeFile
	 * 
	 * Purpose : load shape file with name shapeFileName. returns
	 *           DataSetForApps object
	 *           
	 * Notes   : returns null pointer if an error occurs
	 */
	public static DataSetForApps loadShapeFile(String shapeFileName) {
		
		// dataset for apps object to return
		DataSetForApps dataSet = null;
		
		ShapeFileDataReader shpRead = new ShapeFileDataReader();
						
		shpRead.setFileName(shapeFileName);
		
		Object[] shapeDataArray = shpRead.getDataSet();
		

		if (shapeDataArray != null) {
			dataSet = new DataSetForApps(shapeDataArray);
			logger.info("test data loaded from " + shapeFileName);
		} else {
			logger.severe("unable to read test data from file " + shapeFileName);
		}
		
		return dataSet;
	}
}
