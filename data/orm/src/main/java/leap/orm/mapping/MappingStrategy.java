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
package leap.orm.mapping;

import leap.db.model.DbColumn;
import leap.lang.beans.BeanProperty;
import leap.orm.OrmContext;
import leap.orm.domain.FieldDomain;
import leap.orm.metadata.MetadataContext;
import leap.orm.metadata.MetadataException;
import leap.orm.model.Model;

public interface MappingStrategy {
	
	/**
	 * Returns <code>true</code> if the given class is the model in the given orm context(datasource).
	 */
	boolean isContextModel(OrmContext context, Class<?> cls);
	
	/**
	 * Returns <code>true</code> if the given java type declared as an entity type explicitly.
	 * 
	 * <p>
	 * Returns <code>false</code> if the given java type not declared as an entity type explicitly.
	 */
	boolean isExplicitEntity(MetadataContext context,Class<?> entityType);
	
	/**
	 * Returns <code>true</code> if the given java type declared as not an entity type explicitly.
	 * 
	 * <p>
	 * Returns <code>false</code> if the given java type not declared as not an entity type explicitly.
	 */
	boolean isExplicitNonEntity(MetadataContext context,Class<?> entityType);
	
	/**
	 * Returns <code>true</code> if the given java bean's property declared as an entity field explicitly.
	 * 
	 * <p>
	 * Returns <code>false</code> if the given java bean's property not declared as an entity field explicitly.
	 */
	boolean isExplicitField(MetadataContext context,BeanProperty beanProperty);
	
	/**
	 * Returns <code>true</code> if the given java bean's property can be an entity field in conventional.
	 */
	boolean isConventionalField(MetadataContext context,BeanProperty beanProperty);
	
	/**
	 * Returns <code>true</code> if the given java bean's property declared as not an entity field explicitly.
	 * 
	 * <p>
	 * Returns <code>false</code> if the given java bean's property not declared as not an entity field explicitly.
	 */
	boolean isExplicitNonField(MetadataContext context,BeanProperty beanProperty);
	
	/**
	 * Returns <code>true</code> if the given java bean's property declared as an entity relation explicitly.
	 */
	boolean isExplicitRelation(MetadataContext context,BeanProperty beanProperty);
	
	/**
	 * Returns <code>true</code> if the given column was auto generated by this strategy.
	 */
	boolean isAutoGeneratedColumn(MetadataContext context, DbColumn column);
	
	/**
	 * Returns a new {@link EntityMappingBuilder} holds all the mapping info mapped from a <code>class</code> to an entity type and the underlying db table.
	 */
	EntityMappingBuilder createEntityClassMapping(MetadataContext context, Class<?> entityType) throws MetadataException;
	
	/**
	 * 
	 */
	EntityMappingBuilder createModelMapping(MetadataContext context, Class<? extends Model> modelClass) throws MetadataException;
	
	/**
	 * 
	 */
	FieldMappingBuilder createFieldMappingByColumn(MetadataContext context,EntityMappingBuilder emb, DbColumn column);

    /**
     *
     */
    FieldMappingBuilder createFieldMappingByTemplate(MetadataContext context,EntityMappingBuilder emb, FieldMappingBuilder template);
	
	/**
	 * 
	 */
	FieldMappingBuilder createFieldMappingByDomain(MetadataContext context,EntityMappingBuilder emb,String domainName);
	
	/**
	 * 
	 */
	FieldMappingBuilder createFieldMappingByJoinField(MetadataContext       context,
													EntityMappingBuilder    localEntity,
													EntityMappingBuilder    targetEntity,
													RelationMappingBuilder  relation,
													JoinFieldMappingBuilder joinField);
	
	/**
	 * 
	 */
	void updateFieldMappingByJoinField(MappingConfigContext context, 
									   EntityMappingBuilder emb, 
									   EntityMappingBuilder targetEmb,
									   RelationMappingBuilder rmb, 
									   JoinFieldMappingBuilder jfmb, 
									   FieldMappingBuilder lfmb);


	/**
	 * Configures the attributes of the given {@link FieldMappingBuilder} by the given {@link FieldDomain}.
	 */
	void configFieldMappingByDomain(EntityMappingBuilder emb, FieldMappingBuilder fmb,FieldDomain domain);


}