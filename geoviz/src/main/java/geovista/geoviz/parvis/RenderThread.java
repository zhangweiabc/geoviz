/*
 * RenderThread.java
 *
 * Created on 11. Februar 2002, 19:38
 *
 * Licensed under GNU General Public License (GPL).
 * See http://www.gnu.org/copyleft/gpl.html
 */

package geovista.geoviz.parvis;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import geovista.image_blur.image.BoxBlurFilter;

/**
 * 
 * @author flo
 * 
 */
class RenderThread extends Thread {
	protected final static Logger logger = Logger.getLogger(RenderThread.class
			.getName());

	volatile boolean useSelectionBlur = true;
	/** flags to control rendering */
	volatile boolean quality = false;
	volatile boolean progressive = false;
	volatile boolean isBrushThread = false;

	/** flags to indicate thread state */
	volatile boolean isWorking = false;
	volatile boolean doWork = false;
	volatile boolean wasInterrupted = false;
	volatile boolean progressiveInterrupted = false;
	volatile boolean secondPass = false;
	volatile boolean qualitychanged = false;

	int startAxis, stopAxis;
	int progressiveStartAxis, progressiveStopAxis;
	int lastStart = 0;
	int lastStop = 0;

	int ids[] = null;
	BufferedImage renderedImage = null;

	Stroke stroke = new BasicStroke();
	Color color = Color.black;

	BasicParallelDisplayUI ui = null;
	ParallelDisplay comp = null;

	RenderThread(BasicParallelDisplayUI ui) {
		this.ui = ui;
		setPriority(Thread.MIN_PRIORITY);
	}

	void setCurrentComponent(ParallelDisplay comp) {
		this.comp = comp;
	}

	void setBrushThread(boolean brushMode) {
		isBrushThread = brushMode;
	}

	void setQuality(boolean quality, boolean progressive) {
		this.progressive = progressive;
		if (progressive) {
			this.quality = false;
		} else {
			if (this.quality != quality) {
				qualitychanged = true;
				this.quality = quality;
			}
		}
	}

	synchronized void setRegion(int startAxis, int stopAxis) {
		if (wasInterrupted || ((isWorking || doWork) && !secondPass)
				|| (isWorking && secondPass && doWork)) {
			// old render area still invalid -> expand
			if (startAxis < this.startAxis) {
				this.startAxis = startAxis;
			}
			if (stopAxis > this.stopAxis) {
				this.stopAxis = stopAxis;
			}
			if (startAxis < progressiveStartAxis) {
				progressiveStartAxis = startAxis;
			}
			if (stopAxis > progressiveStopAxis) {
				progressiveStopAxis = stopAxis;
			}
		} else if (progressiveInterrupted || (isWorking || doWork)) {
			this.startAxis = startAxis;
			this.stopAxis = stopAxis;
			if (startAxis < progressiveStartAxis) {
				progressiveStartAxis = startAxis;
			}
			if (stopAxis > progressiveStopAxis) {
				progressiveStopAxis = stopAxis;
			}
		} else {
			this.startAxis = startAxis;
			this.stopAxis = stopAxis;
			progressiveStartAxis = startAxis;
			progressiveStopAxis = stopAxis;
		}
		// this.ids = ids.clone();
		logger.finest("RenderThread: setting repaint axes: " + this.startAxis
				+ ", " + this.stopAxis);
	}

	void setStyle(Stroke stroke, Color color) {
		this.stroke = stroke;
		this.color = color;
	}

	synchronized boolean isWorking() {
		return isWorking;
	}

	public BufferedImage getRenderedImage() {
		return renderedImage;
	}

	public int getRenderedRegionStart() {
		return lastStart;
	}

	public int getRenderedRegionStop() {
		return lastStop;
	}

