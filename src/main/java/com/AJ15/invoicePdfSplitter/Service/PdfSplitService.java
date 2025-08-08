package com.AJ15.invoicePdfSplitter.Service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class PdfSplitService {

    private static final String OUTPUT_DIR = "C:\\Users\\iamar\\Documents\\Documents\\Important Docs\\invoicePdfSplitter\\split-output";

    public List<Integer> findBlankPages(PDDocument document) throws IOException {
        List<Integer> blankPageList = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper();
        int totalPages = document.getNumberOfPages();

        for (int i = 0; i < totalPages; i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);

            String text = stripper.getText(document).trim();
            if (text.isEmpty()) {
                blankPageList.add(i);
                log.info("Detected blank page at index: {}", i);
            }
        }

        return blankPageList;
    }

    public List<File> splitByBlankPages(PDDocument document, String originalFilename) throws IOException {
        List<Integer> blankPages = findBlankPages(document);
        return splitPdfAtPages(document, blankPages, getBaseName(originalFilename));
    }

    public List<File> splitByEqualSizes(PDDocument document, int chunkSize, String originalFilename) throws IOException {
        List<File> splitFiles = new ArrayList<>();
        int totalPages = document.getNumberOfPages();
        String baseName = getBaseName(originalFilename);
        int subPdfNo = 1;

        for (int start = 0; start < totalPages; start += chunkSize) {
            int end = Math.min(start + chunkSize, totalPages);

            try (PDDocument splitDoc = new PDDocument()) {
                for (int i = start; i < end; i++) {
                    splitDoc.addPage(document.getPage(i));
                }

                String fileName = baseName + "_subPdf_" + subPdfNo + ".pdf";
                File outputFile = saveToDisk(splitDoc, fileName);
                splitFiles.add(outputFile);

                log.info("Saved equal-sized split file: {}", outputFile.getAbsolutePath());
                subPdfNo++;
            }
        }

        return splitFiles;
    }

    private List<File> splitPdfAtPages(PDDocument document, List<Integer> splitIndexes, String baseFileName) throws IOException {
        List<File> splitFiles = new ArrayList<>();
        int totalPages = document.getNumberOfPages();
        int startPage = 0;
        int subPdfNo = 1;

        splitIndexes.add(totalPages); // Ensure the last chunk

        for (int endPage : splitIndexes) {
            if (startPage >= endPage) {
                startPage = endPage + 1;
                continue;
            }

            try (PDDocument splitDoc = new PDDocument()) {
                for (int i = startPage; i < endPage; i++) {
                    splitDoc.addPage(document.getPage(i));
                }

                if (splitDoc.getNumberOfPages() > 0) {
                    String fileName = baseFileName + "_subPdf_" + subPdfNo + ".pdf";
                    File outputFile = saveToDisk(splitDoc, fileName);
                    splitFiles.add(outputFile);

                    log.info("Saved split by blank page: {}", outputFile.getAbsolutePath());
                    subPdfNo++;
                }
            }

            startPage = endPage + 1;
        }

        return splitFiles;
    }

    public List<File> splitPdfByRanges(PDDocument document, List<int[]> pageRanges, String baseFileName) throws IOException {
        List<File> splitFiles = new ArrayList<>();
        int subPdfNo = 1;

        for (int[] range : pageRanges) {
            int startPage = range[0] - 1;
            int endPage = range[1];

            if (startPage < 0 || endPage > document.getNumberOfPages() || startPage >= endPage) {
                log.warn("Skipping invalid range: {}-{}", range[0], range[1]);
                continue;
            }

            try (PDDocument splitDoc = new PDDocument()) {
                for (int i = startPage; i < endPage; i++) {
                    splitDoc.addPage(document.getPage(i));
                }

                if (splitDoc.getNumberOfPages() > 0) {
                    String fileName = baseFileName + "_subPdf_" + subPdfNo + ".pdf";
                    File outputFile = saveToDisk(splitDoc, fileName);
                    splitFiles.add(outputFile);

                    log.info("Saved range split: {}", outputFile.getAbsolutePath());
                    subPdfNo++;
                }
            }
        }

        return splitFiles;
    }

    // 🧠 Utility to write PDF to disk
    private File saveToDisk(PDDocument document, String filename) throws IOException {
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) outputDir.mkdirs();

        File outputFile = new File(outputDir, filename);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            document.save(fos);
        }
        return outputFile;
    }

    public File zipFiles(List<File> pdfFiles, String zipFileName) throws IOException {
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) outputDir.mkdirs();

        File zipFile = new File(outputDir, zipFileName);
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File file : pdfFiles) {
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }

        log.info("Created zip file: {}", zipFile.getAbsolutePath());

        return zipFile;
    }


    private String getBaseName(String originalFilename) {
        if (originalFilename == null) return "document";
        int dotIndex = originalFilename.lastIndexOf(".");
        return dotIndex == -1 ? originalFilename : originalFilename.substring(0, dotIndex);
    }
    public StreamingResponseBody zipFilesToStream(List<File> pdfFiles) throws IOException {
        return outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                for (File file : pdfFiles) {
                    ZipEntry entry = new ZipEntry(file.getName());
                    zos.putNextEntry(entry);
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            } catch (IOException e) {
                log.error("Error streaming zip file", e);
            }
            clearOutputDirectory(pdfFiles);

        };
    }
    public static void clearOutputDirectory(List<File> pdfFiles) throws IOException {
            for (File file : pdfFiles) {
                if (!file.delete()) {
                    throw new IOException("Failed to delete file: " + file.getAbsolutePath());
                }
            }
    }

}
