package com.walnutcrasher.coremodvisualizer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.SwingUtilities;

import com.walnutcrasher.coremodvisualizer.gui.ClassTreeNode;

public class DecompileStep implements Runnable {

	private final Collection<ClassTreeNode> classes;

	private DecompileStep(Collection<ClassTreeNode> classes) {
		this.classes = classes;
	}

	public static volatile boolean isRunning = false;

	public static void start(Collection<ClassTreeNode> classes) {
		if(isRunning) {
			throw new IllegalStateException("decompiler allready running");
		}

		Thread t = new Thread(new DecompileStep(classes));
		t.setDaemon(true);
		t.setName("forgeflower decompiler");
		t.start();
	}

	@Override
	public void run() {
		isRunning = true;
		try {
			Path inFile = Paths.get("ff", "in.jar");
			FileOutputStream fos = new FileOutputStream(inFile.toFile());
			ZipOutputStream zipOut = new ZipOutputStream(fos);

			for(ClassTreeNode clazz : classes) {				
				String fullName = clazz.getFullName().replace('.', '/');
				ZipEntry zipEntry = new ZipEntry(fullName + ".class");
				zipOut.putNextEntry(zipEntry);
				zipOut.write(clazz.getClassData().bytecode);
			}
			
			zipOut.close();
			fos.close();
			
			List<String> ffArguments = Arrays.asList("java", "-Xmx4G", "-jar", "ff/forgeflower.jar", "-din=1", "-rbr=1", "-dgs=1", "-asc=1", "-rsy=1", "-iec=1", "-jvn=1", "-isl=0", "-iib=1", "-log=TRACE");
			ffArguments = new ArrayList<>(ffArguments); //Arrays.asList returns an unmodifiable list
			
			for(String dep : JVMConnector.classpath) {
				ffArguments.add("-e=" + dep);
			}
			
			ffArguments.add(inFile.toString());
			Path outFile = Paths.get("ff", "out.jar");
			ffArguments.add(outFile.toString());
			
			Process ps=Runtime.getRuntime().exec(ffArguments.toArray(new String[0]));
	        InputStream is=ps.getInputStream();
	        try(OutputStream os = Files.newOutputStream(Paths.get("ff", "out.log"))) {
	        	 while(ps.isAlive()) {
	 	        	byte b[]=new byte[is.available()];
	 		        int length = is.read(b,0,b.length);
	 		        os.write(b, 0, length);
	 	        }	
	        }catch(IOException e) {
	        	e.printStackTrace();
	        }
			
			try (FileInputStream fis = new FileInputStream(outFile.toFile());
	                ZipInputStream zis = new ZipInputStream(fis)) {
	            
	            ZipEntry ze;
	            while ((ze = zis.getNextEntry()) != null) {
	            	String sourceClass = ze.getName();
	            	for(ClassTreeNode clazz : classes) {
	            		String clazzName = clazz.getSourceFile();
	            		if(clazzName.equalsIgnoreCase(sourceClass)) {
	            			StringBuilder sb = new StringBuilder();
	            			int c = 0;
	            	        while ((c = zis.read()) != -1) {
	            	            sb.append((char) c);
	            	        }
	            	        
	            	        clazz.getClassData().decompiled = sb.toString();
	            	        if(clazz.getClassData().decompiled.isEmpty()) {
	            	        	clazz.getClassData().decompiled = "// Could not decompile class";
	            	        }
	            	        sourceClass = null;
	            			break;
	            		}
	            	}
	            	if(sourceClass != null) {
	            		System.err.println("Could not find ClassTreeNode for file " + ze.getName());
	            	}
	            }
	        }
			
			isRunning = false;
			SwingUtilities.invokeLater(() -> Main.window.updateTextAreas());
		}catch(IOException e) {
			e.printStackTrace();
		}finally {
			isRunning = false;
		}
	}

}
