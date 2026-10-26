package eu.wdaqua.qanary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.domain.events.InstanceRegisteredEvent;
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import eu.wdaqua.qanary.business.QanaryComponent;

/**
 * Checks how the pipeline keeps track of registered components when Spring Boot
 * Admin reports registrations and status changes: which components become
 * callable, which stay registered but unusable, and which events are ignored.
 */
class QanaryComponentRegistrationChangeNotifierCoverageTest {

    private QanaryComponentRegistrationChangeNotifier notifier;

    static Instance instance(String name, String serviceUrl, StatusInfo status) {
        Registration.Builder registration = Registration.create(name, "http://127.0.0.1:1/actuator/health");
        if (serviceUrl != null) {
            registration.serviceUrl(serviceUrl);
        }
        return Instance.create(InstanceId.of(name + "-id")).register(registration.build()).withStatusInfo(status);
    }

    private void statusChanged(Instance instance) {
        notifier.doNotify(new InstanceStatusChangedEvent(instance.getId(), 2L, instance.getStatusInfo()), instance)
                .block();
    }

    @BeforeEach
    void setUp() {
        notifier = new QanaryComponentRegistrationChangeNotifier(mock(InstanceRepository.class));
    }

    @Test
    void componentReportedUpBecomesCallableWithItsServiceUrl() {
        statusChanged(instance("NED-DBpediaSpotlight", "http://127.0.0.1:9001/", StatusInfo.ofUp()));

        assertEquals(List.of("NED-DBpediaSpotlight"), notifier.getAvailableComponentNames());
        QanaryComponent component = notifier.getAvailableComponentFromName("NED-DBpediaSpotlight");
        assertNotNull(component);
        assertEquals("NED-DBpediaSpotlight", component.getName());
        assertEquals("http://127.0.0.1:9001/", component.getUrl());
        assertEquals(1, notifier.getAvailableComponentsFromNames(List.of("NED-DBpediaSpotlight")).size());
    }

    @Test
    void componentReportedUpWithoutServiceUrlIsNotRegistered() {
        statusChanged(instance("QB-NoUrl", null, StatusInfo.ofUp()));

        assertTrue(notifier.getAvailableComponentNames().isEmpty());
        assertNull(notifier.getAvailableComponentFromName("QB-NoUrl"));
    }

    @Test
    void componentReportedDownStaysRegisteredButIsNotCallable() {
        Instance down = instance("QB-Offline", "http://127.0.0.1:9002/", StatusInfo.ofDown());
        statusChanged(down);

        // registered (e.g. for the availability overview) ...
        assertEquals(List.of("QB-Offline"), notifier.getAvailableComponentNames());
        assertFalse(notifier.isComponentUsable(down));
        // ... but never handed out for a question answering run
        assertNull(notifier.getAvailableComponentFromName("QB-Offline"));
        assertTrue(notifier.getAvailableComponentsFromNames(List.of("QB-Offline")).isEmpty());
    }

    @Test
    void eventsOtherThanStatusChangesDoNotChangeTheRegistry() {
        Instance registered = instance("NED-New", "http://127.0.0.1:9003/", StatusInfo.ofUp());
        notifier.doNotify(new InstanceRegisteredEvent(registered.getId(), 1L, registered.getRegistration()), registered)
                .block();

        assertTrue(notifier.getAvailableComponents().isEmpty());
    }

    @Test
    void unknownComponentNamesAreSkippedWhenSelectingComponents() {
        statusChanged(instance("NED-Known", "http://127.0.0.1:9004/", StatusInfo.ofUp()));

        List<QanaryComponent> components = notifier
                .getAvailableComponentsFromNames(List.of("NED-Unknown", "NED-Known"));

        assertEquals(1, components.size());
        assertEquals("NED-Known", components.get(0).getName());
        assertNull(notifier.getAvailableComponentFromName("NED-Unknown"));
    }

    @Test
    void componentsAndAvailabilityMapsEachComponentToItsStatus() {
        Map<String, Instance> components = Map.of( //
                "NED-Up", instance("NED-Up", "http://127.0.0.1:9005/", StatusInfo.ofUp()), //
                "QB-Down", instance("QB-Down", "http://127.0.0.1:9006/", StatusInfo.ofDown()));

        assertEquals(Map.of("NED-Up", "UP", "QB-Down", "DOWN"), notifier.getComponentsAndAvailability(components));
    }
}
