package geovista.network.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;

/**
 * This class is an implementation of CONCOR algorithm, designed for course
 * project of GEOG 597D
 * 
 * @author peifeng
 * @version 1.0
 */
public class Concor {

	protected Matrix matrix;
	protected String outputDir;
	protected Matrix[] matrices;

	public static final double EPSILON = 0.00000000001;

	/**
	 * The constructor
	 */
	public Concor() {
		matrix = new Matrix();
		outputDir = "temp/";
		matrices = null;
	}

	/**
	 * This method loads the original matrix data from disk file
	 * 
	 * @Note The first line of the file should be the dimension of the matrix
	 * @param file
	 */
	public void loadData(String file) {
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					file)));
			String[] dim = (br.readLine()).split(" "); // the first line tells
														// the dimension
			int row = Integer.parseInt(dim[0]);
			int column = Integer.parseInt(dim[1]);
			matrix.setDimension(row, column);
			for (int i = 0; i < row; i++) {
				String s = br.readLine();
				if (s == null) {
					System.err
							.println("matrix error: the number of rows does not agree!");
					br.close();
					System.exit(1);
				}
				String[] data = (s.split(" "));
				if (data.length > column) {
					System.err.println("matrix error on the " + (i + 1)
							+ " row of data. Dimension does not agree!");
					br.close();
					System.exit(1);
				}
				for (int j = 0; j < column; j++)
					matrix.set(i, j, Double.parseDouble(data[j]));
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Load multiple files
	 * 
	 * @param files
	 */
	public void loadData(String[] files) {
		if (files.length == 1) {
			loadData(files[0]);
			return;
		}
		matrices = new Matrix[files.length];
		for (int i = 0; i < files.length; i++) {
			loadData(files[i]);
			matrices[i] = matrix.clone();
		}
	}

	public void setOutputDirectory(String dir) {
		outputDir = dir;
	}

	/**
	 * This method starts CONCOR algorithm
	 * 
	 * @Note the matrix should be square.
	 * @param iteration
	 *            the number of iterations; if it's equal to
	 *            "Integer.MAX_VALUE", the process won't stop until the matrix
	 *            converges.
	 * @param output
	 *            whether the mediate results are saved. If true, the results
	 *            will be saved in outputDir, which can be set by calling
	 *            setOutputDirectory()
	 * @return the output of the algorithm.
	 */
	public double[][] concor(int iteration, boolean output) {
		if (output) {
			File f = new File(outputDir);
			if (!f.exists())
				f.mkdirs();
		}
		double epsilon = EPSILON;
		boolean isConverge = false;
		int count = 0;
		int dim = Math.min(matrix.rows(), matrix.columns());
		Matrix mediateResult = new Matrix();
		mediateResult.setDimension(dim, dim);
		Matrix temp;
		if (matrices == null) // only one network
			temp = matrix.clone();
		else
			temp = FirstIteration();
		while (!isConverge && count < iteration) {
			isConverge = true;
			count++;

			double[] rowMean = ComputeRowMean(temp);
			double[] colMean = ComputeColMean(temp);
			double[] rowVarN = ComputeRowVarN(temp, rowMean);
			double[] colVarN = ComputeColVarN(temp, colMean);
			double[] devN = new double[dim];
			for (int i = 0; i < dim; i++) {
				devN[i] = Math.sqrt(rowVarN[i] + colVarN[i]);
				//devN[i] = Math.max(devN[i], EPSILON);
			}

			for (int i = 0; i < dim; i++) {
				mediateResult.setQuick(i, i, 1);
				for (int j = i + 1; j < dim; j++) {
					double rij = PearsonProduct(temp, i, j, rowMean, colMean,
							devN);
					mediateResult.setQuick(i, j, rij);
					mediateResult.setQuick(j, i, rij);
					/**
					 * If the difference is smaller than a given small value, we
					 * treat it as equal.
					 */
					if (Math.abs(Math.abs(rij) - 1) > epsilon)
						isConverge = false;
				}
			}

			if (output) {
				String file = outputDir + "iterate" + count + ".txt";
				Output(mediateResult, file);
			}

			Matrix temp2 = temp;
			temp = mediateResult;
			mediateResult = temp2;
		}
		return temp.values;
	}

	/**
	 * This method finishes the first iteration process for multi-relation
	 * network.
	 * 
	 * @return the result matrix for first iteration
	 */
	protected Matrix FirstIteration() {
		int dim = Math.min(matrices[0].rows(), matrices[0].columns());
		Matrix mediateResult = new Matrix();
		mediateResult.setDimension(dim, dim);
		double[] rowMean = ComputeRowMean(matrices);
		double[] colMean = ComputeColMean(matrices);
		double[] rowVarN = ComputeRowVarN(matrices, rowMean);
		double[] colVarN = ComputeColVarN(matrices, colMean);
		double[] devN = new double[dim];
		for (int i = 0; i < dim; i++) {
			devN[i] = Math.sqrt(rowVarN[i] + colVarN[i]);
			devN[i] = Math.max(devN[i], EPSILON);
		}

		for (int i = 0; i < dim; i++) {
			mediateResult.setQuick(i, i, 1);
			for (int j = i + 1; j < dim; j++) {
				double rij = PearsonProduct(matrices, i, j, rowMean, colMean,
						devN);
				mediateResult.setQuick(i, j, rij);
				mediateResult.setQuick(j, i, rij);
			}
		}
		return mediateResult;
	}

	protected double[] ComputeRowMean(Matrix[] matrices) {
		double[][] rowmeans = new double[matrices.length][matrices[0].rows()];
		for (int i = 0; i < rowmeans.length; i++)
			rowmeans[i] = ComputeRowMean(matrices[i]);
		double[] rowmean = new double[rowmeans[0].length];
		for (int i = 0; i < rowmean.length; i++) {
			double sum = 0;
			for (int j = 0; j < rowmeans.length; j++)
				sum += rowmeans[j][i];
			rowmean[i] = sum / rowmeans.length;
		}
		return rowmean;
	}

	protected double[] ComputeColMean(Matrix[] matrices) {
		double[][] colmeans = new double[matrices.length][matrices[0].columns()];
		for (int i = 0; i < colmeans.length; i++)
			colmeans[i] = ComputeColMean(matrices[i]);
		double[] colmean = new double[colmeans[0].length];
		for (int i = 0; i < colmean.length; i++) {
			double sum = 0;
			for (int j = 0; j < colmeans.length; j++)
				sum += colmeans[j][i];
			colmean[i] = sum / colmeans.length;
		}
		return colmean;
	}

	protected double[] ComputeRowVarN(Matrix[] matrices, double[] rowMean) {
		double[][] rowvarns = new double[matrices.length][matrices[0].rows()];
		for (int i = 0; i < rowvarns.length; i++)
			rowvarns[i] = ComputeRowVarN(matrices[i], rowMean);
		double[] rowvarn = new double[rowvarns[0].length];
		for (int i = 0; i < rowvarn.length; i++) {
			double sum = 0;
			for (int j = 0; j < rowvarns.length; j++)
				sum += rowvarns[j][i];
			rowvarn[i] = sum;
		}
		return rowvarn;
	}

	protected double[] ComputeColVarN(Matrix[] matrices, double[] colMean) {
		double[][] colvarns = new double[matrices.length][matrices[0].columns()];
		for (int i = 0; i < colvarns.length; i++)
			colvarns[i] = ComputeColVarN(matrices[i], colMean);
		double[] colvarn = new double[colvarns[0].length];
		for (int i = 0; i < colvarn.length; i++) {
			double sum = 0;
			for (int j = 0; j < colvarns.length; j++)
				sum += colvarns[j][i];
			colvarn[i] = sum;
		}
		return colvarn;
	}

	/**
	 * The overload of concor.
	 * 
	 * @param matrix
	 *            the matrix data that is to be analyzed
	 * @param iteration
	 * @param output
	 * @return the output of the algorithm
	 */
	public double[][] concor(double[][] matrix, int iteration, boolean output) {
		this.matrix = new Matrix(matrix);
		return concor(iteration, output);
	}

	public double[][] concor(double[][][] matrix, int iteration, boolean output) {
		if (matrix.length == 1)
			return concor(matrix[0], iteration, output);
		matrices = new Matrix[matrix.length];
		for (int i = 0; i < matrix.length; i++)
			matrices[i] = new Matrix(matrix[i]);
		this.matrix = matrices[0];
		return concor(iteration, output);
	}

	protected double[] ComputeRowMean(Matrix m) {
		double[] rowMean = new double[m.rows()];
		for (int i = 0; i < rowMean.length; i++)
			rowMean[i] = m.rowMean(i);
		return rowMean;
	}

	protected double[] ComputeColMean(Matrix m) {
		double[] colMean = new double[m.columns()];
		for (int i = 0; i < colMean.length; i++)
			colMean[i] = m.colMean(i);
		return colMean;
	}

	protected double[] ComputeRowVarN(Matrix m, double[] rowMean) {
		double[] rowVarN = new double[rowMean.length];
		for (int i = 0; i < rowMean.length; i++)
			rowVarN[i] = m.rowVarN(i, rowMean[i]);
		return rowVarN;
	}

	protected double[] ComputeColVarN(Matrix m, double[] colMean) {
		double[] colVarN = new double[colMean.length];
		for (int i = 0; i < colMean.length; i++)
			colVarN[i] = m.colVarN(i, colMean[i]);
		return colVarN;
	}

	protected double PearsonProduct(Matrix[] matrices, int i, int j,
			double[] rowMean, double[] colMean, double[] devN) {
		DoubleMatrix2D xi, yj;
		double sum = 0;

		for (int k = 0; k < matrices.length; k++) {
			xi = matrices[k].subMatrix(i, 0, 1, matrices[k].columns()).assign(
					Functions.minus(rowMean[i]));
			yj = matrices[k].subMatrix(j, 0, 1, matrices[k].columns()).assign(
					Functions.minus(rowMean[j]));

			sum += xi.aggregate(yj, Functions.plus, Functions.mult);

			xi = matrices[k].subMatrix(0, i, matrices[k].rows(), 1).assign(
					Functions.minus(colMean[i]));
			yj = matrices[k].subMatrix(0, j, matrices[k].rows(), 1).assign(
					Functions.minus(colMean[j]));
			sum += xi.aggregate(yj, Functions.plus, Functions.mult);
		}
		if(devN[i]==0 && devN[j]==0)
			return 1;
		else if(devN[i]*devN[j]==0)
			return 0;
		sum /= (devN[i] * devN[j]);
		return sum;
	}

	protected double PearsonProduct(Matrix temp, int i, int j,
			double[] rowMean, double[] colMean, double[] devN) {
		DoubleMatrix2D xi, yj;
		double sum = 0;

		xi = temp.subMatrix(i, 0, 1, temp.columns()).assign(
				Functions.minus(rowMean[i]));
		yj = temp.subMatrix(j, 0, 1, temp.columns()).assign(
				Functions.minus(rowMean[j]));

		sum += xi.aggregate(yj, Functions.plus, Functions.mult);

		xi = temp.subMatrix(0, i, temp.rows(), 1).assign(
				Functions.minus(colMean[i]));
		yj = temp.subMatrix(0, j, temp.rows(), 1).assign(
				Functions.minus(colMean[j]));
		sum += xi.aggregate(yj, Functions.plus, Functions.mult);

		if(devN[i]==0 && devN[j]==0)
			return 1;
		else if(devN[i]*devN[j]==0)
			return 0;
		sum /= (devN[i] * devN[j]);
		return sum;
	}

	/**
	 * This method writes a matrix to the given file.
	 * 
	 * @Note the format is dimension for the first line and each row's values
	 *       for the following lines.
	 * @param m
	 * @param file
	 */
	protected void Output(Matrix m, String file) {
		try {
			PrintWriter pw = new PrintWriter(file);
			pw.println(m.rows() + " " + m.columns());
			for (int i = 1; i <= m.rows(); i++) {
				for (int j = 1; j <= m.columns(); j++)
					pw.print(m.getQuick(i, j) + " ");
				pw.println();
			}
			pw.flush();
			pw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
