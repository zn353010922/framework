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
package leap.db;

import leap.lang.Args;
import leap.lang.Strings;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.lang.resource.Resource;
import leap.lang.resource.Resources;

import javax.sql.DataSource;

public abstract class DbBase implements Db {
	
	protected final Log log;

	protected final String	     name;
	protected final String		 description;
	protected final DbPlatform   platform;
	protected final DataSource   dataSource;
	protected final DbDialect    dialect;
	protected final DbMetadata   metadata;
	protected final DbComparator comparator;
	
	protected DbBase(String name,DbPlatform platform, DataSource dataSource,DbMetadata metadata,DbDialect dialect, DbComparator comparator){
		Args.notEmpty(name);
		Args.notNull(platform);
		Args.notNull(dataSource);
		Args.notNull(metadata);
		Args.notNull(dialect);
		Args.notNull(comparator);
		
		this.name        = name;
		this.description = metadata.getProductName() + " " + metadata.getProductVersion();
		this.platform	 = platform;
		this.dataSource  = dataSource;
		this.metadata    = metadata;
		this.dialect     = dialect;
		this.comparator  = comparator;
		this.awareObjects();
		
		this.log = getLog(this.getClass());

        this.init();
	}
	
	@Override
    public String getName() {
	    return name;
    }
	
	@Override
    public String getType() {
	    return platform.getName();
    }

	@Override
    public String getDescription() {
	    return description;
    }
	
	@Override
    public DbPlatform getPlatform() {
	    return platform;
    }

	@Override
    public DbDialect getDialect() {
	    return dialect;
    }
	
	@Override
    public DbMetadata getMetadata() {
	    return metadata;
    }
	
	@Override
    public DbComparator getComparator() {
	    return comparator;
    }

	@Override
    public DataSource getDataSource() {
	    return dataSource;
    }
	
	public Log getLog(Class<?> cls){
		return LogFactory.get(cls.getName() + "(" + platform.getName() + ":" + name + ")"); 
	}

    protected void awareObjects(){
        if(null != dataSource && dataSource instanceof DbAware){
            ((DbAware)dataSource).setDb(this);
        }

        if(dialect instanceof DbAware){
            ((DbAware)dialect).setDb(this);
        }

        if(metadata instanceof DbAware){
            ((DbAware)metadata).setDb(this);
        }

        if(comparator instanceof DbAware){
            ((DbAware) comparator).setDb(this);
        }
    }

    protected void init() {
        Resource initSqlFile = findClasspathSql("init");
        if(null != initSqlFile) {
            String sqls = initSqlFile.getContent();
            if(!Strings.isEmpty(sqls)) {
                log.info("Init db '{}' by sql file '{}'", name + initSqlFile.getClasspath());
                createExecution().add(sqls).execute();
            }
        }
    }

    protected Resource findClasspathSql(String filename) {
        final String prefix = "classpath:/conf/db/" + name + "/" + filename;

        Resource resource = Resources.getResource(prefix + "_" + getType().toLowerCase() + ".sql");
        if(null != resource && resource.exists()) {
            return resource;
        }

        resource = Resources.getResource(prefix + ".sql");
        if(null != resource && resource.exists()) {
            return resource;
        }

        return null;
    }
}
