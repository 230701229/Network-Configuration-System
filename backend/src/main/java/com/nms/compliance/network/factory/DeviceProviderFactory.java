package com.nms.compliance.network.factory;

import com.nms.compliance.entity.Device;
import com.nms.compliance.network.provider.*;
import org.springframework.stereotype.Component;

@Component
public class DeviceProviderFactory {
    private final MockDeviceProvider mockProvider;
    private final NetconfDeviceProvider netconfProvider;
    private final RestconfDeviceProvider restconfProvider;
    private final SshDeviceProvider sshProvider;

    public DeviceProviderFactory(MockDeviceProvider mockProvider, 
                                 NetconfDeviceProvider netconfProvider, 
                                 RestconfDeviceProvider restconfProvider,
                                 SshDeviceProvider sshProvider) {
        this.mockProvider = mockProvider;
        this.netconfProvider = netconfProvider;
        this.restconfProvider = restconfProvider;
        this.sshProvider = sshProvider;
    }

    public NetworkDeviceProvider getProvider(Device.ManagementProtocol protocol) {
        if (protocol == null) return mockProvider;
        return switch (protocol) {
            case NETCONF -> netconfProvider;
            case RESTCONF -> restconfProvider;
            case SSH -> sshProvider;
            case MOCK -> mockProvider;
        };
    }
}
