package ncg.statistics;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

/* 
* Implementation of Geographically Weighted Discriminant Analysis
*
* Author : Peter Foley, 29.10.2010
*
* Method taken from 'Geographically Weighted Discriminant Analysis' by Chris Brundson,
* Stewart Fotheringham and Martin Charlton, Geographical Analysis, Volum 39, Issue 4 pp376-96 2007
*  
*/

public class GWDiscriminantAnalysis extends DiscriminantAnalysis {
	
	// default number of nearest neighbours to use
	protected static final int DEFAULT_INT = -1;
		
	/*
	 * Input variables
	 */
	
	// matrix to hold distances from each object to every other object
	protected transient RealMatrix distanceMatrix = null;
	
	// number of nearest neighbours to use for gwda
	protected transient int numNearestNeighbours = DEFAULT_INT;
	
	// set to true if number of nearest neighbours is to be chosen using cross validation
	protected transient boolean useCrossValidation = false;
	
	// holds type of cross validation method to use for gwda
	protected transient int crossValidationMethod = DEFAULT_INT;
	
	// minimum and maximum number of nearest neighbours and step size to use for cross validation
	protected transient int minNumNearestNeighboursCV = DEFAULT_INT;
	protected transient int maxNumNearestNeighboursCV = DEFAULT_INT;
	protected transient int numNearestNeighboursStepSizeCV = DEFAULT_INT;
	
	// kernel function to use for gwda
	protected transient int kernelFunctionType = DEFAULT_INT;
	
	// array of doubles holds the bandwidths used in the classification of each object
	protected transient double[] bandwidths = null;
	
	// array of ints hold the steps if cross validation is used to select the number of nearest neighbours
	protected transient int[] numNeighboursStepsCV = null;
	
	// array of doubles to hold the cross validation scores (output of cross validation)
	protected transient double[] crossValidationScores = null;
	
	// array of doubles to hold the cross validation likelihoods (output of cross validation)
	protected transient double[] crossValidationLikelihoods = null;
	                        
	                 		
	/*
	 * internal variables
	 */
	
	// classIndices is a list of integer arrays which contain the indices of 
	// items in each class. This is set by the setClassIndices method.
	//protected transient List<int[]> classIndices = null;
	
	//logger object
	protected final static Logger logger = 
		Logger.getLogger(GWDiscriminantAnalysis.class.getName());
	
	//*************************************************************************
	// Name    : GWDiscriminantAnalysis
	// 
	// Purpose : empty constructor (java bean requirement)
	// 
	// Notes   : 
	// 
	//*************************************************************************
	public GWDiscriminantAnalysis() {}
	
	//*************************************************************************
	// Name    : reset
	// 
	// Purpose : reset all input variables 
	// 
	// Notes   : also frees up memory by running the java garbage collector
	//           (this is run by the super class)
	// 
	//*************************************************************************
	public void reset() {
				
		// reset geographically weighted specific variables
		distanceMatrix = null;
		numNearestNeighbours = DEFAULT_INT;
		useCrossValidation = false;
		crossValidationMethod = DEFAULT_INT;
		minNumNearestNeighboursCV = DEFAULT_INT;
		maxNumNearestNeighboursCV = DEFAULT_INT;
		numNearestNeighboursStepSizeCV = DEFAULT_INT;
		kernelFunctionType = DEFAULT_INT;
		bandwidths = null;
		numNeighboursStepsCV = null;
		crossValidationScores = null;
		crossValidationLikelihoods = null;
		
		// call super class reset
		super.reset();
		
	}
	
	/*
	 * various methods to check whether certain class attributes required to classify and output
	 * by the classification process have been set
	 * if they are not set then a DiscriminantAnalysisException object is thrown
	 */

