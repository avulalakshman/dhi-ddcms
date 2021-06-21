package com.heraizen.dhi.dlibms.jr2;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.util.StringUtils;

import com.heraizen.ddms.svc.exceptions.DocLibRepoException;
import com.heraizen.ddms.svc.jcr.DlibmsRepo;
import com.heraizen.ddms.svc.jcr.JcrWrapper;
import java.util.function.Supplier;
import lombok.Builder;

import lombok.extern.slf4j.Slf4j;
import com.heraizen.ddms.svc.jcr.JcrSource;

@Slf4j
public class MultiTenantRepoSource implements JcrSource {

    private final YamlReaderUtil yamlReaderUtil;
    private final Supplier<String> tenantResolver;
    
    private final Map<String, JcrWrapper> tenantRepos = new ConcurrentHashMap<>();

    @Builder
    public MultiTenantRepoSource(YamlReaderUtil util, Supplier<String> tenantResolver) {
        this.yamlReaderUtil = util;
        this.tenantResolver = tenantResolver;
    }
    
    @PostConstruct
    public void init() {
        Credentials cred = new SimpleCredentials("admin", "admin".toCharArray());
        DlibmsCndNodeTypeImporter ntImporter = new DlibmsCndNodeTypeImporter();
        yamlReaderUtil.getTenantRepoDetails().stream().forEach(t -> {

            tenantRepos.computeIfAbsent(t.getTenantId(), tid -> {
                try {
                    log.debug(" Trying to initiate repository for " + "Tenant id {} at location {}", tid,
                            t.getTenantLocation());
                    Repository r = JcrUtils.getRepository(t.getTenantLocation());
                    log.info("Repository available for Tenant {} at {}", t.getTenantId(), t.getTenantLocation());
                    log.info("Trying to initialize repository for digilib...");
                    return DlibmsRepo.repoBuilder()
                            .repo(r)
                            .credentials(cred)
                            .dlibmsNtRegistrationMaker(ntImporter)
                            .build();
                } catch (RepositoryException e) {
                    String errMessage = String.format(
                            "Could not start Repository " + "for Tenant %s at Tenant Location %s ", t.getTenantId(),
                            t.getTenantLocation());
                    log.error(errMessage + " Cause : " + e.getMessage(), e);
                    throw new DocLibRepoException(errMessage, e);
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

    @Override
    public JcrWrapper getRepo() {
        return getCurrentTenantRepo();
    }

    private JcrWrapper getCurrentTenantRepo() {
        String currentTenant = tenantResolver.get();
        return getTenantRepo(currentTenant).orElseThrow(() -> {
            log.error("Doc Lib Repository for Tenant {} Not Setup or Found", currentTenant);
            return new DocLibRepoException("Document Library Repository "
                    + "is NOT setup...hence cant do any operations, contact administrator...");
        });
    }

    @PreDestroy
    public void close() {
        tenantRepos.forEach((tn, tjw) -> tjw.close());
    }
}
