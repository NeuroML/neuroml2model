package org.neuroml2.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.lemsml.model.ComponentReference;
import org.lemsml.model.compiler.LEMSCompilerFrontend;
import org.lemsml.model.compiler.semantic.LEMSSemanticAnalyser;
import org.lemsml.model.exceptions.LEMSCompilerException;
import org.lemsml.model.extended.Component;
import org.lemsml.model.extended.Lems;
import org.lemsml.model.extended.interfaces.HasComponents;

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

	@Test
	public void testTypes() throws LEMSCompilerException {
		assertEquals(1, hh.getCells().size());
		assertEquals(0, hh.getIonChannels().size());
		assertEquals(3, hh.getIonChannelHHs().size());
		assertEquals(3, hh.getAllOfType(BaseIonChannel.class).size());
		assertEquals(6, hh.getAllOfType(BaseVoltageDepRate.class).size());

		Cell cell = (Cell) hh.getComponentById("hhcell");

		assertTrue(hh.getAllOfType(Cell.class).contains(cell));
		assertEquals(3, cell.getBiophysicalProperties().getMembraneProperties().getChannelDensities().size());
		assertEquals("-65mV", cell.getBiophysicalProperties().getMembraneProperties().getInitMembPotential().getParameterValue("value"));
	}

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
		eraseDeReferences(hh);// TODO: extremely ugly hack

		marshaller.marshal(hh, tmpFile);
		System.out.println(Files.toString(tmpFile, Charsets.UTF_8));
	}

	private void eraseDeReferences(HasComponents comp) {
		for (Component subComp : comp.getComponents()) {
			for (ComponentReference ref : subComp.getComponentType()
					.getComponentReferences()) {
				subComp.getComponents().removeAll(
						subComp.getSubComponentsBoundToName(ref.getName()));
			}
			eraseDeReferences(subComp);
		}
	}

	// TODO: argh! @XmlTransient in ext.Comp isn't overriding type from
	// (un-ext)Comp
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
