package ncg.statistics;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;

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
				
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
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
			
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			itemFrequencies = new int[0];
			
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			itemFrequencies = new int[0];
		}
		
		return itemFrequencies;
	}

}
