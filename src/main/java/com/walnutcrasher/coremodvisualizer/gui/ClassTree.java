package com.walnutcrasher.coremodvisualizer.gui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import com.walnutcrasher.coremodvisualizer.DecompileStep;
import com.walnutcrasher.coremodvisualizer.Main;

public class ClassTree extends JTree {
	private static final long serialVersionUID = 5920305554405075798L;
	
	public ClassTree() {
		super(new ClassTreeNode("Classes"));
	}
	
	public void addClass(String clazz, byte[] bytecode) {
		String[] parts = clazz.split("\\.");
		ClassTreeNode node = (ClassTreeNode) this.getModel().getRoot();
		for(String part : parts) {
			if(node.hasChild(part)) {
				node = node.getChild(part);
			}else {
				ClassTreeNode childNode = new ClassTreeNode(part);
				node.add(childNode);
				node = childNode;
			}
		}
		node.getClassData().bytecode = bytecode;
		
		((DefaultTreeModel)this.getModel()).reload();
	}
	
	public void computeText(ClassTree tree) {
		ClassTreeNode root = (ClassTreeNode) tree.getModel().getRoot();
		
		Set<ClassTreeNode> nodes = root.flattened()
				.filter(n -> n.getClassData().bytecode != null)
				.filter(n -> n.getClassData().decompiled == null || n.getClassData().instructions == null)
				.collect(Collectors.toSet());
		
		if(nodes.isEmpty()) {
			return;
		}
		
		List<ClassTreeNode> decompileNodes = new ArrayList<>();
		
		for(ClassTreeNode node : nodes) {
			if(node.getClassData().instructions == null) {
				StringBuilder sb = new StringBuilder();
				ClassReader cr = new ClassReader(node.getClassData().bytecode);
			    ClassNode cn = new ClassNode();
			    cr.accept(cn, 0);
			    final List<MethodNode> mns = cn.methods;
			    Printer printer = new Textifier();
			    TraceMethodVisitor mp = new TraceMethodVisitor(printer);
			    for (MethodNode mn : mns) {
			        InsnList inList = mn.instructions;
			        sb.append(mn.name);
			        sb.append('\n');
			        for (int i = 0; i < inList.size(); i++) {
			            inList.get(i).accept(mp);
			            StringWriter sw = new StringWriter();
			            printer.print(new PrintWriter(sw));
			            printer.getText().clear();
			            sb.append(sw.toString());
			        }
			        sb.append('\n');
			    }
			    
			    node.getClassData().instructions = sb.toString();
			}
			
			if(node.getClassData().decompiled == null) {
				decompileNodes.add(node);
			}
		}
		
		if(!decompileNodes.isEmpty()) {
			if(!DecompileStep.isRunning) {
				DecompileStep.start(nodes);
			}else {
				Main.window.requestDecompile();
			}
		}
		
	}

}
