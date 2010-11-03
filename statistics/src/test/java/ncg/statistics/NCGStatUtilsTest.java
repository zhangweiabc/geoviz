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
			int categoryCol = 11;
			
			// get the columns associated with varCols
			double[][] testVarData = testData.getSubMatrix(varCols);
			
			// get the colum associated with categoryCol
			int[] testCategory = testData.getColumnAsInt(categoryCol);
			
			//test array of doubles
			double[] testDoubleArray = {3.5,7.8,-9.6,-19.3};
			
			// test array of ints
			int[] testIntArray = {3,7,-9,-19};
						
			// standardize the first four variables (data are in row order)
			double[][] testDataZScores = NCGStatUtils.standardize(testVarData, true);
			
			// get the comparison z scores
			double[][] comparisonZScores = comparisonData.getSubMatrix(varCols);
			
			// get test index for array of doubles
			int testDoubleIndex = NCGStatUtils.getMin(testDoubleArray);
				
			// set comparison index for array of doubles
			int comparisonDoubleIndex = 3;
			
			// get test index for array of ints
			int testIntIndex = NCGStatUtils.getMin(testIntArray);
			
			// set comparison index for array of ints
			int comparisonIntIndex = 3;
			
			// get the test unique categories
			int[] testUniqueCategories = NCGStatUtils.getUniqueItems(testCategory);
			
			// set the comparison unique categories
			int[] comparisonUniqueCategories = {1,2,3};
			
			// get the test category frequencies
			int[] testFreqCategories = NCGStatUtils.getFrequencies(testCategory);
			
			// set the comparison category frequencies
			int[] comparisonFreqCategories = {50,50,50};
			
			//******************************************************
			// do tests
			//******************************************************
			
			// test z score calculations
			NCGTestUtils.assertArrayEquals(testDataZScores,comparisonZScores,delta);
			
			// test transposition of matrices
			
			// test get minimum function for array of ints
			assertTrue(testIntIndex== comparisonIntIndex);
			
			// test get minimum function for array of doubles
			assertTrue(testDoubleIndex== comparisonDoubleIndex);
			
			// test get unique categories in array
			assertArrayEquals(testUniqueCategories,comparisonUniqueCategories);
			
			// test get category frequencies
			assertArrayEquals(testFreqCategories,comparisonFreqCategories);
						
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
	

}
