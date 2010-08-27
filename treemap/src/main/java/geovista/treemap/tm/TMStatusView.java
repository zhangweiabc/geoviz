/*
 * TMStatusView.java
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

package geovista.treemap.tm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The TMStatusView class implements a model for the view showing the status of
 * what's happening on the TMView.
 * 
 * @author Christophe Bouthier [bouthier@loria.fr]
 * 
 */
class TMStatusView extends Observable {

	private TMStatusDisplay status = null; // the current status displayed

	/* --- Set, Unset, Increment --- */

	/**
	 * Sets the status displayed. Removes the old status displayed if necessary.
	 * 
	 * @param status
	 *            the new status to display
	 */
	void setStatus(TMStatusDisplay status) {
		this.status = status;
		TMSVSetStatus setStatus = new TMSVSetStatus(status);
		SwingUtilities.invokeLater(new NotifyTMSV(setStatus));
	}

	/**
	 * Unsets the status displayed.
	 */
	void unsetStatus() {
		status = null;
		TMSVUnsetStatus unsetStatus = new TMSVUnsetStatus();
		SwingUtilities.invokeLater(new NotifyTMSV(unsetStatus));
	}

	/**
	 * Increments the status display.
	 */
	void increment() {
		TMSVIncrement increment = new TMSVIncrement();
		SwingUtilities.invokeLater(new NotifyTMSV(increment));
	}

	/* --- Views management --- */

	/**
	 * Returns a status view. Every status view returned by this method are
	 * synch with the model.
	 * 
	 * @return the new view
	 */
	JComponent getView() {
		TMSV sv = new TMSV();
		return sv;
	}

	/**
	 * Only to pass IllegalAcessError.
	 */
	@Override
	public synchronized void setChanged() {
		super.setChanged();
	}

	/* --- Inner view --- */

	/**
	 * The TMSV class implements a concrete status view, in synch with the
	 * TMStatusView.
	 */
	class TMSV extends JPanel implements Observer {

		private TMStatusDisplay currentStatus = null; // the current status

		// displayed

		/* --- Constructor --- */

		/**
		 * Constructor.
		 */
		TMSV() {
			super(new BorderLayout());
			setPreferredSize(new Dimension(250, 60));
			addObserver(this);

			if (status != null) {
				currentStatus = status.deepClone();
			}
		}

		/* --- Set, Unset, Increment --- */

		/**
		 * Sets the status displayed. Removes the old status displayed if
		 * necessary.
		 * 
		 * @param status
		 *            the new status to display
		 */
		void setStatus(TMStatusDisplay status) {
			if (currentStatus != null) {
				remove(currentStatus);
			}
			currentStatus = status.deepClone();
			add(currentStatus, BorderLayout.CENTER);
			revalidate();
			repaint();
		}

		/**
		 * Unsets the status displayed.
		 */
		void unsetStatus() {
			if (currentStatus != null) {
				remove(currentStatus);
			}
			currentStatus = null;
			revalidate();
			repaint();
		}

		/**
		 * Increments the status display.
		 */
		void increment() {
			if (currentStatus != null) {
				currentStatus.increment();
			}
			repaint();
		}

		/* --- Dispose --- */

		/**
		 * Unregister the TMSV as an observer of the TMStatusView.
		 */
		@Override
		public void finalize() {
			deleteObserver(this);
		}

		/* --- Observer --- */

		/**
		 * Called by the TMStatusView to update the view.
		 * 
		 * @param o
		 *            the TMStatusView observed
		 * @param arg
		 *            the TMSVUpdate message
		 */
		public void update(Observable o, Object arg) {
			if (arg instanceof TMSVUpdate) {
				TMSVUpdate message = (TMSVUpdate) arg;
				message.execute(this);
			}
		}

	}

	/* --- Inner runnable --- */

	/**
	 * The NotifyTMSV implements a Runnable that will be invoked by
	 * SwingUtilities.invokeLater. It's the only way I have found to pass a
	 * parameter to the Runnable.
	 */
	class NotifyTMSV implements Runnable {

		private TMSVUpdate update = null; // the message to pass

		/**
		 * Constructor.
		 * 
		 * @param update
		 *            the update
		 */
		NotifyTMSV(TMSVUpdate update) {
			this.update = update;
		}

		/**
		 * Notify observers. Called by the event dispatching thread.
		 */
		public void run() {
			setChanged();
			notifyObservers(update);
		}
	}

	/* --- Inners updates --- */

	/**
	 * TMSVUpdate interface define an update message passed from the
	 * TMStatusView to the TMSV.
	 */
	interface TMSVUpdate {

		/**
		 * The effect of the message. Called by the TMSV.
		 * 
		 * @param sv
		 *            the calling TMSV
		 */
		public void execute(TMSV sv);
	}

	/**
	 * TMSVSetStatus class implements a set status message.
	 */
	class TMSVSetStatus implements TMSVUpdate {

		private TMStatusDisplay status = null; // the status to set

		/**
		 * Constructor.
		 * 
		 * @param status
		 *            the new status
		 */
		TMSVSetStatus(TMStatusDisplay status) {
			this.status = status;
		}

		/**
		 * Sets the status displayed. Removes the old status displayed if
		 * necessary.
		 * 
		 * @param sv
		 *            the calling TMSV
		 */
		public void execute(TMSV sv) {
			sv.setStatus(status);
		}
	}

	/**
	 * TMSVUnsetStatus class implements an unset status message.
	 */
	class TMSVUnsetStatus implements TMSVUpdate {

		/**
		 * Unsets the status displayed.
		 * 
		 * @param sv
		 *            the calling TMSV
		 */
		public void execute(TMSV sv) {
			sv.unsetStatus();
		}
	}

	/**
	 * TMSVIncrement class implements an increment message.
	 */
	class TMSVIncrement implements TMSVUpdate {

		/**
		 * Increments the status displayed.
		 * 
		 * @param sv
		 *            the calling TMSV
		 */
		public void execute(TMSV sv) {
			sv.increment();
		}
	}

}
