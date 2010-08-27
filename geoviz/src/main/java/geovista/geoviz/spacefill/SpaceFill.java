/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.geoviz.spacefill;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

import geovista.common.data.DataSetForApps;
import geovista.common.event.ConditioningEvent;
import geovista.common.event.ConditioningListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.common.ui.FoldupPanel;
import geovista.geoviz.visclass.VisualClassifier;
import geovista.symbolization.BivariateColorSymbolClassification;

public class SpaceFill extends JPanel implements ActionListener,
		SelectionListener, IndicationListener, DataSetListener,
		ConditioningListener {

	private final SpaceFillCanvas spatCan;
	private final VisualClassifier vc;

	private String[] variableNames;

	private JPanel topPane;
	private final FoldupPanel allTop;
	private JComboBox colorColumnCombo;
	private JComboBox orderColumnCombo;
	private JComboBox fillOrderCombo;
	private int fillOrder;
	private boolean useDrawingShapes;

	private transient String currColorName;
	private transient String currOrderName;

	public SpaceFill() {
		useDrawingShapes = true;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setLayout(new BorderLayout());
		allTop = new FoldupPanel();
		allTop.getContentPanel().setLayout(
				new BoxLayout(allTop.getContentPanel(), BoxLayout.Y_AXIS));
		topPane = makeTopPanel();
		topPane.setPreferredSize(new Dimension(200, 30));
		vc = new VisualClassifier();
		allTop.getContentPanel().add(vc);

		vc.addActionListener(this);

		allTop.setPreferredSize(new Dimension(200, 50));
		allTop.getContentPanel().add(topPane);
		this.add(allTop, BorderLayout.NORTH);
		spatCan = makeSpat();
		spatCan.addActionListener(this);
		// spatCan.setPreferredSize(new Dimension(2000,2000));
		spatCan.setMaximumSize(new Dimension(20000, 20000));
		this.add(spatCan, BorderLayout.CENTER);
		Color[] colors = vc.getColors();
		spatCan.setColors(colors);

		spatCan.setColorSymbolizer(vc.getColorSymbolClassification());
		setPreferredSize(new Dimension(400, 400));

		// orders to implement (in order): scan, bostrophedon, spiral, Morton,
		// Peano
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals(VisualClassifier.COMMAND_COLORS_CHANGED)) {
			spatCan.setColorSymbolizer(vc.getColorSymbolClassification());
			fireActionPerformed(SpaceFillCanvas.COMMAND_COLOR_CLASSFICIATION);
			// this.spat.setColors(colors);
		}
		if (command.equals(SpaceFillCanvas.COMMAND_SELECTION)
				&& e.getSource() == spatCan) {
			// pass it along
			fireActionPerformed(SpaceFillCanvas.COMMAND_SELECTION);
			fireSelectionChanged(spatCan.getSelectedObservationsInt());
		}

	}

	private JPanel makeTopPanel() {

		JPanel topPanel = new JPanel();
		colorColumnCombo = new JComboBox();
		colorColumnCombo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if (cb.getItemCount() > 0) {
					String arrayName = (String) cb.getSelectedItem();

					if (arrayName.equals(currColorName) == false) {
						currColorName = arrayName;
						int index = cb.getSelectedIndex();
						SpaceFill.this.setCurrColorColumn(index + 1); // skip
						// header
						// + 1
					}// end if
				}// end if count > 0
			}// end inner class
		});// end add listener
		JLabel labelColor = new JLabel("Color by:");

		topPanel.add(labelColor, BorderLayout.NORTH);
		topPanel.add(colorColumnCombo, BorderLayout.NORTH);
		JLabel someSpace = new JLabel("    ");
		topPanel.add(someSpace, BorderLayout.NORTH);

		orderColumnCombo = new JComboBox();
		JLabel labelOrder = new JLabel("Order by:");
		orderColumnCombo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if (cb.getItemCount() > 0) {
					String arrayName = (String) cb.getSelectedItem();

					if (arrayName.equals(currOrderName) == false) {
						currOrderName = arrayName;
						int index = cb.getSelectedIndex();
						SpaceFill.this.setCurrOrderColumn(index + 1);// skip
						// header
					}// end if
				}// end if count > 0
			}// end inner class
		});// end add listener
		topPanel.add(labelOrder, BorderLayout.EAST);
		topPanel.add(orderColumnCombo, BorderLayout.EAST);

		fillOrderCombo = new JComboBox();
		JLabel labelFill = new JLabel("Fill Order:");
		String[] desc = FillOrder.findFillOrderDescriptions();
		for (String element : desc) {
			fillOrderCombo.addItem(element);
		}
		fillOrderCombo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if (cb.getItemCount() > 0) {
					int fillOrd = cb.getSelectedIndex();
					// if (SpaceFill.this.fillOrder != fillOrd){
					SpaceFill.this.setFillOrder(fillOrd);
					// }//end if
				}// end if count > 0
			}// end inner class
		});// end add listener
		topPanel.add(labelFill, BorderLayout.EAST);
		topPanel.add(fillOrderCombo, BorderLayout.EAST);

		JCheckBox useShapesCB = new JCheckBox("Use Shapes?", true);
		useShapesCB.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					SpaceFill.this.setUseDrawingShapes(true);
				} else {
					SpaceFill.this.setUseDrawingShapes(false);
				}
			}// end itemStateChanged

		});// end add listener
		topPanel.add(useShapesCB, BorderLayout.EAST);

		topPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		return topPanel;
	}// end method

	private SpaceFillCanvas makeSpat() {
		SpaceFillCanvas spat = new SpaceFillCanvas();
		// spatCan.setPreferredSize(new Dimension(400,350));
		return spat;
	}

	/**
	 * @param data
	 * 
	 *            This method is deprecated becuase it wants to create its very
	 *            own pet DataSetForApps. This is no longer allowed, to allow
	 *            for a mutable, common data set. Use of this method may lead to
	 *            unexpected program behavoir. Please use setDataSet instead.
	 */
	@Deprecated
	public void setData(Object[] data) {
		setDataSet(new DataSetForApps(data));

	}

	// start accessors
	public void setDataSet(DataSetForApps dataSet) {
		Color[] colors = vc.getColors();
		spatCan.setColors(colors);

		spatCan.setColorSymbolizer(vc.getColorSymbolClassification());
		setVariableNames(dataSet.getAttributeNamesNumeric());
		spatCan.setDataSet(dataSet);
		if (dataSet.getNumberNumericAttributes() > 2) {
			colorColumnCombo.setSelectedIndex(1);
			orderColumnCombo.setSelectedIndex(0);
		}

		spatCan.setObservationNames(dataSet.getObservationNames());
	}

	public Object[] getData() {
		return spatCan.getData();
	}

	public void setCurrOrderColumn(int currOrderColumn) {
		orderColumnCombo.setSelectedIndex(currOrderColumn - 1);

		spatCan.setCurrOrderColumn(currOrderColumn);
	}

	public int getCurrOrderColumn() {
		return spatCan.getCurrOrderColumn();
	}

	public void setCurrColorColumn(int currColorColumn) {
		colorColumnCombo.setSelectedIndex(currColorColumn - 1);
		spatCan.setCurrColorColumn(currColorColumn);
	}

	public int getCurrColorColumn() {
		return spatCan.getCurrColorColumn();
	}

	public void setColorSelection(Color colorSelection) {
		spatCan.setColorSelection(colorSelection);
	}

	public Color getColorSelection() {
		return spatCan.getColorSelection();
	}

	public void setColorIndication(Color colorIndication) {
		spatCan.setColorIndication(colorIndication);
	}

	public Color getColorIndication() {
		return spatCan.getColorIndication();
	}

	public void setColorNull(Color colorNull) {
		spatCan.setColorNull(colorNull);
	}

	public Color getColorNull() {
		return spatCan.getColorNull();
	}

	public void setColorOutOfFocus(Color colorOutOfFocus) {
		spatCan.setColorOutOfFocus(colorOutOfFocus);
	}

	public Color getColorOutOfFocus() {
		return spatCan.getColorOutOfFocus();
	}

	public void setColorNotInStudyArea(Color colorNotInStudyArea) {
		spatCan.setColorNotInStudyArea(colorNotInStudyArea);
	}

	public Color getColorNotInStudyArea() {
		return spatCan.getColorNotInStudyArea();
	}

	public void setVariableNames(String[] variableNames) {

		this.variableNames = variableNames;
		spatCan.setVariableNames(variableNames);
		colorColumnCombo.removeAllItems();
		orderColumnCombo.removeAllItems();
		for (String element : variableNames) {
			colorColumnCombo.addItem(element);
			orderColumnCombo.addItem(element);
		}
	}

	public String[] getVariableNames() {
		return variableNames;
	}

	public void setSelectedObservations(Vector selectedObservations) {
		spatCan.setSelectedObservations(selectedObservations);
	}

	public Vector getSelectedObservations() {
		return spatCan.getSelectedObservations();
	}

	public void setSelectedObservationsInt(int[] selectedObservations) {
		spatCan.setSelectedObservationsInt(selectedObservations);
	}

	public void selectionChanged(SelectionEvent e) {
		int[] sel = e.getSelection();
		setSelectedObservationsInt(sel);
	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, spatCan.getSelectedObservationsInt());
	}

	public void indicationChanged(IndicationEvent e) {
		int indic = e.getIndication();
		if (indic < 0) {
			return;
		}
		spatCan.setIndication(indic);
	}

	public void dataSetChanged(DataSetEvent e) {
		vc.dataSetChanged(e);
		setDataSet(e.getDataSetForApps());
	}

	public void conditioningChanged(ConditioningEvent e) {
		spatCan.setConditionArray(e.getConditioning());
	}

	public int[] getSelectedObservationsInt() {
		return spatCan.getSelectedObservationsInt();
	}

	public void setTopPane(FoldupPanel topPane) {
		this.topPane = topPane;
	}

	public JPanel getTopPane() {
		return topPane;
	}

	public void setColorColumnCombo(JComboBox colorColumnCombo) {
		this.colorColumnCombo = colorColumnCombo;
	}

	public JComboBox getColorColumnCombo() {
		return colorColumnCombo;
	}

	public void setOrderColumnCombo(JComboBox orderColumnCombo) {
		this.orderColumnCombo = orderColumnCombo;
	}

	public JComboBox getOrderColumnCombo() {
		return orderColumnCombo;
	}

	public void setFillOrder(int fillOrder) {
		if (this.fillOrder != fillOrder) {
			if (fillOrder > FillOrder.FILL_ORDER_MAX || fillOrder < 0) {
				throw new IllegalArgumentException(
						"Fill order outside legal range defined in FillOrder");
			}
			this.fillOrder = fillOrder;
			spatCan.setFillOrder(fillOrder);
		}//
	}// end method

	public int getFillOrder() {
		return fillOrder;
	}

	public void setUseDrawingShapes(boolean useDrawingShapes) {
		this.useDrawingShapes = useDrawingShapes;
		spatCan.setUseDrawingShapes(useDrawingShapes);
	}

	public boolean getUseDrawingShapes() {
		return useDrawingShapes;
	}

	public BivariateColorSymbolClassification getBivarColorClasser() {
		return spatCan.getBivarColorClasser();
	}

	// end accessors

	/**
	 * adds an ActionListener to the component
	 */
	public void addActionListener(ActionListener l) {
		listenerList.add(ActionListener.class, l);
	}

	/**
	 * removes an ActionListener from the component
	 */
	public void removeActionListener(ActionListener l) {
		listenerList.remove(ActionListener.class, l);
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireActionPerformed(String command) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		ActionEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
							command);
				}
				((ActionListener) listeners[i + 1]).actionPerformed(e);
			}
		}
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

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
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

}
