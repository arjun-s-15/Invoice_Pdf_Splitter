package com.AJ15.invoicePdfSplitter.Controller;

import com.AJ15.invoicePdfSplitter.Service.PdfSplitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/split")
@RequiredArgsConstructor
public class PdfSplitController {

    private static final String OUTPUT_DIR = "C:\\Users\\iamar\\Documents\\Documents\\Important Docs\\invoicePdfSplitter\\split-output"; // 🔁 Replace with your path
    private final PdfSplitService pdfSplitService;

    @PostMapping("/blank")
    public ResponseEntity<StreamingResponseBody> splitByBlankPages(@RequestParam("file") MultipartFile file) {
        return splitAndZip(file, "blank");
    }

    @PostMapping("/equal")
    public ResponseEntity<StreamingResponseBody> splitByEqualSizes(@RequestParam("file") MultipartFile file,
                                                    @RequestParam("chunkSize") int chunkSize) {
        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {
            String baseName = file.getOriginalFilename();
            List<File> splitFiles = pdfSplitService.splitByEqualSizes(document, chunkSize, baseName);
            StreamingResponseBody stream = pdfSplitService.zipFilesToStream(splitFiles);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"split_pdfs.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        } catch (IOException e) {
            log.error("Error during equal split", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping(value = "/ranges", consumes = "multipart/form-data")
    public ResponseEntity<StreamingResponseBody>  splitByRanges(@RequestParam("file") MultipartFile file,
                                                @RequestParam("ranges") List<String> ranges) {
        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {
            String baseName = file.getOriginalFilename();
            List<int[]> pageRanges = parseRanges(ranges);
            List<File> splitFiles = pdfSplitService.splitPdfByRanges(document, pageRanges, baseName);
            StreamingResponseBody stream = pdfSplitService.zipFilesToStream(splitFiles);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"split_pdfs.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        } catch (IOException e) {
            log.error("Error during range split", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    private ResponseEntity<StreamingResponseBody> splitAndZip(MultipartFile file, String splitType) {
        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes())) {
            String baseName = file.getOriginalFilename();
            List<File> splitFiles = pdfSplitService.splitByBlankPages(document, baseName);
            StreamingResponseBody stream = pdfSplitService.zipFilesToStream(splitFiles);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"split_pdfs.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(stream);
        } catch (IOException e) {
            log.error("Error during blank split", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    private File createZipFromFiles(List<File> files, String zipFileName) throws IOException {
        File zipFile = new File(OUTPUT_DIR, zipFileName);
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : files) {
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
        return zipFile;
    }

    private List<int[]> parseRanges(List<String> rangeStrings) {
        List<int[]> ranges = new ArrayList<>();
        for (String range : rangeStrings) {
            String[] parts = range.split("-");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0]);
                    int end = Integer.parseInt(parts[1]);
                    ranges.add(new int[]{start, end});
                } catch (NumberFormatException e) {
                    log.warn("Invalid range format: {}", range);
                }
            } else {
                log.warn("Invalid range: {}", range);
            }
        }
        return ranges;
    }
}
