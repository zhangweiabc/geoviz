/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Xiping Dai */

package geovista.matrix.visclass;

/**
 * 
 * Description: Viasual classifier for multi-variables, such as K-means.
 * Copyright: Copyright (c) 2001 Company: GeoVISTA
 * 
 * @author Xiping Dai
 * 
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.EventListenerList;

import geovista.common.classification.Classifier;
import geovista.common.classification.ClassifierKMParameters;
import geovista.common.classification.ClassifierKMeans;
import geovista.common.classification.ClassifierMLParameters;
import geovista.common.classification.ClusterMixtureModels;
import geovista.common.data.DataSetForApps;
import geovista.common.data.TrainingData;
import geovista.common.event.ClassColorEvent;
import geovista.common.event.ClassColorListener;
import geovista.common.event.ClassNumberEvent;
import geovista.common.event.ClassNumberListener;
import geovista.common.event.ColorArrayEvent;
import geovista.common.event.ColorArrayListener;
import geovista.common.event.DataSetDoubleEvent;
import geovista.common.event.DataSetDoubleListener;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.MergeCategoryEvent;
import geovista.common.event.MergeCategoryListener;
import geovista.common.event.SelectionListener;
import geovista.readers.util.MyFileFilter;
import geovista.symbolization.ColorRampPicker;
import geovista.symbolization.ColorSymbolizer;

public class VisualClassifierMultiVariables extends JPanel implements
		ActionListener, ComponentListener, DataSetListener, ClassColorListener,
		MergeCategoryListener, ClassNumberListener, DataSetDoubleListener {
	protected final static Logger logger = Logger
			.getLogger(VisualClassifierMultiVariables.class.getName());
	private static final int DEFAULT_CLASS_NUMBER = 5;
	private ColorRampPicker colorPanel;
	// XXX in future, we want to support much beyond colors.
	private Color[] colors;
	public boolean[] anchored;

	private Object[] dataObject;// XXX get rid of this data structure
	private DataSetForApps dataSet;
	private int[] classificationIndex;
	private int[] classificationIndexTrain;
	private Color[] returnColors;

	private int nClasses;
	private boolean update;
	private boolean interpolate;
	private boolean setupFinished;

	private transient JCheckBox updateBox;
	private transient JCheckBox interpolateBox;
	private JPanel classPickerPanel;
	private JPanel colorPickerPanel;
	private JPanel classifyPanel;
	private JComboBox classifierBox;
	private JTextField classNumField;
	private JButton applyButton;
	private JButton detailButton;
	private JButton classifyTrainDataButton;
	private JButton classifyLoadDataButton;
	private JButton displayResultsButton;
	private JButton saveResultsButton;
	private String FileName;
	private String classifierName;
	private final ClassifierKMeans classifierKMeans;
	private final ClusterMixtureModels classifierMixureModels;
	private final ClassifierMaximumLikelihood classifierML;
	private int[] iniObsIdx = null;
	private int[] selectedAttIdx;
	private boolean visualDisplay = false;
	private JDialog kmParameterSetupDialog;
	private JDialog mlParameterSetupDialog;

	private ClassifierKMParameters kMParameterSetup;

	private ClassifierMLParameters mlParameterSetup;

	private TrainingData setUpTrainingData;
	private Vector[] trainingData;
	private String[] trainingClassLabels;
	private double[][] classifyData;

	public static final String COMMAND_COLORS_CHANGED = "colors";
	public static final String COMMAND_BEAN_REGISTERED = "hi!";
	private String classLabel;
	private Color classColor;

	// private int currOrientation = this.X_AXIS;
	// public static final int X_AXIS = 0;
	// public static final int Y_AXIS = 1;

	public VisualClassifierMultiVariables() {
		super();
		addComponentListener(this);
		setupFinished = false;
		nClasses = VisualClassifierMultiVariables.DEFAULT_CLASS_NUMBER;
		update = true;
		interpolate = true;
		// this.colorerLinear = new ColorSymbolizerLinear();
		classifierKMeans = new ClassifierKMeans();
		classifierMixureModels = new ClusterMixtureModels();
		classifierML = new ClassifierMaximumLikelihood();

		// this.setLayout(new BorderLayout());
		makeClassifierPanel();
		makeColorPickerPanel();
		makeClassifyPanel();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(classPickerPanel);
		// this.add(classificationPanel);
		this.add(colorPickerPanel);
		this.add(classifyPanel);
		setupFinished = true;
		// this.makeColors();
		setPreferredSize(new Dimension(350, 80));

		revalidate();

	}

	public void setDataObject(Object[] dataObject) {
		// this.dataObject = dataObject;
		DataSetForApps dataObjTransfer = new DataSetForApps(dataObject);// XXX
		// not
		// really
		// what
		// we
		// want.
		// Can't
		// get
		// updates
		// this.dataObject = dataObjTransfer.getDataSetNumericAndSpatial();
		this.dataObject = dataObjTransfer.getNamedArrays();

		setUpTrainingData = new TrainingData();
		if ((trainingData != null) && (nClasses == trainingData.length)) {
			setUpTrainingData.setNumberOfClasses(nClasses);
			setUpTrainingData.setDataObject(this.dataObject);
			trainingData = setUpTrainingData.getTrainingDataVector();
			trainingClassLabels = setUpTrainingData.getTrainingClassLabels();
		}
	}

	public void setClassifyData(double[][] data) {
		classifyData = data;
		mlcTuple();
	}

	public void setClassifyDataObject() {
		mlcDataObject();
	}

	public Color[] getClassificationColors() {
		return returnColors;
	}

	public int[] getClassification() {
		return classificationIndexTrain;
	}

	public void setClassNumber(int classNumber) {
		if (classNumber != nClasses) {
			nClasses = classNumber;
			classNumField.setText(Integer.toString(nClasses));
			makeColors();
		}
	}

	public void setClassColor(String classLabel, Color classColor) {
		this.classColor = classColor;
		this.classLabel = classLabel; // To do, need to generalize it to any
		// training labels.
		String[] classLabels = new String[nClasses];

		if (setUpTrainingData == null) {
			setUpTrainingData = new TrainingData();
			setUpTrainingData.setNumberOfClasses(nClasses);
			setUpTrainingData.setDataObject(dataObject);
		}
		classLabels = setUpTrainingData.getTrainingClassLabels();
		// int l = (new Integer(this.classLabel)).intValue();
		// if (l > this.nClasses){
		// return;
		// }
		boolean[] pickerAnchors = colorPanel.getAnchored();
		for (int i = 0; i < nClasses; i++) {
			if (classLabels[i].equals(this.classLabel)) {
				colors[i] = this.classColor;
				pickerAnchors[i] = true;
			}
		}

		// this.colors[l-1] = this.classColor;
		colorPanel.setColors(colors);
		// boolean[] pickerAnchors = this.colorPanel.getAnchored();
		// pickerAnchors[l-1] = true;
		colorPanel.setAnchored(pickerAnchors);
		VisualClassifierMultiVariables.this.makeColors();
		VisualClassifierMultiVariables.this
				.fireActionPerformed(VisualClassifierMultiVariables.COMMAND_COLORS_CHANGED);
		// this.mergeClasses(classLabel);
	}

	public void mergeClasses(String mergedClassLabels) {
		String mergedClassLabelString = new String();
		mergedClassLabelString = mergedClassLabels;
		if (setUpTrainingData == null) {
			setUpTrainingData = new TrainingData();
			setUpTrainingData.setNumberOfClasses(nClasses);
			setUpTrainingData.setDataObject(dataObject);
		}
		setUpTrainingData.setMegedClassLabels(mergedClassLabelString);
		trainingClassLabels = setUpTrainingData.getClassLabelsAfterMerge();
		trainingData = setUpTrainingData.getMergedTrainingDataVector();
		nClasses = trainingClassLabels.length;
		classNumField.setText(Integer.toString(nClasses));
		VisualClassifierMultiVariables.this.setColorPanel();
	}

	private void makeColorPickerPanel() {
		// Color picker and apply button of this classificaiton
		colorPickerPanel = new JPanel();
		colorPickerPanel.setLayout(new BoxLayout(colorPickerPanel,
				BoxLayout.X_AXIS));
		colorPanel = new ColorRampPicker();
		colorPanel.setNSwatches(nClasses);

		this.add(colorPanel);
		colorPanel.addActionListener(this);

		applyButton = new JButton("Apply");
		applyButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				applyButton_actionPerformed();
			}
		});
		classPickerPanel.add(applyButton);
	}

	private void makeClassifierPanel() {
		classPickerPanel = new JPanel();
		classPickerPanel.setLayout(new BoxLayout(classPickerPanel,
				BoxLayout.X_AXIS));
		classifierBox = new JComboBox();
		classifierBox.addItem("Classifiers:");
		classifierBox.addItem("K Means Clustering");
		classifierBox.addItem("MixtureModels Clustering");
		classifierBox.addItem("MaximumLikelihood Classifier");
		classifierBox.addItem("Decision Tree");
		classifierName = "Classifiers:";
		classifierBox.setMinimumSize(new Dimension(80, 20));
		classifierBox.setMaximumSize(new Dimension(120, 20));
		classifierBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				if (cb.getItemCount() > 0) {
					classifierName = (String) cb.getSelectedItem();
				}// end if count > 0
			}// end inner class
		});// end add listener
		// JLabel twoSpacesclassifCombo = new JLabel(" ");
		// this.add(twoSpacesclassifCombo);
		// this.add(classifLabel);
		classPickerPanel.add(classifierBox);

		classNumField = new JTextField(2);
		classNumField.setMinimumSize(new Dimension(15, 10));
		classNumField.setMaximumSize(new Dimension(20, 20));
		classNumField.setText(Integer.toString(nClasses));
		classNumField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JTextField classField = (JTextField) e.getSource();
				if (Integer.parseInt(classField.getText()) != nClasses) {
					nClasses = Integer.parseInt(classField.getText());
					makeColors();
				}
			}// end inner class
		});
		classPickerPanel.add(classNumField);

		detailButton = new JButton("Detail");
		detailButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				detailButton_actionPerformed();
			}
		});
		classPickerPanel.add(detailButton);
	}

	private void makeClassifyPanel() {
		classifyPanel = new JPanel();
		classifyPanel.setLayout(new BoxLayout(classifyPanel, BoxLayout.X_AXIS));

		classifyTrainDataButton = new JButton("ClassifyTrain");
		classifyTrainDataButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						classifyTrainButton_actionPerformed();
					}
				});

		classifyLoadDataButton = new JButton("ClassifyLoad");
		classifyLoadDataButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						classifyLoadButton_actionPerformed();
					}
				});

		displayResultsButton = new JButton("Display");
		displayResultsButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						displayButton_actionPerformed();
					}
				});

		saveResultsButton = new JButton("Save");
		saveResultsButton
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(ActionEvent e) {
						saveButton_actionPerformed();
					}
				});

		classifyPanel.add(classifyTrainDataButton);
		classifyPanel.add(classifyLoadDataButton);
		classifyPanel.add(displayResultsButton);
		classifyPanel.add(saveResultsButton);
	}

	private void setColorPanel() {
		colorPanel.setNSwatches(nClasses);
		Color[] pickerColors = colorPanel.getColors();
		boolean[] pickerAnchors = colorPanel.getAnchored();
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
			logger.finest("i = " + i + "index  = " + index);
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
			colorPanel.getRamp().rampColors(colors, colorAnchors);
		}// end if interpolate
	}

	private void makeColors() {
		setColorPanel();
		setObsColors();
	}

	private void setObsColors() {
		if (classificationIndexTrain != null) {
			if ((returnColors == null)
					|| (classificationIndexTrain.length != returnColors.length)) {
				returnColors = new Color[classificationIndexTrain.length];
			}
			for (int i = 0; i < classificationIndexTrain.length; i++) {

				if (classificationIndexTrain[i] == Classifier.NULL_CLASS) {
					returnColors[i] = ColorSymbolizer.DEFAULT_NULL_COLOR;
				} else {
					returnColors[i] = colors[classificationIndexTrain[i]];
				}
			}
			fireColorArrayChanged(getClassificationColors());
		}
	}

	/** Listens to the check boxen. */
	class CheckBoxListener implements ItemListener {
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource().equals(updateBox)) {
				if (e.getStateChange() == ItemEvent.SELECTED && setupFinished) {
					update = true;
					makeColors();
					fireActionPerformed(VisualClassifierMultiVariables.COMMAND_COLORS_CHANGED);
					// VisualClassifierMultiVariables.this.fireColorClassifierPerformed();

				} else if (e.getStateChange() == ItemEvent.DESELECTED) {

					update = false;
				}
			} else if (e.getSource().equals(interpolateBox)) {
				if (e.getStateChange() == ItemEvent.SELECTED && setupFinished) {
					interpolate = true;
					makeColors();
					fireActionPerformed(VisualClassifierMultiVariables.COMMAND_COLORS_CHANGED);
					// VisualClassifierMultiVariables.this.fireColorClassifierPerformed();

				} else if (e.getStateChange() == ItemEvent.DESELECTED) {
					interpolate = false;
					makeColors();
					fireActionPerformed(VisualClassifierMultiVariables.COMMAND_COLORS_CHANGED);
					// VisualClassifierMultiVariables.this.fireColorClassifierPerformed();
				}
			}
		}
	}

	JFrame dummyFrame;

	private void detailButton_actionPerformed() {
		// Bring up a detailed parameter setting GUI.
		nClasses = Integer.parseInt(classNumField.getText());
		if (dataObject == null) {
			return;
		}
		if (classifierName.equals("Classifiers:")) {
			return;
		}
		if (classifierName.equals("K Means Clustering") && (dataObject != null)) {
			logger.finest("Inside K means Clustering...");
			kMParameterSetup = new ClassifierKMParameters(
					(String[]) dataObject[0], nClasses);
			// kMParameterSetup.setAttributeNames((String[])this.dataObject[0]);
			// kMParameterSetup.setClassNum(nClasses);
			if (kmParameterSetupDialog == null) {
				if (dummyFrame == null) {
					dummyFrame = new JFrame();
				}
				kmParameterSetupDialog = new JDialog(dummyFrame,
						"Parameter Setup", true);
				kmParameterSetupDialog.setLocation(300, 300);
				kmParameterSetupDialog.setSize(300, 300);
				kmParameterSetupDialog.getContentPane().setLayout(
						new BorderLayout());
				kmParameterSetupDialog.getContentPane().add(kMParameterSetup,
						BorderLayout.CENTER);
				kMParameterSetup.addActionListener(new ActionListener() {

					/**
					 * Get event from parameter setup GUI.
					 * 
					 * @param e
					 */
					public void actionPerformed(ActionEvent e) {
						try {
							km_para_actionPerformed(e);
						} catch (Exception ex) {
							logger.throwing(this.getClass().getName(),
									"detail_button", ex);
						}
					}
				});
			}
			kmParameterSetupDialog.setVisible(true);
		}
		if (classifierName.equals("MaximumLikelihood Classifier")
				&& (dataObject != null)) {
			logger.finest("Inside MKL ...");
			mlParameterSetup = new ClassifierMLParameters(
					(String[]) dataObject[0], nClasses);
			if (mlParameterSetupDialog == null) {
				if (dummyFrame == null) {
					dummyFrame = new JFrame();
				}
				mlParameterSetupDialog = new JDialog(dummyFrame,
						"Parameter Setup", true);
				mlParameterSetupDialog.setLocation(300, 300);
				mlParameterSetupDialog.setSize(300, 300);
				mlParameterSetupDialog.getContentPane().setLayout(
						new BorderLayout());
				mlParameterSetupDialog.getContentPane().add(mlParameterSetup,
						BorderLayout.CENTER);
				mlParameterSetup.addActionListener(new ActionListener() {

					/**
					 * Get event from parameter setup GUI.
					 * 
					 * @param e
					 */
					public void actionPerformed(ActionEvent e) {
						try {
							ml_para_actionPerformed(e);
						} catch (Exception ex) {
							logger.throwing(this.getClass().getName(),
									"detail_button", ex);
						}
					}
				});
			}
			mlParameterSetupDialog.setVisible(true);

		}
	}

	private void applyButton_actionPerformed() {
		// Actually apply the parameters and do the classification, pass to
		// other components.
		nClasses = Integer.parseInt(classNumField.getText());
		// this.makeColors();
		if (dataObject != null) {
			logger.finest("data object is not null...");
		}
		if (classifierName.equals("Classifiers:")) {
			classifierBox.setSelectedIndex(1);
			classifierKMeans.setClusterNumber(nClasses);
			classifierKMeans.setDataSet(dataSet);
			classificationIndexTrain = classifierKMeans.getKMeansClusters();
		}
		if (classifierName.equals("K Means Clustering") && (dataObject != null)) {
			classifierKMeans.setClusterNumber(nClasses);
			if (selectedAttIdx != null) {
				classifierKMeans.setSelectedAttIdx(selectedAttIdx);
				logger.finest("selectedAttIdx: " + selectedAttIdx.length);
			}
			if (iniObsIdx != null) {
				classifierKMeans.setIniObsIdx(iniObsIdx);
			}
			classifierKMeans.setDataSet(dataSet);

			classificationIndexTrain = classifierKMeans.getKMeansClusters();
		}
		if (classifierName.equals("MixtureModels Clustering")
				&& (dataObject != null)) {
			classifierMixureModels.setNumberOfCluster(nClasses);
			// this.classifierKMeans.setSelectedAttIdx(selectedAttIdx);
			classifierMixureModels.setDataSet(dataSet);
			classificationIndexTrain = classifierMixureModels
					.getClusterResults();
		}

		if (classifierName.equals("MaximumLikelihood Classifier")
				&& (dataObject != null)) {

			if ((trainingData == null) || nClasses != trainingData.length) {
				setUpTrainingData.setNumberOfClasses(nClasses);
				setUpTrainingData.setDataObject(dataObject);
				trainingData = setUpTrainingData.getTrainingDataVector();
				trainingClassLabels = setUpTrainingData
						.getTrainingClassLabels();
			}
			// classifierML.setClassColors(colors);

			classifierML.setVisualDisplay(visualDisplay);
			classifierML.setTrainingData(trainingData);

			return;
		}

		returnColors = new Color[classificationIndexTrain.length];
		for (int i = 0; i < classificationIndexTrain.length; i++) {

			if (classificationIndexTrain[i] == Classifier.NULL_CLASS) {
				returnColors[i] = ColorSymbolizer.DEFAULT_NULL_COLOR;
			} else {
				returnColors[i] = colors[classificationIndexTrain[i]];
			}
		}

		fireColorArrayChanged(getClassificationColors());
	}

	JFrame errorMatrixFrame;

	private void classifyTrainButton_actionPerformed() {
		// Actually apply the parameters and do the classification, pass to
		// other components.
		// this.nClasses = Integer.parseInt(this.classNumField.getText());
		setColorPanel();
		if (dataObject != null) {
			logger.finest("data object is not null...");
		}
		if (classifierName.equals("Classifiers:")) {
			return;
		}
		if (classifierName.equals("K Means Clustering") && (dataObject != null)) {
			return;
		}
		if (classifierName.equals("MaximumLikelihood Classifier")
				&& (dataObject != null)) {
			classifierML.setDataSet(dataSet);
			classificationIndexTrain = classifierML.getClassificaiton();

			// if (this.errorMatrixPane == null){
			// if(this.errorMatrixFrame == null){
			// this.errorMatrixFrame = new JFrame("Error Matrix");
			// }
			// this.errorMatrixPane = new ErrorMatrixPane();
			//
			// this.errorMatrixFrame.setLocation(300, 300);
			// this.errorMatrixFrame.setSize(450, 200);
			// this.errorMatrixFrame.getContentPane().setLayout(new
			// BorderLayout());
			// this.errorMatrixFrame.getContentPane().add(this.errorMatrixPane,
			// BorderLayout.CENTER);
			//
			// }
			// this.errorMatrixPane.setUpErrorMatrix((int[])(this.dataObject[this.dataObject.length-1]),
			// this.classifierML.getClassificaiton(),
			// this.setUpTrainingData.getTrainingClassNumber());
			// this.errorMatrixFrame.setVisible(true);

		}

		setObsColors();
		// this.returnColors = new Color[classificationIndexTrain.length];
		// for (int i = 0; i < classificationIndexTrain.length; i++) {
		//
		// if (classificationIndexTrain[i] == Classifier.NULL_CLASS) {
		// returnColors[i] = ColorSymbolizer.DEFAULT_NULL_COLOR;
		// } else {
		// returnColors[i] = colors[classificationIndexTrain[i]];
		// }
		// }
		// this.fireColorArrayChanged(getClassificationColors());
	}

	private void classifyLoadButton_actionPerformed() {
		// apply the maximum classify rule to a new data set.
		if (dataObject != null) {
			logger.finest("data object is not null...");
		}
		if (classifierName.equals("Classifiers:")) {
			return;
		}
		if (classifierName.equals("K Means Clustering") && (dataObject != null)) {
			return;
		}
		if (classifierName.equals("MaximumLikelihood Classifier")
				&& (dataObject != null)) {
			loadFile();
		}

	}

	BufferedImage resultIMG;
	JPanel imagePane = new JPanel(new BorderLayout()) {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			logger.finest("paintComponenet");
			if (resultIMG != null) {
				logger.finest("paintImage");
				// Dimension d = getSize();
				// Graphics2D g2 = null;
				// if (resultIMG == null || resultIMG.getWidth() != d.width ||
				// resultIMG.getHeight() != d.height) {
				// resultIMG = (BufferedImage) createImage(d.width, d.height);
				// }
				// g2 = resultIMG.createGraphics();
				// g2.setBackground(getBackground());
				// g2.clearRect(0, 0, d.width, d.height);
				// g2.setRenderingHint(RenderingHints.KEY_RENDERING,
				// RenderingHints.VALUE_RENDER_QUALITY);
				g.drawString("test", 100, 100);
				g.drawImage(resultIMG, 0, 0, this);
			}
		}
	};
	JFrame imageFrame = new JFrame();

	private void displayButton_actionPerformed() {

		// int len = this.classificationIndex.length;
		int w, h;
		w = h = 525;
		resultIMG = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		returnColors = new Color[classificationIndex.length];
		for (int i = 0; i < classificationIndex.length; i++) {

			if (classificationIndex[i] == Classifier.NULL_CLASS) {
				returnColors[i] = ColorSymbolizer.DEFAULT_NULL_COLOR;
			} else {
				returnColors[i] = colors[classificationIndex[i]];
			}
		}
		for (int i = 0; i < 525; i++) {
			for (int j = 0; j < 525; j++) {
				resultIMG.setRGB(j, i, returnColors[i * 525 + j].getRGB());
			}
		}
		// geovista.category.GvImageOps image = new
		// geovista.category.GvImageOps(resultIMG);

		imageFrame.getContentPane().add(imagePane);
		imageFrame.setSize(525, 525);
		imageFrame.setVisible(true);
		// this.fireColorArrayChanged(getClassificationColors());
	}

	// save classification results to a file.
	private void saveButton_actionPerformed() {
		saveOutput();
	}

	private void loadFile() {
		FileName = "D:\\";
		MyFileFilter filter = new MyFileFilter("IMG");
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.setDialogTitle("Load Data");
		chooser.setCurrentDirectory(new File(FileName));
		chooser.setApproveButtonText("Load");
		chooser.setFileFilter(filter);

		File[] files;
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			files = chooser.getSelectedFiles();
		} else {
			return;
		}

		try {
			int varnum = dataObject.length - 2;
			FileInputStream[] istreams = new FileInputStream[varnum];
			geovista.readers.Reader[] fileReader = new geovista.readers.Reader[varnum];
			String[] values = new String[varnum];
			double[] tuple = new double[varnum];
			classificationIndex = new int[525 * 525];

			for (int i = 0; i < varnum; i++) {
				istreams[i] = new FileInputStream(files[i]);
				fileReader[i] = new geovista.readers.Reader(istreams[i], ",\n");
			}

			for (int i = 0; i < 525 * 525; i++) {

				for (int j = 0; j < varnum; j++) {
					values[j] = fileReader[j].readToken();
					tuple[j] = new Double(values[j]).doubleValue();
				}
				classifierML.setSingleTuple(tuple);
				classificationIndex[i] = classifierML.getClassTuple();
			}

			for (int i = 0; i < varnum; i++) {
				istreams[i].close();
				fileReader[i].close();
			}
		} catch (Exception ee) {
			ee.printStackTrace();
		}

	}

	private void mlcTuple() {
		int varnum = dataObject.length - 2;
		double[] tuple = new double[varnum];
		classificationIndex = new int[classifyData.length];
		for (int i = 0; i < classifyData.length; i++) {
			tuple = classifyData[i];
			classifierML.setSingleTuple(tuple);
			classificationIndex[i] = classifierML.getClassTuple();
		}
	}

	private void mlcDataObject() {
		classificationIndex = new int[classifyData.length];
		classifierML.setDataSet(dataSet);
	}

	private void saveOutput() {
		MyFileFilter filter = new MyFileFilter("txt");
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save Classifivation Results");
		chooser.setCurrentDirectory(new File("D:\\"));
		chooser.setApproveButtonText("Save");
		chooser.setFileFilter(filter);

		File file;
		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = chooser.getSelectedFile();
		} else {
			return;
		}

		try {

			FileWriter a = new FileWriter(file);
			for (int element : classificationIndex) {
				a.write(new Integer(element).toString());
				a.write("\n");
			}

			a.close();

			// FileOutputStream ostream = new FileOutputStream(file);
			//
			// for(int i = 0; i < this.classificationIndex.length; i ++){
			// output.append(this.classificationIndex[i]);
			// output.append("\n");
			// }
			// byte[] s = output.toString().getBytes();
			// ostream.write(s, 0, s.length);
			// ostream.flush();
			// ostream.close();
		} catch (Exception ee) {
			ee.printStackTrace();
		}
	}

	private void km_para_actionPerformed(ActionEvent e) {
		ClassifierKMParameters kmParameters = (ClassifierKMParameters) e
				.getSource();
		iniObsIdx = kmParameters.getIniObsIdx();
		selectedAttIdx = kmParameters.getSelectedAttIdx();
		kmParameterSetupDialog.setVisible(false);
	}

	private void ml_para_actionPerformed(ActionEvent e) {
		ClassifierMLParameters mlParameters = (ClassifierMLParameters) e
				.getSource();
		mlParameters.getIsUnBiasCov();
		mlParameters.getDistributionType();
		visualDisplay = mlParameters.getVisualDisplay();
		selectedAttIdx = mlParameters.getSelectedAttIdx();
		mlParameterSetupDialog.setVisible(false);
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
		double pickPrefWidth = classPickerPanel.getPreferredSize().getWidth();
		int prefWidth = (int) (pickPrefWidth * 1.5);
		if (getWidth() >= prefWidth) {
			// this.changeOrientation(this.X_AXIS);
		} else {
			// this.changeOrientation(this.Y_AXIS);
		}
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command == ColorRampPicker.COMMAND_SWATCH_COLOR_CHANGED) {
			makeColors();
			fireActionPerformed(VisualClassifierMultiVariables.COMMAND_COLORS_CHANGED);
			// this.fireColorClassifierPerformed();
		} /*
		 * else if (command == this.COMMAND_CLASSES_CHANGED) {
		 * this.colorPickerPanel.setNSwatches(nClasses); this.nClasses =
		 * nClasses; this.makeColors();
		 * this.fireActionPerformed(this.COMMAND_COLORS_CHANGED);
		 * //this.fireColorClassifierPerformed(); }
		 */
		// need to pass this along, if we are a FoldupPanel
		// super.actionPerformed(e);
	}

	/**
	 * implements ActionListener
	 */
	public void addActionListener(ActionListener l) {
		listenerList.add(ActionListener.class, l);
		fireActionPerformed(VisualClassifierMultiVariables.COMMAND_BEAN_REGISTERED);
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

	// Work with coordinator.
	public void dataSetChanged(DataSetEvent e) {
		setDataObject(e.getDataSet());
	}

	public void classColorChanged(ClassColorEvent e) {
		setClassColor(e.getClassLabel(), e.getClassColor());
	}

	public void mergeCategoryChanged(MergeCategoryEvent e) {
		mergeClasses(e.getClassLabel());
	}

	public void classNumberChanged(ClassNumberEvent e) {
		setClassNumber(e.getClassNumber());
	}

	public void dataSetDoubleChanged(DataSetDoubleEvent e) {
		setClassifyData(e.getDataSet());
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
	 * adds an SelectionListener
	 */
	public void addColorArrayListener(ColorArrayListener l) {
		listenerList.add(ColorArrayListener.class, l);
	}

	/**
	 * removes an SelectionListener from the component
	 */
	public void removeColorArrayListener(ColorArrayListener l) {
		listenerList.remove(ColorArrayListener.class, l);

	}

	private void fireColorArrayChanged(Color[] newColorArray) {

		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		ColorArrayEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ColorArrayListener.class) {
				// Lazily create the event:
				if (e == null) {
					e = new ColorArrayEvent(this, newColorArray);
				}
				((ColorArrayListener) listeners[i + 1]).colorArrayChanged(e);
			}
		}// next i

	}
}
