package com.heraizen.ddms.svc;

import com.heraizen.ddms.DlibmsCndNodeTypeImporter;
import com.heraizen.ddms.svc.exceptions.DocAlreadyExists;
import com.heraizen.ddms.svc.exceptions.DocNotFoundException;
import com.heraizen.ddms.svc.jcr.DigitalLibMgmtServiceJcrImpl;
import com.heraizen.ddms.svc.jcr.DlibmsRepo;
import com.heraizen.ddms.svc.jcr.DlibmsRepoSource;
import com.heraizen.ddms.svc.model.Document;
import com.heraizen.ddms.svc.model.Metadata;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import org.apache.commons.io.FileUtils;

import org.apache.jackrabbit.core.TransientRepository;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@SpringBootTest
class DdmsTests {

    static final String TEST_REPO_CONFIG_FILE = "test-repository.xml";
    //NOTE: You can also use URI notation for absolute paths
    static final String TEST_REPO_HOME = "./dlibms-test.repo/";
    static final Credentials TEST_REPO_CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());
    static final DlibmsCndNodeTypeImporter TEST_DLIB_NT_IMPORTER = new DlibmsCndNodeTypeImporter();
    static final String TEST_PDF_FILE = "india.pdf";
    static final String TEST_TXT_FILE = "karnataka.txt";

    TransientRepository repo;
    DlibmsRepo libmsRepo;
    DigitalLibMgmtService libmsSvc;

    TransientRepository createRepository() {
        try {
            Resource repoConfigRes = new ClassPathResource(TEST_REPO_CONFIG_FILE);
            //NOTE: You can also use URI notation for absolute paths
            Path repoHomePath = Paths.get(TEST_REPO_HOME);
            return new TransientRepository(repoConfigRes.getFile(),
                    repoHomePath.toFile());
        } catch (IOException e) {
            System.out.println(String.format("Could not create JCR Repo for Testing error %s", e.getMessage()));
            e.printStackTrace();
            throw new RuntimeException("Create Repository error ", e);
        }
    }

    DlibmsRepo createDlibmsRepo(TransientRepository repo) {
        return DlibmsRepo.repoBuilder()
                .repo(repo)
                .credentials(TEST_REPO_CREDENTIALS)
                .dlibmsNtRegistrationMaker(TEST_DLIB_NT_IMPORTER)
                .build();
    }

    DigitalLibMgmtService createDlibms(DlibmsRepoSource repoSource) {
        return DigitalLibMgmtServiceJcrImpl.builder()
                .dlibmsRepoSource(repoSource)
                .build();
    }

    @BeforeEach
    public void setUp() throws IOException {
        File repoHome = Paths.get(TEST_REPO_HOME).toFile();
        if (repoHome.exists()) {
            FileUtils.deleteDirectory(repoHome);
        }
        this.repo = createRepository();
        this.libmsRepo = createDlibmsRepo(this.repo);
        this.libmsSvc = createDlibms(this.libmsRepo);
    }

    @AfterEach
    public void tearDown() {
        this.repo.shutdown();
    }

    File getClassPathTestFile(String name) {
        try {
            return new ClassPathResource(name).getFile();
        } catch (IOException e) {
            System.out.println(String.format("Test file by name %s not found, "
                    + "Could not continue testing ", name));
            throw new RuntimeException("Error finding test file", e);
        }
    }

    Document createPDFDoc() {
        File docFile = getClassPathTestFile(TEST_PDF_FILE);
        Metadata docMetadata = new Metadata();
        List<String> authorList = Arrays.asList("PDFAuthor1", "PDFAuthor2");
        List<String> tags = Arrays.asList("PDFTag1");
        docMetadata.setAuthorNames(new HashSet<>(authorList));
        docMetadata.setBarCode("PDF_BARCODE1");
        docMetadata.setPublisher("Pdf TestPublisher");
        docMetadata.setIsbn("ISBN Number");
        docMetadata.setSummary("This is a summary for PDF");
        docMetadata.setTags(new HashSet<>(tags));
        docMetadata.setTitle("Title of the PDF document");

        return Document.builder()
                .file(docFile).name(docFile.getName()).encoding("UTF-8")
                .mimeType("application/pdf")
                .metadata(docMetadata)
                .build();
    }

    Document createTextDoc() {
        File docFile = getClassPathTestFile(TEST_TXT_FILE);
        Metadata docMetadata = new Metadata();
        List<String> authorList = Arrays.asList("TxtAuthor1", "TxtAuthor2");
        List<String> tags = Arrays.asList("TxtTag1");
        docMetadata.setAuthorNames(new HashSet<>(authorList));
        docMetadata.setBarCode("BARCODE1");
        docMetadata.setPublisher("Txt TestPublisher");
        docMetadata.setIsbn("ISBN Number");
        docMetadata.setSummary("This is a summary for Text File");
        docMetadata.setTags(new HashSet<>(tags));
        docMetadata.setTitle("Title of the Text document");

        return Document.builder()
                .file(docFile).name(docFile.getName()).encoding("UTF-8")
                .mimeType("text/plain")
                .metadata(docMetadata)
                .build();
    }

    boolean isSame(Metadata md1, Metadata md2) {
        assertThat(md1.getAuthorNames()).containsAll(md2.getAuthorNames());
        assertThat(md1.getBarCode()).isEqualTo(md2.getBarCode());
        assertThat(md1.getIsbn()).isEqualTo(md2.getIsbn());
        assertThat(md1.getPublisher()).isEqualTo(md2.getPublisher());
        assertThat(md1.getSummary()).isEqualTo(md2.getSummary());
        assertThat(md1.getTitle()).isEqualTo(md2.getTitle());
        assertThat(md1.getTags()).containsAll(md2.getTags());
        assertThat(md1.getVolume()).isEqualTo(md2.getVolume());

        return true;
    }

    boolean isSame(Document doc1, Document doc2) {
        assertThat(doc1.getName()).isEqualTo(doc2.getName());
        assertThat(doc1.getEncoding()).isEqualTo(doc2.getEncoding());
        assertThat(doc1.getMimeType()).isEqualTo(doc2.getMimeType());
        assertThat(doc1.getMetadata())
                .matches(md1 -> isSame(md1, doc2.getMetadata()));

        return true;
    }

    @Test
    public void testSave_Get_UpdateMD_Delete_Doc() {
        //Test saving the document...
        Document doc = createPDFDoc();
        boolean docSaved = libmsSvc.saveDoc(doc);
        assertThat(docSaved).isTrue();

        //Test get the saved doc
        Document savedDoc = libmsSvc.getDocument(doc.getName());
        assertThat(savedDoc).isNotNull();
        assertThat(savedDoc).matches(sd -> isSame(sd, doc));

        //Test get Metadata of the doc
        Metadata savedMd = libmsSvc.getDocumentMetadata(doc.getName());
        assertThat(savedMd).isNotNull()
                .matches(smd -> isSame(smd, doc.getMetadata()));

        Metadata tobeUpdated = savedMd;
        tobeUpdated.getAuthorNames().add("PDFAuthor3");
        libmsSvc.updateDocumentMetadata(doc.getName(), tobeUpdated);

        Metadata updated = libmsSvc.getDocumentMetadata(doc.getName());
        assertThat(updated).isNotNull()
                .matches(umd -> isSame(umd, tobeUpdated))
                .matches(umd -> umd.getAuthorNames().size() == 3);

        //Test delete
        libmsSvc.deleteDoc(doc.getName());
        Throwable de = null;
        try {
            libmsSvc.getDocument(doc.getName());
        } catch (Throwable e) {
            de = e;
        }
        assertThat(de).isNotNull()
                .isInstanceOf(DocNotFoundException.class);
    }

    @Test
    public void testSearchPDFdoc() {
        System.out.println("Testing search tag");
        Document doc = createPDFDoc();
        boolean docSaved = libmsSvc.saveDoc(doc);
        assertThat(docSaved).isTrue();

        Map<String, Metadata> searchResult = libmsSvc.searchDocsWith("PDFTag1");
        assertThat(searchResult).hasSizeGreaterThan(0);

        //NOTE: Initially content inside file could not be tested
        //it started working once the datastore is defined in 
        //test-repository.xml
        searchResult = libmsSvc.searchDocsWith("Bengal");
        System.out.println("Search results: " + searchResult);
        assertThat(searchResult).hasSizeGreaterThan(0);
    }

    @Test
    public void testSearchTextdoc() {
        System.out.println("Testing searching a txt file content");
        Document doc = createTextDoc();
        boolean docSaved = libmsSvc.saveDoc(doc);
        assertThat(docSaved).isTrue();

        Map<String, Metadata> searchResult = libmsSvc.searchDocsWith("South");
        System.out.println("Search results: " + searchResult);
        assertThat(searchResult).hasSizeGreaterThan(0);
    }

    @Test
    public void testDocAlreadyExists() {
        System.out.println("Testing avoiding duplicate addition of files");
        Document doc = createTextDoc();
        boolean docSaved = libmsSvc.saveDoc(doc);
        assertThat(docSaved).isTrue();

        Throwable docAlreadyExists = null;
        try {
            libmsSvc.saveDoc(doc);
        } catch (Throwable t) {
            System.out.println("Caught Exception " + t.getMessage());
            docAlreadyExists = t;
        }
        assertThat(docAlreadyExists).isNotNull()
                .isInstanceOf(DocAlreadyExists.class);
    }

    @Test
    public void testDocNotFoundForDeletion() {
        System.out.println("Testing avoiding duplicate addition of files");

        Throwable docNotFound = null;
        try {
            libmsSvc.deleteDoc("doc_does_not_exist.doc");
        } catch (Throwable t) {
            System.out.println("Caught Exception " + t.getMessage());
            docNotFound = t;
        }
        assertThat(docNotFound).isNotNull()
                .isInstanceOf(DocNotFoundException.class);
    }

    @Test
    public void testDocNotFoundForUpdateMetadata() {
        System.out.println("Testing avoiding duplicate addition of files");

        Throwable docNotFound = null;
        try {
            libmsSvc.updateDocumentMetadata("doc_does_not_exist.doc", new Metadata());
        } catch (Throwable t) {
            System.out.println("Caught Exception " + t.getMessage());
            docNotFound = t;
        }

        assertThat(docNotFound).isNotNull()
                .isInstanceOf(DocNotFoundException.class);
    }
}
