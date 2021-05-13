# googledlp

This demonstrate the capability of masking PDF contents with specific stop words using Google DLP


Runnning the code

mvn clean package

Place you PDF in /resources/pdf/<yourfile>

java -cp target/PDFRedact-jar-with-dependencies.jar com.gcp.dlp.sample.RedactImageFileListedInfoTypes <project-id> pdf/delta.pdf
