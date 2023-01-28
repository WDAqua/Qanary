package eu.wdaqua.qanary;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RuntimeDependenciesTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		// DatatypeConverter is required, however, it is validated poorly since Java 9
		// this statement ensures that broken dependencies are uncovered
		@SuppressWarnings("unused")
		jakarta.xml.bind.DatatypeConverter justForSafety;
		javax.xml.bind.annotation.XmlRootElement justForSafety2;
	}

	@Test
	void test() {
	}

}
