/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dlibms.jr2;

import com.heraizen.ddms.svc.jcr.DlibmsSimpleRepoSource;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.commons.JcrUtils;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class DlibmsSingleTenantRepoSource extends DlibmsSimpleRepoSource {

    private final String repoLocation;

    @Builder(builderMethodName = "tenantRepoSourceBuilder")
    public DlibmsSingleTenantRepoSource(String repoLocation, Credentials credentials) throws RepositoryException {
        super(JcrUtils.getRepository(repoLocation),
                credentials, new DlibmsCndNodeTypeImporter());
        this.repoLocation = repoLocation;

    }

    @Override
    public String toString() {
        return "Repository holder for repo at " + this.repoLocation;
    }
}
