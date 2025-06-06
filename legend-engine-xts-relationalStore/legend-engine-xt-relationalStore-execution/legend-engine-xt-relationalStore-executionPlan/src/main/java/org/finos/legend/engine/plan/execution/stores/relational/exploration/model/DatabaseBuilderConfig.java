// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.plan.execution.stores.relational.exploration.model;

import org.eclipse.collections.api.factory.Lists;

import java.util.List;

public class DatabaseBuilderConfig
{
    public Integer maxTables;

    public boolean enrichTables;

    public boolean enrichPrimaryKeys;

    public boolean enrichColumns;
    
    public boolean enrichTableFunctions = false;

    public List<DatabasePattern> patterns = Lists.mutable.empty();

    public void setPatterns(List<DatabasePattern> patterns)
    {
        this.patterns = patterns;
    }

    public List<DatabasePattern> getPatterns()
    {
        return patterns;
    }
}
