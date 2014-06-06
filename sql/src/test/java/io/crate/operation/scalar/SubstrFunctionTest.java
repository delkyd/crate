/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.operation.scalar;

import com.google.common.collect.ImmutableList;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.FunctionInfo;
import io.crate.metadata.Functions;
import io.crate.metadata.Scalar;
import io.crate.operation.Input;
import io.crate.planner.symbol.Function;
import io.crate.planner.symbol.Literal;
import io.crate.planner.symbol.Symbol;
import io.crate.types.DataType;
import io.crate.types.DataTypes;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static io.crate.testing.TestingHelpers.assertLiteralSymbol;
import static io.crate.testing.TestingHelpers.createFunction;
import static io.crate.testing.TestingHelpers.createReference;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SubstrFunctionTest {

    private Functions functions;

    @Before
    public void setUp() {
        functions = new ModulesBuilder()
                .add(new ScalarFunctionModule()).createInjector().getInstance(Functions.class);
    }

    private final SubstrFunction funcA = new SubstrFunction(
            new FunctionInfo(new FunctionIdent(SubstrFunction.NAME, ImmutableList.<DataType>of(DataTypes.STRING, DataTypes.LONG)),
                    DataTypes.STRING));

    private final SubstrFunction funcB = new SubstrFunction(
            new FunctionInfo(new FunctionIdent(SubstrFunction.NAME, ImmutableList.<DataType>of(DataTypes.STRING, DataTypes.LONG, DataTypes.LONG)),
                    DataTypes.STRING));


    private Function substr(String str, long startIndex) {
        return new Function(funcA.info(),
                ImmutableList.<Symbol>of(Literal.newLiteral(str), Literal.newLiteral(startIndex)));
    }

    private Function substr(String str, long startIndex, long count) {
        return new Function(funcB.info(),
                ImmutableList.<Symbol>of(Literal.newLiteral(str), Literal.newLiteral(startIndex), Literal.newLiteral(count)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNormalizeSymbol() throws Exception {

        Function function = substr("cratedata", 0L);
        Symbol result = funcA.normalizeSymbol(function);
        assertLiteralSymbol(result, "cratedata");

        function = substr("cratedata", 6L);
        result = funcA.normalizeSymbol(function);
        assertLiteralSymbol(result, "data");

        function = substr("cratedata", 10L);
        result = funcA.normalizeSymbol(function);
        assertLiteralSymbol(result, "");

        function = substr("cratedata", 1L, 1L);
        result = funcB.normalizeSymbol(function);
        assertLiteralSymbol(result, "c");

        function = substr("cratedata", 3L, 2L);
        result = funcB.normalizeSymbol(function);
        assertLiteralSymbol(result, "at");

        function = substr("cratedata", 6L, 10L);
        result = funcB.normalizeSymbol(function);
        assertLiteralSymbol(result, "data");

        function = substr("cratedata", 6L, 0L);
        result = funcB.normalizeSymbol(function);
        assertLiteralSymbol(result, "");

        function = substr("cratedata", 10L, -1L);
        result = funcB.normalizeSymbol(function);
        assertLiteralSymbol(result, "");

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEvaluate() throws Exception {
        final Literal<Long> startPos = Literal.newLiteral(6L);

        List<Symbol> args = Arrays.<Symbol>asList(
                createReference("tag", DataTypes.STRING),
                startPos
        );
        Function function = createFunction(SubstrFunction.NAME, DataTypes.STRING, args);
        Scalar<BytesRef, Object> format = (Scalar<BytesRef, Object>) functions.get(function.info().ident());

        Input<Object> arg1 = new Input<Object>() {
            @Override
            public Object value() {
                return new BytesRef("cratedata");
            }
        };
        Input<Object> arg2 = new Input<Object>() {
            @Override
            public Object value() {
                return startPos.value();
            }
        };

        BytesRef result = format.evaluate(arg1, arg2);
        assertThat(result.utf8ToString(), is("data"));

        final Literal<Long> count = Literal.newLiteral(2L);

        args = Arrays.<Symbol>asList(
                createReference("tag", DataTypes.STRING),
                startPos,
                count
        );
        function = createFunction(SubstrFunction.NAME, DataTypes.STRING, args);
        format = (Scalar<BytesRef, Object>) functions.get(function.info().ident());

        Input<Object> arg3 = new Input<Object>() {
            @Override
            public Object value() {
                return count.value();
            }
        };

        result = format.evaluate(arg1, arg2, arg3);
        assertThat(result.utf8ToString(), is("da"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEvaluateWithArgsAsNonLiterals() throws Exception {
        List<Symbol> args = Arrays.<Symbol>asList(
                createReference("tag", DataTypes.STRING),
                createReference("start", DataTypes.LONG),
                createReference("end", DataTypes.LONG)
        );
        Function function = createFunction(SubstrFunction.NAME, DataTypes.STRING, args);
        Scalar<BytesRef, Object> format = (Scalar<BytesRef, Object>) functions.get(function.info().ident());

        Input<Object> arg1 = new Input<Object>() {
            @Override
            public Object value() {
                return new BytesRef("cratedata");
            }
        };
        Input<Object> arg2 = new Input<Object>() {
            @Override
            public Object value() {
                return 1L;
            }
        };
        Input<Object> arg3 = new Input<Object>() {
            @Override
            public Object value() {
                return 5L;
            }
        };

        BytesRef result = format.evaluate(arg1, arg2, arg3);
        assertThat(result.utf8ToString(), is("crate"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEvaluateWithArgsAsNonLiteralsIntShort() throws Exception {
        List<Symbol> args = Arrays.<Symbol>asList(
                createReference("tag", DataTypes.STRING),
                createReference("start", DataTypes.INTEGER),
                createReference("end", DataTypes.SHORT)
        );
        Function function = createFunction(SubstrFunction.NAME, DataTypes.STRING, args);
        Scalar<BytesRef, Object> format = (Scalar<BytesRef, Object>) functions.get(function.info().ident());

        Input<Object> arg1 = new Input<Object>() {
            @Override
            public Object value() {
                return new BytesRef("cratedata");
            }
        };
        Input<Object> arg2 = new Input<Object>() {
            @Override
            public Object value() {
                return 1;
            }
        };
        Input<Object> arg3 = new Input<Object>() {
            @Override
            public Object value() {
                return 5;
            }
        };

        BytesRef result = format.evaluate(arg1, arg2, arg3);
        assertThat(result.utf8ToString(), is("crate"));

    }
}