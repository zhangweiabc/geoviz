package ncg.statistics;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.Test;

public class PCATest {
	
	double delta = 0.0001;
	
	protected final static Logger logger = Logger.getLogger(PCATest.class.getPackage().getName());
	
	@Test public void testPCA() {
				
		// test file name
		URL testFileName = this.getClass().getResource("resources/iris_poly.csv");
		
		// comparison file name
		URL comparisonFileName = this.getClass().getResource("resources/iris_poly_pca.csv");
		
		// eigen values / eigen vectors file name
		URL comparisonEigenFileName = this.getClass().getResource("resources/iris_poly_pca_eig.csv");
	
		// read the input test data
		ReadData testData = new ReadData(testFileName.getFile());
		
		// read the input comparison data
		ReadData comparisonData = new ReadData(comparisonFileName.getFile());
		ReadData comparisonEigenData = new ReadData(comparisonEigenFileName.getFile());
		
		try {
			
			// read the test data
			testData.readFile();
			
			// read the comparison data
			comparisonData.readFile();
			
			// read the eigen values / eigen vectors
			comparisonEigenData.readFile();
			
			// now do a pca based on the second, third
			// fourth and fifth columns (sepallength, sepalwidth, 
			// petallength, petalwidth)
			
			// extract the variables to be transformed
			int[] varCols = {1,2,3,4};
			double[][] variables = testData.getSubMatrix(varCols);
						
			// create new discriminant analysis object
			PCA pcaTask = new PCA();
			
			// set the observations (and standardize)
			pcaTask.setObservations(variables, true, true);
			
			// transform the data
			pcaTask.transform();
			
			// get the principal components
			double[][] principalComponents = pcaTask.getPrincipalComponents();
			
			// get the first four principal components
			double[][] nPrincipalComponents = pcaTask.getPrincipalComponents(4);
			
			// get the eigen values
			double[] eValues = pcaTask.getEigenValues();
			
			// get the eigen vectors
			double[][] eVectors = NCGStatUtils.transpose(pcaTask.getEigenVectors());
			
			
			//******************************************************
			// do tests
			//******************************************************
			
			int[] eVectorIndices = {1,2,3,4};
			double[][] comparisonTransformedVariables = comparisonData.getSubMatrix(varCols);
			double[] comparisionEigenValues = comparisonEigenData.getColumn(0);
			double[][] comparisonEigenVectors = comparisonEigenData.getSubMatrix(eVectorIndices);
			
			assertArrayEquals(eValues,comparisionEigenValues);
			assertArrayEquals(comparisonEigenVectors,eVectors,delta);
			assertArrayEquals(principalComponents,comparisonTransformedVariables, delta);
			assertArrayEquals(principalComponents,nPrincipalComponents, delta);
						
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
		} catch (PCAException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		}
	}

	/*
	 * Compare an array of doubles
	 */
	private void assertArrayEquals(double[] init,
			double[] comparison) {
		// TODO Auto-generated method stub
		assertTrue(init.length == comparison.length);
		
		for ( int i = 0; i < init.length; i++) {
			//System.out.println("[" + init[i] + "] : [" + comparison[i] + "]");
			assertTrue((init[i] - comparison[i]) < delta);
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
		
		for ( int i = 0; i < init.length; i++) {
			for(int j=0; j < init[0].length; j++) {
				//System.out.println("[" + init[i][j] + "] : [" + comparison[i][j] + "]");
				assertTrue((init[i][j] - comparison[i][j]) < delta);
			}
		}	
	}

}
