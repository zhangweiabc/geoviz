/* -------------------------------------------------------------------
 Java source file for the class CartogramGUI
 Copyright (c), 2005 Frank Hardisty
 $Author: hardistf $
 $Id: CartogramGUI.java,v 1.9 2005/12/05 20:35:36 hardistf Exp $
 $Date: 2005/12/05 20:35:36 $
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 -------------------------------------------------------------------   */

package geovista.cartogram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.geoviz.map.GeoMapUni;
import geovista.readers.shapefile.ShapeFileDataReader;
import geovista.readers.shapefile.ShapeFileProjection;
import geovista.readers.shapefile.ShapeFileToShape;

//import org.davekeen.gis.shapefile.GeoESRIWriter;

/*
 * This class is the GUI and traffic cop for creating cartograms.
 */

public class CartogramGUI extends JPanel implements ActionListener {
	Rectangle myRect;
	JButton chooseOutputFileButton = new JButton();
	GeoMapUni mapInput;
	GeoMapUni mapOutput;
	JTextField inputFileTextField;
	JTextField outputFileTextField;
	JButton chooseInputFileButton;
	JLabel outputFileNameLabel;
	JLabel inputFileNameLabel;
	// JLabel engineMessageLabel = new JLabel();
	JButton runButton;
	JButton aboutButton;
	JButton helpButton;
	JButton preferencesButton;
	// JEditorPane engineMessagesEditorPane = new JEditorPane();
	// JScrollPane jScrollPane1 = new JScrollPane();
	ShapeFileDataReader shpRead;
	ShapeFileToShape shpToShape;
	ShapeFileProjection shpProj;
	DataSetForApps dataSet;
	String inputFileName;
	// JCheckBox projCheckBox;
	// JCheckBox auxCheckBox;
	JProgressBar progressBar;
	Console messagePane;
	CartogramPreferences preferencesFrame;
	boolean useProj;

	TransformsMain trans;
	boolean isAux;
	final static Logger logger = Logger.getLogger(CartogramGUI.class.getName());

