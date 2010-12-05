package ncg.statistics;

import geovista.common.data.DataSetForApps;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.summary.Sum;

import com.vividsolutions.jts.algorithm.CentroidArea;
import com.vividsolutions.jts.geom.Coordinate;

/*
 * Various simple statistical methods for the ncg.statistics package
 * 
 * Author : Peter Foley
 *
 */

public class NCGStatUtils {
	
	//logger object
	private final static Logger logger = 
		Logger.getLogger(NCGStatUtils.class.getName());
	
	/*
	 * static fields
	 */
	// cross validation methods supported
	public static final int CROSS_VALIDATION_LIKELIHOOD = 0;
	public static final int CROSS_VALIDATION_SCORE = 1;

	
	// type of kernel function methods supported
	public static final int BISQUARE_KERNEL = 0;
	public static final int MOVING_WINDOW = 1;
	
	//*************************************************************************
	// Name    : crossValidationMethodToString
	// 
	// Purpose : converts the cvMethod to a String
	// 
	//*************************************************************************
	public static String crossValidationMethodToString(int cvMethod) {
				
		String crossValidationMethodAsString = null;
		
		switch (cvMethod) {
		
			case CROSS_VALIDATION_SCORE :
				crossValidationMethodAsString = "cross validation score";
				break;
			case CROSS_VALIDATION_LIKELIHOOD : 
				crossValidationMethodAsString = "cross validation likelihood";
				break;
			default : 
				crossValidationMethodAsString = "unsupported cross validation method";
				
		}
				
		return crossValidationMethodAsString;
	}
	
	
	//*************************************************************************
	// Name    : kernelFunctionTypeToString
	// 
	// Purpose : converts the kernel function type to a String
	// 
	//*************************************************************************
	public static String kernelFunctionTypeToString(int kernelType) {
		
		String kernelFunctionTypeAsString = null;
		
		switch (kernelType) {
		
			case MOVING_WINDOW : 
				kernelFunctionTypeAsString = "moving window";
				break;
			case BISQUARE_KERNEL : 
				kernelFunctionTypeAsString = "bisquare kernel";
				break;
			default : 
				kernelFunctionTypeAsString = "unsupported kernel function type";	
		}
		
		return kernelFunctionTypeAsString;
	}
	
	//*************************************************************************
	// Name    : standardize
	// 
	// Purpose : returns z scores of data array
	// 
	// Notes   : returns a zero length array if the the input is null
	//           This method does NOT modify the input array
	// 
	//*************************************************************************
	public static double[] standardize(final double[] data) {
		
		double[] dataTransform = null;
		
		if ( data != null ) {
		
			double dataMean = StatUtils.mean(data);
			double dataStdDev = Math.sqrt(StatUtils.variance(data));
			
			dataTransform = new double[data.length];
			
			for (int i = 0; i < data.length; i++ ) {
				dataTransform[i] = (data[i] - dataMean) / dataStdDev;
			}
		} else {
			dataTransform = new double[0];
		}
		return dataTransform;
	}
	
	//*************************************************************************
	// Name    : standardize
	// 
	// Purpose : returns z scores of columns of data array
	// 
	// Notes   : rowOrder is set to true if the first dimension of the input
	//           array contains the rows (observations). If rowOrder is set to
	//           false the first dimension of the input array refers to the 
	//           columns (Variables). 
	//           returns a zero length 2d array of doubles if the the input is null
	//           This method does NOT modify the input array
	// 
	//*************************************************************************
	public static double[][] standardize(double[][] data, final boolean rowOrder) {
		
		double[][] dataZScores = null;
		if (data != null) {
			
			// if rowOrder is true then the first dimension refers to the 
			// rows - need to transpose this matrix so that the first dimension
			// refers to the columns (variables)
			if (rowOrder == true) { 
					data = transpose(data);
			} 
			
			int numVars = data.length;
			
			dataZScores = new double[numVars][0];
			
			// for each column in the data set, standardize it
			for ( int j=0; j<data.length; j++ ) {
				dataZScores[j] = standardize(data[j]);
			}
			
			// if rowOrder is true then transpose the dataZScores array
			// to make the first dimension refer to the rows again - 
			// in other words it should match the input data array
			if (rowOrder == true) {
				dataZScores = transpose(dataZScores);
			}
		} else {
			dataZScores = new double[0][0];
		}
		return dataZScores;
	}
	
