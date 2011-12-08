/*
 *  soapUI, copyright (C) 2004-2011 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.wsdl.support.wss.saml.callback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoType;
import org.apache.ws.security.saml.ext.SAMLCallback;
import org.apache.ws.security.saml.ext.bean.KeyInfoBean;
import org.apache.ws.security.saml.ext.bean.SubjectBean;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.opensaml.common.SAMLVersion;

import com.eviware.soapui.impl.wsdl.support.wss.entries.AutomaticSAMLEntry;

/**
 * @author Erik R. Yverling
 * 
 *         A Callback Handler implementation for a SAML 2 assertion. By default
 *         it creates an authentication assertion using Sender Vouches.
 */
public class SAML2CallbackHandler extends AbstractSAMLCallbackHandler
{
	public SAML2CallbackHandler( Crypto crypto, String alias, String subjectName, String subjectQualifier )
			throws Exception
	{
		super( crypto, alias, subjectName, subjectQualifier );

		if( certs == null )
		{
			CryptoType cryptoType = new CryptoType( CryptoType.TYPE.ALIAS );
			cryptoType.setAlias( alias );
			certs = crypto.getX509Certificates( cryptoType );
		}
		// FIXME Should we always default to Sender Vouches?
		confirmationMethod = SAML2Constants.CONF_SENDER_VOUCHES;
	}

	public SAML2CallbackHandler( String subjectName, String subjectQualifier )
	{
		super( subjectName, subjectQualifier );
	}

	public void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException
	{
		for( int i = 0; i < callbacks.length; i++ )
		{
			if( callbacks[i] instanceof SAMLCallback )
			{
				SAMLCallback callback = ( SAMLCallback )callbacks[i];
				callback.setSamlVersion( SAMLVersion.VERSION_20 );
				callback.setIssuer( issuer );
				SubjectBean subjectBean = new SubjectBean( subjectName, subjectQualifier, confirmationMethod );
				if( subjectNameIDFormat != null )
				{
					subjectBean.setSubjectNameIDFormat( subjectNameIDFormat );
				}
				if( SAML2Constants.CONF_HOLDER_KEY.equals( confirmationMethod ) )
				{
					try
					{
						KeyInfoBean keyInfo = createKeyInfo();
						subjectBean.setKeyInfo( keyInfo );
					}
					catch( Exception ex )
					{
						throw new IOException( "Problem creating KeyInfo: " + ex.getMessage() );
					}
				}
				callback.setSubject( subjectBean );
				createAndSetStatement( null, callback );
			}
			else
			{
				throw new UnsupportedCallbackException( callbacks[i], "Unrecognized Callback" );
			}
		}
	}

	@Override
	public void setConfirmationMethod( String signingType )
	{
		if( signingType.equals( AutomaticSAMLEntry.HOLDER_OF_KEY_CONFIRMATION_METHOD ) )
		{
			confirmationMethod = SAML2Constants.CONF_HOLDER_KEY;
		}
		else if( signingType.equals( AutomaticSAMLEntry.SENDER_VOUCHES_CONFIRMATION_METHOD ) )
		{
			confirmationMethod = SAML2Constants.CONF_SENDER_VOUCHES;
		}
	}
}