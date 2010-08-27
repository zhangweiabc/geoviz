/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.matrix;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;

import geovista.matrix.scatterplot.DecisionTreeScatterPlot;

/**
 * put your documentation comment here
 */
public class DecisionTreeMatrix extends AbstractMatrix {

	private String elementClassName;

	private String[] attributesArray;

	private final ImageIcon matrixIcon = new ImageIcon(this.getClass()
			.getResource("matrix16.gif"));
	private DecisionTreeScatterPlot[] scatterPlot;
	private Vector decisionTree;
	private final EventListenerList listenerListAction = new EventListenerList();

	/**
	 * put your documentation comment here
	 */
	public DecisionTreeMatrix() {
		super();
	}

	/**
	 * Set up the name for element which will be displayed in matrix. Can be
	 * defined in bean proporty.
	 * 
	 * @param classname
	 */
	public void setElementClassName(String classname) {
		elementClassName = classname;
		try {
			setElementClass((elementClassName != null) ? Class
					.forName(elementClassName) : null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the name for element which will be displayed in matrix.
	 * 
	 * @return
	 */
	public String getElementClassName() {
		return elementClassName;
	}

	/**
	 * Set the element which will be displayed in matrix by connecting to an
	 * object.
	 * 
	 * @param obj
	 */
	public void setElementClass(Object obj) {
		setElementClassName((obj != null) ? obj.getClass().getName() : null);
	}

	/**
	 * Set up the element displayed in matrix. Called by setElementClass(Oject
	 * obj).
	 * 
	 * @param clazz
	 */
	public void setElementClass(Class clazz) {
		if (elementClass != clazz) {
			recreate = true;
		}
		elementClass = clazz;
	}

	/**
	 * put your documentation comment here
	 * 
	 * @return
	 */
	public String[] getAttributesArray() {
		return attributesArray;
	}

	@Override
	protected synchronized void init() {
		logger.finest("get in init()...");
		if (!recreate) {
			return; // maybe display error message.
		}
		removeAll();

		attList = new JList(attributesDisplay);
		varTags = new String[plotNumber];
		// if (this.dataVector != null) {
		scatterPlot = new DecisionTreeScatterPlot[plotNumber * plotNumber];
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
		// }
	}

	/**
	 * The actual procedure to create the matrix, fill graphs in each matrix
	 * element.
	 */
	@Override
	protected void createMatrix() {

		setLayout(matrixLayout);
		columnButton = new SPTagButton[plotNumber];
		rowButton = new SPTagButton[plotNumber];
		Dimension tagDm = new Dimension(0, 0);
		configButton = new JButton();
		configButton.setIcon(matrixIcon);
		try {
			for (int i = 0; i < plotNumber + 1; i++) {
				for (int j = 0; j < plotNumber + 1; j++) {
					if ((i == 0) && (j == 0)) {
						// The M button on left up corner. Click on that will
						// pop up a variable selection dialog for displaying in
						// matrix.
						c.weightx = 0.0;
						c.weighty = 0.0;
						c.gridwidth = 1;
						c.gridheight = 1;
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

										} catch (Exception exception) {
											exception.printStackTrace();
										}
									}
								});
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(configButton, c);
						add(configButton);
					} else if ((i == 0) && (j != 0)) {
						// The first row in matrix. They are buttons which can
						// be dragged to change the column position.
						c.weightx = 1.0;
						c.weighty = 0.0;
						c.gridwidth = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						// c.gridheight = 1;
						if (j == plotNumber) {
							c.gridwidth = GridBagConstraints.REMAINDER; // end
							// row
						}
						varTags[j - 1] = attributesDisplay[plottedAttributes[j - 1]];
						columnButton[j - 1] = new SPTagButton(varTags[j - 1]);
						columnButton[j - 1]
								.setMargin(AbstractMatrix.nullInsets);
						columnButton[j - 1]
								.addMouseListener(columnButton[j - 1]);
						columnButton[j - 1]
								.addMouseMotionListener(columnButton[j - 1]);
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(columnButton[j - 1], c);
						add(columnButton[j - 1]);
					} else if ((i != 0) && (j == 0)) {
						// The first column in matrix. They are buttons which
						// can be dragged to change the row position.
						c.weightx = 0.0;
						c.weighty = 1.0;
						c.gridwidth = 1;
						c.gridheight = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;

						rowButton[i - 1] = new SPTagButton("s");
						rowButton[i - 1].setMargin(AbstractMatrix.nullInsets);
						rowButton[i - 1]
								.setVerticalAlignment(SwingConstants.BOTTOM);
						// rowButton[i-1].setVerticalTextPosition(SwingConstants.BOTTOM);
						rowButton[i - 1]
								.setHorizontalAlignment(SwingConstants.CENTER);
						// JLabel rowLabel = new JLabel();
						AffineTransform trans = new AffineTransform();
						trans.rotate(-Math.PI / 2, rowButton[i - 1].getWidth(),
								rowButton[i - 1].getHeight() / 2);
						Font font = new Font("", Font.BOLD, 11);
						font = font.deriveFont(trans);
						// rowLabel.setFont(font);
						// BufferedImage labelImage = new
						// BufferedImage(rowLabel.getWidth()+1,rowLabel.getHeight()+1,BufferedImage.TYPE_INT_ARGB);
						// Graphics2D g2 = labelImage.createGraphics();
						// rowButton[i-1].paint(g2);
						rowButton[i - 1].setFont(font);
						rowButton[i - 1].setText(varTags[i - 1]);
						rowButton[i - 1].addMouseListener(rowButton[i - 1]);
						rowButton[i - 1]
								.addMouseMotionListener(rowButton[i - 1]);
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(rowButton[i - 1], c);
						add(rowButton[i - 1]);
					} else {
						// Actually set up the graph components in matrix.
						c.weightx = 1.0;
						c.weighty = 1.0;
						c.gridwidth = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						c.gridheight = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						int indexCurrent = (i - 1) * (plotNumber) + (j - 1);
						int[] dataIndices = new int[2];
						dataIndices[0] = plottedAttributes[j - 1] + 1; // +1 is
						// because
						// in
						// the
						// data
						// structure,
						// we
						// reserve
						// the
						// first
						// element
						// for
						// variable
						// names.
						dataIndices[1] = plottedAttributes[i - 1] + 1;
						// construct of each element
						scatterPlot[indexCurrent] = new DecisionTreeScatterPlot();
						scatterPlot[indexCurrent].setAxisOn(false);
						scatterPlot[indexCurrent].setDataSet(dataSet);

						scatterPlot[indexCurrent].setBackground(background);
						scatterPlot[indexCurrent].setDataIndices(dataIndices);
						scatterPlot[indexCurrent]
								.setSelectionColor(selectionColor);
						if (decisionTree != null) {
							scatterPlot[indexCurrent]
									.setTreeVector(decisionTree);
						}
						if (bivarColorClasser != null) {
							scatterPlot[indexCurrent].setBivarColorClasser(
									bivarColorClasser, false);
						}

						if (j == plotNumber) {
							c.gridwidth = GridBagConstraints.REMAINDER; // end
							// row
						}
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(scatterPlot[indexCurrent],
								c);
						add(scatterPlot[indexCurrent]);
						scatterPlot[indexCurrent]
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
										} catch (Exception exception) {
											exception.printStackTrace();
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
			logger.finest("tagDm: " + tagDm.getWidth() + tagDm.getHeight());
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
		DecisionTreeScatterPlot source = (DecisionTreeScatterPlot) e
				.getSource();
		String command = e.getActionCommand();
		if (command.compareTo(DecisionTreeScatterPlot.COMMAND_POINT_SELECTED) == 0) {
			// For selection event.
			// selectedObvs = source.getSelectedObservations();
			selectedObvsInt = source.getSelections();
			for (int k = 0; k < plotNumber * plotNumber; k++) {
				DecisionTreeScatterPlot otherElement = scatterPlot[k];
				// Don't recall the scatterplot which generated the original
				// event
				if (otherElement != source) {
					// otherElement.setSelectedObservations(selectedObvs);
					otherElement.setSelections(selectedObvsInt);
					otherElement.setMultipleSelectionColors(null);
				}
			}
			this.repaint();
			fireChangeEvent();
			fireSelectionChanged(getSelectedObvs());
		} else if (command
				.compareTo(MatrixElement.COMMAND_COLOR_CLASSFICIATION) == 0) {
			// For classification color event.
			bivarColorClasser = source.getBivarColorClasser();
			for (int k = 0; k < plotNumber * plotNumber; k++) {
				DecisionTreeScatterPlot otherElement = scatterPlot[k];
				// Don't recall the scatterplot which generated the original
				// event
				if (otherElement != source) {
					otherElement.setBivarColorClasser(bivarColorClasser, false);
				}
			}
			this.repaint();
			fireChangeEvent();
		} else if (command.compareTo(MatrixElement.COMMAND_DATARANGE_SET) == 0) {
			// For set up new axis extents in each scatterplots in matrix.
			double[] xAxisExtents = source.getXAxisExtents();
			double[] yAxisExtents = source.getYAxisExtents();
			int pos = 0;
			for (int k = 0; k < plotNumber * plotNumber; k++) {
				DecisionTreeScatterPlot otherElement = scatterPlot[k];
				// Don't recall the scatterplot which generated the original
				// event
				if (otherElement == source) {
					pos = k;
					logger.finest("Element: " + pos);
					break;
				}
			}
			// Find the elements which need to reset the axis extents.
			int r = pos / plotNumber;
			int c = pos % plotNumber;
			logger.finest("Position: " + r + " " + c);
			for (int i = 0; i < plotNumber; i++) {
				logger.finest("xAxis: " + (i * plotNumber + c) + " "
						+ (c * plotNumber + i));
				scatterPlot[i * plotNumber + c].setXAxisExtents(xAxisExtents);// The
				// xAxisExtents
				// for
				// all
				// of
				// the
				// elements
				// in
				// the
				// same
				// column
				// have
				// to
				// be
				// changed.
				scatterPlot[c * plotNumber + i].setYAxisExtents(xAxisExtents);// The
				// yAxisExtents
				// for
				// the
				// corresponding
				// elements
				// on
				// the
				// other
				// side
				// of diagonal need to be changed.
				logger.finest("yAxis: " + (r * plotNumber + i) + " "
						+ (r + i * plotNumber));
				scatterPlot[r * plotNumber + i].setYAxisExtents(yAxisExtents); // The
				// yAxisExtents
				// for
				// all
				// of
				// the
				// elements
				// in
				// the
				// same
				// row
				// have
				// to
				// be
				// changed.
				scatterPlot[r + i * plotNumber].setXAxisExtents(yAxisExtents);// The
				// xAxisExtents
				// for
				// the
				// corresponding
				// elements
				// on
				// the
				// other
				// side
				// of diagonal need to be changed.
			}
		} else {
			System.err.println("Unknown command! = " + command);

		}// end if
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
		logger.finest("attribute selection dialog..");
		attList.setSelectedIndices(plottedAttributes);
		if (dialog == null) {
			dialog = new JDialog(dummyFrame, "Attributes for Plot", true);
			JButton selectButton;
			JButton closeButton;
			dialog.setSize(150, 300);
			dialog.getContentPane().setLayout(new BorderLayout());
			attList
					.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			// attList.addListSelectionListener(this);
			// JScrollPane scrollPane = new JScrollPane(attList);
			dialogPane = new JScrollPane(attList);
			selectButton = new JButton("Apply");
			selectButton.addActionListener(new java.awt.event.ActionListener() {

				/**
				 * put your documentation comment here
				 * 
				 * @param e
				 */
				public void actionPerformed(ActionEvent e) {
					logger.finest("about to press button Select");
					selectButton_actionPerformed(e);
					logger.finest("after pressed button Select");
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
			dialog.getContentPane().add(new JLabel("Attribute Names:"),
					BorderLayout.NORTH);
			dialog.getContentPane().add(dialogPane, BorderLayout.CENTER);
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
	 * Selection button in variable selection dialog. Click on it, the variables
	 * selected in the JList will be forwarded to matrix.
	 * 
	 * @param e
	 */
	private void selectButton_actionPerformed(ActionEvent e) {
		logger.finest("select button pressed, in action performed");
		plotNumber = plottedAttributes.length;
		logger.finest("select button pressed, plot number = " + plotNumber);
		init();
	}

	/**
	 * Close the variable selection dialog.
	 * 
	 * @param e
	 */
	private void closeButton_actionPerformed(ActionEvent e) {
		dialog.setVisible(false);
	}

	/**
	 * Inform that value or selection in the JList (variable selection dialog)
	 * has been changed.
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

	/**
	 * Shift columns or rows in the matrix.
	 * 
	 * @param lastPos
	 * @param newPos
	 */
	@Override
	protected void moveRowAndColumn(int lastPos, int newPos) {
		logger.finest("move row or column...");
		int indicesRow;
		int indicesCol;
		int[] indicesLast1;
		int[] indicesNew1;
		int[] indicesLast2;
		int[] indicesNew2;
		String varTagMoved = new String(varTags[lastPos - 1]);
		varTags[lastPos - 1] = varTags[newPos - 1];
		columnButton[lastPos - 1].setText(varTags[lastPos - 1]);
		rowButton[lastPos - 1].setText(varTags[lastPos - 1]);
		varTags[newPos - 1] = varTagMoved;
		columnButton[newPos - 1].setText(varTags[newPos - 1]);
		rowButton[newPos - 1].setText(varTags[newPos - 1]);
		for (int i = 0; i < plotNumber; i++) {
			indicesLast1 = (scatterPlot[i * plotNumber + lastPos - 1]
					.getElementPosition());
			indicesLast2 = (scatterPlot[(lastPos - 1) * plotNumber + i]
					.getElementPosition());
			logger.finest("Indices before move" + indicesLast1[0]
					+ indicesLast1[1]);
			indicesRow = indicesLast1[0];
			indicesCol = indicesLast2[1];
			indicesNew1 = scatterPlot[i * plotNumber + newPos - 1]
					.getElementPosition();
			indicesNew2 = scatterPlot[(newPos - 1) * plotNumber + i]
					.getElementPosition();
			logger.finest("Indices after move" + indicesNew1[0]
					+ indicesNew1[1]);
			indicesLast1[0] = indicesNew1[0];
			indicesLast2[1] = indicesNew2[1];
			scatterPlot[i * plotNumber + lastPos - 1]
					.setDataIndices(indicesLast1);
			scatterPlot[(lastPos - 1) * plotNumber + i]
					.setDataIndices(indicesLast2);
			indicesNew1[0] = indicesRow;
			indicesNew2[1] = indicesCol;
			scatterPlot[i * plotNumber + newPos - 1]
					.setDataIndices(indicesNew1);
			scatterPlot[(newPos - 1) * plotNumber + i]
					.setDataIndices(indicesNew2);

			/*
			 * this.remove((Component)this.element[i*plotNumber + lastPos - 1]);
			 * this.add((Component)this.element[i*plotNumber + newPos - 1],
			 * i*plotNumber + lastPos - 1);
			 * this.remove((Component)this.element[i*plotNumber + newPos - 1]);
			 * this.add((Component)this.element[i*plotNumber + lastPos - 1],
			 * i*plotNumber + newPos - 1);
			 * this.remove((Component)this.element[(lastPos - 1)*plotNumber +
			 * i]); add((Component)this.element[(newPos - 1)*plotNumber + i],
			 * (lastPos - 1)*plotNumber + i);
			 * this.remove((Component)this.element[(newPos - 1)*plotNumber +
			 * i]); add((Component)this.element[(lastPos - 1)*plotNumber + i],
			 * (newPos - 1)*plotNumber + i); this.revalidate(); repaint();
			 */
		}

	}

	public void setDecisionTree(Vector decisionTree) {
		logger.finest("in matrix, set decision tree...");
		this.decisionTree = decisionTree;
		if (this.decisionTree == null) {
			logger.finest("decision tree null...");
			return;
		}
		// Once selection from other components has been set, pass it to each
		// element inside of matrix.
		for (int k = 0; k < plotNumber * plotNumber; k++) {

			DecisionTreeScatterPlot otherElement = scatterPlot[k];
			if (otherElement == null) {
				return;
			}
			otherElement.setTreeVector(this.decisionTree);
		}
	}

	public Vector getDecisionTree() {
		return decisionTree;
	}

	/**
	 * adds an ActionListener to the button
	 */
	public void addActionListener(ActionListener l) {
		listenerListAction.add(ActionListener.class, l);
	}

	/**
	 * removes an ActionListener from the button
	 */
	public void removeActionListener(ActionListener l) {
		listenerListAction.remove(ActionListener.class, l);
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireActionPerformed() {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerListAction.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		ActionEvent e2 = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
				"OK");
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				// Lazily create the event:
				((ActionListener) listeners[i + 1]).actionPerformed(e2);
			}
		}
	}
}
