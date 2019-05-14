package org.exist.xquery.modules.nxs;

import org.exist.dom.QName;
import org.exist.dom.memtree.DocumentImpl;
import org.exist.dom.memtree.MemTreeBuilder;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

import java.util.Optional;

import static org.exist.xquery.FunctionDSL.*;
import static org.exist.xquery.modules.nxs.Nxs.functionSignature;

/**
 * Some very simple XQuery example functions implemented
 * in Java.
 */
public class NxsFunctions extends BasicFunction {

    private static final String FS_TRANSFORM_NAME = "transform";
    static final FunctionSignature FS_TRANSFORM = functionSignature(
        FS_TRANSFORM_NAME,
        "Use Saxon to transform XML with XSLT",
        returns(Type.DOCUMENT),
        param("xml", Type.NODE, Cardinality.ZERO_OR_MORE, "The XML document"),
        param("xsl", Type.ITEM, Cardinality.ONE, "The XSL document"),
        param("parameters", Type.NODE, Cardinality.ZERO_OR_ONE, "Parameters")
    );
    
    private static final String FS_TRANSFORM_ATTRIBS_NAME = "transform-attribs";
    static final FunctionSignature FS_TRANSFORM_ATTRIBS = functionSignature(
        FS_TRANSFORM_ATTRIBS_NAME,
        "Use Saxon to transform XML with XSLT",
        returns(Type.DOCUMENT),
        param("xml", Type.NODE, Cardinality.ZERO_OR_MORE,"The XML document"),
        param("xsl", Type.ITEM, Cardinality.ONE, "The XSL document"),
        param("parameters", Type.NODE, Cardinality.ZERO_OR_ONE, "Parameters"),
        param("attributes", Type.NODE, Cardinality.ZERO_OR_ONE, "Attributes"),
        param("serialization-options", Type.STRING, Cardinality.ZERO_OR_ONE, "Serialization options")
    );

    public NxsFunctions(final XQueryContext context, final FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(final Sequence[] args, final Sequence contextSequence) throws XPathException {
        switch (getName().getLocalPart()) {

            case FS_TRANSFORM_NAME:
                return sayHello(Optional.of(new StringValue("World")));

            case FS_SAY_HELLO_NAME:
                final Optional<StringValue> name = args[0].isEmpty() ? Optional.empty() : Optional.of((StringValue)args[0].itemAt(0));
                return sayHello(name);

            case FS_ADD_NAME:
                final IntegerValue a = (IntegerValue) args[0].itemAt(0);
                final IntegerValue b = (IntegerValue) args[1].itemAt(0);
                return add(a, b);

            default:
                throw new XPathException(this, "No function: " + getName() + "#" + getSignature().getArgumentCount());
        }
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
