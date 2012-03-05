/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.launching;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdi.Bootstrap;
import org.eclipse.jdi.TimeoutException;
import org.eclipse.jdi.internal.connect.SACoreAttachingConnectorImpl;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.osgi.util.NLS;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Connector.Argument;

/**
 * This {@link Connector} is used to attach to a core dump from a VM.
 * <br><br>
 * It is worth noting that you can only connect to a core dump that matches the same word size (32bit / 64bit)
 * *and* using the same level of VM which produced the core dump.
 * <br><br>
 * For example a core dump from a 32bit Java 1.6 VM must be debugged only on a 32bit Java 1.6 VM. All others will fail.
 * 
 * @see http://docs.oracle.com/javase/6/docs/technotes/guides/jpda/conninv.html#Connectors
 * @since 3.8
 */
public class SACoreAttachingConnector implements IVMConnector {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMConnector#connect(java.util.Map, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.debug.core.ILaunch)
	 */
	public void connect(Map<String, String> arguments, IProgressMonitor monitor, ILaunch launch) throws CoreException {
		SubMonitor lmonitor = SubMonitor.convert(monitor, LaunchingMessages.SACoreAttachingConnector_0, 4);
		try {
			AttachingConnector connector = getAttachingConnector();
			if(lmonitor.isCanceled()) {
				return;
			}
			lmonitor.worked(1);
			String corepath = arguments.get(SACoreAttachingConnectorImpl.CORE_PATH);
			if(corepath == null) {
				abort(LaunchingMessages.SACoreAttachingConnector_1, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_CORE_FILE);
			}
			Map<String, Argument> args = connector.defaultArguments();
			Connector.Argument arg = args.get(SACoreAttachingConnectorImpl.CORE_PATH);
			arg.setValue(corepath);
			if(lmonitor.isCanceled()) {
				return;
			}
			lmonitor.worked(1);
			String exepath = arguments.get(SACoreAttachingConnectorImpl.JAVA_EXECUTABLE_PATH);
			if(exepath == null) {
				abort(LaunchingMessages.SACoreAttachingConnector_2, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_JAVA_EXECUTABLE_PATH);
				
			}
			arg = args.get(SACoreAttachingConnectorImpl.JAVA_EXECUTABLE_PATH);
			arg.setValue(exepath);
			if(lmonitor.isCanceled()) {
				return;
			}
			lmonitor.worked(1);
			try {
				VirtualMachine vm = connector.attach(args);
				String vmLabel = constructVMLabel(vm, corepath, exepath, launch.getLaunchConfiguration());
				IDebugTarget debugTarget= JDIDebugModel.newDebugTarget(launch, vm, vmLabel, null, false, true);
				launch.addDebugTarget(debugTarget);
				lmonitor.worked(1);
			}
			catch(TimeoutException toe) {
				abort(LaunchingMessages.SocketAttachConnector_0, toe, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
			}
			catch(UnknownHostException uhe) {
				abort(NLS.bind(LaunchingMessages.SocketAttachConnector_Failed_to_connect_to_remote_VM_because_of_unknown_host____0___1, new String[]{corepath}), uhe, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED); 
			}
			catch (ConnectException ce) {
				abort(LaunchingMessages.SocketAttachConnector_Failed_to_connect_to_remote_VM_as_connection_was_refused_2, ce, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED); 
			}
			catch(IOException ioe) {
				abort(LaunchingMessages.SocketAttachConnector_Failed_to_connect_to_remote_VM_1, ioe, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
			}
			catch (IllegalConnectorArgumentsException icae) {
				abort(LaunchingMessages.SACoreAttachingConnector_3, icae, IJavaLaunchConfigurationConstants.ERR_REMOTE_VM_CONNECTION_FAILED);
			}
		}
		finally {
			if(!lmonitor.isCanceled()) {
				lmonitor.done();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMConnector#getName()
	 */
	public String getName() {
		return LaunchingMessages.SACoreAttachingConnector_4;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMConnector#getIdentifier()
	 */
	public String getIdentifier() {
		return IJavaLaunchConfigurationConstants.ID_SA_ATTACH_VM_CONNECTOR;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMConnector#getDefaultArguments()
	 */
	public Map<String, Argument> getDefaultArguments() throws CoreException {
		return getAttachingConnector().defaultArguments();
	}

	/**
	 * Return the socket transport attaching connector
	 * 
	 * @return the {@link AttachingConnector}
	 * @exception CoreException if unable to locate the connector
	 */
	protected static AttachingConnector getAttachingConnector() throws CoreException {
		AttachingConnector connector= null;
		Iterator<AttachingConnector> iter= Bootstrap.virtualMachineManager().attachingConnectors().iterator();
		while (iter.hasNext()) {
			AttachingConnector lc= iter.next();
			if (lc.name().equals(SACoreAttachingConnectorImpl.JDI_CONNECTOR_ID)) {
				connector= lc;
				break;
			}
		}
		return connector;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMConnector#getArgumentOrder()
	 */
	public List<String> getArgumentOrder() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(SACoreAttachingConnectorImpl.CORE_PATH);
		list.add(SACoreAttachingConnectorImpl.JAVA_EXECUTABLE_PATH);
		return list;
	}
	
	/**
	 * Throws a core exception with an error status object built from
	 * the given message, lower level exception, and error code.
	 * 
	 * @param message the status message
	 * @param exception lower level exception associated with the
	 *  error, or <code>null</code> if none
	 * @param code error code
	 * @throws CoreException if an error occurs
	 */
	protected void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, LaunchingPlugin.getUniqueIdentifier(), code, message, exception));
	}
	
	/**
	 * Helper method that constructs a human-readable label for a remote VM.
	 * @param vm the VM
	 * @param corefile the full path to the core file being 'debugged'
	 * @param javaExecutable the full path to the Java executable
	 * @param configuration the backing configuration
	 * @return the new label for the VM
	 */
	protected String constructVMLabel(VirtualMachine vm, String corefile, String javaExecutable, ILaunchConfiguration configuration) {
		String name = null;
		try {
			name = vm.name();
		} catch (TimeoutException e) {
			// do nothing
		} catch (VMDisconnectedException e) {
			// do nothing
		}
		if (name == null) {
			if (configuration == null) {
				name = ""; //$NON-NLS-1$
			} else {
				name = configuration.getName();
			}
		}
		StringBuffer buffer = new StringBuffer(name);
		buffer.append('['); 
		buffer.append(corefile);
		buffer.append('@');
		buffer.append(javaExecutable);
		buffer.append(']');
		return buffer.toString();
	}
}
