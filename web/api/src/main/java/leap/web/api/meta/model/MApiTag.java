/*
 *
 *  * Copyright 2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package leap.web.api.meta.model;

import java.util.Map;

public class MApiTag extends MApiNamedWithDesc {

    public MApiTag(String name) {
        super(name);
    }

    public MApiTag(String name, String title) {
        super(name, title);
    }

    public MApiTag(String name, String title, String summary, String description, Map<String, Object> attrs) {
        super(name, title, summary, description, attrs);
    }

}