/*
 * TMFileDrawPattern.java
 * www.bouthier.net
 *
 * The MIT License :
 * -----------------
 * Copyright (c) 2001 Christophe Bouthier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package geovista.treemap.tm.demo;

import java.awt.Paint;
import java.util.Date;

import geovista.treemap.tm.TMExceptionBadTMNodeKind;
import geovista.treemap.tm.TMNode;
import geovista.treemap.tm.TMNodeAdapter;
import geovista.treemap.tm.TMPatternFactory;

/**
 * The TMFileDrawPattern implements a example of renderer for TMFileNode. It use
 * the date of last modification as color, and patterns instead of color for
 * filling rects.
 * <P>
 * The color legend is :
 * <UL>
 * <IL> PATTERN_WHITE for files less than a hour old <IL> PATTERN_DIAG1 for
 * files less than a day old <IL> PATTERN_DIAG2 for files less than a week old
 * <IL> PATTERN_DOTS for files less than a month old <IL> PATTERN_LIGHT_GRAY for
 * files less than a year old <IL> PATTERN_DARK_GRAY for files more than a year
 * old
 * </UL>
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
public class TMFileDrawPattern extends TMFileDraw {

	private Paint patternHour = null; // pattern for less than an hour
	private Paint patternDay = null; // pattern for less than an day
	private Paint patternWeek = null; // pattern for less than an week
	private Paint patternMonth = null; // pattern for less than an month
	private Paint patternYear = null; // pattern for less than an year
	private Paint patternEons = null; // pattern for more than an year

	/* --- Constructor --- */

	/**
	 * Constructor.
	 */
	public TMFileDrawPattern() {
		// loading patterns
		patternHour = TMPatternFactory.getInstance().get("PATTERN_WHITE");
		patternDay = TMPatternFactory.getInstance().get("PATTERN_DIAG1");
		patternWeek = TMPatternFactory.getInstance().get("PATTERN_DIAG2");
		patternMonth = TMPatternFactory.getInstance().get("PATTERN_DOTS");
		patternYear = TMPatternFactory.getInstance().get("PATTERN_LIGHT_GRAY");
		patternEons = TMPatternFactory.getInstance().get("PATTERN_DARK_GRAY");
	}

	/**
	 * Returns the filling of the node. The nodeAdapter should return an
	 * instance of TMFileNode.
	 * 
	 * @param nodeAdapter
	 *            we compute the filling of this node; should return an instance
	 *            of TMFileNode
	 * @return the filling of the node
	 * @throws TMExceptionBadTMNodeKind
	 *             If the node does not return an instance of TMFileNode
	 */
	@Override
	public Paint getFilling(TMNodeAdapter nodeAdapter)
			throws TMExceptionBadTMNodeKind {

		TMNode node = nodeAdapter.getNode();
		if (node instanceof TMFileNode) {
			TMFileNode fNode = (TMFileNode) node;
			long time = fNode.getDate();
			long diff = (new Date()).getTime() - time;
			if (diff <= 3600000L) { // less than an hour
				return patternHour;
			} else if (diff <= 86400000L) { // less than a day
				return patternDay;
			} else if (diff <= 604800000L) { // less than a week
				return patternWeek;
			} else if (diff <= 2592000000L) { // less than a month
				return patternMonth;
			} else if (diff <= 31536000000L) { // less than a year
				return patternYear;
			} else { // more than a year
				return patternEons;
			}
		}
		throw new TMExceptionBadTMNodeKind(this, node);
	}

}
