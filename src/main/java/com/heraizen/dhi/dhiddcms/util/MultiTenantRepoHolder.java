package com.heraizen.dhi.dhiddcms.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.JcrUtils;

@Configuration
@ConfigurationProperties(prefix = "dhiddcms")
@PropertySource(value = "classpath:tenantrepo.yaml", factory = YamlPropertySourceFactory.class)
@Slf4j
public class MultiTenantRepoHolder {

    private List<TenantRepoDetails> tenantRepoDetails;

    private final Map<String, Repository> tenantRepos = new ConcurrentHashMap<>();

    public List<TenantRepoDetails> getTenantRepoDetails() {
        return tenantRepoDetails;
    }

    public void setTenantRepoDetails(List<TenantRepoDetails> tenantRepoDetails) {
        this.tenantRepoDetails = tenantRepoDetails;
    }

    @PostConstruct
    public void init() {
        getTenantRepoDetails().stream().forEach(t -> {
            tenantRepos.computeIfAbsent(t.getTenantId(), l -> {
                try {
                    Repository r = JcrUtils.getRepository(l);
                    log.info("Repository available for Tenant {} at {}",
                            t.getTenantId(), l);
                    return r;
                } catch (RepositoryException e) {
                    throw new RuntimeException(String.format("Could not get "
                            + "hold of Repository for Tenant %s with location %s",
                            t.getTenantId(), t.getTenantLocation()), e);
                }
            });
            log.info("Total Tenant Repositories are : {}", tenantRepos.size());
        });
    }

    public Optional<Repository> getTenantRepo(String tenantId) {
        return Optional.ofNullable(tenantRepos.get(tenantId));
    }
}
