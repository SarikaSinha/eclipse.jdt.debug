package org.eclipse.jdt.internal.debug.ui.actions;

/**********************************************************************
Copyright (c) 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

public class ExcludeExceptionLocationAction extends ObjectActionDelegate {

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IStructuredSelection selection= getCurrentSelection();
		if (selection == null) {
			return;
		}
		Iterator enum= selection.iterator();
		
		while (enum.hasNext()) {
			IJavaThread thread= (IJavaThread)enum.next();
			try {
				IBreakpoint[] breakpoints= thread.getBreakpoints();
				IJavaStackFrame frame= (IJavaStackFrame)thread.getTopStackFrame();
				String newFilter= frame.getDeclaringTypeName();
				for (int i = 0; i < breakpoints.length; i++) {
					IBreakpoint breakpoint = breakpoints[i];
					if (breakpoint instanceof IJavaExceptionBreakpoint) {
						IJavaExceptionBreakpoint exBreakpoint= (IJavaExceptionBreakpoint)breakpoint;
						String[] current= exBreakpoint.getExclusionFilters();
						String[] newFilters= new String[current.length+1];
						System.arraycopy(current, 0, newFilters, 0, current.length);
						newFilters[current.length]= newFilter;
						exBreakpoint.setExclusionFilters(newFilters);
						action.setEnabled(false);
					}
				}
			} catch (CoreException de) {
				JDIDebugUIPlugin.log(de);
			}
		}
	}
}
