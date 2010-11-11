package ncg.statistics;

/*
 * Testing code for NCGStatUtils.java
 * 
 * Author : Peter Foley, 04.11.2010
 */

import static org.junit.Assert.*;

import geovista.common.data.DataSetForApps;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;
import org.junit.Test;

public class NCGStatUtilsTest {
	
	double delta = 0.0001;
	
	protected final static Logger logger = Logger.getLogger(NCGStatUtilsTest.class.getPackage().getName());
	
	@Test public void testNCGStatUtils() {
				
		// test csv file name
		URL testFileName = this.getClass().getResource("resources/iris_poly.csv");
		
		// comparison csv file name
		URL comparisonFileName = this.getClass().getResource("resources/iris_poly_zscores.csv");
		
		// test shape file name
		URL testShapeFileName = this.getClass().getResource("resources/iris_grid_petallength.shp");
					
		// read the input csv test data
		ReadData testData = new ReadData(testFileName.getFile());
		
		// read the input csv comparison data
		ReadData comparisonData = new ReadData(comparisonFileName.getFile());
		
		// read the input test shape file data
		DataSetForApps testShapeData = NCGTestUtils.loadShapeFile(testShapeFileName.getFile());
				
		try {
			

			
			// read the test csv data
			testData.readFile();
			
			// read the comparison csv data
			comparisonData.readFile();
			
			// now standardize the data based on the second, third
			// fourth and fifth columns (sepallength, sepalwidth, 
			// petallength, petalwidth)
			
			// extract the variables to be transformed
			int[] varCols = {1,2,3,4};
			int categoryCol = 11;
			
			// get the columns associated with varCols
			double[][] testVarData = testData.getSubMatrix(varCols);
			
			// get the column associated with categoryCol
			int[] testCategory = testData.getColumnAsInt(categoryCol);
			
			// test 2D array of doubles
			double[][] testDouble2dArray = {{4.2,7.4},{9.2,-5.1}};
						
			//test array of doubles
			double[] testDoubleArray = {3.5,7.8,-9.6,-19.3};
			
			// test array of ints
			int[] testIntArray = {3,7,-9,-19};
						
			// comparison array of ints to test reversing array
			int[] comparisonIntArrayReversed = {-19,-9,7,3};
			
			// comparison array of indices of sorted testDoubleArray
			// ascending order
			int[] comparisonDoubleIndexSorted = {3,2,0,1};
						
			// standardize the first four variables (data are in row order)
			double[][] testDataZScores = NCGStatUtils.standardize(testVarData, true);
			
			// get the comparison z scores
			double[][] comparisonZScores = comparisonData.getSubMatrix(varCols);
			
			
			// get the transpose of the test 2d array
			double[][] testDouble2dArrayTranspose = NCGStatUtils.transpose(testDouble2dArray);
			
			// set the comparison transposed matrix
			double[][] comparisonDouble2dArrayTranspose = {{4.2,9.2},{7.4,-5.1}};
			
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
			
			// compute the centroids of the testShapeData polygons and convert them to
			// two separate arrays of doubles
			Point2D[] testShapeDataCentroids = NCGStatUtils.computeCentroids(testShapeData);
			int numObservations = testShapeData.getNumObservations();
			double[] testEastings = new double[numObservations];
			double[] testNorthings = new double[numObservations];
			for (int i = 0 ; i < numObservations; i++ ) {
				testEastings[i] = testShapeDataCentroids[i].getX();
				testNorthings[i] = testShapeDataCentroids[i].getY();
			}
			
			// get the comparison data centroids (these are attributes of testShapeData)
			double[] comparisonEastings  = (double[])testShapeData.getColumnValues(7);
			double[] comparisonNorthings = (double[])testShapeData.getColumnValues(8);
					
			// reverse the array of ints
			int[] testIntRevArrayReversed = NCGStatUtils.reverse(testIntArray);
			
			// sorte the testDoubleArray - returning indices of sorted array
			// in ascending order
			int[] testDoubleIndexSorted = NCGStatUtils.sort(testDoubleArray,false);
			
			// compute test distance matrix for centroids
			RealMatrix testDistanceMatrix = NCGStatUtils.computeDistanceMatrix(testShapeDataCentroids);
			
			// compute comparison distance matrix for centroids
			RealMatrix comparisonDistanceMatrix = MatrixUtils.createRealMatrix(numObservations,numObservations);
			for (int i= 0; i  < numObservations; i++) {
				for(int j = 0; j < numObservations; j++) {
					double dx = testEastings[i] - testEastings[j];
					double dy = testNorthings[i] - testNorthings[j];
					
					double distance = Math.sqrt(dx*dx + dy*dy);
					comparisonDistanceMatrix.setEntry(i, j, distance);
				}
			}
			
			//get attributes sepalLength and sepalWidth
			double[] sepalLength = (double[])testShapeData.getColumnValues(2);
			double[] sepalWidth = (double[])testShapeData.getColumnValues(3);
			double[][] testAttributes = new double[2][0];
			testAttributes[0] = sepalLength;
			testAttributes[1] = sepalWidth;
			
			// create weights array - the first 100 items get weight of 1
			// and the last get weights of 0
			double[] weights = new double[numObservations];
			for (int i = 0; i < numObservations; i++) {
				
				if (i  < 100) {
					weights[i] = 1.0;
				} else {
					weights[i] = 0.0;
				}
			}
			
			// compute test weighted mean
			RealVector testWeightedMean = NCGStatUtils.computeWeightedMean(testAttributes, weights);
			
			// comparison weighted mean
			double[] comparisonWeightedMean = {6.0150,3.0430};
	
			
			// compute test weighted covariance matrix
			RealMatrix testWeightedCovMatrix = NCGStatUtils.computeWeightedCovarianceMatrix(
													testAttributes, testWeightedMean.getData(), weights);
			
						
			// comparison weighted covariance matrix
			double[][] comparisonWeightedCovMatrix = {{0.7437,-0.0321},{-0.0321,0.1699}};
			
			
			// compute test mahalanobis distance squared
			// this is a bit of a cheat since the computeMahalanobisDistance2 method takes the
			// INVERSE of the covariance matrix as an argument. This is simply a numerical test 
			// however
			double[] testVector = {4.8,3};
			double testMahalanobisDistance2 = NCGStatUtils.computeMahalanobisDistance2(
														new ArrayRealVector(testVector), 
														testWeightedCovMatrix, testWeightedMean);

			// comparison mahalanobis distance squared
			double comparisonMahalanobisDistance2 = 1.0948;
			
			//
			double[] testDoubleArrayPositive = {3.5,7.8,9.6,19.3};
			double bandwidth = 10;
			
			// compute test bisquare kernel weights
			double[] testBisquareKernelWeights = NCGStatUtils.bisquareKernel(
													testDoubleArrayPositive, bandwidth);
			
			// comparison bisquare kernel weights
			double[] comparisonBisquareKernelWeights = {0.7700,0.1534,0.0061,0.0};
			
			// compute test moving window weights
			double[] testMovingWindowWeights = NCGStatUtils.movingWindow(
													testDoubleArrayPositive, bandwidth);
			
			// comparison moving window weights
			double[] comparisonMovingWindowWeights = {1.0, 1.0, 1.0, 0.0};
						
			//******************************************************
			// do tests
			//******************************************************
			
			// test z score calculations
			NCGTestUtils.assertArrayEquals(testDataZScores,comparisonZScores,delta);
			
			// test transposition of matrices
			NCGTestUtils.assertArrayEquals(testDouble2dArrayTranspose,comparisonDouble2dArrayTranspose,delta);
			
			// test get minimum function for array of ints
			assertTrue(testIntIndex== comparisonIntIndex);
			
			// test get minimum function for array of doubles
			assertTrue(testDoubleIndex== comparisonDoubleIndex);
			
			// test get unique categories in array
			assertArrayEquals(testUniqueCategories,comparisonUniqueCategories);
			
			// test get category frequencies
			assertArrayEquals(testFreqCategories,comparisonFreqCategories);
			
			// test centroid calculation code
			assertArrayEquals(testEastings,comparisonEastings,delta);
			assertArrayEquals(testNorthings,comparisonNorthings,delta);
					
			// test distance matrix calculation code
			NCGTestUtils.assertArrayEquals(testDistanceMatrix.getData(),comparisonDistanceMatrix.getData(),delta);
			
			// test sorting code
			assertArrayEquals(testDoubleIndexSorted,comparisonDoubleIndexSorted);
			
			// test reverse array code
			assertArrayEquals(testIntRevArrayReversed, comparisonIntArrayReversed);
			
			// test weighted mean calculation
			assertArrayEquals(testWeightedMean.getData(),comparisonWeightedMean,delta);
			
			// test weighted covariance matrix calculation
			NCGTestUtils.assertArrayEquals(testWeightedCovMatrix.getData(), comparisonWeightedCovMatrix, delta);
			
			// test mahanlanobis distance squared calculation
			assertTrue(Math.abs(testMahalanobisDistance2 - comparisonMahalanobisDistance2) < delta);
			
			// test moving window weights calculation
			assertArrayEquals(testMovingWindowWeights,comparisonMovingWindowWeights,delta);
			
			// test bisquare kernel weights calculation
			assertArrayEquals(testBisquareKernelWeights,comparisonBisquareKernelWeights,delta);
			
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
