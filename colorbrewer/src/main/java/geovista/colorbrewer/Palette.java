//******************************************************************************
// interface oblivion.awt.colorbrewer.Palette
// Copyright (c) Christopher E. Weaver. All rights reserved.
//******************************************************************************
// File: Palette.java
// Last modified: Wed Sep  7 15:19:16 2005 by Chris Weaver
//******************************************************************************
// Modification History:
//
// 20050907 [weaver]: Original file.
//
//******************************************************************************
//
//******************************************************************************

package geovista.colorbrewer;

import java.awt.Color;

//import java.lang.*;

//******************************************************************************
// interface Palette
//******************************************************************************

/**
 * The <CODE>Palette</CODE> interface.
 * 
 * @author Chris Weaver
 * @version %I%, %G%
 */
public interface Palette {
	// **********************************************************************
	// Members
	// **********************************************************************

	int NONE = 0;
	int POOR = 1;
	int IFFY = 2;
	int GOOD = 3;

	int COLORBLIND = 0;
	int PHOTOCOPY = 1;
	int PROJECTOR = 2;
	int LCD = 3;
	int CRT = 4;
	int PRINTER = 5;

	public static final Color DEFAULT_NULL_COLOR = Color.darkGray;

	String[] PURPOSE = new String[] { "ColorBlind", "PhotoCopy",
			"LCD Projector", "LCD", "CRT", "Color Printing", };

	// **********************************************************************
	// Methods
	// **********************************************************************

	String getName();
}

// ******************************************************************************
