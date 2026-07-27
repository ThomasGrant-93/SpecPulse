package com.specpulse.version;

public interface SpecVersionPullPort {

    SpecVersionPullResult pullAndSaveVersion(Long serviceId, String serviceName, String specContent);
}
