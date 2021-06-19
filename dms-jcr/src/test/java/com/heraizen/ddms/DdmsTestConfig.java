/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms;

import com.heraizen.ddms.svc.DigitalLibMgmtService;
import com.heraizen.ddms.svc.jcr.DigitalLibMgmtServiceJcrImpl;
import com.heraizen.ddms.svc.jcr.JcrWrapper;
import com.heraizen.ddms.svc.jcr.RepoHolder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.core.TransientRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import lombok.extern.slf4j.Slf4j;
/**
 *
 * @author Pradeepkm
 */
@Configuration
@Slf4j
public class DdmsTestConfig {

    @Bean
    RepoHolder getRepoHolder() throws IOException, URISyntaxException {  
        Resource configFile = new ClassPathResource("test-repository.xml");
        Path repoHome = Paths.get(new URI("file:///temp/dsjcr/test"));
        log.info("\n\n\n Creating a Repository @ {} \n\n\n", repoHome);
        TransientRepository repo = new TransientRepository(configFile.getFile(), repoHome.toFile());
        JcrWrapper repoWrapper = new JcrWrapper(repo, 
                new SimpleCredentials("admin1", "admin1".toCharArray()));
        return new TestRepoHolder(repoWrapper);
    }

    @Bean
    DigitalLibMgmtService getDigitalLibMgmtService(RepoHolder repoHolder) {
        return new DigitalLibMgmtServiceJcrImpl(repoHolder);
    }
    
}