	//*************************************************************************
	// Name    : transpose
	// 
	// Purpose : transpose a 2d array of doubles 'data'
	// 
	// Notes   : returns a zero length array if the the input is null
	//           This method does NOT modify the input array
	// 
	//*************************************************************************
	public static double[][] transpose(double[][] data) {
				
		double[][] dataTransposed = new double[0][0];
		
		if ( data != null ) {
			
			int numRows = data.length;			
			if ( numRows > 0 ) {
				
				int numCols = data[0].length;
				dataTransposed = new double[numCols][numRows];
				for ( int i = 0; i < numRows; i++ ) {
					for ( int j = 0; j < numCols; j++ ) {
						dataTransposed[j][i] = data[i][j];
					}
				}
			}
			
		}
		
		return dataTransposed;
	}
	
	//*************************************************************************
	// Name    : getMin
	// 
	// Purpose : return the index of the minimum element in the items array
	//           items is an array of doubles
	// 
	// Notes   : returns -1 if the input array is null
	// 
	//*************************************************************************
	public static int getMin(double[] items) {
		
		int minIndex = -1;
				
		if (items != null) {
		
			if ( items.length > 0) {
				
				minIndex = 0;
	
				for (int i = 1; i < items.length; i++) {
					
					// check to see if the current item is less
					// than the minimum
					if (items[i] < items[minIndex] ) {
						minIndex = i;
					}
				}
			}
		}
	
		return minIndex;
	
	}
	
	//*************************************************************************
	// Name    : getMin
	// 
	// Purpose : return the index of the minimum element in the items array 
	//           items is an array of ints
	// 
	// Notes   : returns -1 if the input array is null
	// 
	//*************************************************************************
	public static int getMin(int[] items) {
		
		double[] doubleItems = new double[items.length];
		for (int i = 0; i < items.length; i++) {
			doubleItems[i] = items[i];
		}
	
		return getMin(doubleItems);	
	}
	
	//*************************************************************************
	// Name    : getMax
	// 
	// Purpose : return the index of the maximum element in the items array 
	//           items is an array of ints
	// 
	// Notes   : returns -1 if the input array is null
	// 
	//*************************************************************************
	public static int getMax(int[] items) {
		
		double[] doubleItems = new double[items.length];
		for (int i = 0; i < items.length; i++) {
			doubleItems[i] = items[i];
		}
	
		return getMax(doubleItems);	
	}
	
	//*************************************************************************
	// Name    : getMax
	// 
	// Purpose : return the index of the maximum element in the items array
	//           items is an array of doubles
	// 
	// Notes   : returns -1 if the input array is null
	// 
	//*************************************************************************
	public static int getMax(double[] items) {
		
		int minIndex = -1;
				
		if (items != null) {
		
			if ( items.length > 0) {
				
				minIndex = 0;
	
				for (int i = 1; i < items.length; i++) {
					
					// check to see if the current item is less
					// than the minimum
					if (items[i] > items[minIndex] ) {
						minIndex = i;
					}
				}
			}
		}
	
		return minIndex;
	
	}
	
	//*************************************************************************
	// Name    : sort
	// 
	// Purpose : returns array indices of elements of data array. descending
	//           is set to false if the sorting is to be in descending order
	//           sort is set to true if sorting is to be in ascending order
	// 
	// Notes   : returns zero length array of ints if an error occurs
	// 
	//*************************************************************************
	public static int[] sort(final double[] items, boolean descending) {
		
		Integer[] itemIndices = new Integer[items.length];
		
		
		for ( int i = 0; i < items.length; i++ ) {
			itemIndices[i] = new Integer(i);
		}
				
		Arrays.sort(itemIndices, new Comparator() {
			
			@Override
			public int compare(Object index1, Object index2) {
				
				// convert indices to ints
				int intIndex1 = ((Integer)index1).intValue();
				int intIndex2 = ((Integer)index2).intValue();
				
				if ( items[intIndex1] < items[intIndex2] ) {
					return 1;
				} else if ( items[intIndex1] > items[intIndex2] ) {
					return -1;
				} else {
					return 0;
				}
				
				
			}
			
		});
		
		int[] sortedIndices = new int[items.length];
		
		for ( int i = 0; i < items.length; i++ ) {
			sortedIndices[i] = itemIndices[i].intValue();
		}
		
		// if we want to return items in ascending order then reverse the sortedIndices array
		if (descending == false) {
			sortedIndices = reverse(sortedIndices);
		}
	
		return sortedIndices;
	
	}
	
