/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.extension.oauth.github;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.web.security.security.SecureRandomService;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.exception.OAuthExceptionCode;
import org.gatein.security.oauth.spi.InteractionState;
import org.gatein.security.oauth.spi.OAuthCodec;
import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.spi.OAuthProviderType;
import org.gatein.security.oauth.utils.HttpResponseContext;
import org.gatein.security.oauth.utils.OAuthPersistenceUtils;
import org.gatein.security.oauth.utils.OAuthUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Nov
 * 10, 2015
 */
public class GithubProcessorImpl implements GithubProcessor {
  
  public static final String AUTHENTICATION_ENDPOINT_URL = "https://github.com/login/oauth/authorize";
  public static final String ACCESS_TOKEN_ENDPOINT_URL = "https://github.com/login/oauth/access_token";
  public static final String PROFILE_ENDPOINT_URL = "https://api.github.com/user";
  
  public static final String PROFILE_GITHUB_ACCESS_TOKEN = "user.social-info.github.accessToken";
  public static final String USERNAME_JSON_KEY = "login";
  public static final String DISPLAYNAME_JSON_KEY = "name";
  public static final String EMAIL_JSON_KEY = "email";

  private static final Log LOG = ExoLogger.getLogger(GithubProcessorImpl.class);

  private final String     redirectURL;
  private final String     clientID;
  private final String     clientSecret;
  private final int        chunkLength;
  private final SecureRandomService secureRandomService;

  public GithubProcessorImpl(ExoContainerContext context, InitParams params, SecureRandomService secureRandomService) {
    String redirectURL_ = params.getValueParam("redirectURL").getValue();
    String clientID_ = params.getValueParam("clientId").getValue();
    String clientSecret_ = params.getValueParam("clientSecret").getValue();
    if (redirectURL_ == null || redirectURL_.length() == 0 || clientID_ == null
        || clientID_.length() == 0 || clientSecret_ == null || clientSecret_.length() == 0) {
      throw new IllegalArgumentException("redirectURL, clientId and clientSecret must not be empty!");
    }
    this.redirectURL = redirectURL_;
    this.clientID = clientID_;
    this.clientSecret = clientSecret_;
    this.chunkLength = OAuthPersistenceUtils.getChunkLength(params);
    this.secureRandomService = secureRandomService;
  }
  
  @Override
  public InteractionState<GithubAccessTokenContext> processOAuthInteraction(HttpServletRequest request, 
                                                                            HttpServletResponse response) throws IOException, OAuthException {
    HttpSession session = request.getSession();
    String state = (String) session.getAttribute(OAuthConstants.ATTRIBUTE_AUTH_STATE);
    
    // start the flow
    if (state == null || state.isEmpty()) {
      String verificationState = String.valueOf(secureRandomService.getSecureRandom().nextLong());
      initialInteraction(request, response, verificationState);
      state = InteractionState.State.AUTH.name();
      session.setAttribute(OAuthConstants.ATTRIBUTE_AUTH_STATE, state);
      session.setAttribute(OAuthConstants.ATTRIBUTE_VERIFICATION_STATE, verificationState);
      return new InteractionState<GithubAccessTokenContext>(InteractionState.State.valueOf(state), null);
    }
    
    // get access token
    if (state.equals(InteractionState.State.AUTH.name())) {
      //
    }
    return new InteractionState<GithubAccessTokenContext>(InteractionState.State.valueOf(state), null);
  }
  
  @Override
  public InteractionState<GithubAccessTokenContext> processOAuthInteraction(HttpServletRequest request,
                                                                            HttpServletResponse response, String scopes) throws IOException, OAuthException {
    return processOAuthInteraction(request, response);
  }
  
  @Override
  public void revokeToken(GithubAccessTokenContext accessToken) throws OAuthException {
    // nothing to do
  }
  
  @Override
  public GithubAccessTokenContext validateTokenAndUpdateScopes(GithubAccessTokenContext accessToken) throws OAuthException {
    return accessToken;
  }
  
  @Override
  public <C> C getAuthorizedSocialApiObject(GithubAccessTokenContext accessToken, Class<C> socialApiObjectType) {
    return null;
  }
  
  @Override
  public void saveAccessTokenAttributesToUserProfile(UserProfile userProfile, OAuthCodec codec, GithubAccessTokenContext accessToken) {
    String encodedAccessToken = codec.encodeString(accessToken.getAccessToken());
    OAuthPersistenceUtils.saveLongAttribute(encodedAccessToken, userProfile, PROFILE_GITHUB_ACCESS_TOKEN, false, chunkLength);
  }
  
  @Override
  public GithubAccessTokenContext getAccessTokenFromUserProfile(UserProfile userProfile, OAuthCodec codec) {
    String encodedAccessToken = OAuthPersistenceUtils.getLongAttribute(userProfile, PROFILE_GITHUB_ACCESS_TOKEN, false);
    if (encodedAccessToken == null) {
      return null;
    }
    String accessToken = codec.decodeString(encodedAccessToken);
    return new GithubAccessTokenContext(accessToken);
  }
  
  @Override
  public void removeAccessTokenFromUserProfile(UserProfile userProfile) {
    OAuthPersistenceUtils.removeLongAttribute(userProfile, PROFILE_GITHUB_ACCESS_TOKEN, true);
  }
  
