package com.heraizen.dhi.dhiddcms.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.heraizen.dhi.dhiddcms.model.Metadata;
import com.heraizen.dhi.dhiddcms.service.DocumentService;

@RestController
@RequestMapping("/api/v1/")
public class DocumentController {

	@Autowired
	private DocumentService documentService;

	@PostMapping(value = "", consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<?> save(@RequestPart("file") MultipartFile multipartFile,
			@RequestPart("metadata") String metadata) {
		String docId = documentService.saveDocument(multipartFile, metadata);
		if (docId == null || docId.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(docId, HttpStatus.CREATED);
	}

	@GetMapping("metadata/{docname}")
	public ResponseEntity<?> getDocMetadata(@PathVariable String docname) {
		Metadata documentMetadata = documentService.getDocumentMetadata(docname);
		if (documentMetadata !=null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(documentMetadata, HttpStatus.OK);
	}

	@GetMapping("{docid}")
	public ResponseEntity<?> getDoc(@PathVariable String docid) {
		return null;
	}

	@PutMapping("{docid}/")
	public ResponseEntity<?> updateMetadata(@PathVariable String docid, @RequestBody Metadata metadata) {
		return null;
	}

	@GetMapping("search/{searchkeyword}")
	public ResponseEntity<?> search(@PathVariable String searchkeyword) {
		return null;
	}

}
