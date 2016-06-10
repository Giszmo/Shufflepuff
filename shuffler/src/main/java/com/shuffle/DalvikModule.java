package com.shuffle;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

public class DalvikModule extends AbstractModule {
    static {
        System.out.println("adding SC as provider");
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    @Override
    protected void configure() {
        bind(Provider.class).to(BouncyCastleProvider.class);
        bindConstant().annotatedWith(Names.named("providerName")).to(BouncyCastleProvider.PROVIDER_NAME);
    }
}
