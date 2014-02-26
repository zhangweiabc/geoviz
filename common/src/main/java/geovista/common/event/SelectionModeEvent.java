/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.common.event;

import java.util.EventObject;

/**
 * An PalletEvent signals that there is a new or changed pallet.
 * 
 * The recipient can then query the pallet to get colors.
 * 
 */

public class SelectionModeEvent extends EventObject {

    public enum SelectionMode {
	And, Or, Xor, Replace
    }

    private SelectionMode newMode;

    /**
     * The constructor is the same as that for EventObject, except that the
     * pallet is indicated.
     */
    public SelectionModeEvent(Object source, SelectionMode mode) {
	super(source);
	this.newMode = mode;
    }

    public SelectionMode getMode() {
	return newMode;
    }

}
