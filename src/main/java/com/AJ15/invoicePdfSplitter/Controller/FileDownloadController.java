package com.AJ15.invoicePdfSplitter.Controller;

import com.AJ15.invoicePdfSplitter.Service.PdfSplitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/files")
public class FileDownloadController {

    PdfSplitService pdfSplitService;

    public FileDownloadController(PdfSplitService pdfSplitService) {
        this.pdfSplitService = pdfSplitService;
    }

    private static final String OUTPUT_DIR = "C:\\Users\\iamar\\Documents\\Documents\\Important Docs\\invoicePdfSplitter\\split-output";

    @GetMapping("/download-zip")
    public ResponseEntity<StreamingResponseBody> downloadZip() throws IOException {
        List<File> splitFiles = Files.list(Paths.get(OUTPUT_DIR))
                .filter(path -> path.toString().endsWith(".pdf"))
                .map(Path::toFile)
                .toList();  


        StreamingResponseBody stream = pdfSplitService.zipFilesToStream(splitFiles);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"split_pdfs.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream);
    }

}
