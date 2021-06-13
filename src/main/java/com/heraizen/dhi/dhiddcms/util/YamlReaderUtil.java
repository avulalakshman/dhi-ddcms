package com.heraizen.dhi.dhiddcms.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class YamlReaderUtil {

	private List<TenantRepoDetails> tenantDetails = new ArrayList<TenantRepoDetails>();

	@PostConstruct
	public void loadTenantRepoDetails() {
		Yaml yaml = new Yaml(new Constructor(TenantRepoDetails.class));
		try (InputStream in = YamlReaderUtil.class.getResourceAsStream("/tenantrepo.yaml")) {
			Iterable<Object> obj = yaml.loadAll(in);
			for (Object o : obj) {
				TenantRepoDetails trd = (TenantRepoDetails) o;
				tenantDetails.add(trd);
			}
		} catch (Exception e) {
			log.error("While loading tenant repo yaml {}", e);
		}
	}

	public List<TenantRepoDetails> getTenantDetails() {
		return tenantDetails;
	}

}