	//*************************************************************************
	// Name    : reverse
	// 
	// Purpose : reverses order of elements in the items array. returns an
	//           array containing the reversed items
	// 
	// Notes   : 
	// 
	//************************************************************************
	public static int[] reverse(int[] items) {
		
		// array to hold the reversed items
		int[] itemsReversed = new int[items.length];
		
		// reverse the items array
		for ( int i = 0, j = (items.length-1); i <= j; i++, j-- ) {
			itemsReversed[i] = items[j];
			itemsReversed[j] = items[i];
		}
		
		return itemsReversed;
	}
	
	//*************************************************************************
	// Name    : getUniqueItems
	// 
	// Purpose : returns array containing unique items in the data array
	// 
	// Notes   : returns zero length array of ints if an error occurs
	// 
	//*************************************************************************
	public static int[] getUniqueItems(int[] data) {
		
		int[] uniqueItems = null;
			
		try {
					
			// compute unique classes in the classification array using a set
			Set<Integer> classes = new HashSet<Integer>();
			for (int i = 0; i < data.length; i++ ) {
				classes.add(data[i]);
			}
		
			// convert the classes set to an array of ints
			uniqueItems = new int[classes.size()];
			Iterator<Integer> classesIt = classes.iterator();
		
			int i = 0;
			while( classesIt.hasNext() ) {
				uniqueItems[i++] = 
					classesIt.next().intValue();
			}
				
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			uniqueItems = new int[0];
		}  
		
		return uniqueItems;
	}
	
	//*************************************************************************
	// Name    : getFrequencies
	// 
	// Purpose : returns array containing frequencies with which unique items 
	//           in the data array occur
	// 
	// Notes   : returns zero length array of ints if an error occurs
	// 
	//*************************************************************************
	public static int[] getFrequencies(int[] data) {
		
		int[] itemFrequencies = null;
		
		// get the unique items in the data array
		int[] uniqueItems = getUniqueItems(data);
		
		try {
			
			// compute the frequency of each unique item
			Frequency classFrequency = new Frequency();
			for (int i = 0; i < data.length; i++ ) {
				classFrequency.addValue(data[i]);
			}

			// save the item frequencies to an array of ints	
			itemFrequencies = new int[uniqueItems.length];
			for(int i = 0; i < uniqueItems.length; i++) {
				itemFrequencies[i] = 
					(int)classFrequency.getCount(uniqueItems[i]);
			}
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			itemFrequencies = new int[0];
		}  
		
		return itemFrequencies;
	}
	
