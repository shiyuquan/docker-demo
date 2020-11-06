package persion.bleg.dockerdemo.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import persion.bleg.dockerdemo.base.BlegException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;

public class XmlUtil {

    public static String toXML(Object obj) {
        try {
            JAXBContext context = JAXBContext.newInstance(obj.getClass());
            Marshaller marshaller = context.createMarshaller();
            // //编码格式
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // 是否格式化生成的xml串
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            // 是否省略xm头声明信息
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

            StringWriter writer = new StringWriter();
            writer.write("<?xml version=\'1.0\' encoding=\'" + "UTF-8" + "\'?>\n");
            marshaller.marshal(obj, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new BlegException("xml 解析错误");
        }
    }

    /**
     * 解析xml（忽略命名空间）
     *
     * @param cla
     * @param content
     */
    public static <T> T fromXML(String content, Class<T> cla) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(cla);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(content);

            SAXParserFactory sax = SAXParserFactory.newInstance();
            sax.setNamespaceAware(false);
            XMLReader xmlReader = sax.newSAXParser().getXMLReader();
            Source source = new SAXSource(xmlReader, new InputSource(reader));
            return (T) unmarshaller.unmarshal(source);
        } catch (Exception e) {
            throw new BlegException("xml 解析错误");
        }
    }

    /**
     * 解析XML字符串 根据tagName 返回textContent
     *
     * @param xmlStr
     * @param tagName
     */
    public static String getTextContentByTagName(String xmlStr, String tagName) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new ByteArrayInputStream(xmlStr.getBytes("UTF-8")));
            NodeList nodeLists = document.getDocumentElement().getElementsByTagName(tagName);
            return nodeLists.item(0).getTextContent();
        } catch (Exception e) {
            throw new BlegException("xml 解析错误");
        }
    }

}