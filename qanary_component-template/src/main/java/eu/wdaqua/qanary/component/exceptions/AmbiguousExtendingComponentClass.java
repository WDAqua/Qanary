package eu.wdaqua.qanary.component.exceptions;

public class AmbiguousExtendingComponentClass extends Exception {

	public AmbiguousExtendingComponentClass(Class<?> extendedClass) {
		super("" //
				+ "Found multiple classes extending " //
				+ extendedClass.getName() //
				+ " in the current classpath");
	}
}

