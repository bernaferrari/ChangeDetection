package com.bernaferrari.changedetection.util.jsoup.helper;

import com.bernaferrari.changedetection.util.jsoup.nodes.Attribute;
import com.bernaferrari.changedetection.util.jsoup.nodes.Attributes;
import com.bernaferrari.changedetection.util.jsoup.select.NodeTraversor;
import com.bernaferrari.changedetection.util.jsoup.select.NodeVisitor;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Helper class to transform a {@link com.bernaferrari.changedetection.util.jsoup.nodes.Document} to a {@link org.w3c.dom.Document org.w3c.dom.Document},
 * for integration with toolsets that use the W3C DOM.
 */
public class W3CDom {
    protected DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    /**
     * Convert a jsoup Document to a W3C Document.
     *
     * @param in jsoup doc
     * @return w3c doc
     */
    public Document fromJsoup(com.bernaferrari.changedetection.util.jsoup.nodes.Document in) {
        Validate.notNull(in);
        DocumentBuilder builder;
        try {
            //set the factory to be namespace-aware
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
            Document out = builder.newDocument();
            convert(in, out);
            return out;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts a jsoup document into the provided W3C Document. If required, you can set options on the output document
     * before converting.
     *
     * @param in  jsoup doc
     * @param out w3c doc
     * @see com.bernaferrari.changedetection.util.jsoup.helper.W3CDom#fromJsoup(com.bernaferrari.changedetection.util.jsoup.nodes.Document)
     */
    public void convert(com.bernaferrari.changedetection.util.jsoup.nodes.Document in, Document out) {
        if (!StringUtil.isBlank(in.location()))
            out.setDocumentURI(in.location());

        com.bernaferrari.changedetection.util.jsoup.nodes.Element rootEl = in.child(0); // skip the #root node
        NodeTraversor.traverse(new W3CBuilder(out), rootEl);
    }

    /**
     * Serialize a W3C document to a String.
     *
     * @param doc Document
     * @return Document as string
     */
    public String asString(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Implements the conversion by walking the input.
     */
    protected static class W3CBuilder implements NodeVisitor {
        private static final String xmlnsKey = "xmlns";
        private static final String xmlnsPrefix = "xmlns:";

        private final Document doc;
        private final Stack<HashMap<String, String>> namespacesStack = new Stack<>(); // stack of namespaces, prefix => urn
        private Element dest;

        public W3CBuilder(Document doc) {
            this.doc = doc;
            this.namespacesStack.push(new HashMap<String, String>());
        }

        public void head(com.bernaferrari.changedetection.util.jsoup.nodes.Node source, int depth) {
            namespacesStack.push(new HashMap<>(namespacesStack.peek())); // inherit from above on the stack
            if (source instanceof com.bernaferrari.changedetection.util.jsoup.nodes.Element) {
                com.bernaferrari.changedetection.util.jsoup.nodes.Element sourceEl = (com.bernaferrari.changedetection.util.jsoup.nodes.Element) source;

                String prefix = updateNamespaces(sourceEl);
                String namespace = namespacesStack.peek().get(prefix);

                Element el = doc.createElementNS(namespace, sourceEl.tagName());
                copyAttributes(sourceEl, el);
                if (dest == null) { // sets up the root
                    doc.appendChild(el);
                } else {
                    dest.appendChild(el);
                }
                dest = el; // descend
            } else if (source instanceof com.bernaferrari.changedetection.util.jsoup.nodes.TextNode) {
                com.bernaferrari.changedetection.util.jsoup.nodes.TextNode sourceText = (com.bernaferrari.changedetection.util.jsoup.nodes.TextNode) source;
                Text text = doc.createTextNode(sourceText.getWholeText());
                dest.appendChild(text);
            } else if (source instanceof com.bernaferrari.changedetection.util.jsoup.nodes.Comment) {
                com.bernaferrari.changedetection.util.jsoup.nodes.Comment sourceComment = (com.bernaferrari.changedetection.util.jsoup.nodes.Comment) source;
                Comment comment = doc.createComment(sourceComment.getData());
                dest.appendChild(comment);
            } else if (source instanceof com.bernaferrari.changedetection.util.jsoup.nodes.DataNode) {
                com.bernaferrari.changedetection.util.jsoup.nodes.DataNode sourceData = (com.bernaferrari.changedetection.util.jsoup.nodes.DataNode) source;
                Text node = doc.createTextNode(sourceData.getWholeData());
                dest.appendChild(node);
            } else {
                // unhandled
            }
        }

        public void tail(com.bernaferrari.changedetection.util.jsoup.nodes.Node source, int depth) {
            if (source instanceof com.bernaferrari.changedetection.util.jsoup.nodes.Element && dest.getParentNode() instanceof Element) {
                dest = (Element) dest.getParentNode(); // undescend. cromulent.
            }
            namespacesStack.pop();
        }

        private void copyAttributes(com.bernaferrari.changedetection.util.jsoup.nodes.Node source, Element el) {
            for (Attribute attribute : source.attributes()) {
                // valid xml attribute names are: ^[a-zA-Z_:][-a-zA-Z0-9_:.]
                String key = attribute.getKey().replaceAll("[^-a-zA-Z0-9_:.]", "");
                if (key.matches("[a-zA-Z_:][-a-zA-Z0-9_:.]*"))
                    el.setAttribute(key, attribute.getValue());
            }
        }

        /**
         * Finds any namespaces defined in this element. Returns any tag prefix.
         */
        private String updateNamespaces(com.bernaferrari.changedetection.util.jsoup.nodes.Element el) {
            // scan the element for namespace declarations
            // like: xmlns="blah" or xmlns:prefix="blah"
            Attributes attributes = el.attributes();
            for (Attribute attr : attributes) {
                String key = attr.getKey();
                String prefix;
                if (key.equals(xmlnsKey)) {
                    prefix = "";
                } else if (key.startsWith(xmlnsPrefix)) {
                    prefix = key.substring(xmlnsPrefix.length());
                } else {
                    continue;
                }
                namespacesStack.peek().put(prefix, attr.getValue());
            }

            // get the element prefix if any
            int pos = el.tagName().indexOf(":");
            return pos > 0 ? el.tagName().substring(0, pos) : "";
        }
    }
}
