package com.heraizen.dhi.dhiddcms.service;

import com.heraizen.dhi.dhiddcms.model.Document;
import com.heraizen.dhi.dhiddcms.model.Metadata;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    public String saveDocument(MultipartFile file, String metadataStr);
    public Metadata getDocumentMetadata(String docName);
    public Document getDocument(String id);
    public boolean updateDocumentMetadata(String docId, Metadata metadata);
    public List<Document> search(String str);
}
