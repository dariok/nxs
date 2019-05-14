package org.exist.xquery.modules.nxs;


import org.exist.EXistException;
import org.exist.security.PermissionDeniedException;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.test.ExistEmbeddedServer;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQuery;
import org.exist.xquery.value.IntegerValue;
import org.exist.xquery.value.Sequence;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import javax.xml.transform.Source;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NxsTest {
	final String xsl =
			"<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' exclude-result-prefixes='#all' version='3.0'>\n" + 
			"  <xsl:template match='/'>\n" + 
			"    <xsl:variable name='prop'>xsl:product-version</xsl:variable>" +
			"    <xsl:sequence select='system-property($prop)' />\n" + 
			"  </xsl:template>\n" + 
			"</xsl:stylesheet>";
    @ClassRule
    public static ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(false, true);

//    @Test
//    public void sayHello() throws XPathException, PermissionDeniedException, EXistException {
//        final String query =
//                "declare namespace nxs = \"https://github.com/dariok/nxs\";\n" +
//                        "nxs:say-hello('Adam')";
//        final Sequence result = executeQuery(query);
//
//        assertTrue(result.hasOne());
//
//        final Source inExpected = Input.fromString("<hello>Adam</hello>").build();
//        final Source inActual = Input.fromDocument((Document) result.itemAt(0)).build();
//
//        final Diff diff = DiffBuilder.compare(inExpected)
//                .withTest(inActual)
//                .checkForSimilar()
//                .build();
//
//        assertFalse(diff.toString(), diff.hasDifferences());
//    }
//
//    @Test
//    public void sayHello_noName() throws XPathException, PermissionDeniedException, EXistException {
//        final String query =
//                "declare namespace nxs = \"https://github.com/dariok/nxs\";\n" +
//                        "nxs:say-hello(())";
//        final Sequence result = executeQuery(query);
//
//        assertTrue(result.hasOne());
//
//        final Source inExpected = Input.fromString("<hello>stranger</hello>").build();
//        final Source inActual = Input.fromDocument((Document) result.itemAt(0)).build();
//
//        final Diff diff = DiffBuilder.compare(inExpected)
//                .withTest(inActual)
//                .checkForSimilar()
//                .build();
//
//        assertFalse(diff.toString(), diff.hasDifferences());
//    }
//
//    @Test
//    public void add() throws XPathException, PermissionDeniedException, EXistException {
//        final String query =
//                "declare namespace nxs = \"https://github.com/dariok/nxs\";\n" +
//                        "nxs:add(123, 456)";
//        final Sequence result = executeQuery(query);
//
//        assertTrue(result.hasOne());
//
//        assertEquals(579, ((IntegerValue)result.itemAt(0)).getInt());
//    }


    private Sequence executeQuery(final String xquery) throws EXistException, PermissionDeniedException, XPathException {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final XQuery xqueryService = pool.getXQueryService();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().getSystemSubject()))) {
            return xqueryService.execute(broker, xquery, null);
        }
    }
}
