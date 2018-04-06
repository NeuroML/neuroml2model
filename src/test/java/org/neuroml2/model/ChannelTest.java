package org.neuroml2.model;

import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Set;
import javax.measure.Quantity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.lemsml.model.extended.Scope;
import tec.units.ri.quantity.Quantities;

public class ChannelTest {

	private Neuroml2 hh;
	private Neuroml2 calva;
	private Neuroml2 ih;
	private Neuroml2 kdr;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Throwable {
        NeuroML2ModelReader nmlReader = new NeuroML2ModelReader();
        
		hh = nmlReader.read_(getLocalFile("/NML2_SingleCompHHCell.nml"));
		ih = nmlReader.read_(getLocalFile("/Ih.channel.nml"));
		calva = nmlReader.read_(getLocalFile("/Ca_LVAst.channel.nml"));
		kdr = nmlReader.read_(getLocalFile("/kdr.channel.nml"));
		kdr = nmlReader.read_(getLocalFile("/Gran_H_98.channel.nml"));
		kdr = nmlReader.read_(getLocalFile("/k2.channel.nml"));
	
	}
    
	@Test
	public void testChannelExtracts() throws Throwable {
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/NML2_SingleCompHHCell.nml")));
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/Ih.channel.nml")));
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/Gran_KA_98.channel.nml")));
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/Gran_KCa_98.channel.nml")));
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/Gran_KDr_98.channel.nml")));
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/Gran_NaF_98.channel.nml")));
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/NaTa.channel.nml")));
        
    }
    

	@Test
	public void testChannels() throws Throwable {
		assertEquals(hh.getCells().size(),1);
        
        Neuroml2[] nmlDocs = new Neuroml2[]{hh,ih,calva,kdr};
        
        for (Neuroml2 nmlDoc: nmlDocs)
        {
            Set<BaseIonChannel> ics = nmlDoc.getAllOfType(BaseIonChannel.class);
            
            for (BaseIonChannel ic: ics)
            {
                System.out.println("------------\nFound channel: "+ic);
                
                if (ic instanceof IonChannelHH)
                {
                    IonChannelHH ic_ = (IonChannelHH)ic;
                
                    if (ic_.getGates()!=null)
                    {
                        for (Gate g: ic_.getGates())
                        {
                            System.out.println("  Found gate: "+g);
                            if (g instanceof GateHHrates)
                            {
                                GateHHrates gh = (GateHHrates)g;

                                Scope forw = gh.getForwardRate().getScope();

                                System.out.println("    F: "+forw.getExpressions());
                                System.out.println("    F: "+forw.resolve("rate"));
                                System.out.println("    F: "+forw.evaluate("r", getContext("v", 0., "mV")));
                            }
                            else if (g instanceof GateHHtauInf)
                            {
                                GateHHtauInf gt = (GateHHtauInf)g;

                                Scope tau = gt.getTimeCourse().getScope();

                                System.out.println("    T: "+tau.getExpressions());
                                System.out.println("    T: "+tau.evaluate("t", getContext("v", 0., "mV")));
                            }
                        }
                    }
                }
            }
        }
	}

	public ImmutableMap<String, Quantity<?>> getContext(String var, Double i, String unit) {
		ImmutableMap<String, Quantity<?>> ctxt 
            = new ImmutableMap.Builder<String, Quantity<?>>().put(var, Quantities.getQuantity(i, hh.getUnitBySymbol(unit))).build();
		return ctxt;
	}

	protected File getLocalFile(String fname) {
		return new File(getClass().getResource(fname).getFile());
	}

}
