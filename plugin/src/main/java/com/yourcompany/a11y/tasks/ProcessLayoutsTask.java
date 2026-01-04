package com.yourcompany.a11y.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle task for processing layout XML files to inject accessibility attributes.
 *
 * This task:
 * 1. Scans src/main/res/layout* directories for XML files
 * 2. Parses XML and finds elements with app:a11yChartId attribute
 * 3. Injects standard Android accessibility attributes
 * 4. Removes custom attributes (app:a11yChartId, etc.)
 * 5. Outputs processed XML to build/generated/res/a11y-layouts/{variant}/
 */
public abstract class ProcessLayoutsTask extends DefaultTask {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String APP_NS = "http://schemas.android.com/apk/res-auto";
    private static final String A11Y_CHART_ID = "a11yChartId";
    private static final String A11Y_DESC_TYPE = "a11yDescType";
    private static final String A11Y_FOCUSABLE = "a11yFocusable";
    private static final String A11Y_ENABLE_NAVIGATION = "a11yEnableNavigation";

    /** Tag prefix for runtime initialization */
    public static final String A11Y_TAG_PREFIX = "a11y:";

    @InputDirectory
    public abstract DirectoryProperty getLayoutDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void processLayouts() {
        File layoutDir = getLayoutDir().get().getAsFile();
        if (!layoutDir.exists()) {
            getLogger().warn("Layout directory not found: " + layoutDir.getAbsolutePath());
            return;
        }

        File outputDir = getOutputDir().get().getAsFile();
        deleteDirectory(outputDir);
        outputDir.mkdirs();

        // Find all layout directories (layout, layout-land, etc.)
        File resDir = layoutDir.getParentFile();
        File[] layoutDirs = resDir.listFiles((dir, name) -> name.startsWith("layout"));

        if (layoutDirs == null || layoutDirs.length == 0) {
            getLogger().info("No layout directories found");
            return;
        }

        int processedCount = 0;
        for (File dir : layoutDirs) {
            File[] xmlFiles = dir.listFiles((d, name) -> name.endsWith(".xml"));
            if (xmlFiles == null) continue;

            for (File xmlFile : xmlFiles) {
                if (processLayoutFile(xmlFile, dir.getName(), outputDir)) {
                    processedCount++;
                }
            }
        }

        getLogger().lifecycle("Processed {} layout files with a11y attributes", processedCount);
    }

    private boolean processLayoutFile(File xmlFile, String layoutDirName, File outputDir) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            List<Element> elementsWithA11y = findElementsWithA11yAttribute(doc.getDocumentElement());

            if (elementsWithA11y.isEmpty()) {
                return false;
            }

            // Process each element
            for (Element element : elementsWithA11y) {
                processElement(element);
            }

            // Write output file
            File outputLayoutDir = new File(outputDir, layoutDirName);
            outputLayoutDir.mkdirs();
            File outputFile = new File(outputLayoutDir, xmlFile.getName());

            writeXmlFile(doc, outputFile);
            getLogger().info("Processed: {}/{}", layoutDirName, xmlFile.getName());

            return true;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            getLogger().error("Failed to process {}: {}", xmlFile.getName(), e.getMessage());
            return false;
        }
    }

    private List<Element> findElementsWithA11yAttribute(Element root) {
        List<Element> result = new ArrayList<>();
        findElementsRecursive(root, result);
        return result;
    }

    private void findElementsRecursive(Element element, List<Element> result) {
        // Check for a11yChartId attribute
        if (hasA11yAttribute(element)) {
            result.add(element);
        }

        // Recurse into children
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                findElementsRecursive((Element) child, result);
            }
        }
    }

    private boolean hasA11yAttribute(Element element) {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String localName = attr.getLocalName();
            if (A11Y_CHART_ID.equals(localName)) {
                return true;
            }
        }
        return false;
    }

    private void processElement(Element element) {
        // Get chart ID and description type
        String chartId = getAttributeValue(element, A11Y_CHART_ID);
        String enableNavigation = getAttributeValue(element, A11Y_ENABLE_NAVIGATION);

        if (chartId == null || chartId.isEmpty()) {
            return;
        }

        // Convert chartId to resource name
        String resourceName = chartIdToResourceName(chartId);

        // Set contentDescription
        String contentDescRef = "@string/a11y_chart_" + resourceName;
        element.setAttributeNS(ANDROID_NS, "android:contentDescription", contentDescRef);

        // Set focusable
        element.setAttributeNS(ANDROID_NS, "android:focusable", "true");

        // Set importantForAccessibility
        element.setAttributeNS(ANDROID_NS, "android:importantForAccessibility", "yes");

        // If navigation is enabled, save metadata in tag for runtime initialization
        // Tag format: "a11y:{chartId}:{enableNavigation}"
        boolean navEnabled = "true".equalsIgnoreCase(enableNavigation);
        if (navEnabled) {
            String tagValue = A11Y_TAG_PREFIX + chartId + ":true";
            element.setAttributeNS(ANDROID_NS, "android:tag", tagValue);
        }

        // Remove custom attributes
        removeAppAttribute(element, A11Y_CHART_ID);
        removeAppAttribute(element, A11Y_DESC_TYPE);
        removeAppAttribute(element, A11Y_FOCUSABLE);
        removeAppAttribute(element, A11Y_ENABLE_NAVIGATION);
    }

    private String getAttributeValue(Element element, String attrName) {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (attrName.equals(attr.getLocalName())) {
                return attr.getNodeValue();
            }
        }
        return null;
    }

    private void removeAppAttribute(Element element, String attrName) {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = attrs.getLength() - 1; i >= 0; i--) {
            Node attr = attrs.item(i);
            if (attrName.equals(attr.getLocalName())) {
                element.removeAttributeNode((org.w3c.dom.Attr) attr);
                break;
            }
        }
    }

    /**
     * Convert chart ID to resource name.
     * - Convert to lowercase
     * - Replace non-alphanumeric characters with underscores
     * - Merge consecutive underscores
     * - Remove leading and trailing underscores
     */
    private String chartIdToResourceName(String chartId) {
        if (chartId == null || chartId.isEmpty()) {
            return "unknown";
        }
        String result = chartId.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return result.isEmpty() ? "unknown" : result;
    }

    private void writeXmlFile(Document doc, File outputFile) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(outputFile);
            transformer.transform(source, result);
        } catch (Exception e) {
            getLogger().error("Failed to write XML file: {}", e.getMessage());
        }
    }

    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
    }
}
