# Introduction #

There are two aspects to coordination: receiving and sending. Source code examples for each are shown below, using selection as an example. Other kinds of coordination (colors, extent, etc.) can be done in exactly the same manner.


# Details #

**Receiving selections**. To receive a selection, you need only implement the single interface SelectionListener, like this:

```
implements SelectionListener  
```

This will in turn prompt you to add the method selectionChanged, like this:
```
public void selectionChanged(SelectionEvent e) {
	int[] selEvents = e.getSelection();
	//do something with selected events
}
```

**Sending selections**. To send selections, you need to have "addSelectionListener" and "removeSelectionListener" methods that take the appropriate parameter, like so:

```

/**
* Adds a SelectionListener
*/
	
public void addSelectionListener(SelectionListener l) {
		logger.finest("mapCan, selection listeners = "
				+    listenerList.getListenerCount(SelectionListener.class));
		listenerList.add(SelectionListener.class, l);
}

/**
* removes an SelectionListener from the component
*/
public void removeSelectionListener(SelectionListener l) {
		logger.finest("mapCan, removing a selection listener");
		listenerList.remove(SelectionListener.class, l);
}
```

Presumably you will want to notify the listeners of a new selection at some point. To do this, you can use a method like this:

```
/**
* Notify all listeners that have registered interest for notification on
* this event type. The event instance is lazily created using the
* parameters passed into the fire method.
* 
* @see EventListenerList
*/

private void fireSelectionChanged(int[] newSelection) {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	SelectionEvent e = null;

	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length - 2; i >= 0; i -= 2) {
		if (listeners[i] == SelectionListener.class) {
			// Lazily create the event:
			if (e == null) {
				e = new SelectionEvent(this, newSelection);
			}

			((SelectionListener) listeners[i + 1]).selectionChanged(e);
		}
	} // next i
}
```