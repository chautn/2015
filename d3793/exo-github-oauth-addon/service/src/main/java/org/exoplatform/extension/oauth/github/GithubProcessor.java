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

import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.spi.OAuthProviderProcessor;
import org.gatein.security.oauth.spi.OAuthProviderType;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 10, 2015  
 */
public interface GithubProcessor extends OAuthProviderProcessor<GithubAccessTokenContext> {

  //TODO: this method should be implement in GithubFilter
  @Deprecated
  public OAuthPrincipal<GithubAccessTokenContext> getPrincipal(GithubAccessTokenContext accessTokenContext, 
                                                               OAuthProviderType<GithubAccessTokenContext> providerType);

}