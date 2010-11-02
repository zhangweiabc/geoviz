package ncg.statistics;

import java.util.logging.Logger;

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.InvalidMatrixException;
import org.apache.commons.math.linear.MatrixIndexException;

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
	
	// number of nearest neighbours to use for gwda
	protected int numNearestNeighbours = DEFAULT_NUM_NEAREST_NEIGHBOURS;
	
	// set to true if number of nearest neighbours is to be chosen using cross validation
	protected boolean useCrossValidation = false;
	
	// holds type of cross validation method to use for gwda
	protected int crossValidationMethod = CROSS_VALIDATION_LIKELIHOOD;
	
	// kernel function to use for gwda
	protected int kernelFunctionType = BISQUARE_KERNEL;
	
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
	// Name    : validateNumNearestNeighbours
	// 
	// Purpose : check to see if the numNearestNeighbours attribute has been set
	// 
	// Notes   : numNearestNeighours is an integer containing the number of
	//           nearest neighbours to use for the classification
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateNumNearestNeighbours() throws DiscriminantAnalysisException {		
		if ( numNearestNeighbours == DEFAULT_NUM_NEAREST_NEIGHBOURS ) {
			throw new DiscriminantAnalysisException("number of nearest neighbours not set");
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
	public void validateCrossValidationMethod() throws DiscriminantAnalysisException {		
		
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
	public void validateKernelFunctionType() throws DiscriminantAnalysisException {		
		
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
	public void classify() throws DiscriminantAnalysisException {
		
		// check to see if predictorVariables and uniqueClasses have been set
		// cannot continue until these have been set
		validatePredictorVariables();
		validateUniqueClasses();
		
		try {
			
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix(0,0);
			posteriorProbabilities = new Array2DRowRealMatrix(0,0);
			parameters = new Array2DRowRealMatrix(0,0);
			
		} catch(InvalidMatrixException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix(0,0);
			posteriorProbabilities = new Array2DRowRealMatrix(0,0);
			parameters = new Array2DRowRealMatrix(0,0);
			
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix(0,0);
			posteriorProbabilities = new Array2DRowRealMatrix(0,0);
			parameters = new Array2DRowRealMatrix(0,0);
			
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix(0,0);
			posteriorProbabilities = new Array2DRowRealMatrix(0,0);
			parameters = new Array2DRowRealMatrix(0,0);
			
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix(0,0);
			posteriorProbabilities = new Array2DRowRealMatrix(0,0);
			parameters = new Array2DRowRealMatrix(0,0);
		} 
	}
}
