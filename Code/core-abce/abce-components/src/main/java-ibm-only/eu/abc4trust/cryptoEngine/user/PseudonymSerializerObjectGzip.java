//* Licensed Materials - Property of                                  *
//* IBM                                                               *
//*                                                                   *
//* eu.abc4trust.pabce.1.34                                           *
//*                                                                   *
//* (C) Copyright IBM Corp. 2014. All Rights Reserved.                *
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

package eu.abc4trust.cryptoEngine.user;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import eu.abc4trust.smartcard.CardStorage;
import eu.abc4trust.smartcard.Smartcard;
import eu.abc4trust.xml.PseudonymWithMetadata;

/**
 * This class serializes pseudonym by compressing java object streams.
 * 
 * @author enr
 */
public class PseudonymSerializerObjectGzip extends AbstractPseudonymSerializer {

	private final CardStorage cardStorage;
	
	public PseudonymSerializerObjectGzip(CardStorage cardStorage){
		this.cardStorage = cardStorage;
	}
	
	@Override
	public CardStorage getCardStorage(){
		return this.cardStorage;
	}
	
  @Override
  public byte[] serializePseudonym(PseudonymWithMetadata pwm) {
	  if(pwm.getPseudonym().isExclusive()){
		  return this.serializeExclusivePseudonym(pwm);
	  }
	  
    try {
      ByteArrayOutputStream ser = new ByteArrayOutputStream();
      ser.write(magicHeader());

      GZIPOutputStream gs = new GZIPOutputStream(ser);
      ObjectOutputStream objectOutput = new ObjectOutputStream(gs);
      objectOutput.writeObject(pwm);
      gs.close();

      return ser.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public PseudonymWithMetadata unserializePseudonym(byte[] data, URI pseudonymUID) {
    PseudonymWithMetadata pwm;
    int magicHeader = data[0];

    if (magicHeader == this.magicHeaderForScopeExclusive()) {
      return this.unserializeExclusivePseudonym(data, pseudonymUID);
    }
	  
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      int header = bais.read();
      if (header != magicHeader()) {
        throw new RuntimeException("Cannot unserialize this pseudonym: header was " + header
            + " expected header " + magicHeader());
      }
      GZIPInputStream gs = new GZIPInputStream(bais);
      ObjectInputStream objectInput = new ObjectInputStream(gs);
      return (PseudonymWithMetadata) objectInput.readObject();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int magicHeader() {
    return 68;
  }

}
