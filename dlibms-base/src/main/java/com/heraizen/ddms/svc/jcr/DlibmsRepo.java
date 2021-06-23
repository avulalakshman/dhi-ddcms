/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms.svc.jcr;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import lombok.Builder;

/**
 *
 * @author Pradeepkm
 */
public class DlibmsRepo extends JcrWrapper implements DlibmsRepoSource{

    @Builder(builderMethodName = "repoBuilder")
    public DlibmsRepo(Repository repo, Credentials credentials,
            DlibmsNodeTypeProvider dlibmsNtRegistrationMaker) {
        super(repo, credentials, DlibmsRepoInitializer.builder()
                .dlibmsNtRegistrationMaker(dlibmsNtRegistrationMaker)
                .build());
    }

    @Override
    public DlibmsRepo getRepo() {
        return this;
    }
}
