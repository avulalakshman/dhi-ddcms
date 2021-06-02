/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.util;

import com.heraizen.dhi.dhiddcms.exceptions.DocLibRepoException;
import com.heraizen.dhi.dhiddcms.service.DlibConstants;
import com.heraizen.dhi.dhiddcms.service.JcrInitializer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class DigitalLibRepoInitializer implements JcrInitializer {

    public static final String DLIB_MD_CND_FILENAME = "dlib_metadata.cnd";

    private void checkAndRegisterNodeType(Session dlibWsSession) {
        try {
            NodeTypeManager ntManager = dlibWsSession
                    .getWorkspace().getNodeTypeManager();

            if (!ntManager.hasNodeType(DlibConstants.DLIB_NT_METADATA)) {
                log.warn("Node Type {} is not available, hence Registering it!", DlibConstants.DLIB_NT_METADATA);
                ClassPathResource resource = new ClassPathResource(DLIB_MD_CND_FILENAME);
                try (Reader reader = new InputStreamReader(resource.getInputStream())) {
                    CndImporter.registerNodeTypes(reader, dlibWsSession);
                }
            }
            log.info("Node Type {} is registered (or was already available)", DlibConstants.DLIB_NT_METADATA);
        } catch (RepositoryException | ParseException | IOException ex) {
            String errMsg = String.format("Error while registering a Node type "
                    + "for Dlib Workspace and error is: %s", ex.getMessage());
            log.error(errMsg);
            log.debug("Stack trace: ", ex);
            throw new DocLibRepoException(errMsg, ex);
        }
    }

    private Session createWorkSpace(Repository repo, Credentials credentials) {
        try {
            log.debug("Logging into Default workspace...");
            Session session = repo.login(credentials); //login to default workspace...
            session.getWorkspace().createWorkspace(DlibConstants.DLIB_WS_NAME);
            log.debug("Successfully created new workspace...");
            session.save();
            session.logout();
            log.debug("Saved session and logged out of default...will login to freshly minted workspace  {}", DlibConstants.DLIB_WS_NAME);
            Session newSession = repo.login(credentials, DlibConstants.DLIB_WS_NAME);
            log.debug("Successfully logged into new wokspace {}", DlibConstants.DLIB_WS_NAME);
            return newSession;
        } catch (RepositoryException ex) {
            String errMsg = String.format("Error while creating Dlib Workspace "
                    + "and registering a Node type for Dlib : %s error is: %s",
                    DlibConstants.DLIB_WS_NAME, ex.getMessage());
            log.error(errMsg);
            log.debug("Stack trace: ", ex);
            throw new DocLibRepoException(errMsg, ex);
        }
    }

    @Override
    public void initializeRepo(Repository repo, Credentials credentials) {
        try {
            Session wsSession;
            try {
                wsSession = repo.login(credentials, DlibConstants.DLIB_WS_NAME);
                log.debug("Workspace {} already exists...", DlibConstants.DLIB_WS_NAME);
            } catch (NoSuchWorkspaceException we) {
                log.debug("Workspace {} not found in repository {} got error {} "
                        + "hence will attempt to create one...",
                        DlibConstants.DLIB_WS_NAME, repo, we.getMessage());
                wsSession = createWorkSpace(repo, credentials);
            }
            log.debug("Checking (and registering if required) for required nodetype registration...");
            checkAndRegisterNodeType(wsSession);
            wsSession.save();
            wsSession.logout();
        } catch (RepositoryException ex) {
            String errMsg = String.format("Could not initialize Repo %s for "
                    + "Digital lib, something went wrong : %s",
                    repo.toString(), ex.getMessage());
            log.error(errMsg);
            log.debug("Stack trace:", ex);
            throw new DocLibRepoException(errMsg);
        }
    }
}
