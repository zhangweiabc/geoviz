package ncg.statistics;

import java.awt.geom.Point2D;
import java.util.ArrayList;
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
	protected static final int DEFAULT_NUM_NEAREST_NEIGHBOURS = -1;
		
	/*
	 * Input variables
	 */
	
	// matrix to hold distances from each object to every other object
	protected transient RealMatrix distanceMatrix = null;
	
	// number of nearest neighbours to use for gwda
	protected transient int numNearestNeighbours = DEFAULT_NUM_NEAREST_NEIGHBOURS;
	
	// set to true if number of nearest neighbours is to be chosen using cross validation
	protected transient boolean useCrossValidation = false;
	
	// holds type of cross validation method to use for gwda
	protected transient int crossValidationMethod = NCGStatUtils.CROSS_VALIDATION_LIKELIHOOD;
	
	// kernel function to use for gwda
	protected transient int kernelFunctionType = NCGStatUtils.BISQUARE_KERNEL;
	
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
	// 
	//*************************************************************************
	public void reset() {
		
		// reset geographically weighted specific variables
		distanceMatrix = null;
		classIndices = null;
		numNearestNeighbours = DEFAULT_NUM_NEAREST_NEIGHBOURS;
		useCrossValidation = false;
		crossValidationMethod = NCGStatUtils.CROSS_VALIDATION_LIKELIHOOD;
		kernelFunctionType = NCGStatUtils.BISQUARE_KERNEL;
		
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
	// Purpose : check to see if the distanceMatrix attribute has been set
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
	// Purpose : check to see if the numNearestNeighbours attribute has been set
	// 
	// Notes   : numNearestNeighours is an integer containing the number of
	//           nearest neighbours to use for the classification
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	protected void validateNumNearestNeighbours() throws DiscriminantAnalysisException {		
		if ( numNearestNeighbours < 1 ) {
			throw new DiscriminantAnalysisException("number of nearest neighbours not set to valid value");
		}
	}
	
	//*************************************************************************
	// Name    : validateCrossValidationMethod
	// 
	// Purpose : check to see if the crossValidationMethod is valid
	// 
	// Notes   : crossValidationMethod defines the method used to select
	//           the optimum number of nearest neighbours ( cross validation
	//           score or cross validation liklihood)
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
				throw new DiscriminantAnalysisException("invalid cross validation method");
		}
	}
	
	//*************************************************************************
	// Name    : validateKernelFunctionType
	// 
	// Purpose : check to see if the kernelFunctionType attribute is valid
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
				throw new DiscriminantAnalysisException("invalid kernel function type");
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
	protected double computeBandwidth( int objectIndex) throws DiscriminantAnalysisException {
		
		validateUniqueClasses();
		validateNumClasses();
		validateNumNearestNeighbours();
		validateDistanceMatrix();
		validateClassIndices();
		
		// compute the bandwidth required to enclose at least numNearestNeighbours of
		// object with index objectIndex in each class
		double bandwidth = -1.0;
		
		try {
						
			int[] objectIndexArr = {objectIndex};
		
			for ( int c = 0; c < numClasses; c++ ) {
					
				// get the distances from objectIndex to all other objects in the same class
			 	double[] distances = distanceMatrix.getSubMatrix(objectIndexArr, classIndices.get(c)).getRow(0);
			 	
			 	// sort these distances in ascending order
			 	// note that the first distance will be 0 - the distance of objectIndex to itself
			 	int[] distanceIndicesSorted = NCGStatUtils.sort(distances, false);
			
			 	// get the bandwidth required to enclose numNearestNeighbours of current object 
			 	// this means that the bandwidth will equal the distance corresponding to the 
			 	// (numNearestNeighbours +1) neighbour
			 	int neighIndex = numNearestNeighbours+1;
			 	if (neighIndex >= distanceIndicesSorted.length) {
			 		
			 		// first make sure that the number of nearest neighbours + 1 does not
				 	// exceed the number of items in this class
			 		logger.warning("class " + uniqueClasses[c] + " does not contain " + neighIndex + " neighbours" );
			 		neighIndex = (distanceIndicesSorted.length+1 -1);
			 	} 
			 	
			 	int bandwithObjectIndex = distanceIndicesSorted[neighIndex];
				double currentBandwidth = distances[bandwithObjectIndex];
			 					
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
	protected double[] computeWeights(int objectIndex, int classIndex, double bandwidth) throws DiscriminantAnalysisException {
		
		validateCrossValidationMethod();
		validateDistanceMatrix();
		validateClassIndices();
		
		// array of weights to be returned
		double[] weights = null;
		
		try {
				
			// get the distance from objectIndex to other objects in the class classIndex
			int[] objectIndexArr = {objectIndex};
		 	double[] distances = distanceMatrix.getSubMatrix(objectIndexArr, classIndices.get(classIndex)).getRow(0);
		 	
			weights = new double[distances.length];
			
			if ( crossValidationMethod == NCGStatUtils.BISQUARE_KERNEL ) {
				
				// compute the weights array using a bisquare kernel approach
				weights = NCGStatUtils.bisquareKernel(distances, bandwidth);
				
			} else if ( crossValidationMethod == NCGStatUtils.MOVING_WINDOW ) {
				
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
		
		if (useCrossValidation == true) {
			
			//validateCrossValidationMethod();
			//validateClassification();
			validateClassFrequencies();
			validatePredictorVariables();
			validateNumObjects();
			
			try {
				int maxClassSizeIndex = NCGStatUtils.getMax(classFrequencies);
				int maxClassSize = classFrequencies[maxClassSizeIndex];
				
				double[] classificationAccuracies = new double[maxClassSize];
				
				double[] sumLogPosteriorProbs = new double[maxClassSize];
				
				
				// map class labels to class indices
				Map classLabelsToIndicesMap = new HashMap();
				for (int k=0; k < numClasses; k++) {
					classLabelsToIndicesMap.put(Integer.valueOf(uniqueClasses[k]), 
											Integer.valueOf(k));
				}	
				// classify the dataset for each of these neighbour values
				// and compute the optimum number of nearest neighbours
				for (int i=1; i < maxClassSize; i++) {
					
					// set the sum of the logs of the posterior probabilites
					// to zero for the current observation
					sumLogPosteriorProbs[i] = 0.0;
					
					// classify the data set for the current nearest neighbour
					numNearestNeighbours = i;
					
					// allocate memory
					createOutputArrays();
					
					// now classify each observation 
					for ( int j = 0; j < numObjects; j++ ) {
						
						// we don't include the current object in the computation
						// of the covariance matrix by setting the distance to itself
						// to positive infinity. This falls outside all bandwidths
						distanceMatrix.setEntry(j,j,Double.POSITIVE_INFINITY);
						classifyObject(j);
						
						// set the distance for the current object to itself back to zero
						distanceMatrix.setEntry(j,j,0.0);
						
						// get the index corresponding to the class label for the current
						// object
						Integer classLabel = Integer.valueOf(classification[j]);
						int classIndex = ((Integer)classLabelsToIndicesMap.get(classLabel)).intValue();
						sumLogPosteriorProbs[i] += Math.log(posteriorProbabilities.getEntry(j, classIndex));
						
					}
					
					// compute classification accuracy
					// (requires computing confusion matrix)
					confusionMatrix();
					
					// save the classification accuracy for the current bandwidth
					classificationAccuracies[i] = classificationAccuracy;
						
				
				}
			} catch (Exception e) {
				logger.severe(e.toString() + " : " + e.getMessage());
				e.printStackTrace();
			}
			//classFrequencies
			//int[] nearestNeighbourValues = ;
			// go through each object in the data set and classify it for a 
			// range of nearest neighbour values
			//numNearestNeighbours = 99;
		}
		
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
		
		validateNumObjects();
		
		int i = 0;
			
		try {
						
			 // assign memory for output array
			createOutputArrays();
			
			// classify each object
			for ( i = 0; i < numObjects; i++ ) {					
				classifyObject(i);					
			}
					
		} catch (Exception e) {
						
			logger.severe("Exception encountered classifying Observation [" + i + "] - quitting classification");
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
	// Notes   : throws any Exceptions that occur
	// 
	//*************************************************************************
	public void classifyObject(int objectIndex) throws Exception {
		
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
		double bandwidth = computeBandwidth(objectIndex);									
							
		// create a matrix to hold the pooled geographically weighted covariance
		// matrix for observation for ith object
		RealMatrix pooledGWCovarianceMatrix = new Array2DRowRealMatrix(numFields,numFields);
			
		// create a list of vectors to hold the geographically weighted means 
		// for observation i
		List<RealVector> gwMeans = new ArrayList<RealVector>();
							
		// for each class, identify all the objects around i that are enclosed within 
		// the bandwidth and compute geographically weighted mean vectors and covariance matrices
		for( int c = 0; c < numClasses; c++ ) {
				
			// get indices of items in the current class
			int[] currentClassIndices = classIndices.get(c);
			
			// get current class size
			int currentClassSize = classFrequencies[c];
													
		 	// compute weights for all items in the current class based on their
			// proximity to i, the bandwidth and the kernel function type
		 	double[] weights = computeWeights(objectIndex, c, bandwidth);
		 	
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
		 	
		 	// compute pooled geographically weighted covariance matrix iteratively
		 	pooledGWCovarianceMatrix = pooledGWCovarianceMatrix.add(gwCovarianceMatrix.scalarMultiply(currentClassSize));
		 	
		}
			
		// fully compute pooled class covariance matrix
		pooledGWCovarianceMatrix = pooledGWCovarianceMatrix.scalarMultiply(1.0 /numObjects);
				
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
				RealVector classParameters = computeLDAParameters(pooledCovMatrixInv, gwMeans.get(c),
												c);
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
