package org.neuroml2.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.measure.Quantity;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.lemsml.model.ComponentReference;
import org.lemsml.model.exceptions.LEMSCompilerError;
import org.lemsml.model.exceptions.LEMSCompilerException;
import org.lemsml.model.extended.Component;
import org.lemsml.model.extended.Lems;
import org.lemsml.model.extended.Scope;
import org.lemsml.model.extended.interfaces.HasComponents;

import tec.units.ri.quantity.Quantities;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

public class ACNet2Test {

	private Neuroml2 acnet;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Throwable {
		acnet = NeuroML2ModelReader.read(getLocalFile("/acnet2flat.nml"));
	}


	@Test
	public void testAPI() throws Throwable {
		Neuroml2 hh2 = NeuroML2ModelReader
				.read(getLocalFile("/acnet2flat.nml"));
		assertEquals(acnet.getCells().size(), hh2.getCells().size());
	}

	@Test
	public void testTypes() throws LEMSCompilerException {
		assertEquals(2, acnet.getCells().size());
		assertEquals(8, acnet.getIonChannels().size());
		assertEquals(8, acnet.getAllOfType(BaseIonChannel.class).size());

		Cell cell = (Cell) acnet.getComponentById("pyr_4_sym");

		assertTrue(acnet.getAllOfType(Cell.class).contains(cell));
		assertEquals(5, cell.getBiophysicalProperties().getMembraneProperties()
				.getChannelDensities().size());
		assertEquals("-65.0 mV", cell.getBiophysicalProperties()
				.getMembraneProperties().getInitMembPotential()
				.getParameterValue("value"));
	}

	@Test
	public void testEvaluation() throws LEMSCompilerException {

		Cell cell = (Cell) acnet.getComponentById("pyr_4_sym");
		ChannelDensity naChans = (ChannelDensity) cell
				.getBiophysicalProperties().getMembraneProperties()
				.getSubComponentsWithName("Na_pyr_soma_group").get(0);

		// different ways to use the API
		Double g_Na = toDouble(naChans.getParameterValue("condDensity"));
		assertEquals(g_Na, toDouble(naChans.getCondDensity()));
		assertEquals(g_Na, cell.getBiophysicalProperties()
				.getMembraneProperties().getChannelDensities().get(4)
				.getScope().evaluate("condDensity").getValue().doubleValue(),
				1e-12);

		// Testing lems/nml consistence
		// changing par via lems api.
		// TODO: discuss whether we should have a ref or a copy of the Channel
		// inside the density, i.e. should the change below propagate to
		// all instances of naChan?
		BaseIonChannel naChan = naChans.getIonChannel();
		assertEquals("10pS", naChan.getParameterValue("conductance"));
		naChan.withParameterValue("conductance", "42 pS");
		assertEquals(
				acnet.getComponentById("Na_pyr").getParameterValue(
						"conductance"), "42 pS");
		assertEquals(42.0, naChan.getScope().evaluate("conductance").getValue()
				.doubleValue(), 1e-12);

		// changing par via domain api
		naChan.setConductance("10 pS");
		assertEquals("10 pS", naChan.getConductance());

		// Expression evaluation
		List<Gate> naGates = ((IonChannel) naChan).getGates();
		GateHHrates m = (GateHHrates) naGates.get(0);
		assertEquals(m, naChan.getSubComponentsWithName("m").get(0));

		Scope rev = m.getReverseRate().getScope();

		Double rate = rev.evaluate("rate").getValue().doubleValue();
		Double scale = rev.evaluate("scale").getValue().doubleValue();
		Double midpoint = rev.evaluate("midpoint").getValue().doubleValue();
		// rate * x / (1 - exp(0 - x))
		Double expected = 1000 * (rate * (0. - midpoint) / scale / (1. - Math
				.exp(-(0. - midpoint) / scale))); // explinear

		assertEquals(expected, rev.evaluate("r", getContext("v", 0., "mV"))
				.getValue().doubleValue(), 1e-10);

		thrown.expect(LEMSCompilerException.class); // check if it fails for
													// missing val...
		thrown.expectMessage(LEMSCompilerError.MissingSymbolValue.toString());
		m.getForwardRate().getScope().evaluate("r");

	}

	public ImmutableMap<String, Quantity<?>> getContext(String var, Double i,
			String unit) {
		ImmutableMap<String, Quantity<?>> ctxt = new ImmutableMap.Builder<String, Quantity<?>>()
				.put(var,
						Quantities.getQuantity(i, acnet.getUnitBySymbol(unit)))
				.build();
		return ctxt;
	}

	private Double toDouble(String valUnit) {
		return Double.valueOf(valUnit.split(" ")[0]);
	}

	@Test
	public void testMarshalling() throws JAXBException, PropertyException,
			IOException {
		// TODO: autogen marshaller??
		File tmpFile = File.createTempFile("test", ".xml");
		JAXBContext jaxbContext = JAXBContext.newInstance("org.neuroml2.model");
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		acnet.getComponentTypes().clear();
		eraseTypes(acnet.getComponents()); // TODO: extremely ugly hack
		eraseUnits(acnet); // TODO: extremely ugly hack
		eraseDimensions(acnet); // TODO: extremely ugly hack
		eraseDeReferences(acnet);// TODO: extremely ugly hack

		marshaller.marshal(acnet, tmpFile);
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
