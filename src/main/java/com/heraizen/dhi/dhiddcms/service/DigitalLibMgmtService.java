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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
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

    private void logAndThrowException(String errMsg, Throwable t, BiFunction<String, Throwable, RuntimeException> supplier) {
        log.error(errMsg + ":" + t.getMessage());
        log.debug("Error Stack trace", t);
        throw supplier.apply(errMsg, t);
    }

    public String[] toArray(Collection<String> c, Function<String, String> transformer) {
        return c.stream().map(transformer).toArray(i -> new String[i]);
    }

    protected void createFileNode(Document doc, Session session, Node node) {
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

            setMetaData(fileNode, doc.getMetadata());

        } catch (IOException ie) {
            logAndThrowException(String.format("IO Exception occured while adding file %s to JCR",
                    doc.getName()), ie, (e, t) -> new RuntimeException(e, t));
        } catch (RepositoryException re) {
            logAndThrowException(String.format("JCR Exception occured while adding file %s to JCR",
                    doc.getName()), re, (e, t) -> new JcrException(e, t));
        }
    }

    protected void setMetaData(Node fileNode, Metadata metadata) throws RepositoryException {
        fileNode.setProperty("dlib:barcode", metadata.getBarCode());
        fileNode.setProperty("dlib:isbn", metadata.getIsbn());
        fileNode.setProperty("dlib:authors",
                toArray(metadata.getAuthorNames(), s -> s));
        fileNode.setProperty("dlib:summary", metadata.getSummary());
        fileNode.setProperty("dlib:tags",
                toArray(metadata.getTags(), String::toLowerCase));
    }

    public void saveDoc(Document doc) {
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                        (session, node) -> createFileNode(doc, session, node), true);
    }

    public void dumpWorkspace() {
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                        (session, rootNode) -> JcrUtil.dump(rootNode), false);
    }

    private Set<String> toStringSet(Supplier<Set<String>> supplier, Value[] vals) throws RepositoryException {
        Set<String> coll = supplier.get();
        for (Value v : vals) {
            coll.add(v.getString());
        }
        return coll;
    }

    private Optional<Document> getDocument(Node parentNode, String docName) {
        try {
            Optional<Metadata> metadata = getDocumentMetaData(parentNode, docName);
            if (metadata.isPresent()) {
                Document doc = new Document();
                doc.setName(docName);
                doc.setMetadata(metadata.get());
                Node fileNode = parentNode.getNode(docName);
                Node resNode = fileNode.getNode("jcr:content");
                PropertyIterator pi = resNode.getProperties("jcr:data | jcr:mimeType | jcr:encoding");
                while (pi.hasNext()) {
                    Property p = pi.nextProperty();
                    switch (p.getName()) {
                        case "jcr:data":
                            InputStream s = p.getBinary().getStream();
                            Path file = Files.createTempFile(null, null);
                            Files.copy(s, file);
                            doc.setFile(file.toFile());
                            break;
                        case "jcr:mimeType":
                            doc.setMimeType(p.getString());
                            break;
                        case "jcr:encoding":
                            doc.setEncoding(p.getString());
                            break;
                        default:
                            break;
                    }
                }
                return Optional.of(doc);
            } else {
                return Optional.empty();
            }
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Error while getting document %s ", docName),
                    re, (e, t) -> new JcrException(e, t));
        } catch (IOException ie) {
            logAndThrowException(String.format("Error while getting document %s ", docName),
                    ie, (e, t) -> new RuntimeException(e, t));
        }
        return Optional.empty();
    }

    private Optional<Metadata> getDocumentMetaData(Node parentNode, String docName) {
        try {
            if (parentNode.hasNode(docName)) {
                Node fileNode = parentNode.getNode(docName);
                if (!fileNode.isNodeType("nt:file")) {
                    throw new RuntimeException("Document by Name " + docName + " Not found");
                }
                Metadata retMetadata = new Metadata();
                PropertyIterator pi = fileNode.getProperties("dlib:*");
                while (pi.hasNext()) {
                    Property p = pi.nextProperty();
                    String pname = p.getName();
                    switch (pname) {
                        case "dlib:barcode":
                            retMetadata.setBarCode(p.getString());
                            break;
                        case "dlib:isbn":
                            retMetadata.setBarCode(p.getString());
                            break;
                        case "dlib:summary":
                            retMetadata.setSummary(p.getString());
                            break;
                        case "dlib:authors":
                            retMetadata.setAuthorNames(toStringSet(() -> new HashSet<>(), p.getValues()));
                            break;
                        case "dlib:tags":
                            retMetadata.setTags(toStringSet(() -> new HashSet<>(), p.getValues()));
                            break;
                        default:
                            log.warn("Unknown property {} was found...", pname);
                            break;
                    }
                }
                return Optional.of(retMetadata);
            }
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Error while getting meta data of %s ", docName),
                    re, (e, t) -> new JcrException(e, t));
        }
        return Optional.empty();
    }

    public Optional<Metadata> getDocumentMetadata(String docName) {
        //NOTE: Following final reference is NOT correct! We have to fix this, find a mechanism to 
        // return values;
        final AtomicReference<Optional<Metadata>> t = new AtomicReference<>(Optional.empty());
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(), (session, node) -> {
                    Optional<Metadata> m = getDocumentMetaData(node, docName);
                    t.set(m);
                }, false);
        return t.get();
    }

    public Optional<Document> getDocument(String fileName) {
        final AtomicReference<Optional<Document>> t = new AtomicReference<>(Optional.empty());
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(), (session, node) -> {
                    Optional<Document> d = getDocument(node, fileName);
                    t.set(d);
                }, false);
        return t.get();
    }

    public boolean updateDocumentMetadata(String docId, Metadata metadata) {
        throw new UnsupportedOperationException();
    }

    public Set<String> search(String str) {
        Set<String> docNames = new HashSet<>();
        repoHolder.getCurrentTenantRepo().doWithWorkspace(DIGILIB_WORKSPACE, (session) -> {
            try {
                String expression = "SELECT * from [nt:base] AS n WHERE NAME() LIKE '%" + str + "%' OR CONTAINS(n.[jcr:data | dlib:*], '" + str + "')";
                log.info("The Query string is {}", expression);
                Query q = session.getWorkspace().getQueryManager()
                        .createQuery(expression, javax.jcr.query.Query.JCR_SQL2);
                QueryResult result = q.execute();
                NodeIterator ni = result.getNodes();
                while (ni.hasNext()) {
                    Node n = ni.nextNode();
                    log.debug( " Received node {} with path {} in search result", n.getName(), n.getPath());
                    if ( n.isNodeType("nt:file") ) {
                        String path = n.getPath();
                        log.debug("Adding path {} to the search return set", path);
                        docNames.add(path);
                    } else if (n.isNodeType("nt:resource")) {
                        String path = n.getParent().getPath();
                        log.debug("Adding path {} to the search return set", path);
                        docNames.add(path);
                    } 
                }
                log.debug("Done with search result iterating");
            } catch (RepositoryException re) {
                logAndThrowException("Error while searching the JCR ", re, (e,t)-> new JcrException(e, t));
            }
        }, false);
        return docNames;
    }
}
