package com.supplygraph.web;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * SupplyGraph Platform Main Entrypoint and Composition Root.
 */
@SpringBootApplication(scanBasePackages = "com.supplygraph")
@EntityScan(basePackages = "com.supplygraph.adapter.persistence.entity")
@EnableJpaRepositories(basePackages = "com.supplygraph.adapter.persistence.repository")
@Theme("supplygraph")
public class SupplyGraphApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(SupplyGraphApplication.class, args);
    }
}

