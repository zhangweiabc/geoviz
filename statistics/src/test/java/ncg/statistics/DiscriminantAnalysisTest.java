package ncg.statistics;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.Test;

public class DiscriminantAnalysisTest {
	
	double delta = 0.0001;
	
	protected final static Logger logger = Logger.getLogger(DiscriminantAnalysisTest.class.getPackage().getName());
	
	@Test public void testDiscriminantAnalysis() {
				
		// test file name
		URL testFileName = this.getClass().getResource("resources/iris_poly.csv");
		
		// comparison file name
		URL comparisonFileName = this.getClass().getResource("resources/iris_poly_da.csv");
		
		//parameter file name
		URL parameterFileName = this.getClass().getResource("resources/iris_poly_da_parameters.csv");
	
		// read the input test data
		ReadData testData = new ReadData(testFileName.getFile());
		
		// read the input comparison data
		ReadData comparisonData = new ReadData(comparisonFileName.getFile());
		ReadData parameterData = new ReadData(parameterFileName.getFile());
		
		try {
			
			// read the test data
			testData.readFile();
			
			// read the comparison data
			comparisonData.readFile();
			
			// read the parameter data
			parameterData.readFile();
			
			// now try to do discriminant analysis based on the 
			// second & third columns - sepallength and sepalwidth
			
			// extract the predictor variables (independent variables)
			int[] predCols = {1,2};
			double[][] predictorVars = testData.getSubMatrix(predCols);
			
			// get the dependent variable (class)
			int[] classification = testData.getColumnAsInt(11);
			
			// create new discriminant analysis object
			DiscriminantAnalysis da = new DiscriminantAnalysis();
			
			// set the predictor variables
			da.setPredictorVariables(predictorVars,true,false);

			// set the classification variable
			da.setClassification(classification);
			
			// set the prior probabilities
			da.setPriorProbabilities();
			
			// classify
			da.classify();
			
			// get num attributes
			int numAttributes = da.getNumAttributes();
			
			// get number of observations
			int numObservations = da.getNumObservations();
			
			// get the class frequencies
			int[] classFreq = da.getClassFrequencies();
			
			// get unique classes
			int[] uniqueClasses = da.getUniqueClasses();
			
			// get confusion matrix
			int[][] confMatrix = da.confusionMatrix();
		
			// get the classification
			int[] classified = da.getClassified();
			
			// get the classification function coefficients
			double[][] params = da.getParameters();
						
			// get mahalanobis distance squared
			double[][] mhDistance2 = da.getMahalanobisDistance2();
			
			// get posterior probabilities
			double[][] postProb = da.getPosteriorProbabilities();
			
			//******************************************************
			// do tests
			//******************************************************
			
			int comparisonNumObs = 150;
			int comparisonNumAttributes = 2;
			int[] comparisonClassFreq = {50,50,50};
			int[] comparisonUniqueClasses = {1,2,3};
			int[][] comparisonConfMatrix = {{49,1,0},{0,36,14},{0,15,35}};
			int[] comparisonClassified = comparisonData.getColumnAsInt(2);
			double[][] comparisonParams = parameterData.getData();
			int[] mhDistIndices = {6,7,8};
			double[][] comparisonMhDistance2 = comparisonData.getSubMatrix(mhDistIndices);
			int[] postProbIndices = {3,4,5};
			double[][] comparisonPostProb = comparisonData.getSubMatrix(postProbIndices);
			
			assertTrue(numObservations == comparisonNumObs);
			assertTrue(numAttributes == comparisonNumAttributes);
			assertArrayEquals(classFreq,comparisonClassFreq);
			assertArrayEquals(uniqueClasses,comparisonUniqueClasses);
			assertArrayEquals(confMatrix,comparisonConfMatrix);
			assertArrayEquals(classified,comparisonClassified);
			NCGTestUtils.assertArrayEquals(params,comparisonParams,delta);
			NCGTestUtils.assertArrayEquals(mhDistance2,comparisonMhDistance2,delta);
			NCGTestUtils.assertArrayEquals(postProb,comparisonPostProb,delta);
						
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
		} catch (DiscriminantAnalysisException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		}
	}

}
