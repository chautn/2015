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

import java.io.Serializable;
import org.gatein.security.oauth.spi.AccessTokenContext;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 9, 2015  
 */
public class GithubAccessTokenContext extends AccessTokenContext implements Serializable {
  
  private static final long serialVersionUID = 42L;
  
  private final String accessToken;
  
  public GithubAccessTokenContext(String accessToken) {
    if (accessToken == null) {
      throw new IllegalArgumentException("accessToken must not be null!");
    }
    this.accessToken = accessToken;
  }
  
  @Override
  public String getAccessToken() {
    return accessToken;
  }
  
  @Override
  public boolean equals(Object that) {
    if (!(super.equals(that))) {
      return false;
    }
    GithubAccessTokenContext that_ = (GithubAccessTokenContext)that;
    return this.accessToken.equals(that_.getAccessToken()) ;
  }
  
  public int hashCode() {
    return super.hashCode() * 13 + accessToken.hashCode() * 11;
  }

}
