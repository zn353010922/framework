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
package leap.web.download;

import leap.lang.resource.Resource;
import leap.web.Request;
import leap.web.Response;

public class ResourceDownload extends AbstractDownload {
	
	protected Resource resource;

	public ResourceDownload(Resource resource) {
		this(resource, resource.getFilename());
	}
	
	public ResourceDownload(Resource resource, String filename) {
		this.resource = resource;
		this.filename = filename;
    }

	@Override
    protected Resource getResource(Request request, Response response) throws Throwable {
		return resource;
	}

}
