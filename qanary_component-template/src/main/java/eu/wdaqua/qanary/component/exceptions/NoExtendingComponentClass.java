package eu.wdaqua.qanary.component.exceptions;

public class NoExtendingComponentClass extends Exception {

	public NoExtendingComponentClass(Class<?> extendedClass) {
		super("" //
				+ "Could not find any class extending " //
				+ extendedClass.getName() //
				+ " in the current classpath");
	}
}
