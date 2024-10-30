/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.jacoco;


import org.jacoco.core.analysis.*;
import org.jacoco.core.internal.analysis.*;

import java.util.*;
import java.util.concurrent.*;

//TODO all modified jacoco classes should be placed in a separate module and connected as a dependency!!
/**
 * Builder for hierarchical {@link ICoverageNode} structures from single
 * {@link IClassCoverage} nodes. The nodes are feed into the builder through its
 * {@link ICoverageVisitor} interface. Afterwards the aggregated data can be
 * obtained with {@link #getClasses()}, {@link #getSourceFiles()} or
 * {@link #getBundle(String)} in the following hierarchy:
 *
 * <pre>
 * {@link IBundleCoverage}
 * +-- {@link IPackageCoverage}*
 *     +-- {@link IClassCoverage}*
 *     +-- {@link ISourceFileCoverage}*
 * </pre>
 */
public class CustomCoverageBuilder implements ICoverageVisitor {

    private final Map<String, IClassCoverage> classes;

    private final Map<String, ISourceFileCoverage> sourcefiles;

    /**
     * Create a new builder.
     */
    public  CustomCoverageBuilder() {
        this.classes = new ConcurrentHashMap<>();
        this.sourcefiles = new ConcurrentHashMap<>();
    }

    /**
     * Returns all class nodes currently contained in this builder.
     *
     * @return all class nodes
     */
    public Collection<IClassCoverage> getClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }

    /**
     * Returns all source file nodes currently contained in this builder.
     *
     * @return all source file nodes
     */
    public Collection<ISourceFileCoverage> getSourceFiles() {
        return Collections.unmodifiableCollection(sourcefiles.values());
    }

    /**
     * Creates a bundle from all nodes currently contained in this bundle.
     *
     * @param name Name of the bundle
     * @return bundle containing all classes and source files
     */
    public IBundleCoverage getBundle(final String name) {
        return new BundleCoverageImpl(name, classes.values(),
                sourcefiles.values());
    }

    /**
     * Returns all classes for which execution data does not match.
     *
     * @return collection of classes with non-matching execution data
     * @see IClassCoverage#isNoMatch()
     */
    public Collection<IClassCoverage> getNoMatchClasses() {
        final Collection<IClassCoverage> result = new ArrayList<>();
        for (final IClassCoverage c : classes.values()) {
            if (c.isNoMatch()) {
                result.add(c);
            }
        }
        return result;
    }

    // === ICoverageVisitor ===

    public void visitCoverage(final IClassCoverage coverage) {
        final String name = coverage.getName();
        final IClassCoverage dup = classes.put(name, coverage);
        if (dup != null) {
            if (dup.getId() != coverage.getId()) {
                throw new IllegalStateException(
                        "Can't add different class with same name: " + name);
            }
        } else {
            final String source = coverage.getSourceFileName();
            if (source != null) {
                final SourceFileCoverageImpl sourceFile = getSourceFile(source,
                        coverage.getPackageName());
                sourceFile.increment(coverage);
            }
        }
    }

    private SourceFileCoverageImpl getSourceFile(final String filename,
                                                 final String packagename) {
        final String key = packagename + '/' + filename;
        SourceFileCoverageImpl sourcefile = (SourceFileCoverageImpl) sourcefiles
                .get(key);
        if (sourcefile == null) {
            sourcefile = new SourceFileCoverageImpl(filename, packagename);
            sourcefiles.put(key, sourcefile);
        }
        return sourcefile;
    }

}
