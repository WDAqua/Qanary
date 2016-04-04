package eu.wdaqua.qanary.proxy;

import de.codecentric.boot.admin.config.AdminServerImportSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Created by didier cherix on 03.04.16.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AdminServerWithProxyImportSelector.class)
public @interface EnableAdminServerWithProxy {
}
