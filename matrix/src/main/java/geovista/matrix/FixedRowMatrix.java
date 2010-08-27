/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.matrix;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;

public class FixedRowMatrix extends AbstractMatrix {

	private static final int DEFAULT_NUM_GRAPHES = 4;
	protected EventListenerList listenerList = new EventListenerList();
	private SPTagButton[] columnButton;
	private JComboBox attCombo;
	private int yIdx = 0;
	private static Insets nullInsets;
	private final Class[] elementClasses = new Class[DEFAULT_NUM_GRAPHES];
	private final String[] elementClassNames = new String[DEFAULT_NUM_GRAPHES];
	private int graphTypeNumber;
	private String[] varTags;
	protected ImageIcon matrixIcon = new ImageIcon(FixedRowMatrix.class
			.getResource("resources/matrixicon16.gif"));
	final static Logger logger = Logger.getLogger(FixedRowMatrix.class
			.getName());

	public FixedRowMatrix() {
		super();
		this.setSize(panelWidthPixels, panelHeightPixels);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param classname
	 */
	public void setElementClassName0(String classname) {
		elementClassNames[0] = classname;

	}

	public void setElementClassName1(String classname) {
		elementClassNames[1] = classname;

	}

	public void setElementClassName2(String classname) {
		elementClassNames[2] = classname;

	}

	public void setElementClassName3(String classname) {
		elementClassNames[3] = classname;

	}

	/**
	 * put your documentation comment here
	 * 
	 * @return
	 */
	public String getElementClassName0() {
		return elementClassNames[0];
	}

	public String getElementClassName1() {
		return elementClassNames[1];
	}

	public String getElementClassName2() {
		return elementClassNames[2];
	}

	public String getElementClassName3() {
		return elementClassNames[3];
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param obj
	 */
	public void setElementClass0(Object obj) {
		setElementClassName0((obj != null) ? obj.getClass().getName() : null);
	}

	public void setElementClass1(Object obj) {
		setElementClassName1((obj != null) ? obj.getClass().getName() : null);
	}

	public void setElementClass2(Object obj) {
		setElementClassName2((obj != null) ? obj.getClass().getName() : null);
	}

	public void setElementClass3(Object obj) {
		setElementClassName3((obj != null) ? obj.getClass().getName() : null);
	}

	public void setElementClass0(Class clazz) {
		elementClasses[0] = clazz;
	}

	public void setElementClass1(Class clazz) {
		elementClasses[1] = clazz;
	}

	public void setElementClass2(Class clazz) {
		elementClasses[2] = clazz;
	}

	public void setElementClass3(Class clazz) {
		elementClasses[3] = clazz;
	}

	/**
	 * Overwrite the method in AbstractMatrix, because the size of the matrix is
	 * different.
	 * 
	 * @param condition
	 */
	@Override
	public void setConditionArray(int[] condition) {
		if (condition == null) {
			return;
		}
		conditionArray = condition;
		for (int k = 0; k < plotNumber * graphTypeNumber; k++) {
			MatrixElement otherElement = element[k];
			if (otherElement != null) {
				otherElement.setConditionArray(conditionArray);
			}
		}
		repaint();
	}

	@Override
	public void setSelectedObvs(int[] selected) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Set Selected Obs: ");
		}
		if (selected == null) {
			return;
		}
		for (int i = 0; i < selectedObvsInt.length; i++) {
			selectedObvsInt[i] = 0;
		}
		for (int i = 0; i < selected.length; i++) {
			selectedObvsInt[selected[i]] = 1;
		}
		multipleSelectionColors = null;
		// Once selection from other components has been set, pass it to each
		// element inside of matrix.
		for (int k = 0; k < plotNumber * graphTypeNumber; k++) {
			MatrixElement otherElement = element[k];
			otherElement.setSelections(selectedObvsInt);
			otherElement.setMultipleSelectionColors(multipleSelectionColors);
		}
		repaint();
	}

