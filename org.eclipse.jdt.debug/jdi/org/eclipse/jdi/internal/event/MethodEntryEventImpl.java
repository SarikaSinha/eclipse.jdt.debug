package org.eclipse.jdi.internal.event;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.DataInputStream;
import java.io.IOException;

import org.eclipse.jdi.internal.MirrorImpl;
import org.eclipse.jdi.internal.VirtualMachineImpl;
import org.eclipse.jdi.internal.request.RequestID;

import com.sun.jdi.Method;
import com.sun.jdi.event.MethodEntryEvent;

/**
 * this class implements the corresponding interfaces
 * declared by the JDI specification. See the com.sun.jdi package
 * for more information.
 *
 */
public class MethodEntryEventImpl extends LocatableEventImpl implements MethodEntryEvent {
	/** Jdwp Event Kind. */
	public static final byte EVENT_KIND = EVENT_METHOD_ENTRY;

	/**
	 * Creates new MethodEntryEventImpl.
	 */
	private MethodEntryEventImpl(VirtualMachineImpl vmImpl, RequestID requestID) {
		super("MethodEntryEvent", vmImpl, requestID);
	}
		
	/**
	 * @return Creates, reads and returns new EventImpl, of which requestID has already been read.
	 */
	public static MethodEntryEventImpl read(MirrorImpl target, RequestID requestID, DataInputStream dataInStream) throws IOException {
		VirtualMachineImpl vmImpl = target.virtualMachineImpl();
		MethodEntryEventImpl event = new MethodEntryEventImpl(vmImpl, requestID);
		event.readThreadAndLocation(target, dataInStream);
		return event;
   	}
   	
	/**
	 * @return Returns the method that was entered.
	 */
	public Method method() {
		return fLocation.method();
	}
}
