package com.walnutcrasher.coremodvisualizer.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreePath;

import com.walnutcrasher.coremodvisualizer.DecompileStep;
import com.walnutcrasher.coremodvisualizer.JVMConnector;

public class MainWindow extends JFrame {
	private static final long serialVersionUID = -9139490783950887714L;
	
	private ClassTree classTree;
	private JSplitPane codeArea;
	
	private JTextArea instructionsArea;
	private JTextArea decompiledArea;
	
	private JLabel statusLabel;
	
	private boolean requestDecompile = false;

	public MainWindow() {
		super("Coremod Visualizer");
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		classTree = createTreePanel();
		codeArea = createTextPanel();
		
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, classTree, codeArea);
		mainPanel.add(pane, BorderLayout.CENTER);
		
		mainPanel.add(getBottomPanel(), BorderLayout.SOUTH);
		
		this.add(mainPanel);
		this.setVisible(true);
		this.setResizable(true);
		this.setPreferredSize(new Dimension(800, 600));
		this.pack();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		pane.setDividerLocation(0.2);
		codeArea.setDividerLocation(0.5);
	}
	
	public ClassTree getClassTree() {
		return classTree;
	}
	
	public JSplitPane getCodeArea() {
		return codeArea;
	}
	
	public void requestDecompile() {
		this.requestDecompile = true;
	}
	
	public void setStatus(String status) {
		this.statusLabel.setText(status);
	}
	
	private ClassTree createTreePanel() {
		ClassTree tree = new ClassTree();
		
		Border border = tree.getBorder();
		Border margin = new EmptyBorder(10,10,10,10);
		tree.setBorder(new CompoundBorder(border, margin));
		
		tree.getSelectionModel().addTreeSelectionListener(event -> updateTextAreas());
		
		return tree;
	}
	
	public void updateTextAreas() {
		TreePath path = classTree.getSelectionPath();
		if(path != null) {
			ClassTreeNode node = (ClassTreeNode) path.getLastPathComponent();
			if(node.getClassData().bytecode != null) {
				//class selected
				String instructions = node.getClassData().instructions;
				String decompiled = node.getClassData().decompiled;
				if(instructions == null || decompiled == null) {
					classTree.computeText(this.classTree);
					instructions = node.getClassData().instructions;
				}
				
				instructionsArea.setText(instructions);
				
				if(decompiled != null) {
					decompiledArea.setText(decompiled);
				}else {
					decompiledArea.setText("// decompiling...");
				}
			}
		}
		
		if(requestDecompile && !DecompileStep.isRunning) {
			requestDecompile = false;
			classTree.computeText(classTree);
		}
	}
	
	private JSplitPane createTextPanel() {
		JSplitPane panel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		instructionsArea = new JTextArea("// Bytecode");
		instructionsArea.setEditable(false);
		Border border = instructionsArea.getBorder();
		Border margin = new EmptyBorder(10,10,10,10);
		instructionsArea.setBorder(new CompoundBorder(border, margin));
		instructionsArea.setMinimumSize(new Dimension());
		JScrollPane instructionScroll = new JScrollPane(instructionsArea);
		panel.add(instructionScroll);
		
		decompiledArea = new JTextArea("// Decompiled code");
		decompiledArea.setEditable(false);
		border = decompiledArea.getBorder();
		margin = new EmptyBorder(10,10,10,10);
		decompiledArea.setBorder(new CompoundBorder(border, margin));
		decompiledArea.setMinimumSize(new Dimension());
		JScrollPane decompiledScroll = new JScrollPane(decompiledArea);
		panel.add(decompiledScroll);
		
		return panel;
	}
	
	private JPanel getBottomPanel() {
		JPanel panel = new JPanel();
		
		statusLabel = new JLabel("Not connected");
		panel.add(statusLabel);
		
		JButton launchButton = new JButton("connect to Minecraft");
		launchButton.setToolTipText("connects to JVM on port 5005");
		
		launchButton.addActionListener(event -> {
			this.setStatus("Connecting...");
			JVMConnector.launch();
		});
		
		panel.add(launchButton);
		
		return panel;
	}

}
