package org.neuroml2.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Paths;
import java.nio.file.Files;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.lemsml.model.compiler.LEMSCompilerFrontend;
import org.lemsml.model.compiler.parser.XMLUtils;
import org.lemsml.model.compiler.semantic.LEMSSemanticAnalyser;
import org.lemsml.model.extended.ExtObjectFactory;
import org.lemsml.model.extended.Lems;

public class NeuroML2ModelReader {
    
    private final String[] nmlLemsDefs = new String[]{"NeuroML2CoreTypes.xml",
                                                "NeuroMLCoreCompTypes.xml",
                                                "NeuroMLCoreDimensions.xml",
                                                "Cells.xml",
                                                "Channels.xml",
                                                "Inputs.xml",
                                                "Networks.xml",
                                                "PyNN.xml",
                                                "Simulation.xml",
                                                "Synapses.xml"};

    Lems domainDefs = null;
    
    public NeuroML2ModelReader() throws Throwable
    {
        // Currently the LEMS parser requires these to be actual files 
        // => can't read them from jar...
        java.nio.file.Path tempLemsDir = Files.createTempDirectory("lemsNml2Definitions");
        File coreTypes = null;
        for (String f: nmlLemsDefs)
        {
            InputStream input = NeuroML2ModelReader.class.getClassLoader().getResourceAsStream("lems/"+f);
            File fout = new File(tempLemsDir.toFile(),f);
            if (f.equals("NeuroML2CoreTypes.xml"))
                coreTypes = fout;
            OutputStream out = new FileOutputStream(fout);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = input.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        }
        
		domainDefs = new LEMSCompilerFrontend(coreTypes)
				.generateLEMSDocument();
    }
    
	public Neuroml2 read(File modelFile) throws Throwable {

		JAXBContext jaxbContext = JAXBContext.newInstance("org.neuroml2.model");
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		//use the factory for extended LEMS types

		//Adds the correct namespace to ComponentType defs inside nml
		File nsXSLT = getLocalFile("lems/addLemsNS.xslt");
		File kludge71XSLT = getLocalFile("lems/jLEMS_issue71_workaround.xslt");
		File transformed = XMLUtils.transform(modelFile, nsXSLT);
		transformed = XMLUtils.transform(transformed, kludge71XSLT);

		jaxbUnmarshaller.setProperty("com.sun.xml.bind.ObjectFactory", new ExtObjectFactory());
		Neuroml2 model = (Neuroml2) jaxbUnmarshaller.unmarshal(transformed);

		// TODO: ideal way of doing that?
		model.getComponentTypes().addAll(domainDefs.getComponentTypes());
		model.getUnits().addAll(domainDefs.getUnits());
		model.getConstants().addAll(domainDefs.getConstants());
		model.getDimensions().addAll(domainDefs.getDimensions());
		new LEMSSemanticAnalyser(model).analyse();

		return model;
	}
    
    /**
    * This method will read in the file and use the read(String) method to parse it. Note: it 
    * also substitutes <neuroml xmlns="http://www.neuroml.org/schema/neuroml2" ...> for <neuroml2>
    */
	public Neuroml2 read_(File modelFile) throws Throwable {
        
        String contents = new String(Files.readAllBytes(Paths.get(modelFile.getAbsolutePath())));
        
        return read(contents);
    }
    
	public Neuroml2 read(String modelXml) throws Throwable {
        
        if (modelXml.contains("<neuroml"))
        {
            int s = modelXml.indexOf("<neuroml");
            int b1 = modelXml.indexOf(">",s);
            modelXml = modelXml.substring(0, s)+"<neuroml2>"+modelXml.substring(b1+1);
        }
        if (modelXml.contains("</neuroml"))
        {
            int s = modelXml.indexOf("</neuroml");
            int b1 = modelXml.indexOf(">",s);
            modelXml = modelXml.substring(0, s)+"</neuroml2>"+modelXml.substring(b1+1);
        }

		JAXBContext jaxbContext = JAXBContext.newInstance("org.neuroml2.model");
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		//use the factory for extended LEMS types

		//Adds the correct namespace to ComponentType defs inside nml
		File nsXSLT = getLocalFile("lems/addLemsNS.xslt");
		File kludge71XSLT = getLocalFile("lems/jLEMS_issue71_workaround.xslt");
		String transformed = XMLUtils.transform(modelXml, nsXSLT);
		transformed = XMLUtils.transform(transformed, kludge71XSLT);

		jaxbUnmarshaller.setProperty("com.sun.xml.bind.ObjectFactory", new ExtObjectFactory());
        StringReader reader = new StringReader(transformed);
		Neuroml2 model = (Neuroml2) jaxbUnmarshaller.unmarshal(reader);

		// TODO: ideal way of doing that?
		model.getComponentTypes().addAll(domainDefs.getComponentTypes());
		model.getUnits().addAll(domainDefs.getUnits());
		model.getConstants().addAll(domainDefs.getConstants());
		model.getDimensions().addAll(domainDefs.getDimensions());
		new LEMSSemanticAnalyser(model).analyse();

		return model;
        
    }

	private static File getLocalFile(String name) throws IOException {
        File f = null;
        
        String path = NeuroML2ModelReader.class.getClassLoader().getResource(name).toExternalForm();
        if (path.startsWith("file:/"))
        {
            path = path.substring(5);
            f = new File(path);
        }
        else if (path.startsWith("jar:")) {
            InputStream input = NeuroML2ModelReader.class.getClassLoader().getResourceAsStream(name);
            f = File.createTempFile(name.replaceAll("/", "_"), ".tmp");
            OutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = input.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            f.deleteOnExit();
     
        }
        
        if (!f.exists())
            throw new FileNotFoundException("Unable to locate: "+f.getAbsolutePath());
		return f;
	}

}
