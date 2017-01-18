/*
 *
 *  * Copyright 2013 the original author or authors.
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

package tested.base.injected;

import leap.core.annotation.Bean;

/**
 * Created by kael on 2016/12/27.
 */
@Bean(additionalTypeDef = {AbstractInjectBean.class},type = InjectInterface1.class)
public class InjectBean extends AbstractInjectBean implements InjectInterface1,InjectInterface2 {} 