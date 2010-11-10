package ncg.statistics;

/* 
* Implementation of Linear Discriminant Analysis
*
* Author : Peter Foley, 19.07.2010
*
* Method taken from 'Geographically Weighted Discriminant Analysis' by Chris Brundson,
* Stewart Fotheringham and Martin Charlton, Geographical Analysis, Volum 39, Issue 4 pp376-96 2007
* 
* Note that the use of biased covariance matrices follows the instructions in the above paper
* 
*/

import java.util.Arrays;

import java.util.logging.Logger;

import org.apache.commons.math.stat.correlation.Covariance;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.LUDecomposition;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.MatrixIndexException;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.stat.StatUtils;

public class DiscriminantAnalysis {
	
	/*
	 * input variables
	 */
	
	// RealMatrix (array) to hold predictor (independent) variables
	// rows contain observations and columns contain attributes
	protected transient RealMatrix predictorVariables = null;
		
	// classification array represents the actual classes that 
	// the observation belong to
	protected transient int[] classification = null;
	
	// RealVector (array) to hold prior probabilities
	// the length of this vector is equal to the total number of classes
	protected transient RealVector priorProbabilities = null;
	
	/*
	 * variables derived from the input variables
	 */
	
	// predictorVariablesRowOrder is set to true if the first dimension of the  input
	// predictor variables represent rows. Set to false if they represent columns
	// this is used by the getPredictorVariables method to return predictorVariables
	// in the same format that they were read in as.
	protected transient boolean predictorVariablesRowOrder = false;
	
    // array to hold unique class labels
	// the length of this vector is equal to the total number of classes
	// (derived from classification array)
	protected transient int[] uniqueClasses = null;

	// array to hold unique class frequencies
	// the length of this vector is equal to the total number of classes
	// (derived from classification array)
	protected transient int[] classFrequencies = null;
	
	/*
	 * output variables
	 */
	
	// classified is an integer array containing the classes assigned by the 
	// discriminant analysis
	protected transient int[] classified = null;
	
	// the columns of posteriorProbabilities refer to classes and the rows observations
	protected transient RealMatrix posteriorProbabilities = null;
	protected transient RealMatrix parameters = null;
	
	// mahalanobisDistance2 contains the mahalanobis distance squared from each observation to the
	// mean of all the other classes
	// the columns of mahalanobisDistance2 refer to classes and the rows observations
	protected transient RealMatrix mahalanobisDistance2 = null;
	
	// classification accuracy (proportion of correct classifications)
	protected transient double classificationAccuracy = -1;
	
	//logger object
	protected final static Logger logger = 
		Logger.getLogger(DiscriminantAnalysis.class.getName());
	
	//*************************************************************************
	// Name    : DiscriminantAnalysis
	// 
	// Purpose : empty constructor (java bean requirement)
	// 
	// Notes   : 
	// 
	//*************************************************************************
	public DiscriminantAnalysis() {}
	