	public CartogramGUI() {
		trans = new TransformsMain(false);
		isAux = false;
		mapInput = new GeoMapUni();
		mapOutput = new GeoMapUni();
		GuiUtils.connectMaps(mapInput, mapOutput);
		inputFileTextField = new JTextField();
		outputFileTextField = new JTextField();
		chooseInputFileButton = new JButton();
		outputFileNameLabel = new JLabel();
		inputFileNameLabel = new JLabel();
		aboutButton = new JButton("About");
		runButton = new JButton("Run");
		helpButton = new JButton("Help");
		runButton.addActionListener(this);
		helpButton.addActionListener(this);
		aboutButton.addActionListener(this);

		useProj = false;

		shpRead = new ShapeFileDataReader();
		shpToShape = new ShapeFileToShape();
		shpProj = new ShapeFileProjection();
		if (logger.isLoggable(Level.FINEST)) {
			try {
				new Console();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		try {
			initGui();
			Preferences gvPrefs = Preferences.userNodeForPackage(this
					.getClass());
			preferencesFrame = new CartogramPreferences(
					"Cartogram Preferences", this, trans);
			setInputFile(gvPrefs);
			setOutputFileName(gvPrefs);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/*
	 * All action handling goes here.
	 */

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == chooseInputFileButton) {
			GuiUtils.disconnectMaps(mapInput, mapOutput);
			String fileName = GuiUtils.chooseInputFilename(this);
			if (fileName != null) {
				inputFileTextField.setText(fileName);
				readShapeFile(fileName);
			}
		} else if (e.getSource() == chooseOutputFileButton) {
			String fileName = GuiUtils.chooseOutputFilename(this);
			if (fileName != null) {
				outputFileTextField.setText(fileName);
			}
		} else if (e.getSource() == runButton) {

			new CartogramThread(this, isAux).start();

		} else if (e.getSource() == aboutButton) {
			showAboutMessage();
		} else if (e.getSource() == helpButton) {
			showHelpMessage();

		} else if (e.getSource() == preferencesFrame.getProjCheckBox()) {
			useProj = preferencesFrame.getProjCheckBox().isSelected();
			readShapeFile(inputFileName);
		} else if (e.getSource() == preferencesFrame.getAuxCheckBox()) {
			isAux = preferencesFrame.getAuxCheckBox().isSelected();

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("isAux = " + isAux);
			}

		} else if (e.getSource() == preferencesButton) {

			preferencesFrame.setVisible(true);
		} else {
			if (logger.isLoggable(Level.FINEST)) {
				logger
						.warning("CartogramGUI.actionPerformed, unexpected source encountered: "
								+ e.getSource().getClass());
			}

		}
	}

	private void showAboutMessage() throws HeadlessException {
		JOptionPane
				.showMessageDialog(
						this,
						"Cartogram creation using the Gastner-Newman method. \n"
								+ "\n"
								+ "Graphical user interface by Frank Hardisty. \n"
								+ "\n"
								+ "If you use output created by this program please acknowledge the use of this \n"
								+ "code and its first publication in: \n"
								+ "'Generating population density-equalizing maps', Michael T. Gastner and \n"
								+ "M. E. J. Newman, Proceedings of the National Academy of Sciences of the \n"
								+ " United States of America, vol. 101, pp. 7499-7504, 2004. \n");
	}

	private void showHelpMessage() throws HeadlessException {
		JOptionPane
				.showMessageDialog(
						this,
						"The left hand map shows the input shapefile, the right hand map shows the output. \n"
								+ "\n"
								+ "Transformation can take some time (U.S. Counties in three minutes on a 1Ghz machine). \n"
								+ "\n"
								+ "Consult the Console Messages for diagnostic information.");
	}

	private void initGui() throws Exception {
		JPanel mapPanel = new JPanel();
		setLayout(new BorderLayout());
		mapPanel.setPreferredSize(new Dimension(5000, 5000));
		mapPanel.setLayout(new BoxLayout(mapPanel, BoxLayout.X_AXIS));
		this.add(mapPanel, BorderLayout.CENTER);

		int buffer = 10;

		int width = 30;
		int height = 60;

		JPanel inputPanel = GuiUtils.createInputPanel(mapInput,
				inputFileNameLabel, chooseInputFileButton, inputFileTextField);
		chooseInputFileButton.addActionListener(this);

		JPanel outputPanel = createOutputPanel(buffer, width, height,
				inputPanel);
		createMapPanels(mapPanel, inputPanel, outputPanel);

		createBottomPanel();

		setPreferredSize(new Dimension(700, 350));

	}

	private void createBottomPanel() {
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		JPanel runPanel = new JPanel();
		JPanel messagesPanel = new JPanel();
		messagesPanel.setLayout(new BorderLayout());
		progressBar = new JProgressBar(SwingConstants.HORIZONTAL);

		runPanel.add(runButton);
		runPanel.add(progressBar);

		runPanel.add(aboutButton);
		runPanel.add(helpButton);

		preferencesButton = new JButton("Preferences");
		preferencesButton.addActionListener(this);
		runPanel.add(preferencesButton);
		bottomPanel.add(runPanel, BorderLayout.NORTH);

		this.add(bottomPanel, BorderLayout.SOUTH);

	}

	private void createMapPanels(JPanel mapPanel, JPanel inputPanel,
			JPanel outputPanel) {
		Dimension mapSize = new Dimension(200, 150);
		JPanel inputMapPanel = new JPanel();
		JPanel outputMapPanel = new JPanel();
		inputMapPanel.setLayout(new BorderLayout());
		outputMapPanel.setLayout(new BorderLayout());

		mapInput.setMinimumSize(mapSize);
		mapOutput.setMinimumSize(mapSize);

		Border border = BorderFactory.createLineBorder(Color.black);
		inputMapPanel.setBorder(border);
		outputMapPanel.setBorder(border);
		inputMapPanel.add(mapInput, BorderLayout.CENTER);
		outputMapPanel.add(mapOutput, BorderLayout.CENTER);
		Dimension pickerPanelSize = new Dimension(200, 60);
		inputPanel.setPreferredSize(pickerPanelSize);
		outputPanel.setPreferredSize(pickerPanelSize);
		inputMapPanel.add(inputPanel, BorderLayout.SOUTH);
		outputMapPanel.add(outputPanel, BorderLayout.SOUTH);

		mapPanel.add(inputMapPanel);
		mapPanel.add(outputMapPanel);
	}

	private void setOutputFileName(Preferences gvPrefs) {
		String lastOutputDir = gvPrefs.get("LastGoodOutputDirectory", "");
		if (lastOutputDir == "") {
			String homeDir = System.getProperty("user.home");
			outputFileTextField.setText(homeDir + "/test.shp");

		} else {
			outputFileTextField.setText(lastOutputDir);
		}

	}

	private JPanel createOutputPanel(int buffer, int width, int height,
			JPanel inputPanel) {
		JPanel outputPanel = new JPanel();
		outputPanel.setBounds(buffer + inputPanel.getWidth()
				+ inputPanel.getX(), buffer, width, height);

		// mapOutput.setBackground(Color.white);

		chooseOutputFileButton.setText("Choose");
		chooseOutputFileButton.addActionListener(this);
		outputFileNameLabel.setText("Output File Name:");

		outputPanel.add(chooseOutputFileButton);
		outputPanel.add(outputFileNameLabel);
		outputPanel.add(outputFileTextField);
		return outputPanel;
	}

	private void setInputFile(Preferences gvPrefs) {
		String lastInputDir = gvPrefs.get("LastGoodInputDirectory", "");
		if (lastInputDir.equals("")) {

			String fileName = GuiUtils.writeDefaultShapefile();
			lastInputDir = fileName;
			useProj = true;
		}

		try {
			inputFileTextField.setText(lastInputDir);
			inputFileName = lastInputDir;
			readShapeFile(inputFileName);
			useProj = preferencesFrame.getProjCheckBox().isSelected();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this,
					"Sorry, could not correctly read input file: "
							+ inputFileName + " Please restart FX.Cartogram.");
			// if something bad happened here, better reset the prefs file
			// otherwise the user could get stuck with a "bad file" every time
			// the program starts
			gvPrefs.put("LastGoodInputDirectory", "");
			ex.printStackTrace();
			System.exit(0);
		}

	}

