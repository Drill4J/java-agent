package com.epam.drill.agent.classloading

import java.io.*
import java.net.*
import java.util.*
import java.util.jar.*

private val excludedPaths = listOf(
    "com/epam/drill",
    "com/alibaba/ttl"
)

//TODO Kotlinize
class ClassPath(
    private val includedPaths: Iterable<String>
) {
    val scannedUris = mutableSetOf<URL>()
    val resources = IdentityHashMap<ClassLoader, MutableSet<String>>()

    private val scannedClassLoaders = mutRefSet<ClassLoader>()

    fun scan(classLoaders: Iterable<ClassLoader>): MutableMap<String, ClassLoader> {
        classLoaders.forEach { classLoader ->
            getClassPathEntries(classLoader).forEach { (key, value) ->
                scan(key, value)
            }
        }
        val map = resources.map { (k, v) ->
            v.associateWith { k }
        }
        val mutableMapOf = mutableMapOf<String, ClassLoader>()
        map.forEach {
            mutableMapOf.putAll(it)
        }
        return mutableMapOf
    }

    @Throws(IOException::class)
    fun scan(url: URL, classloader: ClassLoader) {
        if (scannedUris.add(url)) {
            scanFrom(url, classloader)
        }
    }

    @Throws(IOException::class)
    private fun scanFrom(url: URL, classloader: ClassLoader) {
        when (url.protocol) {
            "file" -> {
                val file = toFile(url)

                try {
                    if (!file.exists()) {
                        return
                    }
                } catch (e: SecurityException) {

                    return
                }
                if (file.isDirectory) {
                    scanDirectory(classloader, file)
                } else {
                    scanJar(url, classloader)
                }
            }
            "jar" -> {
                scanJar(url, classloader)
            }
            else -> Unit
        }

    }

    @Throws(IOException::class)
    private fun scanJar(jarUrl: URL, classloader: ClassLoader) {

        val jarFile: JarFile = try {
            when (jarUrl.protocol) {
                "file" -> JarFile(toFile(jarUrl))
                "jar" -> {
                    val jarURLConnection = jarUrl.openConnection() as? JarURLConnection
                    jarURLConnection?.jarFile ?: return
                }
                else -> return
            }

        } catch (e: IOException) {
            // Not a jar file
            return
        }

        try {
            for (path in getClassPathFromManifest(jarUrl, jarFile.manifest)) {
                scan(path, classloader)
            }
            scanJarFile(classloader, jarFile)
        } finally {
            try {
                jarFile.close()
            } catch (ignored: IOException) {
            }

        }
    }

    private fun getClassPathFromManifest(jarUrl: URL, manifest: Manifest?): Set<URL> {
        if (manifest == null) {
            return setOf()
        }
        val builder = mutableSetOf<URL>()
        val classpathAttribute = manifest.mainAttributes.getValue(Attributes.Name.CLASS_PATH.toString())
        if (classpathAttribute != null) {
            for (path in classpathAttribute.split(" ")) {
                val url: URL
                try {
                    url = URL(jarUrl, path)
                } catch (e: MalformedURLException) {
                    continue
                }

                if (url.protocol == "file") {
                    builder.add(url)
                }
            }
        }
        return builder
    }

    private fun getClassPathEntries(classloader: ClassLoader): MutableMap<URL, ClassLoader> {
        val entries = mutableMapOf<URL, ClassLoader>()
        val parent = classloader.parent
        if (parent != null) {
            entries.putAll(getClassPathEntries(parent))
        }
        val urls = if (classloader !in scannedClassLoaders) {
            scannedClassLoaders.add(classloader)
            classloader.getUrls()
        } else emptyList()
        for (url in urls) {
            when (url.protocol) {
                "file", "jar" -> {
                    if (!entries.containsKey(url)) {
                        entries[url] = classloader
                    }
                }
                else -> Unit
            }
        }
        return entries
    }

    private fun toFile(url: URL): File {
        return try {
            File(url.toURI()) // Accepts escaped characters like %20.
        } catch (e: URISyntaxException) { // URL.toURI() doesn't escape chars.
            File(url.path) // Accepts non-escaped chars like space.
        }

    }

    private fun ClassLoader.getUrls(): List<URL> = when (this) {
        ClassLoader.getSystemClassLoader() -> parseJavaClassPath()
        is URLClassLoader -> urLs.toList()
        else -> emptyList()
    }

    private fun parseJavaClassPath(): List<URL> {
        val urls = mutableListOf<URL>()
        for (entry in System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
            try {
                try {
                    urls.add(File(entry).toURI().toURL())
                } catch (e: SecurityException) { // File.toURI checks to see if the file is a directory
                    urls.add(URL("file", null, File(entry).absolutePath))
                }

            } catch (e: MalformedURLException) {
            }

        }
        return urls
    }


    private fun scanJarFile(classloader: ClassLoader, file: JarFile) {
        val entries = file.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.isAllowed()) {
                if (resources[classloader] == null) {
                    resources[classloader] = mutableSetOf()
                }
                resources[classloader]?.add(entry.name)
            }
        }
    }

    @Throws(IOException::class)
    protected fun scanDirectory(classloader: ClassLoader, directory: File) {
        val currentPath = HashSet<File>()
        currentPath.add(directory.canonicalFile)
        scanDirectory(directory, classloader, "", currentPath)
    }

    @Throws(IOException::class)
    private fun scanDirectory(
        directory: File, classloader: ClassLoader, packagePrefix: String, currentPath: MutableSet<File>
    ) {
        val files = directory.listFiles()
            ?: // IO error, just skip the directory
            return
        for (f in files) {
            val name = f.name
            if (f.isDirectory) {
                val deref = f.canonicalFile
                if (currentPath.add(deref)) {
                    scanDirectory(deref, classloader, "$packagePrefix$name/", currentPath)
                    currentPath.remove(deref)
                }
            } else {
                val resourceName = packagePrefix + name
                if (resourceName.isAllowed()) {
                    if (resources[classloader] == null) {
                        resources[classloader] = mutableSetOf()
                    }
                    resources[classloader]?.add(resourceName)
                }
            }
        }
    }

    private fun String.isAllowed(): Boolean = run {
        !startsWithAnyOf(excludedPaths) && !contains("$") &&
            startsWithAnyOf(includedPaths) && endsWith(".class")
    }
}

private fun String.startsWithAnyOf(prefixes: Iterable<String>): Boolean = prefixes.any { startsWith(it) }
