package org.eclipse.jdt.internal.debug.ui;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/
 
import java.io.IOException;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jdt.launching.sourcelookup.ArchiveSourceLocation;
import org.eclipse.jdt.launching.sourcelookup.DirectorySourceLocation;
import org.eclipse.jdt.launching.sourcelookup.IJavaSourceLocation;
import org.eclipse.jdt.launching.sourcelookup.JavaProjectSourceLocation;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * UI adapter factory for JDI Debug
 */
/*package*/ class JDIDebugUIAdapterFactory implements IAdapterFactory {

	class SourceLocationPropertiesAdapter implements IWorkbenchAdapter {
		
		private JavaElementLabelProvider fJavaElementLabelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS);
	
		/**
		 * @see IWorkbenchAdapter#getChildren(Object)
		 */
		public Object[] getChildren(Object o) {
			return new Object[0];
		}

		/**
		 * @see IWorkbenchAdapter#getImageDescriptor(Object)
		 */
		public ImageDescriptor getImageDescriptor(Object o) {
			if (o instanceof JavaProjectSourceLocation) {
				return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_PROJECT);
			} else if (o instanceof DirectorySourceLocation) {
				return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
			} else if (o instanceof ArchiveSourceLocation) {
				return JavaUI.getSharedImages().getImageDescriptor(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_JAR);
			}
			return null;
		}

		/**
		 * @see IWorkbenchAdapter#getLabel(Object)
		 */
		public String getLabel(Object o) {
			if (o instanceof JavaProjectSourceLocation) {
				return fJavaElementLabelProvider.getText(((JavaProjectSourceLocation)o).getJavaProject());
			} else if (o instanceof DirectorySourceLocation) {
				try {
					return ((DirectorySourceLocation)o).getDirectory().getCanonicalPath();
				} catch (IOException e) {
					JDIDebugUIPlugin.log(e);
					return ((DirectorySourceLocation)o).getDirectory().getName();
				}
			} else if (o instanceof ArchiveSourceLocation) {
				return ((ArchiveSourceLocation)o).getName();
			}
			return null;
		}

		/**
		 * @see IWorkbenchAdapter#getParent(Object)
		 */
		public Object getParent(Object o) {
			return null;
		}
	}
	
	/**
	 * @see IAdapterFactory#getAdapter(Object, Class)
	 */
	public Object getAdapter(Object obj, Class adapterType) {
		if (adapterType.isInstance(obj)) {
			return obj;
		}
		if (adapterType == IWorkbenchAdapter.class) {
			if (obj instanceof IJavaSourceLocation) {
				return new SourceLocationPropertiesAdapter();
			}
		}
		if (adapterType == IActionFilter.class) {
			if (obj instanceof IJavaThread) {
				return new JavaThreadActionFilter();
			} else if (obj instanceof IJavaStackFrame) {
				return new JavaStackFrameActionFilter();
			} else if (obj instanceof IJavaVariable) {
				return new JavaVariableActionFilter();
			} else if (obj instanceof IMethod) {
				return new MethodActionFilter();
			}
		}
		return null;
	}

	/**
	 * @see IAdapterFactory#getAdapterList()
	 */
	public Class[] getAdapterList() {
		return new Class[] {
			IWorkbenchAdapter.class,
			IActionFilter.class 
		};
	}
}


