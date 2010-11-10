package ncg.statistics;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.InvalidMatrixException;
import org.apache.commons.math.linear.LUDecomposition;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.MatrixIndexException;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Variance;
import org.apache.commons.math.stat.descriptive.summary.Sum;

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
	
	// cross validation method to use for gwda
	// cross validation score maximizes the classification accuracy 
	// (proportion of correct classifications)
	// cross validation likelihood maximizes the sum of the logs of the 
	// posterior probabilities for the correct classes
	public static final int CROSS_VALIDATION_LIKELIHOOD = 0;
	public static final int CROSS_VALIDATION_SCORE = 1;

	
	// type of kernel function to use for gwda weights
	public static final int BISQUARE_KERNEL = 0;
	public static final int MOVING_WINDOW = 1;
	
	
	
	/*
	 * Input variables
	 */
	
	// matrix to hold distances from each object to every other object
	protected RealMatrix distanceMatrix = null;
	
	// number of nearest neighbours to use for gwda
	protected int numNearestNeighbours = DEFAULT_NUM_NEAREST_NEIGHBOURS;
	
	// set to true if number of nearest neighbours is to be chosen using cross validation
	protected boolean useCrossValidation = false;
	
	// holds type of cross validation method to use for gwda
	protected int crossValidationMethod = CROSS_VALIDATION_LIKELIHOOD;
	
	// kernel function to use for gwda
	protected int kernelFunctionType = BISQUARE_KERNEL;
	
	/*
	 * internal variables
	 */
	
	// classIndices is a list of integer arrays which contain the indices of 
	// items in each class. This is set by the setClassIndices method.
	protected List<int[]> classIndices = null;
	
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
		crossValidationMethod = CROSS_VALIDATION_LIKELIHOOD;
		kernelFunctionType = BISQUARE_KERNEL;
		
		// call super class reset
		super.reset();
	}
	
	/*
	 * various methods to check whether certain class attributes required to classify and output
	 * by the classification process have been set
	 * if they are not set then a DiscriminantAnalysisException object is thrown
	 */
	
	//*************************************************************************
	// Name    : validateClassIndices
	// 
	// Purpose : check to see if the classIndices attribute has been set
	// 
	// Notes   : classIndices is a list of integer arrays whose length
	//           is equal to the number of classes. Each integer array 
	//           contains the indices of objects in the respecive class
	//           this allows indexing into the distance matrix and predictor
	//           variables matrix
	// 
	//*************************************************************************
	protected void validateClassIndices() throws DiscriminantAnalysisException {		
		if ( classIndices == null ) {
			throw new DiscriminantAnalysisException("class indices list not set");
		}
	}
	
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
		
			case CROSS_VALIDATION_SCORE : 
				break;
			case CROSS_VALIDATION_LIKELIHOOD : 
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
		
			case MOVING_WINDOW :  
				break;
			case BISQUARE_KERNEL : 
				break;
			default : 
				throw new DiscriminantAnalysisException("invalid kernel function type");
		}
	}
	
	/*
	 * methods to set class variables
	 */
	
	//*************************************************************************
	// Name    : setClassIndices
	// 
	// Purpose : sets the classIndices list. 
	// 
	// Notes   : This is a list of int arrays. The
	//           size of the classIndices list is equal to the number of classes.
	//           the int array contains the indices of items in that class. 
	//           these indices are required to index into the distance matrix
	//           and the predictor variables matrix
	//
	//*************************************************************************
	protected void setClassIndices() throws DiscriminantAnalysisException {
		
		validateUniqueClasses();
		
		// compute indices of items in each class and store in a list of int[] arrays
		// this way we dont' have to comput them each time
		classIndices = new ArrayList<int[]>();
		for ( int j = 0; j < uniqueClasses.length; j++ ) {
			classIndices.add(computeClassIndices(j));		
		}
	}
	
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
	//           score or cross validation liklihood)
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
	//           weights for the geographially weighted discriminanat analysis
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
	// Notes   : throws a DiscriminantAnalysisException Object if uniqueClasses
	//           or numNearestNeighbours is not set. Returns -1 if an exception
	//           occurs
	// 
	//*************************************************************************
	protected double computeBandwidth( int objectIndex) throws DiscriminantAnalysisException {
		
		validateUniqueClasses();
		validateNumNearestNeighbours();
		
		// compute the bandwidth required to enclose at least numNearestNeighbours of
		// object with index objectIndex in each class
		double bandwidth = -1.0;
		
		try {
						
			int[] objectIndexArr = {objectIndex};
		
			for ( int c = 0; c < uniqueClasses.length; c++ ) {
				
				
				// get the distances from objectIndex to all other objects in the same class
			 	double[] distances = distanceMatrix.getSubMatrix(objectIndexArr, classIndices.get(c)).getRow(0);
			 	
			 	// sort these distances in ascending order
			 	// note that the first distance will be 0 - the distance of objectIndex to itself
			 	int[] distanceIndicesSorted = NCGStatUtils.sort(distances, false);
			
			 	// get the bandwidth required to enclose numNearestNeighbours of object with index
			 	// objectIndex in the current class
				int bandwithObjectIndex = distanceIndicesSorted[numNearestNeighbours+1];
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
	//           crossValidationMethod attribute or the distanceMatrix 
	//           attributes are not set. Returns a zero length array
	//           of doubles if an exception occurs
	// 
	//*************************************************************************
	protected double[] computeWeights(int objectIndex, int classIndex, double bandwidth) throws DiscriminantAnalysisException {
		
		validateCrossValidationMethod();
		validateDistanceMatrix();
		validateClassIndices();
		validateUniqueClasses();
		
		// array of weights to be returned
		double[] weights = null;
		
		try {
				
			// get the distance from objectIndex to other objects in the class classIndex
			int[] objectIndexArr = {objectIndex};
		 	double[] distances = distanceMatrix.getSubMatrix(objectIndexArr, classIndices.get(classIndex)).getRow(0);
		 	
			weights = new double[distances.length];
			
			if ( crossValidationMethod == GWDiscriminantAnalysis.BISQUARE_KERNEL ) {
				
				// compute the weights array using a bisquare kernel approach
				for (int i = 0; i < distances.length; i++) {
					if (distances[i] < bandwidth) {
						weights[i] = Math.pow((1.0 - Math.pow((distances[i]/bandwidth), 2.0)),2.0);
					} else {
						weights[i] = 0.0;
					}
				}
			} else if ( crossValidationMethod == GWDiscriminantAnalysis.MOVING_WINDOW ) {
				
				// compute the weights matrix using a moving window approach
				for (int i = 0; i < distances.length; i++) {
					if (distances[i] < bandwidth) {
						weights[i] = 1.0;
					} else {
						weights[i] = 0.0;
					}
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
	// Name    : setClassification
	// 
	// Purpose : set the classification attribute. This represents the actual
	//           class to which each object belongs. Also computes the uniqueClasses 
	//           and classFrequencies which are based on the contents of the classification
	//           array
	// 
	// Notes   :  throws a DiscriminantAnalysisException object if there is an error
	//            setting either uniqueClasses or classFrequencies or setClassIndices
	//            this method overrides setClassification in the super class
	// 
	//*************************************************************************
	@Override
	public void setClassification(int[] classification) throws DiscriminantAnalysisException {
		
		// call the super class method
		super.setClassification(classification);
		
		// set the class indices
		setClassIndices();
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
		
		// check to see if predictorVariables, uniqueClasses, validateDistanceMatrix
		// and numNearestNeighbours have been set - cannot continue until these have
		// been set
		validatePredictorVariables();
		validateClassIndices();
		validateUniqueClasses();
		validateDistanceMatrix();
		validateNumNearestNeighbours();
		
		try {
						
			int numObjects = predictorVariables.getRowDimension();
			int numFields = predictorVariables.getColumnDimension();
			int numClasses = uniqueClasses.length;
									
			// compute indices of fields in the predictorVariables matrix
			int[] fieldIndices = computeFieldIndices();
			
			// assign memory for mahalanobis distance squared
			mahalanobisDistance2 = 	MatrixUtils.createRealMatrix(numObjects,numClasses);
			
			// assign memory for posterior probabilities for each class
			posteriorProbabilities = MatrixUtils.createRealMatrix(numObjects,numClasses);
			
			// assign memory for the classification parameters
			parameters = MatrixUtils.createRealMatrix(numObjects,numClasses*(numFields+1)); 
			
			// assign memory for classified array
			classified = new int[numObjects];
			
			/*
			 * classify each object separately
			 */
			for ( int i = 0; i < numObjects; i++ ) {
				
				// get the attributes for the current object
				//RealVector currentObject = predictorVariables.getRowVector(i);
								
				// compute the bandwidth that encloses numNearestNeighbours neighbours of the object i in all classes
				double bandwidth = computeBandwidth(i);									
				
				//System.out.println("Computed Bandwidth for item " + i + " is "+ bandwidth + "(" + numNearestNeighbours  + ")");
				
				
				// matrix to hold pooled geographically weighted covariance matrix for observation i
				RealMatrix pooledGWCovarianceMatrix = new Array2DRowRealMatrix(numFields,numFields);
				
				// create a list of vectors to hold the geographically weighted means for observation i
				List<RealVector> gwMeans = new ArrayList<RealVector>();
				
				// for each class, identify all the objects around i that are enclosed within the bandwidth
				// then, compute geographically weighted means and covariance matrices
				for( int c = 0; c < numClasses; c++ ) {
					
					// get indices of items in the current class
					int[] currentClassIndices = classIndices.get(c);
					
					// get current class size
					int currentClassSize = currentClassIndices.length;
															
				 	// compute weights for all items in the current class based on their
					// proximity to i and on the bandwidth
				 	double[] weights = computeWeights(i, c, bandwidth);
				 	
				 	// get the predictor variables for items in the current class
					RealMatrix classPredictorVariables = 
							predictorVariables.getSubMatrix(currentClassIndices, fieldIndices);
				 	
				 	// get the class predictor variables in form where
				 	// the rows are the fields and the columns are the observations
				 	double[][] classData = classPredictorVariables.transpose().getData();
				 	
				 	// compute geographically weighted means and save them
				 	gwMeans.add(NCGStatUtils.computeWeightedMean(classData,weights));
				 	//gwMeans.set(c, NCGStatUtils.computeWeightedMean(classData,weights));
				 	
				 	// compute geographically weighted covariance matrices
				 	RealMatrix gwCovarianceMatrix = NCGStatUtils.computeWeightedCovarianceMatrix(classData, gwMeans.get(c).getData(), weights);
				 	
				 	// compute pooled geographically weighted covariance matrix
				 	pooledGWCovarianceMatrix = pooledGWCovarianceMatrix.add(gwCovarianceMatrix.scalarMultiply(currentClassSize));
				 	
				}
				
				// compute pooled class covariance matrix
				pooledGWCovarianceMatrix = pooledGWCovarianceMatrix.scalarMultiply(1.0 /numObjects);
				
				// compute inverse of pooled class covariance matrix
				// (if it exists!)
				LUDecomposition inv = new LUDecompositionImpl(pooledGWCovarianceMatrix);	
				DecompositionSolver solver = inv.getSolver();
				
				
				// make sure that the pooled geographically weighted covariance matrix is
				// non-singular
				if ( solver.isNonSingular() ) {
					
					// compute the inverse of the pooled covariance matrix
					RealMatrix pooledCovMatrixInv = inv.getSolver().getInverse();
					
					// compute the mahalanobis distance squared from 
					// observation i to the mean of each class
					for (int c = 0; c < numClasses; c++) {
						
						// compute the mahalanobis distance squared from observation i to each class mean
						double mh2 = NCGStatUtils.computeMahalanobisDistance2(
												predictorVariables.getRowVector(i), 
												pooledCovMatrixInv, gwMeans.get(c));
						mahalanobisDistance2.setEntry(i,c, mh2);
												
					}
					
					// compute the posterior probabilities
					RealVector postProbs = computePosteriorProbabilities(mahalanobisDistance2.getRowVector(i));			
					posteriorProbabilities.setRowVector(i, postProbs);
					
					// classify the current observation
					classified[i] = classifyObservation(mahalanobisDistance2.getRowVector(i));
					
					// compute the parameters for the current observation
										
				} else {
					String message = "Singular pooled geographically weighted covariance matrix - unable to classify";
					logger.severe(message);
					throw new DiscriminantAnalysisException(message);
				}
				
				
					
			}
			
			// for the time being use ordinary lda on the data
			//super.classify();
			
		} catch (Exception e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix(0,0);
			posteriorProbabilities = new Array2DRowRealMatrix(0,0);
			parameters = new Array2DRowRealMatrix(0,0);
		} 
	}
}
