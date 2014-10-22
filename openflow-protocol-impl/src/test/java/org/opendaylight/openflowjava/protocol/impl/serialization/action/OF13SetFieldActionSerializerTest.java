/*
 * Copyright (c) 2014 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.openflowjava.protocol.impl.serialization.action;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.openflowjava.protocol.api.extensibility.OFSerializer;
import org.opendaylight.openflowjava.protocol.api.extensibility.SerializerRegistry;
import org.opendaylight.openflowjava.protocol.api.keys.MatchEntrySerializerKey;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializerRegistryImpl;
import org.opendaylight.openflowjava.protocol.impl.util.ActionConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.augments.rev131002.ExperimenterIdMatchEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.augments.rev131002.ExperimenterIdMatchEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.augments.rev131002.OxmFieldsAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.augments.rev131002.OxmFieldsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev130731.SetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev130731.actions.grouping.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.ExperimenterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev130731.ExperimenterClass;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev130731.MatchField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev130731.oxm.fields.grouping.MatchEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev130731.oxm.fields.grouping.MatchEntriesBuilder;

/**
 * @author madamjak
 *
 */
public class OF13SetFieldActionSerializerTest {

    private SerializerRegistry registry;
    @Mock OFSerializer<MatchEntries> serializerMock;

    /**
     * Initialize registry and mock
     */
    @Before
    public void startUp() {
        MockitoAnnotations.initMocks(this);
        registry = new SerializerRegistryImpl();
        registry.init();
    }

    /**
     * Test identify ExperimenterClass serializer
     */
    @Test
    public void test(){
        OF13SetFieldActionSerializer ser = new OF13SetFieldActionSerializer();
        ser.injectSerializerRegistry(registry);
        ActionBuilder actionBuilder = new ActionBuilder();
        long experimenterId = 12L;
        ExperimenterIdMatchEntryBuilder expIdMatchBuilder = new ExperimenterIdMatchEntryBuilder();
        expIdMatchBuilder.setExperimenter(new ExperimenterId(experimenterId));
        MatchEntriesBuilder meb = new MatchEntriesBuilder();
        meb.setOxmClass(ExperimenterClass.class);
        meb.setOxmMatchField(OxmMatchFieldClass.class);
        meb.addAugmentation(ExperimenterIdMatchEntry.class, expIdMatchBuilder.build());
        List<MatchEntries> matchEntries = new ArrayList<>();
        MatchEntries me = meb.build();
        matchEntries.add(me);
        OxmFieldsActionBuilder oxmActBuilder = new OxmFieldsActionBuilder();
        oxmActBuilder.setMatchEntries(matchEntries);
        actionBuilder.addAugmentation(OxmFieldsAction.class, oxmActBuilder.build());
        actionBuilder.setType(SetField.class);
        MatchEntrySerializerKey<?, ?> key = new MatchEntrySerializerKey<>(
                EncodeConstants.OF13_VERSION_ID, ExperimenterClass.class, OxmMatchFieldClass.class);
        key.setExperimenterId(experimenterId);
        registry.registerSerializer(key, serializerMock);
        ByteBuf out = UnpooledByteBufAllocator.DEFAULT.buffer();
        ser.serialize(actionBuilder.build(), out);
        Mockito.verify(serializerMock, Mockito.times(1)).serialize((MatchEntries)Mockito.anyObject(), (ByteBuf)Mockito.anyObject());
        int lenght = out.readableBytes();
        Assert.assertEquals("Wrong - bad field code", ActionConstants.SET_FIELD_CODE, out.readUnsignedShort());
        Assert.assertEquals("Wrong - bad lenght", lenght, out.readUnsignedShort());
    }

    private class OxmMatchFieldClass extends MatchField {
        // only for testing purposes
    }
}
