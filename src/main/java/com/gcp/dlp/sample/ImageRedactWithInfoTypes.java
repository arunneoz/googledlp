/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcp.dlp.sample;

// [START dlp_redact_image_listed_infotypes]

import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.privacy.dlp.v2.ByteContentItem;
import com.google.privacy.dlp.v2.ByteContentItem.BytesType;
import com.google.privacy.dlp.v2.CustomInfoType;
import com.google.privacy.dlp.v2.CustomInfoType.Dictionary;
import com.google.privacy.dlp.v2.CustomInfoType.Dictionary.WordList;
import com.google.privacy.dlp.v2.InfoType;
import com.google.privacy.dlp.v2.InspectConfig;
import com.google.privacy.dlp.v2.LocationName;
import com.google.privacy.dlp.v2.RedactImageRequest;
import com.google.privacy.dlp.v2.RedactImageRequest.ImageRedactionConfig;
import com.google.privacy.dlp.v2.RedactImageResponse;
import com.google.protobuf.ByteString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.spire.pdf.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class RedactImageFileListedInfoTypes {


    public static void main(String[] args) throws IOException, URISyntaxException {
        // TODO(developer): Replace these variables before running the sample.
        String projectId = "" ; // ToDo Replace with your Project ID;
        String inputPDF = ""; // Replace the Input PDF you want to mask
        String inputPath; // Replace the Path where you want to store the converted PDF
        String outputPath = "/resources/output"; //Replace


        for(int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
            if(args[0].length() > 0)
                projectId= args[0];
            else
                displayErrorArguments(" Project Id missing");
            if(args[1].length() > 0) {
                inputPDF = args[1];

            }
            else
                displayErrorArguments(" Input PDF Path missing");



        }

        Path targetPath = Paths.get(System.getProperty("user.dir"));

        inputPath = targetPath.toString() + "/output/pdf/images/";
        System.out.print(inputPath);
        outputPath = targetPath.toString() + "/output/images/";



        convertpdf2png(inputPDF);
        redactImageFileListedInfoTypes(projectId, inputPath, outputPath);

        // Replace with the right path
        combineImagesIntoPDF(outputPath + "redacted-delta.pdf",
                outputPath
                );
    }

    static void displayErrorArguments(String argument)
    {
        System.out.println("Argument missing " + argument + "  Proper usage java RedactImageFileListedInfoTypes <projectId> <inputPDFLocation>");
    }

    static void convertpdf2png(String inputPath) throws IOException, URISyntaxException {


        Path  resource = Paths.get(System.getProperty("user.dir") + "/target/classes/"+ inputPath);
        System.out.println(resource);
        Path targetPath = Paths.get(System.getProperty("user.dir"));

        String path = targetPath.toString() + "/output/pdf/images/";
        System.out.print(path);

        PDDocument document = PDDocument.load(new File(String.valueOf(resource)));
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(
                    page, 100, ImageType.RGB);
            ImageIOUtil.writeImage(
                    bim, path + String.format("%s-%d.png", resource.toString().substring(resource.toString().lastIndexOf("/") + 1),page), 100);
        }
        document.close();



    }

    private static void combineImagesIntoPDF(String pdfPath, String... inputDirsAndFiles) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (String input : inputDirsAndFiles) {
                Files.find(Paths.get(input),
                        Integer.MAX_VALUE,
                        (path, basicFileAttributes) -> Files.isRegularFile(path))
                        .forEachOrdered(path -> addImageAsNewPage(doc, path.toString()));
            }
            doc.save(pdfPath);
        }
    }


    private static void addImageAsNewPage(PDDocument doc, String imagePath) {
        try {
            PDImageXObject image          = PDImageXObject.createFromFile(imagePath, doc);
            PDRectangle pageSize       = PDRectangle.A4;

            int            originalWidth  = image.getWidth();
            int            originalHeight = image.getHeight();
            float          pageWidth      = pageSize.getWidth();
            float          pageHeight     = pageSize.getHeight();
            float          ratio          = Math.min(pageWidth / originalWidth, pageHeight / originalHeight);
            float          scaledWidth    = originalWidth  * ratio;
            float          scaledHeight   = originalHeight * ratio;
            float          x              = (pageWidth  - scaledWidth ) / 2;
            float          y              = (pageHeight - scaledHeight) / 2;

            PDPage page           = new PDPage(pageSize);
            doc.addPage(page);
            try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                contents.drawImage(image, x, y, scaledWidth, scaledHeight);
            }
            System.out.println("Added: " + imagePath);
        } catch (IOException e) {
            System.err.println("Failed to process: " + imagePath);
            e.printStackTrace(System.err);
        }
    }

    static void redactImageFileListedInfoTypes(String projectId, String inputPath, String outputPath)
            throws IOException {
        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        File dir = new File(inputPath);
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Directory: " + file.getAbsolutePath());
                //showFiles(file.listFiles()); // Calls same method again.
            } else {
                System.out.println("File: " + file.getAbsolutePath());

                try (DlpServiceClient dlp = DlpServiceClient.create()) {
                    // Specify the content to be redacted.
                    ByteString fileBytes = ByteString.readFrom(new FileInputStream(file.getAbsolutePath()));
                    ByteContentItem byteItem =
                            ByteContentItem.newBuilder().setType(BytesType.IMAGE_PNG).setData(fileBytes).build();
                    // Dictionary List for ITAR / Aviation Terms

                    Dictionary wordList =
                            Dictionary.newBuilder()
                                    .setWordList(
                                            WordList.newBuilder()
                                                    .addWords("DC-10s")
                                                    .addWords("777-300")
                                                    .build())
                                    .build();

                    // Specify the word list custom info type the inspection will look for.
                    InfoType infoType = InfoType.newBuilder().setName("CUSTOM_BOEING_STOPWORDS").build();
                    CustomInfoType customInfoType =
                            CustomInfoType.newBuilder().setInfoType(infoType).setDictionary(wordList).build();
                    InspectConfig inspectConfig =
                            InspectConfig.newBuilder().addCustomInfoTypes(customInfoType).build();

                    // Specify the types of info necessary to redact.
            /*List<InfoType> infoTypes = new ArrayList<>();
            // See https://cloud.google.com/dlp/docs/infotypes-reference for complete list of info types
            for (String typeName :
                    new String[] {"US_SOCIAL_SECURITY_NUMBER", "EMAIL_ADDRESS", "PHONE_NUMBER"}) {
                infoTypes.add(InfoType.newBuilder().setName(typeName).build());
            }*/
                    // InspectConfig inspectConfig = InspectConfig.newBuilder().addAllInfoTypes(infoTypes).build();


                    // Construct the Redact request to be sent by the client.
                    RedactImageRequest request =
                            RedactImageRequest.newBuilder()
                                    .setParent(LocationName.of(projectId, "global").toString())
                                    .setByteItem(byteItem)
                                    .setInspectConfig(inspectConfig)
                                    .build();

                    // Use the client to send the API request.
                    RedactImageResponse response = dlp.redactImage(request);

                    // Parse the response and process results.
                    String outPutFile = outputPath + String.format("/%s-redacted.png", file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("/") + 1));
                    FileOutputStream redacted = new FileOutputStream(outPutFile);
                    redacted.write(response.getRedactedImage().toByteArray());
                    redacted.close();
                    System.out.println("Redacted image written to " + outPutFile);
                }
            }
            }
        }
    }




// [END dlp_redact_image_listed_infotypes]
