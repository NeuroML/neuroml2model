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
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.lemsml.exprparser.utils.UndefinedSymbolException;

import org.lemsml.model.compiler.LEMSCompilerFrontend;
import org.lemsml.model.compiler.parser.XMLUtils;
import org.lemsml.model.compiler.semantic.LEMSSemanticAnalyser;
import org.lemsml.model.exceptions.LEMSCompilerException;
import org.lemsml.model.extended.Component;
import org.lemsml.model.extended.ExtObjectFactory;
import org.lemsml.model.extended.Lems;

import org.neuroml2.model.utils.FunctionNodeHelper;

public class NeuroML2ModelReader
{

    private final String[] nmlLemsDefs = new String[]
    {
        "NeuroML2CoreTypes.xml",
        "NeuroMLCoreCompTypes.xml",
        "NeuroMLCoreDimensions.xml",
        "Cells.xml",
        "Channels.xml",
        "Inputs.xml",
        "Networks.xml",
        "PyNN.xml",
        "Simulation.xml",
        "Synapses.xml"
    };

    static Lems domainDefs = null;

    private final Unmarshaller jaxbUnmarshaller_ = null;

    public NeuroML2ModelReader() throws Throwable
    {
        if (domainDefs == null)
        {
            // Currently the LEMS parser requires these to be actual files 
            // => can't read them from jar...
            java.nio.file.Path tempLemsDir = Files.createTempDirectory("lemsNml2Definitions");
            File coreTypes = null;
            for (String f : nmlLemsDefs)
            {
                InputStream input = NeuroML2ModelReader.class.getClassLoader().getResourceAsStream("lems/" + f);
                File fout = new File(tempLemsDir.toFile(), f);
                if (f.equals("NeuroML2CoreTypes.xml"))
                {
                    coreTypes = fout;
                }
                OutputStream out = new FileOutputStream(fout);

                int read;
                byte[] bytes = new byte[1024];
                while ((read = input.read(bytes)) != -1)
                {
                    out.write(bytes, 0, read);
                }
            }

            domainDefs = new LEMSCompilerFrontend(coreTypes)
                .generateLEMSDocument();
        }
    }

    private Unmarshaller getUnmarshaller() throws JAXBException
    {
        if (jaxbUnmarshaller_ != null)
        {
            return jaxbUnmarshaller_;
        }

        com.sun.xml.bind.v2.ContextFactory cf;
        ExtObjectFactory objFactory = new ExtObjectFactory();

        JAXBContext jaxbContext = com.sun.xml.bind.v2.runtime.JAXBContextImpl.newInstance("org.neuroml2.model");
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        jaxbUnmarshaller.setProperty("com.sun.xml.bind.ObjectFactory", objFactory);

        //use the factory for extended LEMS types
        return jaxbUnmarshaller;
    }

    public Neuroml2 read(File modelFile) throws Throwable
    {
        /*
        TODO: replace this with call to read_() below??
         */

        Unmarshaller jun = getUnmarshaller();

        //Adds the correct namespace to ComponentType defs inside nml
        File nsXSLT = getLocalFile("lems/addLemsNS.xslt");
        File kludge71XSLT = getLocalFile("lems/jLEMS_issue71_workaround.xslt");
        File transformed = XMLUtils.transform(modelFile, nsXSLT);
        transformed = XMLUtils.transform(transformed, kludge71XSLT);

        Neuroml2 model = (Neuroml2) jun.unmarshal(transformed);

        // TODO: ideal way of doing that?
        model.getComponentTypes().addAll(domainDefs.getComponentTypes());
        model.getUnits().addAll(domainDefs.getUnits());
        model.getConstants().addAll(domainDefs.getConstants());
        model.getDimensions().addAll(domainDefs.getDimensions());
        new LEMSSemanticAnalyser(model).analyse();

        return model;
    }

    /**
     * This method will read in the file and use the read(String) method to
     * parse it. Note: it also substitutes
     * <neuroml xmlns="http://www.neuroml.org/schema/neuroml2" ...> for
     * <neuroml2>
     */
    public Neuroml2 read_(File modelFile) throws Throwable
    {

        String contents = new String(Files.readAllBytes(Paths.get(modelFile.getAbsolutePath())));

        return read(contents);
    }

