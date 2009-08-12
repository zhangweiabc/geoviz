/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.geoviz.spreadsheet;

import junit.framework.TestCase;

import geovista.geoviz.Exerciser;

public class TableViewerTest extends TestCase {
	public void testTableViewer() {

		TableViewer comp = new TableViewer();
		Exerciser exer = new Exerciser();
		exer.testGUIAndEvents(comp);
	}
}
