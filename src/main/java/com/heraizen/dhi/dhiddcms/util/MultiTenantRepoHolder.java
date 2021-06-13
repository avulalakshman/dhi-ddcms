package com.heraizen.dhi.dhiddcms.util;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.heraizen.dhi.dhiddcms.exceptions.DocLibRepoException;
import com.heraizen.dhi.dhiddcms.service.JcrInitializer;
import com.heraizen.dhi.dhiddcms.service.JcrWrapper;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MultiTenantRepoHolder {

	@Autowired
	private YamlReaderUtil yamlReaderUtil;

	private final Map<String, JcrWrapper> tenantRepos = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		Credentials cred = new SimpleCredentials("admin", "admin".toCharArray());

		JcrInitializer dlibInitializer = new DigitalLibRepoInitializer();
		yamlReaderUtil.getTenantRepoDetails().stream().forEach(t -> {

			tenantRepos.computeIfAbsent(t.getTenantId(), tid -> {
				try {
					log.debug(" Trying to initiate repository for " + "Tenant id {} at location {}", tid,
							t.getTenantLocation());
					Repository r = JcrUtils.getRepository(t.getTenantLocation());
					log.info("Repository available for Tenant {} at {}", t.getTenantId(), t.getTenantLocation());
					log.info("Trying to initialize repository for digilib...");
					dlibInitializer.initializeRepo(r, cred);
					return new JcrWrapper(r, cred);
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

	public JcrWrapper getCurrentTenantRepo() {
		String currentTenant = TenantContext.getCurrentTenant();
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
