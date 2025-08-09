package org.ofd.render;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class OFDRender {
    private static final Logger logger = LoggerFactory.getLogger(OFDRender.class);

    /**
     * user space units per millimeter
     */
    private static final float POINTS_PER_MM = 1 / (10 * 2.54f) * 72;

    public static byte[] convertPdfToOfd(byte[] pdfBytes) {
        String tempFilePath = generateTempFilePath();
        PDDocument doc = null;
        try {
            FileUtils.writeByteArrayToFile(new File(tempFilePath), pdfBytes);
            doc = PDDocument.load(new File(tempFilePath));

            long parseStart = System.currentTimeMillis();
            OFDCreator ofdCreator = new OFDCreator();
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                ofdCreator.addPage(i);
                PDRectangle cropBox = doc.getPage(i).getCropBox();
                float widthPt = cropBox.getWidth();
                float heightPt = cropBox.getHeight();
                OFDPageDrawer ofdPageDrawer = new OFDPageDrawer(i, doc.getPage(i), ofdCreator, 1 / POINTS_PER_MM);
                ofdPageDrawer.drawPage();
                ofdCreator.addPageContent(i, ofdPageDrawer.getCtLayer(), widthPt / POINTS_PER_MM, heightPt / POINTS_PER_MM);
            }
            long parseEnd = System.currentTimeMillis();
            logger.info("parse speed time {}", parseEnd - parseStart);

            long genStart = System.currentTimeMillis();
            byte[] ofdBytes = ofdCreator.jar();
            long genEnd = System.currentTimeMillis();
            logger.info("gen ofd speed time {}", genEnd - genStart);
            return ofdBytes;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (doc != null) {
                    doc.close();
                }
                FileUtils.forceDeleteOnExit(new File(tempFilePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected static String generateTempFilePath() {
        return System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID().toString();
    }
}
