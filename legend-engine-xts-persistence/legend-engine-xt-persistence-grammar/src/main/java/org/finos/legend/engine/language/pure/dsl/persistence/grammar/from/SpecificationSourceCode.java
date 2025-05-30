// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;

public class SpecificationSourceCode
{
    private final String code;
    private final String type;
    private final SourceInformation sourceInformation;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public SpecificationSourceCode(String code, String type, SourceInformation sourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.code = code;
        this.type = type;
        this.sourceInformation = sourceInformation;
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public String getCode()
    {
        return code;
    }

    public String getType()
    {
        return type;
    }

    public SourceInformation getSourceInformation()
    {
        return sourceInformation;
    }

    public ParseTreeWalkerSourceInformation getWalkerSourceInformation()
    {
        return walkerSourceInformation;
    }
}
