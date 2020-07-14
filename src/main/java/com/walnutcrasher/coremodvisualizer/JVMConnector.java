package com.walnutcrasher.coremodvisualizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ByteValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PathSearchingVirtualMachine;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;

public class JVMConnector {
	
	public static List<String> classpath;
	public static volatile boolean isConnected = false;
	
	private JVMConnector() {}
	
	public void connnect() throws IOException, IllegalConnectorArgumentsException, AbsentInformationException, InterruptedException, IncompatibleThreadStateException {
		isConnected = true;
		List<AttachingConnector> connectors = Bootstrap.virtualMachineManager().attachingConnectors();
		if(connectors.isEmpty()) {
			throw new IllegalStateException("no AttachingConnector found");
		}
		AttachingConnector connector = connectors.get(0);
		Map<String, Connector.Argument> env = connector.defaultArguments();
		env.get("hostname").setValue("localhost");
		env.get("port").setValue("5005");
		
		VirtualMachine vm = connector.attach(env);
		SwingUtilities.invokeLater(() -> Main.window.setStatus("connected to JVM"));
		
		if(vm instanceof PathSearchingVirtualMachine) {
			PathSearchingVirtualMachine pathVm = (PathSearchingVirtualMachine) vm;
			classpath = pathVm.classPath();
		}else {
			classpath = new ArrayList<>();
		}
		
		boolean foundModlauchner = false;
		for(String dep : classpath) {
			if(dep.contains("modlauncher")) {
				foundModlauchner = true;
				if(!dep.endsWith(Constants.MODLAUCHER_VERSION + ".jar")) {
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,"It look like you are using a diffrent modlaucher version than this program is compiled for. Things may not work well","Modlauncher", JOptionPane.CANCEL_OPTION));
				}
				break;
			}
		}
		
		if(!foundModlauchner) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,"Could not find used modlauncher version. Things may not work well","Modlauncher", JOptionPane.CANCEL_OPTION));
		}
		
		ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
		classPrepareRequest.addClassFilter(Constants.MODLAUCNHER_TRANSFORMER);
		classPrepareRequest.enable();
		
		EventSet eventSet = null;
		Method toByteArrayMethod = null;
		Set<String> innerClasses = new HashSet<>();
		try {
			while ((eventSet = vm.eventQueue().remove()) != null) {
				for (Event event : eventSet) {

					//set breakpoint
					if (event instanceof ClassPrepareEvent) {
						ClassPrepareEvent evt = (ClassPrepareEvent) event;
						ClassType classType = (ClassType) evt.referenceType();
						
						List<Location> locations = classType.locationsOfLine(Constants.MODLAUNCHER_BREAKLINE);

						Location location = locations.get(0);
						BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
						bpReq.enable();
						
						for(int i : Constants.MODLAUNCHER_SHORTCUT_BREAKLINES) {
							locations = classType.locationsOfLine(i);

							location = locations.get(0);
							bpReq = vm.eventRequestManager().createBreakpointRequest(location);
							bpReq.enable();
						}
					}
					
					//breakpoint fired
					if (event instanceof BreakpointEvent) {
						// Get values of all variables that are visible and print
						StackFrame stackFrame = ((BreakpointEvent) event).thread().frame(0);
						Map<LocalVariable, Value> visibleVariables = (Map<LocalVariable, Value>) stackFrame
								.getValues(stackFrame.visibleVariables());

						boolean needsTransforming = this.<BooleanValue>getVar(visibleVariables, "needsTransforming").booleanValue();
						String className = this.<StringReference>getVar(visibleVariables, "className").value();
						
						if(needsTransforming || innerClasses.contains(className)) {
							try {
								ThreadReference thread = ((BreakpointEvent) event).thread();
								
								ArrayReference transformedBytes = null;
								if(((BreakpointEvent) event).location().lineNumber() == Constants.MODLAUNCHER_BREAKLINE) {
									if(toByteArrayMethod == null) {
										toByteArrayMethod = vm.classesByName(Constants.CLASS_WRITER).get(0).methodsByName("toByteArray").get(0);	
									}
									transformedBytes = (ArrayReference) this.<ObjectReference>getVar(visibleVariables, "cw").invokeMethod(thread, toByteArrayMethod, new ArrayList<>(), 0);	
								}else {
									transformedBytes = this.<ArrayReference>getVar(visibleVariables, "inputClass");
								}
								
								byte[] classBytes = new byte[transformedBytes.length()];
								for(int i = 0; i < classBytes.length; i++) {
									ByteValue byteVal = (ByteValue)transformedBytes.getValue(i);
									classBytes[i] = byteVal.byteValue();
								}
								
								ClassReader cr = new ClassReader(classBytes);
							    ClassNode cn = new ClassNode();
							    cr.accept(cn, 0);
							    cn.innerClasses.stream()
							    	.map(cls -> cls.name)
							    	.filter(cls -> cls.startsWith(className.replace('.', '/')))
							    	.map(cls -> cls.replace('/', '.'))
							    	.forEach(innerClasses::add);
								
								SwingUtilities.invokeLater(() -> Main.window.getClassTree().addClass(className, classBytes));
							}catch(InvalidTypeException | ClassNotLoadedException | IncompatibleThreadStateException | InvocationException e) {
								System.err.println("could not get transformed class bytes");
								e.printStackTrace();
							}
						}

					}
					vm.resume();
				}

			}
		} catch (VMDisconnectedException e) {
			SwingUtilities.invokeLater(() -> Main.window.setStatus("disconnected"));
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Value> T getVar(Map<LocalVariable, Value> vars, String name) {
		for(Map.Entry<LocalVariable, Value> var : vars.entrySet()) {
			if(var.getKey().name().equals(name)) {
				return (T) var.getValue();
			}
		}
		return null;
	}
	
	public static void launch() {
		if(isConnected)
			return;
		
		Thread connectThread = new Thread(() -> {
			JVMConnector connector = new JVMConnector();
			try {
				connector.connnect();
			}catch(Exception e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> Main.window.setStatus("Error: " + e.getMessage()));
			}finally {
				isConnected = false;
			}
		});
		connectThread.setName("JVM connector");
		connectThread.setDaemon(true);
		connectThread.start();
	}
	
	

}
