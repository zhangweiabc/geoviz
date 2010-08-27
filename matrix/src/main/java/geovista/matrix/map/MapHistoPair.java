/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.matrix.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.data.SpatialWeights;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.PaletteEvent;
import geovista.common.event.PaletteListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SpatialExtentEvent;
import geovista.common.event.SpatialExtentListener;
import geovista.coordination.CoordinationManager;
import geovista.geoviz.map.GeoMapUni;
import geovista.geoviz.scatterplot.SingleHistogram;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.symbolization.event.ColorClassifierEvent;
import geovista.symbolization.event.ColorClassifierListener;

/**
 * A univariate map and a histogram tied together. Should be refactored to have
 * a single set of toolbar controls.
 */
public class MapHistoPair extends JPanel implements IndicationListener,
		DataSetListener, ColorClassifierListener, SpatialExtentListener,
		PaletteListener, TableModelListener, ActionListener,
		ListSelectionListener, MouseListener {
	protected final static Logger logger = Logger.getLogger(MapHistoPair.class
			.getName());

	static boolean useMap = true;

	private final GeoMapUni map;

	private final SingleHistogram histo;

	DataSetForApps dataSetOriginal;

	SpatialWeights spatialWeights;

	int monteCarloIterations = 1000;

	CoordinationManager coord;

	public MapHistoPair() {

		super();
		addMouseListener(this);
		map = new GeoMapUni();

		map.removeLegendPanel();

		histo = new SingleHistogram();
		histo.removeBottomPanel();
		coord = new CoordinationManager();

		coord.addBean(map);

		coord.addBean(histo);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(2, 1));
		mainPanel.add(map);

		mainPanel.add(histo);

		// BoxLayout box = new BoxLayout(this, BoxLayout.X_AXIS);
		// varSigMap = new GeoMap();
		// sp = new SingleScatterPlot();
		// varSigMap.addSelectionListener(sp);
		// sp.addSelectionListener(varSigMap);
		// varSigMap.addIndicationListener(sp);
		// sp.addIndicationListener(varSigMap);
		// setLayout(box);
		Dimension prefSize = new Dimension(100, 100);

		histo.setMaximumSize(prefSize);
		// LineBorder border = (LineBorder) BorderFactory
		// .createLineBorder(Color.black);
		// varSigMap.setBorder(border);
		// sp.setBorder(border);
		setLayout(new BorderLayout());
		this.add(mainPanel);
		// this.add(sp);

	}

	void setSelectedVar(int var) {
		if (useMap) {
			map.setSelectedVariable(var);
		}
		histo.setSelectedVariable(var);
	}

	public void selectionChanged(SelectionEvent e) {
		map.selectionChanged(e);
		if (histo != null) {
			histo.selectionChanged(e);
		}

	}

	public void setSelectionStatus(boolean isSelected) {
		if (isSelected) {
			setBorder(BorderFactory.createLineBorder(Color.red, 5));
		}
	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, map.getSelectedObservations());
	}

	public void indicationChanged(IndicationEvent e) {
		map.indicationChanged(e);

		histo.indicationChanged(e);

	}

	public void dataSetChanged(DataSetEvent e) {
		if (useMap) {
			map.dataSetChanged(e);
		}
		histo.dataSetChanged(e);
	}

	public void colorClassifierChanged(ColorClassifierEvent e) {
		// TODO Auto-generated method stub

	}

	SpatialExtentEvent savedEvent;

	public SpatialExtentEvent getSpatialExtentEvent() {
		return savedEvent;
	}

	public void spatialExtentChanged(SpatialExtentEvent e) {
		savedEvent = e;
		// TODO Auto-generated method stub

	}

	public void paletteChanged(PaletteEvent e) {
		// TODO Auto-generated method stub

	}

	public void tableChanged(TableModelEvent e) {
		// TODO Auto-generated method stub

	}

	public void actionPerformed(ActionEvent e) {

	}

	public static void main(String[] args) {

		MapHistoPair masto = new MapHistoPair();
		double[] vals = { 0, 1, 2, 3 };
		double along = DescriptiveStatistics.percentAbove(vals, 2.9);
		logger.info("" + along);

		JFrame frame = new JFrame("MapHistoPair");
		frame.add(masto);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GeoDataGeneralizedStates geodata = new GeoDataGeneralizedStates();
		DataSetEvent e = new DataSetEvent(geodata.getDataForApps(), geodata);
		masto.dataSetChanged(e);
		masto.setSelectionStatus(true);

	}

	public void valueChanged(ListSelectionEvent e) {
		if (dataSetOriginal == null) {
			return;
		}

	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		logger.info("clicked");

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

}
