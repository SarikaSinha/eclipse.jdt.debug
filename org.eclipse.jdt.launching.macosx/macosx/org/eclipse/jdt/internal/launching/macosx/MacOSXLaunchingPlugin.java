/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.launching.macosx;

import java.io.*;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Plugin;

public class MacOSXLaunchingPlugin extends Plugin {
	
	private static MacOSXLaunchingPlugin fgPlugin;
	private static final String RESOURCE_BUNDLE= "org.eclipse.jdt.internal.launching.macosx.MacOSXLauncherMessages";//$NON-NLS-1$
	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	public MacOSXLaunchingPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgPlugin= this;
	}
	
	public static MacOSXLaunchingPlugin getDefault() {
		return fgPlugin;
	}
	
	static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	static String getUniqueIdentifier() {
		if (getDefault() == null) {
			// If the default instance is not yet initialized,
			// return a static identifier. This identifier must
			// match the plugin id defined in plugin.xml
			return "org.eclipse.jdt.launching.macosx"; //$NON-NLS-1$
		}
		return getDefault().getDescriptor().getUniqueIdentifier();
	}

	static String[] wrap(Class clazz, String[] cmdLine) {
		
		for (int i= 0; i < cmdLine.length; i++) {
			String arg= cmdLine[i];
			if (arg.indexOf("swt.jar") >= 0 || arg.indexOf("org.eclipse.swt") >= 0 || "-ws".equals(arg)) {	//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				if (false) {
					
					cmdLine[0]= "/Eclipse/Apple/WWDC\'2003/simpleJavaLauncher/build/simple"; //$NON-NLS-1$
					return cmdLine;
					
				} else {
					try {
						return new String[] { createBundle(clazz, cmdLine) };
					} catch (IOException e) {
						e.printStackTrace();
					}		
				}
			}
		}
		return cmdLine;
	}
	
	static String createBundle(Class clazz, String[] cmdLine) throws IOException {
		
		String javaApplicationStub= "JavaApplicationStub"; //$NON-NLS-1$
		String version= "2.1"; //$NON-NLS-1$
		String signature= "????"; //$NON-NLS-1$
		
		String jvmVersion= "1.3.1"; //$NON-NLS-1$
		String vm= cmdLine[0];
		if (vm.indexOf("1.4.1") >= 0) //$NON-NLS-1$
			jvmVersion= "1.4.1"; //$NON-NLS-1$

		String jvmOptions= ""; //$NON-NLS-1$
		String classPath= null;
		String mainClass= null;
		String arguments= ""; //$NON-NLS-1$
		
		String workingDir= System.getProperty("user.dir"); //$NON-NLS-1$
		if (workingDir == null)
			workingDir= new File(".").getAbsolutePath(); //$NON-NLS-1$
			
		int i= 1;
		
		for (; i < cmdLine.length; i++) {
			String arg= cmdLine[i];
			
			if ("-classpath".equals(arg) || "-cp".equals(arg)) { //$NON-NLS-1$ //$NON-NLS-2$
				classPath= cmdLine[++i];
				continue;
			}
			
			if (arg.charAt(0) == '-') {
				jvmOptions+= "    <string>"+arg+"</string>\n"; //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}
			
			// main class
			mainClass= arg;
			i++;
			break;
		}
		
		for (; i < cmdLine.length; i++)
			arguments+= "    <string>"+cmdLine[i]+"</string>\n"; //$NON-NLS-1$ //$NON-NLS-2$

		int pos= mainClass.lastIndexOf('.');
		String appName= mainClass.substring(pos+1);
		
		File tmp_dir= new File("/tmp"); //$NON-NLS-1$
		File swt_dir= createDir(tmp_dir, "swt_stubs", false); //$NON-NLS-1$
		File app_dir= createDir(swt_dir, appName + ".app", true); //$NON-NLS-1$
		File contents_dir= createDir(app_dir, "Contents", false); //$NON-NLS-1$
		File macos_dir= createDir(contents_dir, "MacOS", false); //$NON-NLS-1$
		
		// JavaApplicationBundle
		InputStream is= clazz.getResourceAsStream(javaApplicationStub);
		File stub= new File(macos_dir, javaApplicationStub);
		copyFile(is, stub);
		Process p= Runtime.getRuntime().exec(new String[] { "/bin/chmod", "a+x", stub.getAbsolutePath() }); //$NON-NLS-1$ //$NON-NLS-2$
		if (p != null) {						
			try {
				p.waitFor();
			} catch (InterruptedException e) {
					// silently ignore
			}
		}
			
		// Info.plist
		File info= new File(contents_dir, "Info.plist"); //$NON-NLS-1$
		FileOutputStream fos= new FileOutputStream(info);
		OutputStreamWriter ow= new OutputStreamWriter(fos, "UTF8"); //$NON-NLS-1$
		PrintWriter w= new PrintWriter(ow);
		w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		w.println("<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">"); //$NON-NLS-1$
		w.println("<plist version=\"1.0\">"); //$NON-NLS-1$
		w.println(" <dict>"); //$NON-NLS-1$
		w.println("  <key>CFBundleDevelopmentRegion</key><string>English</string>"); //$NON-NLS-1$
		w.println("  <key>CFBundleExecutable</key><string>"+javaApplicationStub+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		w.println("  <key>CFBundleGetInfoString</key><string>"+appName+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		w.println("  <key>CFBundleInfoDictionaryVersion</key><string>6.0</string>"); //$NON-NLS-1$
		w.println("  <key>CFBundleName</key><string>"+appName+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		w.println("  <key>CFBundlePackageType</key><string>APPL</string>"); //$NON-NLS-1$
		w.println("  <key>CFBundleShortVersionString</key><string>"+version+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		w.println("  <key>CFBundleSignature</key><string>"+signature+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		w.println("  <key>CFBundleVersion</key><string>1.0.1</string>"); //$NON-NLS-1$
		w.println("  <key>Java</key><dict>"); //$NON-NLS-1$
		w.println("   <key>JVMVersion</key><string>"+jvmVersion+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (jvmOptions.length() > 0) {
			w.println("   <key>VMOptions</key><array>"); //$NON-NLS-1$
			w.print(jvmOptions);
			w.println("   </array>"); //$NON-NLS-1$
		}
		if (classPath != null)
			w.println("   <key>ClassPath</key><string>"+classPath+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (mainClass != null)
			w.println("   <key>MainClass</key><string>"+mainClass+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (workingDir != null)
			w.println("   <key>WorkingDirectory</key><string>"+workingDir+"</string>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (arguments.length() > 0) {
			w.println("   <key>Arguments</key><array>"); //$NON-NLS-1$
			w.print(arguments);
			w.println("   </array>"); //$NON-NLS-1$
		}
		w.println("  </dict>"); //$NON-NLS-1$
		w.println(" </dict>"); //$NON-NLS-1$
		w.println("</plist>"); //$NON-NLS-1$
		w.close();
		
		return stub.getAbsolutePath();
	}
	
	static void deleteDir(File dir) {
		File[] files= dir.listFiles();
		if (files != null) {
			for (int i= 0; i < files.length; i++)
				deleteDir(files[i]);
		}
		dir.delete();
	}
	
	static File createDir(File parent_dir, String dir_name, boolean remove) throws IOException {
		File dir= new File(parent_dir, dir_name);
		if (dir.exists()) {
			if (!remove)
				return dir;
			deleteDir(dir);
		}
		if (! dir.mkdir())
			throw new IOException("cannot create dir " + dir_name); //$NON-NLS-1$
		return dir;
	}
	
	static void copyFile(InputStream from, File to) throws IOException {		
		FileOutputStream os= new FileOutputStream(to);
		try {
			byte[] buffer= new byte[2048];
			while (true) {
				int n= from.read(buffer);
				if (n == -1)
					break;
				os.write(buffer, 0, n);
			}
			os.flush();
		} finally {
			try {
				from.close();
			} catch (IOException e) {
				// we don't log these
			}
			try {
				os.close();
			} catch(IOException e) {
				// we don't log these
			}
		}
	}
}
