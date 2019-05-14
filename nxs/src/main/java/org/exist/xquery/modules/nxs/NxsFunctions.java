package org.exist.xquery.modules.nxs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.QName;
import org.exist.dom.memtree.DocumentBuilderReceiver;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.dom.persistent.NodeProxy;
import org.exist.numbering.NodeId;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.ErrorCodes.ErrorCode;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.*;
import org.exist.xslt.Stylesheet;
import org.exist.xslt.TemplatesFactory;
import org.exist.xslt.TransformerFactoryAllocator;
import org.exist.xslt.XSLTErrorsListener;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import net.sf.saxon.Transform;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.dom.DocumentBuilderImpl;
import net.sf.saxon.jaxp.TransformerImpl;
import net.sf.saxon.lib.StandardErrorListener;
import net.sf.saxon.lib.StandardLogger;
import net.sf.saxon.s9api.BuildingStreamWriter;
import net.sf.saxon.s9api.DOMDestination;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SAXDestination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import java.io.StringReader;
import java.util.Optional;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.modules.nxs.Nxs.functionSignature;

/**
 * Some very simple XQuery example functions implemented in Java.
 */
public class NxsFunctions extends BasicFunction {

	private static final String FS_TRANSFORM_NAME = "transform";
	static final FunctionSignature FS_TRANSFORM = functionSignature(FS_TRANSFORM_NAME,
			"Use Saxon to transform XML with XSLT", returns(Type.DOCUMENT),
			param("xml", Type.NODE, Cardinality.ZERO_OR_MORE, "The XML document"),
			param("xsl", Type.ITEM, Cardinality.ONE, "The XSL document"),
			param("parameters", Type.NODE, Cardinality.ZERO_OR_ONE, "Parameters"));

	private static final String FS_TRANSFORM_ATTRIBS_NAME = "transform-attribs";
	static final FunctionSignature FS_TRANSFORM_ATTRIBS = functionSignature(FS_TRANSFORM_ATTRIBS_NAME,
			"Use Saxon to transform XML with XSLT", returns(Type.DOCUMENT),
			param("xml", Type.NODE, Cardinality.ZERO_OR_MORE, "The XML document"),
			param("xsl", Type.ITEM, Cardinality.ONE, "The XSL document"),
			param("parameters", Type.NODE, Cardinality.ZERO_OR_ONE, "Parameters"),
			param("attributes", Type.NODE, Cardinality.ZERO_OR_ONE, "Attributes"),
			param("serialization-options", Type.STRING, Cardinality.ZERO_OR_ONE, "Serialization options"));

	public NxsFunctions(final XQueryContext context, final FunctionSignature signature) {
		super(context, signature);
	}

	@Override
	public DocumentImpl eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
		Logger logger = LogManager.getLogger();

		switch (getSignature().getArgumentCount()) {
		case 5:
			final Optional<NodeValue> xmla = args[0].isEmpty() ? Optional.empty()
					: Optional.of((NodeValue) args[0].itemAt(0));
			final Item xsla = args[1].itemAt(0);
			final Optional<NodeValue> parama = args[2].isEmpty() ? Optional.empty()
					: Optional.of((NodeValue) args[2].itemAt(0));
			final Optional<NodeValue> attrib = args[3].isEmpty() ? Optional.empty()
					: Optional.of((NodeValue) args[3].itemAt(0));
			final Optional<StringValue> sero = args[4].isEmpty() ? Optional.empty()
					: Optional.of((StringValue) args[4].itemAt(0));
			try {
				return transform(xmla, xsla, parama, attrib, sero);
			} catch (SaxonApiException e) {
				logger.debug(e.getLocalizedMessage());
				e.printStackTrace();
			}
		case 3:
			final Optional<NodeValue> xml = args[0].isEmpty() ? Optional.empty()
					: Optional.of((NodeValue) args[0].itemAt(0));
			final Item xsl = args[1].itemAt(0);
			final Optional<NodeValue> params = args[2].isEmpty() ? Optional.empty()
					: Optional.of((NodeValue) args[2].itemAt(0));
			try {
				return transform(xml, xsl, params);
			} catch (SaxonApiException e) {
				logger.debug(e.getLocalizedMessage());
				e.printStackTrace();
			}
		default:
			throw new XPathException(this, new ErrorCode("nxs0000", "function not found"),
					"No function: " + getName() + "#" + getSignature().getArgumentCount());
		}
	}

	private DocumentImpl transform(Optional<NodeValue> xml, Item xsl, Optional<NodeValue> params,
			Optional<NodeValue> attrib, Optional<StringValue> sero) throws SaxonApiException {

		final Properties attributes = new Properties();
		final Properties stylesheetParams = new Properties();

		final Processor processor = new Processor(false);

		final XsltCompiler compiler = processor.newXsltCompiler();

		final StandardErrorListener errorListener = (StandardErrorListener) compiler.getErrorListener();
		errorListener.setLogger(new NxsLogger());

		XdmNode xslt = processor.newDocumentBuilder().wrap(xsl);
		XsltExecutable xx = compiler.compile(xslt.asSource());

		XdmNode source = processor.newDocumentBuilder().wrap(xml.get().getNode());
//		XdmNode source = processor.newDocumentBuilder().build(new StreamSource(new StringReader(xml.get().toString())));

		XsltTransformer xt = xx.load();

		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		dfactory.setNamespaceAware(true);
		Document dom;
		
		try {
			dom = dfactory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new SaxonApiException(e);
		}

		DOMDestination destination = new DOMDestination(dom);
		
		xt.setInitialContextNode(source);
		xt.setDestination(destination);
		xt.transform();

		return null;
	}

	private DocumentImpl transform(Optional<NodeValue> xml, Item xsl, Optional<NodeValue> params)
			throws SaxonApiException {
		return transform(xml, xsl, params, Optional.empty(), Optional.empty());
	}

	/**
	 * Creates an XML document like <hello>name</hello>.
	 *
	 * @param name An optional name, if empty then "stranger" is used.
	 *
	 * @return An XML document
	 */
	private DocumentImpl sayHello(final Optional<StringValue> name) throws XPathException {
		try {
			final MemTreeBuilder builder = new MemTreeBuilder(context);
			builder.startDocument();
			builder.startElement(new QName("hello"), null);
			builder.characters(name.map(StringValue::toString).orElse("stranger"));
			builder.endElement();
			builder.endDocument();

			return builder.getDocument();
		} catch (final QName.IllegalQNameException e) {
			throw new XPathException(this, e.getMessage(), e);
		}
	}

	/**
	 * Adds two numbers together.
	 *
	 * @param a The first number
	 * @param b The second number
	 *
	 * @return The result;
	 */
	private IntegerValue add(final IntegerValue a, final IntegerValue b) throws XPathException {
		final int result = a.getInt() + b.getInt();
		return new IntegerValue(result);
	}

}
