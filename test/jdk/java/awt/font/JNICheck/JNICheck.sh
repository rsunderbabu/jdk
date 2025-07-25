#!/bin/ksh -p
#
# Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
#
#   @test
#   @bug        6430247 8130507 8020448 8172813
#   @summary    Tests that there are no JNI warnings.
#   @compile JNICheck.java
#   @run shell/timeout=300 JNICheck.sh
#
OS=`uname`

# pick up the compiled class files.
if [ -z "${TESTCLASSES}" ]; then
  CP="."
else
  CP="${TESTCLASSES}"
fi

if [ $OS != Linux ]
then
    exit 0
fi

if [ -z "${TESTJAVA}" ] ; then
   JAVA_HOME=../../../../../build/solaris-sparc
else
   JAVA_HOME=$TESTJAVA
fi

$JAVA_HOME/bin/java ${TESTVMOPTS} \
    -cp "${CP}" -Xcheck:jni JNICheck | grep -v SIG | grep -v Signal | grep -v Handler | grep -v jsig > "${CP}"/log.txt

# any messages logged may indicate a failure.
if [ -s "${CP}"/log.txt ]; then
    echo "Test failed"
    exit 1
fi

echo "Test passed"
exit 0
