/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.extension.oauth.github;

import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.exception.OAuthException;
import org.gatein.security.oauth.exception.OAuthExceptionCode;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.spi.InteractionState;
import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.spi.OAuthProviderType;
import org.gatein.security.oauth.utils.HttpResponseContext;
import org.gatein.security.oauth.utils.OAuthUtils;
import org.gatein.security.oauth.web.OAuthProviderFilter;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class GithubFilter extends OAuthProviderFilter<GithubAccessTokenContext> {
  @Override
  protected OAuthProviderType<GithubAccessTokenContext> getOAuthProvider() {
    return this.getOauthProvider("GITHUB", GithubAccessTokenContext.class);
  }

  @Override
  protected void initInteraction(HttpServletRequest request, HttpServletResponse response) {
    HttpSession session = request.getSession();
    session.removeAttribute(OAuthConstants.ATTRIBUTE_AUTH_STATE);
    session.removeAttribute(OAuthConstants.ATTRIBUTE_VERIFICATION_STATE);
  }

  @Override
  protected OAuthPrincipal<GithubAccessTokenContext> getOAuthPrincipal(HttpServletRequest request, HttpServletResponse response, InteractionState<GithubAccessTokenContext> interactionState) {
    GithubAccessTokenContext accessTokenContext = interactionState.getAccessTokenContext();
    String accessToken = accessTokenContext.getAccessToken();
    Map<String, String> params = new HashMap<String, String>();
    params.put(OAuthConstants.ACCESS_TOKEN_PARAMETER, accessToken);
    String location = new StringBuilder(GithubProcessorImpl.PROFILE_ENDPOINT_URL).append("?").append(OAuthUtils.createQueryString(params)).toString();
    try {
      URL url = new URL(location);
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();
      HttpResponseContext responseContext = OAuthUtils.readUrlContent(connection);
      if (responseContext.getResponseCode() == 200) {
        return parsePrincipal(responseContext.getResponse(), accessTokenContext, this.getOAuthProvider());
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
    String userName = jsonObject.getString(GithubProcessorImpl.USERNAME_JSON_KEY);
    String displayName = jsonObject.getString(GithubProcessorImpl.DISPLAYNAME_JSON_KEY);
    String firstName = displayName; //Github profile doesn't contain first and last name.
    String lastName = "";
    String email = jsonObject.getString(GithubProcessorImpl.EMAIL_JSON_KEY);

    return new OAuthPrincipal<GithubAccessTokenContext>(userName, firstName, lastName, displayName, email, accessTokenContext, providerType);
  }
}
