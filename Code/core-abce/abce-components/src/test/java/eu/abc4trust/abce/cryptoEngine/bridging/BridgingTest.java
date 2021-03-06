//* Licensed Materials - Property of                                  *
//* IBM                                                               *
//* Miracle A/S                                                       *
//* Alexandra Instituttet A/S                                         *
//*                                                                   *
//* eu.abc4trust.pabce.1.34                                           *
//*                                                                   *
//* (C) Copyright IBM Corp. 2014. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2014. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2014. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*                                                                   *
//* This file is licensed under the Apache License, Version 2.0 (the  *
//* "License"); you may not use this file except in compliance with   *
//* the License. You may obtain a copy of the License at:             *
//*   http://www.apache.org/licenses/LICENSE-2.0                      *
//* Unless required by applicable law or agreed to in writing,        *
//* software distributed under the License is distributed on an       *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY            *
//* KIND, either express or implied.  See the License for the         *
//* specific language governing permissions and limitations           *
//* under the License.                                                *
//*/**/****************************************************************

package eu.abc4trust.abce.cryptoEngine.bridging;

import static eu.abc4trust.abce.internal.revocation.RevocationConstants.REVOCATION_HANDLE_STR;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ibm.zurich.idmx.interfaces.util.Pair;

import eu.abc4trust.TestConfiguration;
import eu.abc4trust.abce.external.inspector.InspectorAbcEngine;
import eu.abc4trust.abce.external.issuer.IssuerAbcEngine;
import eu.abc4trust.abce.external.verifier.VerifierAbcEngine;
import eu.abc4trust.abce.testharness.IntegrationModuleFactory;
import eu.abc4trust.abce.testharness.IssuanceHelper;
import eu.abc4trust.cryptoEngine.util.SystemParametersUtil;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.util.CryptoUriUtil;
import eu.abc4trust.xml.CredentialInToken;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.InspectorPublicKey;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.PresentationPolicyAlternatives;
import eu.abc4trust.xml.PresentationToken;
import eu.abc4trust.xml.SystemParameters;
import eu.abc4trust.xml.VerifierParameters;
import eu.abc4trust.xml.util.XmlUtils;

@Ignore("Range proofs not implements; also needs to fix one method to force failed presentation")
public class BridgingTest {

    private static final String PRESENTATION_POLICY_ALTERNATIVES_BRIDGING = "/eu/abc4trust/sampleXml/presentationPolicies/presentationPolicyAlternativesBridging.xml";

    private static final String ISSUANCE_POLICY_PASSPORT = "/eu/abc4trust/sampleXml/issuance/issuancePolicyPassport.xml";
    private static final String CREDENTIAL_SPECIFICATION_PASSPORT = "/eu/abc4trust/sampleXml/credspecs/credentialSpecificationPassport.xml";
    private static final String ISSUANCE_POLICY_STUDENT_CARD = "/eu/abc4trust/sampleXml/issuance/issuancePolicyStudentCard.xml";
    private static final String CREDENTIAL_SPECIFICATION_STUDENT_CARD = "/eu/abc4trust/sampleXml/credspecs/credentialSpecificationStudentCardForHotelBooking.xml";

    private static final String USERNAME = "defaultUser";
    
    private static final String NAME = "John";
    private static final String LASTNAME = "Doe";

    private static final URI INSPECTOR_URI = URI.create("http://thebestbank.com/inspector/pub_key_v1");

   // @Ignore
    @Test(timeout=TestConfiguration.TEST_TIMEOUT)
    public void bridgingTestUProve() throws Exception {
        this.setupIdenticalEngines(CryptoEngine.UPROVE);
    }
    // @Ignore
    @Test(timeout=TestConfiguration.TEST_TIMEOUT)
    public void bridgingTestIdemix() throws Exception {
        this.setupIdenticalEngines(CryptoEngine.IDEMIX);
    }

