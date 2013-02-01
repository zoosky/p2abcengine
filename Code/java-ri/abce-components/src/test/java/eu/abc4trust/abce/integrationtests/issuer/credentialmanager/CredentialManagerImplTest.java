//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*/**/****************************************************************

package eu.abc4trust.abce.integrationtests.issuer.credentialmanager;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.Test;

import eu.abc4trust.abce.internal.issuer.credentialManager.CredentialManagerImpl;
import eu.abc4trust.abce.internal.issuer.credentialManager.CredentialStorage;
import eu.abc4trust.abce.internal.issuer.credentialManager.PersistentCredentialStorage;
import eu.abc4trust.util.TemporaryFileFactory;
import eu.abc4trust.xml.CryptoParams;
import eu.abc4trust.xml.SecretKey;


public class CredentialManagerImplTest {

    private static final URI EXPECTED_UUID = URI.create("ba419d35-0dfe-4af7-aee7-bbe10c45c028");

    @Test
    public void getIssuerKey() throws Exception {
        CredentialStorage credStore = new PersistentCredentialStorage(
                TemporaryFileFactory.createTemporaryFile());

        CredentialManagerImpl credMng = new CredentialManagerImpl(credStore);

        SecretKey issuerSecretKey = new SecretKey();
        CryptoParams cryptoParams = new CryptoParams();
        cryptoParams.getAny().add("TestString1");
        issuerSecretKey.setCryptoParams(cryptoParams);
        credMng.storeIssuerSecretKey(EXPECTED_UUID,
                issuerSecretKey);

        SecretKey storedIssuerSecretKey = credMng
                .getIssuerSecretKey(EXPECTED_UUID);
        assertEquals(issuerSecretKey.getCryptoParams().getAny()
                .get(0),
                storedIssuerSecretKey.getCryptoParams().getAny()
                .get(0));
    }

    @Test
    public void listCredentials() throws Exception {
        CredentialStorage credStore = new PersistentCredentialStorage(
                TemporaryFileFactory.createTemporaryFile());

        CredentialManagerImpl credMng = new CredentialManagerImpl(credStore);

        SecretKey[] issuerSecretKeys = new SecretKey[3];
        issuerSecretKeys[0] = new SecretKey();
        CryptoParams cryptoParams = new CryptoParams();
        cryptoParams.getAny().add("TestString1");
        issuerSecretKeys[0].setCryptoParams(cryptoParams);

        issuerSecretKeys[1] = new SecretKey();
        cryptoParams = new CryptoParams();
        cryptoParams.getAny().add("TestString2");
        issuerSecretKeys[1].setCryptoParams(cryptoParams);

        issuerSecretKeys[2] = new SecretKey();
        cryptoParams = new CryptoParams();
        cryptoParams.getAny().add("TestString2");
        issuerSecretKeys[2].setCryptoParams(cryptoParams);

        credMng.storeIssuerSecretKey(URI.create("1"), issuerSecretKeys[0]);
        credMng.storeIssuerSecretKey(URI.create("2"), issuerSecretKeys[1]);
        credMng.storeIssuerSecretKey(URI.create("3"), issuerSecretKeys[2]);

        List<URI> storedURIs = credMng.listIssuerSecretKeys();
        for (int inx = 0; inx < issuerSecretKeys.length; inx++) {
            SecretKey issuerSecretKey = credMng
                    .getIssuerSecretKey(storedURIs
                            .get(inx));
            assertEquals(issuerSecretKeys[inx].getCryptoParams().getAny()
                    .get(0), issuerSecretKey.getCryptoParams().getAny().get(0));
        }
    }
}
