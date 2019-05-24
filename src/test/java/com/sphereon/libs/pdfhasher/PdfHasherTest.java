package com.sphereon.libs.pdfhasher;


import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import static j2html.TagCreator.*;


public class PdfHasherTest {

    @Test
    public void testPdfHasher_20() throws IOException, URISyntaxException {
        PdfHasher pdfHasher = new PdfHasher();
        String hashOriginal = pdfHasher.hashPdf(openInputStream("/20.org.pdf"));
        String hashFoxIt = pdfHasher.hashPdf(openInputStream("/20.foxit.pdf"));
        String hashAdobe = pdfHasher.hashPdf(openInputStream("/20.adobe.pdf"));
        Assert.assertEquals(hashOriginal, hashFoxIt);
        Assert.assertEquals(hashOriginal, hashAdobe);
    }


    @Test
    public void testPdfHasher_26() throws IOException, URISyntaxException {
        PdfHasher pdfHasher = new PdfHasher();
        String hashOriginal = pdfHasher.hashPdf(openInputStream("/26.org.pdf"));
        String hashFoxIt = pdfHasher.hashPdf(openInputStream("/26.foxit.pdf"));
        String hashAdobe = pdfHasher.hashPdf(openInputStream("/26.adobe.pdf"));

        Assert.assertEquals(hashOriginal, hashFoxIt);
        Assert.assertEquals(hashOriginal, hashAdobe);
    }


    //    @Test
    public void printContent() throws IOException, URISyntaxException {
        PdfHasher pdfHasher = new PdfHasher();
        MerkleTree merkleTree = new MerkleTree(true);
        TreeMap<String, byte[]> evidenceListOrg = pdfHasher.buildEvidence(openInputStream("/20.org.pdf"));
        TreeMap<String, byte[]> evidenceListFoxIt = pdfHasher.buildEvidence(openInputStream("/20.foxit.pdf"));
        TreeMap<String, byte[]> evidenceListAdobe = pdfHasher.buildEvidence(openInputStream("/20.adobe.pdf"));

        print("./20.html", evidenceListOrg, evidenceListFoxIt, evidenceListAdobe);


    }


    private void print(String outputFileName, TreeMap<String, byte[]> evidenceListOrg, TreeMap<String, byte[]> evidenceListFoxIt, TreeMap<String, byte[]> evidenceListAdobe) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {

            String[] aOriginal = evidenceListOrg.keySet().toArray(new String[evidenceListOrg.size()]);
            String[] aFoxit = evidenceListFoxIt.keySet().toArray(new String[evidenceListFoxIt.size()]);
            String[] aAdobe = evidenceListAdobe.keySet().toArray(new String[evidenceListAdobe.size()]);

            List<DomContent> rows = new ArrayList();
            for (int i = 0; i < evidenceListOrg.size(); i++) {
                String itemOrg = aOriginal[i];
                String itemFoxit = null;
                if (aFoxit.length > i) {
                    itemFoxit = aFoxit[i];
                }
                String itemAdobe = null;
                if (aAdobe.length > 0) {
                    itemAdobe = aAdobe[i];
                }
                rows.add(tr(td(itemOrg), td(itemFoxit), td(itemAdobe)));
            }

            ContainerTag output = html(body(table(thead(
                    tr(
                            th("Original"), th("Foxit"), th("Acrobat")
                    )), tbody(
                    rows.toArray(new DomContent[rows.size()])
            ))));

            final String str = output.renderFormatted();
            writer.write(str);
        }
    }


    private RandomAccessBufferedFileInputStream openInputStream(String resourceName) throws URISyntaxException, IOException {
        final URL resource = getClass().getResource(resourceName);
        File testFile = new File(resource.toURI().getPath());
        return new RandomAccessBufferedFileInputStream(testFile);
    }
}
