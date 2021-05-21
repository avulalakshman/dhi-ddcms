/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.service;

import com.heraizen.dhi.dhiddcms.model.Document;
import com.heraizen.dhi.dhiddcms.model.Metadata;
import com.heraizen.dhi.dhiddcms.util.MultiTenantRepoHolder;
import com.heraizen.dhi.dhiddcms.util.JcrUtil;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Pradeepkm
 */
@Component
@Slf4j
public class DigitalLibMgmtService {

    @Autowired
    MultiTenantRepoHolder repoHolder;

//    public static final Optional<String> DIGILIB_WORKSPACE = Optional.of("__dhi_digitalLibrary__");
    
    public static final Optional<String> DIGILIB_WORKSPACE = Optional.empty();
    
    private void logAndThrowException(String errMsg, Throwable re,
            Supplier<RuntimeException> supplier) {
        log.error(errMsg + ":" + re.getMessage());
        log.debug("Error Stack trace", re);
        throw supplier.get();
    }

    public void saveDoc(Document doc) {
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                        (session, node) -> {
                            try (InputStream stream = new BufferedInputStream(new FileInputStream(doc.getFile()))) {
                                log.info("Adding {} into the repository...", doc.getName());
                                //Create the the file node
                                Node fileNode = node.addNode(doc.getName(), "nt:file");
                                //create the mandatory child node - jcr:content
                                Node resNode = fileNode.addNode("jcr:content", "nt:resource");

                                Binary binary = session.getValueFactory().createBinary(stream);
                                resNode.setProperty("jcr:data", binary);
                                resNode.setProperty("jcr:mimeType", doc.getMimeType());
                                resNode.setProperty("jcr:encoding", doc.getEncoding());
                            } catch (IOException ie) {
                                String errMsg = String.format("IO Exception occured while adding file %s to JCR",
                                        doc.getName());
                                logAndThrowException(errMsg, ie, () -> new RuntimeException(errMsg, ie));
                            } catch (RepositoryException re) {
                                String errMsg = String.format("JCR Exception occured while adding file %s to JCR",
                                        doc.getName());
                                logAndThrowException(errMsg, re, () -> new JcrException(errMsg, re));
                            }
                        }, true);
    }

    public void dumpWorkspace() {
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(), 
                        (session, rootNode) -> JcrUtil.dump(rootNode), false);
    }

    public Metadata getDocumentMetadata(String docName) {
        throw new UnsupportedOperationException();
    }

    public Document getDocument(String id) {
        throw new UnsupportedOperationException();
    }

    public boolean updateDocumentMetadata(String docId, Metadata metadata) {
        throw new UnsupportedOperationException();
    }

    public List<Document> search(String str) {
        throw new UnsupportedOperationException();
    }

}
