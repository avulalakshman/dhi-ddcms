/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.heraizen.dhi.dhiddcms.service;

import com.heraizen.dhi.dhiddcms.model.Document;
import com.heraizen.dhi.dhiddcms.model.Metadata;
import java.util.Map;

/**
 *
 * @author Pradeepkm
 */
public interface DigitalLibMgmtService {
    public void saveDoc(Document doc);
    public Document getDocument(String docName);
    public Metadata getDocumentMetadata(String docName);
    public void updateDocumentMetadata(String docName, Metadata metadata);
    public void deleteDoc(String docName);
    public Map<String, Metadata> searchDocsWith(String searchStr);
    
    public void dump();
}
