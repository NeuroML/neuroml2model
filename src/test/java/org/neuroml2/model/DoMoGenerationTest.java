package org.neuroml2.model;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.junit.Before;
import org.junit.Test;
import org.lemsml.model.compiler.LEMSCompilerFrontend;
import org.lemsml.model.compiler.semantic.LEMSSemanticAnalyser;
import org.lemsml.model.extended.Component;
import org.lemsml.model.extended.Lems;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class DoMoGenerationTest {

	private JAXBContext jaxbContext;
	private Neuroml2 hh;
	private Lems domainDefs;

	@Before
	public void setUp() throws Throwable {
		domainDefs = new LEMSCompilerFrontend(
				getLocalFile("/lems/NeuroML2CoreTypes.xml"))
				.generateLEMSDocument();

		File model = getLocalFile("/NML2_SingleCompHHCell.nml");

		jaxbContext = JAXBContext.newInstance("org.neuroml2.model");
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		hh = (Neuroml2) jaxbUnmarshaller.unmarshal(model);
		// TODO: ideal way of doing that?
		hh.getComponentTypes().addAll(domainDefs.getComponentTypes());
		hh.getUnits().addAll(domainDefs.getUnits());
		hh.getConstants().addAll(domainDefs.getConstants());
		hh.getDimensions().addAll(domainDefs.getDimensions());
		new LEMSSemanticAnalyser(hh).analyse();
	}

	@Test
	public void testGeneration() {

		assertEquals(172, domainDefs.getComponentTypes().size());
	}

	// @Test
	// public void testTypes() throws LEMSCompilerException {
	// assertEquals(1, fooModel.getFoos().size());
	// assertEquals(0, fooModel.getBars().size());
	// assertEquals(6, fooModel.getAllOfType(Bar.class).size());
	// assertEquals(5, fooModel.getAllOfType(Baz.class).size());
	// assertEquals(10, fooModel.getAllOfType(Base.class).size());
	//
	// Foo foo0 = (Foo) fooModel.getComponentById("foo0");
	//
	// assertTrue(fooModel.getAllOfType(Foo.class).contains(foo0));
	// assertEquals(2, foo0.getFooBazs().size());
	// assertEquals("10", foo0.getFooBar().getParameterValue("pBar"));
	// }
	//
	// @Test
	// public void testEvaluation() throws LEMSCompilerException {
	//
	// Component foo0 = fooModel.getComponentById("foo0");
	// Component barInFoo0 = fooModel.getComponentById("fooBar");
	//
	// Double pBar =
	// Double.valueOf(fooModel.getFoos().get(0).getFooBar().getPBar());
	// assertEquals(pBar, foo0
	// .getChildren()
	// .get(0)
	// .getScope()
	// .evaluate("pBar").getValue().doubleValue(), 1e-12);
	// assertEquals(0.1, barInFoo0
	// .getScope()
	// .evaluate("dpBar").getValue().doubleValue(), 1e-12);
	//
	// //testing synch
	// //changing par via lems api
	// foo0.withParameterValue("pFoo", "3");
	// assertEquals(0.3, barInFoo0
	// .getScope()
	// .evaluate("dpBar").getValue().doubleValue(), 1e-12);
	//
	// //changing par via domain api
	// ((Foo) foo0).setPFoo("4");
	//
	// assertEquals("4", ((Foo) foo0).getPFoo());
	// assertEquals(0.4, barInFoo0
	// .getScope()
	// .evaluate("dpBar").getValue().doubleValue(), 1e-12);
	// }
	//
	@Test
	public void testMarshalling() throws JAXBException, PropertyException,
			IOException {

		File tmpFile = File.createTempFile("test", ".xml");
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		hh.getComponentTypes().clear();
		eraseTypes(hh.getComponents()); // TODO: extremely ugly hack
		eraseUnits(hh); // TODO: extremely ugly hack
		eraseDimensions(hh); // TODO: extremely ugly hack
		eraseDeReferences(hh);

		marshaller.marshal(hh, tmpFile);
		System.out.println(Files.toString(tmpFile, Charsets.UTF_8));
	}

	private void eraseDeReferences(Neuroml2 hh2) {
		// TODO Auto-generated method stub

	}

	// TODO: argh! @XmlTransient in ext.Comp isn't overriding type from
	//       (un-ext)Comp
	void eraseTypes(List<Component> list) {
		for (Component comp : list) {
			eraseTypes(comp.getComponent());
			comp.withType(null);
		}
	}
	void eraseUnits(Neuroml2 model) {
		model.getUnits().clear();
	}

	void eraseDimensions(Neuroml2 model) {
        model.getDimensions().clear();
	}

	protected File getLocalFile(String fname) {
		return new File(getClass().getResource(fname).getFile());
	}

}
