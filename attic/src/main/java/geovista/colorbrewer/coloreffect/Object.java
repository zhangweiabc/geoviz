package geovista.colorbrewer.coloreffect;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * 
 */

public class Object extends JPanel {

  //create a titled border
  TitledBorder title = BorderFactory.createTitledBorder("title");

  public Object() {
    //customize the slider and the textfield
    //this.setSize(new Dimension(100, 200));
    this.setLayout(new GridLayout(3, 1));

    //setting titled border
    this.setBorder(title);
  }

  //a customized function to set the name of the control
  public void setTitle(String title){
    this.title = BorderFactory.createTitledBorder(title);
    this.setBorder(this.title);
  }

}