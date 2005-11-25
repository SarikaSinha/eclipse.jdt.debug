/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.launching.environments;


/**
 * Analyzes vm installs for compatibility with execution environments.
 *
 * @since 3.2
 * @see IExecutionEnvironmentAnalyzerDelegate
 */
public interface IExecutionEnvironmentAnalyzer extends IExecutionEnvironmentAnalyzerDelegate {
	
	/**
	 * Returns the unique identifier for this analyzer.
	 * 
	 * @return analyzer identifier
	 */
	public String getId();

}