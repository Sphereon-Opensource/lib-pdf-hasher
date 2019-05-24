package com.sphereon.libs.pdfhasher;

import com.sphereon.libs.blockchain.commons.Digest;
import com.sphereon.libs.blockchain.commons.Utils;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


public class PdfHasher {


    private Digest digest;


    public String hashPdf(RandomAccessBufferedFileInputStream inputStream) {
        TreeMap<String, byte[]> evidenceList = buildEvidence(inputStream);
        MerkleTree merkleTree = new MerkleTree(false);
        evidenceList.forEach((hex, bytes) -> {
            merkleTree.addLeaf(bytes);
        });
        merkleTree.build();
        return merkleTree.getMerkleRootAsHex();
    }


    public TreeMap<String, byte[]> buildEvidence(RandomAccessBufferedFileInputStream inputStream) {

        try {
            PDFParser pdfParser = new PDFParser(inputStream);
            pdfParser.parse();

            final TreeMap<String, byte[]> evidenceList = new TreeMap<>();
            final PDDocument pdDocument = pdfParser.getPDDocument();
            String value = "PG:" + pdDocument.getNumberOfPages();
            addHash(evidenceList, value.getBytes());
            AtomicInteger pageNr = new AtomicInteger(0);
            for (PDPage page : pdDocument.getPages()) {
                scanPage(evidenceList, pdDocument, page, pageNr.incrementAndGet());
            }
            return evidenceList;
        } catch (Throwable throwable) {
            throw new RuntimeException("The hashing process failed: " + throwable.getMessage(), throwable);
        }
    }


    private void scanPage(TreeMap<String, byte[]> evidenceList, PDDocument pdDocument, PDPage page, int pageNr) {

        try {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(pageNr);
            pdfStripper.setEndPage(pageNr);
            String pageText = pdfStripper.getText(pdDocument);

            addHash(evidenceList, pageText.getBytes());
        } catch (IOException e) {

        }
/*

        for (Iterator<PDStream> it = page.getContentStreams(); it.hasNext(); ) {
            PDStream stream = it.next();
            final COSStream cosObject = stream.getCOSObject();
            String test = cosObject.toTextString();
            addHash("page data", evidenceList, test.getBytes());
        }
*/

        final PDResources resources = page.getResources();
        List<RenderedImage> images = getImagesFromResources(resources);
        for (RenderedImage image : images) {
            BufferedImage b = (BufferedImage) image;
            WritableRaster raster = b.getRaster();
            DataBufferInt data = (DataBufferInt) raster.getDataBuffer();
            ByteBuffer byteBuffer = ByteBuffer.allocate(data.getSize() * 4);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(data.getData());
            addHash(evidenceList, byteBuffer.array());
        }


        try {
            for (Iterator<PDAnnotation> it = page.getAnnotations().iterator(); it.hasNext(); ) {
                PDAnnotation annotation = it.next();
                final COSDictionary cosObject = annotation.getCOSObject();
                processObject(evidenceList, cosObject.getValues());
            }
        } catch (
                Throwable throwable) {
            throw new RuntimeException("The hashing process failed: " + throwable.getMessage(), throwable);
        }

    }


    private void processObject(TreeMap<String, byte[]> evidenceList, Iterable<COSBase> subValues) {
        for (COSBase subValue : subValues) {
            if (subValue instanceof COSName) {
                final String name = ((COSName) subValue).getName();
                addHash(evidenceList, name.getBytes());
                continue;
            }

            if (subValue instanceof COSObject) {
                COSBase obj = ((COSObject) subValue).getObject();
                if (obj instanceof COSDictionary) {
                    processObject(evidenceList, ((COSDictionary) obj).getValues());

                }
            } else if (subValue instanceof COSArray) {
                processObject(evidenceList, (Iterable<COSBase>) subValue);

            } else if (subValue instanceof COSString) {
                addHash(evidenceList, subValue.toString().getBytes());
            }
        }
    }


    private List<RenderedImage> getImagesFromResources(PDResources resources) {
        List<RenderedImage> images = new ArrayList<>();

        try {
            for (COSName xObjectName : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(xObjectName);

                if (xObject instanceof PDFormXObject) {
                    images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
                } else if (xObject instanceof PDImageXObject) {
                    images.add(((PDImageXObject) xObject).getImage());
                }
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("getImagesFromResources failed: " + throwable.getMessage(), throwable);
        }
        return images;
    }


    private void addHash(TreeMap<String, byte[]> evidenceList, byte[] bytes) {
        digest = Digest.getInstance();
        byte[] hash = digest.getHash(Digest.Algorithm.SHA_256, bytes);
        evidenceList.put(Utils.Hex.encodeAsString(hash), hash);
    }
}
