package org.openintents.alternativeapps.common;

import android.content.pm.PackageInfo;
import android.os.Environment;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import fr.xgouchet.androidlib.data.TextFileUtils;
import fr.xgouchet.axml.CompressedXmlParser;

public final class ManifestUtils {

    public static final String ANDROID_NAME_SPACE = "http://schemas.android.com/apk/res/android";
    public static final String MANIFEST = "AndroidManifest.xml";

    /**
     * @param info    the package info
     * @return
     * @throws TransformerException         when an error occurs while converting DOM to file xml
     * @throws FileNotFoundException        when the destination file can't be written
     * @throws ZipException                 if a zip error occurs while reading the source file
     * @throws IOException                  when a reading exception occurs
     * @throws ParserConfigurationException if a DocumentBuilder can't be created
     */
    public static File exportManifest(final PackageInfo info) throws TransformerException, ZipException,
            IOException, ParserConfigurationException {

        // Read doc
        File destFile = new File(exportedManifestPath(info));
        Document doc = getManifest(info);

        StringBuilder builder = new StringBuilder();
        formatXml(doc, builder, 0);

        Log.i("Apex", builder.toString());

        TextFileUtils.writeTextFile(destFile.getPath(), builder.toString(),
                "UTF-8");

        return destFile;
    }

    private static void formatXml(final Node node, final StringBuilder builder,
                                  final int depth) {
        NodeList children = node.getChildNodes();
        NamedNodeMap attrs = node.getAttributes();

        int childrenCount, attrsCount, i;
        childrenCount = children.getLength();
        attrsCount = (attrs == null) ? 0 : attrs.getLength();

        formatIndent(builder, depth);
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
                for (i = 0; i < childrenCount; i++) {
                    formatXml(children.item(i), builder, depth);
                }
                break;
            case Node.ELEMENT_NODE:
                builder.append('<');
                builder.append(node.getNodeName());

                if (attrs != null) {
                    for (i = 0; i < attrsCount; i++) {
                        formatXml(attrs.item(i), builder, 0);
                    }
                }

                if (childrenCount == 0) {
                    builder.append("/>\n");
                } else {
                    builder.append(">\n");
                    for (i = 0; i < childrenCount; i++) {
                        formatXml(children.item(i), builder, depth + 1);
                    }

                    formatIndent(builder, depth);
                    builder.append("</");
                    builder.append(node.getNodeName());
                    builder.append(">\n");
                }
                break;
            case Node.TEXT_NODE:
                builder.append(node.getNodeValue());
                break;
            case Node.ATTRIBUTE_NODE:
                builder.append(' ');
                builder.append(node.getNodeName());
                builder.append("=\"");
                builder.append(node.getNodeValue());
                builder.append("\"");
                break;
            default:
                builder.append("<!-- Unknown node [#" + node.getNodeType() + " : "
                        + node.getNodeValue() + "] -->");
                break;
        }
    }

    private static void formatIndent(final StringBuilder builder,
                                     final int depth) {
        int i;
        for (i = 0; i < depth; i++) {
            builder.append("  ");
        }
    }

    /**
     * @param info    the package info
     * @return
     * @throws TransformerException         when an error occurs while converting DOM to file xml
     * @throws FileNotFoundException        when the destination file can't be written
     * @throws ZipException                 if a zip error occurs while reading the source file
     * @throws IOException                  when a reading exception occurs
     * @throws ParserConfigurationException if a DocumentBuilder can't be created
     */
    public static Document getManifest(final PackageInfo info) throws ZipException, IOException,
            ParserConfigurationException {
        String srcPackage = info.applicationInfo.publicSourceDir;
        File srcFile = new File(srcPackage);
        return parseManifestFile(srcFile);
    }

    /**
     * Extracts the manifest file from an apk to the desired file
     *
     * @param apkFile the apk file to read
     * @throws ZipException                 if a zip error occurs while reading the source file
     * @throws IOException                  when a reading exception occurs
     * @throws ParserConfigurationException if a DocumentBuilder can't be created
     */
    private static Document parseManifestFile(final File apkFile)
            throws ZipException, IOException, ParserConfigurationException {
        ZipFile zipFile;
        ZipEntry entry;
        Enumeration<? extends ZipEntry> entries;
        Document doc = null;

        zipFile = new ZipFile(apkFile);
        entries = zipFile.entries();
        CompressedXmlParser parser = new CompressedXmlParser();

        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            if (entry.getName().equals(MANIFEST)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                copy(zipFile.getInputStream(entry), baos, true);
                Log.d("manifest", baos.toString());

                doc = parser.parseDOM(zipFile.getInputStream(entry));
                Element manifest = doc.getDocumentElement();
                String packageName = findPackageName(manifest);
                List<Node> intentFilters = findIntentFilters(doc);

                RepositoryUtils.storeInDatabase(doc, packageName, intentFilters);
                break;
            }
        }

        zipFile.close();

        return doc;
    }

    private static String findPackageName(Element manifest) {
        return manifest.getAttributes().getNamedItem("package").getTextContent();
    }

    private static List<Node> findIntentFilters(Document doc) {
        NodeList intentFilters = doc.getElementsByTagName("intent-filter");
        List<Node> exportedIntentFilters = new ArrayList<>();
        for (int i = 0; i < intentFilters.getLength(); i++) {
            Node intentFilter = intentFilters.item(i);
            if (isExported(intentFilter)) {
                exportedIntentFilters.add(intentFilter);
            }
        }
        return exportedIntentFilters;
    }

    private static boolean isExported(Node intentFilter) {
        Node exportedAttribute = intentFilter.getParentNode().getAttributes().getNamedItemNS(ANDROID_NAME_SPACE, "exported");
        return exportedAttribute != null &&
                "true".equals(exportedAttribute.getNodeValue());
    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public static long copy(InputStream pInputStream,
                            OutputStream pOutputStream, boolean pClose)
            throws IOException {
        return copy(pInputStream, pOutputStream, pClose,
                new byte[DEFAULT_BUFFER_SIZE]);
    }

    public static long copy(InputStream pIn,
                            OutputStream pOut, boolean pClose,
                            byte[] pBuffer)
            throws IOException {
        OutputStream out = pOut;
        InputStream in = pIn;
        try {
            long total = 0;
            for (; ; ) {
                int res = in.read(pBuffer);
                if (res == -1) {
                    break;
                }
                if (res > 0) {
                    total += res;
                    if (out != null) {
                        out.write(pBuffer, 0, res);
                    }
                }
            }
            if (out != null) {
                if (pClose) {
                    out.close();
                } else {
                    out.flush();
                }
                out = null;
            }
            in.close();
            in = null;
            return total;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                    /* Ignore me */
                }
            }
            if (pClose && out != null) {
                try {
                    out.close();
                } catch (Throwable t) {
                    /* Ignore me */
                }
            }
        }
    }

    /**
     * @param pkg
     * @return the path to export the manifest
     */
    private static String exportedManifestPath(final PackageInfo pkg) {
        String path;
        path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getPath();
        path = path + File.separator + pkg.packageName.replaceAll("\\.", "_")
                + ".xml";
        return path;
    }

    public static class SparsePackageInfo {
        private String packageName;
        public String versionCode;

        public void setPackage(String packageName) {
            this.packageName = packageName;
        }

        public void setVersionCode(String versionCode) {
            this.versionCode = versionCode;
        }

        public String getPackageNameAsKey() {
            return packageName.replace('.', '_');
        }

        public String getPackageName() {
            return packageName;
        }
    }
}
