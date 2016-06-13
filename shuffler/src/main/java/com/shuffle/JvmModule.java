package com.shuffle;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.Security;

import javax.crypto.Cipher;

public class JvmModule extends AbstractModule {
    static {
        System.out.println("adding BC as provider");
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        crashIfJCEMissing();
    }

    private static void crashIfJCEMissing() {
        int size = 0;
        try {
            size = Cipher.getMaxAllowedKeyLength("AES");
        } catch (Exception e) {
            System.out.println("Crypto limited by policy?");
        }
        if (size < Integer.MAX_VALUE) {
            String msg = "Please either use OpenJDK or allow yourself to use strong crypto\n" +
                    "by installing the according JCE files:\n" +
                    "http://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters";
            throw new RuntimeException(msg);
        }
    }

    @Override
    protected void configure() {
        bind(Provider.class).to(BouncyCastleProvider.class);
        bindConstant().annotatedWith(Names.named("providerName")).to(BouncyCastleProvider.PROVIDER_NAME);
    }
}