	//*************************************************************************
	// Name    : reset
	// 
	// Purpose : reset all input variables 
	// 
	// Notes   : also frees up memory by running the java garbage collector
	// 
	//*************************************************************************
	public void reset() {
		
		// input variables, arrays & objects
		predictorVariables = null;
		classification = null;
		priorProbabilities = null;
		
		// input dependent variables, arrays & objects
		uniqueClasses = null;
		classFrequencies = null;
		
		// output variables, arrays & objects
		classified = null;
		posteriorProbabilities = null;
		parameters = null;
		mahalanobisDistance2 = null;
		classificationAccuracy = -1;
		
		// run the java garbage collector
		Runtime.getRuntime().gc();
		
	}
	
	
	/*
	 * various methods to check whether certain class attributes required to classify and output
	 * by the classification process have been set
	 * if they are not set then a DiscriminantAnalysisException object is thrown
	 */
	
	
	//*************************************************************************
	// Name    : validateClassification
	// 
	// Purpose : check to see if the classification attribute has been set
	// 
	// Notes   : classification is an integer array holding the actual classes
	//           that the observations belong to
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateClassification() throws DiscriminantAnalysisException {		
		if ( classification == null ) {
			throw new DiscriminantAnalysisException("input classification variable not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateUniqueClasses
	// 
	// Purpose : check to see if the uniqueClasses attribute has been set
	//        
	// 
	// Notes   : uniqueClasses is an integer array holding the unique class labels
	//           it is derived from the classification array
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateUniqueClasses() throws DiscriminantAnalysisException {
			
		if ( uniqueClasses == null ) {
			throw new DiscriminantAnalysisException("unique classes not computed (set input classification variable)");
		}
	}
	
	//*************************************************************************
	// Name    : validateClassFrequencies
	// 
	// Purpose : check to see if the classFrequencies attribute has been set
	// 
	// Notes   : classFrequencies is an integer array holding the number of observations
	//           in each class
	//           it is derived from the classification array
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateClassFrequencies() throws DiscriminantAnalysisException {		
		if ( classFrequencies == null ) {
			throw new DiscriminantAnalysisException("class frequencies not computed (set input classification variable)");
		}
	}
	
	//*************************************************************************
	// Name    : validatePredictorVariables
	// 
	// Purpose : check to see if the predictorVariables attribute has been set
	// 
	// Notes   : the columns of predictorVariables contain the attributes
	//           the rows of predictorVariables contain the observations
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validatePredictorVariables() throws DiscriminantAnalysisException {	
		if ( predictorVariables == null ) {
			throw new DiscriminantAnalysisException("input predictor variables not set");
		}
	}
	
	//*************************************************************************
	// Name    : validatePriorProbabilities
	// 
	// Purpose : check to see if the priorProbabilities attribute has been set
	// 
	// Notes   : priorProbabilities is a RealVector holding the prior probabilities
	//           of class membership
	//           its length is equal to the number of unique classes
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validatePriorProbabilities() throws DiscriminantAnalysisException {
		if ( priorProbabilities== null ) {
			throw new DiscriminantAnalysisException("input prior probabilities not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateClassified
	// 
	// Purpose : check to see if the classified attribute has been set
	//       
	// Notes   : classified is an integer array containing the classes assigned to
	//           each observation by the discriminant analysis. it is set by the
	//           classify method
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateClassified() throws DiscriminantAnalysisException {
		if ( classified == null ) {
			throw new DiscriminantAnalysisException("output classification not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateClassificationAccuracy
	// 
	// Purpose : check to see if the classificationAccuracy has been set
	// 
	// Notes   : classificationAccuracy is a scalar containing the proportion
	//           of correctly assigned observations
	//           it is set by the confusionMatrix method
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateClassificationAccuracy() throws DiscriminantAnalysisException {
		
		if ( classificationAccuracy == -1 ) {
			throw new DiscriminantAnalysisException("output classification accuracy not set");
		}
	}
	
	//*************************************************************************
	// Name    : validatePosteriorProbabilities
	// 
	// Purpose : check to see if the posteriorProbabilities attribute has been set
	//
	// Notes   : posteriorProbablilities is a RealMatrix where the rows of refer to 
	//           observations and the columns contain the posterior probabilities
	//           for each class
	//           posteriorProbabilities is set by the classify method
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validatePosteriorProbabilities() throws DiscriminantAnalysisException {
		if ( posteriorProbabilities == null ) {
			throw new DiscriminantAnalysisException("output posterior probabilities not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateParameters
	// 
	// Purpose : check to see if the parameters attribute has been set
	// 
	// Notes   : parameters is a RealMatrix - the columns refer to the classes
	//           and the rows, the classification function coefficients
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateParameters() throws DiscriminantAnalysisException {
		if ( parameters == null ) {
			throw new DiscriminantAnalysisException("output parameters not set");
		}
	}
	
	//*************************************************************************
	// Name    : validateMahalanobisDistance2
	// 
	// Purpose : check to see if the mahalanobisDistance2 attribute has been set
	// 
	// Notes   : mahalanobisDistance2 is a RealMatrix containing the mahalanobis
	//           distance squared for each observation. The rows of mahalanobisDistance2
	//           refer to observations and the columns refer to the classes
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateMahalanobisDistance2() throws DiscriminantAnalysisException {
		if ( mahalanobisDistance2 == null ) {
			throw new DiscriminantAnalysisException("output mahalanobis distance squared not set");
		}
	}
	
	//*************************************************************************
	// Name    : setPredictorVariables
	// 
	// Purpose : set the predictor (independent) variables.
	// 
	// Notes   : rowOrder is set to true if the first dimension of predictorVariables
	//           contains the rows (observations). If rowOrder is set to false
	//           the first dimension of predictorVariables refers to columns (attributes)
	//           if standardize is set to true, also standardize predictorVariables
	//           create array of size zero in case of an error 
	// 
	//*************************************************************************
	public void setPredictorVariables(double[][] predictorVariables,
					boolean rowOrder, boolean standardize) {
		try {
			
			// may also need to standardize the input variables
			if (standardize == true) {
				predictorVariables = NCGStatUtils.standardize(predictorVariables, rowOrder);
			}
			
			if (rowOrder == true) {
				this.predictorVariables =  
					MatrixUtils.createRealMatrix(predictorVariables);
			} else {
				this.predictorVariables =  
					MatrixUtils.createRealMatrix(predictorVariables).transpose();
			}
			
			// save the rowOrder for use by getPredictorVariables
			// want to return the predictorVariables in the same format that
			// they were read in
			predictorVariablesRowOrder = rowOrder;
						
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.predictorVariables = new Array2DRowRealMatrix(0,0);
		} 	
	}
	 
	//*************************************************************************
	// Name    : getPredictorVariables
	// 
	// Purpose : returns a copy of the predictor (independent) variables
	// 
	// Notes   : throws a DiscriminantAnalysisException object if predictorVariables
	//           are not set
	// 
	//*************************************************************************
	public double[][] getPredictorVariables() throws DiscriminantAnalysisException {		
		validatePredictorVariables();
		
		double[][] predictorVariablesReordered = null;
		
		if (predictorVariablesRowOrder == true) {
			predictorVariablesReordered = predictorVariables.getData();
		} else {
			predictorVariablesReordered = predictorVariables.transpose().getData();
		}
		return	predictorVariablesReordered;	
	}

	//*************************************************************************
	// Name    : getNumAttributes
	// 
	// Purpose : returns the number of predictor (independent) variables
	// 
	// Notes   : throws a DiscriminantAnalysisException object if predictorVariables 
	//           are not set
	// 
	//*************************************************************************
	public int getNumAttributes() throws DiscriminantAnalysisException {
		validatePredictorVariables();
		return predictorVariables.getColumnDimension();
	}
	
	//*************************************************************************
	// Name    : getNumObservations
	// 
	// Purpose : returns the number of observations/objects to classify
	// 
	// Notes   : throws a DiscriminantAnalysisException object if predictorVariables 
	//           are not set
	// 
	//*************************************************************************
	public int getNumObservations() throws DiscriminantAnalysisException {
		validatePredictorVariables();
		return predictorVariables.getRowDimension();
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
	//            setting either uniqueClasses or classFrequencies
	// 
	//*************************************************************************
	public void setClassification(int[] classification) throws DiscriminantAnalysisException {
		
		this.classification = classification;
		
		// compute unique class labels
		setUniqueClasses();

		// compute class frequencies
		setClassFrequencies();
		
	}
	
	//*************************************************************************
	// Name    : getClassification
	// 
	// Purpose : returns a copy of the classification array (actual classes)
	// 
	// Notes   :  throws a DiscriminantAnalysisException object if the classification
	//            is not set
	// 
	//*************************************************************************
	public int[] getClassification() throws DiscriminantAnalysisException {
		validateClassification();
		return Arrays.copyOf(classification,classification.length);
	}

	//*************************************************************************
	// Name    : setPriorProbabilities
	// 
	// Purpose : sets the prior probabilities based on the contents of the 
	//           input array
	// 
	// Notes   : creates zero length vector in case of an error
	// 
	//*************************************************************************
	public void setPriorProbabilities(double[] priorProbabilities) {
		try{
			this.priorProbabilities = new ArrayRealVector(priorProbabilities);
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.priorProbabilities = new ArrayRealVector();
		} 
	}
	
	//*************************************************************************
	// Name    : setPriorProbabilities
	// 
	// Purpose : sets the prior probabilities to be equal (1 / number of classes)
	//
	// Notes   : throws a DiscriminantAnalysisException if uniqueClasses has not 
	//           set
	//           creates zero length vector in case of an error
	// 
	//*************************************************************************
	public void setPriorProbabilities() throws DiscriminantAnalysisException {
		
		validateUniqueClasses();
		int numClasses = uniqueClasses.length;
		
		try{
			this.priorProbabilities = new ArrayRealVector(numClasses, (1 / (double)numClasses));
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.priorProbabilities = new ArrayRealVector();
		}
	}

	//*************************************************************************
	// Name    : getPriorProbabilities
	// 
	// Purpose : returns a copy of the prior probabilities array
	// 
	// Notes   : throws a DiscriminantAnalysisException if the posteriorProbabilies
	//           are not set
	// 
	//*************************************************************************
	public double[] getPriorProbabilities() throws DiscriminantAnalysisException {
		validatePriorProbabilities();
		return priorProbabilities.getData();
	}
	
	/*
	 * get methods for output of classification : classified, posteriorProbabilities, parameters
	 * and mahalanobisDistance2
	 */
	
	//*************************************************************************
	// Name    : getPosteriorProbabilities
	// 
	// Purpose : return a copy of the posterior probabilities array
	// 
	// Notes   : throws a DiscriminantAnalysisException object if posterior
	//           probabilities are not set
	// 
	//*************************************************************************
	public double[][] getPosteriorProbabilities() throws DiscriminantAnalysisException {
		validatePosteriorProbabilities();
		return posteriorProbabilities.getData();
	}
	
	//*************************************************************************
	// Name    : getPosteriorProbabilities
	// 
	// Purpose : return a copy of the column 'classIndex' of posteriorProbabilities
	//           this corresponds to the posterior probabilities for 'classIndex'
	//  
	// Notes   : throws a DiscriminantAnalysisException object if posterior
	//           probabilities are not set or if 'classIndex' is out of range
	// 
	//*************************************************************************
	public double[] getPosteriorProbabilities(int classIndex) throws DiscriminantAnalysisException {
	
		validatePosteriorProbabilities();
		
		double[] col = null;
			
		try {
			col = posteriorProbabilities.getColumn(classIndex);
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			col = new double[0];
			
			if (e instanceof MatrixIndexException ) {
				throw new DiscriminantAnalysisException("Class Index " + classIndex  + 
						" for Posterior Probabilities is out of range");
			}
		} 
						
		return col;
	}
		
	
	//*************************************************************************
	// Name    : getClassified
	// 
	// Purpose : returns a copy of the classified array. This holds the assigned
	//           classification by the discriminant analysis algorithm
	// 
	// Notes   : throws a DiscriminantAnalysisException object if classified is
	//           not set
	// 
	//*************************************************************************
	public int[] getClassified() throws DiscriminantAnalysisException {
		validateClassified();
		return Arrays.copyOf(classified,classified.length);
	}

		
	//*************************************************************************
	// Name    : getParameters
	// 
	// Purpose : returns copy of parameters (coefficients) of classification functions
	//           rows of parameters refer to the coefficients, columns refer to
	//           classes
	// 
	// Notes   : throws a DiscriminantAnalysisException object if parameters 
	//           are not set
	// 
	//*************************************************************************
	public double[][] getParameters() throws DiscriminantAnalysisException {
		validateParameters();
		return parameters.getData();
	}
	
	//*************************************************************************
	// Name    : getMahalanobisDistance2
	// 
	// Purpose : return a copy of the mahalanobisDistanse2 array (Mahalanobis
	//           Distance Squared).
	// 
	// Notes   : throws a DiscriminantAnalysisException object if 
	//           mahalanobisDistance2 is not set
	// 
	//*************************************************************************
	public double[][] getMahalanobisDistance2() throws DiscriminantAnalysisException {		
		validateMahalanobisDistance2();
		return mahalanobisDistance2.getData();
	}
	
	//*************************************************************************
	// Name    : getMahalanobisDistance2
	// 
	// Purpose : get mahalanobis distance squared for class 'classIndex'
	// 
	// Notes   : throws a DiscriminantAnalysisException object if not set
	//           or if classIndex is out of range. returns a zero length
	//           array of doubles if an error occurs
	// 
	//*************************************************************************
	public double[] getMahalanobisDistance2(int classIndex) throws DiscriminantAnalysisException {
			
		validateMahalanobisDistance2();
		double[] col = null;
				
		try {
			col = mahalanobisDistance2.getColumn(classIndex);
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			col = new double[0];
			
			if (e instanceof MatrixIndexException ) {
				throw new DiscriminantAnalysisException("Class Index " + classIndex  + 
						" for Mahalanobis Distance Squared is out of range");
			}
		} 
						
		return col;
		
	}
	
	//*************************************************************************
	// Name    : getClassificationAccuracy
	// 
	// Purpose : returns the classification accuracy (proportion of correct
	//           classifications)
	// 
	// Notes   : throws a DiscriminantAnalysisException object if 
	//           classificationAccuracy is not set
	// 
	//*************************************************************************
	public double getClassificationAccuracy() throws DiscriminantAnalysisException {
		validateClassificationAccuracy();
		return classificationAccuracy;
	}
	
	//*************************************************************************
	// Name    : getRandomClassificationAccuracy
	// 
	// Purpose : return the random classification accuracy ( result of 
	//           classification based solely on class frequencies)
	// 
	// Notes   : throws a DiscriminantAnalysisException object if the classification
	//           or classFrequencies arrays are not set
	// 
	//*************************************************************************
	public double getRandomClassificationAccuracy() throws DiscriminantAnalysisException {
		
		validateClassification();
		validateClassFrequencies();
			
		double randomClassAccuracy = 0;
		
		for ( int i = 0; i < classFrequencies.length; i++) {
			randomClassAccuracy += Math.pow(( (double)classFrequencies[i] / (double)classification.length),2);
		}
		
		return randomClassAccuracy;
	}
	
	/*
	 * get and set methods for internal variables - uniqueClasses and classFrequencies	
	 */
	
	//*************************************************************************
	// Name    : setUniqueClasses
	// 
	// Purpose : compute the uniqueClasses (class labels) from the classification array
	// 
	// Notes   : throws a DiscriminantAnalysisException object if 
	//           the classification array is not set
	//           uniqueClasses set to zero length array if an error occurs
	// 
	//*************************************************************************
	private void setUniqueClasses() throws DiscriminantAnalysisException {
		
		// check to see if the classification attribute has been set
		// cannot execute this method until it has been set
		validateClassification();
		uniqueClasses = NCGStatUtils.getUniqueItems(classification);
		
	}
	
	//*************************************************************************
	// Name    : getUniqueClasses
	// 
	// Purpose : returns a  copy of the uniqueClasses array (unique class labels)
	// 
	// Notes   : throws a DiscriminantAnalysisException object if uniqueClasses
	//           are not set
	// 
	//*************************************************************************
	public int[] getUniqueClasses() throws DiscriminantAnalysisException {	
		validateUniqueClasses();
		return Arrays.copyOf(uniqueClasses,uniqueClasses.length);
	}
	
	//*************************************************************************
	// Name    : setClassFrequencies
	// 
	// Purpose : compute the classFrequencies array which is derived from the
	//           classification and uniqeclasses arrays
	// 
	// Notes   : throws a DiscriminantAnalysisException object if the classification
	//           and uniqueClasses arrays are not set. Sets the classFrequencies
	//           array to zero length array in case of an error.
	// 
	//*************************************************************************
	private void setClassFrequencies() throws DiscriminantAnalysisException {
		
		// check to see if the classification array has been set
		// cannot continue if this is not set
		validateClassification();		
		classFrequencies = NCGStatUtils.getFrequencies(classification);
		
	}

	//*************************************************************************
	// Name    : getClassFrequencies
	// 
	// Purpose : return a copy of the classFrequencies array.
	// 
	// Notes   : throws a DiscriminantAnalysisException object if classFrequencies
	//           is not set
	// 
	//*************************************************************************
	public int[] getClassFrequencies() throws DiscriminantAnalysisException {
		validateClassFrequencies();
		return Arrays.copyOf(classFrequencies,classFrequencies.length);
	}

	//*************************************************************************
	// Name    : computeClassIndices
	// 
	// Purpose : return integer array of row indices for the classIndex th class in the
	//           uniqueClasses array
	// 
	// Notes   : throws a DiscriminantAnalysisException object if classification/uniqueClasses or 
	//           classFrequencies are not set
	//           returns a zero length array of ints in case of an error
	// 
	//*************************************************************************
	protected int[] computeClassIndices(int classIndex) throws DiscriminantAnalysisException {
		
		// check to see if the classification, uniqueClasses & classFrequencies attributes have been set
		// cannot continue until they are set
		validateClassification();
		validateUniqueClasses();
		validateClassFrequencies();
		
		// array to hold indices for class with index 'classIndex'
		int[] classRowIndices = null;
				
		try {
			
			classRowIndices = new int[classFrequencies[classIndex]];
	
			// identify indices for class with index 'classIndex'
			int j = 0;
			for(int i = 0; i < classification.length; i++) {
				
				if ( classification[i] == uniqueClasses[classIndex] ) {
					classRowIndices[j++] = i;
				}
			}
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classRowIndices = new int[0];
		}
				
		return classRowIndices;
	}	
	
	//*************************************************************************
	// Name    : computeFieldIndices
	// 
	// Purpose : return integer array of column (field) indices
	// 
	// Notes   : throws a DiscriminantAnalysisException object if predictor 
	//           variables are not set
	//           returns zero length array in case of an error
	// 
	//*************************************************************************
	protected int[] computeFieldIndices() throws DiscriminantAnalysisException {
		
		// check to see if predictorVariables have been set
		// cannot continue unti this has been set
		validatePredictorVariables();
		
		int[] colIndices = null;
		
		try {
			
			colIndices = new int[predictorVariables.getColumnDimension()];
			
			for(int j = 0; j < colIndices.length; j++) {
				colIndices[j] = j; 
			}
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			colIndices = new int[0];
		} 
		
		return colIndices;
	}
	
	//*************************************************************************
	// Name    : getClassMean
	// 
	// Purpose : compute mean vector for class with index classIndex in the
	//           uniqueClasse array.
	// 
	// Notes   : throws a DiscriminantAnalysisException object if not set. 
	//           returns zero length vector in case of an error
	// 
	//*************************************************************************
	protected RealVector getClassMean(int classIndex) throws DiscriminantAnalysisException {
		
		// check to see if predictorVariables have been set
		// cannot continue unti this has been set
		validatePredictorVariables();
		
		// vector reference to hold class means
		RealVector classMeansVector = null;
		
		// compute field indices
		int[] fieldIndices = computeFieldIndices();

		// compute row indices for class 'classIndex'
		int[] classRowIndices = computeClassIndices(classIndex);
		
		try {
		
			// compute sub matrix for class with index 'classIndex'
			RealMatrix predC = 
				predictorVariables.getSubMatrix(classRowIndices,fieldIndices);
	
			// assign memory for group means
			double[] groupMeans = new double[predC.getColumnDimension()];
				
			for ( int j = 0; j < groupMeans.length; j++ ) {	
					groupMeans[j] = StatUtils.mean(predC.getColumn(j));
			}
			
			classMeansVector = new ArrayRealVector(groupMeans);
				
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classMeansVector = new ArrayRealVector();
		}
		
		return classMeansVector;
			
	}

	//*************************************************************************
	// Name    : getCovarianceMatrix
	// 
	// Purpose : compute covariance matrix for the class with index classIndex
	//           in the array unique classes. 
	// 
	// Notes   : throws a DiscriminantAnalysisException object if the predictor variables
	//           are not set or if there are less than two observations/objects.
	//           Return zero length matrix in case of an error
	// 
	//*************************************************************************
	private RealMatrix getCovarianceMatrix(int classIndex) throws DiscriminantAnalysisException {
		
		// check to see if predictorVariables have been set
		// cannot continue unti this has been set
		validatePredictorVariables();
				
		// compute field indices
		int[] fieldIndices = computeFieldIndices();
		
		// check to make sure that there are least two fields
		//if ( fieldIndices.length < 2 ) {
		//	throw new DiscriminantAnalysisException("cannot compute covariance matrix for less than two fields");
		//}

		// compute row indices for class 'classIndex'
		int[] classRowIndices = computeClassIndices(classIndex);
		
		// check to make sure that there are least two observations
		if ( classRowIndices.length < 2 ) {
			throw new DiscriminantAnalysisException("cannot compute covariance matrix for less than two observations");
		}
		
		// reference to covariance matrix
		RealMatrix covMatrix = null;
		
		try {
				
			// compute sub matrix for class 'classIndex'
			RealMatrix predC = 
				predictorVariables.getSubMatrix(classRowIndices,fieldIndices);
			
			if ( fieldIndices.length > 1) {
				// compute covariance matrix for class 'classIndex' if we have more than 1 attribute
				Covariance c = new Covariance(predC);
				covMatrix = c.getCovarianceMatrix();
			} else {
				// if we only have one attribute calculate the variance instead
				covMatrix = MatrixUtils.createRealMatrix(1,1);
				covMatrix.setEntry(0, 0, StatUtils.variance(predC.getColumn(0)));
			}	
					
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			covMatrix = new Array2DRowRealMatrix(0,0);
		}
						
		return covMatrix;
			
	}

	//*************************************************************************
	// Name    : getPooledCovMatrix
	// 
	// Purpose : compute the pooled class covariance matrix. Return null if an 
	//           error occurs
	// 
	// Notes   : throws a DiscriminantAnalysisException object if predictor 
	//           variables/unique classes/class frequencies are not set
	//           returns zero size matrix in case of an error
	// 
	//*************************************************************************
	private RealMatrix getPooledCovMatrix() throws DiscriminantAnalysisException {
		
		// check to see if predictorVariables, uniqueClasses & classFrequencies have been set
		// cannot continue until these have been set
		validatePredictorVariables();
		validateUniqueClasses();
		validateClassFrequencies();
		
		// reference to pooled class covariance matrix
		RealMatrix pooledCovMatrix = null;
		
		try {
				
			// get number of fields
			int numFields = predictorVariables.getColumnDimension();

			// assign memory for pooled sum of squares matrix
			// java arrays values are automatically set to zero
			RealMatrix pooledSumSquares = 
				MatrixUtils.createRealMatrix(numFields,numFields);
	
			// get indices for each class in turn and compute the 
			// total sum of squares (in order to compute the pooled
			// class covariance matrix
			for( int classIndex = 0; classIndex < uniqueClasses.length; classIndex++ ) {
			
				// compute covariance matrix for the class with index
				// classIndex in the array returned by getClasses()
				RealMatrix covMatrix = getCovarianceMatrix(classIndex);
				
				// compute the degrees of freedom for the class with index
				// classIndex in the array returned by getClasses()
				double degreesFreedom = (classFrequencies[classIndex] -1);
			
				// compute sum of squares for the class with index
				// classIndex in the array returned by getClasses()
				RealMatrix sumSquares = covMatrix.scalarMultiply(degreesFreedom);
					
				// add the sum of squares for the class with index
				// classIndex in the array returned by getClasses()
				// to the pooledSumSquares matrix
				pooledSumSquares = pooledSumSquares.add(sumSquares);
			
			}
						
			// compute the covariance matrix (maximum likelihood version)
			pooledCovMatrix = pooledSumSquares.scalarMultiply(
								1.0 / predictorVariables.getRowDimension());
				
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			pooledCovMatrix = new Array2DRowRealMatrix(0,0);
		} 
		
		return pooledCovMatrix;
	}
	

	//*************************************************************************
	// Name    : confusionMatrix
	// 
	// Purpose : compute the confusion matrix. 
	// 
	// Notes   : throws a DiscriminantAnalysisException Object if not set
	//           returns a 2d array of zeros in case of an error
	// 
	//*************************************************************************
	public int[][] confusionMatrix() throws DiscriminantAnalysisException {
		
		
		// check to see if classification, classified and uniqueClasses have been set
		// cannot continue until these have been set
		validateClassification();
		validateUniqueClasses();
		validateClassified();
		
		int[][] cMatrix = null;
		
		try{
			
			// create the confusion matrix and set the 
			// values are set to zero initially by default in java
			cMatrix = new int[uniqueClasses.length][uniqueClasses.length];
			
			// number of objects classified
			int numObjects = classified.length;
				
			// compute confusion matrix
			for ( int c = 0; c < numObjects; c++) {
				
				for ( int i = 0; i < uniqueClasses.length; i++) {
					for ( int j = 0; j < uniqueClasses.length; j++) {
						if ( classification[c] == uniqueClasses[i]) {		
							// object in row c belongs to uniqueClasses[i]
							if ( classified[c] == uniqueClasses[j]) {
								// object in row c assigned to uniqueClasses[j]
								cMatrix[i][j]++;
							}
						}
	
					}
				}
			}
			
			// compute classification accuracy
			double classAccuracy = 0.0;
			for ( int i = 0; i < uniqueClasses.length; i++) {		
				classAccuracy += cMatrix[i][i];
			}
			classificationAccuracy = classAccuracy / numObjects;
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			cMatrix = new int[0][0];
		} 

		return cMatrix;
	}
	
	//*************************************************************************
	// Name    : computePosteriorProbability
	// 
	// Purpose : computes the posterior probabilities for a vector of 
	//           mahalanobis distance squared values
	// 
	// Notes   : throws a DiscriminantAnalysisException Object if the prior
	//           probabilites are not set. 
	//           returns a zero length vector if an error occurs
	// 
	//*************************************************************************
	protected RealVector computePosteriorProbabilities(RealVector mhDistance2) throws DiscriminantAnalysisException {
		
		validatePriorProbabilities();
		
		RealVector postProbs = null;
		
		try {
			// compute the multivariate normal probability density values for the observations with
			// mahalanobis distance square values in mhDistance2.
			RealVector mvnD = mhDistance2.mapDivide(-2.0).mapExp().ebeMultiply(priorProbabilities);
									
			// since pdf values are in the range [0,1] computing the l1 norm (sum of absolute values) 
			// is equivalent to summing the vector item values
			// ( covariance matrix used to compute mahalanobis distance squared is positive semi-definite
			// so x'C' >= 0) and pdf is therefore constrained to lie in range[0,1])
			double sumMhd = mvnD.getL1Norm();
			
			postProbs = mvnD.mapDivide(sumMhd);	
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			postProbs = new ArrayRealVector();
		} 

		return postProbs;
	}
	
	//*************************************************************************
	// Name    : classifyObservation
	// 
	// Purpose : classifies an observation with mahalanobis distances mhDistance2
	//           returns the label of the assigned class as given in the 
	//           uniqueClasses array
	// 
	// Notes   : throws a DiscriminantAnalysisException Object if the prior
	//           probabilities are not set. 
	//           returns -1 if an error occurs
	// 
	//************************************************************************
	protected int classifyObservation(RealVector mhDistance2) throws DiscriminantAnalysisException {
		
		validatePriorProbabilities();
		validateUniqueClasses();
		
		int classLabel = -1;
		
		try {
			
			// compute the log of the prior probabilities
			RealVector logPriorProbabilites = priorProbabilities.mapLog();
			
			// subtract the log of the prior probabilities from the mahalanobis distance squared vector
			double[] mh2Classes = mhDistance2.mapDivide(2).subtract(logPriorProbabilites).getData();
			
			// identify the class index
			int classIndex = NCGStatUtils.getMin(mh2Classes);
			
			// identify the class label
			classLabel = uniqueClasses[classIndex];
			
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classLabel = -1;
		}
		
		return classLabel;
	}
		
	//*************************************************************************
	// Name    : classify
	// 
	// Purpose : classify the predictor (independent) variables
	// 
	// Notes   : throws a DiscriminantAnalysisException Object if not set
	//           sets classifed array, parameters matrix, posteriorProbabilites matrix and 
	//           mahalanobisDistance2 matrix to empty zero length arrays
	//           in case of an error.
	// 
	//*************************************************************************
	public void classify() throws DiscriminantAnalysisException {
		
		// check to see if predictorVariables, priorProbabilites and uniqueClasses have been set
		// cannot continue until these have been set
		validatePredictorVariables();
		validateUniqueClasses();
		validatePriorProbabilities();
				
		try {
			
			int numObjects = predictorVariables.getRowDimension();
			int numFields = predictorVariables.getColumnDimension();
			int numClasses = uniqueClasses.length;
			
			// compute the log of the prior probabilities
			RealVector logPriorProbabilities = priorProbabilities.mapLog();
			
			// create pooled group covariance matrix (maximum likelihood version)
			RealMatrix pooledCovMatrix = getPooledCovMatrix();

			// compute inverse of pooled group covariance matrix
			// (if it exists!)
			LUDecomposition inv = new LUDecompositionImpl(pooledCovMatrix);	
			DecompositionSolver solver = inv.getSolver();
			
			if ( solver.isNonSingular() ) {
				
				RealMatrix pooledCovMatrixInv = inv.getSolver().getInverse();

				
				// assign memory for mahalanobis distance squared
				mahalanobisDistance2 = 
					MatrixUtils.createRealMatrix(numObjects,numClasses);
			
				// assign memory for parameters (coefficients)
				parameters = MatrixUtils.createRealMatrix(numFields+1,numClasses);

				// compute mahalanobis distance squared for each class
				for( int c = 0; c < numClasses; c++ ) {
					
					// compute class means for the cth class
					RealVector classMeans = getClassMean(c);
	
					// compute mahalanobis distance squared separately for each object in the dataset
					for (int i = 0; i < numObjects; i++) {
		
						double mh2 = NCGStatUtils.computeMahalanobisDistance2(
															predictorVariables.getRowVector(i), 
															pooledCovMatrixInv, classMeans);
						mahalanobisDistance2.setEntry(i,c, mh2);
						
					}
				
					// compute parameters (coefficients) for the cth class
					RealMatrix classMeansMatrix = MatrixUtils.createColumnRealMatrix(classMeans.getData());
					RealMatrix pn = pooledCovMatrixInv.multiply(classMeansMatrix);
					parameters.setSubMatrix(pn.getData(),1,c);
					parameters.setEntry(0,c, ( (classMeans.dotProduct(pn.getColumnVector(0)) * (-0.5)) + logPriorProbabilities.getEntry(c)));
				
				}
				
				// assign memory for classified array
				classified = new int[numObjects];
				
				// assign memory for posterior probabilities for each class
				posteriorProbabilities = MatrixUtils.createRealMatrix(numObjects,numClasses);
			
				// classify each observation and compute posterior probabilities
				for (int i = 0; i < numObjects; i++) {
					
					// classify the current observation
					classified[i] = classifyObservation(mahalanobisDistance2.getRowVector(i));
					
					// compute the posterior probabilities for the current observation
					RealVector postProbs = computePosteriorProbabilities(mahalanobisDistance2.getRowVector(i));			
					posteriorProbabilities.setRowVector(i, postProbs);
					
					
				}
				
			} else {
				String message = "Singular pooled covariance matrix - unable to classify";
				logger.severe(message);
				throw new DiscriminantAnalysisException(message);
			}
					
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classified = new int[0];
			mahalanobisDistance2 = new Array2DRowRealMatrix(0,0);
			posteriorProbabilities = new Array2DRowRealMatrix(0,0);
			parameters = new Array2DRowRealMatrix(0,0);
		}  
	}
}
