/*
 * GeoVISTA Center (Penn State, Dept. of Geography)
 * Copyright (c), 1999 - 2002, GeoVISTA Center
 * All Rights Researved.
 *
 *
 * @author: jin Chen
 * @date: Aug 19, 2003$
 * 
 */
package geovista.category;

import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

public abstract class ListControlPane extends JPanel {
    ButtonListner btnListener=new ButtonListner();
    protected final static Logger logger = Logger.getLogger(ListControlPane.class.getName());
    public ListControlPane() {
         initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;


       //create components

        mainP = new javax.swing.JPanel();
        btnP = new javax.swing.JPanel();

        tableP = new javax.swing.JPanel();
        TableScrollPane = new javax.swing.JScrollPane();
        myTable = new ConfigureTable();
        listP = new javax.swing.JPanel();
        ListScrollPane = new javax.swing.JScrollPane();
        myList = new ColorItemList();


        //placement
        setLayout(new java.awt.GridBagLayout());
        setPreferredSize(new java.awt.Dimension(400, 375));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;

        mainP.setLayout(new java.awt.GridBagLayout());


        btnP.setLayout(new java.awt.GridLayout(9, 1));


        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        mainP.add(btnP, gridBagConstraints);

        tableP.setLayout(new java.awt.BorderLayout());

        tableP.setBorder(new javax.swing.border.TitledBorder(""));
        tableP.setPreferredSize(new java.awt.Dimension(473, 349));
        //TableScrollPane.setBorder(new javax.swing.border.TitledBorder("Visible"));
        //Vector colName=new Vector();
        //colName.add("Index");colName.add("Name");colName.add("Interval");colName.add("DisplayType");
        //VisibleAxisTable.setModel(this.getVisibleAxisTableModel( this.yaxisList.size()));

        TableScrollPane.setViewportView(myTable);

        tableP.add(TableScrollPane, java.awt.BorderLayout.CENTER);
       //tableP.setBorder(new javax.swing.border.TitledBorder("Visible"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainP.add(tableP, gridBagConstraints);

        listP.setLayout(new java.awt.BorderLayout());


        ListScrollPane.setViewportView(myList);

        listP.add(ListScrollPane, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainP.add(listP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(mainP, gridBagConstraints);

        initButtons()   ;
    }
    /*******************************************************************************************************
     *                GUI
     *******************************************************************************************************/
     public void setMainSectionName(String name) {
        mainP.setBorder(new javax.swing.border.TitledBorder(name));
     }
    public void setListSectionName(String name) {
        listP.setBorder(new javax.swing.border.TitledBorder(name));
    }
    public void setTableSectionName(String name) {
        tableP.setBorder(new javax.swing.border.TitledBorder(name));
    }
    /*******************************************************************************************************
     *                List
     *******************************************************************************************************/
    protected abstract void initList();
    public void setListData(Object[] items) {
        this.myList.setListData(items);
    }

    /*******************************************************************************************************
     *                table
     *******************************************************************************************************/
    //1.create tableModel //2. init values
    protected abstract void initTable();

    public void setTableModel(TableModel dataModel) {
        this.myTable.setModel(dataModel);
    }
    public void setValueAt(Object value,int row, int col) {
        myTable.setValueAt(value,row,col);
    }

    /*******************************************************************************************************
     *                button
     *******************************************************************************************************/
    protected void initButtons() {
        String[] names=new String[]{"left","Right","Up","Down","Apply"};
        this.addButtons(names);

    }

    public void addButtons(String[] names) {
        int numBtn=names.length;
        btnP.removeAll() ;

        this.btnP.setLayout(new java.awt.GridLayout(numBtn, 1)) ;
        for (int i=0;i<numBtn ;i++){
             JButton btn=new JButton(names[i]);
             btn.setName(names[i]);
             btn.addActionListener(this.btnListener );
             btnP.add(btn);
        }
    }
    /*******************************************************************************************************
     *                Event
     *******************************************************************************************************/
    protected  abstract void actOnButtonClick(String btnName);
    /**
     *
     * @param items
     */
    protected  void moveRight(){
         Object[] names=myList.getSelectedValues() ;
        for (int i=0;i<names.length ;i++){
                String name=(String) names[i];
                if(!myTable.isRecordContained("Name",name) ){
                    this.addRecord(name);
                    this.myList.colorItem(name,true);
                }
        }
    }
    /**
     * Add a record to table. Implementation is determined by tableModel
     * @param name  the key attribute of the record
     */
    protected abstract void addRecord(String name);/*{
        Object[] arow={new Integer(-1),name, YAxis.DTYPE_Nml};
                this.myTable.addRow(arow);
    }*/
    /**
     * Assume table has a column named "Name", and your operation will be based on the column
     * If not, override the method
     */
    protected  void moveLeft(){
        // Add your handling code here:
        int index=this.myTable.getSelectedRow() ;
        if(index<0)return ;

          String name= (String) myTable.getValueAt(index,"Name");
          if(name!=null){
            this.myTable.removeRow(index);
            this.myList.colorItem(name,false);
          }
    }
    protected  void moveUp(){
        int row=this.myTable.getSelectedRow() ;
        if (row>0){
           myTable.moveRowUp(row);
        }
    }
    protected  void moveDown(){
        // Add your handling code here:
        int row=this.myTable.getSelectedRow() ;
        if (row>=0&&row<myTable.getRowCount() ){
           myTable.moveRowDown(row);
        }
    }



    // Variables declaration - do not modify

    protected ConfigureTable myTable;
    protected javax.swing.JScrollPane TableScrollPane;
    protected javax.swing.JPanel tableP;
    protected ColorItemList myList;
    protected javax.swing.JScrollPane ListScrollPane;
    protected javax.swing.JPanel listP;

    protected javax.swing.JPanel btnP;
    protected javax.swing.JPanel mainP;


    private class ButtonListner implements ActionListener{
            public void actionPerformed(java.awt.event.ActionEvent evt) {

                JButton btn=(JButton) evt.getSource();

                logger.finest("click on :"+btn.getName() );
                actOnButtonClick(btn.getName());
            }
    }


}
