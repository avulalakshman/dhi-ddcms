/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.jr2;

import com.heraizen.ddms.svc.exceptions.DocLibRepoException;
import com.heraizen.ddms.svc.jcr.JcrInitializer;
import com.heraizen.ddms.svc.jcr.JcrWrapper;
import com.heraizen.ddms.svc.jcr.RepoHolder;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.commons.JcrUtils;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class DlibmsRepoHolder implements RepoHolder {

    private final String repoLocation;
    private final JcrWrapper repoWrapper;

    @Builder
    public DlibmsRepoHolder(String repoLocation,
            JcrInitializer dlibInitializer,
            Credentials credentials) {
        try {
            this.repoLocation = repoLocation;
            Repository repo = JcrUtils.getRepository(repoLocation);
            dlibInitializer.initializeRepo(repo, credentials);
            this.repoWrapper = new JcrWrapper(repo, credentials);
        } catch (RepositoryException ex) {
            log.error("Jcr Repository creation error for {} {}", 
                    repoLocation, ex.getMessage());
            log.debug("Stack trace:", ex);
            throw new DocLibRepoException("Error creating/opening repo @ "+ repoLocation, ex);
        }
    }

    @Override
    public JcrWrapper getRepo() {
        return repoWrapper;
    }

    @Override
    public String toString() {
        return "Repository holder for repo at " + repoLocation;
    }
    
}
