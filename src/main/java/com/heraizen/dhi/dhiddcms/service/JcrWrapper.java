/*          
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.service;

import com.heraizen.dhi.dhiddcms.exceptions.DocLibRepoException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class JcrWrapper {

    private final Repository repo;
    private final Credentials credentials;
    private final Session keepAliveSession;

    public JcrWrapper(Repository repo, Credentials cred) {
        this.repo = repo;
        this.credentials = cred;
        this.keepAliveSession = createKeepAliveSession();
    }

    private Session createKeepAliveSession() {
        return login();
    }

    protected Session login(String workspaceName) {
        log.debug("logging into workspace {}", workspaceName);
        try {
            return repo.login(credentials, workspaceName);
        } catch (RepositoryException re) {
            String errMsg = String.format("Error while logging into workspace %s of Repository repo %s",
                    workspaceName, repo);
            log.error(errMsg + ":" + re.getMessage());
            log.debug("JCR Error Stack trace : ", re);
            throw new DocLibRepoException("Could not login to Workspace of Doc Lib Repository");
        }
    }

    protected Session login() {
        log.debug("logging into default workspace");
        try {
            return repo.login(credentials);
        } catch (RepositoryException re) {
            String errMsg = "Could not login to default workspace of repository "
                    + repo.toString();
            log.error(errMsg + " : " + re.getMessage());
            log.debug("JCR error :", re);
            throw new DocLibRepoException("Could not login to Workspace of Doc Lib Repository");
        }
    }

    protected Session getSession(Optional<String> workspaceName) {
        return workspaceName.map(wn -> login(wn))
                .orElse(login());
    }

    public void doWithWorkspace(Optional<String> workspaceName,
            Consumer<Session> sessionConsumer, boolean saveWhenDone) {
        Session session = getSession(workspaceName);
        try {
            log.debug("Session {} is being delegated...", session);
            sessionConsumer.accept(session);
            log.debug("Session {} has been consumed", session);
            if (saveWhenDone) {
                log.debug("Saving session post consumption... ");
                session.save();
            }
        } catch (RepositoryException ex) {
            log.error("Error while consuming session (mostly during session save)"
                    + "...workspace {}, session {}, error : {}", workspaceName, session, ex.getMessage());
            log.debug("Stack trace: ", ex);
            throw new DocLibRepoException(String.format("Internal repository error while saving session"));
        } finally {
            log.debug("logging out from session {}", session.toString());
            session.logout();
        }
    }

    public void doWithNode(Optional<String> workspace,
            Optional<String> nodePath,
            BiConsumer<Session, Node> sessionConsumer,
            boolean saveWhenDone) {
        Session session = getSession(workspace);
        try {
            Node node;
            //session operations throws typed exceptions, hence it is clumsier 
            //to use map operation on nodePath Optional...
            if (nodePath.isPresent()) {
                node = session.getNode(nodePath.get());
            } else {
                node = session.getRootNode();
            }
            log.debug("Session for workspace {} and  Node {} is being delegated...",
                    workspace, nodePath);
            sessionConsumer.accept(session, node);
            log.debug("Session and Node {} has been consumed", nodePath);
            if (saveWhenDone) {
                log.debug("Saving session post consumption... ");
                session.save();
            }
        } catch (RepositoryException re) {
            log.error("Error while consuming session (mostly during session save)"
                    + "...workspace {}, session {}, error : {}", workspace, session, re.getMessage());
            log.debug("Stack trace: ", re);
            throw new DocLibRepoException(String.format("Internal repository error while saving session"));
        } finally {
            log.debug("logging out from Session ", session.toString());
            session.logout();
        }
    }

    public void close() {
        this.keepAliveSession.logout();
    }
}
