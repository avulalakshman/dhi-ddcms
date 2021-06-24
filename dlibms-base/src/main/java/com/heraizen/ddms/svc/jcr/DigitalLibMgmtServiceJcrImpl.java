/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.ddms.svc.jcr;

import com.heraizen.ddms.svc.exceptions.DocAlreadyExists;
import com.heraizen.ddms.svc.exceptions.DocLibIOException;
import com.heraizen.ddms.svc.exceptions.DocLibRepoException;
import com.heraizen.ddms.svc.exceptions.DocNotFoundException;
import com.heraizen.ddms.svc.model.Document;
import com.heraizen.ddms.svc.model.Metadata;
import com.heraizen.ddms.svc.DigitalLibMgmtService;
import static com.heraizen.ddms.svc.jcr.DlibConstants.DLIB_WS_NAME;
import static com.heraizen.ddms.svc.jcr.DlibUtil.toStringArray;
import static com.heraizen.ddms.svc.jcr.DlibUtil.toStringSet;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
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
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 *
 * @author Pradeepkm
 */
@Slf4j
public class DigitalLibMgmtServiceJcrImpl implements DigitalLibMgmtService {

    public static final String DIGILIB_ROOTNODE = null;

    private final DlibmsRepoSource repoSource;

    @Builder
    protected DigitalLibMgmtServiceJcrImpl(DlibmsRepoSource dlibmsRepoSource) {
        this.repoSource = dlibmsRepoSource;
    }

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

