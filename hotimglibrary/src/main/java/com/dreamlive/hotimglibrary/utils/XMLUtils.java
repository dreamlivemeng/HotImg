package com.dreamlive.hotimglibrary.utils;

import android.content.Context;

import com.dreamlive.hotimglibrary.entity.HotArea;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * xml解析帮助类
 * Created by dreamlivemeng on 2016/6/7.
 */
public class XMLUtils {

    private final static String TAG = XMLUtils.class.getName();

    private final Context mContext;

    private static volatile XMLUtils mXmlUtils;


    private XMLUtils(Context context) {
        mContext = context;
    }

    public static XMLUtils getInstance(Context context) {
        if (mXmlUtils == null) {
            synchronized (XMLUtils.class) {
                if (mXmlUtils == null) {
                    mXmlUtils = new XMLUtils(context);
                }
            }
        }
        return mXmlUtils;
    }

    /**
     * 从文件流里读取xml文档
     *
     * @param inputStream
     * @return
     */
    public HotArea readDoc(InputStream inputStream) {
        Document doc = null;
        HotArea root = new HotArea();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(inputStream);
            NodeList nodeList = doc.getChildNodes();
            if (nodeList != null && nodeList.getLength() == 1) {
                Node node = nodeList.item(0);
                root = readToPlace(null, node);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        }
        return root;
    }

    /**
     * 返回dom树
     *
     * @param fileName
     * @return
     */
    public HotArea readDoc(String fileName) {
        InputStream inputStream = null;
        HotArea root = null;
        try {
            inputStream = new FileInputStream(fileName);
            root = readDoc(inputStream);
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        } finally {
            FileUtils.closeInputStream(inputStream);
        }
        return root;
    }

    /**
     * 获取特定节点下属性的值，如有多个节点取第一个
     *
     * @param fileName
     * @param NodeName
     * @param attr
     * @return
     */
    public String readAttr(String fileName, String NodeName, String attr) {
        String nodeValue = "";
        Document doc = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new File(fileName));
            NodeList nodeList = doc.getElementsByTagName(NodeName);
            if (nodeList != null && nodeList.getLength() == 1) {
                Node node = nodeList.item(0);
                NamedNodeMap nodeMap = node.getAttributes();
                Node n = nodeMap.getNamedItem(attr);
                nodeValue = n.getNodeValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nodeValue;
    }

    public HotArea readToPlace(HotArea placeNode, Node node) {
        HotArea hNode = placeNode;
        if (hNode == null) {
            hNode = new HotArea();
        }
        Element element = (Element) node;
        NamedNodeMap attrs = element.getAttributes();
        int len = attrs.getLength();
        for (int i = 0; i < len; ++i) {
            Node n = attrs.item(i);
            String name = n.getNodeName();
            String value = n.getNodeValue();
            if ("areaId".equals(name)) {
                hNode.setAreaId(value);
            } else if ("areaTitle".equals(name)) {
                hNode.setAreaTitle(value);
            } else if ("pts".equals(name)) {
                hNode.setPts(value, ",");
            } else if ("desc".equals(name)) {
                hNode.setDesc(value);
            }
        }

        NodeList nodeList = element.getChildNodes();
        int jLen = nodeList.getLength();
        for (int j = 0; j < jLen; ++j) {
            Node n = nodeList.item(j);
            if (n instanceof Element) {
                HotArea h = new HotArea();
                hNode.getAreas().add(h);
                readToPlace(h, n);
            }
        }

        return hNode;
    }

    static class Parent {

        protected String title;

        protected String desc;

        protected String code;

        protected List<Child> places = new ArrayList<Child>();

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public List<Child> getPlaces() {
            return places;
        }

        public void setPlaces(List<Child> places) {
            this.places = places;
        }
    }

    public static class Root extends Parent {

        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class Child extends Parent {

        private String pointStr;

        private final List<Integer> points = new ArrayList<Integer>();

        public String getPointStr() {
            return pointStr;
        }

        public void setPointStr(String pointStr) {
            this.pointStr = pointStr;
        }

        public List<Integer> getPoints() {
            return points;
        }

        public void setPoints(String pointStr) {
            if (null != pointStr && !"".equals(pointStr)) {
                String[] points = pointStr.split(",");
                int len = points.length;
                for (int i = 0; i < len; ++i) {
                    this.points.add(Integer.parseInt(points[i]));
                }
            }
        }
    }
}
