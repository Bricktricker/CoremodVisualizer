package com.walnutcrasher.coremodvisualizer.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.tree.TreeNode;

public class ClassTreeNode implements TreeNode {

	private final String name;
	private final List<ClassTreeNode> children;
	private ClassTreeNode parent;
	private final ClassData classData;
	
	public ClassTreeNode(String name) {
		this.name = name;
		this.children = new ArrayList<>();
		this.classData = new ClassData();
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		List<String> nameList = new LinkedList<>();
		ClassTreeNode parent = this;
		while(parent != null) {
			nameList.add(0, parent.getName());
			parent = (ClassTreeNode) parent.getParent();
		}
		nameList.remove(0); //remove to 'Classes' entry
		
		return nameList.stream().collect(Collectors.joining("."));
	}
	
	public String getSourceFile() {
		List<String> nameList = new LinkedList<>();
		ClassTreeNode parent = this;
		while(parent != null) {
			nameList.add(0, parent.getName());
			parent = (ClassTreeNode) parent.getParent();
		}
		nameList.remove(0); //remove to 'Classes' entry
		
		return nameList.stream().collect(Collectors.joining("/")) + ".java";
	}
	
	public void add(ClassTreeNode child) {
		if(getIndex(child) != -1) {
			throw new IllegalArgumentException("child allready in tree");
		}
		
		child.parent = this;
		children.add(child);
		Collections.sort(children, (a,b) -> a.getName().compareTo(b.getName()));
	}
	
	public ClassData getClassData() {
		return classData;
	}
	
	public boolean hasChild(String child) {
		return children.stream()
			.map(c -> c.name)
			.anyMatch(n -> n.equals(child));
	}
	
	public ClassTreeNode getChild(String name) {
		for(int i = 0; i < children.size(); i++) {
			if(children.get(i).getName().equals(name)) {
				return children.get(i);
			}
		}
		return null;
	}
	 
	@Override
	public Enumeration<ClassTreeNode> children() {
		return new IteratorEnumaration<ClassTreeNode>(this.children.iterator());
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public TreeNode getChildAt(int childIndex) {
		return this.children.get(childIndex);
	}

	@Override
	public int getChildCount() {
		return this.children.size();
	}

	@Override
	public int getIndex(TreeNode node) {
		if(!(node instanceof ClassTreeNode)) {
			return -1;
		}
		ClassTreeNode classNode = (ClassTreeNode) node;
		for(int i = 0; i < children.size(); i++) {
			if(children.get(i).getName().equals(classNode.name)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public TreeNode getParent() {
		return this.parent;
	}

	@Override
	public boolean isLeaf() {
		return this.children.isEmpty();
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public Stream<ClassTreeNode> flattened() {
        return Stream.concat(
                Stream.of(this),
                children.stream().flatMap(ClassTreeNode::flattened));
    }
	
	private static class IteratorEnumaration<T> implements Enumeration<T> {
		private final Iterator<T> itr;
		public IteratorEnumaration(Iterator<T> itr) {
			this.itr = itr;
		}
		@Override
		public boolean hasMoreElements() {
			return itr.hasNext();
		}
		@Override
		public T nextElement() {
			return itr.next();
		}
	}
	
	public static class ClassData {
		public byte[] bytecode;
		public String instructions;
		public String decompiled;
	}

}
