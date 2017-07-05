/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8058575
 * @summary Creates an anonymous class inside of an anonymous class.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 * @run main p.NestedUnsafe2
 */

package p;

import java.security.ProtectionDomain;
import java.io.InputStream;
import java.lang.*;
import jdk.test.lib.*;
import jdk.internal.misc.Unsafe;
import jdk.test.lib.unsafe.UnsafeHelper;


// Test that an anonymous class that gets put in its host's package cannot define
// an anonymous class in another package.
public class NestedUnsafe2 {
    // The String concatenation should create the nested anonymous class.
    public static byte klassbuf[] = InMemoryJavaCompiler.compile("q.TestClass",
        "package q; " +
        "public class TestClass { " +
        "    public static void concat(String one, String two) throws Throwable { " +
        "        System.out.println(one + two);" +
        " } } ");

    public static void main(String args[]) throws Exception {
        Unsafe unsafe = UnsafeHelper.getUnsafe();

        // The anonymous class calls defineAnonymousClass creating a nested anonymous class.
        byte klassbuf2[] = InMemoryJavaCompiler.compile("TestClass2",
            "import jdk.internal.misc.Unsafe; " +
            "public class TestClass2 { " +
            "    public static void doit() throws Throwable { " +
            "        Unsafe unsafe = jdk.internal.misc.Unsafe.getUnsafe(); " +
            "        Class klass2 = unsafe.defineAnonymousClass(TestClass2.class, p.NestedUnsafe2.klassbuf, new Object[0]); " +
            "        unsafe.ensureClassInitialized(klass2); " +
            "        Class[] dArgs = new Class[2]; " +
            "        dArgs[0] = String.class; " +
            "        dArgs[1] = String.class; " +
            "        try { " +
            "            klass2.getMethod(\"concat\", dArgs).invoke(null, \"CC\", \"DD\"); " +
            "        } catch (Throwable ex) { " +
            "            throw new RuntimeException(\"Exception: \" + ex.toString()); " +
            "        } " +
            "} } ",
            "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED");

        Class klass2 = unsafe.defineAnonymousClass(p.NestedUnsafe2.class, klassbuf2, new Object[0]);
        try {
            klass2.getMethod("doit").invoke(null);
            throw new RuntimeException("Expected exception not thrown");
        } catch (Throwable ex) {
            Throwable iae = ex.getCause();
            if (!iae.toString().contains(
                "IllegalArgumentException: Host class p/NestedUnsafe2 and anonymous class q/TestClass")) {
                throw new RuntimeException("Exception: " + iae.toString());
            }
        }
    }
}
