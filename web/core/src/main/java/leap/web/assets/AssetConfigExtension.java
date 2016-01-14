/*
 * Copyright 2016 the original author or authors.
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
package leap.web.assets;

import java.util.ArrayList;
import java.util.List;

public class AssetConfigExtension {

    private List<AssetFolderConfig> folders = new ArrayList<>();

    public AssetConfigExtension addFolder(AssetFolderConfig folder) {
        folders.add(folder);
        return this;
    }

    public List<AssetFolderConfig> getFolders() {
        return folders;
    }

    public static class AssetFolderConfig {
        private String location;
        private String pathPrefix;

        public AssetFolderConfig() {

        }

        public AssetFolderConfig(String location, String pathPrefix) {
            this.location   = location;
            this.pathPrefix = pathPrefix;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getPathPrefix() {
            return pathPrefix;
        }

        public void setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }
    }
}
