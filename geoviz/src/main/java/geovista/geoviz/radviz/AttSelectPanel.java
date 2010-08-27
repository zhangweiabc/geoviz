package geovista.geoviz.radviz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.table.AbstractTableModel;

/**
 * <p>
 * Title: AttSelectPanel
 * </p>
 * <p>
 * Description: This panel provides a generic table-based dialog GUI for
 * selecting and ordering attributes represented by an list of attribute names.
 * It is intended to be used by RadViz GUI and GUI components in the
 * geovista.jdm.gui package
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company: GeoVISTA
 * </p>
 * 
 * @author Gary L.
 * 
 * @extends JPanel
 * @implements ActionListener
 */
public class AttSelectPanel extends JPanel implements ActionListener {
	final static Logger logger = Logger.getLogger(AttSelectPanel.class
			.getName());
	public static final String[] COLUMN_NAMES = { "Attribute", "Selected" };

	private int[] selection = new int[0];

	protected EventListenerList ell = new EventListenerList();
	protected HashMap attIdxMap = new HashMap();
	protected JTable table = new JTable();

	/**
	 * Default constructor
	 */
	public AttSelectPanel() {
		this(true);
	}

	/**
	 * Initialize the AttSelectPanel with an empty attribute table. Depending on
	 * if the paramet "showOrderingButtons" evaluates to true or false, the
	 * toolbar in AttSelectPanel will or will not contain these four buttons
	 * dealing with ordering attributes: MoveUp, MoveTop, MoveDown and
	 * MoveBottom
	 * 
	 * @param showOrderingButtons
	 *            a boolean deciding if to add ordering buttons
	 */
	public AttSelectPanel(boolean showOrderingButtons) {
		super(new BorderLayout());

		// Initialize the table
		Object[][] data = new Object[0][0];
		makeTable(data);

		// Create the scroll pane and add the table to it.
		JScrollPane scrollPane = new JScrollPane(table);

		// Create the top toolbar and add the tool buttons to it.
		JToolBar toolBar = makeToolBar(showOrderingButtons);

		// Create the bottom panel containing Ok and Cancel buttons
		JPanel btnPane = makeBtnPane();

		// Add the toolbar, the scroll pane, and the bottom buttons to this
		// panel
		add(toolBar, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
		add(btnPane, BorderLayout.PAGE_END);
	}

	/**
	 * Initialize the AttSelectPanel with an attribute table constructed from
	 * the parameter "attList". The parameter "selectedIdx" contains indices of
	 * initially selected attributes in "attList". Depending on if the paramet
	 * "showOrderingButtons" evaluates to true or false, the toolbar in
	 * AttSelectPanel will or will not contain these four buttons dealing with
	 * ordering attributes: MoveUp, MoveTop, MoveDown and MoveBottom
	 * 
	 * @param attList
	 *            the list of attribute names
	 * @param selectedIdx
	 *            indices of selected attributes in "attList"
	 * @param showOrderingButtons
	 *            a boolean deciding if to add ordering buttons
	 */
	public AttSelectPanel(String[] attList, int[] selectedIdx,
			boolean showOrderingButtons) {
		this(showOrderingButtons);
		setAttList(attList, selectedIdx);
	}

	/**
	 * Return the indices of the selected attributes
	 * 
	 * @return returns the indices of the selected attributes
	 */
	public int[] getSelection() {
		return selection;
	}

	/**
	 * Set the indices of the selected attributes
	 * 
	 * @param selection
	 *            the indices of the selected attributes
	 */
	public void setSelection(int[] selection) {
		this.selection = selection;
	}

	/**
	 * Set the content of the attribute selection table according to parameter
	 * "attList" and parameter "selectedIdx".
	 * 
	 * @param attList
	 *            the list of attribute names
	 * @param selectedIdx
	 *            indices of selected attributes in "attList"
	 */
	public void setAttList(String[] attList, int[] selectedIdx) {
		for (int i = 0; i < attList.length; i++) {
			attIdxMap.put(attList[i], new Integer(i));
		}
		boolean[] selVals = new boolean[attList.length];
		for (int i = 0; i < selVals.length; i++) {
			selVals[i] = false;
		}
		setSelection(selectedIdx);
		for (int i = 0; i < selectedIdx.length; i++) {
			selVals[selectedIdx[i]] = true;
		}
		Object[][] data = new Object[attList.length][2];
		for (int i = 0; i < data.length; i++) {
			data[i][0] = attList[i];
			data[i][1] = new Boolean(selVals[i]);
		}
		makeTable(data);
	}

	/**
	 * Create a JPanel to place at the bottom of AttSelectPanel. This panel
	 * contains two buttons: Ok and Cancel.
	 */
	protected JPanel makeBtnPane() {
		JPanel btnPane = new JPanel();
		JButton button = makeToolButton(null, "Ok", null, "Ok");
		btnPane.add(button);
		button = makeToolButton(null, "Cancel", null, "Cancel");
		btnPane.add(button);
		return btnPane;
	}

	/**
	 * Reset the content of the attribute selection table according to parameter
	 * "data"
	 * 
	 * @param data
	 *            new data to populate the table
	 */
	protected void makeTable(Object[][] data) {
		TableSorter sorter = new TableSorter(new MyTableModel(COLUMN_NAMES,
				data));
		table.setModel(sorter);
		sorter.setTableHeader(table.getTableHeader());
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		table.setPreferredScrollableViewportSize(new Dimension(322, 270));
		table.getColumnModel().getColumn(1).setMinWidth(
				table.getColumnModel().getColumn(1).getPreferredWidth());
		table.getColumnModel().getColumn(1).setMaxWidth(
				table.getColumnModel().getColumn(1).getPreferredWidth());
	}

	/**
	 * Create a JToolBar that contains these buttons: SelectAll, UnselectAll and
	 * InvertSelection. Depending on if the paramet "showOrderingButtons"
	 * evaluates to true or false, the toolbar in AttSelectPanel will or will
	 * not contain these four buttons dealing with ordering attributes: MoveUp,
	 * MoveTop, MoveDown and MoveBottom.
	 * 
	 * @param showOrderingButtons
	 *            a boolean deciding if to add ordering buttons
	 */
	protected JToolBar makeToolBar(boolean showOrderingButtons) {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		JButton button = null;
		button = makeToolButton("SelectAll", "SelectAll", "Select all",
				"SelectAll");
		toolBar.add(button);
		button = makeToolButton("ClearAll", "ClearAll", "Unselect all",
				"ClearAll");
		toolBar.add(button);
		button = makeToolButton("Invert", "Invert", "Invert selection",
				"Invert");
		toolBar.add(button);
		toolBar.addSeparator();
		if (showOrderingButtons) {
			button = makeToolButton("MoveUp", "MoveUp", "Move up a record",
					"MoveUp");
			toolBar.add(button);
			button = makeToolButton("MoveTop", "MoveTop", "Move to the top",
					"MoveTop");
			toolBar.add(button);
			button = makeToolButton("MoveDown", "MoveDown",
					"Move down a record", "MoveDown");
			toolBar.add(button);
			button = makeToolButton("MoveBottom", "MoveBottom",
					"Move to the bottom", "MoveBottom");
			toolBar.add(button);
		}
		return toolBar;
	}

	/**
	 * Create a JButton and set its Icon, ActionCommand, ToolTip and Alternative
	 * Text according to the input parameters.
	 * 
	 * @param imageName
	 *            name of the button icon image
	 * @param actionCommand
	 *            the action command
	 * @param toolTipText
	 *            the tool tips
	 * @param altText
	 *            alternative text to show in case an icon is not available
	 */
	protected JButton makeToolButton(String imageName, String actionCommand,
			String toolTipText, String altText) {
		// Look for the image.
		String imgLocation = "resources/" + imageName + ".gif";
		URL imageURL = this.getClass().getResource(imgLocation);
		// Create and initialize the button.
		JButton button = new JButton();
		button.setActionCommand(actionCommand);
		button.setToolTipText(toolTipText);
		button.addActionListener(this);
		if (imageURL != null) { // image found
			ImageIcon icon = new ImageIcon(imageURL, altText);
			button.setIcon(icon);
			button.setPreferredSize(new Dimension(icon.getIconWidth(), icon
					.getIconHeight()));
		} else { // no image found
			button.setText(altText);
			// System.err.println("Resource not found: " + imgLocation);
		}
		return button;
	}

	/**
	 * Adds the listener object to the list of listeners for actions
	 * 
	 * @param sl
	 *            listener
	 */
	public void addActionListener(ActionListener sl) {
		ell.add(ActionListener.class, sl);
	}

	/**
	 * Removes the listener object from the list
	 * 
	 * @param sl
	 *            listener
	 */
	public void removeActionListener(ActionListener sl) {
		ell.remove(ActionListener.class, sl);
	}

	/**
	 * Fires the actionPerformed event on all the registered listeners
	 * 
	 * @param command
	 *            the action command to associate with an ActionEvent
	 */
	public void fireAction(String command) {
		Object[] listeners = ell.getListenerList();
		int numListeners = listeners.length;
		ActionEvent se = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
				command);
		for (int i = 0; i < numListeners; i++) {
			if (listeners[i] == ActionListener.class) {
				// pass the event to the listeners event dispatch method
				((ActionListener) listeners[i + 1]).actionPerformed(se);
			}
		}
	}

