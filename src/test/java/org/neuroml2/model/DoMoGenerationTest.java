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
import javax.xml.bind.Unmarshaller;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.lemsml.model.ComponentReference;
import org.lemsml.model.compiler.LEMSCompilerFrontend;
import org.lemsml.model.compiler.semantic.LEMSSemanticAnalyser;
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

public class DoMoGenerationTest {

	private JAXBContext jaxbContext;
	private Neuroml2 hh;
	private Lems domainDefs;

	@Rule public ExpectedException thrown = ExpectedException.none();

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

		assertEquals(212, domainDefs.getComponentTypes().size());
	}

	@Test
	public void testAPI() throws Throwable {
        NeuroML2ModelReader nmlReader = new NeuroML2ModelReader();

		Neuroml2 hh2 = nmlReader.read_(getLocalFile("/NML2_SingleCompHHCell.nml"));
		assertEquals(hh.getCells().size(), hh2.getCells().size());
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
		assertEquals(3, cell.getBiophysicalProperties().getMembraneProperties()
				.getChannelDensities().size());
		assertEquals("-65mV", cell.getBiophysicalProperties()
				.getMembraneProperties().getInitMembPotential()
				.getParameterValue("value"));
	}

	@Test
	public void testEvaluation() throws LEMSCompilerException {

		Cell cell = (Cell) hh.getComponentById("hhcell");
		ChannelDensity naChans = (ChannelDensity) cell
				.getBiophysicalProperties()
				.getMembraneProperties()
				.getSubComponentsWithName("naChans").get(0);


		//different ways to use the API
		Double g_Na = toDouble(naChans.getParameterValue("condDensity"));
		assertEquals(g_Na, toDouble(naChans.getCondDensity()));
		assertEquals(g_Na, cell.getBiophysicalProperties().getMembraneProperties()
								.getChannelDensities().get(1).getScope()
								.evaluate("condDensity").getValue().doubleValue(), 1e-12);

		// Testing lems/nml consistence
		// changing par via lems api.
		//TODO: discuss whether we should have a ref or a copy of the Channel
		//      inside the density, i.e. should the change below propagate to
		//      all instances of naChan?
		BaseIonChannel naChan = naChans.getIonChannel();
		assertEquals("10pS", naChan.getParameterValue("conductance"));
		naChan.withParameterValue("conductance", "42 pS");
		assertEquals(hh.getComponentById("naChan").getParameterValue("conductance"), "42 pS");
		assertEquals(42.0, naChan.getScope().evaluate("conductance").getValue()
				.doubleValue(), 1e-12);

		// changing par via domain api
		naChan.setConductance("10 pS");
		assertEquals("10 pS", naChan.getConductance());

		// Expression evaluation
		List<Gate> naGates = ((IonChannelHH) naChan).getGates();
		GateHHrates m = (GateHHrates) naGates.get(0);
		assertEquals(m, naChan.getSubComponentsWithName("m").get(0));

		Scope rev = m.getReverseRate().getScope();

		Double rate = rev.evaluate("rate").getValue().doubleValue();
		Double scale = rev.evaluate("scale").getValue().doubleValue();
		Double midpoint = rev.evaluate("midpoint").getValue().doubleValue();
		Double expected = rate * Math.exp((0. - midpoint)/scale);

		assertEquals(expected,
				rev.evaluate("r", getContext("v", 0., "mV")).getValue().doubleValue(), 1e-10);

		thrown.expect(LEMSCompilerException.class); // check if it fails for missing val...
		thrown.expectMessage(LEMSCompilerError.MissingSymbolValue.toString());
		m.getForwardRate().getScope().evaluate("r");


	}

	public ImmutableMap<String, Quantity<?>> getContext(String var, Double i, String unit) {
		ImmutableMap<String, Quantity<?>> ctxt = new ImmutableMap.Builder<String, Quantity<?>>()
				.put(var, Quantities.getQuantity(i , hh.getUnitBySymbol(unit))).build();
		return ctxt;
	}

	private Double toDouble(String valUnit) {
		return Double.valueOf(valUnit.split(" ")[0]);
	}

	@Test
	public void testMarshalling() throws JAXBException, PropertyException,
			IOException {
		//TODO: autogen marshaller??
		File tmpFile = File.createTempFile("test", ".xml");
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		hh.getComponentTypes().clear();
		eraseTypes(hh.getComponents()); // TODO: extremely ugly hack
		eraseUnits(hh); // TODO: extremely ugly hack
		eraseDimensions(hh); // TODO: extremely ugly hack
		eraseDeReferences(hh);// TODO: extremely ugly hack

		marshaller.marshal(hh, tmpFile);
		//System.out.println(Files.toString(tmpFile, Charsets.UTF_8));
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
