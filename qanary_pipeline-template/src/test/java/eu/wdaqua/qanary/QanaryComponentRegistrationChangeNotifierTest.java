package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import eu.wdaqua.qanary.business.QanaryComponent;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnectorQanaryInternal;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = QanaryPipeline.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan("eu.wdaqua.qanary")
@AutoConfigureWebClient
class QanaryComponentRegistrationChangeNotifierTest {

	@Autowired
	QanaryComponentRegistrationChangeNotifier myNotifier;

	@MockBean
	QanaryTripleStoreConnector myQanaryTripleStoreConnector = Mockito.mock(QanaryTripleStoreConnectorQanaryInternal.class);

	final static String COMPONENTNAME = "myMockedInstance";

	/**
	 * add component and test its availability
	 */
	@Test
	void testAddComponentAndCheckAvailability() {

		assertNotEquals(null, myNotifier);
		myNotifier.getAvailableComponents().clear();
		assertEquals(0, myNotifier.getAvailableComponentNames().size());

		Instance myMockedInstance = Mockito.mock(Instance.class);
		String myMockedInstanceName = COMPONENTNAME;

		Registration myMockedRegistration = Mockito.mock(Registration.class);
		Mockito.when(myMockedInstance.getRegistration()).thenReturn(myMockedRegistration);
		StatusInfo myMockedStatusInfo = Mockito.mock(StatusInfo.class);
		Mockito.when(myMockedInstance.getStatusInfo()).thenReturn(myMockedStatusInfo);

		myNotifier.addAvailableComponent(myMockedInstanceName, myMockedInstance);
		Mockito.when(myMockedInstance.getRegistration().getName()).thenReturn(COMPONENTNAME);

		assertEquals(1, myNotifier.getAvailableComponentNames().size());

		assertNotEquals(null, myNotifier.getAvailableComponents().getOrDefault(COMPONENTNAME, null));
		assertEquals(COMPONENTNAME, myNotifier.getAvailableComponentNames().get(0));
		assertEquals(COMPONENTNAME, myNotifier.getAvailableComponents().get(COMPONENTNAME).getRegistration().getName());
	}

	/**
	 * set status of component to UP and test if it is considered as usable
	 */
	@Test
	void testAddComponentAndCheckAvailabilityAndUp() {
		this.testAddComponentAndCheckAvailability();

		Instance myMockedInstance = myNotifier.getAvailableComponents().getOrDefault(COMPONENTNAME, null);
		assertNotEquals(null, myMockedInstance);

		// test for UP component
		extracted(myMockedInstance, true, 1);
		// test for DOWN component
		extracted(myMockedInstance, false, 0);
	}

	private void extracted(Instance myMockedInstance, boolean up, int expectedNumberOfUsableComponents) {
		Mockito.when(myMockedInstance.getStatusInfo().isUp()).thenReturn(up);

		List<String> requestedComponentNames = new LinkedList<>();
		requestedComponentNames.add(COMPONENTNAME);
		List<QanaryComponent> availableComponents = myNotifier.getAvailableComponentsFromNames(requestedComponentNames);
		assertEquals(expectedNumberOfUsableComponents, availableComponents.size());

		// check if component status is correctly evaluated
		assertEquals(up, myNotifier.isComponentUsable(myMockedInstance));
	}
}
