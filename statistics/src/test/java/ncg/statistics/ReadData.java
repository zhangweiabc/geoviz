package ncg.statistics;
/*
Class to Read data from an input csv file into an array
Peter Foley, 19.07.2010
*/
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math.linear.MatrixIndexException;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.MatrixUtils;

public class ReadData {
	
	// url to input file
	private String url = null;
	
	// data 
	private RealMatrix data = null;

	// fieldnames for data array
	private String[] fieldNames = null;

	// array to store row indices (speed up)
	private int[] rowIndices = null;
	
	// logger
	protected final static Logger logger = Logger.getLogger(DiscriminantAnalysisTest.class.getPackage().getName());

	// default empty constructor
	public ReadData() {
	}
	
	//constructor specifying url to input file
	public ReadData(String url) {
		this.url = url;
	}

	// read the data from input file
	public void readFile() throws IOException {
		
		LineNumberReader inputStream = null;

		try {
			// open the input stream
			inputStream = new LineNumberReader(
								new FileReader(url));
			
			// temporary list of string arrays to hold input data
			List<String[]> s_data = new ArrayList<String[]>();

			// read the data from file
			int num_cols = 0;
			String line = null;
			while ((line = inputStream.readLine() ) != null ) {
				String[] fields = line.split(",");
				s_data.add(fields);
				num_cols = Math.max(num_cols,fields.length);
			}
			
			// compute the number of rows in input file
			int num_rows = s_data.size();
			
			// allocate enough memory to store data in file
			double[][] data_arr =  new double[num_rows-1][num_cols];

			// allocate memory for rowIndices 
			rowIndices = new int[num_rows-1];

			//convert s_data to data / fieldnames
			for ( int item = 0; item < num_rows; item++) {
				
				// get fields for current line
				String[] fields = s_data.get(item);

				if (item > 0) {
					
					// store data
					for (int field = 0; field < fields.length; field++ ) {
						data_arr[item-1][field] = 
									Double.parseDouble(fields[field]);
					}

					rowIndices[item-1] = item-1;

				} else {
					
					// store field names
					fieldNames = fields;
				}
			}
			
			// save the array of doubles as a real matrix
			data = MatrixUtils.createRealMatrix(data_arr);
				
			logger.info("Read [" + String.valueOf(data.getRowDimension()) + 
					"] lines from input file " + this.url);
			
			logger.info("Input Data contains [" + 
					String.valueOf(data.getColumnDimension()) + "] fields");
			
			if (data.getColumnDimension() != fieldNames.length ) {
				logger.severe("Data Column Dimension [" + 
						String.valueOf(data.getColumnDimension()) + 
						"] does not match number of fields[" + 
						String.valueOf(fieldNames.length));
			}

		} catch(IOException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} finally {
			
			// close the input stream
			if (inputStream != null) {
				inputStream.close();
			}
		}

	}

	// return the data as an array of doubles
	// return a zero length 2d array of doubles if an error occurs
	public double[][] getData() {
		
		double[][] d = null;
		
		try {
			d = data.getData();
		} catch (NullPointerException e){
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			d = new double[0][0];
		}
		
		return d;
	}
	
	// set the data array
	public void setData(double[][] data) {
		
		try {
			this.data = MatrixUtils.createRealMatrix(data);
		} catch(IllegalArgumentException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// get the fieldNames array
	// in case of an error return a zero length array of strings
	public String[] getFieldNames() {
		
		String[] fNames = null;
		
		try {
			fNames = fieldNames;
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			fNames = new String[0];
		}
		
		return fNames;
	}
	
	// set the fieldNames array
	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}
	
	// get the number of fields
	public int getNumFields() {
		
		int numFields = 0;
		
		try {
			numFields =  data.getColumnDimension();
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			numFields = 0;
		}
		
		return numFields;		
	}
	
	// get the number of rows in the data
	public int getSizeData() {
		
		int size = 0;
		
		try {
			size =  data.getRowDimension();
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			size = 0;
		}
		
		return size;
	}
	
	// get the string for the field at index 'index'
	public String getField(int index ) {
		
		String field = null;
				
		try {
			field = fieldNames[index];
		
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			field = "";
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			field = "";
		}
		
		return field;
	}
	
	// get data item at (i,j)
	public double getDataItem(int i, int j) {
		
		double item = 0;
		
		try {
			item = data.getEntry(i, j);
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			item = 0;
		}
		catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			item = 0;
		}
		
		return item;
	}
	
	// get the data in column j as an array of ints
	public int[] getColumnAsInt(int j) {
		
		int[] cI = null;
		
		try {
			
			// the column  j as an array of doubles
			double[] cD = data.getColumn(j);
			
			// then cast them to ints
			cI = new int[cD.length];
			
			for (int i = 0; i < cD.length; i++) {
				cI[i] = (int)cD[i];
			}
			
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			cI = new int[0];
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			cI = new int[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			cI = new int[0];
		}
		
		return cI;
	}

	// get the data in column j as an array of doubles
	public double[] getColumn(int j) {
		
		double[] c = null;
		
		try {
			c = data.getColumn(j);
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			c = new double[0];
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			c = new double[0];
		}
		
		return c;
	}

	// get the data in row i as an array of doubles
	public double[] getRow(int i) {
			
		double[] r = null;
		
		try {
			r = data.getRow(i);
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			r = new double[0];
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			r = new double[0];
		}
		
		return r;
	}

	// get a submatrix for all rows and columns specified by indices 
	// in cols array
	public double[][] getSubMatrix(int [] cols) {
		
		double[][] submatrix = null;
		try {
			submatrix = data.getSubMatrix(rowIndices,cols).getData();
		} catch (NullPointerException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			submatrix = new double[0][0];
		} catch (MatrixIndexException e) {
			logger.severe(e.toString() + " : " + e.getMessage());
			e.printStackTrace();
			submatrix = new double[0][0];
		}
		
		return submatrix;
	}



}
