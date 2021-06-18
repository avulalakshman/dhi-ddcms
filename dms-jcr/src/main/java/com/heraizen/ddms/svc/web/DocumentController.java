package com.heraizen.ddms.svc.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

import com.heraizen.ddms.svc.model.Metadata;
//import com.heraizen.ddms.jcr.jr2.TenantContext;
import com.heraizen.ddms.svc.model.Document;
import com.heraizen.ddms.svc.DigitalLibMgmtService;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/")
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

    @PostMapping(value = "{tenant}/save", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> save(@PathVariable String tenant, @RequestPart("file") MultipartFile multipartFile,
            @RequestPart("metadata") String metadata) {
//        TenantContext.setTenant(tenant);
        try {
            Document doc = toDocument(multipartFile, metadata);
            digiLibSvc.saveDoc(doc);
            try {
                Files.deleteIfExists(doc.getFile().toPath());
            } catch (IOException ie) {
                log.warn("Could not delete temporary file {}...Ignoring err : {}",
                        doc.getFile(), ie.getMessage());
            }
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

    @GetMapping("{tenant}/metadata/{docname}")
    public ResponseEntity<?> getDocMetadata(@PathVariable String tenant, @PathVariable String docname) {
        log.debug("Invoked getDocMetaData for tenant {} and doc name {}", tenant, docname);
//        TenantContext.setTenant(tenant);
        Metadata md = digiLibSvc.getDocumentMetadata(docname);
        return ResponseEntity.ok(md);
    }

    @GetMapping("{tenant}/{docname}")
    public ResponseEntity<?> getDoc(@PathVariable String tenant, @PathVariable String docname) {
        log.debug("Invoked getDocMetaData for tenant {} and doc name {}", tenant, docname);
//        TenantContext.setTenant(tenant);
        Document doc = digiLibSvc.getDocument(docname);
        log.info("Found Document {} in Digi Lib...Returning Doc content...", doc);
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

    @PutMapping("/{tenant}/update_metadata/{docname}")
    public ResponseEntity<?> updateMetadata(@PathVariable String tenant, @PathVariable String docname, @RequestBody Metadata metadata) {
//        TenantContext.setTenant(tenant);
        digiLibSvc.updateDocumentMetadata(docname, metadata);
        return ResponseEntity.accepted()
                .body(String.format("Updated %s's metadata with %s", docname, metadata));
    }

    @GetMapping("{tenant}/search")
    public ResponseEntity<?> search(@PathVariable String tenant, @RequestParam String searchStr) {
        log.debug("Invoked search for tenant {} and search string {}", tenant, searchStr);
//        TenantContext.setTenant(tenant);
        Map<String, Metadata> docMetadata = digiLibSvc.searchDocsWith(searchStr);
        return ResponseEntity.ok(docMetadata);
    }

    @PostMapping("{tenant}/deletedoc/{docname}")
    public ResponseEntity<?> deleteDoc(@PathVariable String tenant, @PathVariable String docname) {
        log.debug("Deleting the document {} for tenant {}", docname, tenant);
//        TenantContext.setTenant(tenant);
        digiLibSvc.deleteDoc(docname);
        return ResponseEntity.ok(docname + " deleted!");
    }

    @GetMapping("metadata/{docname}")
    public ResponseEntity<?> getDocMetadata(@PathVariable String docname) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("{docid}")
    public ResponseEntity<?> getDoc(@PathVariable String docid) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }

    @PutMapping("{docid}/")
    public ResponseEntity<?> updateMetadata(@PathVariable String docid, @RequestBody Metadata metadata) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("search/{searchkeyword}")
    public ResponseEntity<?> search(@PathVariable String searchkeyword) {
        return new ResponseEntity<>("Not Yet Implemented", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("{tenant}/dumpws")
    public ResponseEntity<?> dumpWorkspace(@PathVariable String tenant) {
        log.debug("Invoked dump ws for tenant {}", tenant);
//        TenantContext.setTenant(tenant);
        digiLibSvc.dump();
        return ResponseEntity.ok("Done");
    }

    private Document getDocument(MultipartFile multipartFile, String metadataStr) {
        try {
            Path tempFile = Files.createTempFile("dlib", "tmp");
            String fileName = multipartFile.getOriginalFilename();
            String mimeType = multipartFile.getContentType();
            Metadata metadata = objectMapper.readValue(metadataStr, Metadata.class);
            log.debug("Extracting document fileName: {} \n mimeType: {} \n encoding : UTF_8 \n MetaData : {} \n ",
                    fileName, mimeType, metadata);
            multipartFile.transferTo(tempFile);
            return Document.builder().file(tempFile.toFile()).name(fileName).mimeType(multipartFile.getContentType())
                    .encoding("UTF_8").metadata(metadata).build();
        } catch (JsonProcessingException jpe) {
            log.error("Error while parsing metadata", jpe.getMessage());
            log.debug("Stack trace :", jpe);
        } catch (IOException ie) {
            log.error("Error while reading Multi part file :", ie.getMessage());
            log.debug("Stack trace :", ie);
        }
        return null;
    }

}