	//*************************************************************************
	// Name    : validateDistanceMatrix
	// 
	// Purpose : check to see if distanceMatrix has been set
	// 
	// Notes   : distanceMatrix is a RealMatrix containing the distance of each the
	//           predictor objects to every other distance object.
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	protected void validateDistanceMatrix() throws DiscriminantAnalysisException {		
		if ( distanceMatrix == null ) {
			throw new DiscriminantAnalysisException("distance matrix not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateNumNearestNeighbours
	// 
	// Purpose : check to see if numNearestNeighbours has been set
	// 
	// Notes   : numNearestNeighours is an integer containing the number of
	//           nearest neighbours to use for the classification
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	protected void validateNumNearestNeighbours() throws DiscriminantAnalysisException {		
		if ( numNearestNeighbours < 1 ) {
			throw new DiscriminantAnalysisException("number of nearest neighbours not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateCrossValidationMethod
	// 
	// Purpose : check to see if crossValidationMethod is valid
	// 
	// Notes   : crossValidationMethod defines the method used to select
	//           the optimum number of nearest neighbours ( cross validation
	//           score or cross validation likelihood)
	//           throws a DiscrimiantAnalysisException if invalid cross validation method
	//           is set
	//
	//*************************************************************************
	protected void validateCrossValidationMethod() throws DiscriminantAnalysisException {		
		
		switch (crossValidationMethod) {
		
			case NCGStatUtils.CROSS_VALIDATION_SCORE : 
				break;
			case NCGStatUtils.CROSS_VALIDATION_LIKELIHOOD : 
				break;
			default : 
				throw new DiscriminantAnalysisException("cross validation method not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateMinNumNearestNeighboursCV
	// 
	// Purpose : check to see if minNumNearestNeighboursCV has been set
	// 
	// Notes   : this variable is used together with maxNumNearestNeighboursCV 
	//           and numNearestNeighboursStepSizeCV to create a range of 
	//           neighbourhood values to test for 
	//           throws a DiscrimiantAnalysisException if not set
	//
	//*************************************************************************
	protected void validateMinNumNearestNeighboursCV() throws DiscriminantAnalysisException {
		if ( minNumNearestNeighboursCV < 1) {
			throw new DiscriminantAnalysisException("minimum number of nearest neighbours for cross validation not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateMaxNumNearestNeighboursCV
	// 
	// Purpose : check to see if maxNumNearestNeighboursCV has been set
	// 
	// Notes   : this variable is used together with minNumNearestNeighboursCV 
	//           and numNearestNeighboursStepSizeCV to create a range of 
	//           neighbourhood values to test for 
	//           throws a DiscrimiantAnalysisException if not set
	//
	//*************************************************************************
	protected void validateMaxNumNearestNeighboursCV() throws DiscriminantAnalysisException {
		if ( maxNumNearestNeighboursCV < 1) {
			throw new DiscriminantAnalysisException("maximum number of nearest neighbours for cross validation not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateNumNearestNeighboursStepSizeCV
	// 
	// Purpose : check to see if numNearestNeighboursStepSizeCV has been set
	// 
	// Notes   : this variable is used together with minNumNearestNeighboursCV 
	//           and maxNumNearestNeighboursCV to create a range of 
	//           neighbourhood values to test for 
	//           throws a DiscrimiantAnalysisException if not set
	//
	//*************************************************************************
	protected void validateNumNearestNeighboursStepSizeCV() throws DiscriminantAnalysisException {
		if ( numNearestNeighboursStepSizeCV < 1) {
			throw new DiscriminantAnalysisException("number of nearest neighbours step size for cross validation not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateKernelFunctionType
	// 
	// Purpose : check to see if kernelFunctionType is valid
	// 
	// Notes   : kernelFunctionType defines the kernel function used to select
	//           weights for the geographially weighted discriminanat analysis
	//           (moving window or gaussian kernel)
	//           throws a DiscriminantAnalysisException if an invalid kernel
	//           function type is set
	// 
	//*************************************************************************
	protected void validateKernelFunctionType() throws DiscriminantAnalysisException {		
		
		switch (kernelFunctionType) {
		
			case NCGStatUtils.MOVING_WINDOW :  
				break;
			case NCGStatUtils.BISQUARE_KERNEL : 
				break;
			default : 
				throw new DiscriminantAnalysisException("kernel function type is not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateBandwidths
	// 
	// Purpose : check to see if bandwidths have been set
	// 
	// Notes   : bandwidths is an array of doubles which contains the bandwidths
	//           used for the classification
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	protected void validateBandwidths() throws DiscriminantAnalysisException {		
		if ( bandwidths == null ) {
			throw new DiscriminantAnalysisException("bandwidths have not been computed");
		}
	}
	
	//*************************************************************************
	// Name    : validateNumNeighboursStepsCV
	// 
	// Purpose : check to see if numNearestNeighboursStepsCV has been set
	// 
	// Notes   : throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	protected void validateNumNeighboursStepsCV() throws DiscriminantAnalysisException {		
		if ( numNeighboursStepsCV == null ) {
			throw new DiscriminantAnalysisException("cross validation steps have not been computed");
		}
	}
	
	//*************************************************************************
	// Name    : validateCrossValidationScores
	// 
	// Purpose : check to see if crossValidationScores has been set
	// 
	// Notes   : throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	protected void validateCrossValidationScores() throws DiscriminantAnalysisException {		
		if ( crossValidationScores == null ) {
			throw new DiscriminantAnalysisException("cross validation scores have not been computed");
		}
	}
	
	//*************************************************************************
	// Name    : validateCrossValidationLikelihoods
	// 
	// Purpose : check to see if crossValidationLikelihoods has been set
	// 
	// Notes   : throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	protected void validateCrossValidationLikelihoods() throws DiscriminantAnalysisException {		
		if ( crossValidationLikelihoods == null ) {
			throw new DiscriminantAnalysisException("cross validation likelihoods have not been computed");
		}
	}
			
	/*
	 * methods to set class variables
	 */
	
	
	//*************************************************************************
	// Name    : setDistanceMatrix
	// 
	// Purpose : sets the distance matrix for for the objects to be classified
	//           centroids is an array of Point2D objects holding the centroid
	//           for each polygon / point that is to be classified
	// 
	// Notes   : if an error occurs distanceMatrix will be set to a zero size
	//           RealMatrix
	//
	//*************************************************************************
	public void setDistanceMatrix(Point2D[] centroids) {
		
		// compute the 	distance from each centroid to all the other centroids
		distanceMatrix = NCGStatUtils.computeDistanceMatrix(centroids);
							
	}
	
	//*************************************************************************
	// Name    : setNumNearestNeighbours
	// 
	// Purpose : sets the numNearestNeighbours attribute
	// 
	// Notes   : numNearestNeighours is an integer containing the number of
	//           nearest neighbours to use for the classification
	//
	//*************************************************************************
	public void setNumNearestNeighbours(int numNearestNeighbours) {
		this.numNearestNeighbours = numNearestNeighbours;
	}
	
	//*************************************************************************
	// Name    : setUseCrossValidation
	// 
	// Purpose : set the useCrossValidation attribute
	// 
	// Notes   : useCrossValidation is set to true if cross validation is to
	//           be used to select the optimum number of nearest neighbours
	//           for the classification
	// 
	//*************************************************************************
	public void setUseCrossValidation(boolean useCrossValidation) {
		this.useCrossValidation = useCrossValidation;
	}
	
	//*************************************************************************
	// Name    : setCrossValidationMethod
	// 
	// Purpose : set the crossValidationMethod attribute
	// 
	// Notes   : crossValidationMethod refers to the technique used to choose
	//           the optimum number of nearest neighbours
	//           throws a DiscriminantAnalysisException object if an invalid
	//           value is used
	// 
	//*************************************************************************
	public void setCrossValidationMethod(int crossValidationMethod) throws DiscriminantAnalysisException {
		
		this.crossValidationMethod = crossValidationMethod;
		validateCrossValidationMethod();
	}
	
	//*************************************************************************
	// Name    : setMinNumNearestNeighboursCV
	// 
	// Purpose : sets the minNumNearestNeighboursCV attribute
	// 
	// Notes   : mininNumNearestNeighboursCV is an integer containing minimum the number of
	//           nearest neighbours to use for the cross validation
	//
	//*************************************************************************
	public void setMinNumNearestNeighboursCV(int minNumNearestNeighboursCV) {
		this.minNumNearestNeighboursCV = minNumNearestNeighboursCV;
	}
	
	//*************************************************************************
	// Name    : setMaxNumNearestNeighboursCV
	// 
	// Purpose : sets the maxNumNearestNeighboursCV attribute
	// 
	// Notes   : numNearestNeighours is an integer containing the maximum number of
	//           nearest neighbours to use for the cross validation
	//
	//*************************************************************************
	public void setMaxNumNearestNeighboursCV(int maxNumNearestNeighboursCV) {
		this.maxNumNearestNeighboursCV = maxNumNearestNeighboursCV;
	}
	
	//*************************************************************************
	// Name    : setNumNearestNeighboursStepSizeCV
	// 
	// Purpose : sets the numNearestNeighboursStepSizeCV attribute
	// 
	// Notes   : numNearestNeighboursStepSizeCV is an integer containing 
	//           step size to be used for the cross validation range defined
	//           by [minNumNearestNeighboursCV,maxNumNearestNeighboursCV]
	//
	//*************************************************************************
	public void setNumNearestNeighboursStepSizeCV(int numNearestNeighboursStepSizeCV) {
		this.numNearestNeighboursStepSizeCV = numNearestNeighboursStepSizeCV;
	}
		
	//*************************************************************************
	// Name    : setKernelFunctionType
	// 
	// Purpose : set the kernelFunctionType attribute
	// 
	// Notes   : kernelFunctionType sets the kernel function type (gaussian or 
	//           moving window) to be used for the geographically weighted 
	//           discrimiant analysis
	//           throws a DiscriminantAnalysisException object if an invalid 
	//           kernelFunctionType is used
	// 
	//*************************************************************************
	public void setKernelFunctionType( int kernelFunctionType ) throws DiscriminantAnalysisException {
		this.kernelFunctionType = kernelFunctionType;
		validateKernelFunctionType();
	}
	
	/*
	 * get methods for class variables
	 */
	
	//*************************************************************************
	// Name    : getDistanceMatrix
	// 
	// Purpose : returns the distanceMatrix
	// 
	// Notes   : distanceMatrix is a matrix containing the distances from each
	//           observation to every other observation
	//           throws a new DiscriminantAnalysisException if it has not been set
	// 
	//*************************************************************************
	public double[][] getDistanceMatrix() throws DiscriminantAnalysisException {
		validateDistanceMatrix();
		return distanceMatrix.getData();
	}
	
	//*************************************************************************
	// Name    : getNumNearestNeighbours
	// 
	// Purpose : returns the value of the numNearestNeighbours attribute
	// 
	// Notes   : numNearestNeighours is an integer containing the number of n
	//           nearest neighbours to use for the classification
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public int getNumNearestNeighbours() throws DiscriminantAnalysisException {
		validateNumNearestNeighbours();
		return numNearestNeighbours;
	}
	
	//*************************************************************************
	// Name    : getUseCrossValidation
	// 
	// Purpose : returns the value of the useCrossValidation attribute
	// 
	// Notes   : useCrossValidation is set to true if cross validation is to
	//           be used to select the optimum number of nearest neighbours
	//           for the classification
	// 
	//*************************************************************************
	public boolean getUseCrossValidation() {
		return useCrossValidation;
	}
	
	//*************************************************************************
	// Name    : getCrossValidationMethod
	// 
	// Purpose : returns the value of the crossValidationMethod attribute
	// 
	// Notes   : crossValidationMethod defines the method used to select
	//           the optimum number of nearest neighbours ( cross validation
	//           score or cross validation likelihood)
	//           throws a DiscrimiantAnalysisException if invalid cross validation method
	//           is set
	// 
	//*************************************************************************
	public int getCrossValidationMethod() throws DiscriminantAnalysisException {
		validateCrossValidationMethod();
		return crossValidationMethod;
	}
	
	//*************************************************************************
	// Name    : getMinNumNearestNeighbourCV
	// 
	// Purpose : returns the value of the minNumNearestNeighboursCV attribute
	// 
	// Notes   : minNumNearestNeighoursCV is an integer containing the minimum number of
	//           nearest neighbours to use for the cross validation
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public int getMinNumNearestNeighbourCV() throws DiscriminantAnalysisException {
		validateMinNumNearestNeighboursCV();
		return minNumNearestNeighboursCV;
	}
	
	//*************************************************************************
	// Name    : getMaxNumNearestNeighbourCV
	// 
	// Purpose : returns the value of the maxNumNearestNeighboursCV attribute
	// 
	// Notes   : minNumNearestNeighoursCV is an integer containing the maximum number of
	//           nearest neighbours to use for the cross validation
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public int getMaxNumNearestNeighbourCV() throws DiscriminantAnalysisException {
		validateMaxNumNearestNeighboursCV();
		return maxNumNearestNeighboursCV;
	}
	
	//*************************************************************************
	// Name    : getNumNearestNeighboursStepSizeCV
	// 
	// Purpose : gets the value of the numNearestNeighboursStepSizeCV attribute
	// 
	// Notes   : numNearestNeighboursStepSizeCV is an integer containing 
	//           step size to be used for the cross validation range defined
	//           by [minNumNearestNeighboursCV,maxNumNearestNeighboursCV]
	//           throws a new DiscriminantAnalysisException if it has not been  set
	//
	//*************************************************************************
	public int getNumNearestNeighboursStepSizeCV() throws DiscriminantAnalysisException {
		validateNumNearestNeighboursStepSizeCV();
		return numNearestNeighboursStepSizeCV;
	}
			
	//*************************************************************************
	// Name    : getKernelFunctionType
	// 
	// Purpose : returns the value of the kernelFunctionType attribute
	// 
	// Notes   : kernelFunctionType defines the kernel function used to select
	//           weights for the geographically weighted discriminant analysis
	//           (moving window or gaussian kernel)
	//           throws a DiscriminantAnalysisException if an invalid kernel
	//           function type is set
	// 
	//*************************************************************************
	public int getKernelFunctionType() throws DiscriminantAnalysisException {
		validateKernelFunctionType();
		return kernelFunctionType;
	}
	
	//*************************************************************************
	// Name    : getBandwidths
	// 
	// Purpose : returns the value of the bandwidths attribute
	// 
	// Notes   : kernelFunctionType defines the kernel function used to select
	//           weights for the geographically weighted discriminant analysis
	//           (moving window or gaussian kernel)
	//           throws a DiscriminantAnalysisException if an invalid kernel
	//           function type is set
	// 
	//*************************************************************************
	public double[] getBandwidths() throws DiscriminantAnalysisException {
		validateBandwidths();
		return Arrays.copyOf(bandwidths,bandwidths.length);
	}
	
	//*************************************************************************
	// Name    : getNumNeighboursStepsCV
	// 
	// Purpose : returns the value of the numNeighboursStepsCV attribute
	// 
	// Notes   : numNeighboursStepsCV is an array of ints containing a list of the 
	//           number of nearest neighbours used for a cross validation run
	//           throws a DiscriminantAnalysisException if not set
	// 
	//*************************************************************************
	public int[] getNumNeighboursStepsCV() throws DiscriminantAnalysisException {
		validateNumNeighboursStepsCV();
		return Arrays.copyOf(numNeighboursStepsCV, numNeighboursStepsCV.length);
	}
	
	//*************************************************************************
	// Name    : getCrossValidationScores
	// 
	// Purpose : returns the value of the crossValidationScores attribute
	// 
	// Notes   : crossValidationScores is an array of doubles containing a list of the 
	//           classification accuracies for each of the steps in the cross validation run
	//           throws a DiscriminantAnalysisException if not set
	// 
	//*************************************************************************
	public double[] getCrossValidationScores() throws DiscriminantAnalysisException {
		validateCrossValidationScores();
		return Arrays.copyOf(crossValidationScores, crossValidationScores.length);
	}
	
	//*************************************************************************
	// Name    : getCrossValidationLikelihoods
	// 
	// Purpose : returns the value of the crossValidationLikelihoods attribute
	// 
	// Notes   : crossValidationLikelihoods is an array of doubles containing a list of the 
	//           classification accuracies in terms of the sum of the posterior probabilities
	//           for each of the steps in the cross validation run
	//           throws a DiscriminantAnalysisException if not set
	// 
	//*************************************************************************
	public double[] getCrossValidationLikelihoods() throws DiscriminantAnalysisException {
		validateCrossValidationLikelihoods();
		return Arrays.copyOf(crossValidationLikelihoods, crossValidationLikelihoods.length);
	}
		
	//*************************************************************************
	// Name    : computeBandwidth
	// 
	// Purpose : computes the bandwidth required to enclose at least numNearestNeighbours
	//           of objectIndex in all classes
	// 
	// Notes   : throws a DiscriminantAnalysisException Object if uniqueClasses,
	//           numClasses, distanceMatrix, classIndices or numNearestNeighbours
	//           are not set. Returns -1 if an exception occurs
	// 
	//*************************************************************************
	protected double computeBandwidth( int objectIndex, boolean excludeObject) throws DiscriminantAnalysisException {
		
		validateUniqueClasses();
		validateNumClasses();
		validateNumNearestNeighbours();
		validateDistanceMatrix();
		validateClassIndices();
		validateClassification();
		
		// compute the bandwidth required to enclose at least numNearestNeighbours of
		// object with index objectIndex in each class
		double bandwidth = -1.0;
		
		try {
						
			int[] objectIndexArr = {objectIndex};
					
			for ( int c = 0; c < numClasses; c++ ) {
									
				// get the distances from objectIndex to all other objects in the same class
			 	double[] distances = distanceMatrix.getSubMatrix(objectIndexArr, classIndices.get(c)).getRow(0);
			 	
			 	// sort these distances in ascending order
			 	int[] distanceIndicesSorted = NCGStatUtils.sort(distances, false);
			 	
			 	// maximum number of neighbours allowed for the current class
			 	int maxNumNeighbours = (distanceIndicesSorted.length - 1);
			 	
			 	// index for the bandwidth that encloses numNearestNeighbours of the current object
			 	int neighIndex = numNearestNeighbours;
			 	
			 	// is the current object to be included in the calculation of the bandwidth?
			 	if (excludeObject == true) {
			 		
			 		// the current object is to be excluded from the bandwidth calculation.
			 		// Do this by checking if it is in the current class
			 		// if it is, increment the neighIndex by one
			 		if (classification[objectIndex] == uniqueClasses[c]) {
			 			neighIndex++;		 			
			 		}
			 	}
			 			 	
			 	if (numNearestNeighbours > maxNumNeighbours) {
			 		logger.warning("number of nearest neighbours [" + numNearestNeighbours + 
			 				"] is greater than maximum number of nearest neighbours allowed for class [" + 
			 				uniqueClasses[c] +"] : [" + maxNumNeighbours+ "]");
			 		logger.warning("using maximum number of nearest neighbours ");
			 		neighIndex = maxNumNeighbours;
			 	}
			 	
			 	// get the bandwidth required to enclose numNearestNeighbours neighbours
			 	int bandwithObjectIndex = distanceIndicesSorted[neighIndex];				
			 	double currentBandwidth = distances[bandwithObjectIndex];
			 	
			 	// need to make sure that the bandwidth required to enclose numNearestNeighbours of objectIndex is actually
			 	// greater than the bandwidth required to enclose (numNearestNeighbours-1) neighbours of objectIndex. 
			 	// if not, then choose the next greatest distance
			 	
			 	int bandwithPrevObjectIndex = distanceIndicesSorted[neighIndex-1];				
			 	double prevBandwidth = distances[bandwithPrevObjectIndex];
			 	
			 	// keep checking until currentBandwidth > prevBandwidth
			 	while (currentBandwidth == prevBandwidth) {
			 		
			 		int nextNeighIndex = neighIndex+1;
			 		
			 		if (nextNeighIndex <= maxNumNeighbours) {
			 			
			 			// set prevBandwidth to be the currentBandwidth and
			 			// set the currentBandwidth to point to the next distance
			 			prevBandwidth = currentBandwidth;
			 		
			 			bandwithObjectIndex = distanceIndicesSorted[nextNeighIndex];				
			 			currentBandwidth = distances[bandwithObjectIndex];
			 			
			 			neighIndex = nextNeighIndex;
			 			
			 		} else {
			 			
			 			// if we can't check any more neighbours then stop
			 			break;
			 		}
			 	}
			 				 	
				// is the currentBandwidth greater than the previous value?
				if (currentBandwidth > bandwidth) {
					bandwidth = currentBandwidth;
				}							
			}
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			bandwidth = -1.0;
		} 
		
		return bandwidth;
	}
	
	
	//*************************************************************************
	// Name    : computeWeights
	// 
	// Purpose : computes an array of weights for object objectIndex in 
	//           class classIndex
	// 
	// Notes   : throws a DiscriminantAnalysisException Object if the 
	//           crossValidationMethod, distanceMatrix or classIndices
	//           attributes are not set. Returns a zero length array
	//           of doubles if an exception occurs
	// 
	//*************************************************************************
	protected double[] computeWeights(int objectIndex, int classIndex, double bandwidth, boolean excludeObject) throws DiscriminantAnalysisException {
		
		validateKernelFunctionType();
		validateDistanceMatrix();
		validateClassIndices();
		
		// array of weights to be returned
		double[] weights = null;
					
		try {
										
			// get the distance from objectIndex to other objects in the class classIndex
			int[] objectIndexArr = {objectIndex};
			
			
			
			// Excludes the current object from the calculation of means and covariance matrices
			// by setting it's distance to itself to be infinite temporarily
			if ( excludeObject == true ) {
				distanceMatrix.setEntry(objectIndex,objectIndex,Double.POSITIVE_INFINITY);
			}
			
		 	double[] distances = distanceMatrix.getSubMatrix(objectIndexArr, classIndices.get(classIndex)).getRow(0);
		 	
		 	if ( excludeObject == true ) {
				distanceMatrix.setEntry(objectIndex,objectIndex,0.0);
			}
		 	
			weights = new double[distances.length];
			
			if ( kernelFunctionType == NCGStatUtils.BISQUARE_KERNEL ) {
							
				// compute the weights array using a bisquare kernel approach
				weights = NCGStatUtils.bisquareKernel(distances, bandwidth);
				
			} else if ( kernelFunctionType == NCGStatUtils.MOVING_WINDOW ) {
				
				// compute the weights matrix using a moving window approach
				weights = NCGStatUtils.movingWindow(distances, bandwidth);
				
			}
					
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			weights = new double[0];
		} 
		
		return weights;
	}
		
	//*************************************************************************
	// Name    : computeNumNeighboursStepsCV
	// 
	// Purpose : assigns memory for and populates numNeighboursStepsCV attribute
	// 
	// Notes   : numNeighboursStepsCV is an array containing the number of 
	//           nearest neighbours required for each step of the cross validation
	//           It is created and populated using minNumNearestNeighboursCV and
	//           maxNumNearestNeighboursCV as a range with a step size of 
	//           numNearestNeighboursStepSizeCV
	//           throws a DiscriminantAnalysisException if an error occurs
	// 
	//*************************************************************************
	protected void computeNumNeighboursStepsCV() throws DiscriminantAnalysisException {
		
		validateMinNumNearestNeighboursCV();
		validateMaxNumNearestNeighboursCV();
		validateNumNearestNeighboursStepSizeCV();
		
		// create the array with the steps for each cross validation stage
		int numNNRange = (maxNumNearestNeighboursCV - minNumNearestNeighboursCV);
		
		// make sure that the range is greater than or equal to zero
		if (numNNRange < 0) {
			throw new DiscriminantAnalysisException("maximum number of nearest neighbours is less than minimum number of neareste neighbours for cross validation");
		}
		
		// compute the number of steps required - at minimum, one step is required when 
		// maxNumNearestNeighboursCV is equal to minNumNearestNeighboursCV
		int numSteps = 1;
		if (numNearestNeighboursStepSizeCV > 0) {
			numSteps += (int)Math.ceil(numNNRange / numNearestNeighboursStepSizeCV);
		}
		
		// allocate memory for the numNeighboursStepsCV array
		numNeighboursStepsCV = new int[numSteps];
		
		// now populate the array
		numNeighboursStepsCV[0] = minNumNearestNeighboursCV;
		for (int i = 1; i < numSteps; i++) {
			numNeighboursStepsCV[i] = numNeighboursStepsCV[i-1] + numNearestNeighboursStepSizeCV;
			
			if (numNeighboursStepsCV[i] > maxNumNearestNeighboursCV) {
				numNeighboursStepsCV[i] = maxNumNearestNeighboursCV;
			}
		}
	}

	//*************************************************************************
	// Name    : createOutputArrays
	// 
	// Purpose : assign memory for the output arrays / matrices 
	//           (mahalanobisDistance2, posteriorProbabilities, parameters
	//           and classified)
	// 
	// Notes   : overridden super class method
	//           throws any exceptions encountered during the assignment
	// 
	//*************************************************************************
	@Override
	protected void createOutputArrays() throws Exception {
		
		validateNumClasses();
		validateNumObjects();
		
		// assign memory for mahalanobis distance squared
		mahalanobisDistance2 = 	MatrixUtils.createRealMatrix(numObjects,numClasses);
		
		// assign memory for posterior probabilities for each class
		posteriorProbabilities = MatrixUtils.createRealMatrix(numObjects,numClasses);
		
		// assign memory for the classification parameters
		parameters = MatrixUtils.createRealMatrix(numObjects,(numClasses*(numFields+1))); 
		
		// assign memory for classified array
		classified = new int[numObjects];
		
		// create bandwidths array
		bandwidths = new double[numObjects];
	}
	
	//*************************************************************************
	// Name    : createCrossValidationOutputArrays
	// 
	// Purpose : assign memory for the cross validation output arrays / matrices 
	//           (crossValidationScores and crossValidationLikelihoods)
	// 
	// Notes   : throws any exceptions encountered during the assignment
	// 
	//*************************************************************************
	protected void createCrossValidationOutputArrays(int size) throws Exception {
		
		crossValidationScores = new double[size];
		crossValidationLikelihoods = new double[size];
	}
	
	
	//*************************************************************************
	// Name    : crossValidate
	// 
	// Purpose : performs cross validation to select optimum number of nearest
	//           neighbours
	// 
	// Notes   : throws a DiscriminantAnalysisException object if the 
	//           cross validation method is not selected
	// 
	//*************************************************************************
	public void crossValidate() throws DiscriminantAnalysisException {
		
		validateNumClasses();
		validateUniqueClasses();
		validateDistanceMatrix();
		validateClassification();
		validateNumObjects();
		validateCrossValidationMethod();
		
		try {
			
			// compute the steps required for the cross validation
			computeNumNeighboursStepsCV();
			
			// number of steps in cross validation
			int numSteps = numNeighboursStepsCV.length;

			// allocate memory for the output arrays for the cross validation results
			createCrossValidationOutputArrays(numSteps);
																		
			// map class labels to class indices
			Map classLabelsToIndicesMap = new HashMap();
			for (int k=0; k < numClasses; k++) {
				classLabelsToIndicesMap.put(Integer.valueOf(uniqueClasses[k]), 
										Integer.valueOf(k));
			}
						
			// classify the dataset for each of these neighbour values
			// and compute the optimum number of nearest neighbours
			for (int i = 0; i < numSteps; i++) {
				
				// set the sum of the logs of the posterior probabilities
				// to zero for the current number of nearest neighbours
				crossValidationLikelihoods[i] = 0.0;
				
				// classify the data set for the current nearest neighbour
				numNearestNeighbours = numNeighboursStepsCV[i];
								
				// allocate memory
				createOutputArrays();
				
				// now classify each observation without using it in the classification 
				for ( int j = 0; j < numObjects; j++ ) {
															
					// classify the current object without using it in the computation
					classifyObject(j,true);
					
					// get the index corresponding to the class label for the current object
					Integer classLabel = Integer.valueOf(classification[j]);
					int classIndex = ((Integer)classLabelsToIndicesMap.get(classLabel)).intValue();
					
					crossValidationLikelihoods[i] += Math.log(posteriorProbabilities.getEntry(j, classIndex));
								
				}
				
				// compute classification accuracy
				// (requires computing confusion matrix)
				confusionMatrix();
				
				// save the classification accuracy for the current bandwidth
				crossValidationScores[i] = classificationAccuracy;
									
			}
			
			if ( crossValidationMethod == NCGStatUtils.CROSS_VALIDATION_SCORE ) {
				
				// maximize cross validation score
				int maxClassAccurracyIndex = NCGStatUtils.getMax(crossValidationScores);
				numNearestNeighbours = numNeighboursStepsCV[maxClassAccurracyIndex];
				
				//System.out.println("cross validation score is maximized for " + numNearestNeighbours);
				
			} else if (crossValidationMethod == NCGStatUtils.CROSS_VALIDATION_LIKELIHOOD) {
				// maximize cross validation likelihood
				int maxSumPostProbIndex = NCGStatUtils.getMax(crossValidationLikelihoods);
				numNearestNeighbours = numNeighboursStepsCV[maxSumPostProbIndex];
				//System.out.println("cross validation likelihood is maximized for " + numNearestNeighbours);				
			} 	
			
		} catch (Exception e) {
			
			logger.severe("Exception encountered during cross validation - quitting classification");
			logger.severe(e.toString() + " : " + e.getMessage());			
			e.printStackTrace();
			
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix();
			posteriorProbabilities = new Array2DRowRealMatrix();
			parameters = new Array2DRowRealMatrix();
						
			throw new DiscriminantAnalysisException(e.getMessage(),e.getCause()); 
		}
		//classFrequencies
		//int[] nearestNeighbourValues = ;
		// go through each object in the data set and classify it for a 
		// range of nearest neighbour values
		//numNearestNeighbours = 99;
		
	}
		
	//*************************************************************************
	// Name    : classify
	// 
	// Purpose : classify the predictor (independent) variables using gwda
	// 
	// Notes   : throws a DiscriminantAnalysisException Object if not set
	//           sets classifed array, parameters matrix, posteriorProbabilites matrix and 
	//           mahalanobisDistance2 matrix to empty zero length arrays
	//           in case of an error.
	// 
	//*************************************************************************
	@Override
	public void classify() throws DiscriminantAnalysisException {
		
		// select the optimum number of nearest neighbours using cross validation if required
		if (useCrossValidation == true) {
			crossValidate();
		}
				
		validateNumObjects();
		
		int i = 0;
			
		try {
						
			 // assign memory for output array
			createOutputArrays();
			
			// classify each object
			for ( i = 0; i < numObjects; i++ ) {					
				classifyObject(i,false);
			}
			
		} catch (Exception e) {
						
			logger.severe("Exception encountered classifying observation [" + i + "] - quitting classification");
			logger.severe(e.toString() + " : " + e.getMessage());
			
			e.printStackTrace();
			
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix();
			posteriorProbabilities = new Array2DRowRealMatrix();
			parameters = new Array2DRowRealMatrix();
						
			throw new DiscriminantAnalysisException(e.getMessage(),e.getCause()); 
		} 
	}
	
	//*************************************************************************
	// Name    : classifyObject
	// 
	// Purpose : classify a specific observation with index objectIndex using
	//           GWDA
	//           
	//           if excludeObject is set to true then the current object is 
	//           excluded from the calculation of the class means and covariance
	//           matrices
	// 
	// Notes   : throws any Exceptions that occur
	// 
	//*************************************************************************
	public void classifyObject(int objectIndex, boolean excludeObject) throws Exception {
		
		// make sure that we have the necessary input variables
		validatePredictorVariables();
		validateClassIndices();
		validateNumFields();
		validateNumClasses();
		validateClassFrequencies();
		validateFieldIndices();
		
		// make sure that the output arrays are set
		validateClassified();
		validatePosteriorProbabilities();
		validateMahalanobisDistance2();
		validateParameters();
		
								
		// compute the bandwidth that encloses at least numNearestNeighbours
		// neighbours of the ith object in every class
		bandwidths[objectIndex] = computeBandwidth(objectIndex,excludeObject);
								
		// create a matrix to hold the pooled geographically weighted covariance
		// matrix for observation for ith object
		RealMatrix pooledGWCovarianceMatrix = new Array2DRowRealMatrix(numFields,numFields);
			
		// create a list of vectors to hold the geographically weighted means 
		// for observation i
		List<RealVector> gwMeans = new ArrayList<RealVector>();
									
		// for each class, identify all the objects around i that are enclosed within 
		// the bandwidth and compute geographically weighted mean vectors and covariance matrices
		
		int totalNumNeighbours = 0; // total number of neighbours across all classes used to classify objectIndex
		
		for( int c = 0; c < numClasses; c++ ) {
				
			// get indices of items in the current class
			int[] currentClassIndices = classIndices.get(c);
						
		 	// compute weights for all items in the current class based on their
			// proximity to i, the bandwidth and the kernel function type
		 	double[] weights = computeWeights(objectIndex, c, bandwidths[objectIndex],excludeObject);
		 	
		 	// get the predictor variables for items in the current class
			RealMatrix classPredictorVariables = 
					predictorVariables.getSubMatrix(currentClassIndices, fieldIndices);
		 	
		 	// get the class predictor variables in form where
		 	// the rows are the fields and the columns are the observations
		 	double[][] classData = classPredictorVariables.transpose().getData();
		 	
		 	// compute geographically weighted means 
		 	gwMeans.add(NCGStatUtils.computeWeightedMean(classData,weights));
		 			 			 			 	
		 	// compute geographically weighted covariance matrices
		 	RealMatrix gwCovarianceMatrix = NCGStatUtils.computeWeightedCovarianceMatrix(
		 										classData, gwMeans.get(c).getData(), weights);
		 	
		 	// number of neighbours in the current class used to compute the means and covariance matrices
		 	// (number of non-zero weights)
		 	int classNumNeighbours = 0;
		 	for(int i=0;i<weights.length;i++) {
		 		if (weights[i] > 0) {
		 			classNumNeighbours++;
		 		}
		 	}
		 	totalNumNeighbours += classNumNeighbours;
		 			 	
		 	// compute pooled geographically weighted covariance matrix iteratively
		 	pooledGWCovarianceMatrix = pooledGWCovarianceMatrix.add(gwCovarianceMatrix.scalarMultiply(classNumNeighbours));
		 	
		}
							
		// fully compute pooled class covariance matrix
		pooledGWCovarianceMatrix = pooledGWCovarianceMatrix.scalarMultiply(1.0 / totalNumNeighbours);
						
		// compute inverse of pooled class covariance matrix (if it exists)
		DecompositionSolver solver = (new LUDecompositionImpl(pooledGWCovarianceMatrix)).getSolver();
						
		// make sure that the pooled geographically weighted covariance matrix is non-singular
		if ( solver.isNonSingular() ) {
			
			// compute the inverse of the pooled covariance matrix
			RealMatrix pooledCovMatrixInv = solver.getInverse();
			
			// compute the mahalanobis distance squared and the LDA parameters
			RealVector ldaParameters = new ArrayRealVector();
			for (int c = 0; c < numClasses; c++) {
				
				// compute the mahalanobis distance squared from observation i to each class mean
				double mh2 = NCGStatUtils.computeMahalanobisDistance2(
										predictorVariables.getRowVector(objectIndex), 
										pooledCovMatrixInv, gwMeans.get(c));
				mahalanobisDistance2.setEntry(objectIndex,c, mh2);
				
				// compute the parameters (classification function coefficients)			
				RealVector classParameters = computeLDAParameters(pooledCovMatrixInv, gwMeans.get(c),c);
				ldaParameters = ldaParameters.append(classParameters);
				
			}
									
			// save the parameters for the current observation
			parameters.setRowVector(objectIndex, ldaParameters);
			
			// compute the posterior probabilities
			RealVector postProbs = computePosteriorProbabilities(mahalanobisDistance2.getRowVector(objectIndex));			
			posteriorProbabilities.setRowVector(objectIndex, postProbs);
			
			// classify the current observation
			classified[objectIndex] = assignObservationToClass(mahalanobisDistance2.getRowVector(objectIndex));
			
													
		} else {
			String message = "Singular pooled geographically weighted covariance matrix";
			throw new DiscriminantAnalysisException(message);
		}					

	}
	
}
