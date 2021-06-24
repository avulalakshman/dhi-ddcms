package com.heraizen.ddms.svc.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

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
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestParam;

import com.heraizen.ddms.svc.model.Metadata;
import com.heraizen.ddms.svc.model.Document;
import com.heraizen.ddms.svc.DigitalLibMgmtService;

@RestController
@RequestMapping("/doclibapi/v1")
@Slf4j
public class DocumentController {

    @Autowired
    private DigitalLibMgmtService digiLibSvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Document toDocument(MultipartFile multipartFile, String metadataStr) throws JsonProcessingException, IOException {
        Path tempFile = Files.createTempFile("dlib", "tmp");
        String fileName = multipartFile.getOriginalFilename();
        String mimeType = multipartFile.getContentType();
        String encoding = "UTF-8";
        Metadata metadata = objectMapper.readValue(metadataStr, Metadata.class);
        log.debug("Extracting document fileName: {} \n mimeType: {} \n encoding : {} \n MetaData : {} \n ", fileName, mimeType, encoding, metadata);
        multipartFile.transferTo(tempFile);
        return Document.builder()
                .file(tempFile.toFile())
                .name(fileName)
                .mimeType(mimeType)
                .encoding(encoding)
                .metadata(metadata)
                .build();
    }

    @PostMapping(value = "/save", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> save(@RequestPart("file") MultipartFile multipartFile,
            @RequestPart("metadata") String metadata) {
        log.debug("Saving a file into dlib repository...");
        try {
            Document doc = toDocument(multipartFile, metadata);
            digiLibSvc.saveDoc(doc);
            try {
                Files.deleteIfExists(doc.getFile().toPath());
            } catch (IOException ie) {
                log.warn("Could not delete temporary file {}...Ignoring err : {}",
                        doc.getFile(), ie.getMessage());
            }
            log.info("Successfully saved the document {} into dlib repository...", doc.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(doc.getName() + " successfully saved into Document library");
        } catch (JsonProcessingException jpe) {
            log.error("Error while parsing metadata of {} : {}", multipartFile.getName(), jpe.getMessage());
            log.debug("Stack trace :", jpe);
            return ResponseEntity.badRequest()
                    .body("Could not parse metadata of file " + multipartFile.getName());
        } catch (IOException ie) {
            log.error("Error while reading Multi part file {} : {}",
                    multipartFile.getName(), ie.getMessage());
            log.debug("Stack trace :", ie);
            return ResponseEntity.badRequest()
                    .body("Could not read the uploaded file because of IO Error...");
        }
    }

    @GetMapping("/metadata/{docname}")
    public ResponseEntity<?> getDocMetadata(@PathVariable String docname) {
        log.debug("Getting document metadata for doc {}", docname);
        Metadata md = digiLibSvc.getDocumentMetadata(docname);
        return ResponseEntity.ok(md);
    }

    @GetMapping("/getdoc/{docname}")
    public ResponseEntity<?> getDoc(@PathVariable String docname) {
        log.debug("Getting document with name {} from dlib repository...", docname);
        Document doc = digiLibSvc.getDocument(docname);
        log.info("Found Document {} in dlib repo, returning Doc content...", doc);
        MediaType mt = MediaType.valueOf(doc.getMimeType());
        log.debug("Media Type : {}", mt);
        FileSystemResource resource = new FileSystemResource(doc.getFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment;filename=" + doc.getName()) // Content-Disposition
                .contentType(mt) // Content-Type
                .contentLength(doc.getFile().length()) // Content-Length
                .body(resource);
    }

    @PutMapping("/update_metadata/{docname}")
    public ResponseEntity<?> updateMetadata(@PathVariable String docname, @RequestBody Metadata metadata) {
        log.debug("Updating metadata of doc {} ", docname);
        digiLibSvc.updateDocumentMetadata(docname, metadata);
        log.info("Updated the metadata of doc {}", docname);
        return ResponseEntity.accepted()
                .body(String.format("Updated %s's metadata with %s", docname, metadata));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String searchStr) {
        log.debug("Searching dlib repository for {}", searchStr);
        Map<String, Metadata> docMetadata = digiLibSvc.searchDocsWith(searchStr);
        return ResponseEntity.ok(docMetadata);
    }

    @PostMapping("/deletedoc/{docname}")
    public ResponseEntity<?> deleteDoc(@PathVariable String docname) {
        log.debug("Deleting document {} from dlib repository", docname);
        digiLibSvc.deleteDoc(docname);
        log.info("Deleted the document from dlib repository {}", docname);
        return ResponseEntity.ok(docname + " deleted!");
    }

    @GetMapping("/dumpws")
    public ResponseEntity<?> dumpWorkspace() {
        log.debug("Invoked dumping dlibworkspace...");
        digiLibSvc.dump();
        return ResponseEntity.ok("Done");
    }
}
