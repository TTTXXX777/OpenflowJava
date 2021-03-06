/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowjava.protocol.impl.deserialization.experimenter;

import io.netty.buffer.ByteBuf;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.openflowjava.protocol.api.extensibility.DeserializerRegistry;
import org.opendaylight.openflowjava.protocol.api.extensibility.DeserializerRegistryInjector;
import org.opendaylight.openflowjava.protocol.api.extensibility.OFDeserializer;
import org.opendaylight.openflowjava.protocol.api.keys.MessageCodeKey;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializerRegistryImpl;
import org.opendaylight.openflowjava.protocol.impl.util.BufferHelper;
import org.opendaylight.openflowjava.util.ByteBufUtils;
import org.opendaylight.openflowjava.util.ExperimenterDeserializerKeyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.BundleControlType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.BundleFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.BundlePropertyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.bundle.properties.BundleProperty;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.bundle.properties.bundle.property.bundle.property.entry.BundleExperimenterProperty;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.bundle.properties.bundle.property.bundle.property.entry.bundle.experimenter.property.BundleExperimenterPropertyData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.experimenter.input.experimenter.data.of.choice.BundleControl;

/**
 * Tests for {@link org.opendaylight.openflowjava.protocol.impl.deserialization.experimenter.BundleControlFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BundleControlFactoryTest {

    private OFDeserializer<BundleControl> factory;
    @Mock
    DeserializerRegistry registry;
    @Mock
    OFDeserializer<BundleExperimenterPropertyData> experimenterPropertyDeserializer;

    @Before
    public void startUp() {
        DeserializerRegistry registry = new DeserializerRegistryImpl();
        registry.init();
        factory = registry.getDeserializer(ExperimenterDeserializerKeyFactory.createExperimenterMessageDeserializerKey(
                EncodeConstants.OF13_VERSION_ID, EncodeConstants.ONF_EXPERIMENTER_ID, EncodeConstants.ONF_ET_BUNDLE_CONTROL));
    }

    @Test
    public void testDeserializeWithoutProperties() {
        ByteBuf buffer = ByteBufUtils.hexStringToByteBuf("00 00 00 01 " // bundle ID
                                                       + "00 01 " // type
                                                       + "00 03"); // flags
        BundleControl builtByFactory = factory.deserialize(buffer);
        Assert.assertEquals(1, builtByFactory.getBundleId().getValue().intValue());
        BundleFlags flags = new BundleFlags(true, true);
        Assert.assertEquals("Wrong atomic flag", flags.isAtomic(), builtByFactory.getFlags().isAtomic());
        Assert.assertEquals("Wrong ordered flag", flags.isOrdered(), builtByFactory.getFlags().isOrdered());
        Assert.assertEquals("Wrong type", BundleControlType.ONFBCTOPENREPLY, builtByFactory.getType());
        Assert.assertTrue("Properties not empty", builtByFactory.getBundleProperty().isEmpty());
    }

    @Test
    public void testDeserializeWithProperties() {
        ByteBuf buffer = ByteBufUtils.hexStringToByteBuf("00 00 00 01 " // bundle ID
                                                       + "00 05 " // type
                                                       + "00 02 " // flags
                                                       + "ff ff " // type 1
                                                       + "00 0c " // length 1
                                                       + "00 00 00 01 " // experimenter ID 1
                                                       + "00 00 00 02 " // experimenter type 1
                                                       + "00 00 00 00 " // experimenter data 1
                                                       + "00 00 " // type 2
                                                       + "00 04 " // length 2
                                                       + "00 00 00 00"); // data 2
        Mockito.when(registry.getDeserializer(Matchers.any(MessageCodeKey.class))).thenReturn(experimenterPropertyDeserializer);
        ((DeserializerRegistryInjector)factory).injectDeserializerRegistry(registry);
        BundleControl builtByFactory = BufferHelper.deserialize(factory, buffer);
        Assert.assertEquals(1, builtByFactory.getBundleId().getValue().intValue());
        BundleFlags flags = new BundleFlags(false, true);
        Assert.assertEquals("Wrong atomic flag", flags.isAtomic(), builtByFactory.getFlags().isAtomic());
        Assert.assertEquals("Wrong ordered flag", flags.isOrdered(), builtByFactory.getFlags().isOrdered());
        Assert.assertEquals("Wrong type", BundleControlType.ONFBCTCOMMITREPLY, builtByFactory.getType());
        BundleProperty property = builtByFactory.getBundleProperty().get(0);
        Assert.assertEquals("Wrong bundle property type", BundlePropertyType.ONFETBPTEXPERIMENTER, property.getType());
        BundleExperimenterProperty experimenterProperty = (BundleExperimenterProperty) property.getBundlePropertyEntry();
        Assert.assertEquals("Wrong experimenter ID", 1, experimenterProperty.getExperimenter().getValue().intValue());
        Assert.assertEquals("Wrong experimenter type", 2, experimenterProperty.getExpType().longValue());
        Mockito.verify(experimenterPropertyDeserializer, Mockito.times(1)).deserialize(buffer);
    }

}