package ncg.statistics;
/* 
Implementation of Global Discriminant Analysis
Peter Foley, 19.07.2010
*/

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
	private RealMatrix predictorVariables = null;
	
	// array to hold classification variable
	private int[] classification = null;
	
	// array to hold prior probabilities
	private RealVector priorProbabilities = null;
	
	// array to hold unique class labels
	private int[] uniqueClasses = null;

	// array to hold unique class frequencies
	private int[] classFrequencies = null;
	
	// output variables
	private int[] classified = null;
	private RealMatrix posteriorProbabilities = null;
	private RealMatrix parameters = null;
	private RealMatrix mahalanobisDistance2 = null;
	
	//logger object
	protected final static Logger logger = 
		Logger.getLogger(DiscriminantAnalysis.class.getName());
	
	// empty constructor
	public DiscriminantAnalysis(){
	}
	
	// set the predictor variables
	// rowOrder is set to true if the first dimension of the input array
	// contains the rows (observations). If rowOrder is set to false
	// the first dimension of the input array refers to columns (attributes
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
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}
		
	}

	// get the predictor variables 
	// return zero length 2d array of doubles if not set
	public double[][] getPredictorVariables() {
		
		if ( predictorVariables != null ) {
			return predictorVariables.getData();
		} else {
			return new double[0][0];
		}
		
	}
	
	// return the number of independent variables (attributes)
	public int getNumAttributes() {
		if ( predictorVariables != null ) {
			return predictorVariables.getColumnDimension();
		} else {
			return 0;
		}
	}
	
	// return the number of observations (rows)
	public int getNumObservations() {
		if ( predictorVariables != null ) {
			return predictorVariables.getRowDimension();
		} else {
			return 0;
		}
	}
	
	// return parameters (coefficients) of classification functions
	// return zero length 2d array of doubles if not set
	public double[][] getParameters() {
		if (parameters != null) {
			return parameters.getData();
		} else {
			return new double[0][0];
		}
	}
	
	// return posterior probabilities for class 'classIndex'
	// return zero length array of doubles if not set
	public double[] getPosteriorProbabilities(int classIndex) {
	
		double[] col = null;
		
		if ( posteriorProbabilities != null) {
			
			try {
				col = posteriorProbabilities.getColumn(classIndex);
			} catch (MatrixIndexException e) {
				logger.severe(e.toString() + " : " + e.getMessage());
				e.printStackTrace();
				col = new double[0];
			}
			
		} else {
			col = new double[0];
		}
		
		return col;
	}
	
	// get full 2d array of posterior probabilities
	public double[][] getPosteriorProbabilities() {
		
		if (posteriorProbabilities != null) {
			return posteriorProbabilities.getData();
		} else {
			return new double[0][0];
		}
	}
		
	// set the classification
	public void setClassification(int[] classification) {
		
		this.classification = classification;
		
		// compute unique class labels
		setUniqueClasses();

		// compute class frequencies
		setClassFrequencies();
		
	}

	// get the classification
	// return zero length array of ints if not set
	public int[] getClassification() {

		if (classification != null) {
			return classification;
		} else {
			return new int[0];
		}
	}

	// set prior probabilities
	public void setPriorProbabilities(double[] priorProbabilities) {
		try{
			this.priorProbabilities = new ArrayRealVector(priorProbabilities);
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}
	}

	// get prior probabilites
	// returns zero length array of doubles if not set
	public double[] getPriorProbabilities() {
	
		if ( priorProbabilities != null ) {
			return priorProbabilities.getData();
		} else {
			return new double[0];
		}
	}

	// get classication 
	// returns zero length array of ints if not set
	public int[] getClassified() {
		if (classified != null) {
			return classified;
		} else {
			return new int[0];
		}
	}

	// get mahalanobis distance squared for class 'classIndex'
	// return zero 2d length array of double if not set
	public double[] getMahalanobisDistance2(int classIndex) {
		
		double[] col = null;
		
		if ( mahalanobisDistance2 != null) {
			
			try {
				col = mahalanobisDistance2.getColumn(classIndex);
			} catch (MatrixIndexException e) {
				logger.severe(e.toString() + " : " + e.getMessage());
				e.printStackTrace();
				col = new double[0];
			}
			
		} else {
			col = new double[0];
		}
		
		return col;
		
	}
	// return full 2d array of mahalanobis distance
	public double[][] getMahalanobisDistance2() {
		
		if (mahalanobisDistance2 != null) {
			return mahalanobisDistance2.getData();
		} else {
			return new double[0][0];
		}
	}

	
	//compute unique classes labels from classification array
	private void setUniqueClasses() {
		
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
		}
	}
	
	// compute class frequencies
	private void setClassFrequencies() {
		
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
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}
	}

	// get unique class labels 
	// return zero length array of ints if not set
	public int[] getUniqueClasses() {
		if (uniqueClasses != null) {
			return uniqueClasses;
		} else {
			return new int[0];
		}
	}
	// get class frequencies 
	// return zero length array of ints if not set
	public int[] getClassFrequencies() {
		if (classFrequencies != null) {
			return classFrequencies;
		} else {
			return new int[0];
		}
	}

	// return integer array of row indices for the classIndex th class
	// classIndex is the index of the classIndex th class in the 
	// integer array returned by uniqueClasses
	// return null in case of an error
	private int[] computeClassIndices(int classIndex){
		
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
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} 
		
		return classRowIndices;
	}	
	
	// return integer array of column (field) indices
	// return null in case of an error
	private int[] computeFieldIndices() {
		
		int[] colIndices = null;
		
		try {
			
			colIndices = new int[predictorVariables.getColumnDimension()];
			
			for(int j = 0; j < colIndices.length; j++) {
				colIndices[j] = j; 
			}
			
		} catch ( NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}
	
		return colIndices;
	}
	
	// compute group means for class with index classIndex in the
	// array uniqueClasses
	// return null in case of an error
	private RealVector getClassMean(int classIndex) {
		
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
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}
	
		return classMeansVector;
			
	}

	// compute covariance matrix for the class with index classIndex
	// in the array uniqueClases
	// return null in case of an error
	private RealMatrix getCovarianceMatrix(int classIndex) {
		
		// reference to covariance matrix
		RealMatrix covMatrix = null;
		
		// compute field indices
		int[] fieldIndices = computeFieldIndices();

		// compute row indices for class 'classIndex'
		int[] classRowIndices = computeClassIndices(classIndex);
		
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
		} catch (IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}
				
		return covMatrix;
			
	}

	// compute the pooled class covariance matrix
	// return null in case of an error
	private RealMatrix getPooledCovMatrix() {
		
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
	public int[][] confusionMatrix() {
		
		int[][] cMatrix = new int[0][0];
		
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
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}

		return cMatrix;
	}
	
	// do the classification
	public void classify() {
		
		try {
			
			int numObjects = predictorVariables.getRowDimension();
			int numFields = predictorVariables.getColumnDimension();
			int numClasses = uniqueClasses.length;
			
			// compute the log of the prior probabilities (if set)
			RealVector logPriorProbabilities = null;
			if (priorProbabilities != null) {
				logPriorProbabilities = priorProbabilities.mapLog();
			} else {
				logPriorProbabilities = new ArrayRealVector(numClasses, (1.0 / (double)numClasses)).mapLog();
			}
			
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
					RealVector mvnD = mahalanobisDistance2.getRowVector(i).mapDivide(-2.0).mapExp();
					
					if (priorProbabilities != null) {
						mvnD = mvnD.ebeMultiply(priorProbabilities);
					}
					
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
				logger.severe("Singular pooled covariance matrix - unable to classify");
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
