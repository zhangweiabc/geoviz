package ncg.statistics;
/* 
Implementation of Global Discriminant Analysis
Peter Foley, 19.07.2010
*/

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import java.util.logging.Logger;

import org.apache.commons.math.stat.correlation.Covariance;
import org.apache.commons.math.linear.LUDecomposition;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.MatrixIndexException;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.linear.InvalidMatrixException;

public class DiscriminantAnalysis {
	
	// array to hold predictor variables
	private transient RealMatrix predictorVariables = null;
	
	// array to hold classification variable
	private transient int[] classification = null;
	
	// array to hold prior probabilities
	private transient RealVector priorProbabilities = null;
	
	// array to hold unique class labels
	private transient int[] uniqueClasses = null;

	// array to hold unique class frequencies
	private transient int[] classFrequencies = null;
	
	// output variables
	private transient int[] classified = null;
	private transient RealMatrix posteriorProbabilities = null;
	private transient RealMatrix parameters = null;
	private transient RealMatrix mahalanobisDistance2 = null;
	
	// classification accuracy
	private transient double classificationAccuracy = -1;
	
	//logger object
	protected final static Logger logger = 
		Logger.getLogger(DiscriminantAnalysis.class.getName());
	
	// empty constructor
	public DiscriminantAnalysis(){
	}
	
	// reset all member variables & try to free up memory
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
	
