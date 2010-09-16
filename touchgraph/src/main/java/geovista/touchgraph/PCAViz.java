/*
 * Frank Hardisty
 */
package geovista.touchgraph;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import cern.jet.math.Functions;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.SubspaceEvent;
import geovista.common.event.SubspaceListener;
import geovista.common.ui.VariablePicker;
import geovista.readers.example.GeoDataGeneralizedStates;

/**
 *
 */
public class PCAViz extends JPanel implements DataSetListener,
		SubspaceListener, ActionListener {

	PCAGraph graph;

	VariablePicker picker;

	final static Logger logger = Logger.getLogger(PCAViz.class.getName());

	DataSetForApps dataSetOriginal;

	DataSetForApps dataSet;

	DataSetForApps newData;

	int nFactors;

	/**
	 * Default constructor.
	 */
	public PCAViz() {
		setLayout(new BorderLayout());
		graph = new PCAGraph();
		graph.sendButton.addActionListener(this);
		picker = new VariablePicker();
		picker.setPreferredSize(new Dimension(100, 400));
		picker.setBorder(BorderFactory.createTitledBorder("Pick Variables"));
		graph.setBorder(BorderFactory.createTitledBorder("Visualize Results"));
		this.add(picker, BorderLayout.WEST);
		this.add(graph, BorderLayout.CENTER);
		picker.addSubspaceListener(this);

	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == graph.sendButton && nFactors > 0) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("PCAViz, asking data set to fire");
			}

		}
	}

	public void dataSetChanged(DataSetEvent e) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("PCAViz, got a data set, id = "
					+ e.getDataSetForApps().hashCode());
		}
		// we need to keep a reference to the original
		dataSetOriginal = e.getDataSetForApps();

		picker.dataSetChanged(e);
		dataSet = e.getDataSetForApps();
		// let's do an initial variable selection so the GUI has something to
		// show
		int nVars = dataSet.getNumberNumericAttributes();
		int nPCAVars = 0;
		if (nVars < 6) {
			nPCAVars = nVars;
		} else {
			nPCAVars = 6;
		}
		int[] selVars = new int[nPCAVars];
		for (int i = 0; i < nPCAVars; i++) {
			selVars[i] = i;
		}
		SubspaceEvent eSub = new SubspaceEvent(this, selVars);
		subspaceChanged(eSub);

	}

	/**
	 * adds an SubspaceListener
	 */
	public void addSubspaceListener(SubspaceListener l) {
		listenerList.add(SubspaceListener.class, l);

	}

	/**
	 * removes an SubspaceListener from the component
	 */
	public void removeSubspaceListener(SubspaceListener l) {
		listenerList.remove(SubspaceListener.class, l);

	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireSubspaceChanged(int[] selection) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		SubspaceEvent e = null;

		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == SubspaceListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new SubspaceEvent(this, selection);
				}

				((SubspaceListener) listeners[i + 1]).subspaceChanged(e);
			}
		}
		// next i
	}

	public void subspaceChanged(SubspaceEvent e) {

		// we do a pca analysis here....
		int[] subspace = e.getSubspace();
		if (subspace.length < 1) {
			return;
		}

		int nFactors = 3;
		DataSetForApps results = doPCA(dataSet, subspace, nFactors);
		newData = results;
		// here we assemble a list of nodes and their distances.
		// we want from (factor) to (original variable) for each original
		// variable

		String[] oldNames = dataSet.getAttributeNamesNumeric();
		String[] newNames = results.getAttributeNamesNumeric();

		String[] allNames = new String[subspace.length + newNames.length];
		for (int i = 0; i < allNames.length; i++) {
			if (i < subspace.length) {
				allNames[i] = oldNames[subspace[i]];
			} else {
				allNames[i] = newNames[i - subspace.length];
			}

		}

		Object[] newData = results.getDataSetNumeric();

		Object[] allData = new Object[subspace.length + newData.length + 1];
		allData[0] = allNames;
		int counter = 0;
		for (int i = 0; i < allData.length - 1; i++) {
			if (i < subspace.length) {
				allData[i + 1] = dataSet.getNumericDataAsDouble(subspace[i]);
			} else {
				allData[i + 1] = newData[counter];
				counter++;
			}

		}
		// XXX looking for an alternative to this cntr
		DataSetForApps newDataSet = new DataSetForApps(allData);
		graph.setDataSet(newDataSet, nFactors);
		this.nFactors = nFactors;

	}

	DoubleMatrix2D replaceNaN(DoubleMatrix2D matrix, double newVal) {
		DoubleMatrix2D newMatrix = matrix.copy();

		int rows = newMatrix.rows();
		int columns = newMatrix.columns();

		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				double val = newMatrix.get(row, column);
				if (Double.isNaN(val)) {
					newMatrix.set(row, column, newVal);
				}
			}
		}

		return newMatrix;

	}

	public DoubleMatrix2D getPCAmatrix(DoubleMatrix2D matrix, int pcn) {
		Algebra alg = new Algebra();
		DoubleFactory2D factory = DoubleFactory2D.dense;
		int row = matrix.rows();
		int col = matrix.columns();
		for (int i = 0; i < col; i++) {
			double mean = matrix.viewColumn(i).zSum() / row;

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("mean = " + mean);
			}
			matrix.viewColumn(i).assign(Functions.minus(mean));

			double norm = Math.sqrt(alg.norm2(matrix.viewColumn(i)));
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("norm = " + norm);
			}
			// replace NaN with zero
			matrix = replaceNaN(matrix, 0);

			matrix.viewColumn(i).assign(Functions.div(norm));
		}

		SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("svd " + svd);
		}
		DoubleMatrix2D v = svd.getV();
		DoubleMatrix2D vn = factory.make(v.rows(), pcn);
		for (int i = 0; i < pcn; i++) {
			vn.viewColumn(i).assign(v.viewColumn(i));
		}
		return alg.transpose(svd.getU());
	}

	private DataSetForApps doPCA(DataSetForApps data, int[] subspace,
			int nFactors) {

		if (data == null || subspace == null
				|| subspace.length > data.getNumberNumericAttributes()) {
			return null;
		}

		int nVars = subspace.length;
		int nObs = data.getNumObservations();
		DoubleMatrix2D matrix = DoubleFactory2D.dense.make(nObs, nVars);

		double[][] data1 = new double[nObs][nVars];

		for (int obs = 0; obs < nObs; obs++) {
			for (int var = 0; var < nVars; var++) {
				data1[obs][var] = data.getNumericValueAsDouble(var, obs);
			}
		}

		matrix.assign(data1);

		DoubleMatrix2D results = getPCAmatrix(matrix, nFactors);
		double[][] pcaFactors = results.toArray();
		String[] factorNames = new String[nFactors];
		for (int i = 0; i < nFactors; i++) {
			factorNames[i] = new String("PCA Factor " + (i + 1));
		}
		Object[] resultData = new Object[nFactors + 1];
		resultData[0] = factorNames;
		for (int i = 0; i < nFactors; i++) {
			resultData[i + 1] = pcaFactors[i];
		}
		// XXX not supposed to use constructor here
		DataSetForApps resultDataSet = new DataSetForApps(resultData);
		return resultDataSet;
	}

	public static void main(String[] args) {
		// try {
		// UIManager.setLookAndFeel(new SubstanceLookAndFeel());
		// } catch (UnsupportedLookAndFeelException ex) {
		// ex.printStackTrace();
		// }
		Logger logger = Logger.getLogger("geovista.touchgraph");
		logger.setLevel(Level.FINEST);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINEST);
		logger.addHandler(handler);
		logger.finest("java.version = " + System.getProperty("java.version")
				+ ", Runtime.avaialableProcessors = "
				+ Runtime.getRuntime().availableProcessors());
		JFrame frame;
		frame = new JFrame("Graph Layout");

		PCAViz glPanel = new PCAViz();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.getContentPane().add("Center", glPanel);
		frame.setSize(500, 500);
		frame.setVisible(true);

		GeoDataGeneralizedStates data = new GeoDataGeneralizedStates();

		// GeoDataSCarolina data = new GeoDataSCarolina();
		DataSetEvent e = new DataSetEvent(data.getDataForApps(), frame);
		glPanel.dataSetChanged(e);
		int[] subspace = { 1, 2, 3 };
		glPanel.subspaceChanged(new SubspaceEvent(frame, subspace));

	}
}
