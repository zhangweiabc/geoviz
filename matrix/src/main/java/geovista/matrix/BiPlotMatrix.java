/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.matrix;

/**
 * Title: Mixed Matrix Description: Manipulable Matrix Copyright: Copyright (c)
 * 2001 Company: GeoVISTA Center
 *
 * @author Xiping Dai
 *
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;

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
import javax.swing.event.ListSelectionListener;

import geovista.symbolization.BivariateColorClassifierOneColor;

public class BiPlotMatrix extends AbstractMatrix {

	private Color foreground = Color.white;
	protected EventListenerList listenerList = new EventListenerList();
	private final Class[] elementClasses = new Class[2];
	private final String[] elementClassNames = new String[2];
	private transient BivariateColorClassifierOneColor bivarOneColor;
	protected ImageIcon matrixIcon = new ImageIcon(
			BiPlotMatrix.class.getResource("resources/matrixicon16.gif"));

	public BiPlotMatrix() {
		super();

	}

	/**
	 * put your documentation comment here
	 * 
	 * @param classname
	 */
	public void setElementClassName1(String classname) {
		elementClassNames[0] = classname;
		try {
			setElementClass1((elementClassNames[0] != null) ? Class
					.forName(elementClassNames[0]) : null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setElementClassName2(String classname) {
		elementClassNames[1] = classname;
		try {
			setElementClass2((elementClassNames[1] != null) ? Class
					.forName(elementClassNames[1]) : null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * put your documentation comment here
	 * 
	 * @return
	 */
	public String getElementClassName1() {
		return elementClassNames[0];
	}

	public String getElementClassName2() {
		return elementClassNames[1];
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param obj
	 */
	public void setElementClass1(Object obj) {
		if (obj instanceof MatrixElementWrapper) {
			setElementClassName1(((MatrixElementWrapper) obj)
					.getMatrixElementClass().getName());
		} else {
			setElementClassName1((obj != null) ? obj.getClass().getName()
					: null);
		}
	}

	public void setElementClass2(Object obj) {
		if (obj instanceof MatrixElementWrapper) {
			setElementClassName2(((MatrixElementWrapper) obj)
					.getMatrixElementClass().getName());
		} else {
			setElementClassName2((obj != null) ? obj.getClass().getName()
					: null);
		}
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param clazz
	 */
	public void setElementClass1(Class clazz) {
		elementClasses[0] = clazz;
	}

	public void setElementClass2(Class clazz) {
		elementClasses[1] = clazz;
	}

	/**
	 * put your documentation comment here
	 */
	@Override
	protected synchronized void init() {

		logger.finest("get in init()...");
		if (!recreate) {
			return; // maybe display error message.
		}
		removeAll();
		panelWidthPixels = DEFAULT_PANEL_WIDTH_PIXELS;
		panelHeightPixels = DEFAULT_PANEL_HEIGHT_PIXELS;
		setPanelSize(panelWidthPixels, panelHeightPixels);

		bivarOneColor = new BivariateColorClassifierOneColor();
		int colorTotal = background.getRed() + background.getGreen()
				+ background.getBlue();
		int greyColor = 128 * 3;
		if (colorTotal < greyColor) {
			foreground = Color.white;
		} else {
			foreground = Color.black;
		}
		bivarOneColor.setOneColor(foreground);

		if (dataObject != null) {

			attList = new JList(attributesDisplay);
			if (attributeDescriptions != null) {
				descriptionList = new JList(attributeDescriptions);
			}
			element = new MatrixElement[plotNumber * plotNumber];
			matrixLayout = new GridBagLayout();
			c = new SPGridBagConstraints();
			// this.setLayout(matrixLayout);
			c.fill = GridBagConstraints.BOTH;
			varTags = new String[plotNumber];
			createMatrix();
			Container parent = getParent();
			if (parent != null) {
				parent.validate();
			} else {
				validate();
			}
		}
		// for indication
		super.registerIndicationListeners();
	}

	/**
	 * put your documentation comment here
	 */
	@Override
	protected void createMatrix() {
		logger.finest("create SP:");
		setLayout(matrixLayout);
		columnButton = new SPTagButton[plotNumber];
		rowButton = new SPTagButton[plotNumber];
		Dimension tagDm = new Dimension(0, 0);
		configButton = new JButton();
		try {
			for (int i = 0; i < plotNumber + 1; i++) {
				for (int j = 0; j < plotNumber + 1; j++) {
					if ((i == 0) && (j == 0)) {
						c.weightx = 0.0;
						c.weighty = 0.0;
						c.gridwidth = 1;
						c.gridheight = 1;

						configButton.setIcon(matrixIcon);
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
						c.weightx = 1.0;
						c.weighty = 0.0;
						c.gridwidth = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						c.gridheight = 1;
						if (j == plotNumber) {
							c.gridwidth = GridBagConstraints.REMAINDER;
						} // end row
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
						c.weightx = 0.0;
						c.weighty = 1.0;
						c.gridwidth = 1;
						c.gridheight = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						rowButton[i - 1] = new SPTagButton("s");
						rowButton[i - 1].setMargin(AbstractMatrix.nullInsets);
						rowButton[i - 1]
								.setVerticalAlignment(SwingConstants.BOTTOM);
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
						// BufferedImage(rowLabel.getWidth()+1,rowLabel.getHeight
						// ()+1,BufferedImage.TYPE_INT_ARGB);
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
						c.weightx = 1.0;
						c.weighty = 1.0;
						c.gridwidth = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						c.gridheight = AbstractMatrix.DEFAULT_BUTTON_CONSTRAINTS;
						int indexCurrent = (i - 1) * (plotNumber) + (j - 1);
						int[] dataIndices = new int[2];
						dataIndices[0] = plottedAttributes[j - 1];
						dataIndices[1] = plottedAttributes[i - 1];
						// construct of each element
						if (elementClasses[0] != null
								&& elementClasses[1] == null) {
							element[indexCurrent] = (MatrixElement) elementClasses[0]
									.newInstance();
						} else if (elementClasses[0] == null
								&& elementClasses[1] != null) {
							element[indexCurrent] = (MatrixElement) elementClasses[1]
									.newInstance();
						} else if (elementClasses[0] != null
								&& elementClasses[1] != null) {
							if (i <= j) {
								element[indexCurrent] = (MatrixElement) elementClasses[0]
										.newInstance();
							} else {
								element[indexCurrent] = (MatrixElement) elementClasses[1]
										.newInstance();
							}
						}
						element[indexCurrent].setAxisOn(false);
						element[indexCurrent].setDataSet(dataSet);

						element[indexCurrent].setBackground(background);
						element[indexCurrent].setDataIndices(dataIndices);
						element[indexCurrent]
								.setSelOriginalColorMode(selOriginalColorMode);
						element[indexCurrent].setSelectionColor(selectionColor);
						if ((bivarColorClasser != null)
								&& (!element[indexCurrent]
										.getClass()
										.getName()
										.equals("geovista.geoviz.scatterplot.ScatterPlot"))) {
							boolean reverseColor = false;
							if (i > j) {
								reverseColor = true;
							}
							element[indexCurrent].setBivarColorClasser(
									bivarColorClasser, reverseColor);
						}

						// if (colorArrayForObs != null) {
						// element[indexCurrent]
						// .setColorArrayForObs(colorArrayForObs);
						// }
						if (j == plotNumber) {
							c.gridwidth = GridBagConstraints.REMAINDER; // end
						}
						// row
						c.column = j;
						c.row = i;
						matrixLayout.setConstraints(
								(Component) element[indexCurrent], c);
						add((Component) element[indexCurrent]);
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
		Dimension rowTag = new Dimension((int) tagDm.getHeight(),
				(int) tagDm.getHeight());
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
		// ScatterPlot source = (ScatterPlot)e.getSource();
		MatrixElement source = (MatrixElement) e.getSource();
		String command = e.getActionCommand();
		if (command.compareTo(MatrixElement.COMMAND_POINT_SELECTED) == 0) {
			logger.finest("SPMC.plotUnitPanel.actionPerformed(), point selected");
			// selectedObvs = source.getSelectedObservations();
			selectedObvsInt = source.getSelections();
			for (int k = 0; k < plotNumber * plotNumber; k++) {
				MatrixElement otherElement = element[k];
				// Don't recall the scatterplot which generated the original
				// event
				if (otherElement != source) {
					// otherElement.setSelectedObservations(selectedObvs);
					otherElement.setSelections(selectedObvsInt);
				}
			}
			this.repaint();
			fireChangeEvent();
			fireSelectionChanged(getSelectedObvs());
		} else if (command
				.compareTo(MatrixElement.COMMAND_COLOR_CLASSFICIATION) == 0) {
			bivarColorClasser = source.getBivarColorClasser();
			boolean reverseColor = false;
			int row, column;
			for (int k = 0; k < plotNumber * plotNumber; k++) {
				MatrixElement otherElement = element[k];
				// Don't recall the scatterplot which generated the original
				// event
				if (otherElement != source) {
					row = k / plotNumber;
					column = k % plotNumber;
					if (row > column) {
						reverseColor = true;
					} else {
						reverseColor = false;
					}
					otherElement.setBivarColorClasser(bivarColorClasser,
							reverseColor);
				}
			}
			this.repaint();
			fireChangeEvent();
		} else {
			if (command.compareTo(MatrixElement.COMMAND_DATARANGE_SET) == 0) {
				logger.finest("SPMC.plotUnitPanel.actionPerformed(),data range");
				double[] xAxisExtents = source.getXAxisExtents();
				double[] yAxisExtents = source.getYAxisExtents();
				int pos = 0;
				for (int k = 0; k < plotNumber * plotNumber; k++) {
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
				logger.finest("pos: " + pos);
				for (int i = 0; i < plotNumber; i++) {
					element[i * plotNumber + c].setXAxisExtents(xAxisExtents);// The
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
					element[c * plotNumber + i].setYAxisExtents(xAxisExtents);// The
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
					element[r * plotNumber + i].setYAxisExtents(yAxisExtents); // The
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
					element[r + i * plotNumber].setXAxisExtents(yAxisExtents);// The
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
			}
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
	 * Construct the variable selection for displaying in the matrix.
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
			dialog.setSize(150, 300);
			dialog.getContentPane().setLayout(new BorderLayout());
			attList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
			JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
			buttonPanel.add(selectButton);
			buttonPanel.add(closeButton);

			JPanel attSelPanel = new JPanel(new BorderLayout());
			attSelPanel.setPreferredSize(new Dimension(150, 290));
			attSelPanel.add(new JLabel("Attribute Names:"), BorderLayout.NORTH);
			attSelPanel.add(dialogPane, BorderLayout.CENTER);
			attSelPanel.add(buttonPanel, BorderLayout.SOUTH);

			if (attributeDescriptions != null) {
				logger.finest("attributeDescription not null..");
				JPanel descPane = new JPanel(new BorderLayout());
				descPane.add(new JLabel("Descriptions:"), BorderLayout.NORTH);
				JScrollPane descListPane = new JScrollPane(descriptionList);
				descPane.add(descListPane, BorderLayout.CENTER);
				dialog.getContentPane().add(descPane, BorderLayout.CENTER);
			}
			dialog.getContentPane().add(attSelPanel, BorderLayout.WEST);
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
		super.fireSubspaceChanged(plottedAttributes);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @param e
	 */
	private void closeButton_actionPerformed(ActionEvent e) {
		dialog.setVisible(false);
	}

	/**
	 * put your documentation comment here
	 * 
	 * @see ListSelectionListener
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
}
