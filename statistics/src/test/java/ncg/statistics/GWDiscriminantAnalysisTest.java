package ncg.statistics;

/*
 * Testing code for GWDiscriminantAnalysis.java
 * 
 * Author : Peter Foley, 14.12.2010
 */

import static org.junit.Assert.*;

import geovista.common.data.DataSetForApps;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.Test;

public class GWDiscriminantAnalysisTest {
	
	double delta = 0.0001;
	
	protected final static Logger logger = Logger.getLogger(GWDiscriminantAnalysisTest.class.getPackage().getName());
	
	@Test public void testDiscriminantAnalysis() {
				
		// test file name
		URL testFileName = this.getClass().getResource("resources/iris_grid_petallength.shp");
		
		// comparison file name (moving window gwda - 11 nearest neighbours)
		URL comparisonFileNameMW = this.getClass().getResource("resources/iris_gwda_output_11_mw.csv");
		
		// comparison file name (bisquare kernel gwda - 10 nearest neighbours)
		URL comparisonFileNameBK = this.getClass().getResource("resources/iris_gwda_output_10_bk.csv");
		
		// comparison cross validation file name (moving window gwda - 10-48 nearest neighbours)
		URL comparisonCVFileNameMW = this.getClass().getResource("resources/iris_gwda_cv_score_10_48_mw.csv");
		
		// comparison cross validation file name (bisquare kernel gwda - 10-48 nearest neighbours)
		URL comparisonCVFileNameBK = this.getClass().getResource("resources/iris_gwda_cv_score_10_48_bk.csv");
		
		// list of the two gwda output comparison files
		URL[] comparisonFilesList = {comparisonFileNameMW, comparisonFileNameBK};
		
		// list of the two gwda cross validation comparison files
		URL[] comparisonCVFilesList = {comparisonCVFileNameMW, comparisonCVFileNameBK};
		
		// indices of fields in the two gwda output files
		int comparisonClassifiedIndex = 2;
		int comparisonBandwidthIndex = 3;
		int[] comparisonPostProbIndices = {4,5,6};
		int[] comparisonMhDistIndices = {7,8,9};
		int[] comparisonParamIndices = {10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};
		
		// comparison confusion matrices (not in files)
		int[][] comparisonConfMatrixMW = {{50,0,0},{0,50,0},{0,0,50}};
		int[][] comparisonConfMatrixBK = {{50,0,0},{0,50,0},{0,0,50}};
		int[][][] comparisonConfMatrices = {comparisonConfMatrixMW,comparisonConfMatrixBK};
		
		// comparison data (not in files)
		int comparisonNumObs = 150;
		int comparisonNumAttributes = 4;
		int[] comparisonClassFreq = {50,50,50};
		int[] comparisonUniqueClasses = {1,2,3};
		int[] comparisonNumNeighbours = {11,10};
				
		// classification kernel function types
		int[] kernelFunctionTypes = {NCGStatUtils.MOVING_WINDOW, NCGStatUtils.BISQUARE_KERNEL};
					
		try {
			
			// read the test data
			DataSetForApps testData = NCGTestUtils.loadShapeFile(testFileName.getFile());
						
			// extract the predictor variables (independent variables)
			int[] predCols = {2,3,4,5};
			double[][] predictorVars = new double[predCols.length][0];
		
			for(int i=0;i<predCols.length;i++) {
				Object col = testData.getColumnValues(predCols[i]);
				predictorVars[i] = (double[])col;
			}
			
			// get the dependent variable (class)
			Object col = testData.getColumnValues(6);
			int[] classification = (int[])col;
		
			// compute centroids of the test data
			Point2D[] centroids = NCGStatUtils.computeCentroids(testData);
					
			for( int i=0; i < comparisonFilesList.length; i++ ) {
				
				// read the gwd output comparison data for file i
				ReadData comparisonData = new ReadData(comparisonFilesList[i].getFile());
				comparisonData.readFile();
				
				// read the gwda cross validation comparison data for file i
				ReadData comparisonCVData = new ReadData(comparisonCVFilesList[i].getFile());
				comparisonCVData.readFile();
								
				// create new gw discriminant analysis object
				GWDiscriminantAnalysis gwda = new GWDiscriminantAnalysis();
							
				// set the predictor variables
				gwda.setPredictorVariables(predictorVars,false,false);

				// set the classification variable
				gwda.setClassification(classification);
			
				// set the prior probabilities
				gwda.setPriorProbabilities();
							
				// set the kernel function type to moving window
				gwda.setKernelFunctionType(kernelFunctionTypes[i]);
			
				// set the distance matrix
				gwda.setDistanceMatrix(centroids);
				
				// set the cross validation parameters
				gwda.setUseCrossValidation(true);
				gwda.setCrossValidationMethod(NCGStatUtils.CROSS_VALIDATION_LIKELIHOOD);
				gwda.setMinNumNearestNeighboursCV(10);
				gwda.setMaxNumNearestNeighboursCV(48);
				gwda.setNumNearestNeighboursStepSizeCV(1);
			
				// classify
				gwda.classify();
				
				/*
				 * get the gwda test output
				 */
			
				// get num attributes
				int numAttributes = gwda.getNumAttributes();
				
				// get number of observations
				int numObservations = gwda.getNumObservations();
				
				// get the class frequencies
				int[] classFreq = gwda.getClassFrequencies();
				
				// get unique classes
				int[] uniqueClasses = gwda.getUniqueClasses();
			
				// get confusion matrix
				int[][] confMatrix = gwda.confusionMatrix();
			
				// get the classification
				int[] classified = gwda.getClassified();
				
				// get the bandwidths
				double[] bandwidths = gwda.getBandwidths();
				
				// get the number of nearest neighbours output by classification
				int numNeighbours = gwda.getNumNearestNeighbours();
			
				// get the classification function coefficients
				double[][] params = gwda.getParameters();
							
				// get mahalanobis distance squared
				double[][] mhDistance2 = gwda.getMahalanobisDistance2();
				
				// get posterior probabilities
				double[][] postProb = gwda.getPosteriorProbabilities();
				
				// get cross validation scores
				double[] cvScores = gwda.getCrossValidationScores();
				
				// get cross validation likelihoods
				double[] cvLikelihoods = gwda.getCrossValidationLikelihoods();
				
				// get cross validation steps
				int[] cvSteps = gwda.getNumNeighboursStepsCV();
				
				/*
				 * Get the comparison data
				 */
				
				// comparison classification data
				int[] comparisonClassified = comparisonData.getColumnAsInt(comparisonClassifiedIndex);
				double[] comparisonBandwidths = comparisonData.getColumn(comparisonBandwidthIndex);					
				double[][] comparisonParams = comparisonData.getSubMatrix(comparisonParamIndices);
				double[][] comparisonMhDistance2 = comparisonData.getSubMatrix(comparisonMhDistIndices);	
				double[][] comparisonPostProb = comparisonData.getSubMatrix(comparisonPostProbIndices);
				
				// comparison cross validation data
				int[] comparisonCVSteps = comparisonCVData.getColumnAsInt(0);
				double[] comparisonCVScores = comparisonCVData.getColumn(1);
				double[] comparisonCVLikelihoods = comparisonCVData.getColumn(2);
				
				
				//******************************************************
				// do comparison tests
				//******************************************************
				assertTrue(numObservations == comparisonNumObs);
				assertTrue(numAttributes == comparisonNumAttributes);
				assertArrayEquals(classFreq,comparisonClassFreq);
				assertArrayEquals(uniqueClasses,comparisonUniqueClasses);
				assertArrayEquals(confMatrix,comparisonConfMatrices[i]);
				assertArrayEquals(classified,comparisonClassified);
				assertTrue(numNeighbours == comparisonNumNeighbours[i]);
				NCGTestUtils.assertArrayEquals(bandwidths,comparisonBandwidths,delta);
				NCGTestUtils.assertArrayEquals(params,comparisonParams,delta);
				NCGTestUtils.assertArrayEquals(mhDistance2,comparisonMhDistance2,delta);
				NCGTestUtils.assertArrayEquals(postProb,comparisonPostProb,delta);
			
				assertArrayEquals(cvSteps,comparisonCVSteps);
				NCGTestUtils.assertArrayEquals(cvScores, comparisonCVScores, delta);
				NCGTestUtils.assertArrayEquals(cvLikelihoods, comparisonCVLikelihoods, delta);
				//return;
			}			
						
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
		} catch (DiscriminantAnalysisException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		}
	}

}
