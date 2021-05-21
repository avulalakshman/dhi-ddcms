package com.heraizen.dhi.dhiddcms.util;

import com.heraizen.dhi.dhiddcms.service.JcrWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "dhiddcms")
@PropertySource(value = "classpath:tenantrepo.yaml", factory = YamlPropertySourceFactory.class)
@Slf4j
public class MultiTenantRepoHolder {

    private List<TenantRepoDetails> tenantRepoDetails;

    private final Map<String, JcrWrapper> tenantRepos = new ConcurrentHashMap<>();
    
    public List<TenantRepoDetails> getTenantRepoDetails() {
        return tenantRepoDetails;
    }

    public void setTenantRepoDetails(List<TenantRepoDetails> tenantRepoDetails) {
        this.tenantRepoDetails = tenantRepoDetails;
    }

    @PostConstruct
    public void init() {
        Credentials cred = new SimpleCredentials("admin", "admin".toCharArray());
        getTenantRepoDetails().stream().forEach(t -> {
            tenantRepos.computeIfAbsent(t.getTenantId(), tid -> {
                try {
                    log.debug(" Trying to initiate repository for "
                            + "Tenant id {} at location {}", 
                            tid, t.getTenantLocation());
                    Repository r = JcrUtils.getRepository(t.getTenantLocation());
                    log.info("Repository available for Tenant {} at {}",
                            t.getTenantId(), t.getTenantLocation());
                    return new JcrWrapper(r, cred);
                } catch (RepositoryException e) {
                    throw new RuntimeException(String.format("Could not get "
                            + "hold of Repository for Tenant %s with location %s",
                            t.getTenantId(), t.getTenantLocation()), e);
                }
            });
            log.info("Total Tenant Repositories are : {}", tenantRepos.size());
        });
    }

    public Optional<JcrWrapper> getTenantRepo(String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            return Optional.ofNullable(tenantRepos.get(tenantId));
        } else {
            return Optional.empty();
        }
    }
    
    public JcrWrapper getCurrentTenantRepo() {
        String currentTenant = TenantContext.getCurrentTenant();
        return getTenantRepo(currentTenant)
                .orElseThrow(()->new RuntimeException(String.format("JCR for Tenant id %s not found",
                        currentTenant)));
    }
}
