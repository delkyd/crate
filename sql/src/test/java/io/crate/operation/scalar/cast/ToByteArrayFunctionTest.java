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

package io.crate.operation.scalar.cast;

import com.google.common.collect.ImmutableList;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.Functions;
import io.crate.operation.Input;
import io.crate.operation.scalar.ScalarFunctionModule;
import io.crate.planner.symbol.Function;
import io.crate.planner.symbol.Literal;
import io.crate.planner.symbol.Symbol;
import io.crate.types.ArrayType;
import io.crate.types.DataType;
import io.crate.types.DataTypes;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ToByteArrayFunctionTest {

    private Functions functions;

    @Before
    public void setUp() throws Exception {
        functions = new ModulesBuilder()
                .add(new ScalarFunctionModule()).createInjector().getInstance(Functions.class);
    }

    @Test
    public void testArrayDifferentTypes() throws Exception {
        Object[] expected = new Byte[] { 10, 20, 30 };
        Object[] actual;

        actual = eval(new String[]{"10", "20", "30"}, DataTypes.STRING);
        assertThat(actual, is(expected));
        actual = eval(new BytesRef[]{new BytesRef("10"), new BytesRef("20"), new BytesRef("30")}, DataTypes.STRING);
        assertThat(actual, is(expected));

        actual = eval(new Double[] { 10.5d, 20.3d, 30d }, DataTypes.LONG);
        assertThat(actual, is(expected));
    }


    private Object[] eval(final Object objects, DataType innerType) {
        final DataType arrayType = new ArrayType(innerType);
        ToByteArrayFunction impl = (ToByteArrayFunction)functions.get(
                new FunctionIdent(ToByteArrayFunction.NAME, ImmutableList.of(arrayType)));

        Literal input = new Literal() {
            @Override
            public Object value() {
                return objects;
            }

            @Override
            public DataType valueType() {
                return arrayType;
            }
        };
        Symbol normalized = impl.normalizeSymbol(new Function(impl.info(), ImmutableList.<Symbol>of(input)));
        Object[] integers = impl.evaluate(new Input[]{input});

        assertThat(integers, is(((Input) normalized).value()));
        return integers;
    }
}