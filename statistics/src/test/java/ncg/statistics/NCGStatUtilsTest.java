package ncg.statistics;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.Test;

public class NCGStatUtilsTest {
	
	double delta = 0.0001;
	
	protected final static Logger logger = Logger.getLogger(NCGStatUtilsTest.class.getPackage().getName());
	
	@Test public void testNCGStatUtils() {
				
		// test file name
		URL testFileName = this.getClass().getResource("resources/iris_poly.csv");
		
		// comparison file name
		URL comparisonFileName = this.getClass().getResource("resources/iris_poly_zscores.csv");
		
		
		// read the input test data
		ReadData testData = new ReadData(testFileName.getFile());
		
		// read the input comparison data
		ReadData comparisonData = new ReadData(comparisonFileName.getFile());
		
		try {
			
			// read the test data
			testData.readFile();
			
			// read the comparison data
			comparisonData.readFile();
			
			// now standardize the data based on the second, third
			// fourth and fifth columns (sepallength, sepalwidth, 
			// petallength, petalwidth)
			
			// extract the variables to be transformed
			int[] varCols = {1,2,3,4};
			double[][] data = testData.getSubMatrix(varCols);
						
			// standardize the first four variables (data are in row order)
			double[][] dataZScores = NCGStatUtils.standardize(data, true);
						
			//******************************************************
			// do tests
			//******************************************************
			
			
			double[][] comparisonZScores = comparisonData.getSubMatrix(varCols);
			
			assertArrayEquals(dataZScores,comparisonZScores,delta);
						
		} catch (IOException e) {
			logger.severe("Unable to read test data from [" + testFileName.getFile() + "]");
			logger.severe(e.getMessage());
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		} catch (AssertionError e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
	/*
	 * Compare a 2d array of doubles
	 */
	private void assertArrayEquals(double[][] init,
			double[][] comparison, double delta) {
		// TODO Auto-generated method stub
		assertTrue(init.length == comparison.length);
		assertTrue(init[0].length == comparison[0].length);
		
		for ( int i = 0; i < init.length; i++ ) {
			for ( int j = 0; j < init[0].length; j++ ) {
				assertTrue((init[i][j] - comparison[i][j]) < delta);
			}
		}	
	}

}
