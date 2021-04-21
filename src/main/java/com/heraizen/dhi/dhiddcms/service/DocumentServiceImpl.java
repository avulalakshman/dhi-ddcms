package com.heraizen.dhi.dhiddcms.service;

import java.io.IOException;
import java.util.List;


import com.heraizen.dhi.dhiddcms.model.Document;
import com.heraizen.dhi.dhiddcms.model.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentServiceImpl implements  DocumentService {

	@Autowired
	private ObjectMapper objectMapper;

	public String saveDocument(MultipartFile file, String metadataStr) {
		Document doc = new Document();
		try {
			doc.setFile(file.getBytes());
			doc.setName(file.getOriginalFilename());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Metadata metadata = objectMapper.readValue(metadataStr, Metadata.class);
			doc.setMetadata(metadata);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		String id = null;
		// Save and get the unique path
		log.info("New document saved with id :{}", id);
		return id;
	}

	public Metadata getDocumentMetadata(String docName) {
		Assert.notNull(docName, "Document name can't be empty or null");
		Metadata documentMetadata = null;
		log.info("For given {docName} document found metadata {} ", docName,documentMetadata);
		return documentMetadata;
	}

	public Document getDocument(String id) {
		return null;
	}

	public boolean updateDocumentMetadata(String docId, Metadata metadata) {
		return false;
	}

	public List<Document> search(String str) {
		return null;
	}

	public List<Metadata> getAllMetadata(){
		return null;
	}

}