	@Override
	public void run() {
		while (true) {
			synchronized (this) {
				isWorking = false;
				// wait for next rendering to be queued
				do {
					try {
						if (!doWork) {
							logger.finest("RenderThread: waiting...");
							this.wait();
						}
						logger.finest("RenderThread: exit waiting.");
					} catch (InterruptedException iex) {
						logger.finest("RenderThread: interruptedExcpetion.");
						// rendering has been cancelled
					}
				} while (Thread.interrupted());

				isWorking = true;
				doWork = false;
				renderedImage = null;
				qualitychanged = false;
			}

			logger.finest("RenderThread: run loop start...");

			boolean progress = true;

			while (comp != null && progress) {
				String modestr;

				if (progressive && quality) {
					secondPass = true;

					logger
							.finest("RenderThread: starting progressive paint...");
					modestr = "[quality]";

					// 2nd pass: lower priority, keep response time low
					Thread.yield();
				} else {
					secondPass = false;

					logger.finest("RenderThread: starting paint...");
					modestr = "[preview]";
				}

				// this is the main rendering routine
				comp.fireProgressEvent(new ProgressEvent(comp,
						ProgressEvent.PROGRESS_START, 0.0f, "rendering "
								+ modestr));

				BufferedImage img = new BufferedImage(comp.getWidth(), comp
						.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = (Graphics2D) img.getGraphics();
				setupRendering(g2, quality, stroke, color);

				// render all records
				int i = 0;
				float brushVal = 0.0f;
				if (isBrushThread) {
					// color = comp.getBrushedColor();// changed fah july 30 02
				}

				for (; i < comp.getNumRecords(); i++) {
					if (i % 300 == 0) {
						comp.fireProgressEvent(new ProgressEvent(comp,
								ProgressEvent.PROGRESS_UPDATE, ((float) i)
										/ comp.getNumRecords(), "rendering "
										+ modestr));
					}

					if (!isBrushThread
							|| (brushVal = comp.getBrushValue(i)) > 0.0f) {
						// select records in brushmode, render all in normal
						// mode
						// skip soft edges

						if (!quality && isBrushThread && brushVal < 0.8) {
							continue;
						}

						if (isBrushThread && quality) {

							Color col = new Color(color.getRed(), color
									.getBlue(), color.getGreen(),
									(int) (255 * brushVal));
							if (logger.isLoggable(Level.FINEST)) {
								logger.finest("Brush value: " + brushVal
										+ " alpha: " + col.getAlpha());
							}

							ui.drawRecord(g2, comp, i, progressiveStartAxis,
									progressiveStopAxis, brushVal);

						}

						if (secondPass) {

							ui.drawRecord(g2, comp, i, progressiveStartAxis,
									progressiveStopAxis, brushVal);

						} else {

							ui.drawRecord(g2, comp, i, startAxis, stopAxis,
									brushVal);

						}

						if (qualitychanged || secondPass) {
							// 2nd pass: lower priority, keep response time low
							Thread.yield();
							if (Thread.interrupted()) {
								progressiveInterrupted = true;
								logger.finest("### breaking!");
								break;
							}
						}
					}

				}

				if (i == comp.getNumRecords()) {
					// finished all records
					wasInterrupted = false;

					renderedImage = img;

					if (secondPass) {
						lastStart = progressiveStartAxis;
						lastStop = progressiveStopAxis;
						progressiveInterrupted = false;

					} else {
						lastStart = startAxis;
						lastStop = stopAxis;
					}
					if (useSelectionBlur && (isBrushThread == false)) {

						// drawSlections(g, pointColors,
						// dataArrayX.length());
						// g.fillRect(0, 0, this.getWidth(),
						// this.getHeight());
						BoxBlurFilter filter = new BoxBlurFilter();
						filter.setHRadius(3);
						filter.setVRadius(2);
						filter.setIterations(2);
						// maybe we could eliminate the use of the extra
						// buffer?
						// we could use the panel itself as one drawing
						// surface
						// and the drawingBuff as the other.

						// OK, new theory. Grabbing bufferedimages this
						// often is causing
						// problems
						// so we cache.
						BufferedImage blurBuff = new BufferedImage(
								renderedImage.getWidth(comp), renderedImage
										.getHeight(comp),
								BufferedImage.TYPE_INT_ARGB);
						// VolatileImage blurBuff=
						// this.getGraphicsConfiguration().createCompatibleVolatileImage(this.drawingBuff.getWidth(this),
						// this.drawingBuff.getHeight(this));
						blurBuff.getGraphics().drawImage(renderedImage, 0, 0,
								comp);
						filter.filter(blurBuff, blurBuff);
						renderedImage = blurBuff;
						// g2.drawImage(blurBuff, null, 0, 0);

					}
					comp.fireProgressEvent(new ProgressEvent(comp,
							ProgressEvent.PROGRESS_FINISH, 1.0f, "rendering "
									+ modestr));
					comp.repaint();

					logger.finest("RenderThread: paint finished...");

					if (progressive) {
						if (quality) {
							quality = false;
							progress = false;
						} else {
							quality = true;
						}
					} else {
						progress = false;
					}
				} else {
					// we have been interrupted
					logger.finest("RenderThread: paint interrupted...");
					progress = false;
					if (secondPass) {
						// 2nd pass progressive -> throw away
						wasInterrupted = false;
						quality = false;
					}
				}
				secondPass = false;

			}
		}
	}

	synchronized void render() {
		logger.finest(getName() + ".render() called");
		if (isWorking) {
			interrupt();
		}
		doWork = true;
		notify();
	}

	public void reset() {
		// throw away image
		renderedImage = null;
	}

	public void setupRendering(Graphics2D g2, boolean quality, Stroke stroke,
			Color color) {
		RenderingHints qualityHints = new RenderingHints(null);

		if (quality) {
			qualityHints.put(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			qualityHints.put(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);

			AlphaComposite ac = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.7f);
			g2.setComposite(ac);

			g2.setStroke(stroke);
			g2.setColor(color);
		} else {
			qualityHints.put(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_OFF);
			qualityHints.put(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_SPEED);

			g2.setStroke(new BasicStroke());
			// strip out alpha
			g2.setColor(new Color(color.getRed(), color.getGreen(), color
					.getBlue()));
		}

		g2.setRenderingHints(qualityHints);

	}

}
