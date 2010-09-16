package geovista.geoviz.spreadsheet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import geovista.common.data.DataSetForApps;
import geovista.common.data.DescriptiveStatistics;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.readers.example.GeoData48States;

public class VariableTransformer extends JPanel implements DataSetListener,
		ActionListener, ListSelectionListener {
	DataSetForApps dataSet;
	JList varList;
	JList varList2;
	JList resultsList;
	static boolean DEBUG;
	double[] resultData;

	JRadioButton normalizeButt;
	JRadioButton addButt;
	JRadioButton subtractButt;
	JRadioButton multButt;
	JRadioButton divideButt;
	JRadioButton observedToExpectedButt;

	JButton sendButt;
	private static int OP_NORMALIZE = 0;
	private static int OP_ADD = 1;
	private static int OP_SUBTRACT = 2;
	private static int OP_DIVIDE = 3;
	private static int OP_MULTIPLY = 4;
	private int currOp = OP_NORMALIZE;

	public VariableTransformer() {
		setPreferredSize(new Dimension(550, 220));
		JPanel firstVarPanel = new JPanel();
		firstVarPanel.setBorder(BorderFactory.createTitledBorder("First"));
		JPanel opPanel = createOpPanel();
		opPanel.setBorder(BorderFactory.createTitledBorder("Operation"));
		JPanel secondVarPanel = new JPanel();
		secondVarPanel.setBorder(BorderFactory.createTitledBorder("Second"));
		JPanel resultsPanel = new JPanel();
		resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));

		varList = new JList();
		varList2 = new JList();
		resultsList = new JList();
		varList.setVisibleRowCount(5);
		varList2.setVisibleRowCount(5);

		varList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		varList2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		varList.addListSelectionListener(this);
		varList2.addListSelectionListener(this);

		JScrollPane scrollPane = new JScrollPane(varList);
		JScrollPane scrollPane2 = new JScrollPane(varList2);
		JScrollPane scrollPane3 = new JScrollPane(resultsList);
		scrollPane.setPreferredSize(new Dimension(100, 160));
		scrollPane2.setPreferredSize(new Dimension(100, 160));
		scrollPane3.setPreferredSize(new Dimension(100, 140));
		firstVarPanel.add(scrollPane);
		secondVarPanel.add(scrollPane2);

		resultsPanel.setLayout(new BorderLayout());
		resultsPanel.add(scrollPane3, BorderLayout.CENTER);
		resultsList.addListSelectionListener(this);
		resultsList.setForeground(Color.black);
		sendButt = new JButton("Send Results");
		resultsPanel.add(sendButt, BorderLayout.SOUTH);
		sendButt.addActionListener(this);
		varList2.setEnabled(false);

		this.add(firstVarPanel);
		this.add(opPanel);
		this.add(secondVarPanel);
		this.add(resultsPanel);

	}

	private JPanel createOpPanel() {
		JPanel pan = new JPanel();
		// ok, which ops... normalize and the arithmetic ones for sure
		pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
		normalizeButt = new JRadioButton("Normalize");
		addButt = new JRadioButton("Add");
		subtractButt = new JRadioButton("Subtract");
		multButt = new JRadioButton("Multiply");
		divideButt = new JRadioButton("Divide");
		observedToExpectedButt = new JRadioButton("Observed to Expected");
		observedToExpectedButt.setEnabled(false);
		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();

		group.add(normalizeButt);
		group.add(addButt);
		group.add(subtractButt);
		group.add(multButt);
		group.add(divideButt);
		normalizeButt.setSelected(true);

		pan.add(normalizeButt);
		pan.add(addButt);
		pan.add(subtractButt);
		pan.add(multButt);
		pan.add(divideButt);
		pan.add(observedToExpectedButt);

		normalizeButt.addActionListener(this);
		addButt.addActionListener(this);
		subtractButt.addActionListener(this);
		multButt.addActionListener(this);
		divideButt.addActionListener(this);

		return pan;

	}

	public void dataSetChanged(DataSetEvent e) {
		dataSet = e.getDataSetForApps();

		initData();
	}

	private void initData() {
		String[] newVarNames = dataSet.getAttributeNamesNumeric();
		varList.setListData(newVarNames);
		varList2.setListData(newVarNames);
		varList.setSelectedIndex(0);
		varList.repaint();
	}

	public static void main(String[] args) {
		JFrame app = new JFrame();
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		VariableTransformer trans = new VariableTransformer();
		app.add(trans);
		app.pack();
		app.setVisible(true);
		GeoData48States stateData = new GeoData48States();
		DataSetEvent e = new DataSetEvent(stateData.getDataForApps(), app);
		trans.dataSetChanged(e);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == normalizeButt) {
			varList2.setEnabled(false);
			currOp = VariableTransformer.OP_NORMALIZE;
			performOp();
		} else if (e.getSource() == addButt) {
			varList2.setEnabled(true);
			currOp = VariableTransformer.OP_ADD;
			performOp();
		} else if (e.getSource() == subtractButt) {
			varList2.setEnabled(true);
			currOp = VariableTransformer.OP_SUBTRACT;
			performOp();
		} else if (e.getSource() == multButt) {
			varList2.setEnabled(true);
			currOp = VariableTransformer.OP_MULTIPLY;
			performOp();
		} else if (e.getSource() == divideButt) {
			varList2.setEnabled(true);
			currOp = VariableTransformer.OP_DIVIDE;
			performOp();
		} else if (e.getSource() == sendButt) {
			// dataSet.addColumn("newCol", resultData);
		}

	}

	private void performOp() {
		int firstIndex = varList.getSelectedIndex();
		if (firstIndex < 0) {
			return;
		}
		double[] data = dataSet.getNumericDataAsDouble(firstIndex);
		if (currOp == VariableTransformer.OP_NORMALIZE) {
			resultData = DescriptiveStatistics.calculateZScores(data);
			Vector vecData = new Vector();
			for (double z : resultData) {
				vecData.add(new Double(z));
			}
			resultsList.setListData(vecData);
		}

		int secondIndex = varList2.getSelectedIndex();
		if (secondIndex < 0) {
			return;
		}
		double[] data2 = dataSet.getNumericDataAsDouble(secondIndex);
		if (currOp == VariableTransformer.OP_ADD) {

			Vector vecData = new Vector();
			for (int i = 0; i < data.length; i++) {
				resultData[i] = data[i] + data2[i];
				vecData.add(resultData[i]);
			}
			resultsList.setListData(vecData);
		}

		if (currOp == VariableTransformer.OP_SUBTRACT) {

			Vector vecData = new Vector();
			for (int i = 0; i < data.length; i++) {
				resultData[i] = data[i] - data2[i];
				vecData.add(resultData[i]);
			}
			resultsList.setListData(vecData);
		}
		if (currOp == VariableTransformer.OP_MULTIPLY) {

			Vector vecData = new Vector();
			for (int i = 0; i < data.length; i++) {
				resultData[i] = data[i] * data2[i];
				vecData.add(resultData[i]);
			}
			resultsList.setListData(vecData);
		}
		if (currOp == VariableTransformer.OP_DIVIDE) {

			Vector vecData = new Vector();
			for (int i = 0; i < data.length; i++) {
				resultData[i] = data[i] / data2[i];
				vecData.add(resultData[i]);
			}
			resultsList.setListData(vecData);
		}

	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource().equals(resultsList)) {
			resultsList.clearSelection();
			return;
		}
		performOp();

	}

}
