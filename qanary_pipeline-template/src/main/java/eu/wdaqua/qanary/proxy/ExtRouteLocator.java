package eu.wdaqua.qanary.proxy;

import de.codecentric.boot.admin.model.Application;
import de.codecentric.boot.admin.registry.ApplicationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by didier on 03.04.16.
 */
public class ExtRouteLocator implements RouteLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtRouteLocator.class);

    public static final String DEFAULT_ROUTE = "/**";

    private AtomicReference<Map<String, ZuulProperties.ZuulRoute>> routes = new AtomicReference<>();
    private ApplicationRegistry registry;
    private String prefix;
    private PathMatcher pathMatcher = new AntPathMatcher();
    private String servletPath;
    private String[] proxyEndpoints = {"/env", "/metrics", "/trace", "/dump", "/jolokia", "/info",
            "/configprops", "/trace", "/activiti", "/logfile", "/refresh"};
    private DiscoveryClient discovery;
    private ZuulProperties properties;
    private Map<String, ZuulProperties.ZuulRoute> staticRoutes = new LinkedHashMap<>();


    public ExtRouteLocator(String servletPath, ApplicationRegistry registry,
                           String prefix, DiscoveryClient discovery,
                           ZuulProperties properties) {
        this.servletPath = servletPath;
        this.registry = registry;
        this.prefix = prefix;
        if (StringUtils.hasText(servletPath)) { // a servletPath is passed explicitly
            this.servletPath = servletPath;
        } else {
            //set Zuul servlet path
            this.servletPath = properties.getServletPath() != null ? properties.getServletPath() : "";
        }

        this.discovery = discovery;
        this.properties = properties;
    }

    private LinkedHashMap<String, ZuulProperties.ZuulRoute> locateRoutes() {
        LinkedHashMap<String, ZuulProperties.ZuulRoute> locateRoutes = new LinkedHashMap<String, ZuulProperties.ZuulRoute>();
        for (Application application : registry.getApplications()) {
            String appPath = prefix + "/" + application.getId();
            addRoute(locateRoutes, appPath + "/health/**", application.getHealthUrl());
            if (!StringUtils.isEmpty(application.getManagementUrl())) {
                for (String endpoint : proxyEndpoints) {
                    addRoute(locateRoutes, appPath + endpoint + "/**",
                            application.getManagementUrl() + endpoint);
                }
            }
        }
        return locateRoutes;
    }

    private void addRoute(LinkedHashMap<String, ZuulProperties.ZuulRoute> locateRoutes, String path, String url) {
        locateRoutes.put(path, new ZuulProperties.ZuulRoute(path, url));
    }

    public ProxyRouteSpec getMatchingRoute(String path) {
        LOGGER.debug("Finding route for path: {}", path);
        LOGGER.debug("servletPath={}", this.servletPath);
        if (StringUtils.hasText(this.servletPath) && !this.servletPath.equals("/")
                && path.startsWith(this.servletPath)) {
            path = path.substring(this.servletPath.length());
        }
        for (Map.Entry<String, ZuulProperties.ZuulRoute> entry : this.routes.get().entrySet()) {
            String pattern = entry.getKey();
            LOGGER.debug("Matching pattern: {}", pattern);
            if (this.pathMatcher.match(pattern, path)) {
                ZuulProperties.ZuulRoute route = entry.getValue();
                int index = route.getPath().indexOf("*") - 1;
                String routePrefix = route.getPath().substring(0, index);
                String targetPath = path.substring(index, path.length());
                return new ProxyRouteSpec(route.getId(), targetPath, route.getLocation(),
                        routePrefix);
            }
        }
        return null;
    }

    @Override
    public Collection<String> getRoutePaths() {
        return getRoutes().keySet();
    }

    public void resetRoutes() {
        this.routes.set(locateRoutes());
    }

    public Map<String, String> getRoutes() {
        if (this.routes.get() == null) {
            this.routes.set(locateRoutes());
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : this.routes.get().keySet()) {
            String url = key;
            values.put(url, this.routes.get().get(key).getLocation());
        }
        return values;
    }

    public Collection<String> getIgnoredPaths() {
        return Collections.emptyList();
    }

    public void setProxyEndpoints(String[] proxyEndpoints) {
        for (String endpoint : proxyEndpoints) {
            Assert.hasText(endpoint, "The proxyEndpoints must not contain null");
            Assert.isTrue(endpoint.startsWith("/"), "All proxyEndpoints must start with '/'");
        }
        this.proxyEndpoints = proxyEndpoints.clone();
    }

    public static class ProxyRouteSpec {
        private final String id;
        private final String path;
        private final String location;
        private final String prefix;

        public ProxyRouteSpec(String id, String path, String location, String prefix) {
            this.id = id;
            this.path = path;
            this.location = location;
            this.prefix = prefix;
        }

        public String getId() {
            return id;
        }

        public String getPath() {
            return path;
        }

        public String getLocation() {
            return location;
        }

        public String getPrefix() {
            return prefix;
        }
    }


    public void addRoute(String path, String location) {
        this.staticRoutes.put(path, new ZuulProperties.ZuulRoute(path, location));
        resetRoutes();
    }

    public void addRoute(ZuulProperties.ZuulRoute route) {
        this.staticRoutes.put(route.getPath(), route);
        resetRoutes();
    }

    @Override
    public Collection<String> getRoutePaths() {
        return getRoutes().keySet();
    }

    public Map<String, String> getRoutes() {
        if (this.routes.get() == null) {
            this.routes.set(locateRoutes());
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : this.routes.get().keySet()) {
            String url = key;
            values.put(url, this.routes.get().get(key).getLocation());
        }
        return values;
    }

    public ProxyRouteSpec getMatchingRoute(String path) {
        log.info("Finding route for path: " + path);

        String location = null;
        String targetPath = null;
        String id = null;
        String prefix = this.properties.getPrefix();
        log.debug("servletPath=" + this.servletPath);
        if (StringUtils.hasText(this.servletPath) && !this.servletPath.equals("/")
                && path.startsWith(this.servletPath)) {
            path = path.substring(this.servletPath.length());
        }
        log.debug("path=" + path);
        Boolean retryable = this.properties.getRetryable();
        for (Map.Entry<String, ZuulProperties.ZuulRoute> entry : this.routes.get().entrySet()) {
            String pattern = entry.getKey();
            log.debug("Matching pattern:" + pattern);
            if (this.pathMatcher.match(pattern, path)) {
                ZuulProperties.ZuulRoute route = entry.getValue();
                id = route.getId();
                location = route.getLocation();
                targetPath = path;
                if (path.startsWith(prefix) && this.properties.isStripPrefix()) {
                    targetPath = path.substring(prefix.length());
                }
                if (route.isStripPrefix()) {
                    int index = route.getPath().indexOf("*") - 1;
                    if (index > 0) {
                        String routePrefix = route.getPath().substring(0, index);
                        targetPath = targetPath.replaceFirst(routePrefix, "");
                        prefix = prefix + routePrefix;
                    }
                }
                if (route.getRetryable() != null) {
                    retryable = route.getRetryable();
                }
                break;
            }
        }
        return (location == null ? null : new ProxyRouteSpec(id, targetPath, location,
                prefix, retryable));
    }

    public void resetRoutes() {
        this.routes.set(locateRoutes());
    }

    protected LinkedHashMap<String, ZuulProperties.ZuulRoute> locateRoutes() {
        LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<String, ZuulProperties.ZuulRoute>();
        addConfiguredRoutes(routesMap);
        routesMap.putAll(this.staticRoutes);
        if (this.discovery != null) {
            Map<String, ZuulProperties.ZuulRoute> staticServices = new LinkedHashMap<String, ZuulProperties.ZuulRoute>();
            for (ZuulProperties.ZuulRoute route : routesMap.values()) {
                String serviceId = route.getServiceId();
                if (serviceId == null) {
                    serviceId = route.getId();
                }
                if (serviceId != null) {
                    staticServices.put(serviceId, route);
                }
            }
            // Add routes for discovery services by default
            List<String> services = this.discovery.getServices();
            String[] ignored = this.properties.getIgnoredServices()
                    .toArray(new String[0]);
            for (String serviceId : services) {
                // Ignore specifically ignored services and those that were manually
                // configured
                String key = "/" + serviceId + "/**";
                if (staticServices.containsKey(serviceId)
                        && staticServices.get(serviceId).getUrl() == null) {
                    // Explicitly configured with no URL, cannot be ignored
                    // all static routes are already in routesMap
                    // Update location using serviceId if location is null
                    ZuulProperties.ZuulRoute staticRoute = staticServices.get(serviceId);
                    if (!StringUtils.hasText(staticRoute.getLocation())) {
                        staticRoute.setLocation(serviceId);
                    }
                }
                if (!PatternMatchUtils.simpleMatch(ignored, serviceId)
                        && !routesMap.containsKey(key)) {
                    // Not ignored
                    routesMap.put(key, new ZuulProperties.ZuulRoute(key, serviceId));
                }
            }
        }
        if (routesMap.get(DEFAULT_ROUTE) != null) {
            ZuulProperties.ZuulRoute defaultRoute = routesMap.get(DEFAULT_ROUTE);
            // Move the defaultServiceId to the end
            routesMap.remove(DEFAULT_ROUTE);
            routesMap.put(DEFAULT_ROUTE, defaultRoute);
        }
        LinkedHashMap<String, ZuulProperties.ZuulRoute> values = new LinkedHashMap<>();
        for (Map.Entry<String, ZuulProperties.ZuulRoute> entry : routesMap.entrySet()) {
            String path = entry.getKey();
            // Prepend with slash if not already present.
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (StringUtils.hasText(this.properties.getPrefix())) {
                path = this.properties.getPrefix() + path;
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
            }
            values.put(path, entry.getValue());
        }
        return values;
    }

    protected void addConfiguredRoutes(Map<String, ZuulProperties.ZuulRoute> routes) {
        Map<String, ZuulProperties.ZuulRoute> routeEntries = this.properties.getRoutes();
        for (ZuulProperties.ZuulRoute entry : routeEntries.values()) {
            String route = entry.getPath();
            if (routes.containsKey(route)) {
                log.warn("Overwriting route " + route + ": already defined by "
                        + routes.get(route));
            }
            routes.put(route, entry);
        }
    }

    public String getTargetPath(String matchingRoute, String requestURI) {
        String path = getRoutes().get(matchingRoute);
        return (path != null ? path : requestURI);

    }

    @Data
    @AllArgsConstructor
    public static class ProxyRouteSpec {

        private String id;

        private String path;

        private String location;

        private String prefix;

        private Boolean retryable;

    }
}
