package geovista.geoviz.spreadsheet;

/*
 * Description:
 * Date: Feb 9, 2003
 * Time: 12:09:50 PM
 * @author Jin Chen
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DataSetTableModel;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.coordination.CoordinationManager;
import geovista.geoviz.descriptive.DescriptiveStats;
import geovista.readers.example.GeoDataGeneralizedStates;
import geovista.readers.shapefile.ShapeFileProjection;

public class TableViewer extends JPanel implements SelectionListener,
	DataSetListener, MouseListener {

    private DescriptiveStats stats;
    private transient DataSetTableModel dataSet;
    boolean mouseIn;

    // this renders table header cells as buttons (used for statistics display)
    private transient ButtonHeaderRenderer renderer;

    private JTable table;

    JScrollPane scrollPane;
    final static Logger logger = Logger.getLogger(TableViewer.class.getName());

    boolean seeStats = true;

    public TableViewer() {
	// super(new GridLayout(1, 0));
	super(new BorderLayout());
	init();

    }

    public TableViewer(boolean seeStats) {
	super(new BorderLayout());
	this.seeStats = seeStats;
	init();
    }

    /*
     * Set the header renderer for the current table Model The header cells are
     * rendered as JButtons. When a header cell button is pressed, statistics
     * are displayed for the header
     */

    private void setTableHeaderRenderer() {

	JTableHeader header = table.getTableHeader();
	header.setDefaultRenderer(renderer);
	header.addMouseListener(new HeaderListener(renderer, header));

    }

    boolean mouseIsOverDisplayPanel() {

	if (MouseInfo.getPointerInfo().getLocation().x >= getLocationOnScreen().x
		&& MouseInfo.getPointerInfo().getLocation().x <= getLocationOnScreen().x
			+ getWidth()
		&& MouseInfo.getPointerInfo().getLocation().y >= getLocationOnScreen().y
		&& MouseInfo.getPointerInfo().getLocation().y <= getLocationOnScreen().y
			+ getHeight()) {

	    return true;

	}
	return false;
    }

    /*
     * Returns a new JTable object
     */

    private void init() {
	logger.fine("adding self as mouse listener");
	// System.exit(0);
	addMouseListener(this);
	if (dataSet == null) {
	    dataSet = getDefaultData();
	}

	// clear out whatever was there before
	removeAll();

	table = new JTable(dataSet);
	table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	table.setAutoCreateRowSorter(true);
	ListSelectionReporter reporter = new ListSelectionReporter();
	table.getSelectionModel().addListSelectionListener(reporter);
	// table.setColumnSelectionAllowed(true);
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	table.setPreferredScrollableViewportSize(new Dimension(300, 70));
	// table.setFillsViewportHeight(true);

	renderer = new ButtonHeaderRenderer();

	setTableHeaderRenderer();

	// Create the scroll pane and add the table to it.
	scrollPane = new JScrollPane(table,
		ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
		ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

	// Add the scroll pane to this panel.
	add(scrollPane);
	stats = new DescriptiveStats();
	if (seeStats) {
	    add(stats, BorderLayout.SOUTH);
	}
	setPreferredSize(new Dimension(300, 600));
	setVisible(true);
    }

    DataSetTableModel getDefaultData() {
	String[] columnNames = { "First Name", "Age", "Weight" };

	String[] names = { "Frank", "HyangJa", "Hannah", "Lena", "Alex" };
	double[] ages = { 40, 35, 6, 3, 1 };
	double[] weight = { 140, 104, 45, 30, 20 };

	Object[] dataArrays = { columnNames, names, ages, weight };
	DataSetForApps data = new DataSetForApps(dataArrays);
	DataSetTableModel tableModel = new DataSetTableModel(data);
	return tableModel;
    }

    /*
     * private DefaultTableModel getTableModel(){ SSTableModel
     * sstbm=(SSTableModel) SSTableModel.getInstance() ; return sstbm; }
     */
    /***************************************************************************
     * Bean property
     **************************************************************************/
    public void dataSetChanged(DataSetEvent e) {

	dataSet = new DataSetTableModel(e.getDataSetForApps());
	table.setModel(dataSet);
	setTableHeaderRenderer();
	stats.dataSetChanged(e);

    }

    public void selectionChanged(SelectionEvent e) {

	int[] selVals = e.getSelection();
	for (int i : selVals) {
	    if (i > table.getRowCount()) {
		return;
	    }
	}
	table.clearSelection();
	for (int i : selVals) {

	    // if we have sorted the table we need to convert the indices
	    // to the new view indices
	    int v = table.getRowSorter().convertRowIndexToView(i);
	    table.addRowSelectionInterval(v, v);
	}
	// this.setSelectedIndex(e.getSelection());
	stats.selectionChanged(e);

    }

    public SelectionEvent getSelectionEvent() {
	return new SelectionEvent(this, stats.getSelectionEvent()
		.getSelection());
    }

    /**
     * Notify all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * parameters passed into the fire method.
     * 
     * @see EventListenerList
     */
    // @SuppressWarnings("unused")
    private void fireSelectionChanged(int[] newSelection) {

	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	SelectionEvent e = null;
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
	    if (listeners[i] == SelectionListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new SelectionEvent(this, newSelection);
		}
		((SelectionListener) listeners[i + 1]).selectionChanged(e);
	    }
	}// next i

    }

    /**
     * adds an SelectionListener
     */
    public void addSelectionListener(SelectionListener l) {
	listenerList.add(SelectionListener.class, l);
    }

    /**
     * removes an SelectionListener from the component
     */
    public void removeSelectionListener(SelectionListener l) {
	listenerList.remove(SelectionListener.class, l);

    }

    /***************************************************************************
     * GeoVista Function/interface
     **************************************************************************/
    /*
     * for use with coordinator
     */
    public void setDataSet(DataSetForApps dataSet) {
	this.dataSet = new DataSetTableModel(dataSet);

	init();

	/*
	 * 
	 * String[] attributeNames = dataSet.getAttributeNamesOriginal(); int
	 * numObs = dataSet.getNumObservations(); Object[][] tablesData = new
	 * Object[numObs][attributeNames.length];
	 * 
	 * double[] doubleData = null; int[] intData = null; String[] stringData
	 * = null; boolean[] boolData = null;
	 * 
	 * for (int column = 0; column < attributeNames.length; column++) { //
	 * Object datum = data[column+1]; Object datum =
	 * dataSet.getNamedArrays()[column];// XXX is this the // right
	 * accessor? if (datum instanceof double[]) { doubleData = (double[])
	 * datum; for (int row = 0; row < numObs; row++) { double dat =
	 * doubleData[row]; Double d = new Double(dat); tablesData[row][column]
	 * = d; } }
	 * 
	 * if (datum instanceof int[]) { intData = (int[]) datum; for (int row =
	 * 0; row < numObs; row++) { int dat = intData[row]; Integer d = new
	 * Integer(dat); tablesData[row][column] = d; } }
	 * 
	 * if (datum instanceof String[]) {
	 * 
	 * stringData = (String[]) datum; for (int row = 0; row < numObs; row++)
	 * { String dat = stringData[row]; tablesData[row][column] = dat; } }
	 * 
	 * if (datum instanceof boolean[]) { boolData = (boolean[]) datum; for
	 * (int row = 0; row < numObs; row++) { boolean dat = boolData[row];
	 * Boolean d = new Boolean(dat); tablesData[row][column] = d; } } }
	 * 
	 * this.setData(tablesData, attributeNames);
	 */
    }

    /*
     * This class acts as a listener for mouse click events on the header cells.
     * when a header cell is clicked the corresponding button is depressed and
     * all other header cell buttons are set to be "not pressed". Statistics are
     * displayed for the column with the pressed button
     */

    private class HeaderListener extends MouseAdapter {

	ButtonHeaderRenderer renderer;
	JTableHeader header;

	public HeaderListener(ButtonHeaderRenderer renderer, JTableHeader header) {
	    this.renderer = renderer;
	    this.header = header;
	}

	@Override
	public void mousePressed(MouseEvent e) {
	    int pushedColumn = header.columnAtPoint(e.getPoint());
	    renderer.setPressedColumn(pushedColumn);
	    stats.setSelectedColumn(pushedColumn);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
    }

    /*
     * Subclass JButton to create header cells that act like buttons When a cell
     * header for a column is pressed statistics for that column are displayed.
     * Only one button can be pressed at at time
     */
    private class ButtonHeaderRenderer extends JButton implements
	    TableCellRenderer {

	int pushedColumn = 0;

	public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus, int row,
		int column) {
	    setText((value == null) ? "" : value.toString());
	    boolean isPressed = (column == pushedColumn);
	    getModel().setPressed(isPressed);
	    getModel().setArmed(isPressed);
	    return this;
	}

	public void setPressedColumn(int col) {
	    pushedColumn = col;
	}
    }

    /***************************************************************************
     * Test
     **************************************************************************/
    public static void main(String args[]) {

	JFrame mf = new JFrame();
	TableViewer tView = new TableViewer(false);
	mf.getContentPane().add(tView);

	DataSetForApps dataSet = getStateData(tView);
	// tView.setDataSet(dataSet);
	mf.pack();
	mf.setVisible(true);

    }

    private static DataSetForApps getStateData(TableViewer tView) {
	CoordinationManager coord = new CoordinationManager();
	ShapeFileProjection shpProj = new ShapeFileProjection();
	GeoDataGeneralizedStates stateData = new GeoDataGeneralizedStates();
	coord.addBean(tView);
	coord.addBean(shpProj);
	shpProj.setInputDataSet(stateData.getDataSet());

	DataSetForApps dataSet = shpProj.getOutputDataSetForApps();
	// for (int row = 0; row < dataSet.getRowCount(); row++) {
	// for (int column = 0; column < dataSet.getColumnCount(); column++) {
	// Object obj = dataSet.getValueAt(row, column);
	// logger.info("row = " + row + ", col = " + column + ", obj = "
	// + obj);
	// }
	// }
	return dataSet;
    }

    class ListSelectionReporter implements ListSelectionListener {

	public void valueChanged(ListSelectionEvent e) {

	    // only fire new selections when they originate from the JTable
	    // selections from other beans will also cause the valueChanged
	    // method to be
	    // called via table.addRowSelectionInterval method which fires a
	    // ListSelectionEvent

	    if (!e.getValueIsAdjusting() && table.hasFocus()
		    && mouseIsOverDisplayPanel()) {

		/*
		 * logger.info("**********"); logger.info("");
		 * logger.info(e.toString()); logger.info("" +
		 * e.getFirstIndex()); logger.info("" + e.getLastIndex());
		 * 
		 * logger.info("*****"); logger.info("Selected column is : " +
		 * table.getSelectedColumn());
		 */

		// if the table has been sorted we need to convert the view
		// based indices
		// to the model indices
		int[] selRowsView = table.getSelectedRows();
		int[] selRowsModel = new int[selRowsView.length];
		RowSorter rowSort = table.getRowSorter();
		for (int i = 0; i < selRowsView.length; i++) {
		    selRowsModel[i] = rowSort
			    .convertRowIndexToModel(selRowsView[i]);
		}

		// send the newly selected observations to the other beans
		fireSelectionChanged(selRowsModel);

		// compute the statistics for the changed selection
		stats.selectionChanged(new SelectionEvent(this, selRowsModel));
	    }

	}
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
	// TODO Auto-generated method stub

    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
	logger.info("");
	mouseIn = true;

    }

    @Override
    public void mouseExited(MouseEvent arg0) {
	logger.info("");
	mouseIn = false;

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