	private void readShapeFile(String fileName) {
		GuiUtils.disconnectMaps(mapInput, mapOutput);
		if (fileName == null || fileName == "") {
			return;
		}
		try {
			shpRead.setFileName(fileName);
		} catch (Exception ex) {
			System.err.println("Error reading file " + fileName);
			ex.printStackTrace();
			// this.setInputFile();
		}
		if (useProj) {
			shpProj.setInputDataSet(shpRead.getDataSet());
			dataSet = shpProj.getOutputDataSetForApps();
		} else {
			shpToShape.setInputDataSet(shpRead.getDataSet());
			dataSet = shpRead.getDataForApps();
		}
		DataSetEvent e2 = new DataSetEvent(dataSet, this);
		mapInput.dataSetChanged(e2);
		if (dataSet.getSpatialType() == DataSetForApps.SPATIAL_TYPE_LINE
				|| dataSet.getSpatialType() == DataSetForApps.SPATIAL_TYPE_POINT) {
			// this.preferencesFrame.getAuxCheckBox().setSelected(true);
			isAux = true;
		} else {
			// this.preferencesFrame.getAuxCheckBox().setSelected(false);
			isAux = false;
		}
		inputFileName = fileName;
	}

	/*
	 * This method actually creates the temporary files and creates a
	 * TransformsMain to do the work.
	 */
	public void createCartogram() {
		Preferences gvPrefs = Preferences.userNodeForPackage(this.getClass());
		String shapeFile = inputFileName;
		String newShapeFile = outputFileTextField.getText();
		int currentVar = mapInput.getCurrentVariable();
		trans = new TransformsMain(false);
		preferencesFrame.setTransformParams(trans);
		DataSetForApps newData = MapGenFile.createCartogram(progressBar,
				dataSet, gvPrefs, newShapeFile, shapeFile, currentVar, trans);

		mapOutput.setDataSet(newData);

		GuiUtils.connectMaps(mapInput, mapOutput);
		// String transLoc = "C:\\temp\\trans\\";

		// TransformSerialization.writeTransform(this.trans,transLoc);

	}

	public void createAuxCartogram() {
		Preferences gvPrefs = Preferences.userNodeForPackage(this.getClass());
		String shapeFile = inputFileName;
		String newShapeFile = outputFileTextField.getText();
		int currentVar = mapInput.getCurrentVariable();

		DataSetForApps newData = MapGenFile.createAuxCartogram(progressBar,
				dataSet, gvPrefs, newShapeFile, shapeFile, currentVar, trans);

		mapOutput.setDataSet(newData);
		GuiUtils.connectMaps(mapInput, mapOutput);

	}

	private class CartogramThread extends Thread {
		CartogramGUI cGUI;
		boolean isAux;

		CartogramThread(CartogramGUI cGUI, boolean isAux) {
			this.isAux = isAux;
			this.cGUI = cGUI;
		}

		@Override
		public void run() {
			cGUI.runButton.setEnabled(false);
			if (isAux) {
				cGUI.createAuxCartogram();
			} else {
				cGUI.createCartogram();
			}
			cGUI.runButton.setEnabled(true);
		}

	}

	public static void main(String[] args) {

		CartogramGUI gui = new CartogramGUI();
		JFrame frame = new JFrame("FX.Cartogram");
		frame.getContentPane().add(gui);

		frame.pack();
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		gui.mapInput.zoomFullExtent();
	}

}
