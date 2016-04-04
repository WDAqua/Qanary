package eu.wdaqua.qanary.proxy;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.constants.ZuulHeaders;
import com.netflix.zuul.context.RequestContext;
import de.codecentric.boot.admin.zuul.ApplicationRouteLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by didier on 03.04.16.
 */
public class PreDecorationFilter extends ZuulFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreDecorationFilter.class);
    private ExtRouteLocator routeLocator;
    private boolean addProxyHeaders;

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    public PreDecorationFilter(ExtRouteLocator routeLocator, boolean addProxyHeaders) {
        this.routeLocator = routeLocator;
        this.addProxyHeaders = addProxyHeaders;
    }

    @Override
    public int filterOrder() {
        return 5;
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        final String requestURI = this.urlPathHelper.getPathWithinApplication(ctx.getRequest());
        ApplicationRouteLocator.ProxyRouteSpec route = this.routeLocator.getMatchingRoute(requestURI);
        if (route != null) {
            String location = route.getLocation();
            if (location != null) {

                ctx.put("requestURI", route.getPath());
                ctx.put("proxy", route.getId());

                if (route.getRetryable() != null) {
                    ctx.put("retryable", route.getRetryable());
                }

                if (location.startsWith("http:") || location.startsWith("https:")) {
                    ctx.setRouteHost(getUrl(location));
                    ctx.addOriginResponseHeader("X-Zuul-Service", location);
                } else {
                    // set serviceId for use in filters.route.RibbonRequest
                    ctx.set("serviceId", location);
                    ctx.setRouteHost(null);
                    ctx.addOriginResponseHeader("X-Zuul-ServiceId", location);
                }
                if (this.addProxyHeaders) {
                    ctx.addZuulRequestHeader(
                            "X-Forwarded-Host",
                            ctx.getRequest().getServerName() + ":"
                                    + String.valueOf(ctx.getRequest().getServerPort()));
                    if (StringUtils.hasText(route.getPrefix())) {
                        ctx.addZuulRequestHeader("X-Forwarded-Prefix", route.getPrefix());
                    }
                }
            } else {
                ctx.put("requestURI", route.getPath());
                ctx.put("proxy", route.getId());
                ctx.setRouteHost(getUrl(route.getLocation()));
                ctx.addOriginResponseHeader("X-Zuul-Service", route.getLocation());
                if (this.addProxyHeaders) {
                    ctx.addZuulRequestHeader("X-Forwarded-Host", ctx.getRequest().getServerName() + ":"
                            + String.valueOf(ctx.getRequest().getServerPort()));
                    ctx.addZuulRequestHeader(ZuulHeaders.X_FORWARDED_PROTO,
                            ctx.getRequest().getScheme());
                    if (StringUtils.hasText(route.getPrefix())) {
                        ctx.addZuulRequestHeader("X-Forwarded-Prefix", route.getPrefix());
                    }
                }
            }
        } else {
            LOGGER.warn("No route found for uri: " + requestURI);
            return null;
        }
    }

    private URL getUrl(String target) {
        try {
            return new URL(target);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Target URL is malformed", ex);
        }
    }


}
