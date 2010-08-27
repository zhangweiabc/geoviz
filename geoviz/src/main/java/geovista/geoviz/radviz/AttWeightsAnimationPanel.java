/*
 * AttWeightsAnimationPanel.java
 *
 * Created on November 9, 2004, 7:29 PM
 */
/**
 *
 * @author  yliu
 */
package geovista.geoviz.radviz;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.text.NumberFormatter;

public class AttWeightsAnimationPanel extends JPanel implements ActionListener, ChangeListener, ItemListener,
                                                                PropertyChangeListener {

    private static final int WT_MIN = 0;
    private static final int WT_MAX = 100;
    private static final int WT_INIT = 1;
    private static final int FPS_MIN = 0;
    private static final int FPS_MAX = 30;
    private static final int FPS_INIT = 15;
    private static final int LEN_MIN = 0;
    private static final int LEN_MAX = 120;
    private static final int LEN_INIT = 30;
    private double[] startWeights, endWeights;
    private int animationLength = LEN_INIT;
    private JComboBox startAttCombo, endAttCombo;
    private JFormattedTextField startWeightText, endWeightText, animationLengthText;
    private JSlider startWeightSlider, endWeightSlider, animationLengthSlider;
    private JSpinner fpsSpinner;
    private JButton animateBtn, cancelBtn, okBtn;
    private String[] attributes;
    private EventListenerList ell = new EventListenerList();
    
    /** Creates a new instance of AttWeightsAnimationPanel */
    public AttWeightsAnimationPanel(String[] attributeList, double[] weights) {
        startWeights = weights.clone();//new double[weights.length];
        endWeights = weights.clone();//new double[weights.length];
        attributes = attributeList.clone();
        //System.arraycopy(weights, 0, startWeights, 0, weights.length);
        //System.arraycopy(weights, 0, endWeights, 0, weights.length);
        initComponents();
    }
    
    public double[] getEndWeights() {
        return endWeights;
    }
    
    public int getNumberOfSeconds() {
        return animationLength;
    }
    
    public int getFPS() {
        int fps = Integer.parseInt(fpsSpinner.getValue().toString());
        return fps;
    }
    
    public double[] getStartWeights() {
        return startWeights;
    }
    
    public void setAttributeNames(String[] newNames) {
        attributes = newNames.clone();
        DefaultComboBoxModel startModel = new DefaultComboBoxModel(attributes);
        startAttCombo.setModel(startModel);
        DefaultComboBoxModel endModel = new DefaultComboBoxModel(attributes);
        endAttCombo.setModel(endModel);
    }
    
    public void setStartWeights(double[] newWeights) {
        startWeights = newWeights.clone();//new double[weights.length];
        startWeightText.setValue(new Double(startWeights[startAttCombo.getSelectedIndex()]));
        int initWtSliderValue = Math.round((float)(startWeights[startAttCombo.getSelectedIndex()] * 100));
        startWeightSlider.setValue(initWtSliderValue);
    }
    
    public void setEndWeights(double[] newWeights) {
        endWeights = newWeights.clone();//new double[weights.length];
        endWeightText.setValue(new Double(endWeights[endAttCombo.getSelectedIndex()]));
        int initWtSliderValue = Math.round((float)(endWeights[endAttCombo.getSelectedIndex()] * 100));
        endWeightSlider.setValue(initWtSliderValue);
    }
    
    /** Listen to the ComboBox */
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (e.getSource() == startAttCombo) {
                startWeightText.setValue(new Double(startWeights[startAttCombo.getSelectedIndex()]));
                int newWtSliderValue = Math.round((float)(startWeights[startAttCombo.getSelectedIndex()] * 100));
                startWeightSlider.setValue(newWtSliderValue);
            }
            else if (e.getSource() == endAttCombo) {
                endWeightText.setValue(new Double(endWeights[endAttCombo.getSelectedIndex()]));
                int newWtSliderValue = Math.round((float)(endWeights[endAttCombo.getSelectedIndex()] * 100));
                endWeightSlider.setValue(newWtSliderValue);
            }
        }
    }
    
    /**
     * Listen to the text field.  This method detects when the
     * value of the text field (not necessarily the same
     * number as you'd get from getText) changes.
     */
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getSource() == startWeightText) {
            int start_curr = startAttCombo.getSelectedIndex();
            if ("value".equals(e.getPropertyName())) {
                Number value = (Number)e.getNewValue();
                if (startWeightSlider != null && value != null) {
                    int helpInt = Math.round((float)(value.doubleValue() * 100));
                    double help = ((double)helpInt) / WT_MAX;
                    
                    if (! (Math.abs(help - startWeights[start_curr]) < .0000001 )) startWeights[start_curr] = help;
                    if (helpInt != startWeightSlider.getValue()) startWeightSlider.setValue(helpInt);
                }
            }
        }
        if (e.getSource() == endWeightText) {
            int end_curr = endAttCombo.getSelectedIndex();
            if ("value".equals(e.getPropertyName())) {
                Number value = (Number)e.getNewValue();
                if (endWeightSlider != null && value != null) {
                    int helpInt = Math.round((float)(value.doubleValue() * 100));
                    double help = ((double)helpInt) / WT_MAX;
                    if (!(Math.abs(help - endWeights[end_curr]) < .0000001) ) endWeights[end_curr] = help;
                    if (helpInt != endWeightSlider.getValue()) endWeightSlider.setValue(helpInt);
                }
            }
        }
        if (e.getSource() == animationLengthText) {
            if ("value".equals(e.getPropertyName())) {
                Number value = (Number)e.getNewValue();
                if (animationLengthSlider != null && value != null) {
                    int help = value.intValue();
                    if (help != animationLength) animationLength = help;
                    if (help != animationLengthSlider.getValue()) animationLengthSlider.setValue(help);
                }
            }
        }
    }
    
    /** Listen to the slider. */
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if (source == startWeightSlider) {
            int start_curr = startAttCombo.getSelectedIndex();
            if (source.getValueIsAdjusting()) {
                startWeights[start_curr] = source.getValue();
                startWeights[start_curr] /= WT_MAX;
                startWeightText.setText(String.valueOf(startWeights[start_curr]));
            }
            else { //done adjusting
                double help = source.getValue();
                help /= WT_MAX;
                if (! (Math.abs(help - startWeights[start_curr]) < .0000001 )) {
                    startWeights[start_curr] = help;
                    startWeightText.setValue(new Double(startWeights[start_curr])); //update weight value
                }
            }
        }
        if (source == endWeightSlider) {
            int end_curr = endAttCombo.getSelectedIndex();
            if (source.getValueIsAdjusting()) {
                endWeights[end_curr] = source.getValue();
                endWeights[end_curr] /= WT_MAX;
                endWeightText.setText(String.valueOf(endWeights[end_curr]));
            }
            else { //done adjusting
                double help = source.getValue();
                help /= WT_MAX;
                
                if (!(Math.abs(help - endWeights[end_curr]) < .0000001) ){
                    endWeights[end_curr] = help;
                    endWeightText.setValue(new Double(endWeights[end_curr])); //update weight value
                }
            }
        }
        if (source == animationLengthSlider) {
            if (source.getValueIsAdjusting()) {
                animationLength = source.getValue();
                animationLengthText.setText(String.valueOf(animationLength));
            }
            else { //done adjusting
                int help = source.getValue();
                if (help != animationLength) {
                    animationLength = help;
                    animationLengthText.setValue(new Integer(animationLength)); //update weight value
                }
            }
        }
    }

    /** Listen to the buttons */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okBtn) {
            fireAction("AttWeightsAnimationPanel.OK");
        }
        else if (e.getSource() == cancelBtn) {
            fireAction("AttWeightsAnimationPanel.CANCEL");
        }
        else if (e.getSource() == animateBtn) {
            if (animateBtn.getText().equalsIgnoreCase("Animate")) {
                fireAction("AttWeightsAnimationPanel.ANIMATE");
                animateBtn.setText("Stop");
            }
            else {
                fireAction("AttWeightsAnimationPanel.STOP");
                animateBtn.setText("Animate");
            }
        }
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
          if (listeners[i] == ActionListener.class){
        // pass the event to the listeners event dispatch method
            ((ActionListener)listeners[i+1]).actionPerformed(se);
          }
        }
    }
    
    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.PAGE_AXIS));
        startPanel.setBorder(BorderFactory.createCompoundBorder(
                             BorderFactory.createTitledBorder("Start"),
                             BorderFactory.createEmptyBorder(0,0,0,0)));
        JPanel startSubPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startSubPanel.add(new JLabel("Attribute:"));
        startAttCombo = new JComboBox(attributes);
        startAttCombo.addItemListener(this);
        startSubPanel.add(startAttCombo);
        startSubPanel.add(new JLabel(" Weight:"));
        //Create the formatted text field and its formatter.
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        NumberFormatter formatter = new NumberFormatter(decimalFormat);
        formatter.setOverwriteMode(true);
        formatter.setAllowsInvalid(true);
        formatter.setMinimum(new Float(WT_MIN));
        formatter.setMaximum(new Float(WT_MAX / WT_MAX));
        startWeightText = new JFormattedTextField(formatter);
        startWeightText.setValue(new Double(startWeights[startAttCombo.getSelectedIndex()]));
        startWeightText.setColumns(4); //get some space
        startWeightText.addPropertyChangeListener(this);
        //React when the user presses Enter.
        startWeightText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        startWeightText.getActionMap().put("check", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!startWeightText.isEditValid()) { //The text is invalid.
                    Toolkit.getDefaultToolkit().beep();
                    startWeightText.selectAll();
                } else try {                    //The text is valid,
                    startWeightText.commitEdit();     //so use it.
                } catch (java.text.ParseException exc) {exc.printStackTrace(); }
            }
        });
        startSubPanel.add(startWeightText);
        startPanel.add(startSubPanel);
        //Create the slider.
        startWeightSlider = new JSlider(SwingConstants.HORIZONTAL, WT_MIN, WT_MAX, WT_INIT);
        startWeightSlider.addChangeListener(this);
        //Turn on labels at major tick marks.
        startWeightSlider.setMajorTickSpacing(50);
        startWeightSlider.setMinorTickSpacing(10);
        startWeightSlider.setPaintTicks(true);
        Hashtable labelTableWT = new Hashtable();
        labelTableWT.put(new Integer(WT_MIN), new JLabel("0.0"));
        labelTableWT.put(new Integer(WT_MAX/2), new JLabel("0.5"));
        labelTableWT.put(new Integer(WT_MAX), new JLabel("1.0"));
        startWeightSlider.setLabelTable(labelTableWT);
        startWeightSlider.setPaintLabels(true);
        int initWtSliderValue = Math.round((float)(startWeights[startAttCombo.getSelectedIndex()] * 100));
        startWeightSlider.setValue(initWtSliderValue);
        //startWeightSlider.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));
        startPanel.add(startWeightSlider);
        startPanel.setMaximumSize(new Dimension(startPanel.getMaximumSize().width, 
                                                   startPanel.getPreferredSize().height));
        
        JPanel endPanel = new JPanel();
        endPanel.setLayout(new BoxLayout(endPanel, BoxLayout.PAGE_AXIS));
        endPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createTitledBorder("End"),
                            BorderFactory.createEmptyBorder(0,0,0,0)));
        JPanel endSubPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        endSubPanel.add(new JLabel("Attribute:"));
        endAttCombo = new JComboBox(attributes);
        endAttCombo.addItemListener(this);
        endSubPanel.add(endAttCombo);
        endSubPanel.add(new JLabel(" Weight:"));
        //Create the formatted text field and its formatter.
        endWeightText = new JFormattedTextField(formatter);
        endWeightText.setValue(new Double(endWeights[endAttCombo.getSelectedIndex()]));
        endWeightText.setColumns(4); //get some space
        endWeightText.addPropertyChangeListener(this);
        //React when the user presses Enter.
        endWeightText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        endWeightText.getActionMap().put("check", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!endWeightText.isEditValid()) { //The text is invalid.
                    Toolkit.getDefaultToolkit().beep();
                    endWeightText.selectAll();
                } else try {                    //The text is valid,
                    endWeightText.commitEdit();     //so use it.
                } catch (java.text.ParseException exc) {exc.printStackTrace(); }
            }
        });
        endSubPanel.add(endWeightText);
        endPanel.add(endSubPanel);
        //Create the slider.
        endWeightSlider = new JSlider(SwingConstants.HORIZONTAL, WT_MIN, WT_MAX, WT_INIT);
        endWeightSlider.addChangeListener(this);
        //Turn on labels at major tick marks.
        endWeightSlider.setMajorTickSpacing(50);
        endWeightSlider.setMinorTickSpacing(10);
        endWeightSlider.setPaintTicks(true);
        endWeightSlider.setLabelTable(labelTableWT);
        endWeightSlider.setPaintLabels(true);
        //initWtSliderValue = Math.round((float)(endWeights[endAttCombo.getSelectedIndex()] * 100));
        endWeightSlider.setValue(initWtSliderValue);
        //startWeightSlider.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));
        endPanel.add(endWeightSlider);
        endPanel.setMaximumSize(new Dimension(endPanel.getMaximumSize().width, 
                                                 endPanel.getPreferredSize().height));

        JPanel animationPanel = new JPanel();
        animationPanel.setLayout(new BoxLayout(animationPanel, BoxLayout.PAGE_AXIS));
        animationPanel.setBorder(BorderFactory.createCompoundBorder(
                                 BorderFactory.createTitledBorder("Animation"),
                                 BorderFactory.createEmptyBorder(0,0,0,0)));
        JPanel animationSubPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        animationSubPanel.add(new JLabel("Frames Per Second:"));
        fpsSpinner = new JSpinner(new SpinnerNumberModel(FPS_INIT,FPS_MIN,FPS_MAX,1));
        animationSubPanel.add(fpsSpinner);
        animationSubPanel.add(new JLabel(" Length:"));
        //Create the formatted text field and its formatter.
        java.text.NumberFormat integerFormat = java.text.NumberFormat.getIntegerInstance();
        NumberFormatter formatterInt = new NumberFormatter(integerFormat);
        formatterInt.setOverwriteMode(true);
        formatterInt.setAllowsInvalid(true);
        formatterInt.setMinimum(new Integer(LEN_MIN));
        formatterInt.setMaximum(new Integer(LEN_MAX));
        animationLengthText = new JFormattedTextField(formatterInt);
        animationLengthText.setValue(new Integer(animationLength));
        animationLengthText.setColumns(5); //get some space
        animationLengthText.addPropertyChangeListener(this);
        //React when the user presses Enter.
        animationLengthText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "check");
        animationLengthText.getActionMap().put("check", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!animationLengthText.isEditValid()) { //The text is invalid.
                    Toolkit.getDefaultToolkit().beep();
                    animationLengthText.selectAll();
                } else try {                    //The text is valid,
                    animationLengthText.commitEdit();     //so use it.
                } catch (java.text.ParseException exc) { }
            }
        });
        animationSubPanel.add(animationLengthText);
        animationSubPanel.add(new JLabel("seconds"));
        animationPanel.add(animationSubPanel);
        //Create the slider.
        animationLengthSlider = new JSlider(SwingConstants.HORIZONTAL, LEN_MIN, LEN_MAX, LEN_INIT);
        animationLengthSlider.addChangeListener(this);
        //Turn on labels at major tick marks.
        animationLengthSlider.setMajorTickSpacing(60);
        animationLengthSlider.setMinorTickSpacing(10);
        animationLengthSlider.setPaintTicks(true);
        Hashtable labelTableLEN = new Hashtable();
        labelTableLEN.put(new Integer(LEN_MIN), new JLabel("0"));
        labelTableLEN.put(new Integer(LEN_MAX/2), new JLabel("60"));
        labelTableLEN.put(new Integer(LEN_MAX), new JLabel("120"));
        animationLengthSlider.setLabelTable(labelTableLEN);
        animationLengthSlider.setPaintLabels(true);
        animationLengthSlider.setValue(animationLength);
        //startWeightSlider.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));
        animationPanel.add(animationLengthSlider);
        animationPanel.setMaximumSize(new Dimension(animationPanel.getMaximumSize().width, 
                                                    animationPanel.getPreferredSize().height));
        
        //Create Animate, Ok and Cancel buttons
        animateBtn = new JButton("Animate");
        animateBtn.addActionListener(this);
        okBtn = new JButton("Ok");
        okBtn.addActionListener(this);
        cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(this);
        JPanel btnPane = new JPanel();
        btnPane.add(animateBtn);
        btnPane.add(okBtn);
        //btnPane.add(new JLabel("  "));
        btnPane.add(cancelBtn);
        
        //Put everything together.
        add(startPanel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(endPanel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(animationPanel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(btnPane);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    }
    
    public static void main(String args[]) {
      String[] names = {"att1", "att2", "att3"};
      double[] wts = {0.2, 0.6, 0.7};
      AttWeightsAnimationPanel sel = new AttWeightsAnimationPanel(names, wts);
      JFrame f = new JFrame("Test");
      f.getContentPane().add(sel);
      f.setSize(400, 400);
      f.setVisible(true);
      f.repaint();
   }
    
}
