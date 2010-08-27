/*
 * Proclude.java
 *
 * Created on May 22, 2008, 3:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package geovista.satscan;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.MouseInputAdapter;

import geovista.common.data.DataSetForApps;
import geovista.common.event.DataSetEvent;
import geovista.common.event.DataSetListener;
import geovista.common.event.SelectionEvent;
import geovista.common.event.SelectionListener;
import geovista.proclude.AbstractGam;
import geovista.proclude.BesagNewellGAM;
import geovista.proclude.CrossMidLine;
import geovista.proclude.Fitness;
import geovista.proclude.FitnessRelativePct;
import geovista.proclude.Gene;
import geovista.proclude.GeneticGAM;
import geovista.proclude.InitGAMFile;
import geovista.proclude.MutateLinearAmount;
import geovista.proclude.RandomGam;
import geovista.proclude.RelocateDifference;
import geovista.proclude.SelectRandomElite;
import geovista.proclude.SelectionGene;
import geovista.proclude.StopAtNGens;
import geovista.proclude.SurviveEliteN;
import geovista.proclude.SystematicGam;
import geovista.readers.example.GeoData48States;

/**
 * 
 * @author jfc173
 */
public class Proclude extends JPanel implements ActionListener,
		DataSetListener, SelectionListener {

	InitGAMFile initializer;
	int type = 0;
	Gene[] out;
	private static final String[] typeNames = { "Genetic", "Random",
			"Systematic", "Besag" };
	JComboBox types = new JComboBox(typeNames);
	JButton runButton = new JButton("RUN");
	JPanel output = new JPanel();
	JPanel[] genePanels;
	int[] selectedPoints;

	public final static int GENETIC_TYPE = 0;
	public final static int RANDOM_TYPE = 1;
	public final static int SYSTEMATIC_TYPE = 2;
	public final static int BESAG_TYPE = 3;

	protected final static Logger logger = Logger.getLogger(Proclude.class
			.getName());

	/** Creates a new instance of Proclude */
	public Proclude() {
		initializer = new InitGAMFile();
		types.setActionCommand("NEW_TYPE");
		types.addActionListener(this);
		runButton.setActionCommand("RUN");
		runButton.addActionListener(this);

		setLayout(new BorderLayout());
		JPanel topPanel = new JPanel();
		// topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(types);
		topPanel.add(runButton);
		this.add(topPanel, BorderLayout.NORTH);
		JScrollPane jsp = new JScrollPane(output,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.add(jsp, BorderLayout.CENTER);

		GAMMouseListener gml = new GAMMouseListener();
		output.addMouseListener(gml);
		output.addMouseMotionListener(gml);
	}

	public void dataSetChanged(DataSetEvent e) {
		initializer.processDataSetForApps(e.getDataSetForApps());
		// run();
	}

	public void setDataSetForApps(DataSetForApps dsfa) {
		initializer.processDataSetForApps(dsfa);
	}

	public void setType(int i) {
		type = i;
	}

	public void run() {
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		double largeDimension = Math.max(initializer.getMaxX()
				- initializer.getMinX(), initializer.getMaxY()
				- initializer.getMinY());
		logger.info("largeDimension is " + largeDimension);
		AbstractGam gam;
		switch (type) {
		case GENETIC_TYPE:
			gam = initGenetic(largeDimension);
			break;
		case RANDOM_TYPE:
			gam = initRandom();
			break;
		case SYSTEMATIC_TYPE:
			gam = initSystematic();
			break;
		case BESAG_TYPE:
			gam = initBesag();
			break;
		default:
			gam = initGenetic(largeDimension);
		}
		gam.setMaxRadius(0.05 * largeDimension);
		gam.setMinRadius(0.005 * largeDimension);
		gam.setMinPoints(3);
		gam.setMinAccepted(1.5);
		gam
				.setFitnessFunction(new FitnessRelativePct(initializer
						.getDataSet()));
		gam.setInitializer(initializer);

		// Whew. Now that the method is created, run it! This is a lot easier.
		Vector solutions = gam.run();

		out = new Gene[solutions.size()];
		for (int i = 0; i < solutions.size(); i++) {
			out[i] = (Gene) solutions.get(i);
		}
		updateOutputDisplay();
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public Gene[] getOutput() {
		return out;
	}

	private AbstractGam initSystematic() {
		return new SystematicGam();
	}

	private AbstractGam initRandom() {
		RandomGam gam = new RandomGam();
		gam.setNumTests(500);
		return gam;
	}

	private AbstractGam initBesag() {
		return new BesagNewellGAM();
	}

	private AbstractGam initGenetic(double largeDimension) {
		// Whee! A lot of parameters. These values work reasonably well, in my
		// experience.
		GeneticGAM gam = new GeneticGAM();
		StopAtNGens halt = new StopAtNGens(75);
		(gam).setHaltCondition(halt);
		(gam).setPopSize(100);
		(gam).setProbMut(0.05);
		(gam).setRandomGenes(0);
		(gam).setFirstAdd(4);
		(gam).setUpdateOften(2);
		(gam).setSelectPairs(false);
		(gam).setBannedList(true);
		(gam).setSolutionList(false);
		(gam).setAntiConvergence(true);
		SelectRandomElite selection = new SelectRandomElite(0.2);
		(gam).setSelectMethod(selection);
		SurviveEliteN survive = new SurviveEliteN(0.2);
		(gam).setSurviveMethod(survive);
		CrossMidLine crossover = new CrossMidLine();
		(gam).setCrossoverMethod(crossover);
		MutateLinearAmount mutation = new MutateLinearAmount(
				0.05 * largeDimension);
		(gam).setMutationMethod(mutation);
		RelocateDifference relocation = new RelocateDifference(3, 1.5);
		(gam).setRelocationMethod(relocation);
		return gam;
	}

	public static void main(String[] args) {
		GeoData48States states = new GeoData48States();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("n num atts"
					+ states.getDataForApps().getNumberNumericAttributes());
		}
		Proclude scanner = new Proclude();
		scanner.setDataSetForApps(states.getDataForApps());

		JFrame app = new JFrame("testing Proclude");
		app.add(scanner);
		app.pack();
		app.setVisible(true);
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equalsIgnoreCase("RUN")) {
			run();
		} else if (command.equalsIgnoreCase("NEW_TYPE")) {
			int selected = types.getSelectedIndex();
			setType(selected);
		} else {
			logger.info("snuh?");
		}
	}

	private void updateOutputDisplay() {
		logger.info("there are " + out.length + " clusters");
		output.removeAll();
		output.setLayout(new GridLayout(0, 10));
		genePanels = new JPanel[out.length];
		for (int i = 0; i < out.length; i++) {
			Gene next = out[i];
			JLabel[] labels = geneToLabels(next);
			JPanel thisGene = new JPanel();
			genePanels[i] = thisGene;
			thisGene.setLayout(new BoxLayout(thisGene, BoxLayout.Y_AXIS));
			for (JLabel element : labels) {
				thisGene.add(element);
			}
			output.add(thisGene);
		}
		revalidate();
		repaint();
	}

	private JLabel[] geneToLabels(Gene g) {
		JLabel[] labels = new JLabel[10];
		labels[0] = new JLabel("Center X: " + roundToHundredths(g.getX()));
		labels[1] = new JLabel("Center Y: " + roundToHundredths(g.getY()));
		labels[2] = new JLabel("Major Radius: "
				+ roundToHundredths(g.getMajorAxisRadius()));
		labels[3] = new JLabel("Minor Radius: "
				+ roundToHundredths(g.getMinorAxisRadius()));
		labels[4] = new JLabel("Orientation: "
				+ roundToHundredths(g.getOrientation()));
		labels[5] = new JLabel("Area: "
				+ roundToHundredths(Math.PI * g.getMajorAxisRadius()
						* g.getMinorAxisRadius()));
		labels[6] = new JLabel("# Points: " + g.getContainedPoints().length);
		labels[7] = new JLabel("Population: " + g.getPopulation());
		labels[8] = new JLabel("Count: " + g.getCount());
		labels[9] = new JLabel("Fitness: " + roundToHundredths(g.getFitness()));
		labels[9].setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		labels[0].setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		return labels;
	}

	private void createSelectionDisplay(double fitness, SelectionGene sGene) {
		output.removeAll();
		// JLabel ids = new JLabel("IDs: " +
		// Arrays.toString(sGene.getContainedPoints()));
		JLabel ids = new JLabel("n = " + sGene.getContainedPoints().length);
		ids.setMaximumSize(new java.awt.Dimension(250, 50));
		ids.setToolTipText(Arrays.toString(sGene.getContainedPoints()));
		JLabel pop = new JLabel("Population: " + sGene.getPopulation());
		JLabel count = new JLabel("Count: " + sGene.getCount());
		JLabel fit = new JLabel("Fitness: " + roundToHundredths(fitness));
		JPanel thisGene = new JPanel();
		thisGene.setLayout(new BoxLayout(thisGene, BoxLayout.Y_AXIS));
		thisGene.add(ids);
		thisGene.add(pop);
		thisGene.add(count);
		thisGene.add(fit);
		thisGene.setMaximumSize(new java.awt.Dimension(260, 60));
		logger.info(thisGene.getSize().toString());
		output.add(thisGene);
		genePanels = new JPanel[] { thisGene };
		out = new Gene[] { sGene };
		revalidate();
		repaint();
	}

	private double roundToHundredths(double d) {
		return ((double) Math.round(d * 100)) / 100;
	}

	private void highlightLabelAt(int x, int y, MouseEvent e) {
		JPanel selected = (JPanel) output.getComponentAt(x, y);

		if (!(selected.equals(output))) { // if the selected object is the
			// output panel, then the click
			// didn't fall on any genePanel
			int selectedIndex = -1;
			for (int i = 0; i < genePanels.length; i++) {
				genePanels[i].setBorder(new EmptyBorder(3, 3, 3, 3));
				if (genePanels[i].equals(selected)) {
					selectedIndex = i;
				}
			}
			selected.setBorder(new LineBorder(Color.RED, 3, true));
			revalidate();
			this.repaint();

			// now send a selection event
			if (selectedIndex == -1) {
				logger.info("huh?  Why didn't this find the index?");
			} else {
				int[] newSelectedPoints = out[selectedIndex]
						.getContainedPoints();
				if (e.isShiftDown()) {
					selectedPoints = SelectionEvent.makeAndSelection(
							selectedPoints, newSelectedPoints);
				} else if (e.isControlDown()) {
					selectedPoints = SelectionEvent.makeXORSelection(
							selectedPoints, newSelectedPoints);
				} else {
					selectedPoints = newSelectedPoints;
				}

				fireSelectionChanged(selectedPoints);
			}
		}

	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. The event instance is lazily created using the
	 * parameters passed into the fire method.
	 * 
	 * @see EventListenerList
	 */
	public void fireSelectionChanged(int[] newSelection) {
		// logger.info(Arrays.toString(newSelection));
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
		} // next i
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

	public void selectionChanged(SelectionEvent e) {
		selectedPoints = e.getSelection();
		Fitness fit = new FitnessRelativePct(initializer.getDataSet());
		if (initializer.getDataSet() == null) {
			return;
		}
		SelectionGene sGene = new SelectionGene(selectedPoints, fit);
		logger.info(Arrays.toString(selectedPoints));
		double out = fit.run(sGene);
		createSelectionDisplay(out, sGene);
	}

	public SelectionEvent getSelectionEvent() {
		return new SelectionEvent(this, selectedPoints);
	}

	/**
	 * This inner class is a mouse listener that listens for events in the
	 * display panel and updates the display appropriately. Adapted from the
	 * MyListener inner class in the SelectionDemo from the Java Tutorial.
	 * 
	 * @author Jamison Conley
	 * @version 1.0
	 */
	class GAMMouseListener extends MouseInputAdapter {

		public GAMMouseListener() {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			requestFocusInWindow();
			int x = e.getX();
			int y = e.getY();
			logger.info("I clicked at " + x + ", " + y);
			highlightLabelAt(x, y, e);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			// shrug
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// eh.
		}

	} // END class GAMMouseListener

}
