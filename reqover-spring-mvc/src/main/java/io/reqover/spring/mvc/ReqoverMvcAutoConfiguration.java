package io.reqover.spring.mvc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Auto-configuration entry point for servlet applications using Spring MVC.
 *
 * <p>Set {@code reqover.mvc.enabled=false} to keep the adapter out of the
 * context entirely.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnProperty(prefix = "reqover.mvc", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(ReqoverMvcConfiguration.class)
public class ReqoverMvcAutoConfiguration {
}
