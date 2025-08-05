package com.AJ15.invoicePdfSplitter.Service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlankPageDetector {

    public static void main(String[] args) {
        File file = new File("Blank_tester.pdf");

        try (PDDocument document = Loader.loadPDF(file)) {
//            Test print
//        for(Integer i:findBlankPages(document)){
//            System.out.println(i);
//        }

        splitPdf(document,findBlankPages(document));
//        test print

        } catch (IOException e) {
            throw new RuntimeException("Failed to load PDF", e);
        }
    }

    public static List<Integer> findBlankPages(PDDocument document) throws IOException {
        ArrayList<Integer> blankPageList = new ArrayList<>();

        int pageCount = document.getNumberOfPages();
        PDFTextStripper stripper = new PDFTextStripper();


        for (int i = 0; i < pageCount; i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);

            String text = stripper.getText(document).trim();
            if (text.isEmpty()) {
                blankPageList.add(i);
            }
        }
//        System.out.println(pageCount);
        return blankPageList;
    }

    public static void splitPdf(PDDocument document,List<Integer> blankPages) throws IOException {
        int totalPages = document.getNumberOfPages();
//start page for subpdf
        int startPage = 0;
//        number for new subPdfFile
        int subPdfNo = 1;
//adding last page no for the last pdf

        blankPages.add(totalPages);


        for(int bPages: blankPages){
//            check curretn page is blank
            if(startPage>=bPages){
                startPage = bPages + 1;
                continue;
            }



            try (PDDocument splitDoc = new PDDocument()) {
                for (int i = startPage; i < bPages; i++) {
//                    new pdf for non empty pages
                    splitDoc.addPage(document.getPage(i));
                }
                if(splitDoc.getNumberOfPages()>0){

                    String subFileName = document.getDocumentInformation().getTitle() + "_subPdf_" + subPdfNo+".pdf";
                    splitDoc.save(subFileName);
                    subPdfNo++;
                    System.out.println("file created no:" + subFileName);
                }

            }

//            go to net blank page
            startPage = bPages+1;
        }
    }
}
