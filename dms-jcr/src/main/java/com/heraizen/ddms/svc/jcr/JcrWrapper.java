/*          
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms.svc.jcr;

import com.heraizen.ddms.svc.exceptions.DocLibRepoException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import lombok.extern.slf4j.Slf4j;
import static org.springframework.util.StringUtils.hasText;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class JcrWrapper {

    protected final Repository internalRepo;
    protected final Credentials credentials;
    protected Session keepAliveSession;
    protected final JcrInitializer jcrInitializer;

    protected JcrWrapper(Repository repo, 
            Credentials repoCredentials, JcrInitializer repoInitializer) {
        this.internalRepo = repo;
        this.credentials = repoCredentials;
        this.jcrInitializer = repoInitializer;
        this.keepAliveSession = createKeepAliveSession();
        repoInitializer.initializeRepo(repo, repoCredentials);
    }

    private Session createKeepAliveSession() {
        return login();
    }

    private Session login(String workspaceName) {
        log.debug("logging into workspace {}", workspaceName);
        try {
            return internalRepo.login(credentials, workspaceName);
        } catch (RepositoryException re) {
            String errMsg = String.format("Error while logging into workspace %s of Repository repo %s",
                    workspaceName, internalRepo);
            log.error(errMsg + ":" + re.getMessage());
            log.debug("JCR Error Stack trace : ", re);
            throw new DocLibRepoException("Could not login to Workspace of Doc Lib Repository");
        }
    }

    private Session login() {
        log.debug("logging into default workspace...");
        try {
            return internalRepo.login(credentials);
        } catch (RepositoryException re) {
            String errMsg = "Could not login to default workspace of repository "
                    + internalRepo.toString();
            log.error(errMsg + " : " + re.getMessage());
            log.debug("JCR error :", re);
            throw new DocLibRepoException("Could not login to Workspace of Doc Lib Repository");
        }
    }

    private Session getSession(Optional<String> workspaceName) {
        //NOTE: orElse(login()) will always invoke login() method 
        //since the param is evaluated even though method is not invoked. 
        //This was resulting in a leak...
        return workspaceName.map(wn -> login(wn))
                .orElseGet(() -> login());
    }

    private void doWithWorkspace(Optional<String> workspaceName,
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

    private void doWithNode(Optional<String> workspace,
            Optional<String> nodePath,
            BiConsumer<Session, Node> sessionConsumer,
            boolean saveWhenDone) {
        Session session = getSession(workspace);
        try {
            Node node = nodePath.isPresent() ? session.getNode(nodePath.get())
                    : session.getRootNode();
            //session operations throws typed exceptions, hence it is clumsier 
            //to use map operation on nodePath Optional...

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

    private <T> T doAndGetWithWorkspace(Optional<String> workspaceName,
            Function<Session, T> sessionConsumer, boolean saveWhenDone) {
        Session session = getSession(workspaceName);
        T retVal;
        try {
            log.debug("Session {} is being delegated...", session);
            retVal = sessionConsumer.apply(session);
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
        return retVal;
    }

    private <T> T doAndGetWithNode(Optional<String> workspace,
            Optional<String> nodePath,
            BiFunction<Session, Node, T> sessionConsumer,
            boolean saveWhenDone) {
        Session session = getSession(workspace);
        T retVal;
        try {
            Node node = nodePath.isPresent() ? session.getNode(nodePath.get())
                    : session.getRootNode();
            //session operations throws typed exceptions, hence it is clumsier 
            //to use map operation on nodePath Optional...

            log.debug("Session for workspace {} and  Node {} is being delegated...",
                    workspace, nodePath);
            retVal = sessionConsumer.apply(session, node);
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
        return retVal;
    }

    public void doWithWorkspace(String workspaceName,
            Consumer<Session> sessionConsumer, boolean saveWhenDone) {
        this.doWithWorkspace(toOptional(workspaceName), sessionConsumer, saveWhenDone);
    }

    public void doWithNode(String workspaceName,
            String nodePath,
            BiConsumer<Session, Node> sessionConsumer,
            boolean saveWhenDone) {
        this.doWithNode(toOptional(workspaceName), toOptional(nodePath),
                sessionConsumer, saveWhenDone);
    }

    public void doWithRootNode(String workspaceName, BiConsumer<Session, Node> sessionConsumer,
            boolean saveWhenDone) {
        this.doWithNode(workspaceName, (String) null, sessionConsumer, saveWhenDone);
    }

    public <T> T doAndGetWithWorkspace(String workspaceName,
            Function<Session, T> sessionConsumer, boolean saveWhenDone) {
        return doAndGetWithWorkspace(toOptional(workspaceName), sessionConsumer,
                saveWhenDone);
    }

    public <T> T doAndGetWithNode(String workspaceName,
            String nodePath,
            BiFunction<Session, Node, T> sessionConsumer,
            boolean saveWhenDone) {
        return doAndGetWithNode(toOptional(workspaceName), toOptional(nodePath),
                sessionConsumer, saveWhenDone);
    }

    public <T> T doAndGetWithRootNode(String workspaceName, BiFunction<Session, Node, T> sessionConsumer, boolean saveWhenDone) {
        return this.doAndGetWithNode(workspaceName, (String) null, sessionConsumer, saveWhenDone);
    }

    private Optional<String> toOptional(String s) {
        return hasText(s) ? Optional.of(s.trim()) : Optional.empty();
    }

    public boolean isLive() {
        return this.keepAliveSession != null && this.keepAliveSession.isLive();
    }
    
    public void close() {
        if ( isLive() ) {
            this.keepAliveSession.logout();
        }
        this.keepAliveSession = null;
    }
}
