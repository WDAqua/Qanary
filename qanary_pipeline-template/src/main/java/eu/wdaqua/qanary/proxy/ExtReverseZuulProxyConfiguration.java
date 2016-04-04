package eu.wdaqua.qanary.proxy;

import de.codecentric.boot.admin.config.AdminServerProperties;
import de.codecentric.boot.admin.config.AdminServerWebConfiguration;
import de.codecentric.boot.admin.config.RevereseZuulProxyConfiguration;
import de.codecentric.boot.admin.event.RoutesOutdatedEvent;
import de.codecentric.boot.admin.registry.ApplicationRegistry;
import de.codecentric.boot.admin.zuul.ApplicationRouteLocator;
import de.codecentric.boot.admin.zuul.PreDecorationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatMonitor;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.event.ParentHeartbeatEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.RoutesEndpoint;
import org.springframework.cloud.netflix.zuul.RoutesRefreshedEvent;
import org.springframework.cloud.netflix.zuul.ZuulConfiguration;
import org.springframework.cloud.netflix.zuul.ZuulProxyConfiguration;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by didier cherix on 03.04.16.
 */
@Configuration
@AutoConfigureAfter({AdminServerWebConfiguration.class})
public class ExtReverseZuulProxyConfiguration extends ZuulConfiguration {

    @Autowired(required = false)
    private TraceRepository traces;

    @Autowired
    private SpringClientFactory clientFactory;

    @Autowired
    private DiscoveryClient discovery;

    @Autowired
    private ZuulProperties zuulProperties;

    @Autowired
    private ServerProperties server;

    @Autowired(required = false)
    private TraceRepository traces;

    @Autowired
    private ApplicationRegistry registry;

    @Autowired
    private AdminServerProperties adminServer;

    @Bean
    @Override
    public RouteLocator routeLocator() {
        return new ProxyRouteLocator(this.server.getServletPrefix(), this.discovery,
                this.zuulProperties);
    }


    @Bean
    public ApplicationRouteLocator applicationRouteLocator() {
        return new ApplicationRouteLocator(this.server.getServletPrefix(), registry,
                adminServer.getContextPath() + "/api/applications");
    }

    // pre filters
    @Bean
    public org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter preDecorationFilter() {
        return new PreDecorationFilter(routeLocator(),
                this.zuulProperties.isAddProxyHeaders());
    }

    // route filters
    @Bean
    public RibbonRoutingFilter ribbonRoutingFilter() {
        ProxyRequestHelper helper = new ProxyRequestHelper();
        if (this.traces != null) {
            helper.setTraces(this.traces);
        }
        RibbonRoutingFilter filter = new RibbonRoutingFilter(helper, this.clientFactory);
        return filter;
    }

    @Bean
    public SimpleHostRoutingFilter simpleHostRoutingFilter() {
        ProxyRequestHelper helper = new ProxyRequestHelper();
        if (this.traces != null) {
            helper.setTraces(this.traces);
        }
        return new SimpleHostRoutingFilter(helper);
    }

    @Bean
    @Override
    public ApplicationListener<ApplicationEvent> zuulRefreshRoutesListener() {
        return new ZuulRefreshListener();
    }

    private static class ZuulRefreshListener implements ApplicationListener<ApplicationEvent> {

        private HeartbeatMonitor monitor = new HeartbeatMonitor();

        @Autowired
        private ProxyRouteLocator routeLocator;

        @Autowired
        private ZuulHandlerMapping zuulHandlerMapping;

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof PayloadApplicationEvent && ((PayloadApplicationEvent<?>) event)
                    .getPayload() instanceof RoutesOutdatedEvent) {
                routeLocator.resetRoutes();
                zuulHandlerMapping.registerHandlers();
            } else if (event instanceof InstanceRegisteredEvent
                    || event instanceof RefreshScopeRefreshedEvent
                    || event instanceof RoutesRefreshedEvent) {
                reset();
            } else if (event instanceof ParentHeartbeatEvent) {
                ParentHeartbeatEvent e = (ParentHeartbeatEvent) event;
                resetIfNeeded(e.getValue());
            } else if (event instanceof HeartbeatEvent) {
                HeartbeatEvent e = (HeartbeatEvent) event;
                resetIfNeeded(e.getValue());
            }
        }

        @Configuration
        @ConditionalOnClass(Endpoint.class)
        protected static class RoutesEndpointConfiguration {

            @Autowired
            private ProxyRouteLocator routeLocator;

            @Bean
            // @RefreshScope
            public RoutesEndpoint zuulEndpoint() {
                return new RoutesEndpoint(this.routeLocator);
            }

        }


        private void resetIfNeeded(Object value) {
            if (this.monitor.update(value)) {
                reset();
            }
        }

        private void reset() {
            this.routeLocator.resetRoutes();
            this.zuulHandlerMapping.registerHandlers();
        }
    }


    @Bean
    public PreDecorationFilter preDecorationFilter() {
        return new PreDecorationFilter(routeLocator(), true);
    }


}