    public void setupIdenticalEngines(CryptoEngine chosenEngine) throws Exception{

        // The Guice injector is configured to return the same instance for
        // every invocation with the same class type. So the storage of
        // credentials is all done as side-effects.

        //Construct issuers
        Injector governmentInjector = Guice
                .createInjector(IntegrationModuleFactory.newModule(new Random(1231),
                        chosenEngine));
        Injector universityInjector = Guice
                .createInjector(IntegrationModuleFactory.newModule(new Random(1231),
                        chosenEngine));

        //Construct user
        Injector userInjector = Guice.createInjector(IntegrationModuleFactory.newModule(
                new Random(1987)));

        //Construct verifier
        Injector hotelInjector = Guice
                .createInjector(IntegrationModuleFactory.newModule(new Random(1231)));

        Injector inspectorInjector = Guice.createInjector(IntegrationModuleFactory.newModule(new Random(1231)));

        IssuanceHelper issuanceHelper = new IssuanceHelper();

        IssuerAbcEngine governmentEngine = governmentInjector
                .getInstance(IssuerAbcEngine.class);
        IssuerAbcEngine universityEngine = universityInjector
                .getInstance(IssuerAbcEngine.class);

        InspectorAbcEngine inspectorEngine = null;
        if (inspectorInjector != null) {
            inspectorEngine = inspectorInjector.getInstance(InspectorAbcEngine.class);
        }

        KeyManager inspectorKeyManager = null;
        if (inspectorInjector != null) {
            inspectorKeyManager = inspectorInjector.getInstance(KeyManager.class);
        }

        KeyManager governmentKeyManager = governmentInjector
                .getInstance(KeyManager.class);
        KeyManager userKeyManager = userInjector.getInstance(KeyManager.class);
        KeyManager universityKeyManager = universityInjector
                .getInstance(KeyManager.class);
        KeyManager hotelKeyManager = hotelInjector
                .getInstance(KeyManager.class);



        // Fetch system parameters.
        SystemParameters systemParameters = SystemParametersUtil.getDefaultSystemParameters_1024();
            XmlUtils.fixNestedContent(systemParameters.getCryptoParams());
        
        governmentKeyManager.storeSystemParameters(systemParameters);
        universityKeyManager.storeSystemParameters(systemParameters);
        userKeyManager.storeSystemParameters(systemParameters);
        hotelKeyManager.storeSystemParameters(systemParameters);
        inspectorKeyManager.storeSystemParameters(systemParameters);


        // Setup issuance policies.
        IssuancePolicy passportIssuancePolicy = (IssuancePolicy) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                ISSUANCE_POLICY_PASSPORT), true);
        URI passportIssuancePolicyUid = passportIssuancePolicy
                .getCredentialTemplate().getIssuerParametersUID();

        IssuancePolicy studentCardIssuancePolicy = (IssuancePolicy) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                ISSUANCE_POLICY_STUDENT_CARD), true);
        URI studentCardIssuancePolicyUid = studentCardIssuancePolicy
                .getCredentialTemplate().getIssuerParametersUID();

        // Load credential specifications.
        CredentialSpecification passportCredSpec = (CredentialSpecification) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                CREDENTIAL_SPECIFICATION_PASSPORT), true);
        CredentialSpecification credentialSpecificationStudent = (CredentialSpecification) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                CREDENTIAL_SPECIFICATION_STUDENT_CARD), true);

        // Store credential specifications.
        governmentKeyManager.storeCredentialSpecification(
                passportCredSpec.getSpecificationUID(), passportCredSpec);

        userKeyManager.storeCredentialSpecification(
                passportCredSpec.getSpecificationUID(), passportCredSpec);

        hotelKeyManager.storeCredentialSpecification(
                passportCredSpec.getSpecificationUID(), passportCredSpec);

        universityKeyManager.storeCredentialSpecification(
                credentialSpecificationStudent.getSpecificationUID(),
                credentialSpecificationStudent);

        userKeyManager.storeCredentialSpecification(
                credentialSpecificationStudent.getSpecificationUID(),
                credentialSpecificationStudent);

        hotelKeyManager.storeCredentialSpecification(
                credentialSpecificationStudent.getSpecificationUID(),
                credentialSpecificationStudent);

        // Generate issuer parameters.
        URI hash = new URI("urn:abc4trust:1.0:hashalgorithm:sha-256");
        URI engineType = null;
        if(chosenEngine==CryptoEngine.IDEMIX){
            engineType = URI.create("Idemix");
        } else {
            engineType = URI.create("Uprove");
        }

        URI revocationId = new URI("revocationUID1");
        IssuerParameters governementPassportIssuerParameters = governmentEngine
                .setupIssuerParameters(passportCredSpec, systemParameters,
                        passportIssuancePolicyUid, hash, engineType, revocationId, null);

        revocationId = new URI("revocationUID2");
        IssuerParameters universityStudentCardIssuerParameters = universityEngine
                .setupIssuerParameters(
                        credentialSpecificationStudent, systemParameters,
                        studentCardIssuancePolicyUid, hash, engineType, revocationId, null);
        
    IssuerParameters dummyForRangeProof =
        universityEngine.setupIssuerParameters(systemParameters, 0, URI.create("cl"),
            URI.create("vp:rangeProof"), null, null);

        // store issuance parameters for government and user.
        governmentKeyManager.storeIssuerParameters(passportIssuancePolicyUid,
                governementPassportIssuerParameters);
        userKeyManager.storeIssuerParameters(passportIssuancePolicyUid,
                governementPassportIssuerParameters);
        hotelKeyManager.storeIssuerParameters(passportIssuancePolicyUid,
                governementPassportIssuerParameters);

        // store parameters for university and user:
        universityKeyManager.storeIssuerParameters(studentCardIssuancePolicyUid,
                universityStudentCardIssuerParameters);
        userKeyManager.storeIssuerParameters(studentCardIssuancePolicyUid,
                universityStudentCardIssuerParameters);
        hotelKeyManager.storeIssuerParameters(studentCardIssuancePolicyUid,
                universityStudentCardIssuerParameters);
        
        // store parameters range proofs
        universityKeyManager.storeIssuerParameters(dummyForRangeProof.getParametersUID(),
          dummyForRangeProof);
        userKeyManager.storeIssuerParameters(dummyForRangeProof.getParametersUID(),
          dummyForRangeProof);
        hotelKeyManager.storeIssuerParameters(dummyForRangeProof.getParametersUID(),
          dummyForRangeProof);

        if (inspectorInjector != null) {
            List<FriendlyDescription> friendlyDescription = Collections.emptyList();
            InspectorPublicKey inspectorPubKey =
                    inspectorEngine.setupInspectorPublicKey(systemParameters,
                            CryptoUriUtil.getIdemixMechanism(),
                            BridgingTest.INSPECTOR_URI,
                            friendlyDescription);
            inspectorKeyManager.storeInspectorPublicKey(BridgingTest.INSPECTOR_URI, inspectorPubKey);
            userKeyManager.storeInspectorPublicKey(
                    BridgingTest.INSPECTOR_URI, inspectorPubKey);
            hotelKeyManager.storeInspectorPublicKey(
                    BridgingTest.INSPECTOR_URI, inspectorPubKey);
        }
        
        // Generate verifier parameters
        VerifierAbcEngine verifierEngine = universityInjector.getInstance(VerifierAbcEngine.class);
        VerifierParameters verifierParameters = verifierEngine.createVerifierParameters(systemParameters);

        this.runTests(governmentInjector, userInjector, universityInjector, hotelInjector, issuanceHelper, verifierParameters);
    }

    //@Ignore
    @Test(timeout=TestConfiguration.TEST_TIMEOUT)
    public void bridgingTestDifferentIssuers() throws Exception {


        // The Guice injector is configured to return the same instance for
        // every invocation with the same class type. So the storage of
        // credentials is all done as side-effects.

        //Construct issuers
        Injector governmentInjector = Guice
                .createInjector(IntegrationModuleFactory.newModule(new Random(1231),
                  CryptoEngine.IDEMIX));
        Injector universityInjector = Guice
                .createInjector(IntegrationModuleFactory.newModule(new Random(1231),
                  CryptoEngine.UPROVE));

        //Construct user
        Injector userInjector = Guice.createInjector(IntegrationModuleFactory.newModule(
                new Random(1987)));

        //Construct verifier
        Injector hotelInjector = Guice
                .createInjector(IntegrationModuleFactory.newModule(new Random(1231)));

        Injector inspectorInjector = Guice.createInjector(IntegrationModuleFactory.newModule(new Random(1231)));

        IssuanceHelper issuanceHelper = new IssuanceHelper();

        IssuerAbcEngine governmentEngine = governmentInjector
                .getInstance(IssuerAbcEngine.class);
        IssuerAbcEngine universityEngine = universityInjector
                .getInstance(IssuerAbcEngine.class);

        InspectorAbcEngine inspectorEngine = null;
        if (inspectorInjector != null) {
            inspectorEngine = inspectorInjector.getInstance(InspectorAbcEngine.class);
        }

        KeyManager inspectorKeyManager = null;
        if (inspectorInjector != null) {
            inspectorKeyManager = inspectorInjector.getInstance(KeyManager.class);
        }

        KeyManager governmentKeyManager = governmentInjector
                .getInstance(KeyManager.class);
        KeyManager userKeyManager = userInjector.getInstance(KeyManager.class);
        KeyManager universityKeyManager = universityInjector
                .getInstance(KeyManager.class);
        KeyManager hotelKeyManager = hotelInjector
                .getInstance(KeyManager.class);



        // Fetch system parameters.
        SystemParameters sysParam = SystemParametersUtil.getDefaultSystemParameters_1024();
        
        governmentKeyManager.storeSystemParameters(sysParam);
        universityKeyManager.storeSystemParameters(sysParam);
        userKeyManager.storeSystemParameters(sysParam);
        hotelKeyManager.storeSystemParameters(sysParam);
        if (inspectorKeyManager != null) {
            inspectorKeyManager.storeSystemParameters(sysParam);
        }

        // Setup issuance policies.
        IssuancePolicy passportIssuancePolicy = (IssuancePolicy) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                ISSUANCE_POLICY_PASSPORT), true);
        URI passportIssuancePolicyUid = passportIssuancePolicy
                .getCredentialTemplate().getIssuerParametersUID();

        IssuancePolicy studentCardIssuancePolicy = (IssuancePolicy) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                ISSUANCE_POLICY_STUDENT_CARD), true);
        URI studentCardIssuancePolicyUid = studentCardIssuancePolicy
                .getCredentialTemplate().getIssuerParametersUID();


        // Load credential specifications.
        CredentialSpecification passportCredSpec = (CredentialSpecification) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                CREDENTIAL_SPECIFICATION_PASSPORT), true);
        CredentialSpecification credentialSpecificationStudent = (CredentialSpecification) XmlUtils
                .getObjectFromXML(
                        this.getClass().getResourceAsStream(
                                CREDENTIAL_SPECIFICATION_STUDENT_CARD), true);


        // Store credential specifications.
        governmentKeyManager.storeCredentialSpecification(
                passportCredSpec.getSpecificationUID(), passportCredSpec);

        userKeyManager.storeCredentialSpecification(
                passportCredSpec.getSpecificationUID(), passportCredSpec);

        hotelKeyManager.storeCredentialSpecification(
                passportCredSpec.getSpecificationUID(), passportCredSpec);

        universityKeyManager.storeCredentialSpecification(
                credentialSpecificationStudent.getSpecificationUID(),
                credentialSpecificationStudent);

        userKeyManager.storeCredentialSpecification(
                credentialSpecificationStudent.getSpecificationUID(),
                credentialSpecificationStudent);

        hotelKeyManager.storeCredentialSpecification(
                credentialSpecificationStudent.getSpecificationUID(),
                credentialSpecificationStudent);

        // Generate issuer parameters.
        URI hash = new URI("urn:abc4trust:1.0:hashalgorithm:sha-256");
        URI revocationId = new URI("revocationUID1");
        IssuerParameters governementPassportIssuerParameters = governmentEngine
                .setupIssuerParameters(passportCredSpec, sysParam,
                        passportIssuancePolicyUid, hash, URI.create("Idemix"), revocationId, null);

        revocationId = new URI("revocationUID2");
        IssuerParameters universityStudentCardIssuerParameters = universityEngine
                .setupIssuerParameters(
                        credentialSpecificationStudent, sysParam,
                        studentCardIssuancePolicyUid, hash, URI.create("uprove"), revocationId, null);
        
        IssuerParameters dummyForRangeProof =
            universityEngine.setupIssuerParameters(sysParam, 0, URI.create("cl"),
                URI.create("vp:rangeProof"), null, null);

        // store issuance parameters for government and user.
        governmentKeyManager.storeIssuerParameters(passportIssuancePolicyUid,
                governementPassportIssuerParameters);
        userKeyManager.storeIssuerParameters(passportIssuancePolicyUid,
                governementPassportIssuerParameters);
        hotelKeyManager.storeIssuerParameters(passportIssuancePolicyUid,
                governementPassportIssuerParameters);

        // store parameters for university and user:
        universityKeyManager.storeIssuerParameters(
                studentCardIssuancePolicyUid,
                universityStudentCardIssuerParameters);
        userKeyManager.storeIssuerParameters(studentCardIssuancePolicyUid,
                universityStudentCardIssuerParameters);
        hotelKeyManager.storeIssuerParameters(studentCardIssuancePolicyUid,
                universityStudentCardIssuerParameters);
        
        // store parameters range proofs
        universityKeyManager.storeIssuerParameters(dummyForRangeProof.getParametersUID(),
          dummyForRangeProof);
        userKeyManager.storeIssuerParameters(dummyForRangeProof.getParametersUID(),
          dummyForRangeProof);
        hotelKeyManager.storeIssuerParameters(dummyForRangeProof.getParametersUID(),
          dummyForRangeProof);

        if (inspectorInjector != null) {
            List<FriendlyDescription> friendlyDescription = Collections.emptyList();
            InspectorPublicKey inspectorPubKey =
                    inspectorEngine.setupInspectorPublicKey(sysParam,
                            CryptoUriUtil.getIdemixMechanism(),
                            BridgingTest.INSPECTOR_URI,
                            friendlyDescription);
            inspectorKeyManager.storeInspectorPublicKey(BridgingTest.INSPECTOR_URI, inspectorPubKey);
            userKeyManager.storeInspectorPublicKey(
                    BridgingTest.INSPECTOR_URI, inspectorPubKey);
            hotelKeyManager.storeInspectorPublicKey(
                    BridgingTest.INSPECTOR_URI, inspectorPubKey);
        }

        // Generate verifier parameters
        VerifierAbcEngine verifierEngine = universityInjector.getInstance(VerifierAbcEngine.class);
        VerifierParameters verifierParameters = verifierEngine.createVerifierParameters(sysParam);
        
        this.runTests(governmentInjector, userInjector, universityInjector, hotelInjector, issuanceHelper, verifierParameters);
    }

    private void runTests(Injector governmentInjector, Injector userInjector,
            Injector universityInjector, Injector hotelInjector, IssuanceHelper issuanceHelper, VerifierParameters verifierParameters) throws Exception{

        // Step 1. Get passport.
        System.out.println(">> Get passport.");
        this.issueAndStorePassport(governmentInjector, userInjector,
                issuanceHelper, verifierParameters);

        //     userInjector.getInstance(CryptoEngineChoice.class).chosenEngine = CryptoEngine.UPROVE;
        // Step 2. Get student id.
        System.out.println(">> Get student id.");
        this.issueAndStoreStudentId(universityInjector, userInjector,
                issuanceHelper, verifierParameters);

        // Step 4a. Book a hotel room using passport and credit card. This uses
        // the first alternative of the presentation policy.
        System.out.println(">> Verify.");
        @SuppressWarnings("unused")
        PresentationToken pt = this.bookHotelRoomUsingPassportAndCreditcard(
                issuanceHelper, hotelInjector, userInjector, verifierParameters);

        // Step 4b. Booking a hotel room using passport and credit card fails
        // because customer is blacklisted by hotel.
        this.failBookingHotelRoomUsingPassportAndStudentCard(hotelInjector,
                userInjector, issuanceHelper, verifierParameters);

    }

    private void issueAndStorePassport(Injector governmentInjector,
            Injector userInjector, IssuanceHelper issuanceHelper, VerifierParameters verifierParameters)
                    throws Exception {
        Map<String, Object> passportAtts = this.populatePassportAttributes();
        issuanceHelper.issueCredential(USERNAME, governmentInjector, userInjector,
                CREDENTIAL_SPECIFICATION_PASSPORT, ISSUANCE_POLICY_PASSPORT,
                passportAtts, verifierParameters);
    }

    private Map<String, Object> populatePassportAttributes() {
        Map<String, Object> att = new HashMap<String, Object>();
        att.put("Name", NAME);
        att.put("LastName", LASTNAME);
        att.put(REVOCATION_HANDLE_STR,
                "http://admin.ch/passport/revocation/parameters");
        att.put("PassportNumber", 895749);
        att.put("Issued", "2010-02-06Z");
        att.put("Expires", "2022-02-06Z");
        att.put("IssuedBy", "admin.ch");
        return att;
    }

    private void issueAndStoreStudentId(Injector univsersityInjector,
            Injector userInjector, IssuanceHelper issuanceHelper, VerifierParameters verifierParameters)
                    throws Exception {
        Map<String, Object> atts = this
                .populateStudentIdIssuerAttributes();
        issuanceHelper.issueCredential(USERNAME, univsersityInjector, userInjector,
                CREDENTIAL_SPECIFICATION_STUDENT_CARD,
                ISSUANCE_POLICY_STUDENT_CARD, atts, verifierParameters);
    }

    private Map<String, Object> populateStudentIdIssuerAttributes() {
        Map<String, Object> atts = new HashMap<String, Object>();
        atts.put("Name", NAME);
        atts.put("LastName", LASTNAME);
        atts.put("StudentNumber", 345);
        atts.put("Issued", "2012-02-02Z");
        atts.put("Expires", "2022-02-02Z");

        atts.put("IssuedBy", "ethz.ch");

        return atts;
    }


    private PresentationToken bookHotelRoomUsingPassportAndCreditcard(
            IssuanceHelper issuanceHelper, Injector hotelInjector,
            Injector userInjector, VerifierParameters verifierParameters) throws Exception {
        return this.bookHotel(issuanceHelper, hotelInjector, userInjector, verifierParameters);
    }


    private PresentationToken bookHotel(IssuanceHelper issuanceHelper,
            Injector hotelInjector, Injector userInjector, VerifierParameters verifierParameters)
                    throws Exception {
        Pair<PresentationToken, PresentationPolicyAlternatives> p = issuanceHelper
                .createPresentationToken(USERNAME, userInjector,
                        //                        PRESENTATION_POLICY_ALTERNATIVES_HOTEL,
                        PRESENTATION_POLICY_ALTERNATIVES_BRIDGING, verifierParameters);

        // Store all required cred specs in the verifier key manager.
        KeyManager hotelKeyManager = hotelInjector.getInstance(KeyManager.class);
        KeyManager userKeyManager = userInjector.getInstance(KeyManager.class);

        PresentationToken pt = p.first;
        assertNotNull(pt);
        for (CredentialInToken cit: pt.getPresentationTokenDescription().getCredential()){
            hotelKeyManager.storeCredentialSpecification(
                    cit.getCredentialSpecUID(),
                    userKeyManager.getCredentialSpecification(cit.getCredentialSpecUID()));
        }

        return issuanceHelper.verify(hotelInjector, p.second, p.first);
    }

    private void failBookingHotelRoomUsingPassportAndStudentCard(
            Injector hotelInjector, Injector userInjector,
            IssuanceHelper issuanceHelper, VerifierParameters verifierParameters) throws Exception {
      //TODO(enr): Fix this method
      org.junit.Assert.fail();
      /*
        int presentationTokenChoice = 0;
        int pseudonymChoice = 1;
        Pair<PresentationToken, PresentationPolicyAlternatives> p = issuanceHelper
                .createPresentationToken(USERNAME, userInjector, userInjector,
                        //                        PRESENTATION_POLICY_ALTERNATIVES_HOTEL,
                        PRESENTATION_POLICY_ALTERNATIVES_BRIDGING,
                        new PolicySelector(presentationTokenChoice,
                                pseudonymChoice));
        PresentationToken pt = p.first();
        // TODO: The user should not be able to create a presentation token as
        // his passport number is on the Hotel blacklist.
        // assertNull(pt);
        assertNotNull(pt);*/
    }


}
