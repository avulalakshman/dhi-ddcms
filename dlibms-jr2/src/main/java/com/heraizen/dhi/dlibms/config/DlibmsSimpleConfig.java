/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.config;

import com.heraizen.ddms.svc.jcr.DlibmsRepo;
import com.heraizen.ddms.svc.jcr.DlibmsRepoSource;
import com.heraizen.dhi.dlibms.jr2.DlibmsCndNodeTypeImporter;
import javax.jcr.SimpleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.commons.JcrUtils;
import org.springframework.context.annotation.Profile;

/**
 *
 * @author Pradeepkm
 */
@Configuration
@Profile("!multi-tenant")
public class DlibmsSimpleConfig {

    @Value(value = "${dlib.location}")
    String repoLocation;

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

}
