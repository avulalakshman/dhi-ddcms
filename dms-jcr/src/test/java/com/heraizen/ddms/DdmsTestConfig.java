/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms;

import com.heraizen.ddms.svc.DigitalLibMgmtService;
import com.heraizen.ddms.svc.jcr.DlibmsSimpleRepoSource;
import com.heraizen.ddms.svc.jcr.DigitalLibMgmtServiceJcrImpl;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import lombok.extern.slf4j.Slf4j;
import com.heraizen.ddms.svc.jcr.DlibmsRepoSource;

/**
 *
 * @author Pradeepkm
 */
@Configuration
@Slf4j
public class DdmsTestConfig {

    Resource repoXml = new ClassPathResource("test-repository.xml");
    String repoHomeFileName = "file:///temp/dsjcr/test";
    Credentials testRepoCredentials = new SimpleCredentials("admin1", "admin1".toCharArray());
    
    @Bean
    TransientRepository getRepo() throws URISyntaxException, IOException {
        Path repoHome = Paths.get(new URI(repoHomeFileName));
        log.info("\n\n\n Creating a Repository @ {} \n\n\n", repoHome);
        return new TransientRepository(repoXml.getFile(), repoHome.toFile());
    }

    @Bean
    DlibmsRepoSource getDlibmsRepoSource(TransientRepository repo) throws IOException, URISyntaxException {
        return DlibmsSimpleRepoSource.repoSourceBuilder()
                .repo(repo)
                .repoCredentials(testRepoCredentials)
                .dlibmsNtRegistrationMaker(new DlibmsCndNodeTypeImporter())
                .build();
    }

    @Bean
    DigitalLibMgmtService getDigitalLibMgmtService(DlibmsRepoSource repoHolder) {
        return DigitalLibMgmtServiceJcrImpl.builder()
                .dlibmsRepoHolder(repoHolder)
                .build();
    }
}
