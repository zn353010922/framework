/*
 * Copyright 2013 the original author or authors.
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
package leap.orm.value;

import java.util.Map;

import leap.lang.Args;
import leap.lang.Named;
import leap.lang.params.ParamsMap;

@SuppressWarnings("unchecked")
public class Entity extends ParamsMap implements EntityBase {

	private static final long serialVersionUID = 5313900644420987429L;
	
	protected final String entityName;

	public Entity(String name){
		Args.notEmpty(name,"entity name");
		this.entityName = name;
	}
	
	public Entity(String name,Map<String,Object> fields){
		Args.notEmpty(name,"entity name");
		this.entityName = name;
		this.putAll(fields);
	}
	
	/**
	 * Returns the entity name.
	 */
	public String getEntityName() {
		return entityName;
	}
	
	@Override
    public Object get(String field) {
	    return get((Object)field);
    }
	
	@Override
    public Object get(Named field) {
	    return get((Object)field.getName());
    }
	
    @Override
    public Entity set(String field, Object value) {
		put(field,value);
	    return this;
    }

	@Override
    public Entity set(Named field, Object value) {
		put(field.getName(),value);
	    return this;
    }

	@Override
    public boolean hasField(String field) {
	    return containsKey(field);
    }
}