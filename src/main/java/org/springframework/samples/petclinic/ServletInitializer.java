package org.springframework.samples.petclinic;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Bootstraps the application when it is deployed as a WAR into a standalone servlet
 * container (Tomcat), instead of being launched from {@code main()} as an executable JAR.
 *
 * <p>
 * When Spring Boot runs as a fat JAR, {@link PetClinicApplication#main} starts an
 * embedded Tomcat. Deployed as a WAR there is no {@code main()} call — the container
 * discovers this class via the Servlet 3.0+ {@code ServletContainerInitializer} mechanism
 * and uses it to build the application context.
 *
 * <p>
 * Omitting this class is the classic silent failure: the WAR deploys without error but
 * every request returns 404, because nothing ever bootstraps Spring.
 *
 * @see PetClinicApplication
 */
public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(PetClinicApplication.class);
	}

}
