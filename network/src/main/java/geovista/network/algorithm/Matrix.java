package geovista.network.algorithm;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;

public class Matrix extends DoubleMatrix2D{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected double[][] values;
	
	public Matrix(){
		values=null;
	}
	
	public Matrix(double[][] data){
		values=data.clone();
		super.rows=data.length;
		super.columns=data.length>0?data[0].length:0;
		super.columnStride=super.columns;
		super.columnStride=super.rowStride;
	}
	
	public void setDimension(int row, int column){
		values=new double[row][column];
		super.rows=row;
		super.columns=column;
	}
	
	@Override
	public double getQuick(int row, int column) {
		if(values.length<=row || (values.length>0 && values[0].length<=column))
			return Double.NaN;
		return values[row][column];
	}

	@Override
	public DoubleMatrix2D like(int rows, int columns) {
		return new Matrix(values);
	}

	@Override
	public DoubleMatrix1D like1D(int size) {
		return null;
	}

	@Override
	protected DoubleMatrix1D like1D(int size, int zero, int stride) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setQuick(int row, int column, double value) {
		if(values==null || values.length<=row 
				|| values.length>0 && values[0].length<=column)
			return;
		values[row][column]=value;
	}

	@Override
	protected DoubleMatrix2D viewSelectionLike(int[] rowOffsets,
			int[] columnOffsets) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public int columns(){
		if(values==null || values.length<=0)
			return 0;
		return values[0].length;
	}
	
	public Matrix clone(){
		Matrix temp=new Matrix(values);
		return temp;
	}

/**
 * This method computes mean value of designated row
 * @param row
 * @return
 */
	public double rowMean(int row){
		//DoubleMatrix2D m=this.subMatrix(row, 0, 1, super.columns);
		//return m.aggregate(Functions.plus, Functions.identity)/super.columns;
		double sum=0;
		for(int i=0; i<columns(); i++)
			sum+=values[row][i];
		return sum/columns();
	}
	
	public double colMean(int col){
		//DoubleMatrix2D m=this.subMatrix(0, col, super.rows, 1);
		//return m.aggregate(Functions.plus, Functions.identity)/super.rows;
		double sum=0;
		for(int i=0; i<rows(); i++)
			sum+=values[i][col];
		return sum/rows();
	}
	
/**
 * Compute the given row's variance*N (where N is the number of columns)
 * @param row
 * @return
 */
	public double rowVarN(int row, double mean){
		//DoubleMatrix2D m=this.viewPart(row, 0, 1, super.columns);
		//return m.aggregate(Functions.plus, Functions.chain(Functions.square, Functions.minus(mean)));
		double sum=0;
		for(int i=0; i<columns(); i++){
			sum+=Math.pow(values[row][i]-mean, 2);
		}
		return sum;
	}
	
	public double colVarN(int col, double mean){
		//DoubleMatrix2D m=this.viewPart(0, col, super.rows, 1);
		//return m.aggregate(Functions.plus, Functions.chain(Functions.square, Functions.minus(mean)));
		double sum=0;
		for(int i=0; i<rows(); i++){
			sum+=Math.pow(values[i][col]-mean, 2);
		}
		return sum;
	}
	
/**
 * gets a copy of designated submatrix.
 * @param row
 * @param column
 * @param height
 * @param width
 * @return
 */
	public Matrix subMatrix(int row, int column, int height, int width){
		double[][] v=new double[height][width];
		for(int i=row; i<row+height; i++)
			for(int j=column; j<column+width; j++)
				v[i-row][j-column]=values[i][j];
		Matrix subm=new Matrix();
		subm.values=v;
		subm.rows=height;
		subm.columns=width;
		return subm;
	}
}
