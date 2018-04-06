package org.neuroml2.model;

import com.google.common.collect.ImmutableMap;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import javax.measure.Quantity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.lemsml.model.extended.Scope;
import tec.units.ri.quantity.Quantities;
import static org.junit.Assert.assertEquals;

public class ChannelTest {

	private ArrayList<Neuroml2> channelDocs = new ArrayList();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Throwable {
        NeuroML2ModelReader nmlReader = new NeuroML2ModelReader();
        
		channelDocs.add(nmlReader.read_(getLocalFile("/NML2_SingleCompHHCell.nml")));
		channelDocs.add(nmlReader.read_(getLocalFile("/Ih.channel.nml")));
		channelDocs.add(nmlReader.read_(getLocalFile("/Ca_LVAst.channel.nml")));
		channelDocs.add(nmlReader.read_(getLocalFile("/kdr.channel.nml")));
		channelDocs.add(nmlReader.read_(getLocalFile("/Gran_H_98.channel.nml")));
		channelDocs.add(nmlReader.read_(getLocalFile("/k2.channel.nml")));
		channelDocs.add(nmlReader.read_(getLocalFile("/kx_rod.channel.nml")));
	
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
        System.out.println("5555");
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/kx_rod.channel.nml")));
        System.out.println(NeuroML2ModelReader.extractInfo(getLocalFile("/rods.nml")));
        
    }
    
/*
	@Test
	public void testChannels() throws Throwable {
		assertEquals(channelDocs.get(0).getCells().size(),1);
        
        for (Neuroml2 nmlDoc: channelDocs)
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
	}*/

	public ImmutableMap<String, Quantity<?>> getContext(String var, Double i, String unit) {
		ImmutableMap<String, Quantity<?>> ctxt 
            = new ImmutableMap.Builder<String, Quantity<?>>().put(var, Quantities.getQuantity(i, channelDocs.get(0).getUnitBySymbol(unit))).build();
		return ctxt;
	}

	protected File getLocalFile(String fname) {
		return new File(getClass().getResource(fname).getFile());
	}

}
