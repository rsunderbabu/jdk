/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.foreign.abi.x64.sysv;

import jdk.internal.foreign.Utils;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.PaddingLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class TypeClass {
    enum Kind {
        STRUCT,
        POINTER,
        INTEGER,
        FLOAT
    }

    private final Kind kind;
    final List<ArgumentClassImpl> classes;

    private TypeClass(Kind kind, List<ArgumentClassImpl> classes) {
        this.kind = kind;
        this.classes = classes;
    }

    public static TypeClass ofValue(ValueLayout layout) {
        final Kind kind;
        ArgumentClassImpl argClass = argumentClassFor(layout);
        kind = switch (argClass) {
            case POINTER -> Kind.POINTER;
            case INTEGER -> Kind.INTEGER;
            case SSE -> Kind.FLOAT;
            default -> throw new IllegalStateException("Unexpected argument class: " + argClass);
        };
        return new TypeClass(kind, List.of(argClass));
    }

    public static TypeClass ofStruct(GroupLayout layout) {
        return new TypeClass(Kind.STRUCT, classifyStructType(layout));
    }

    boolean inMemory() {
        return classes.stream().anyMatch(c -> c == ArgumentClassImpl.MEMORY);
    }

    private long numClasses(ArgumentClassImpl clazz) {
        return classes.stream().filter(c -> c == clazz).count();
    }

    public long nIntegerRegs() {
        return numClasses(ArgumentClassImpl.INTEGER) + numClasses(ArgumentClassImpl.POINTER);
    }

    public long nVectorRegs() {
        return numClasses(ArgumentClassImpl.SSE);
    }

    public Kind kind() {
        return kind;
    }

    // layout classification

    // The AVX 512 enlightened ABI says "eight eightbytes"
    // Although AMD64 0.99.6 states 4 eightbytes
    private static final int MAX_AGGREGATE_REGS_SIZE = 8;
    static final List<ArgumentClassImpl> COMPLEX_X87_CLASSES = List.of(
         ArgumentClassImpl.X87,
         ArgumentClassImpl.X87UP,
         ArgumentClassImpl.X87,
         ArgumentClassImpl.X87UP
    );

    private static List<ArgumentClassImpl> createMemoryClassArray(long size) {
        return IntStream.range(0, (int)size)
                .mapToObj(i -> ArgumentClassImpl.MEMORY)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static ArgumentClassImpl argumentClassFor(ValueLayout layout) {
        Class<?> carrier = layout.carrier();
        if (carrier == boolean.class || carrier == byte.class || carrier == char.class ||
                carrier == short.class || carrier == int.class || carrier == long.class) {
            return ArgumentClassImpl.INTEGER;
        } else if (carrier == float.class || carrier == double.class) {
            return ArgumentClassImpl.SSE;
        } else if (carrier == MemorySegment.class) {
            return ArgumentClassImpl.POINTER;
        } else {
            throw new IllegalStateException("Cannot get here: " + carrier.getName());
        }
    }

    // TODO: handle zero length arrays
    private static List<ArgumentClassImpl> classifyStructType(GroupLayout type) {
        List<ArgumentClassImpl>[] eightbytes = groupByEightBytes(type);
        long nWords = eightbytes.length;
        if (nWords > MAX_AGGREGATE_REGS_SIZE) {
            return createMemoryClassArray(nWords);
        }

        ArrayList<ArgumentClassImpl> classes = new ArrayList<>();

        for (int idx = 0; idx < nWords; idx++) {
            List<ArgumentClassImpl> subclasses = eightbytes[idx];
            ArgumentClassImpl result = subclasses.stream()
                    .reduce(ArgumentClassImpl.NO_CLASS, ArgumentClassImpl::merge);
            classes.add(result);
        }

        for (int i = 0; i < classes.size(); i++) {
            ArgumentClassImpl c = classes.get(i);

            if (c == ArgumentClassImpl.MEMORY) {
                // if any of the eightbytes was passed in memory, pass the whole thing in memory
                return createMemoryClassArray(classes.size());
            }

            if (c == ArgumentClassImpl.X87UP) {
                if (i == 0) {
                    throw new IllegalArgumentException("Unexpected leading X87UP class");
                }

                if (classes.get(i - 1) != ArgumentClassImpl.X87) {
                    return createMemoryClassArray(classes.size());
                }
            }
        }

        if (classes.size() > 2) {
            if (classes.get(0) != ArgumentClassImpl.SSE) {
                return createMemoryClassArray(classes.size());
            }

            for (int i = 1; i < classes.size(); i++) {
                if (classes.get(i) != ArgumentClassImpl.SSEUP) {
                    return createMemoryClassArray(classes.size());
                }
            }
        }

        return classes;
    }

    static TypeClass classifyLayout(MemoryLayout type) {
        try {
            if (type instanceof ValueLayout valueLayout) {
                return ofValue(valueLayout);
            } else if (type instanceof GroupLayout groupLayout) {
                return ofStruct(groupLayout);
            } else {
                throw new IllegalArgumentException("Unsupported layout: " + type);
            }
        } catch (UnsupportedOperationException e) {
            System.err.println("Failed to classify layout: " + type);
            throw e;
        }
    }

    private static List<ArgumentClassImpl>[] groupByEightBytes(GroupLayout group) {
        long offset = 0L;
        int nEightbytes;
        try {
            // alignUp can overflow the value, but it's okay since toIntExact still catches it
            nEightbytes = Math.toIntExact(Utils.alignUp(group.byteSize(), 8) / 8);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("GroupLayout is too large: " + group, e);
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<ArgumentClassImpl>[] groups = new List[nEightbytes];
        for (MemoryLayout l : group.memberLayouts()) {
            groupByEightBytes(l, offset, groups);
            if (group instanceof StructLayout) {
                offset += l.byteSize();
            }
        }
        return groups;
    }

    private static void groupByEightBytes(MemoryLayout layout,
                                          long offset,
                                          List<ArgumentClassImpl>[] groups) {
        switch (layout) {
            case GroupLayout group -> {
                for (MemoryLayout m : group.memberLayouts()) {
                    groupByEightBytes(m, offset, groups);
                    if (group instanceof StructLayout) {
                        offset += m.byteSize();
                    }
                }
            }
            case PaddingLayout  _ -> {
            }
            case SequenceLayout seq -> {
                MemoryLayout elem = seq.elementLayout();
                for (long i = 0; i < seq.elementCount(); i++) {
                    groupByEightBytes(elem, offset, groups);
                    offset += elem.byteSize();
                }
            }
            case ValueLayout vl -> {
                List<ArgumentClassImpl> layouts = groups[(int) offset / 8];
                if (layouts == null) {
                    layouts = new ArrayList<>();
                    groups[(int) offset / 8] = layouts;
                }
                // if the aggregate contains unaligned fields, it has class MEMORY
                ArgumentClassImpl argumentClass = (offset % vl.byteAlignment()) == 0 ?
                        argumentClassFor(vl) :
                        ArgumentClassImpl.MEMORY;
                layouts.add(argumentClass);
            }
            case null, default -> throw new IllegalStateException("Unexpected layout: " + layout);
        }
    }
}