  public void initialInteraction(HttpServletRequest request, HttpServletResponse response, String verificationState) throws IOException {
    Map<String, String> params = new HashMap<String, String>();
    params.put(OAuthConstants.REDIRECT_URI_PARAMETER, redirectURL);
    params.put(OAuthConstants.CLIENT_ID_PARAMETER, clientID);
    params.put(OAuthConstants.ATTRIBUTE_VERIFICATION_STATE, verificationState);
    
    String location = new StringBuilder(AUTHENTICATION_ENDPOINT_URL).append("?").append(OAuthUtils.createQueryString(params)).toString();
    response.sendRedirect(location);
  }
  
  public String getAccessToken(HttpServletRequest request, HttpServletResponse response, String verificationState) {
    String authorizationCode = request.getParameter(OAuthConstants.CODE_PARAMETER);
    if (authorizationCode == null) {
      LOG.error("Authorization code not found!");
      handleCodeRequestError(request, response);
      return null;
    }
    String sessionVerificationState = (String)request.getSession().getAttribute(OAuthConstants.ATTRIBUTE_VERIFICATION_STATE);
    String requestVerificationState = request.getParameter(OAuthConstants.STATE_PARAMETER);
    if (sessionVerificationState == null || requestVerificationState == null || !sessionVerificationState.equals(requestVerificationState)) {
      throw new OAuthException(OAuthExceptionCode.INVALID_STATE, "State validation failed!");
    }
    
    Map<String, String> params = new HashMap<String, String>();
    params.put(OAuthConstants.REDIRECT_URI_PARAMETER, redirectURL);
    params.put(OAuthConstants.CLIENT_ID_PARAMETER, clientID);
    params.put(OAuthConstants.ATTRIBUTE_VERIFICATION_STATE, verificationState);
    params.put(OAuthConstants.CLIENT_SECRET_PARAMETER, clientSecret);
    params.put(OAuthConstants.CODE_PARAMETER, authorizationCode);
    
    String location = new StringBuilder(ACCESS_TOKEN_ENDPOINT_URL).append("?").append(OAuthUtils.createQueryString(params)).toString();
    try {
      URL url = new URL(location);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST"); //Github specifies the method
    
      HttpResponseContext responseContext = OAuthUtils.readUrlContent(connection);
      if (responseContext.getResponseCode() == 200) {
        return parseAccessToken(responseContext.getResponse());
      } else if (responseContext.getResponseCode() == 400) {
        throw new OAuthException(OAuthExceptionCode.ACCESS_TOKEN_ERROR, responseContext.getResponse());
      } else {
        String errorMessage = "Unspecified IO error. Http response code: " + responseContext.getResponseCode() + ", details: " + responseContext.getResponse();
        throw new OAuthException(OAuthExceptionCode.IO_ERROR, errorMessage);
      }
    } catch (JSONException e) {
      throw new OAuthException(OAuthExceptionCode.IO_ERROR, e);
    } catch (IOException e) {
      throw new OAuthException(OAuthExceptionCode.IO_ERROR, e);
    }
    
  }
  
  public void handleCodeRequestError(HttpServletRequest request, HttpServletResponse response) {
    //
  }
  
  public String parseAccessToken(String httpResponse) throws JSONException {
    JSONObject jsonObject = new JSONObject(httpResponse);
    return jsonObject.getString(OAuthConstants.ACCESS_TOKEN_PARAMETER);
  }
  
  public OAuthPrincipal<GithubAccessTokenContext> getPrincipal(GithubAccessTokenContext accessTokenContext, OAuthProviderType<GithubAccessTokenContext> providerType) {
    String accessToken = accessTokenContext.getAccessToken();
    Map<String, String> params = new HashMap<String, String>();
    params.put(OAuthConstants.ACCESS_TOKEN_PARAMETER, accessToken);
    String location = new StringBuilder(PROFILE_ENDPOINT_URL).append("?").append(OAuthUtils.createQueryString(params)).toString();
    try {
      URL url = new URL(location);
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      HttpResponseContext responseContext = OAuthUtils.readUrlContent(connection);
      if (responseContext.getResponseCode() == 200) {
        return parsePrincipal(responseContext.getResponse(), accessTokenContext, providerType);
      } else {
        String errorMessage = "Unspecified IO error. Http response code: " + responseContext.getResponseCode() + ", details: " + responseContext.getResponse();
        throw new OAuthException(OAuthExceptionCode.IO_ERROR, errorMessage);
      }
    } catch (JSONException e) {
      throw new OAuthException(OAuthExceptionCode.IO_ERROR, e);
    } catch (IOException e) {
      throw new OAuthException(OAuthExceptionCode.IO_ERROR, e);
    }
  }
  
  public OAuthPrincipal<GithubAccessTokenContext> parsePrincipal(String response, GithubAccessTokenContext accessTokenContext,
                                                                 OAuthProviderType<GithubAccessTokenContext> providerType) throws JSONException {
    JSONObject jsonObject = new JSONObject(response);
    String userName = jsonObject.getString(USERNAME_JSON_KEY);
    String displayName = jsonObject.getString(DISPLAYNAME_JSON_KEY);
    String firstName = displayName; //Github profile doesn't contain first and last name.
    String lastName = "";
    String email = jsonObject.getString(EMAIL_JSON_KEY);
    
    return new OAuthPrincipal<GithubAccessTokenContext>(userName, firstName, lastName, displayName, email, accessTokenContext, providerType);
  }

}
