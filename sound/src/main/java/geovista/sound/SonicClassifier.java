/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.sound;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import geovista.common.classification.Classifier;
import geovista.common.classification.ClassifierPicker;
import geovista.common.color.Palette;
import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.IndicationEvent;
import geovista.common.event.IndicationListener;
import geovista.common.event.PaletteEvent;
import geovista.common.event.PaletteListener;

public class SonicClassifier extends JPanel implements ActionListener,
		ComponentListener, PaletteListener, IndicationListener, DataSetListener {
	protected final static Logger logger = Logger
			.getLogger(SonicClassifier.class.getName());

	private SonicRampPicker sonificationPanel;
	// XXX in future, we want to support much beyond colors.
	private transient Color[] colors;
	private transient Color[] dataColors;
	public transient boolean[] anchored;

	private transient int[] classificationIndex;

	private int nClasses;
	private boolean update;
	private boolean interpolate;
	private boolean setupFinished;

	private transient JCheckBox updateBox;
	private transient JCheckBox interpolateBox;
	public static final String COMMAND_COLORS_CHANGED = "colors";
	public static final String COMMAND_BEAN_REGISTERED = "hi!";

	private int currOrientation = SonicClassifier.X_AXIS;
	public static final int X_AXIS = 0;
	public static final int Y_AXIS = 1;
	public static final int DEFAULT_N_CLASSES = 8;
	public transient boolean orientationInParentIsX = false;

	private ClassifierPicker classPick;

	private transient TexturePaint texPaint;
	private transient int oldIndication = 0;
	private transient int[] currClassification;

	public SonicClassifier() {
		super();
		addComponentListener(this);

		setupFinished = false;
		nClasses = SonicRampPicker.DEFAULT_NUM_SWATCHES;

		update = true;
		interpolate = true;

		classPick = new ClassifierPicker();

		classPick.addActionListener(this);

		makeSymbolizationPanel();
		classPick.setNClasses(nClasses);

		this.add(classPick);
		// this.add(classificationPanel);
		this.add(sonificationPanel);
		setupFinished = true;
		makeColors();
		setVariableChooserMode(ClassifierPicker.VARIABLE_CHOOSER_MODE_ACTIVE);
		setPreferredSize(new Dimension(300, 40));
		/*
		 * int prefHeight = (int)this.classPick.getPreferredSize().getHeight();
		 * prefHeight = prefHeight +
		 * (int)this.sonificationPanel.getPreferredSize().getHeight(); int
		 * prefWidth = (int)this.classPick.getPreferredSize().getWidth(); if
		 * (prefWidth >
		 * (int)this.sonificationPanel.getPreferredSize().getWidth()) {
		 * prefWidth =
		 * (int)this.sonificationPanel.getPreferredSize().getWidth(); //take the
		 * smaller one } this.setPreferredSize(new Dimension(prefWidth,
		 * prefHeight));
		 */
		revalidate();
		makeTexPaint();
	}

	private void makeTexPaint() {

		int texSize = 4;
		Rectangle2D.Float rect = new Rectangle2D.Float(0, 0, texSize, texSize);
		BufferedImage buff = new BufferedImage(texSize, texSize,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = buff.createGraphics();
		Color trans = new Color(0, 0, 0, 0); // clear black
		g2.setColor(trans);
		g2.fillRect(0, 0, texSize, texSize);
		g2.setColor(Color.blue);
		g2.drawLine(0, 0, 32, 32);

		texPaint = new TexturePaint(buff, rect);

	}

	private void makeSymbolizationPanel() {
		sonificationPanel = new SonicRampPicker();
		this.add(sonificationPanel);
		sonificationPanel.addActionListener(this);
	}

	private void makeColors() {
		Color[] pickerColors = sonificationPanel.getColors();
		boolean[] pickerAnchors = sonificationPanel.getAnchored();
		// now we interpolate...linearly
		int nPicks = pickerColors.length;
		double picksPerColor = (double) nPicks / (double) nClasses;
		if (colors == null) {
			colors = new Color[nClasses];
		} else if (colors.length != nClasses) {
			colors = new Color[nClasses];
		}

		for (int i = 0; i < nClasses; i++) {
			double whichColor = i * picksPerColor;
			int index = (int) Math.floor(whichColor);
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("i = " + i + "index  = " + index);
			}
			colors[i] = pickerColors[index];

		}
		if (interpolate) {
			// for each lock in the picker, find the class that is closest.
			double colorsPerPick = (double) (nClasses - 1)
					/ (double) (nPicks - 1);
			// int[] newAnchors = new int[pickerAnchors.length];
			Vector newAnchors = new Vector();
			for (int i = 0; i < pickerAnchors.length; i++) {
				double whichClass = i * colorsPerPick;
				int aClass = (int) Math.round(whichClass);
				if (pickerAnchors[i]) {
					Integer Ind = new Integer(aClass);
					newAnchors.add(Ind);
				}
			}

			boolean[] colorAnchors = new boolean[nClasses];
			if (newAnchors.size() > 2) {
				for (Enumeration e = newAnchors.elements(); e.hasMoreElements();) {
					Integer ind = (Integer) e.nextElement();
					colorAnchors[ind.intValue()] = true;
				}

				colorAnchors[0] = true;
				colorAnchors[colorAnchors.length - 1] = true;
			} else if (newAnchors.size() == 2) {
				colorAnchors[0] = true;
				colorAnchors[colorAnchors.length - 1] = true;
			} else if (newAnchors.size() == 1) {
				colorAnchors[0] = true;
			}
			// now find those durn colors!
			sonificationPanel.getRamp().rampColors(colors, colorAnchors);

		}// end if interpolate
	}

	public Color[] findDataColors() {// side effect: finds and records
		// classification
		if (classPick.getDataSet() == null) {
			return null;
		}
		int currVar = classPick.getCurrVariableIndex();
		Classifier classer = classPick.getClasser();
		double[] data = classPick.getDataSet().getNumericDataAsDouble(currVar);
		int[] classedData = classer.classify(data, classPick.getNClasses());
		currClassification = classedData;
		if (dataColors == null || dataColors.length != classedData.length) {
			dataColors = new Color[classedData.length];
		}
		colors = sonificationPanel.getColors();
		for (int i = 0; i < classedData.length; i++) {
			int aClass = classedData[i];
			if (aClass < 0) {
				aClass = 0;// XXX egregious hack
			}
			Color c = colors[aClass];
			dataColors[i] = c;
		}
		return dataColors;
	}

	/** Listens to the check boxen. */
	class CheckBoxListener implements ItemListener {
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource().equals(updateBox)) {
				if (e.getStateChange() == ItemEvent.SELECTED && setupFinished) {
					update = true;
					// hack alertXXX combine these next two lines
					findDataColors();
					makeColors();

					fireActionPerformed(SonicClassifier.COMMAND_COLORS_CHANGED);

				} else if (e.getStateChange() == ItemEvent.DESELECTED) {

					update = false;
				}
			} else if (e.getSource().equals(interpolateBox)) {
				if (e.getStateChange() == ItemEvent.SELECTED && setupFinished) {
					interpolate = true;
					// hack alertXXX combine these next two lines
					findDataColors();
					makeColors();
					fireActionPerformed(SonicClassifier.COMMAND_COLORS_CHANGED);

				} else if (e.getStateChange() == ItemEvent.DESELECTED) {
					interpolate = false;
					// hack alertXXX combine these next two lines
					findDataColors();
					makeColors();
					fireActionPerformed(SonicClassifier.COMMAND_COLORS_CHANGED);

				}
			}
		}
	}

	class SliderListener implements ChangeListener {
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider) e.getSource();
			// if (!source.getValueIsAdjusting()) {

			int fps = source.getValue();
			if (fps == 0) {
				// if (!frozen) stopAnimation();
			} else {
				sonificationPanel.setNSwatches(fps);
				nClasses = fps;
				// hack alertXXX combine these next two lines
				findDataColors();
				makeColors();
				fireActionPerformed(SonicClassifier.COMMAND_COLORS_CHANGED);

			}

		}
	}

	public SonicRampPicker getSymbolizationPanel() {
		return sonificationPanel;
	}

	public void setSymbolizationPanel(SonicRampPicker sonificationPanel) {
		this.sonificationPanel = sonificationPanel;
		makeColors();
		logger.finest("Sonic Classifier, settingSymbolizationPanel");
	}

	public void playObservationKey(int obs) {
		if (obs < 0) {
			getSymbolizationPanel().playKey(obs); // could be null
			return;
		}
		if (currClassification == null) {
			findDataColors();
		} else if (currClassification == null) {
			return;
		} else if (obs >= currClassification.length) {
			return;
		}
		int aClass = currClassification[obs];
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("aClass = " + aClass);
		}
		if (aClass < 0) {
			return;// null class, maybe play special sound?
		}
		getSymbolizationPanel().playKey(aClass);

	}

	public void indicationChanged(IndicationEvent e) {
		int ind = e.getIndication();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("indication = " + ind);
		}
		if (ind >= 0) {
			playObservationKey(ind);
		}

	}

	public void dataSetChanged(DataSetEvent e) {
		setDataSet(e.getDataSetForApps());
	}

	public Color[] getColorForObservations() {
		return findDataColors();
	}

	public Color[] getColors() {
		logger.finest("VC, gettingColors");
		colors = sonificationPanel.getColors();
		return colors;
	}

	public void setColors(Color[] colors) {
		this.colors = colors;
	}

	public void paletteChanged(PaletteEvent e) {
		Palette pal = e.getPalette();
		int maxColors = pal.getRecommendedMaxLength();
		logger.finest("max colors = " + maxColors);

		// we can't go over the max, or it blows null exceptions
		// so, we interpolate....

		int numClasses = classPick.getNClasses();
		Color[] cols = null;
		if (numClasses <= maxColors) {
			cols = pal.getColors(numClasses);
		} else {
			// we have more colors wanted than the pallet can give us
			// todo: make this work
			// this.assignColors
			cols = pal.getColors(numClasses);
		}

		setColors(cols);
		sonificationPanel.setColors(cols);
		revalidate();
		this.repaint();
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command == SonicRampPicker.COMMAND_SWATCH_COLOR_CHANGED) {
			// hack alertXXX combine these next two lines
			findDataColors();
			makeColors();
			fireActionPerformed(SonicClassifier.COMMAND_COLORS_CHANGED);

		} else if (command == ClassifierPicker.COMMAND_CLASSES_CHANGED) {
			int nClasses = classPick.getNClasses();
			sonificationPanel.setNSwatches(nClasses);
			this.nClasses = nClasses;
			// hack alertXXX combine these next two lines
			findDataColors();
			makeColors();
			fireActionPerformed(SonicClassifier.COMMAND_COLORS_CHANGED);

		} else if (command == ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED) {

			fireActionPerformed(ClassifierPicker.COMMAND_SELECTED_VARIABLE_CHANGED);
			// hack alertXXX combine these next two lines
			findDataColors();

		} else if (command == ClassifierPicker.COMMAND_SELECTED_CLASSIFIER_CHANGED) {
			// this.colorClasser.setColorer(this.colorerLinear);

			// hack alertXXX combine these next two lines
			SonicClassifier.this.findDataColors();

		}

		// need to pass this along, if we are a FoldupPanel
		// super.actionPerformed(e);
	}

	// start component event handling
	// note: this class only listens to itself
	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
	}

	public void componentResized(ComponentEvent e) {
		double pickPrefWidth = classPick.getPreferredSize().getWidth();

		int prefWidth = (int) (pickPrefWidth * 1.5);
		if (getWidth() >= prefWidth) {
			changeOrientation(SonicClassifier.X_AXIS);
		} else {
			changeOrientation(SonicClassifier.Y_AXIS);
		}

	}

	// end component event handling

	public void changeOrientation(int orientation) {
		if (orientation == currOrientation) {
			return;
		} else if (orientation == SonicClassifier.X_AXIS) {
			Component[] comps = new Component[getComponentCount()];
			for (int i = 0; i < getComponentCount(); i++) {
				comps[i] = getComponent(i);
			}
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			for (int i = 0; i < getComponentCount(); i++) {
				this.add(comps[i]);
			}
			currOrientation = SonicClassifier.X_AXIS;
			setPreferredSize(new Dimension(300, 20));
			revalidate();
		} else if (orientation == SonicClassifier.Y_AXIS) {
			Component[] comps = new Component[getComponentCount()];
			for (int i = 0; i < getComponentCount(); i++) {
				comps[i] = getComponent(i);
			}
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			for (int i = 0; i < getComponentCount(); i++) {
				this.add(comps[i]);
			}
			currOrientation = SonicClassifier.Y_AXIS;
			setPreferredSize(new Dimension(300, 40));
			revalidate();
		}

	}

	/**
	 * implements ActionListener
	 */
	public void addActionListener(ActionListener l) {
		listenerList.add(ActionListener.class, l);
		fireActionPerformed(SonicClassifier.COMMAND_BEAN_REGISTERED);
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
	private void fireActionPerformed(String command) {
		if (update) {
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
			}// next i
		}// end if
	}

	public void setIndicatedClass(int indicClass) {
		// clear old indication
		// if it still exists
		if (oldIndication < sonificationPanel.getPanSet().length) {
			sonificationPanel.getPanSet()[oldIndication].setTexPaint(null);
		}

		if (indicClass < 0) { // used for null or out of range indication
			this.repaint(); // clear old indication
			return;
		}
		if (indicClass >= sonificationPanel.getPanSet().length) {
			this.repaint(); // clear old indication
			return;
		}

		sonificationPanel.getPanSet()[indicClass].setTexPaint(texPaint);
		oldIndication = indicClass;
		this.repaint();
	}

	public boolean[] getAnchored() {
		return anchored;
	}

	public void setAnchored(boolean[] anchored) {
		this.anchored = anchored;
	}

	public int[] getClassificationIndex() {
		return classificationIndex;
	}

	public void setClassificationIndex(int[] classificationIndex) {
		this.classificationIndex = classificationIndex;
	}

	public ClassifierPicker getClassPick() {
		return classPick;
	}

	public void setClassPick(ClassifierPicker classPick) {
		this.classPick = classPick;
	}

	public int getCurrOrientation() {
		return currOrientation;
	}

	public void setCurrOrientation(int currOrientation) {
		this.currOrientation = currOrientation;
	}

	public int getVariableChooserMode() {
		return classPick.getVariableChooserMode();
	}

	public void setVariableChooserMode(int variableChooserMode) {
		if (classPick.getVariableChooserMode() != variableChooserMode) {
			classPick.setVariableChooserMode(variableChooserMode);

		}
	}

	public void setCurrVariableIndex(int index) {
		classPick.setCurrVariableIndex(index);
	}

	public void setDataSet(DataSetForApps data) {
		classPick.setDataSet(data);
		// hack alertXXX combine these next two lines
		findDataColors();

	}

	public int getCurrVariableIndex() {
		return classPick.getCurrVariableIndex();
	}

	public void setHighColor(Color c) {
		sonificationPanel.setHighColor(c);
	}

	public void setOrientationInParentIsX(boolean orientationInParentIsX) {
		this.orientationInParentIsX = orientationInParentIsX;
	}

}