    public Neuroml2 read(String modelXml) throws Throwable
    {

        if (modelXml.contains("<neuroml"))
        {
            int s = modelXml.indexOf("<neuroml");
            int b1 = modelXml.indexOf(">", s);
            modelXml = modelXml.substring(0, s) + "<neuroml2>" + modelXml.substring(b1 + 1);
        }
        if (modelXml.contains("</neuroml"))
        {
            int s = modelXml.indexOf("</neuroml");
            int b1 = modelXml.indexOf(">", s);
            modelXml = modelXml.substring(0, s) + "</neuroml2>" + modelXml.substring(b1 + 1);
        }

        Unmarshaller jaxbUnmarshaller = getUnmarshaller();

        //Adds the correct namespace to ComponentType defs inside nml
        File nsXSLT = getLocalFile("lems/addLemsNS.xslt");
        File kludge71XSLT = getLocalFile("lems/jLEMS_issue71_workaround.xslt");
        String transformed = XMLUtils.transform(modelXml, nsXSLT);
        transformed = XMLUtils.transform(transformed, kludge71XSLT);

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

    private static File getLocalFile(String name) throws IOException
    {
        File f = null;

        String path = NeuroML2ModelReader.class.getClassLoader().getResource(name).toExternalForm();
        if (path.startsWith("file:/"))
        {
            path = path.substring(5);
            f = new File(path);
        }
        else if (path.startsWith("jar:"))
        {
            InputStream input = NeuroML2ModelReader.class.getClassLoader().getResourceAsStream(name);
            f = File.createTempFile(name.replaceAll("/", "_"), ".tmp");
            f.deleteOnExit();
            OutputStream out = new FileOutputStream(f);
            int read;
            byte[] bytes = new byte[1024];
            while ((read = input.read(bytes)) != -1)
            {
                out.write(bytes, 0, read);
            }
            f.deleteOnExit();

        }

        if (!f.exists())
        {
            throw new FileNotFoundException("Unable to locate: " + f.getAbsolutePath());
        }
        return f;
    }

    public static String extractInfo(File nmlFile) throws Throwable
    {
        NeuroML2ModelReader nmlr;

        //System.out.println("Opening file: " + nmlFile.getCanonicalPath());
        nmlr = new NeuroML2ModelReader();
        Neuroml2 nml2Doc = nmlr.read_(nmlFile);

        String info = "";

        for (IonChannelHH ic : nml2Doc.getAllOfType(IonChannelHH.class))
        {
            //System.out.println("Ion channel: "+ic);
            for (BaseGate g : ic.getAllOfType(BaseGate.class))
            {
                info+=extractFromGate(g, ic.getId(), nml2Doc);
            }
        }

        for (IonChannel ic : nml2Doc.getAllOfType(IonChannel.class))
        {
            //System.out.println("Ion channel: "+ic);
            for (BaseGate g : ic.getAllOfType(BaseGate.class))
            {
                info+=extractFromGate(g, ic.getId(), nml2Doc);
            }
        }
        return info;

    }
    
    private static String extractFromGate(BaseGate g, String parentId, Neuroml2 nml2Doc) throws LEMSCompilerException, UndefinedSymbolException
    {
        String info = "";
        String preg = parentId + ":gate " + g.getId();
        //System.out.println("Gate: "+g);
        info += preg + ":instances = " + g.getInstances() + "\n";
        for (Component c : g.getChildren())
        {
            if (c.getId().equals("forwardRate"))
            {
                String exp = FunctionNodeHelper.processExpression(c.getScope().resolve("r"), nml2Doc);
                info += preg + ":forward rate = " + exp + "\n";

            }
            if (c.getId().equals("reverseRate"))
            {
                String exp = FunctionNodeHelper.processExpression(c.getScope().resolve("r"), nml2Doc);
                info += preg + ":reverse rate = " + exp + "\n";

            }
            if (c.getId().equals("timeCourse"))
            {
                String exp = FunctionNodeHelper.processExpression(c.getScope().resolve("t"), nml2Doc);
                info += preg + ":time course = " + exp + "\n";

            }
            if (c.getId().equals("steadyState"))
            {
                String exp = FunctionNodeHelper.processExpression(c.getScope().resolve("x"), nml2Doc);
                info += preg + ":steady state = " + exp + "\n";

            }
        }
        return info;
        
    }

    public static void main(String[] args)
    {
        String ionChannelFile = "src/test/resources/Ca_LVAst.channel.nml";
        ionChannelFile = "src/test/resources/SKv3_1.channel.nml";
        ionChannelFile = "src/test/resources/NML2_SingleCompHHCell.nml";
        //ionChannelFile = "src/test/resources/kdr.channel.nml";
        //ionChannelFile = "../../geppetto/org.geppetto.model.neuroml/src/test/resources/traub/k2.channel.nml";
        //ionChannelFile = "../../geppetto/org.geppetto.model.neuroml/src/test/resources/hhcell/NML2_SingleCompHHCell.nml";
        //ionChannelFile = "/tmp/NeuroML_456530365697517062.tmp";

        if (args.length == 1)
        {
            ionChannelFile = args[0];
        }

        File nmlFile = new File(ionChannelFile);

        try
        {
            String info = extractInfo(nmlFile);

            System.out.println(info);
        }
        catch (Throwable ex)
        {
            System.out.println("Error parsing NML2 file " + nmlFile.getAbsolutePath() + ": " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

}