	//*************************************************************************
	// Name    : computeCentroids
	// 
	// Purpose : returns Point2D array of centroids for a DataSetForApps 
	//           object
	// 
	// Notes   : uses the java topology suite CentroidArea class to compute polygon
	//           centroids.
	//           returns zero length array of Point2D objects if an error occurs
	// 
	//*************************************************************************
	public static Point2D[] computeCentroids(DataSetForApps data) {
		
		Point2D[] centroids = null;
		
		switch(data.getSpatialType()) {
			
			case DataSetForApps.SPATIAL_TYPE_POINT:
								
				// centroids of point data are simply the point coordinates
				centroids = data.getPoint2DData();
				
				break;
			
			case DataSetForApps.SPATIAL_TYPE_POLYGON:
								
				// get shape data representing the polygons
				Shape[] polygons = data.getShapeData();
				
				// get the total number of observations in the data object
				int numObservations = data.getNumObservations();
				
				// create an array of centroids to hold the centroid for each polygon
				centroids = new Point2D[numObservations];
				
				// go through each polyong in the data set and save it's centroid
				// in the centroids array
				for (int i=0; i < numObservations; i++) {
					
					// get shape data for the current polygon
					Shape polygon = polygons[i];
					
					// save the coordinates for the current polygon in this List
					List<Coordinate> polygonCoordinatesList = new ArrayList<Coordinate>();
					
					// get iterator for the points defining the current polygon
					PathIterator polygonIterator = polygon.getPathIterator(null);
						
					// go through each point defining the polygon and save the coordinates 
					// in the polygonCoordinatesList
					while(!polygonIterator.isDone()) {
						
						double[] coordinates = new double[6];
						
						// get the coordinates for the current vertext
						if ( polygonIterator.currentSegment(coordinates) == PathIterator.WIND_NON_ZERO) {
							
							// save the coordinates for hte current vertex in the polygonCoordinatesList
							polygonCoordinatesList.add(new Coordinate(coordinates[0],coordinates[1]));
							
						}
						
						// get the next coordinate
						polygonIterator.next();
						
					}
										
					// create a new array of Coordinates to hold the polygon vertices
					Coordinate[] polygonCoordinates = new Coordinate[polygonCoordinatesList.size()];
					
					// convert the list of polygon vertices to an array
					polygonCoordinatesList.toArray(polygonCoordinates);
										
					// compute the centroid for the current polygon
					CentroidArea polygonCentroid = new CentroidArea();
					polygonCentroid.add(polygonCoordinates);
					Coordinate centroid = polygonCentroid.getCentroid();
					centroids[i] = new Point2D.Double(centroid.x,centroid.y);
					
				}
				break;
			default:
				logger.warning("unable to compute centroids for unsupported spatial type");
				centroids = new Point2D[0];
		}
		
		return centroids;
	}
	
	//*************************************************************************
	// Name    : computeDistanceMatrix
	// 
	// Purpose : computes euclidean distance from each of points in the vertices
	//           array to every other point - these distances are returned as
	// 
	// Notes   : returns zero size RealMatrix if an error occurs
	// 
	//*************************************************************************
	public static RealMatrix computeDistanceMatrix(Point2D[] vertices) {
		
		// number of vertices in the vertices array
		int numVertices = vertices.length;
		
		// distanceMatrix will hold the distance from each vertex to all the other vertices
		RealMatrix distanceMatrix = null;
		
		try {
			// create a matrix to hold the distance from each vertex to all the others
			distanceMatrix = MatrixUtils.createRealMatrix(numVertices, numVertices);
			
			// compute the distance from each vertex to all the other vertices
			for ( int i = 0; i  < numVertices; i++ ) {
				
				// the distance from each vertex to itself is zero 
				// set all diagonal elements to zero
				distanceMatrix.setEntry(i,i,0.0);
				
				for ( int j = (i+1); j < numVertices; j++ ) {
					
					// compute the distance from vertex i to vertex j
					double distance = vertices[i].distance(vertices[j]);
					
					// distance matrix is symmetric
					distanceMatrix.setEntry(i, j, distance);
					distanceMatrix.setEntry(j, i, distance);
				}
				
			}
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			distanceMatrix = MatrixUtils.createRealMatrix(0, 0);
		}  

		return distanceMatrix;
	}
	