	@Override
	protected synchronized void init() {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("get in init()...");
		}
		if (!recreate) {
			return; // maybe display error message.
		}
		removeAll();
		graphTypeNumber = 0;
		attList = new JList(attributesDisplay);
		attCombo = new JComboBox(attributesDisplay);
		for (int i = 0; i < DEFAULT_NUM_GRAPHES; i++) {
			if (elementClassNames[i] != null) {
				try {
					elementClasses[graphTypeNumber] = Class
							.forName(elementClassNames[i]);
				} catch (Exception e) {
					e.printStackTrace();
				}
				graphTypeNumber++;
			} else {
				graphTypeNumber = elementClasses.length;
			}
		}
		varTags = new String[plotNumber];
		if (dataObject != null) {
			element = new MatrixElement[plotNumber * graphTypeNumber];
			matrixLayout = new GridBagLayout();
			c = new SPGridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			createMatrix();
			Container parent = getParent();
			if (parent != null) {
				parent.validate();
			} else {
				validate();
			}
		}
		super.registerIndicationListeners();
	}

	// Set up elements in matrix.
	@Override
	protected void createMatrix() {
		logger.finest("create SP:");
		setLayout(matrixLayout);
		columnButton = new SPTagButton[plotNumber];
		SPTagButton[] rowButton = new SPTagButton[graphTypeNumber];
		Dimension tagDm = new Dimension(0, 0);
		configButton = new JButton();
		try {
			for (int i = 0; i < graphTypeNumber + 1; i++) {
				for (int j = 0; j < plotNumber + 1; j++) {
					if ((i == 0) && (j == 0)) {
						c.weightx = 0.0;
						c.weighty = 0.0;
						c.gridwidth = 1;
						c.gridheight = 1;
						configButton.setIcon(matrixIcon);
						configButton.setMargin(FixedRowMatrix.nullInsets);
						configButton
								.addActionListener(new java.awt.event.ActionListener() {

									/**
									 * Set up the attributes for plotted in SPM.
									 * 
									 * @param e
									 */
									public void actionPerformed(ActionEvent e) {
										try {
											configButton_actionPerformed(e);
										} catch (Exception ex) {
											ex.printStackTrace();
										}
									}
								});
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(configButton, c);
						add(configButton);
					} else if ((i == 0) && (j != 0)) {
						c.weightx = 1.0;
						c.weighty = 0.0;
						c.gridwidth = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						c.gridheight = 1;
						if (j == plotNumber) {
							c.gridwidth = GridBagConstraints.REMAINDER; // end
						}
						// row
						varTags[j - 1] = attributesDisplay[plottedAttributes[j - 1]];
						columnButton[j - 1] = new SPTagButton(varTags[j - 1]);
						columnButton[j - 1]
								.setMargin(FixedRowMatrix.nullInsets);
						columnButton[j - 1]
								.addMouseListener(columnButton[j - 1]);
						columnButton[j - 1]
								.addMouseMotionListener(columnButton[j - 1]);
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(columnButton[j - 1], c);
						add(columnButton[j - 1]);

					} else if ((i != 0) && (j == 0)) {
						c.weightx = 0.0;
						c.weighty = 1.0;
						c.gridwidth = 1;
						c.gridheight = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						String yVarTag = attributesDisplay[yIdx];
						rowButton[i - 1] = new SPTagButton("s");
						rowButton[i - 1].setMargin(FixedRowMatrix.nullInsets);
						rowButton[i - 1]
								.setVerticalAlignment(SwingConstants.BOTTOM);
						AffineTransform trans = new AffineTransform();
						trans.rotate(-Math.PI / 2);
						Font font = new Font("", Font.BOLD, 11);
						font = font.deriveFont(trans);

						rowButton[i - 1].setFont(font);
						rowButton[i - 1].setText(yVarTag);
						rowButton[i - 1].addMouseListener(rowButton[i - 1]);
						rowButton[i - 1]
								.addMouseMotionListener(rowButton[i - 1]);
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(rowButton[i - 1], c);
						add(rowButton[i - 1]);
					} else {
						c.weightx = 1.0;
						c.weighty = 1.0;
						c.gridwidth = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						c.gridheight = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						if (j == plotNumber) {
							c.gridwidth = GridBagConstraints.REMAINDER; // end
						}
						// row
						int indexCurrent = (i - 1) * (plotNumber) + (j - 1);
						int[] dataIndices = new int[2];
						dataIndices[0] = plottedAttributes[j - 1] + 1;
						dataIndices[1] = yIdx + 1;
						// construct of each element
						element[indexCurrent] = (MatrixElement) elementClasses[i - 1]
								.newInstance();
						logger.info(element[indexCurrent].getClass().getName());
						element[indexCurrent].setAxisOn(false);
						element[indexCurrent].setDataSet(dataSet);
						element[indexCurrent].setSelectionColor(selectionColor);
						element[indexCurrent].setBackground(background);
						element[indexCurrent].setDataIndices(dataIndices);
						if (bivarColorClasser != null) {
							element[indexCurrent].setBivarColorClasser(
									bivarColorClasser, false);
						}
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(
								(Component) element[indexCurrent], c);
						add((Component) element[indexCurrent]);
						if (rowButton[i - 1].getText().equals("s")) {
							rowButton[i - 1]
									.setText(stringToVertical(element[indexCurrent]
											.getShortDiscription()));
						}
						element[indexCurrent]
								.addActionListener(new ActionListener() {

									/**
									 * Get the event from a unit plot and send
									 * it to all units.
									 * 
									 * @param e
									 */
									public void actionPerformed(ActionEvent e) {
										// This gets the source or originator of
										// the event
										try {
											unit_actionPerformed(e);
										} catch (Exception ex) {
											ex.printStackTrace();
										}
									}
								});
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (int j = 0; j < plotNumber; j++) {
			if (columnButton[j].getPreferredSize().getWidth() > tagDm
					.getWidth()) {
				tagDm = columnButton[j].getPreferredSize();
			}
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("tagDm: " + tagDm.getWidth() + tagDm.getHeight());
			}
		}
		for (int j = 0; j < plotNumber; j++) {
			columnButton[j].setPreferredSize(tagDm);
			columnButton[j].setMinimumSize(tagDm);
			columnButton[j].setMaximumSize(tagDm);
		}
		Dimension rowTag = new Dimension((int) tagDm.getHeight(), (int) tagDm
				.getHeight());
		configButton.setPreferredSize(rowTag);
		configButton.setMinimumSize(rowTag);
		configButton.setMaximumSize(rowTag);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param e
	 */
	private void unit_actionPerformed(ActionEvent e) {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("action performed...");
		}
		MatrixElement source = (MatrixElement) e.getSource();
		String command = e.getActionCommand();
		if (command.compareTo(MatrixElement.COMMAND_POINT_SELECTED) == 0) {
			if (logger.isLoggable(Level.FINEST)) {
				System.out
						.println("SPMC.plotUnitPanel.actionPerformed(), point selected");
			}
			selectedObvsInt = source.getSelections();
			for (int k = 0; k < plotNumber * graphTypeNumber; k++) {
				MatrixElement otherElement = element[k];
				// Don't recall the scatterplot which generated the original
				// event
				if (otherElement != source) {
					otherElement.setSelections(selectedObvsInt);
				}
			}
			this.repaint();
			fireChangeEvent();
			fireSelectionChanged(getSelectedObvs());
		} else if (command
				.compareTo(MatrixElement.COMMAND_COLOR_CLASSFICIATION) == 0) {
			bivarColorClasser = source.getBivarColorClasser();
			for (int k = 0; k < plotNumber * graphTypeNumber; k++) {
				MatrixElement otherElement = element[k];
				// Don't recall the scatterplot which generated the original
				// event
				if (otherElement != source) {
					otherElement.setBivarColorClasser(bivarColorClasser, false);
				}
			}
			this.repaint();
		} else if (command.compareTo(MatrixElement.COMMAND_DATARANGE_SET) == 0) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("in axis reset...");
			}
			double[] xAxisExtents = source.getXAxisExtents();
			double[] yAxisExtents = source.getYAxisExtents();
			int pos = 0;
			for (int k = 0; k < graphTypeNumber * plotNumber; k++) {
				MatrixElement otherElement = element[k];
				// Don't recall the scatterplot which generated the original
				// event
				if (otherElement == source) {
					pos = k;
					break;
				}
			}
			int r = pos / plotNumber;
			int c = pos % plotNumber;

			for (int i = 0; i < plotNumber; i++) {
				element[r + i].setYAxisExtents(yAxisExtents);
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest("in y axis reset...");
				}
			}
			for (int i = 0; i < graphTypeNumber; i++) {
				if (element[c + i * plotNumber].getClass().getName().equals(
						"geovista.geoviz.scatterplot.ScatterPlot")) {
					element[c + i * plotNumber].setXAxisExtents(xAxisExtents);
				}
			}
		} else {
			System.err.println("Unknown command! = " + command);
		}
	}

	/**
	 * Shift columns or rows in the matrix.
	 * 
	 * @param lastPos
	 * @param newPos
	 */
	private void moveColumn(int lastPos, int newPos) {
		int indicesRow;
		int[] indicesLast1;
		int[] indicesNew1;
		String varTagMoved = new String(varTags[lastPos - 1]);
		varTags[lastPos - 1] = varTags[newPos - 1];
		columnButton[lastPos - 1].setText(varTags[lastPos - 1]);
		varTags[newPos - 1] = varTagMoved;
		columnButton[newPos - 1].setText(varTags[newPos - 1]);
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("lastPos: " + lastPos + "newPos:" + newPos);
		}
		for (int i = 0; i < graphTypeNumber; i++) {
			indicesLast1 = (element[i * plotNumber + lastPos - 1]
					.getElementPosition());
			indicesRow = indicesLast1[0];

			indicesNew1 = element[i * plotNumber + newPos - 1]
					.getElementPosition();
			indicesLast1[0] = indicesNew1[0];
			element[i * plotNumber + lastPos - 1].setDataIndices(indicesLast1);
			indicesNew1[0] = indicesRow;
			element[i * plotNumber + newPos - 1].setDataIndices(indicesNew1);
		}
	}

	/**
	 * Configure attributes for plotted in matrix.
	 * 
	 * @param e
	 */
	private void configButton_actionPerformed(ActionEvent e) {
		attSelectDialog(400, 400);
	}

	JFrame dummyFrame = new JFrame();
	JDialog dialog = null;
	JScrollPane dialogPane = null;

	/**
	 * put your documentation comment here
	 * 
	 * @param x
	 * @param y
	 */
	private void attSelectDialog(int x, int y) {
		attList.setSelectedIndices(plottedAttributes);
		if (dialog == null) {
			dialog = new JDialog(dummyFrame, "Attributes for Plot", true);
			JButton selectButton;
			JButton closeButton;
			dialog.setSize(200, 300);
			dialog.getContentPane().setLayout(new BorderLayout());
			attList
					.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			// attList.addListSelectionListener(this);
			// JScrollPane scrollPane = new JScrollPane(attList);
			dialogPane = new JScrollPane(attList);

			attCombo.setSelectedIndex(yIdx);
			attCombo.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					JComboBox cb = (JComboBox) e.getSource();
					yIdx = cb.getSelectedIndex();
				}
			});
			JPanel comboPanel = new JPanel(new BorderLayout());
			comboPanel.add(attCombo, BorderLayout.NORTH);

			selectButton = new JButton("Apply");
			selectButton.addActionListener(new java.awt.event.ActionListener() {

				/**
				 * put your documentation comment here
				 * 
				 * @param e
				 */
				public void actionPerformed(ActionEvent e) {
					selectButton_actionPerformed(e);
				}
			});
			closeButton = new JButton("Close");
			closeButton.addActionListener(new java.awt.event.ActionListener() {

				/**
				 * put your documentation comment here
				 * 
				 * @param e
				 */
				public void actionPerformed(ActionEvent e) {
					closeButton_actionPerformed(e);
				}
			});

			JPanel buttonPanel = new JPanel(new BorderLayout());
			buttonPanel.add(selectButton, BorderLayout.WEST);
			buttonPanel.add(closeButton, BorderLayout.EAST);
			JPanel namePanel = new JPanel(new GridLayout(1, 2));
			namePanel.add(new JLabel("Column Vars:"));
			namePanel.add(new JLabel("Row Var:"));
			JPanel attPanel = new JPanel(new GridLayout(1, 2));
			attPanel.add(dialogPane);
			attPanel.add(comboPanel);
			dialog.getContentPane().add(namePanel, BorderLayout.NORTH);
			dialog.getContentPane().add(attPanel, BorderLayout.CENTER);
			dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		} else {
			dialogPane.setViewportView(attList);
		}
		attList.addListSelectionListener(this);
		plottedAttributes = attList.getSelectedIndices();
		dialog.setLocation(x, y);
		dialog.setVisible(true);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param e
	 */
	private void selectButton_actionPerformed(ActionEvent e) {
		plotNumber = plottedAttributes.length;
		init();
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param e
	 */
	private void closeButton_actionPerformed(ActionEvent e) {
		dialog.setVisible(false);
	}

	protected class SPTagButton extends JButton implements MouseListener,
			MouseMotionListener {

		/**
		 * put your documentation comment here
		 * 
		 * @param String
		 *            label
		 */
		SPTagButton(String label) {
			super(label);
			// buttonLabel = label;
		}

		/**
		 * put your documentation comment here
		 * 
		 * @param ImageIcon
		 *            icon
		 */
		SPTagButton(ImageIcon icon) {
			super(icon);
		}

		/**
		 * put your documentation comment here
		 * 
		 * @param e
		 */
		public void mousePressed(MouseEvent e) {
			JButton button = (JButton) e.getSource();
			SPGridBagConstraints gbconst = (SPGridBagConstraints) matrixLayout
					.getConstraints(button);
			posLast = new Point(gbconst.column, gbconst.row);
			posDrag = (Point) posLast.clone();
		}

		/**
		 * put your documentation comment here
		 * 
		 * @param e
		 */
		public void mouseReleased(MouseEvent e) {
			posNew = SwingUtilities.convertPoint((SPTagButton) e.getSource(), e
					.getX(), e.getY(), FixedRowMatrix.this);
			posNew = matrixLayout.location(posNew.x, posNew.y);
			if (posNew.x > 4 * (plotNumber - 1)) {
				posNew.setLocation(posNew.x / 4 + 1, posNew.y / 4);
			} else {
				posNew.setLocation(posNew.x / 4, posNew.y / 4);
			}
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("PosNewX: " + posNew.x + "posNewY: " + posNew.y);
			}
			int lastPos = 0;
			int newPos = 0;
			if (!validCellPos(posDrag) || !validCellPos(posNew)) {
				return;
			}
			if ((posDrag.x != posNew.x) && (posDrag.y == 0)) {
				lastPos = posLast.x;
				newPos = posNew.x;
				moveColumn(lastPos, newPos);
				posDrag = posNew;
				repaint();
			}
		}

		/**
		 * put your documentation comment here
		 * 
		 * @param e
		 */
		public void mouseExited(MouseEvent e) {

		}

		/**
		 * put your documentation comment here
		 * 
		 * @param e
		 */
		public void mouseDragged(MouseEvent e) {
			posNew = SwingUtilities.convertPoint((SPTagButton) e.getSource(), e
					.getX(), e.getY(), FixedRowMatrix.this);
			posNew = matrixLayout.location(posNew.x, posNew.y);
			if (logger.isLoggable(Level.FINEST)) {
				logger
						.finest("PosNew0X: " + posNew.x + "posNew0Y: "
								+ posNew.y);
			}
			if (posNew.x > 4 * (plotNumber - 1)) {
				posNew.setLocation(posNew.x / 4 + 1, posNew.y / 4);
			} else {
				posNew.setLocation(posNew.x / 4, posNew.y / 4);
			}
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("PosNewX: " + posNew.x + "posNewY: " + posNew.y);
			}
			int lastPos = 0;
			int newPos = 0;
			if (!validCellPos(posDrag) || !validCellPos(posNew)) {
				return;
			}
			if (posDrag.x != posNew.x) {
				lastPos = posDrag.x;
				newPos = posNew.x;
				moveColumn(lastPos, newPos);
				posDrag = posNew;
				repaint();
			}
		}

		/**
		 * put your documentation comment here
		 * 
		 * @param e
		 */
		public void mouseMoved(MouseEvent e) {

		}

		/**
		 * put your documentation comment here
		 * 
		 * @param e
		 */
		public void mouseEntered(MouseEvent e) {

		}

		/**
		 * put your documentation comment here
		 * 
		 * @param e
		 */
		public void mouseClicked(MouseEvent e) {

		}
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param e
	 */
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			return;
		}
		JList theList = (JList) e.getSource();
		if (theList.isSelectionEmpty()) {
			return;
		}
		plottedAttributes = theList.getSelectedIndices();
	}

	private String stringToVertical(String s) {
		String vSt;
		char[] sChar = s.toCharArray();
		int len = sChar.length;
		vSt = "<html> ";
		for (int i = 0; i < len; i++) {
			vSt = vSt + sChar[i] + "<br>";
		}
		vSt = vSt + "</html> ";
		return vSt;
	}

}