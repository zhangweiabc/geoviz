/*
 * AttSetWeightPanel.java
 *
 * Created on November 9, 2004, 7:29 PM
 */
/**
 *
 * @author  yliu
 */
package geovista.geoviz.radviz;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.NumberFormatter;

public class AttSetWeightPanel extends JPanel implements ActionListener, ChangeListener, PropertyChangeListener {

    private static final int WT_MIN = 0;
    private static final int WT_MAX = 100;
    private static final int WT_INIT = 1;    //
    private double weight = 0.5;
    private JFormattedTextField weightText;
    private JSlider weightSlider;
    private JButton okBtn, cancelBtn;
    private EventListenerList ell = new EventListenerList();
    protected final static Logger logger = Logger.getLogger(AttSetWeightPanel.class.getName());
    /** Creates a new instance of AttSetWeightPanel */
    public AttSetWeightPanel() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        //Create the label.
        JLabel sliderLabel = new JLabel("Attribute Weight: ", SwingConstants.CENTER);
        sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        //Create the formatted text field and its formatter.
        
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        NumberFormatter formatter = new NumberFormatter(decimalFormat);
        formatter.setOverwriteMode(true);
        formatter.setAllowsInvalid(true);
        formatter.setMinimum(new Float(WT_MIN));
        formatter.setMaximum(new Float(WT_MAX / WT_MAX));
        weightText = new JFormattedTextField(formatter);
        weightText.setValue(new Double(weight));
        weightText.setColumns(4); //get some space
        weightText.addPropertyChangeListener(this);

        //React when the user presses Enter.
        weightText.getInputMap().put(KeyStroke.getKeyStroke(
                                        KeyEvent.VK_ENTER, 0),
                                        "check");
        weightText.getActionMap().put("check", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!weightText.isEditValid()) { //The text is invalid.
                    Toolkit.getDefaultToolkit().beep();
                    weightText.selectAll();
                } else try {                    //The text is valid,
                    weightText.commitEdit();     //so use it.
                } catch (java.text.ParseException exc) { exc.printStackTrace();}
            }
        });

        //Create the slider.
        weightSlider = new JSlider(SwingConstants.HORIZONTAL, WT_MIN, WT_MAX, WT_INIT);
        weightSlider.addChangeListener(this);

        //Turn on labels at major tick marks.
        weightSlider.setMajorTickSpacing(50);
        weightSlider.setMinorTickSpacing(10);
        weightSlider.setPaintTicks(true);
        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(WT_MIN), new JLabel("0.0"));
        labelTable.put(new Integer(WT_MAX/2), new JLabel("0.5"));
        labelTable.put(new Integer(WT_MAX), new JLabel("1.0"));
        weightSlider.setLabelTable( labelTable );
        weightSlider.setPaintLabels(true);
        weightSlider.setValue(WT_MAX/2);
        weightSlider.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));

        //Create a subpanel for the label and text field.
        JPanel labelAndTextField = new JPanel(); //use FlowLayout
        labelAndTextField.add(sliderLabel);
        labelAndTextField.add(weightText);

        //Create Ok and Cancel buttons
        okBtn = new javax.swing.JButton("Ok");
        okBtn.addActionListener(this);
        cancelBtn = new javax.swing.JButton("Cancel");
        cancelBtn.addActionListener(this);
        JPanel btnPane = new JPanel();
        btnPane.add(okBtn);
        btnPane.add(new JLabel("  "));
        btnPane.add(cancelBtn);
        
        //Put everything together.
        add(labelAndTextField);
        add(weightSlider);
        add(btnPane);
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    }
    
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double newWeight) {
        int help = Math.round((float)(newWeight * 100));
        weightSlider.setValue(help);
    }
    
    /** Listen to the slider. */
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if (source.getValueIsAdjusting()) {
            weight = source.getValue();
            weight /= WT_MAX;
            weightText.setText(String.valueOf(weight));
            fireAction("AttSetWeightPanel.WeightIsChanging");
        }
        else { //done adjusting
            double help = source.getValue();
            help /= WT_MAX;
            if (! (Math.abs(help - weight) < .0000001 )) {
                weight = help;
                weightText.setValue(new Double(weight)); //update weight value
            }
        }
        logger.finest("weight="+weight);
    }

    /**
     * Listen to the text field.  This method detects when the
     * value of the text field (not necessarily the same
     * number as you'd get from getText) changes.
     */
    public void propertyChange(PropertyChangeEvent e) {
        if ("value".equals(e.getPropertyName())) {
            Number value = (Number)e.getNewValue();
            if (weightSlider != null && value != null) {
                int helpInt = Math.round((float)(value.doubleValue() * 100));
                double help = ((double)helpInt) / WT_MAX;
                if (! (Math.abs(help - weight) < .0000001 )) weight = help;
                if (helpInt != weightSlider.getValue()) weightSlider.setValue(helpInt);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okBtn) {
            //weight = Double.parseDouble(weightText.getText());
            logger.finest("weight="+weight);
            fireAction("AttSetWeightPanel.OK");
        }
        else if (e.getSource() == cancelBtn) fireAction("AttSetWeightPanel.CANCEL");
    }
    
    public void addActionListener(ActionListener sl){
        ell.add(ActionListener.class, sl);
    }

    public void removeActionListener(ActionListener sl){
        ell.remove(ActionListener.class, sl);
    }
    
    public void fireAction(String command){
        Object[] listeners = ell.getListenerList();
        int numListeners = listeners.length;
        ActionEvent se = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command);
        for (int i = 0; i < numListeners; i++){
          if (listeners[i]==ActionListener.class){
        // pass the event to the listeners event dispatch method
            ((ActionListener)listeners[i+1]).actionPerformed(se);
          }
        }
    }
    
    public static void main(String args[]) {
      AttSetWeightPanel sel = new AttSetWeightPanel();
      JFrame f = new JFrame("Test");
      f.getContentPane().add(sel);
      f.setSize(300, 180);
      f.setVisible(true);
      f.repaint();
   }
    
}