	// check to see if the classification attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validateClassification() throws DiscriminantAnalysisException {		
		if ( classification == null ) {
			throw new DiscriminantAnalysisException("input classification variable not set");
		}
	}
	
	// check to see if the uniqueClasses attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validateUniqueClasses() throws DiscriminantAnalysisException {
			
		if ( uniqueClasses == null ) {
			throw new DiscriminantAnalysisException("unique classes not computed (set input classification variable)");
		}
	}
	
	// check to see if the classFrequencies attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validateClassFrequencies() throws DiscriminantAnalysisException {		
		if ( classFrequencies == null ) {
			throw new DiscriminantAnalysisException("class frequencies not computed (set input classification variable)");
		}
	}
	
	// check to see if the predictorVariables attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validatePredictorVariables() throws DiscriminantAnalysisException {	
		if ( predictorVariables == null ) {
			throw new DiscriminantAnalysisException("input predictor variables not set");
		}
	}
	
	// check to see if the priorProbabilities attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validatePriorProbabilities() throws DiscriminantAnalysisException {
		if ( priorProbabilities== null ) {
			throw new DiscriminantAnalysisException("input prior probabilities not set");
		}
	}
	
	// check to see if the uniqueClasses attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validateClassified() throws DiscriminantAnalysisException {
		if ( classified == null ) {
			throw new DiscriminantAnalysisException("output classification not set");
		}
	}
	
	// check to see if classificationAccuracy has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validateClassificationAccuracy() throws DiscriminantAnalysisException {
		
		if ( classificationAccuracy == -1 ) {
			throw new DiscriminantAnalysisException("output classification accuracy not set");
		}
	}
	
	// check to see if the posteriorProbabilities attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validatePosteriorProbabilities() throws DiscriminantAnalysisException {
		if ( posteriorProbabilities == null ) {
			throw new DiscriminantAnalysisException("output posterior probabilities not set");
		}
	}
	
	// check to see if the parameters attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validateParameters() throws DiscriminantAnalysisException {
		if ( parameters == null ) {
			throw new DiscriminantAnalysisException("output parameters not set");
		}
	}
	
	// check to see if the mahalanobisDistance2 attribute has been set
	// throw a new DiscriminantAnalysisException if it has not been set
	public void validateMahalanobisDistance2() throws DiscriminantAnalysisException {
		if ( mahalanobisDistance2 == null ) {
			throw new DiscriminantAnalysisException("output mahalanobis distance squared not set");
		}
	}
	
	// set the predictor variables
	// rowOrder is set to true if the first dimension of the input array
	// contains the rows (observations). If rowOrder is set to false
	// the first dimension of the input array refers to columns (attributes)
	public void setPredictorVariables(double[][] predictorVariables,
					boolean rowOrder) {
		try {
			if (rowOrder == true) {
				this.predictorVariables =  
					MatrixUtils.createRealMatrix(predictorVariables);
			} else {
				this.predictorVariables =  
					MatrixUtils.createRealMatrix(predictorVariables).transpose();
			}
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.predictorVariables = null;
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.predictorVariables = null;
		}
		
	}

	// get the predictor variables 
	// throws a DiscriminantAnalysisException object if not set
	public double[][] getPredictorVariables() throws DiscriminantAnalysisException {		
		validatePredictorVariables();
		return predictorVariables.getData();		
	}

	// return the number of independent variables (attributes)
	// throws a DiscriminantAnalysisException object if not set
	public int getNumAttributes() throws DiscriminantAnalysisException {
		validatePredictorVariables();
		return predictorVariables.getColumnDimension();
	}
	
	// return the number of observations (rows)
	// throws a DiscriminantAnalysisException object if not set
	public int getNumObservations() throws DiscriminantAnalysisException {
		validatePredictorVariables();
		return predictorVariables.getRowDimension();
	}
	
	// set the classification
	// throws a DiscriminantAnalysisException if not set
	public void setClassification(int[] classification) throws DiscriminantAnalysisException {
		
		this.classification = classification;
		
		// compute unique class labels
		setUniqueClasses();

		// compute class frequencies
		setClassFrequencies();
		
	}
	
	// get the (input) classification 
	// throws a DiscriminantAnalysisException if not set
	public int[] getClassification() throws DiscriminantAnalysisException {
		validateClassification();
		return Arrays.copyOf(classification,classification.length);
	}
	
	// set prior probabilities
	public void setPriorProbabilities(double[] priorProbabilities) {
		try{
			this.priorProbabilities = new ArrayRealVector(priorProbabilities);
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.priorProbabilities = null;
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.priorProbabilities = null;
		}
	}
	
	// set the prior probabilities to be equal by default
	// this relies on the uniqueClasses attributes having been set
	// if this is not set then a DiscriminantAnalysisException object is thrown
	public void setPriorProbabilities() throws DiscriminantAnalysisException {
		
		validateUniqueClasses();
		int numClasses = uniqueClasses.length;
		
		try{
			this.priorProbabilities = new ArrayRealVector(numClasses, (1.0 / (double)numClasses));
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.priorProbabilities = null;
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			this.priorProbabilities = null;
		}
	}

	// get prior probabilities
	// throws a DiscriminantAnalysisException object if not set
	public double[] getPriorProbabilities() throws DiscriminantAnalysisException {
		validatePriorProbabilities();
		return priorProbabilities.getData();
	}
	
	// return posterior probabilities for class 'classIndex'
	// throws a DiscriminantAnalysisException object if not set
	// if 'classIndex' is out of range then return a zero length array of doubles
	public double[] getPosteriorProbabilities(int classIndex) throws DiscriminantAnalysisException {
	
		validatePosteriorProbabilities();
		
		double[] col = null;
			
		try {
			col = posteriorProbabilities.getColumn(classIndex);
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			col = new double[0];
		}
				
		return col;
	}
	
	/*
	 * get methods for output of classification : classified, posteriorProbabilities, parameters
	 * and mahalanobisDistance2
	 */
	
	// get classification output (copy)
	// throws a DiscriminantAnalysisException object if not set
	public int[] getClassified() throws DiscriminantAnalysisException {
		validateClassified();
		return Arrays.copyOf(classified,classified.length);
	}
	// return the classification accuracy (number of correct classifications)
	public double getClassificationAccuracy() throws DiscriminantAnalysisException {
		validateClassificationAccuracy();
		return classificationAccuracy;
	}
	
	// return the random classification accuracy ( result of classification based on class frequencies)
	public double getRandomClassificationAccuracy() throws DiscriminantAnalysisException {
		
		validateClassification();
		validateClassFrequencies();
			
		double randomClassAccuracy = 0;
		
		for ( int i = 0; i < classFrequencies.length; i++) {
			randomClassAccuracy += Math.pow(( (double)classFrequencies[i] / (double)classification.length),2);
		}
		
		return randomClassAccuracy;
	}
	
	// get full 2d array of posterior probabilities
	// throws a DiscriminantAnalysisException object if not set
	public double[][] getPosteriorProbabilities() throws DiscriminantAnalysisException {
		validatePosteriorProbabilities();
		return posteriorProbabilities.getData();
	}
	
	// return parameters (coefficients) of classification functions
	// throws a DiscriminantAnalysisException object if not set
	public double[][] getParameters() throws DiscriminantAnalysisException {
		validateParameters();
		return parameters.getData();
	}
	
	// return full 2d array of mahalanobis distance
	// throws a DiscriminantAnalysisException object if not set
	public double[][] getMahalanobisDistance2() throws DiscriminantAnalysisException {		
		validateMahalanobisDistance2();
		return mahalanobisDistance2.getData();
	}
	
	// get mahalanobis distance squared for class 'classIndex'
	// throws a DiscriminantAnalysisException object if not set 
	// if 'classIndex' is out of range return a zero length array of doubles
	public double[] getMahalanobisDistance2(int classIndex) throws DiscriminantAnalysisException {
			
		validateMahalanobisDistance2();
		double[] col = null;
				
		try {
			col = mahalanobisDistance2.getColumn(classIndex);
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			col = new double[0];
		}
					
		return col;
		
	}
	
	/*
	 * get and set methods for internal variables - uniqueClasses and classFrequencies	
	 */
	
	//compute unique classes labels from classification array
	private void setUniqueClasses() throws DiscriminantAnalysisException {
		
		// check to see if the classification attribute has been set
		// cannot execute this method until it has been set
		validateClassification();
		
		try {
			
			// compute unique classes in the classification array using a set
			Set<Integer> classes = new HashSet<Integer>();
			for (int i = 0; i < classification.length; i++ ) {
				classes.add(classification[i]);
			}
		
			// convert the classes set to an array of ints
			uniqueClasses = new int[classes.size()];
			Iterator<Integer> classesIt = classes.iterator();
		
			int i = 0;
			while( classesIt.hasNext() ) {
				uniqueClasses[i++] = 
					((Integer)classesIt.next()).intValue();
			}
			
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			uniqueClasses = null;
		}
	}
	
	// get unique class labels 
	// throws a DiscriminantAnalysisException object if not set
	public int[] getUniqueClasses() throws DiscriminantAnalysisException {	
		validateUniqueClasses();
		return Arrays.copyOf(uniqueClasses,uniqueClasses.length);
	}
	
	// compute class frequencies
	private void setClassFrequencies() throws DiscriminantAnalysisException {
		
		// check to see if the classification and uniqueClasses attributes have been set
		// cannot continue if these are not set
		validateClassification();
		validateUniqueClasses();
		
		try {
			
			// compute the frequency of each class label
			Frequency classFrequency = new Frequency();
			for (int i = 0; i < classification.length; i++ ) {
				classFrequency.addValue(classification[i]);
			}

			// save the class frequencies to an int array	
			classFrequencies = new int[uniqueClasses.length];
			for(int i = 0; i < uniqueClasses.length; i++) {
				classFrequencies[i] = 
					(int)classFrequency.getCount(uniqueClasses[i]);
			}
			
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classFrequencies = null;
			
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classFrequencies = null;
		}
	}

	
	// get class frequencies 
	// throws a DiscriminantAnalysisException object  if not set
	public int[] getClassFrequencies() throws DiscriminantAnalysisException {
		validateClassFrequencies();
		return Arrays.copyOf(classFrequencies,classFrequencies.length);
	}

	// return integer array of row indices for the classIndex th class
	// classIndex is the index of the classIndex th class in the 
	// integer array returned by uniqueClasses
	// throws a DiscriminantAnalysisException object or returns a zero length array of ints in case of an error
	private int[] computeClassIndices(int classIndex) throws DiscriminantAnalysisException {
		
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
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classRowIndices = new int[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classRowIndices = new int[0];
		} 
		
		return classRowIndices;
	}	
	
	// return integer array of column (field) indices
	// throws DiscriminantAnalysisException object or returns zero length array
	// in case of an error
	private int[] computeFieldIndices() throws DiscriminantAnalysisException {
		
		// check to see if predictorVariables have been set
		// cannot continue unti this has been set
		validatePredictorVariables();
		
		int[] colIndices = null;
		
		try {
			
			colIndices = new int[predictorVariables.getColumnDimension()];
			
			for(int j = 0; j < colIndices.length; j++) {
				colIndices[j] = j; 
			}
			
		} catch ( NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			colIndices = new int[0];
		}
	
		return colIndices;
	}
	
	// compute group means for class with index classIndex in the
	// array uniqueClasses
	// return null in case of an error
	private RealVector getClassMean(int classIndex) throws DiscriminantAnalysisException {
		
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
				
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classMeansVector = null;
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classMeansVector = null;
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			classMeansVector = null;
		}
	
		return classMeansVector;
			
	}

	// compute covariance matrix for the class with index classIndex
	// in the array uniqueClases
	// return null in case of an error
	private RealMatrix getCovarianceMatrix(int classIndex) throws DiscriminantAnalysisException {
		
		// check to see if predictorVariables have been set
		// cannot continue unti this has been set
		validatePredictorVariables();
				
		// compute field indices
		int[] fieldIndices = computeFieldIndices();
		
		// check to make sure that there are least two fields
		if ( fieldIndices.length < 2 ) {
			throw new DiscriminantAnalysisException("cannot compute covariance matrix for less than two fields");
		}

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
			
			// compute covariance matrix for class 'classIndex'
			Covariance c = new Covariance(predC);
			covMatrix = c.getCovarianceMatrix();
			
			
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			covMatrix = null;
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			covMatrix = null;
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			covMatrix = null;
		}
				
		return covMatrix;
			
	}

	// compute the pooled class covariance matrix
	// return null in case of an error
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
				double degreesFreedom = (double)(classFrequencies[classIndex] -1);
			
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
				
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();				
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}
	
		return pooledCovMatrix;
	}
	
	// return the index of the minimum element in the items array
	private int getMin(double[] items) {
		
		int minIndex = -1;
		
		if (items != null) {
		
			if ( items.length > 0) {
				
				minIndex = 0;
	
				for (int i = 1; i < items.length; i++) {
					
					if (items[i] < items[minIndex] ) {
						minIndex = i;
					}
				}
			}
		}
	
		return minIndex;
	
	}

	// compute confusion matrix
	// return 2d array of zeros in case of an error
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
			
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			cMatrix = new int[0][0];
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			cMatrix = new int[0][0];
		}

		return cMatrix;
	}
		
	// do the classification
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
	
					// compute mahalanobis distance squared separately for 
					// each object in the dataset
					for (int i = 0; i < numObjects; i++) {
	
						RealVector meanDiff = 
						predictorVariables.getRowVector(i).subtract(classMeans);
					
						double mh2 = pooledCovMatrixInv.preMultiply(meanDiff).dotProduct(meanDiff);
						mahalanobisDistance2.setEntry(i,c, mh2);
					}
				
					// compute parameters (coefficients) for the cth class
					RealMatrix classMeansMatrix = MatrixUtils.createColumnRealMatrix(classMeans.getData());
					RealMatrix pn = pooledCovMatrixInv.multiply(classMeansMatrix);
					parameters.setSubMatrix(pn.getData(),1,c);
					parameters.setEntry(0,c, ( (classMeans.dotProduct(pn.getColumnVector(0)) * (-0.5)) + logPriorProbabilities.getEntry(c)));
				
				}
				
			
				// assign memory for posterior probabilities for each class
				posteriorProbabilities = MatrixUtils.createRealMatrix(numObjects,numClasses);
			
				// compute posterior probabilities for observation
				for (int i = 0; i < numObjects; i++) {
				
					// compute the pdf values for the observations in row i
					// covariance matrix C is positive semi-definite so x'C' >= 0
					// pdf is therefore constrained to lie in range[0,1]
					RealVector mvnD = mahalanobisDistance2.getRowVector(i).mapDivide(-2.0).mapExp().ebeMultiply(priorProbabilities);
										
					// since pdf values are in the range [0,1] computing
					// the l1 norm (sum of absolute values) is equivalent to
					// summing the vector item values
					double sumMhd = mvnD.getL1Norm();
					posteriorProbabilities.setRowVector(i, mvnD.mapDivide(sumMhd));
				}
				
				// assign memory for classified array
				classified = new int[numObjects];
				
				// now classify the data by assigning each object to the
				// nearest class using the mahalanobis distance squared 
				// metric together with the prior probabilities (if they are set)
				for (int i = 0; i < numObjects; i++) {
					
					double[] mh2Classes = null;
				
					if (logPriorProbabilities != null ) {			
						mh2Classes = mahalanobisDistance2.getRowVector(i).mapDivide(2).subtract(logPriorProbabilities).getData();
					} else {
						mh2Classes = mahalanobisDistance2.getRowVector(i).mapDivide(2).getData();
					}
				
					int classIndex = getMin(mh2Classes);
					classified[i] = uniqueClasses[classIndex];
				}
			} else {
				String message = "Singular pooled covariance matrix - unable to classify";
				logger.severe(message);
				throw new DiscriminantAnalysisException(message);
			}
			
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch(InvalidMatrixException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} 
	}
}
