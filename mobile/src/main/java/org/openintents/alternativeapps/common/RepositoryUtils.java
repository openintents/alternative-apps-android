package org.openintents.alternativeapps.common;


import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.openintents.alternativeapps.common.ManifestUtils.SparsePackageInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;

import static org.openintents.alternativeapps.common.ManifestUtils.ANDROID_NAME_SPACE;

public class RepositoryUtils {

    private static final String TAG = "RepositoryUtils";

    /*
    public static MicrodataParserReport extractMicroData(String packageName) throws Exception {
        String value = "http://getschema.org/microdataextractor?url=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3D" + packageName + "&out=json";
        HTTPDocumentSource doc = new HTTPDocumentSource(DefaultHTTPClient.createInitializedHTTPClient(), value);
        InputStream documentInputInputStream = doc.openInputStream();
        TagSoupParser tagSoupParser = new TagSoupParser(documentInputInputStream, doc.getDocumentURI());
        Document document = tagSoupParser.getDOM();
        return MicrodataParser.getMicrodata(document);
    }

     private static void storeMicrodataForPackageInDatabase(String packageName, DatabaseReference ref) {
        try {
            MicrodataParserReport report = extractMicroData(packageName);
            Map<String, List<ItemProp>> mobileApp = report.getDetectedItemScopes()[0].getProperties();
            String imageUrl = mobileApp.get("http://schema.org/image").get(0).getValue().toString();
            String name = mobileApp.get("http://schema.org/name").get(0).getValue().toString();

            DatabaseReference microData = ref.child("metadata").child(packageName.replace('.', '_'));
            microData.child("package").setValue(packageName);
            microData.child("imageUrl").setValue(imageUrl);
            microData.child("name").setValue(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    */
    public static void storeInDatabase(Document doc, String packageName, List<Node> intentFilters) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReferenceFromUrl("https://oi-apps-fm.firebaseio.com/");

        // storeMicrodataForPackageInDatabase(packageName, ref);

        Log.d(TAG, "storing " + packageName);
        Element element = doc.getDocumentElement();
        DatabaseReference childRef = ref.child(element.getTagName()).child(packageName.replace('.', '_'));

        SparsePackageInfo info = new SparsePackageInfo();
        setValues(element, childRef, info);
        setChildren(element, childRef, true, info);

    }


    private static void setChildren(Element element, DatabaseReference childRef, boolean addActionsRecursively, SparsePackageInfo info) {
        for (int i = 0, size = element.getChildNodes().getLength(); i < size; i++) {
            Node node = element.getChildNodes().item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                DatabaseReference cRef;
                if (child.hasAttributeNS(ANDROID_NAME_SPACE, "name")) {
                    cRef = childRef.child(child.getTagName()).child(getAndroidNameAttributeForKey(child));
                } else {
                    cRef = childRef.child(child.getTagName());
                }
                setValues(child, cRef, info);
                setChildren(child, cRef, addActionsRecursively, info);

                if (child.getTagName().equals("action") && addActionsRecursively && ((Element) child.getParentNode().getParentNode()).getTagName().equals("activity")) {
                    DatabaseReference c2Ref = childRef.getRoot().child("action").child(getAndroidNameAttributeForKey(child));
                    setActionValues(child, c2Ref, info.getPackageNameAsKey(), info.getPackageName(), info.versionCode, info);
                }
            }
        }
    }

    private static void setActionValues(Element actionNode, DatabaseReference firebase, String packageNameAsKey, String packageName, String versionCode, SparsePackageInfo info) {
        Node activityNode = actionNode.getParentNode().getParentNode();
        Node nameAttribute = activityNode.getAttributes().getNamedItemNS(ANDROID_NAME_SPACE, "name");
        String activityName;
        if (nameAttribute == null) {
            activityName = "unknown";
        } else {
            activityName = nameAttribute.getNodeValue();
        }
        Node exported = activityNode.getAttributes().getNamedItemNS(ANDROID_NAME_SPACE, "exported");
        if (versionCode != null && (exported == null || "true".equals(exported.getNodeValue()))) {
            DatabaseReference packageRef = firebase.child("packages").child(packageNameAsKey);
            packageRef.child(versionCode).setValue(activityName);
            packageRef.child("packageName").setValue(packageName);
            setChildren((Element) actionNode.getParentNode(), packageRef.child("intent-filter"), false, info);
            Log.d(TAG, "setActionValues: " + packageNameAsKey);
        }
    }

    @NonNull
    private static String getAndroidNameAttributeForKey(Element e) {
        String nameAttribute = e.getAttributeNS("http://schemas.android.com/apk/res/android", "name");
        return sanitizeForFirebasePath(nameAttribute);
    }

    @NonNull
    public static String sanitizeForFirebasePath(String nameAttribute) {
        return nameAttribute.replace('.', '_').replace('$', '_');
    }

    private static void setValues(Element element, DatabaseReference childRef, SparsePackageInfo packageInfo) {
        boolean updatePackageInfo = "manifest".equals(element.getTagName());
        for (int i = 0, size = element.getAttributes().getLength(); i < size; i++) {
            final Node attr = element.getAttributes().item(i);
            childRef.child(attr.getNodeName()).setValue(attr.getNodeValue()).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "Couldn't set " + attr.getNodeName() + " " + attr.getNodeValue(), e);
                }
            });
            if (updatePackageInfo) {
                if ("package".equals(attr.getNodeName())) {
                    packageInfo.setPackage(attr.getNodeValue());
                }

                if ("versionCode".equals(attr.getLocalName()) && ANDROID_NAME_SPACE.equals(attr.getNamespaceURI())) {
                    packageInfo.setVersionCode(attr.getNodeValue());
                }
            }
        }

        NodeList applications = element.getElementsByTagName("application");
        if (applications.getLength() > 0) {
            Element app = (Element) applications.item(0);
            String icon = app.getAttributeNS(ANDROID_NAME_SPACE, "icon");
        }
    }

}
