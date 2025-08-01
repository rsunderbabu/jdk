/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * {@link HotSpotCompiledCode} destined for installation as an nmethod.
 */
public final class HotSpotCompiledNmethod extends HotSpotCompiledCode {

    protected final HotSpotResolvedJavaMethod method;
    protected final int entryBCI;

    /**
     * Compilation identifier.
     */
    protected final int id;

    /**
     * Address of a native {@code JVMCICompileState} object or 0L if no such object exists.
     */
    protected final long compileState;

    protected final boolean hasUnsafeAccess;

    /**
     * May be set by VM if code installation fails. It will describe in more detail why installation
     * failed (e.g., exactly which dependency failed).
     */
    @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "set by the VM") private String installationFailureMessage;

    public HotSpotCompiledNmethod(String name,
                    byte[] targetCode,
                    int targetCodeSize,
                    Site[] sites,
                    Assumption[] assumptions,
                    ResolvedJavaMethod[] methods,
                    Comment[] comments,
                    byte[] dataSection,
                    int dataSectionAlignment,
                    DataPatch[] dataSectionPatches,
                    boolean isImmutablePIC,
                    int totalFrameSize,
                    StackSlot deoptRescueSlot,
                    HotSpotResolvedJavaMethod method,
                    int entryBCI,
                    int id,
                    long compileState,
                    boolean hasUnsafeAccess) {
        super(name,
                        targetCode,
                        targetCodeSize,
                        sites,
                        assumptions,
                        methods,
                        comments,
                        dataSection,
                        dataSectionAlignment,
                        dataSectionPatches,
                        isImmutablePIC,
                        totalFrameSize,
                        deoptRescueSlot);
        this.method = method;
        this.entryBCI = entryBCI;
        this.id = id;
        this.compileState = compileState;
        this.hasUnsafeAccess = hasUnsafeAccess;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + ":" + method.format("%H.%n(%p)%r@") + entryBCI + "]";
    }

    public String getInstallationFailureMessage() {
        return installationFailureMessage;
    }

    /**
     * Determines if {@code methods} contains at least one entry for which {@code HotSpotResolvedJavaMethod.isScoped()} returns true.
     */
    public boolean hasScopedAccess() {
        if (methods == null) {
            return false;
        }
        for (ResolvedJavaMethod method : methods) {
            if (method instanceof HotSpotResolvedJavaMethod hotSpotMethod) {
                if (hotSpotMethod.isScoped()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the method to which this compiled nmethod belongs.
     */
    public HotSpotResolvedJavaMethod getMethod() {
        return method;
    }

    /**
     * Returns the bytecode index (BCI) in the {@link #getMethod() method} that is the beginning of this compiled
     * nmethod. -1 denotes the beginning of the method.
     *
     * @return the entry BCI of this nmethod or -1 if the entry is the method's beginning
     */
    public int getEntryBCI() {
        return entryBCI;
    }

    /**
     * Returns the identifier of the compilation request.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the address of a native {@code JVMCICompileState} object associated with this compiled nmethod.
     * If no such object exists, it returns 0L.
     *
     * @return the address of the native {@code JVMCICompileState} object or 0L if it does not exist
     */
    public long getCompileState() {
        return compileState;
    }

    /**
     * Checks if this compiled nmethod has a memory access via the {@code Unsafe} class.
     */
    public boolean hasUnsafeAccess() {
        return hasUnsafeAccess;
    }
}
