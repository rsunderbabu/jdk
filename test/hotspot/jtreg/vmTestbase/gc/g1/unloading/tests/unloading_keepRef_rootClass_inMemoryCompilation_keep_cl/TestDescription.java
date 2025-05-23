/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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


/*
 * @test
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/g1/unloading/tests/unloading_keepRef_rootClass_inMemoryCompilation_keep_cl.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, javac]
 *
 * @modules java.base/jdk.internal.misc
 * @library /testlibrary/asm
 * @library /vmTestbase
 *          /test/lib
 *
 *
 * @comment generate HumongousTemplateClass and compile it to test.classes
 * @run driver gc.g1.unloading.bytecode.GenClassesBuilder
 *
 * @comment build classPool.jar
 * @run driver gc.g1.unloading.GenClassesBuilder
 *
 * @requires vm.gc.G1
 * @requires vm.opt.ClassUnloading != false
 * @requires vm.opt.ClassUnloadingWithConcurrentMark != false
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI
 *      -XX:+UseG1GC
 *      -XX:+ExplicitGCInvokesConcurrent
 *      -Xbootclasspath/a:classPool.jar
 *      -Xlog:gc:gc.log
 *      -XX:-UseGCOverheadLimit
 *      gc.g1.unloading.UnloadingTest
 *      -keepRefMode STATIC_FIELD_OF_ROOT_CLASS
 *      -inMemoryCompilation
 *      -keep classloader
 *      -numberOfChecksLimit 4
 *      -stressTime 180
 */