    protected void createFileNode(Document doc, Session session, Node parentNode) {
        Binary jcrBinaryData = null;
        try (InputStream stream = new BufferedInputStream(new FileInputStream(doc.getFile()))) {
            log.info("Adding {} into the repository...", doc.getName());
            //Create the the file node
            Node fileNode = parentNode.addNode(doc.getName(), NodeType.NT_FILE);
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
        log.info("Saved document {} into Doc library", doc);
    }

    protected void setMetaDataTo(Node resNode, Metadata metadata) throws RepositoryException {
        if (Objects.isNull(metadata)) {
            log.warn(" A document with NULL metadata was provided to be saved into the Doc Library");
            return;
        }
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

    private File readFileData(Property p) throws RepositoryException {
        File file = null;
        Binary jcrData = p.getBinary();
        try (InputStream dataStream = jcrData.getStream()) {
            Path filePath = Files.createTempFile(null, null);
            Files.copy(dataStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            file = filePath.toFile();
        } catch (IOException ie) {
            logAndThrowException(String.format("IO Error while copying "
                    + "document content from internal repository "),
                    ie, (e, t) -> new DocLibIOException(e));
        } finally {
            jcrData.dispose();
        }
        return file;
    }

    private Document extractDocument(Node fileNode) {
        Document doc = new Document();
        Metadata metadata = new Metadata();
        log.debug("Extracting document and metadata from node {}", fileNode);
        try {
            String docName = fileNode.getName();
            log.info("Reading content and other metadata from properties of {} ", docName);
            doc.setName(docName);
            Node resNode = fileNode.getNode(Node.JCR_CONTENT);  //
            PropertyIterator pi = resNode.getProperties("jcr:data | jcr:mimeType | jcr:encoding | dlib:*");
            while (pi.hasNext()) {
                Property p = pi.nextProperty();
                log.debug("Doc extraction, found property {} ", p.getName());
                switch (p.getName()) {
                    case "jcr:data":            //Property.JCR_DATA
                        doc.setFile(readFileData(p));
                        break;
                    case "jcr:mimeType":        //Property.JCR_MIMETYPE
                        doc.setMimeType(p.getString());
                        break;
                    case "jcr:encoding":        //Property.JCR_ENCODING
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
        log.debug("Extracting metadata of JCR file Node...");
        String docName = null;
        try {
            docName = fileNode.getName();
            Node resNode = fileNode.getNode(Node.JCR_CONTENT);  //
            PropertyIterator pi = resNode.getProperties("dlib:*");
            while (pi.hasNext()) {
                Property p = pi.nextProperty();
                log.debug("Reading property {}", p.getName());
                setMetadataFromProps(metadata, p);
            }
            log.debug("Extracted doc's {} metadata {}", docName, metadata);
        } catch (RepositoryException ex) {
            logAndThrowException(String.format("Error while retrieving metadata "
                    + "from internal repository for %s",
                    docName), ex, (e, t) -> new DocLibRepoException(e));
        }

        return metadata;
    }

    public Map<String, Metadata> executeDocSearchQry(Session session, String qry) {
        log.debug("Executing doc search query: {}", qry);
        //NOTE: Dont know at this time, whether a node appears twice (if we search)
        //against type (nt:base). Hence we will keep one map, with path as a key,
        //so that the duplicate paths get eliminated
        //Once we settle on nodes, we will extract the Metadata based on fileNodes
        Map<String, Node> docNodes = new TreeMap<>();
        Map<String, Metadata> docMetadata = new TreeMap<>();
        try {
            Query q = session.getWorkspace().getQueryManager()
                    .createQuery(qry, javax.jcr.query.Query.JCR_SQL2);
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
            docNodes.forEach((pt, nd) -> docMetadata.put(pt, extractMetadata(nd)));
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Internal repository error while searching for %s", qry),
                    re, (e, t) -> new DocLibRepoException(e));
        }
        log.debug("Returning {} docs as search results for {}", docMetadata.size(), qry);
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

    private void deleteDocNode(Node docFileNode) {
        String name = null;
        try {
            name = docFileNode.getName();
            log.debug("Found doc node for {}, attempting to remove", name);
            docFileNode.remove();
            log.info("Deleted the document {} from Doc Library...", name);
        } catch (RepositoryException re) {
            logAndThrowException(String.format("Internal Repository Error while removing doc %s", name),
                    re, (e, t) -> new DocLibRepoException(e));
        }
    }

    @Override
    public boolean saveDoc(Document doc) {
        Assert.notNull(doc, "Document (obj) can not be NULL while saving the doc into library");
        Assert.hasText(doc.getName(), "Document name should be provided for saving the document into Library");
        Assert.notNull(doc.getFile(), "Document file MUST be available for saving the doc into library");
        Assert.isTrue(doc.getFile().exists(),
                "Document file MUST be available for saving the doc into library "
                + "supplied file " + doc.getFile() + " does not seem to exist");
        Assert.isTrue(doc.getFile().isFile(), "Document file should be a normal file, "
                + "and can not be directory or otherwise");
        Assert.isTrue(doc.getFile().length() > 0, "Empty file can not be saved into library, "
                + "supplied file " + doc.getFile() + " with provided name " + doc.getName()
                + "seems to be empty");

        return repoSource.getRepo()
                .doAndGetWithNode(DLIB_WS_NAME, DIGILIB_ROOTNODE,
                        (session, node) -> {
                            if (findDocNode(node, doc.getName()).isPresent()) {
                                logAndThrowException(String.format("Cant save! Document "
                                        + "by name %s Already exists ", doc.getName()), null,
                                        (e, t) -> new DocAlreadyExists(e));
                                return false;
                            } else {
                                createFileNode(doc, session, node);
                                return true;
                            }
                        }, true);
    }

    @Override
    public Metadata getDocumentMetadata(String docName) {
        Assert.hasText(docName, "Document name MUST be provided for fetching the document metadata");
        log.debug("Getting Metadata of Document {} from Doc Library...");
        return repoSource.getRepo()
                .doAndGetWithNode(DLIB_WS_NAME, DIGILIB_ROOTNODE,
                        (s, rnode) -> findDocNode(rnode, docName)
                                .map(this::extractMetadata)
                                .orElseThrow(() -> new DocNotFoundException("Document by name "
                                + docName + " Not found")),
                        false); //dont have to save the workspace, as we are just reading...
    }

    @Override
    public Document getDocument(String docName) {
        Assert.hasText(docName, "Document name must be provided for fetching the document");
        log.debug("Getting Document {} from Doc Library...");
        return repoSource.getRepo()
                .doAndGetWithNode(DLIB_WS_NAME, DIGILIB_ROOTNODE,
                        (s, rnode) -> findDocNode(rnode, docName)
                                .map(this::extractDocument)
                                .orElseThrow(() -> new DocNotFoundException("Document by name "
                                + docName + " Not found")),
                        false); //dont have to save the workspace, as we are just reading...
    }

    //NOTE: For binary data type (such as pdf, word doc, xls) to be searched
    //tika-parsers should be in the class path. 
    //NOTE 1 : The search should be ordered by lastModifiedDate (or create date) and also
    //should have pagination
    @Override
    public Map<String, Metadata> searchDocsWith(String searchStr) {
        Assert.hasText(searchStr, "Empty search is not supported yet...");
        return repoSource.getRepo().doAndGetWithWorkspace(DLIB_WS_NAME, (session) -> {
            String docSearchQry = "SELECT * from [nt:resource] as n WHERE CONTAINS(n.*, '" + searchStr + "')";
            return executeDocSearchQry(session, docSearchQry);
        }, false);
    }

    @Override
    public void updateDocumentMetadata(String docName, Metadata metadata) {
        Assert.hasText(docName, "Document name must be provided for updating metadata");
        Assert.notNull(docName, "Document metadata can not be null for updation docname :" + docName);
        repoSource.getRepo().doWithNode(DLIB_WS_NAME, DIGILIB_ROOTNODE,
                (s, rnode) -> {
                    Node docFileNode = findDocNode(rnode, docName)
                            .orElseThrow(() -> new DocNotFoundException(docName
                            + " Not found for updating metadata!"));
                    updateDocumentMetadata(docFileNode, metadata);
                }, true); //save the workspace...
    }

    @Override
    public void deleteDoc(String docName) {
        Assert.hasText(docName, "Document name MUST be provided for deletion!");
        repoSource.getRepo().doWithNode(DLIB_WS_NAME, DIGILIB_ROOTNODE,
                (s, node) -> deleteDocNode(findDocNode(node, docName)
                        .orElseThrow(() -> new DocNotFoundException(docName + " Not found!"))),
                true);//save the workspace...
    }

    @Override
    public void dump() {
        repoSource.getRepo()
                .doWithRootNode(DLIB_WS_NAME, (s, rootNode) -> DlibUtil.dump(rootNode),
                        false); // nothing to save...
    }
}
