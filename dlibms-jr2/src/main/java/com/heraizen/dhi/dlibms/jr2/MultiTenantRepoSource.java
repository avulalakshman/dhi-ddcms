package com.heraizen.dhi.dlibms.jr2;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.util.StringUtils;

import com.heraizen.ddms.svc.exceptions.DocLibRepoException;
import com.heraizen.ddms.svc.jcr.DlibmsRepo;
import com.heraizen.ddms.svc.jcr.DlibmsRepoSource;
import java.util.function.Supplier;
import lombok.Builder;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.util.Assert;

@Slf4j
public class MultiTenantRepoSource implements DlibmsRepoSource {

    private final Supplier<String> tenantResolver;
    private final Map<String, TenantRepoDetails> tenantRepoDetails = new ConcurrentHashMap<>();
    private final Map<String, DlibmsRepo> tenantRepos = new ConcurrentHashMap<>();

    @Builder
    public MultiTenantRepoSource(List<TenantRepoDetails> tenantRepoDetails, Supplier<String> tenantResolver) {
        Assert.notNull(tenantRepoDetails, 
                "Tenant Repo Details cant be NULL, while while establishing Multi Tenant repository source");
        Assert.notEmpty(tenantRepoDetails, 
                "List of TenantRepoDetails cant be empty while establishing Multi Tenant repository source");
        Assert.notNull(tenantResolver, "Tenant Resolver must be provided (cant be NULL) to resolve Tenant");
        tenantRepoDetails.forEach(td -> this.tenantRepoDetails.put(td.getTenantId(), td));
        this.tenantResolver = tenantResolver;
    }

    public Optional<DlibmsRepo> findTenantRepo(String tenantId) {
        Assert.hasText(tenantId, "Tenant ID can not be NULL (while finding Tenant)");
        Credentials cred = new SimpleCredentials("admin", "admin".toCharArray());
        DlibmsCndNodeTypeImporter ntImporter = new DlibmsCndNodeTypeImporter();
        TenantRepoDetails tenantDetails = tenantRepoDetails.get(tenantId);
        if (tenantDetails != null) {
            DlibmsRepo tenantRepo = tenantRepos.computeIfAbsent(tenantId, tid -> {
                try {
                    log.debug(" Trying to initiate repository "
                            + "for Tenant id {} at location {}",
                            tid, tenantDetails.getTenantRepoLocation());
                    Repository r = JcrUtils.getRepository(tenantDetails.getTenantRepoLocation());
                    log.info("Repository available for Tenant {} at {}",
                            tenantDetails.getTenantId(), tenantDetails.getTenantRepoLocation());
                    return DlibmsRepo.repoBuilder()
                            .repo(r)
                            .credentials(cred)
                            .dlibmsNtRegistrationMaker(ntImporter)
                            .build();
                } catch (RepositoryException e) {
                    String errMessage = String.format("Could not start Repository "
                            + "for Tenant %s at Tenant Location %s ",
                            tenantDetails.getTenantId(), tenantDetails.getTenantRepoLocation());
                    log.error(errMessage + " Cause : " + e.getMessage(), e);
                    throw new DocLibRepoException(errMessage, e);
                }
            });
            return Optional.of(tenantRepo);
        }
        return Optional.empty();
    }

    public DlibmsRepo getTenantRepo(String tenantId) {
        Optional<DlibmsRepo> tenantRepo = StringUtils.hasText(tenantId)?findTenantRepo(tenantId)
                : Optional.empty();
        return tenantRepo.orElseThrow(()-> {
            log.error("Doc Lib Repository for Tenant {} Not Found Setup (Not Setup)", tenantId);
            return new DocLibRepoException("Document Library Repository "
                    + "is NOT setup...hence cant do any operations, contact administrator...");
        });
    }

    @Override
    public DlibmsRepo getRepo() {
        return getCurrentTenantRepo();
    }

    private DlibmsRepo getCurrentTenantRepo() {
        return getTenantRepo(tenantResolver.get());
    }

    @PreDestroy
    public void close() {
        tenantRepos.forEach((tn, tjw) -> tjw.close());
    }
}
