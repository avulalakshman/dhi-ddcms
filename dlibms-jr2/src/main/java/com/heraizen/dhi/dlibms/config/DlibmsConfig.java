/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.config;

import com.heraizen.ddms.svc.DigitalLibMgmtService;
import com.heraizen.ddms.svc.jcr.DigitalLibMgmtServiceJcrImpl;
import com.heraizen.ddms.svc.jcr.DlibmsRepo;
import com.heraizen.ddms.svc.jcr.DlibmsRepoSource;
import com.heraizen.dhi.dlibms.jr2.DlibmsCndNodeTypeImporter;
import com.heraizen.dhi.dlibms.jr2.MultiTenantRepoSource;
import com.heraizen.dhi.dlibms.jr2.TenantContext;
import com.heraizen.dhi.dlibms.jr2.TenantRepoDetails;
import com.heraizen.dhi.dlibms.jr2.YamlReaderUtil;
import java.util.List;
import javax.jcr.SimpleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.commons.JcrUtils;

/**
 *
 * @author Pradeepkm
 */
@Configuration
public class DlibmsConfig {

    @Value(value = "${dlib.location}")
    String repoLocation;

    @Value(value = "${dlib.multitenancy.config}")
    String mtConfigFile;

    Credentials repoCredentials = new SimpleCredentials("admin",
            "admin".toCharArray());

    //NOTE: DlibmsRepo is also a DlibmsRepoSource...
    @Bean
    public DlibmsRepoSource getRepoSource() throws RepositoryException {
        Repository repo = JcrUtils.getRepository(repoLocation);
        return DlibmsRepo.repoBuilder()
                .repo(repo).credentials(repoCredentials)
                .dlibmsNtRegistrationMaker(new DlibmsCndNodeTypeImporter())
                .build();
    }

    public DlibmsRepoSource getMultiTenantRepoSource() {
        List<TenantRepoDetails> repoConfig = YamlReaderUtil.loadTenantRepoDetailsFromCp(mtConfigFile);
        return MultiTenantRepoSource.builder()
                .tenantRepoDetails(repoConfig)
                .tenantResolver(() -> TenantContext.getCurrentTenant())
                .build();
    }

    @Bean
    DigitalLibMgmtService getDigitalLibMgmtService(DlibmsRepoSource repoSource) {
        return DigitalLibMgmtServiceJcrImpl.builder()
                .dlibmsRepoSource(repoSource)
                .build();
    }
    
}
