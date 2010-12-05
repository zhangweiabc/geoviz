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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.logging.Logger;

import org.apache.commons.math.stat.correlation.Covariance;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
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
	
	// matrix to hold predictor (independent) variables
	// rows contain observations and columns contain attributes
	protected transient RealMatrix predictorVariables = null;
		
	// classification array represents the classes that each observation belongs to
	protected transient int[] classification = null;
	
	// vector to hold prior probabilities
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
	// it is derived from classification array
	protected transient int[] uniqueClasses = null;

	// array to hold unique class frequencies
	// the length of this vector is equal to the total number of classes
	// it is derived from classification array
	protected transient int[] classFrequencies = null;
	
	// array to hold log of prior probabilities
	protected transient RealVector logPriorProbabilities = null;
		
	// number of objects for classification (number of rows in predictorVariables matrix)
	protected transient int numObjects = -1;
	
	// number of fields used as independent variables in the classification
	// (number of rows in predictorVariables matrix)
	protected transient int numFields = -1;
	
	// number of distinct classes for classification
	// it is derived from the uniqueClasses array.
	protected transient int numClasses = -1;
	
	// field indices are the indices of fields in the predictor variables matrix
	protected transient int[] fieldIndices = null;
	
	// classIndices is a list of integer arrays which contain the indices of 
	// items in each class. This is set by the setClassIndices method.
	protected transient List<int[]> classIndices = null;
	
	/*
	 * output variables
	 */
	
	// classified is an int array containing the assigned classes
	protected transient int[] classified = null;
	
	// the columns of posteriorProbabilities refer to classes and the rows observations
	protected transient RealMatrix posteriorProbabilities = null;
	
	// the rows of parameters refer to classification function coefficients and the 
	// columns, the classes
	protected transient RealMatrix parameters = null;
	
	// mahalanobisDistance2 contains the mahalanobis distance squared from each 
	// observation to the mean of all the other classes
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
		logPriorProbabilities = null;
		numObjects = -1;
		numClasses = -1;
		numFields = -1;
		fieldIndices = null;
		classIndices = null;
		
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
	// Name    : validateNumClasses
	// 
	// Purpose : check to see if the numClasses attribute has been set
	//        
	// 
	// Notes   : numClasses holds the number of unique classes in the data set
	//           it is derived from the uniqueClasses array
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateNumClasses() throws DiscriminantAnalysisException {
			
		if ( numClasses == -1 ) {
			throw new DiscriminantAnalysisException("number of unique classes not computed (set input classification variable)");
		}
	}
	
	//*************************************************************************
	// Name    : validateClassIndices
	// 
	// Purpose : check to see if the classIndices attribute has been set
	// 
	// Notes   : classIndices is a list of integer arrays whose length
	//           is equal to the number of classes. Each integer array 
	//           contains the indices of objects in the respective class
	//           this allows indexing into the distance matrix and predictor
	//           variables matrix. It is derived from the classification array
	// 
	//*************************************************************************
	protected void validateClassIndices() throws DiscriminantAnalysisException {		
		if ( classIndices == null ) {
			throw new DiscriminantAnalysisException("class indices list not computed (set input classification variables)");
		}
	}
	
	//*************************************************************************
	// Name    : validateClassFrequencies
	// 
	// Purpose : check to see if the classFrequencies attribute has been set
	// 
	// Notes   : classFrequencies is an int array holding the number of observations
	//           in each class. it is derived from the classification array
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateClassFrequencies() throws DiscriminantAnalysisException {		
		if ( classFrequencies == null ) {
			throw new DiscriminantAnalysisException("class frequencies not computed (set input classification variable)");
		}
	}
	
	//*************************************************************************
	// Name    : validateNumObjects
	// 
	// Purpose : check to see if the numObjects attribute has been set
	// 
	// Notes   : numObjects is equal to the number of rows of the 
	//           predictorVariables matrix
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateNumObjects() throws DiscriminantAnalysisException {	
		if ( numObjects == -1 ) {
			throw new DiscriminantAnalysisException("number of objects in dataset not computed (input predictor variables not set)");
		}
	}
	
	//*************************************************************************
	// Name    : validateNumFields
	// 
	// Purpose : check to see if the numFields attribute has been set
	// 
	// Notes   : numFields is equal to the number of columns of the 
	//           predictorVariables matrix (the number of attributes)
	//           throws a new DiscriminantAnalysisException if it has not been  set
	// 
	//*************************************************************************
	public void validateNumFields() throws DiscriminantAnalysisException {	
		if ( numFields == -1 ) {
			throw new DiscriminantAnalysisException("number of fields in predictor variables not computed (input predictor variables not set)");
		}
	}
	
	//*************************************************************************
	// Name    : validateFieldIndices
	// 
	// Purpose : check to see if the fieldIndices array has been computed
	// 
	// Notes   : fieldIndices is an array containing the indices of fields in
	//           the predictorVariables matrix
	//           throws a new DiscriminantAnalysisException if it has not been set
	// 
	//*************************************************************************
	public void validateFieldIndices() throws DiscriminantAnalysisException {	
		if ( fieldIndices == null ) {
			throw new DiscriminantAnalysisException("field indices not computed (input predictor variables not set)");
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
		if ( priorProbabilities== null || logPriorProbabilities == null ) {
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
	// Purpose : set the predictor (independent) variables, numObjects (number of objects),
	//           numFields (the number of variables) and the fieldIndices array (holds
	//           indices of each column)
	// 
	// Notes   : rowOrder is set to true if the first dimension of predictorVariables
	//           contains the rows (observations). If rowOrder is set to false
	//           the first dimension of predictorVariables refers to columns (attributes)
	//           if standardize is set to true, also standardize predictorVariables
	//           create arrays of size zero and sets int fields to zero in case of an error 
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
			
			// compute the number of objects (rows)
			numObjects = this.predictorVariables.getRowDimension();
			
			// compute the number of variables (columns)
			numFields = this.predictorVariables.getColumnDimension();
			
			// compute the field indices array and populate it
			fieldIndices = new int[numFields];		
			for(int j = 0; j < numFields; j++) {
				fieldIndices[j] = j; 
			}
			
			// save the rowOrder for use by getPredictorVariables
			// want to return the predictorVariables in the same format that
			// they were read in
			predictorVariablesRowOrder = rowOrder;
						
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.predictorVariables = new Array2DRowRealMatrix(0,0);
			fieldIndices = new int[0];
			numObjects = 0;
			numFields = 0;
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
	// Notes   : throws a DiscriminantAnalysisException object if numFields 
	//           is not set
	// 
	//*************************************************************************
	public int getNumAttributes() throws DiscriminantAnalysisException {
		validateNumFields();
		return numFields;
	}
	
	//*************************************************************************
	// Name    : getNumObservations
	// 
	// Purpose : returns the number of observations/objects to classify
	// 
	// Notes   : throws a DiscriminantAnalysisException object if numObjects 
	//           is not set
	// 
	//*************************************************************************
	public int getNumObservations() throws DiscriminantAnalysisException {
		validateNumObjects();
		return numObjects;
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
		
		// compute class indices
		setClassIndices();
		
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
	//           input array. Also sets the log prior probabilities vector.
	// 
	// Notes   : creates zero length vectors in case of an error
	// 
	//*************************************************************************
	public void setPriorProbabilities(double[] priorProbabilities) {
		try{
			
			// set the prior probabilities vector
			this.priorProbabilities = new ArrayRealVector(priorProbabilities);
			
			// compute the log of the prior probabilities
			this.logPriorProbabilities = this.priorProbabilities.mapLog();
				
		} catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.priorProbabilities = new ArrayRealVector();
			this.logPriorProbabilities = new ArrayRealVector();
		} 
	}
	
	//*************************************************************************
	// Name    : setPriorProbabilities
	// 
	// Purpose : sets the prior probabilities to be equal (1 / number of classes)
	//
	// Notes   : throws a DiscriminantAnalysisException if numClasses has not 
	//           been set. creates zero length vector in case of an error
	// 
	//*************************************************************************
	public void setPriorProbabilities() throws DiscriminantAnalysisException {
		
		validateNumClasses();
		
		double priorProb = (1 / (double)numClasses);
		double[] priorProbs = new double[numClasses];
		Arrays.fill(priorProbs, priorProb);
			
		setPriorProbabilities(priorProbs);
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
	// Notes   : throws a DiscriminantAnalysisException object if the classification,
	//           classFrequencies arrays or numClasses are not set
	// 
	//*************************************************************************
	public double getRandomClassificationAccuracy() throws DiscriminantAnalysisException {
		
		validateClassification();
		validateClassFrequencies();
		validateNumClasses();
			
		double randomClassAccuracy = 0;
		
		for ( int i = 0; i < numClasses; i++) {
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
	//           also compute the number of classes and the classIndices
	// 
	// Notes   : throws a DiscriminantAnalysisException object if 
	//           the classification array is not set
	//           uniqueClasses set to zero length array if an error occurs
	//           numClasses set to zero
	//           classIndices is a zero length array list
	// 
	//*************************************************************************
	protected void setUniqueClasses() throws DiscriminantAnalysisException {
		
		validateClassification();
		
		uniqueClasses = NCGStatUtils.getUniqueItems(classification);
		
		// compute the number of unique classes
		numClasses = uniqueClasses.length;
		
	}
	
	//*************************************************************************
	// Name    : setClassIndices
	// 
	// Purpose : compute the classIndices (indices of each class in
	//           the classification array)
	// 
	// Notes   : throws a DiscriminantAnalysisException object if the
	//           classification, numClasses, classFrequences or uniqueClasses
	//           attributes are not set
	//           uniqueClasses set to zero length array if an error occurs
	//           numClasses set to zero
	//           classIndices is a zero length array list
	// 
	//*************************************************************************
	protected void setClassIndices() throws DiscriminantAnalysisException {
		
		validateNumClasses();
		validateClassFrequencies();
		validateClassification();
		validateUniqueClasses();
		
		// compute indices of items in each class and store in a list of int[] arrays
		// this way we don't have to compute them each time
		classIndices = new ArrayList<int[]>();
		
		for ( int c = 0; c < numClasses; c++ ) {
			
			int[] classIndicesArray = new int[classFrequencies[c]];
			
			int j=0;
			for(int i = 0; i < classification.length; i++) {
				if ( classification[i] == uniqueClasses[c] ) {
					classIndicesArray[j++] = i;
				}
			}
			
			classIndices.add(classIndicesArray);		
		}
		
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
	//           classification array
	// 
	// Notes   : throws a DiscriminantAnalysisException object if the classification
	//           array is not set or if any class contains less 
	//           than two objects. Sets the classFrequencies array to zero length 
	//           array in case of an error.
	// 
	//*************************************************************************
	private void setClassFrequencies() throws DiscriminantAnalysisException {
		
		validateClassification();
		
		classFrequencies = NCGStatUtils.getFrequencies(classification);
		
		// make sure that each class contains at least two observations
		// cannot compute covariance matrix for less than two observations per class.
		for (int j=0; j < classFrequencies.length; j++) {
			if (classFrequencies[j] < 2) {
				throw new DiscriminantAnalysisException("cannot compute covariance matrix for less than two observations");
			}
		}
				
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
	// Notes   : throws a DiscriminantAnalysisException object if 
	//           classification/uniqueClasses or classFrequencies are not set
	//           returns a zero length array of ints in case of an error
	// 
	//*************************************************************************
	/*protected int[] computeClassIndices(int classIndex) throws DiscriminantAnalysisException {
		
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
	}*/	
		
	//*************************************************************************
	// Name    : getClassMean
	// 
	// Purpose : compute mean vector for class with index classIndex in the
	//           uniqueClasse array.
	// 
	// Notes   : throws a DiscriminantAnalysisException object if the 
	//           predictorVariables, fieldIndices, numFields or classIndices are not set. 
	//           returns zero length vector in case of an error
	// 
	//*************************************************************************
	protected RealVector getClassMean(int classIndex) throws DiscriminantAnalysisException {
		
		validatePredictorVariables();
		validateFieldIndices();
		validateNumFields();
		validateClassIndices();
		
		// vector reference to hold class means
		RealVector classMeansVector = null;
		
		// get the row indices for class 'classIndex'
		int[] classRowIndices = classIndices.get(classIndex);
		
		try {
		
			// compute sub matrix for class with index 'classIndex'
			RealMatrix predC = 
				predictorVariables.getSubMatrix(classRowIndices,fieldIndices);
	
			// assign memory for group means
			double[] groupMeans = new double[numFields];
				
			for ( int j = 0; j < numFields; j++ ) {	
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
	// Notes   : throws a DiscriminantAnalysisException object if the
	//           predictorVariables, fieldIndices, classIndices or numFields
	//           are not set.
	//           Return zero length matrix in case of an error
	// 
	//*************************************************************************
	private RealMatrix getCovarianceMatrix(int classIndex) throws DiscriminantAnalysisException {
		
		validatePredictorVariables();
		validateFieldIndices();
		validateClassIndices();
		validateNumFields();
					
		// reference to covariance matrix
		RealMatrix covMatrix = null;
		
		try {
			
			// compute row indices for class 'classIndex'
			int[] classRowIndices = classIndices.get(classIndex);
							
			// compute sub matrix for class 'classIndex'
			RealMatrix predC = 
				predictorVariables.getSubMatrix(classRowIndices,fieldIndices);
			
			if ( numFields > 1) {
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
	// Notes   : throws a DiscriminantAnalysisException object if the 
	//           classFrequencies, numFields, numClasses or numObjects are not set
	//           returns zero size matrix in case of an error
	// 
	//*************************************************************************
	private RealMatrix getPooledCovMatrix() throws DiscriminantAnalysisException {
		
		validateClassFrequencies();
		validateNumFields();
		validateNumClasses();
		validateNumObjects();
		
		// reference to pooled class covariance matrix
		RealMatrix pooledCovMatrix = null;
		
		try {
				
			// assign memory for pooled sum of squares matrix
			// java arrays values are automatically set to zero
			RealMatrix pooledSumSquares = 
				MatrixUtils.createRealMatrix(numFields,numFields);
	
			// get indices for each class in turn and compute the 
			// total sum of squares (in order to compute the pooled
			// class covariance matrix
			for( int classIndex = 0; classIndex < numClasses; classIndex++ ) {
			
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
								(1.0 / numObjects));
				
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
	// Notes   : throws a DiscriminantAnalysisException Object if 
	//           classifed, classification, numClasses or numObjects are not set
	//           returns a 2d array of zeros in case of an error
	// 
	//*************************************************************************
	public int[][] confusionMatrix() throws DiscriminantAnalysisException {
		
		validateClassification();
		validateClassified();
		validateNumClasses();
		validateNumObjects();
		
		int[][] cMatrix = null;
		
		try{
			
			// create the confusion matrix and set the 
			// values are set to zero initially by default in java
			cMatrix = new int[numClasses][numClasses];
						
			// compute confusion matrix
			for ( int c = 0; c < numObjects; c++) {
				
				for ( int i = 0; i < numClasses; i++) {
					for ( int j = 0; j < numClasses; j++) {
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
			for ( int i = 0; i < numClasses; i++) {		
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
	// Name    : computeLDAParameters
	// 
	// Purpose : computes the parameters for a linear discriminant analysis 
	//           classifier 
	// 
	// Notes   : throws a DiscriminantAnalysisException if the number of fields
	//           is not set or prior probabilities are not set.
	//           returns a zero length vector if an error occurs
	// 
	//*************************************************************************
	protected RealVector computeLDAParameters(RealMatrix covInv, RealVector mean, int c) throws DiscriminantAnalysisException  {
				
		validateNumFields();
		validatePriorProbabilities();
		
		// reference to vector containing the new parameters
		RealVector params = null;
		
		try {
				
			params = new ArrayRealVector(numFields+1);
			
			RealVector paramsNoIntercept = covInv.preMultiply(mean);
			double intercept = (mean.dotProduct(paramsNoIntercept) * (-0.5)) + logPriorProbabilities.getEntry(c);
			params.setEntry(0, intercept);
			params.setSubVector(1, paramsNoIntercept);
								
		}  catch (Exception e) {
			logger.severe(e.getCause().toString() + " : " + e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			params = new ArrayRealVector();
		} 
		
		return params;
	}
	
	//*************************************************************************
	// Name    : assignObservationToClass
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
	protected int assignObservationToClass(RealVector mhDistance2) throws DiscriminantAnalysisException {
		
		validatePriorProbabilities();
		validateUniqueClasses();
		
		int classLabel = -1;
		
		try {
						
			// subtract the log of the prior probabilities from the mahalanobis distance squared vector
			double[] mh2Classes = mhDistance2.mapDivide(2).subtract(logPriorProbabilities).getData();
			
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
	// Name    : createOutputArrays
	// 
	// Purpose : assign memory for the output arrays / matrices 
	//           (mahalanobisDistance2, posteriorProbabilities, parameters
	//           and classified)
	// 
	// Notes   : throws any exceptions encountered during the assignment
	// 
	//*************************************************************************
	protected void createOutputArrays() throws Exception {
		
		validateNumObjects();
		validateNumFields();
		
		// assign memory or mahalanobis distance squared matrix
		mahalanobisDistance2 = MatrixUtils.createRealMatrix(numObjects,numClasses);
		
		// assign memory for parameters (coefficients)
		parameters = MatrixUtils.createRealMatrix(numFields+1,numClasses);
			
		// assign memory for classified array
		classified = new int[numObjects];
			
		// assign memory for posterior probabilities for each class
		posteriorProbabilities = MatrixUtils.createRealMatrix(numObjects,numClasses);
			
	}
		
	//*************************************************************************
	// Name    : classify
	// 
	// Purpose : classify the predictor (independent) variables
	// 
	// Notes   : throws a DiscriminantAnalysisException Object if 
	//           predictorVariables, numObjects or numClasses are not set
	//           sets classified array, parameters matrix, posteriorProbabilites matrix 
	//           and mahalanobisDistance2 matrix to empty zero length arrays
	//           in case of an error.
	// 
	//*************************************************************************
	public void classify() throws DiscriminantAnalysisException {
		
		validatePredictorVariables();		
		validateNumObjects();
		validateNumClasses();
				
		try {
						
			//assign memory for output variables	 
			createOutputArrays();
									
			// create pooled group covariance matrix (maximum likelihood version)
			RealMatrix pooledCovMatrix = getPooledCovMatrix();

			// compute inverse of pooled group covariance matrix (if it exists)
			DecompositionSolver solver = (new LUDecompositionImpl(pooledCovMatrix)).getSolver();
			
			// check to see if covariance matrix is invertible
			if ( solver.isNonSingular() ) {
				
				// compute the inverse of the pooled class covariance matrix
				RealMatrix pooledCovMatrixInv = solver.getInverse();

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
					
					// compute the parameters (classification function coefficients)
					RealVector params = computeLDAParameters(pooledCovMatrixInv,classMeans,c);
					parameters.setColumnVector(c, params);
								
				}
							
				// classify each observation and compute posterior probabilities
				for (int i = 0; i < numObjects; i++) {
					
					// classify the current observation
					classified[i] = assignObservationToClass(mahalanobisDistance2.getRowVector(i));
					
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
