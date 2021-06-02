/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.service;

import com.heraizen.dhi.dhiddcms.exceptions.DocAlreadyExists;
import com.heraizen.dhi.dhiddcms.exceptions.DocLibIOException;
import com.heraizen.dhi.dhiddcms.exceptions.DocLibRepoException;
import com.heraizen.dhi.dhiddcms.exceptions.DocNotFoundException;
import com.heraizen.dhi.dhiddcms.model.Document;
import com.heraizen.dhi.dhiddcms.model.Metadata;
import com.heraizen.dhi.dhiddcms.util.MultiTenantRepoHolder;
import static com.heraizen.dhi.dhiddcms.service.DlibUtil.toStringArray;
import static com.heraizen.dhi.dhiddcms.service.DlibUtil.toStringSet;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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
public class DigitalLibMgmtServiceJcrImpl implements DigitalLibMgmtService {

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
            logAndThrowException(String.format("Internal Repository error "
                    + "while finding doc (node) %s ", docName),
                    ex, (e, t) -> new DocLibRepoException(e));
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
            logAndThrowException(String.format("IO Exception occured while saving Doc %s to Library",
                    doc.getName()), ie, (e, t) -> new DocLibIOException(e));
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Internal Repository error while saving %s ", 
                    doc.getName()), re, (e, t) -> new DocLibRepoException(e));
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

    @Override
    public void saveDoc(Document doc) {
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, DIGILIB_ROOTNODE,
                        (session, node) -> {
                            if (findDocNode(node, doc.getName()).isPresent()) {
                                logAndThrowException(String.format("Cant save! Document "
                                        + "by name %s Already exists ", doc.getName()), null,
                                        (e, t) -> new DocAlreadyExists(e));
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
                            logAndThrowException(String.format("IO Error while copying "
                                    + "document from internal repository %s ", doc.getName()),
                                    ie, (e,t)->new DocLibIOException(e));
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
            logAndThrowException(String.format("Internal Repository Error while "
                    + "fetching document %s", doc.getName()),
                    re, (e, t) -> new DocLibRepoException(e));
        }
        return doc;
    }

    private Metadata extractMetadata(Node fileNode) {
        Metadata metadata = new Metadata();
        log.debug("Extracting metadata JCR file Node...");
        String docName = null;
        try {
            docName = fileNode.getName();
            Node resNode = fileNode.getNode(Node.JCR_CONTENT);  //
            PropertyIterator pi = resNode.getProperties();
            while (pi.hasNext()) {
                setMetadataFromProps(metadata, pi.nextProperty());
            }
            log.debug("Extracted doc's {} metadata {}", docName, metadata);
        } catch (RepositoryException ex) {
            logAndThrowException(String.format("Error while retrieving metadata "
                    + "from internal repository for %s",
                    docName), ex, (e, t) -> new DocLibRepoException(e));
        }

        return metadata;
    }

    private Document getDocument(Node parentNode, String docName) {
        return findDocNode(parentNode, docName).map(this::extractDocument)
                .orElseThrow(()-> new DocNotFoundException("Document by name " + docName + " Not found"));
    }

    private Metadata getDocumentMetaData(Node parentNode, String docName) {
        return findDocNode(parentNode, docName).map(this::extractMetadata)
                .orElseThrow(()-> new DocNotFoundException("Document by name " + docName + " Not found"));
    }

    @Override
    public Metadata getDocumentMetadata(String docName) {
        //NOTE: Following final reference is NOT correct! We have to fix this, find a mechanism to 
        // return values;
        final AtomicReference<Metadata> t = new AtomicReference<>();
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, DIGILIB_ROOTNODE,
                        (s, node) -> t.set(getDocumentMetaData(node, docName)),
                        false);
        return t.get();
    }

    @Override
    public Document getDocument(String fileName) {
        final AtomicReference<Document> t = new AtomicReference<>();
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, DIGILIB_ROOTNODE,
                        (s, node) -> t.set(getDocument(node, fileName)), false);
        return t.get();
    }

    public Map<String, Metadata> executeQuery(Session session, String expression) {
        log.debug("The search expression {}", expression);
        //NOTE: Dont know at this time, whether a node appears twice (if we search)
        //against type (nt:base). Hence we will keep one map, with path as a key,
        //so that the duplicate paths get eliminated
        //Once we settle on nodes, we will extract the Metadata based on fileNodes
        Map<String, Node> docNodes = new TreeMap<>(); 
        Map<String, Metadata> docMetadata = new TreeMap<>();
        try {
            Query q = session.getWorkspace().getQueryManager()
                    .createQuery(expression, javax.jcr.query.Query.JCR_SQL2);
            QueryResult result = q.execute();
            NodeIterator ni = result.getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();
                log.debug(" Received node {} with path {} in search result", n.getName(), n.getPath());
                if (n.isNodeType(NodeType.NT_FILE)) {  //"nt:file"
                    log.debug("Adding Node {} to the search return set", n);
                    docNodes.put(n.getPath(), n);                    
                } else if (n.isNodeType(NodeType.NT_RESOURCE)) {   //"nt:resource"
                    Node fileNode = n.getParent();
                    log.debug("Adding Node {} to the search return set", fileNode);
                    docNodes.put(fileNode.getPath(), fileNode);
                }
            }
            log.debug("Done with search result iterating");
            docNodes.forEach((pt,nd)->docMetadata.put(pt, extractMetadata(nd)));
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Internal repository error while searching for %s", expression),
                    re, (e, t) -> new DocLibRepoException(e));
        }
        return docMetadata;
    }

    //NOTE: For binary data type (such as pdf, word doc, xls) to be searched
    //tika-parsers should be in the class path. 
    //NOTE 1 : The search should be ordered by lastModifiedDate (or create date) and also
    //should have pagination
    @Override
    public Map<String, Metadata> searchDocsWith(String str) {
        Map<String, Metadata> docMetadata = new TreeMap<>();
        repoHolder.getCurrentTenantRepo().doWithWorkspace(DIGILIB_WORKSPACE, (session) -> {
            String expression = "SELECT * from [nt:resource] as n WHERE CONTAINS(n.*, '" + str + "')";
            docMetadata.putAll(executeQuery(session, expression));
        }, false);
        return docMetadata;
    }

    private void updateDocumentMetadata(Node fileNode, Metadata metadata) {
        log.debug("Updating metadata JCR file Node...");
        String docName = null;
        try {
            docName = fileNode.getName();
            Node resNode = fileNode.getNode(Node.JCR_CONTENT);  //
            setMetaDataTo(resNode, metadata);
            log.debug("Updated doc's {} metadata {}", fileNode.getName(), metadata);
        } catch (RepositoryException ex) {
            logAndThrowException(String.format("Internal Repository Error while updating metadata for %s ",
                    docName), ex, (e, t) -> new DocLibRepoException(e));
        }
    }

    @Override
    public void updateDocumentMetadata(String docName, Metadata metadata) {
        repoHolder.getCurrentTenantRepo().doWithNode(DIGILIB_WORKSPACE, DIGILIB_ROOTNODE,
                (s, rnode) -> {
                    Node fileNode = findDocNode(rnode, docName)
                            .orElseThrow(() -> new DocNotFoundException(docName + 
                                    " Not found for updating metadata!"));
                    updateDocumentMetadata(fileNode, metadata);
                }, true);
    }

    private void deleteDocNode(Node node) {
        String name = null;
        try {
            name = node.getName();
            log.debug("Found doc node for {}, attempting to remove", name);
            node.remove();
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Internal Repository Error while removing doc %s", name),
                    re, (e, t) -> new DocLibRepoException(e));
        }
    }

    @Override
    public void deleteDoc(String docName) {
        repoHolder.getCurrentTenantRepo().doWithNode(DIGILIB_WORKSPACE, DIGILIB_ROOTNODE,
                (s, node) ->deleteDocNode(findDocNode(node, docName)
                            .orElseThrow(()->new DocNotFoundException(docName + " Not found!"))), true);
    }

    @Override
    public void dump() {
        repoHolder.getCurrentTenantRepo()
                .doWithNode(DIGILIB_WORKSPACE, Optional.empty(),
                        (s, rootNode) -> DlibUtil.dump(rootNode), false);
    }
}