	/**
	 * Implements the ActionListener interface
	 * 
	 * @param e
	 *            an ActionEvent
	 */
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		boolean isSorting = ((TableSorter) table.getModel()).isSorting();
		if (cmd.equals("MoveUp") && !isSorting) {
			int[] selected = table.getSelectedRows();
			if (selected != null && selected.length > 0 && selected[0] > 0) {
				int first = selected[0] - 1;
				int last = selected[selected.length - 1];
				Object[] attRow = new Object[last - first + 1];
				Object[] selRow = new Object[last - first + 1];
				for (int i = 0; i < attRow.length; i++) {
					attRow[i] = table.getValueAt(first + i, 0);
					selRow[i] = table.getValueAt(first + i, 1);
				}
				for (int i = first; i < last; i++) {
					table.setValueAt(attRow[i - first + 1], i, 0);
					table.setValueAt(selRow[i - first + 1], i, 1);
				}
				table.setValueAt(attRow[0], last, 0);
				table.setValueAt(selRow[0], last, 1);
				table.setRowSelectionInterval(first, last - 1);
			}
		} else if (cmd.equals("MoveTop") && !isSorting) {
			int[] selected = table.getSelectedRows();
			if (selected != null && selected.length > 0 && selected[0] > 0) {
				int first = selected[0];
				int last = selected[selected.length - 1];
				int length = last - first + 1;
				Object[] attRow = new Object[last + 1];
				Object[] selRow = new Object[last + 1];
				for (int i = 0; i < attRow.length; i++) {
					attRow[i] = table.getValueAt(i, 0);
					selRow[i] = table.getValueAt(i, 1);
				}
				for (int i = 0; i < first; i++) {
					table.setValueAt(attRow[i], i + length, 0);
					table.setValueAt(selRow[i], i + length, 1);
				}
				for (int i = first; i <= last; i++) {
					table.setValueAt(attRow[i], i - first, 0);
					table.setValueAt(selRow[i], i - first, 1);
				}
				table.setRowSelectionInterval(0, last - first);
			}
		} else if (cmd.equals("MoveDown") && !isSorting) {
			int[] selected = table.getSelectedRows();
			if (selected != null
					&& selected.length > 0
					&& selected[selected.length - 1] < (table.getRowCount() - 1)) {
				int first = selected[0];
				int last = selected[selected.length - 1] + 1;
				Object[] attRow = new Object[last - first + 1];
				Object[] selRow = new Object[last - first + 1];
				for (int i = 0; i < attRow.length; i++) {
					attRow[i] = table.getValueAt(first + i, 0);
					selRow[i] = table.getValueAt(first + i, 1);
				}
				for (int i = last; i > first; i--) {
					table.setValueAt(attRow[i - first - 1], i, 0);
					table.setValueAt(selRow[i - first - 1], i, 1);
				}
				table.setValueAt(attRow[last - first], first, 0);
				table.setValueAt(selRow[last - first], first, 1);
				table.setRowSelectionInterval(first + 1, last);
			}
		} else if (cmd.equals("MoveBottom") && !isSorting) {
			int[] selected = table.getSelectedRows();
			if (selected != null
					&& selected.length > 0
					&& selected[selected.length - 1] < (table.getRowCount() - 1)) {
				int first = selected[0];
				int last = selected[selected.length - 1];
				int length = last - first + 1;
				int nRow = table.getModel().getRowCount();
				Object[] attRow = new Object[nRow - first];
				Object[] selRow = new Object[nRow - first];
				for (int i = 0; i < attRow.length; i++) {
					attRow[i] = table.getValueAt(i + first, 0);
					selRow[i] = table.getValueAt(i + first, 1);
				}
				for (int i = nRow - 1; i > last; i--) {
					table.setValueAt(attRow[i - first], i - length, 0);
					table.setValueAt(selRow[i - first], i - length, 1);
				}
				for (int i = last; i >= first; i--) {
					table.setValueAt(attRow[i - first], i + nRow - 1 - last, 0);
					table.setValueAt(selRow[i - first], i + nRow - 1 - last, 1);
				}
				table.setRowSelectionInterval(nRow - length, nRow - 1);
			}
		} else if (cmd.equals("SelectAll")) {
			int nRow = table.getRowCount();
			for (int i = 0; i < nRow; i++) {
				table.setValueAt(new Boolean(true), i, 1);
			}
		} else if (cmd.equals("ClearAll")) {
			int nRow = table.getRowCount();
			for (int i = 0; i < nRow; i++) {
				table.setValueAt(new Boolean(false), i, 1);
			}
		} else if (cmd.equals("Invert")) {
			int nRow = table.getRowCount();
			for (int i = 0; i < nRow; i++) {
				boolean selected = ((Boolean) table.getValueAt(i, 1))
						.booleanValue();
				if (selected) {
					table.setValueAt(new Boolean(false), i, 1);
				} else {
					table.setValueAt(new Boolean(true), i, 1);
				}
			}
		} else if (cmd.equals("Ok")) {
			int[] help = new int[table.getRowCount()];
			int count = 0;
			for (int i = 0; i < table.getRowCount(); i++) {
				boolean selected = ((Boolean) table.getValueAt(i, 1))
						.booleanValue();
				if (selected) {
					help[count++] = ((Integer) attIdxMap.get(table.getValueAt(
							i, 0))).intValue();
				}
			}
			int[] selected = new int[count];
			System.arraycopy(help, 0, selected, 0, count);
			setSelection(selected);
			fireAction("AttSelectPanel.OK");
		} else if (cmd.equals("Cancel")) {
			if (getSelection() != null) {
				int[] selected = getSelection();
				boolean[] selVals = new boolean[table.getRowCount()];
				for (int i = 0; i < selVals.length; i++) {
					selVals[i] = false;
				}
				for (int i = 0; i < selected.length; i++) {
					selVals[selected[i]] = true;
				}
				for (int i = 0; i < table.getRowCount(); i++) {
					int index = ((Integer) attIdxMap
							.get(table.getValueAt(i, 0))).intValue();
					table.setValueAt(new Boolean(selVals[index]), i, 1);
				}
			}
			fireAction("AttSelectPanel.CANCEL");
		}
	}

	/**
	 * <p>
	 * Title: MyTableModel
	 * </p>
	 * <p>
	 * Description: This inner class is obtained from SUN and it defines a
	 * customized table model.
	 * </p>
	 * 
	 * @extends AbstractTableModel
	 */
	class MyTableModel extends AbstractTableModel {
		private final String[] columnNames;
		private final Object[][] data;

		public MyTableModel(String[] colNames, Object[][] tableData) {
			columnNames = colNames;
			data = tableData;
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public int getRowCount() {
			return data.length;
		}

		@Override
		public String getColumnName(int col) {
			return columnNames[col];
		}

		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		/**
		 * JTable uses this method to determine the default renderer/ editor for
		 * each cell. If we didn't implement this method, then the last column
		 * would contain text ("true"/"false"), rather than a check box.
		 */
		@Override
		public Class getColumnClass(int c) {
			return getValueAt(0, c).getClass();
		}

		/**
		 * Don't need to implement this method unless your table's editable.
		 */
		@Override
		public boolean isCellEditable(int row, int col) {
			// Note that the data/cell address is constant,
			// no matter where the cell appears onscreen.
			if (col < 1) {
				return false;
			}
			return true;
		}

		/**
		 * Don't need to implement this method unless your table's data can
		 * change.
		 */
		@Override
		public void setValueAt(Object value, int row, int col) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Setting value at " + row + "," + col + " to "
						+ value + " (an instance of " + value.getClass() + ")");
			}

			data[row][col] = value;
			fireTableCellUpdated(row, col);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("New value of data:");
				printDebugData();
			}
		}

		private void printDebugData() {
			int numRows = getRowCount();
			int numCols = getColumnCount();

			for (int i = 0; i < numRows; i++) {
				System.out.print("    row " + i + ":");
				for (int j = 0; j < numCols; j++) {
					System.out.print("  " + data[i][j]);
				}

			}
			logger.finest("--------------------------");
		}
	}

	/**
	 * Creates and shows the AttSelectPanel for testing
	 */
	protected static void createAndShowGUI() {
		AttSelectPanel asp = new AttSelectPanel(true);
		JFrame f = new JFrame("Test");
		f.getContentPane().add(asp);
		f.setSize(325, 400);
		f.pack();
		f.setVisible(true);
		String[] atts = { "Name", "State Name", "County Name", "Shape",
				"Geology", "Slope" };
		int[] selected = new int[atts.length];
		for (int i = 0; i < selected.length; i++) {
			selected[i] = i;
		}
		asp.setAttList(atts, selected);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	/**
	 * To test the AttSelectPanel
	 * 
	 * @param args
	 *            arguments passed to the main() function
	 */
	public static void main(String args[]) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

}