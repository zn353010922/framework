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
package leap.oauth2.rs.auth;

import leap.core.security.ClientPrincipal;

public class ResClientPrincipal implements ClientPrincipal {
	
	private static final long serialVersionUID = -3438856033102626691L;
	
	protected String clientId;

	public ResClientPrincipal(String clientId) {
	    this.clientId = clientId;
	}

	@Override
    public Object getId() {
	    return clientId;
	}

}