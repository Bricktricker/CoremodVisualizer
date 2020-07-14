package com.walnutcrasher.coremodvisualizer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.walnutcrasher.coremodvisualizer.gui.MainWindow;

public class Main {
	
	public static MainWindow window;
	
	public static void main(String[] args) {
		window = new MainWindow();
		
		//get forge flower decompiler
		Path forgeflower = Paths.get("ff", "forgeflower.jar");
		if(!forgeflower.toFile().exists()) {
			forgeflower.getParent().toFile().mkdir();
			Thread downloadThread = new Thread(() -> {
				try {
					InputStream in = new URL(Constants.FORGE_FLOWER_URL).openStream();
					Files.copy(in, forgeflower, StandardCopyOption.REPLACE_EXISTING);
					System.out.println("ff download complete");
				}catch(IOException e) {
					//TODO: display warning
					e.printStackTrace();
				}
			});
			downloadThread.setName("forgeflower downloader");
			downloadThread.start();
		}
	}

}
