/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.oauth2.rs.token;

import leap.core.annotation.Inject;
import leap.core.security.token.TokenVerifyException;
import leap.core.security.token.jwt.JwtVerifier;
import leap.lang.Result;
import leap.oauth2.as.OAuth2AuthzServerConfig;
import leap.oauth2.rs.OAuth2ResServerConfig;
import leap.web.security.SecurityConfig;
import leap.web.security.user.UserDetails;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

/**
 * Created by KAEL on 2016/5/8.
 */
public class JwtBearerResAccessTokenStore implements ResBearerAccessTokenStore {

    protected RSAPublicKey               publicKey;
    protected @Inject SecurityConfig     sc;
    protected @Inject OAuth2AuthzServerConfig asc;
    protected @Inject OAuth2ResServerConfig rsc;


    @Override
    public Result<ResAccessTokenDetails> loadAccessTokenDetails(ResAccessToken token) {
        JwtVerifier verifier = rsc.getJwtVerifier();
        if(verifier == null){
            throw new TokenVerifyException(TokenVerifyException.ErrorCode.VERIFY_FAILED, "the jwt verifier must be specified!");
        }
        Map<String,Object> jwtDetail = verifier.verify(token.getToken());
        SimpleResAccessTokenDetails resAccessTokenDetails = new SimpleResAccessTokenDetails();
        UserDetails ud = sc.getUserStore().loadUserDetailsByLoginName((String)jwtDetail.remove("username"));
        resAccessTokenDetails.setUserId(ud==null?null:ud.getIdAsString());
        resAccessTokenDetails.setScope((String)jwtDetail.remove("scope"));
        resAccessTokenDetails.setClientId((String)jwtDetail.remove("client_id"));
        //TODO How to ensure is expired?
        resAccessTokenDetails.setCreated(System.currentTimeMillis());
        try {
            Object expiresIn = jwtDetail.get("expires_in");
            if(expiresIn == null){
                resAccessTokenDetails.setExpiresIn(asc.getDefaultAccessTokenExpires());
            }else{
                int second = expiresIn instanceof Integer?(Integer)expiresIn:Integer.parseInt(expiresIn.toString());
                resAccessTokenDetails.setExpiresIn(second * 1000);
            }
        } catch (NumberFormatException e) {
            resAccessTokenDetails.setExpiresIn(asc.getDefaultAccessTokenExpires());
        }
        return Result.of(resAccessTokenDetails);
    }

    @Override
    public void removeAccessToken(ResAccessToken token) {
        //Do nothing
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
