/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.service;

import com.heraizen.dhi.dhiddcms.model.Document;
import com.heraizen.dhi.dhiddcms.model.Metadata;
import com.heraizen.dhi.dhiddcms.util.DlibConstants;
import com.heraizen.dhi.dhiddcms.util.MultiTenantRepoHolder;
import com.heraizen.dhi.dhiddcms.util.DlibUtil;
import static com.heraizen.dhi.dhiddcms.util.DlibUtil.toStringArray;
import static com.heraizen.dhi.dhiddcms.util.DlibUtil.toStringSet;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
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

    public static final Optional<String> DIGILIB_WORKSPACE = Optional.of(DlibConstants.DLIB_WS_NAME);
    public static final Optional<String> DIGILIB_ROOTNODE = Optional.empty();

    private static void logAndThrowException(String errMsg, Throwable t,
            BiFunction<String, Throwable, RuntimeException> supplier) {
        log.error(errMsg + ((t == null) ? "" : " : " + t.getMessage()));
        log.debug("Error Stack trace", t);
        throw supplier.apply(errMsg, t);
    }

    protected Optional<Node> findDocNode(Node parentNode, String docName) {
        try {
            if (parentNode.hasNode(docName)) {
                Node fileNode = parentNode.getNode(docName);
                if (fileNode.isNodeType(NodeType.NT_FILE)) {  //"nt:file"
                    return Optional.of(fileNode);
                }
            }
        } catch (RepositoryException ex) {
            logAndThrowException(String.format("Error while finding doc node for %s ", docName),
                    ex, (e, t) -> new JcrException(e, t));
        }
        return Optional.empty();
    }

    protected void createFileNode(Document doc, Session session, Node node) {
        Binary jcrBinaryData = null;
        try (InputStream stream = new BufferedInputStream(new FileInputStream(doc.getFile()))) {
            log.info("Adding {} into the repository...", doc.getName());
            //Create the the file node
            Node fileNode = node.addNode(doc.getName(), NodeType.NT_FILE); 
            //create the mandatory child node - jcr:content
            Node resNode = fileNode.addNode(Node.JCR_CONTENT, NodeType.NT_RESOURCE);  

            jcrBinaryData = session.getValueFactory().createBinary(stream);
            resNode.setProperty(Property.JCR_DATA, jcrBinaryData);         // "jcr:data"
            resNode.setProperty(Property.JCR_MIMETYPE, doc.getMimeType()); // "jcr:mimeType"
            resNode.setProperty(Property.JCR_ENCODING, doc.getEncoding()); // "jcr:encoding"
            // nt:resource node type does not accept other properties
            // Add/Set a mixin type which enhances node type to accept 
            // metadata namespace and properties
            resNode.addMixin(DlibConstants.DLIB_NT_METADATA);
            setMetaDataTo(resNode, doc.getMetadata());

        } catch (IOException ie) {
            logAndThrowException(String.format("IO Exception occured while adding file %s to JCR",
                    doc.getName()), ie, (e, t) -> new RuntimeException(e, t));
        } catch (RepositoryException re) {
            logAndThrowException(String.format("JCR Exception occured while adding file %s to JCR",
                    doc.getName()), re, (e, t) -> new JcrException(e, t));
        } finally {
            if (jcrBinaryData != null) {
                jcrBinaryData.dispose();
            }
        }
    }

    protected void setMetaDataTo(Node resNode, Metadata metadata) throws RepositoryException {
        resNode.setProperty(DlibConstants.DLIB_DOC_TITLE, metadata.getTitle());
        resNode.setProperty(DlibConstants.DLIB_DOC_VOL, metadata.getVolume());
        resNode.setProperty(DlibConstants.DLIB_DOC_PUBLISHER, metadata.getPublisher());
        resNode.setProperty(DlibConstants.DLIB_DOC_BARCODE, metadata.getBarCode());
        resNode.setProperty(DlibConstants.DLIB_DOC_ISBN, metadata.getIsbn());
        resNode.setProperty(DlibConstants.DLIB_DOC_AUTHORS,
                toStringArray(metadata.getAuthorNames(), s -> s));
        resNode.setProperty(DlibConstants.DLIB_DOC_SUMMARY, metadata.getSummary());
        resNode.setProperty(DlibConstants.DLIB_DOC_TAGS,
                toStringArray(metadata.getTags(), s -> s));
    }

    public void saveDoc(Document doc) {
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                        (session, node) -> {
                            if (findDocNode(node, doc.getName()).isPresent()) {
                                logAndThrowException("File Already Exists!", null,
                                        (e, t) -> new RuntimeException(e, t));
                            } else {
                                createFileNode(doc, session, node);
                            }
                        }, true);
    }

    private void setMetadataFromProps(Metadata metadata, Property p) throws RepositoryException {
        log.debug("Extracting metadata property {}", p.getName());
        switch (p.getName()) {
            case DlibConstants.DLIB_DOC_TITLE:
                metadata.setTitle(p.getString());
                break;
            case DlibConstants.DLIB_DOC_ISBN:
                metadata.setIsbn(p.getString());
                break;
            case DlibConstants.DLIB_DOC_BARCODE:
                metadata.setBarCode(p.getString());
                break;
            case DlibConstants.DLIB_DOC_SUMMARY:
                metadata.setSummary(p.getString());
                break;
            case DlibConstants.DLIB_DOC_AUTHORS:
                metadata.setAuthorNames(toStringSet(() -> new HashSet<>(), p.getValues()));
                break;
            case DlibConstants.DLIB_DOC_TAGS:
                metadata.setTags(toStringSet(() -> new HashSet<>(), p.getValues()));
                break;
            case DlibConstants.DLIB_DOC_PUBLISHER:
                metadata.setPublisher(p.getString());
                break;
            case DlibConstants.DLIB_DOC_VOL:
                metadata.setVolume(p.getString());
                break;
            default:
                log.warn("Unknown property {} was found...", p.getName());
                break;
        }
    }

    private Document extractDocument(Node fileNode) {
        Document doc = new Document();
        Metadata metadata = new Metadata();
        try {
            doc.setName(fileNode.getName());
            Node resNode = fileNode.getNode(Node.JCR_CONTENT);  //
            PropertyIterator pi = resNode.getProperties();
            while (pi.hasNext()) {
                Property p = pi.nextProperty();
                switch (p.getName()) {
                    case Property.JCR_DATA:     //"jcr:data"
                        Binary jcrData = p.getBinary();
                        try (InputStream in = jcrData.getStream()) {
                            Path file = Files.createTempFile(null, null);
                            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                            doc.setFile(file.toFile());
                        } catch (IOException ie) {
                            logAndThrowException(String.format("Error while copying document from JCR %s ",
                                    doc.getName()), ie, (e, t) -> new RuntimeException(e, t));
                        } finally {
                            jcrData.dispose();
                        }
                        break;
                    case Property.JCR_MIMETYPE:   //"jcr:mimeType"
                        doc.setMimeType(p.getString());
                        break;
                    case Property.JCR_ENCODING:  //"jcr:encoding"
                        doc.setEncoding(p.getString());
                        break;
                    default:
                        setMetadataFromProps(metadata, p);
                        break;
                }
            }
            doc.setMetadata(metadata);
            log.debug("Extracted document {}", doc);
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Error while getting document %s ", doc.getName()),
                    re, (e, t) -> new JcrException(e, t));
        }
        return doc;
    }

    private Metadata extractMetadata(Node fileNode) {
        Metadata metadata = new Metadata();
        log.debug("Extracting metadata JCR file Node...");
        try {
            Node resNode = fileNode.getNode(Node.JCR_CONTENT);  //
            PropertyIterator pi = resNode.getProperties();
            while (pi.hasNext()) {
                setMetadataFromProps(metadata, pi.nextProperty());
            }
            log.debug("Extracted doc's {} metadata {}", fileNode.getName(), metadata);
        } catch (RepositoryException ex) {
            logAndThrowException(String.format("Error while retrieving metadata from JCR : %s",
                    ex.getMessage()), ex, (e, t) -> new JcrException(e, t));
        }

        return metadata;
    }

    private Optional<Document> getDocument(Node parentNode, String docName) {
        return findDocNode(parentNode, docName).map(n -> extractDocument(n));
    }

    private Optional<Metadata> getDocumentMetaData(Node parentNode, String docName) {
        return findDocNode(parentNode, docName).map(this::extractMetadata);
    }

    public Optional<Metadata> getDocumentMetadata(String docName) {
        //NOTE: Following final reference is NOT correct! We have to fix this, find a mechanism to 
        // return values;
        final AtomicReference<Optional<Metadata>> t = new AtomicReference<>(Optional.empty());
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                        (s, node) -> t.set(getDocumentMetaData(node, docName)),
                        false);
        return t.get();
    }

    public Optional<Document> getDocument(String fileName) {
        final AtomicReference<Optional<Document>> t = new AtomicReference<>(Optional.empty());
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                        (s, node) -> t.set(getDocument(node, fileName)), false);
        return t.get();
    }

    public Set<String> executeQuery(Session session, String expression) {
        log.debug("The search expression {}", expression);
        Set<String> docNames = new HashSet<>();
        try {
            Query q = session.getWorkspace().getQueryManager()
                    .createQuery(expression, javax.jcr.query.Query.JCR_SQL2);
            QueryResult result = q.execute();
            NodeIterator ni = result.getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();
                log.debug(" Received node {} with path {} in search result", n.getName(), n.getPath());
                if (n.isNodeType(NodeType.NT_FILE)) {  //"nt:file"
                    String path = n.getPath();
                    log.debug("Adding path {} to the search return set", path);
                    docNames.add(path);
                } else if (n.isNodeType(NodeType.NT_RESOURCE)) {   //"nt:resource"
                    String path = n.getParent().getPath();
                    log.debug("Adding path {} to the search return set", path);
                    docNames.add(path);
                }
            }
            log.debug("Done with search result iterating");
        } catch (RepositoryException re) {
            logAndThrowException("Error while searching the JCR ", re, (e, t) -> new JcrException(e, t));
        }
        return docNames;
    }

    //NOTE: For binary data type (such as pdf, word doc, xls) to be searched
    //tika-parsers should be in the class path. 
    //NOTE 1 : The search should be ordered by lastModifiedDate (or create date) and also
    //should have pagination
    public Set<String> search(String str) {
        Set<String> docNames = new HashSet<>();
        repoHolder.getCurrentTenantRepo().doWithWorkspace(DIGILIB_WORKSPACE, (session) -> {
            String expression = "SELECT * from [nt:resource] as n WHERE CONTAINS(n.*, '" + str + "')";
            docNames.addAll(executeQuery(session, expression));
        }, false);
        return docNames;
    }

    private Optional<Metadata> updateDocumentMetadata(Node fileNode, Metadata metadata) {
        log.debug("Updating metadata JCR file Node...");
        Optional<Metadata> retMetadata = Optional.empty();
        try {
            Node resNode = fileNode.getNode(Node.JCR_CONTENT);  //
            setMetaDataTo(resNode, metadata);
            log.debug("Updated doc's {} metadata {}", fileNode.getName(), metadata);
            retMetadata = Optional.of(extractMetadata(fileNode));
        } catch (RepositoryException ex) {
            logAndThrowException(String.format("Error while updating metadata of JCR : %s",
                    ex.getMessage()), ex, (e, t) -> new JcrException(e, t));
        }
        return retMetadata;
    }

    public Optional<Metadata> updateDocumentMetadata(String docName, Metadata metadata) {
        AtomicReference<Optional<Metadata>> t = new AtomicReference<>(Optional.empty());
        repoHolder.getCurrentTenantRepo().doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                (s, node) -> {
                    Node fileNode = findDocNode(node, docName)
                            .orElseThrow(() -> new RuntimeException(String.format("Doc by name %s not found... ", docName)));
                    t.set(updateDocumentMetadata(fileNode, metadata));
                }, true);
        return t.get();
    }

    private void deleteDocNode(Node node) {
        String name = null;
        try {
            name = node.getName();
            log.debug("Found doc node for {}, attempting to remove", name);
            node.remove();
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Error while removing doc %s", name),
                    re, (e, t) -> new JcrException(e, t));
        }
    }

    public void deleteDoc(String docName) {
        repoHolder.getCurrentTenantRepo().doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                (s, node) -> findDocNode(node, docName).ifPresent(this::deleteDocNode), true);
    }

    public void dumpWorkspace() {
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                        (s, rootNode) -> DlibUtil.dump(rootNode), false);
    }
}
