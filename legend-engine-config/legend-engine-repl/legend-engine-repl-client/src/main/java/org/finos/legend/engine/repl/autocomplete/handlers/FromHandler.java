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

package org.finos.legend.engine.repl.autocomplete.handlers;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompletionItem;
import org.finos.legend.engine.repl.autocomplete.FunctionHandler;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;

public class FromHandler extends FunctionHandler
{
    @Override
    public String functionName()
    {
        return "from";
    }

    @Override
    public MutableList<CompletionItem> proposedParameters(AppliedFunction currentFunc, GenericType leftType, PureModel pureModel, Completer completer, ProcessingContext processingContext, ValueSpecification currentVS)
    {
        RichIterable<Root_meta_pure_runtime_PackageableRuntime> runtimes;
        if (currentFunc.parameters.size() == 1)
        {
            runtimes = pureModel.getAllRuntimes();
        }
        else
        {
            String path = ((PackageableElementPtr) currentFunc.parameters.get(1)).fullPath;
            runtimes = pureModel.getAllRuntimes().select(c -> PackageableElement.getUserPathForPackageableElement(c).startsWith(path));
        }
        return runtimes.collect(c -> new CompletionItem(PackageableElement.getUserPathForPackageableElement(c), PackageableElement.getUserPathForPackageableElement(c) + ")")).toList();
    }
}
