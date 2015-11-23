package org.neuroml2.model;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.lemsml.model.compiler.LEMSCompilerFrontend;
import org.lemsml.model.compiler.semantic.LEMSSemanticAnalyser;
import org.lemsml.model.extended.ExtObjectFactory;
import org.lemsml.model.extended.Lems;

public class NeuroML2ModelReader {
	public static Neuroml2 read(File modelFile) throws Throwable {

		File coreTypes = new File(NeuroML2ModelReader.class.getClassLoader()
				.getResource("lems/NeuroML2CoreTypes.xml").getFile());
		Lems domainDefs = new LEMSCompilerFrontend(coreTypes)
				.generateLEMSDocument();

		JAXBContext jaxbContext = JAXBContext.newInstance("org.neuroml2.model");
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		//use the factory for extended LEMS types
		jaxbUnmarshaller.setProperty("com.sun.xml.bind.ObjectFactory", new ExtObjectFactory());
		Neuroml2 model = (Neuroml2) jaxbUnmarshaller.unmarshal(modelFile);

		// TODO: ideal way of doing that?
		model.getComponentTypes().addAll(domainDefs.getComponentTypes());
		model.getUnits().addAll(domainDefs.getUnits());
		model.getConstants().addAll(domainDefs.getConstants());
		model.getDimensions().addAll(domainDefs.getDimensions());
		new LEMSSemanticAnalyser(model).analyse();

		return model;
	}

}