	//*************************************************************************
	// Name    : computeWeightedCovarianceMatrix
	// 
	// Purpose : compute weighted (biased ) covariance matrix of items in values
	//           array rows in the values array correspond to variables and columns
	//           correspond to objects. means is a vector of means
	// 
	// Notes   : returns zero size RealMatrix in case of an error    
	// 
	//*************************************************************************
	public static RealMatrix computeWeightedCovarianceMatrix(double[][] values, double[] means, double[] weights)  { 
		
		
		// covariance matrix
		RealMatrix weightedCovariance = null;
				
		try {
						
			int numVars = values.length;
			
			int numItems = values[0].length;
					
			// create the covariance matrix
			weightedCovariance = new Array2DRowRealMatrix(numVars,numVars);
			
			// compute the sum of the weights
			double sumWeights = (new Sum()).evaluate(weights);
			
			// for each variable compute the weighed covariance with all the others
			for( int i = 0; i < numVars; i++ ) {
				
				for ( int j = i; j < numVars; j++) {
					
					// compute weighted sum of squares of variables i and j
					double weightedSumSquares = 0.0;
					
					for (int k = 0; k < numItems; k++) {
						double dx1 = (values[i][k] - means[i]);
						double dx2 = (values[j][k] - means[j]);
						weightedSumSquares += (weights[k]  * dx1 * dx2);
					}
					
					// compute weighted covariance and save it
					double biasedWeightedCovariance = weightedSumSquares / sumWeights;
					weightedCovariance.setEntry(i, j, biasedWeightedCovariance);
					weightedCovariance.setEntry(j, i, biasedWeightedCovariance);
				}
			
			}
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			weightedCovariance = new Array2DRowRealMatrix(0,0);
		}   
					
		return weightedCovariance;
	}
	
	
	//*************************************************************************
	// Name    : computeWeightedMean
	// 
	// Purpose : compute weighted mean of items in the values array
	//           rows in the values array correspond to variables and columns
	//           correspond to objects. 
	// 
	// Notes   : throws a DiscriminantAnalysisException object if not set. 
	//           returns zero length vector in case of an error
	// 
	//*************************************************************************
	public static RealVector computeWeightedMean(double[][] values, double[] weights) { 
			
		// vector of weighted means
		RealVector weightedMeanVector = null;
			
		try { 
			
			int numVars = values.length;
				
			// compute the sum of the weights
			double sumWeights = (new Sum()).evaluate(weights);
			
			// create a vector to hold the weighted means
			weightedMeanVector = new ArrayRealVector(numVars);
			
			// compute weighted mean for each variable in turn
			for (int i = 0; i < numVars; i++) {
			
				// compute weighted sum for the ith variables
				double weightedItemSum = (new Sum()).evaluate(values[i],weights);
				
				// save the weighted mean for the ith variables
				weightedMeanVector.setEntry(i, weightedItemSum / sumWeights);
			}  
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			weightedMeanVector = new ArrayRealVector();
		}  
			
		return weightedMeanVector;
	}
	
	//*************************************************************************
	// Name    : computeMahalanobisDistance2
	// 
	// Purpose : computes mahalanobis distance squared between a vector x and 
	//           a distribution with mean vector mean and covariance matrix 
	//           with inverse covInv
	// 
	// Notes   : returns a mahalanobis distance square of -1.0 in case of an error
	// 
	//*************************************************************************
	public static double computeMahalanobisDistance2(RealVector x, RealMatrix covInv, RealVector mean) {
		
		double mhd2 = -1.0;
		
		try {
			
			RealVector meanDiff = x.subtract(mean);	
			mhd2 = covInv.preMultiply(meanDiff).dotProduct(meanDiff);
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			mhd2 = -1.0;
		} 
				
		return mhd2;
	}
	
	//*************************************************************************
	// Name    : movingWindow
	// 
	// Purpose : computes a weights array based on an array of distances and a 
	//           bandwidth using a moving window function
	//
	// 
	// Notes   : returns zero length array in case of an error
	// 
	//*************************************************************************
	public static double[] movingWindow(double[] distances, double bandwidth) {
		
		// reference to weights array
		double[] weights = null;
		
		try {
			
			// create the weights array
			weights = new double[distances.length];
			
			// compute weights for each distances
			for (int i = 0; i < distances.length; i++) {
				if (distances[i] < bandwidth) {
					weights[i] = 1.0;
				} else {
					weights[i] = 0.0;
				}
			}
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			weights = new double[0];
		} 
		
		return weights;
	}
	
	//*************************************************************************
	// Name    : bisquareKernel
	// 
	// Purpose : computes a weights array based on an array of distances and a 
	//           bandwidth using a bisquare kernel function
	//
	// 
	// Notes   : returns zero length array in case of an error
	// 
	//*************************************************************************
	public static double[] bisquareKernel(double[] distances, double bandwidth) {
		
		// reference to weights array
		double[] weights = null;
		
		try {
			
			// create the weights array
			weights = new double[distances.length];
			
			// compute weights for each distances
			for (int i = 0; i < distances.length; i++) {
				if (distances[i] < bandwidth) {
					weights[i] = Math.pow((1.0 - Math.pow((distances[i]/bandwidth), 2.0)),2.0);
				} else {
					weights[i] = 0.0;
				}
			}
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			weights = new double[0];
		} 
		
		return weights;
	}


}
